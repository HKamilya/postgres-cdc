package ru.kpfu.itis.postgrescdc.service.replication.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.ServerVersion;
import org.postgresql.replication.LogSequenceNumber;
import org.postgresql.replication.PGReplicationStream;
import org.postgresql.util.PSQLException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.model.PluginEnum;
import ru.kpfu.itis.postgrescdc.service.ProducerService;
import ru.kpfu.itis.postgrescdc.service.replication.ReplicationServiceImpl;
import ru.kpfu.itis.postgrescdc.service.replication.PgOutputReplicationService;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PgOutputReplicationServiceImpl extends ReplicationServiceImpl implements PgOutputReplicationService {
    private final ProducerService sender;

    private static String toString(ByteBuffer buffer) {
        int offset = buffer.arrayOffset();
        byte[] source = buffer.array();
        int length = source.length - offset;

        return new String(source, offset, length);
    }

    @Override
    public void receiveChanges(Connection connection,
                               Connection replicationConnection,
                               boolean fromBegin,
                               String slotName,
                               String publicationName,
                               String topic) throws Exception {
        PGConnection pgConnection = (PGConnection) replicationConnection;

        LogSequenceNumber lsn;
        if (fromBegin) {
            lsn = getAllLSN(connection, slotName, publicationName);
        } else {
            lsn = getCurrentLSN(connection);
        }

        PGReplicationStream stream =
                pgConnection
                        .getReplicationAPI()
                        .replicationStream()
                        .logical()
                        .withSlotName(slotName)
                        .withStartPosition(lsn)
                        .withSlotOption("proto_version", 1)
                        .withSlotOption("publication_names", publicationName)
                        .withStatusInterval(10, TimeUnit.SECONDS)
                        .start();
        ByteBuffer buffer;
        while (true) {
            buffer = stream.readPending();
            if (buffer == null) {
                TimeUnit.MILLISECONDS.sleep(10L);
                continue;
            }

            String changes = toString(buffer);
            log.info(changes);
            sender.sendByteAsync(changes.getBytes(), topic);
            stream.setAppliedLSN(stream.getLastReceiveLSN());
            stream.setFlushedLSN(stream.getLastReceiveLSN());
        }

    }

    @Async
    @Override
    public void createConnection(ConnectorModel connectorModel) {
        Connection connection;

        try {
            connection = createConnection(connectorModel.getUser(), connectorModel.getPassword(), connectorModel.getHost(), connectorModel.getPort(), connectorModel.getDatabase());

            if (!isServerCompatible(connection)) {
                log.info("must have server version greater than 9.4");
                System.exit(-1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }
        try {
            if (!isReplicationSlotExists(connectorModel.getSlotName(), PluginEnum.pgoutput.name(), connection)) {

                createLogicalReplicationSlot(connection, connectorModel.getSlotName(), PluginEnum.pgoutput.name());
                try {
                    dropPublication(connection, connectorModel.getPublicationName());
                } catch (PSQLException e) {
                    // ignore
                }
                createPublication(connection, connectorModel.getPublicationName(), connectorModel.isForAllTables(), connectorModel.getTables());
            }
            Connection replicationConnection = openReplicationConnection(connectorModel.getUser(), connectorModel.getPassword(), connectorModel.getHost(), connectorModel.getPort(), connectorModel.getDatabase());
            receiveChanges(connection, replicationConnection, connectorModel.isFromBegin(), connectorModel.getSlotName(), connectorModel.getPublicationName(), connectorModel.getTopicName());

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private LogSequenceNumber getAllLSN(Connection connection, String slotName, String publicationName) throws SQLException {
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT * FROM pg_logical_slot_peek_binary_changes('" + slotName + "', null, null, 'proto_version', '1', 'publication_names', '" + publicationName + "');")) {

                if (rs.next()) {
                    String lsn = rs.getString(1);
                    return LogSequenceNumber.valueOf(lsn);
                } else {
                    return LogSequenceNumber.INVALID_LSN;
                }
            }
        }
    }

    private boolean isServerCompatible(Connection connection) {
        return ((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v9_5);
    }

}