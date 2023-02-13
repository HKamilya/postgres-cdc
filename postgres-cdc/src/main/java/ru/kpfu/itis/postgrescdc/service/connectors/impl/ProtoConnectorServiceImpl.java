package ru.kpfu.itis.postgrescdc.service.connectors.impl;

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
import ru.kpfu.itis.postgrescdc.service.SenderService;
import ru.kpfu.itis.postgrescdc.service.connectors.ConnectorServiceImpl;
import ru.kpfu.itis.postgrescdc.service.connectors.ProtoConnectorService;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProtoConnectorServiceImpl extends ConnectorServiceImpl implements ProtoConnectorService {

    private static final String SLOT_NAME = "cdc_postgres_proto_replication_slot";
    private final SenderService sender;
    private static final String PUBLICATION = "cdc_postgres_proto_pub";

    private static String toString(ByteBuffer buffer) {
        int offset = buffer.arrayOffset();
        byte[] source = buffer.array();
        int length = source.length - offset;

        return new String(source, offset, length);
    }

    @Override
    public void receiveChanges(Connection connection, Connection replicationConnection, boolean fromBegin) throws Exception {
        PGConnection pgConnection = (PGConnection) replicationConnection;

        LogSequenceNumber lsn;
        if (fromBegin) {
            lsn = getAllLSN(connection);
        } else {
            lsn = getCurrentLSN(connection);
        }

        PGReplicationStream stream =
                pgConnection
                        .getReplicationAPI()
                        .replicationStream()
                        .logical()
                        .withSlotName(SLOT_NAME)
                        .withStartPosition(lsn)
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
            sender.sendProtoAsync(changes.getBytes());

            stream.setAppliedLSN(stream.getLastReceiveLSN());
            stream.setFlushedLSN(stream.getLastReceiveLSN());
        }

    }

    private LogSequenceNumber getAllLSN(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT * FROM pg_logical_slot_peek_binary_changes('" + SLOT_NAME + "', null, null);")) {

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


    @Async
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
            if (!isReplicationSlotExists(SLOT_NAME, PluginEnum.decoderbufs.name(), connection)) {
                createLogicalReplicationSlot(connection, SLOT_NAME, PluginEnum.decoderbufs.name());
                try {
                    dropPublication(connection, PUBLICATION);
                } catch (PSQLException e) {
                    throw new IllegalArgumentException(e);
                }
                createPublication(connection, PUBLICATION, connectorModel.isForAllTables(), connectorModel.getTables());
            }
            Connection replicationConnection = openReplicationConnection(connectorModel.getUser(), connectorModel.getPassword(), connectorModel.getHost(), connectorModel.getPort(), connectorModel.getDatabase());
            receiveChanges(connection, replicationConnection, connectorModel.isFromBegin());

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
