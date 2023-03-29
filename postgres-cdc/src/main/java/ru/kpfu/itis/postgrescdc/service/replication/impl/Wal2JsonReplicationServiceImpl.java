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
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.model.PluginEnum;
import ru.kpfu.itis.postgrescdc.service.ConnectorService;
import ru.kpfu.itis.postgrescdc.service.ProducerService;
import ru.kpfu.itis.postgrescdc.service.replication.ReplicationServiceImpl;
import ru.kpfu.itis.postgrescdc.service.replication.Wal2JsonReplicationService;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class Wal2JsonReplicationServiceImpl extends ReplicationServiceImpl implements Wal2JsonReplicationService {

    private final ConnectorService connectorService;
    private final ProducerService sender;

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
            if (!isReplicationSlotExists(connectorModel.getSlotName(), PluginEnum.wal2json.name(), connection)) {

                createLogicalReplicationSlot(connection, connectorModel.getSlotName(), PluginEnum.wal2json.name());
                try {
                    dropPublication(connection, connectorModel.getPublicationName());
                } catch (PSQLException e) {
                    // ignore
                }
                createPublication(connection, connectorModel.getPublicationName(), connectorModel.isForAllTables(), connectorModel.getTables());
            }
            if (!isReplicationSlotActive(connection, connectorModel.getSlotName())) {
                Connection replicationConnection = openReplicationConnection(connectorModel.getUser(), connectorModel.getPassword(), connectorModel.getHost(), connectorModel.getPort(), connectorModel.getDatabase());
                receiveChanges(connection, replicationConnection, connectorModel.isFromBegin(), connectorModel.getPlugin(), connectorModel.getSlotName(), connectorModel.getPublicationName(), connectorModel.getTopicName(), connectorModel.getId());
            }

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void receiveChanges(Connection connection,
                               Connection replicationConnection,
                               boolean fromBegin,
                               PluginEnum plugin,
                               String slotName,
                               String publicationName,
                               String topic,
                               UUID connectorId) throws Exception {
        PGConnection pgConnection = (PGConnection) replicationConnection;

        LogSequenceNumber lsn;
        if (fromBegin) {
            lsn = getAllLSN(connection, slotName);
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
            Optional<ConnectorEntity> connector = connectorService.loadConnector(connectorId);
            connector.ifPresent(connectorEntity -> connectorService.updateCdcInfo(connectorEntity, stream.getLastReceiveLSN(), publicationName, slotName, changes));
            if (plugin == PluginEnum.wal2json) {
                sender.sendJsonAsync(changes, topic);
            }
            if (plugin == PluginEnum.avro) {
                sender.sendAvroAsync(changes, topic);
            }
            if (plugin == PluginEnum.decoderbufs) {
                sender.sendProtoAsync(changes, topic);
            }
            stream.setAppliedLSN(stream.getLastReceiveLSN());
        }
    }

    @Async
    @Override
    public void connectToExistingSlot(ConnectorEntity connectorEntity) {
        Connection connection;
        try {
            connection = createConnection(connectorEntity.getUsername(), connectorEntity.getPassword(), connectorEntity.getHost(), connectorEntity.getPort(), connectorEntity.getDatabase());

            if (!isServerCompatible(connection)) {
                log.info("must have server version greater than 9.4");
                System.exit(-1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }

        try {
            if (!isReplicationSlotActive(connection, connectorEntity.getCdcInfoEntity().getSlotName())) {
                Connection replicationConnection = openReplicationConnection(connectorEntity.getUsername(), connectorEntity.getPassword(), connectorEntity.getHost(), connectorEntity.getPort(), connectorEntity.getDatabase());

                receiveChangesFromCurrentLsn(connection, replicationConnection, connectorEntity.getPlugin(), connectorEntity.getCdcInfoEntity().getSlotName(), connectorEntity.getCdcInfoEntity().getPublicationName(), connectorEntity.getTopicName(), connectorEntity.getId(), connectorEntity.getCdcInfoEntity().getLastAppliedChange() != null ? connectorEntity.getCdcInfoEntity().getLastAppliedChange().getLsn() : null);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void receiveChangesFromCurrentLsn(Connection connection,
                                             Connection replicationConnection,
                                             PluginEnum plugin,
                                             String slotName,
                                             String publicationName,
                                             String topic,
                                             UUID connectorId,
                                             String lsnString) throws Exception {
        LogSequenceNumber lsn;
        if (lsnString == null) {
            lsn = getCurrentLSN(connection);
        } else {
            lsn = LogSequenceNumber.valueOf(lsnString);
        }
        PGConnection pgConnection = (PGConnection) replicationConnection;

        PGReplicationStream stream =
                pgConnection
                        .getReplicationAPI()
                        .replicationStream()
                        .logical()
                        .withSlotName(slotName)
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
            Optional<ConnectorEntity> connector = connectorService.loadConnector(connectorId);
            connector.ifPresent(connectorEntity -> connectorService.updateCdcInfo(connectorEntity, stream.getLastReceiveLSN(), publicationName, slotName, changes));
            if (plugin == PluginEnum.wal2json) {
                sender.sendJsonAsync(changes, topic);
            }
            if (plugin == PluginEnum.avro) {
                sender.sendAvroAsync(changes, topic);
            }
            if (plugin == PluginEnum.decoderbufs) {
                sender.sendProtoAsync(changes, topic);
            }
            stream.setAppliedLSN(stream.getLastReceiveLSN());
        }
    }

    private LogSequenceNumber getAllLSN(Connection connection, String slotName) throws SQLException {
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT * FROM pg_logical_slot_peek_changes('" + slotName + "', null, null);")) {

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


    private static String toString(ByteBuffer buffer) {
        int offset = buffer.arrayOffset();
        byte[] source = buffer.array();
        int length = source.length - offset;

        return new String(source, offset, length);
    }
}
