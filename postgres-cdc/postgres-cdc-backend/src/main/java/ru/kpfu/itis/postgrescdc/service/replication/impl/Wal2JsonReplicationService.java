package ru.kpfu.itis.postgrescdc.service.replication.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.ServerVersion;
import org.postgresql.replication.LogSequenceNumber;
import org.postgresql.replication.PGReplicationStream;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.kpfu.itis.postgrescdc.ConverterUtils;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.Changes;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.model.DataTypeEnum;
import ru.kpfu.itis.postgrescdc.model.PluginEnum;
import ru.kpfu.itis.postgrescdc.service.ConnectorService;
import ru.kpfu.itis.postgrescdc.service.ProducerService;

import java.net.ConnectException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
public class Wal2JsonReplicationService implements ru.kpfu.itis.postgrescdc.service.replication.Wal2JsonReplicationService {

    private final ConnectorService connectorService;
    private final ProducerService sender;
    private static final Integer RETRY_CONNECTION_COUNT = 3;

    @Async
    public void createConnection(ConnectorModel connectorModel) {
        int retryCount = RETRY_CONNECTION_COUNT;
        Connection connection = null;

        try {
            connection = createConnection(connectorModel.getUser(), connectorModel.getPassword(), connectorModel.getHost(), connectorModel.getPort(), connectorModel.getDatabase());

            if (!isServerCompatible(connection)) {
                log.info("must have server version greater than 9.4");
                System.exit(-1);
            }
        } catch (SQLException ex) {
            if (ex.getCause() instanceof ConnectException) {
                while (retryCount > 0) {
                    try {
                        connection = createConnection(connectorModel.getUser(), connectorModel.getPassword(), connectorModel.getHost(), connectorModel.getPort(), connectorModel.getDatabase());
                    } catch (SQLException e) {
                        retryCount--;
                        e.printStackTrace();
                    }
                }
            }
            if (connection == null) {
                throw new IllegalStateException(ex);
            }
            return;
        }
        try {
            if (!isReplicationSlotExists(connectorModel.getSlotName(), PluginEnum.wal2json.name(), connection)) {

                createLogicalReplicationSlot(connection, connectorModel.getSlotName(), PluginEnum.wal2json.name());
            }
            if (!isReplicationSlotActive(connection, connectorModel.getSlotName())) {
                Connection replicationConnection = openReplicationConnection(connectorModel.getUser(), connectorModel.getPassword(), connectorModel.getHost(), connectorModel.getPort(), connectorModel.getDatabase());
                receiveChanges(connection, replicationConnection, connectorModel.isFromBegin(), connectorModel.getDataType(), connectorModel.getSlotName(), connectorModel.getPublicationName(), connectorModel.getTopicName(), connectorModel.getId(), connectorModel.getTables());
            }

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void receiveChanges(Connection connection,
                               Connection replicationConnection,
                               boolean fromBegin,
                               DataTypeEnum dataType,
                               String slotName,
                               String publicationName,
                               String topic,
                               UUID connectorId, String tableNames) throws Exception {
        PGConnection pgConnection = (PGConnection) replicationConnection;
        LogSequenceNumber lsn;
        String[] tables = tableNames.split(",");
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
            Optional<ConnectorEntity> connector = connectorService.loadConnector(connectorId);
            if (connector.isPresent() && connector.get().getIsActive()) {
                String changes = toString(buffer);
                boolean containsTableChanges = checkContainsTable(tables, changes);
                if (containsTableChanges) {
                    log.info(changes);
                    connectorService.updateCdcInfo(connectorId, stream.getLastReceiveLSN(), publicationName, slotName, changes);
                    if (dataType == DataTypeEnum.json) {
                        sender.sendJsonAsync(changes, topic);
                    }
                    if (dataType == DataTypeEnum.avro) {
                        sender.sendAvroAsync(changes, topic);
                    }
                    if (dataType == DataTypeEnum.proto) {
                        sender.sendProtoAsync(changes, topic);
                    }
                    if (dataType == DataTypeEnum.bytes) {
                        sender.sendByteAsync(changes.getBytes(), topic);
                    }
                }
                stream.setAppliedLSN(stream.getLastReceiveLSN());
            } else {
                stream.close();
                break;
            }
        }
    }

    @Async
    @Override
    public void connectToExistingSlot(ConnectorEntity connectorEntity) {
        int retryCount = RETRY_CONNECTION_COUNT;
        Connection connection = null;
        try {
            connection = createConnection(connectorEntity.getUsername(), connectorEntity.getPassword(), connectorEntity.getHost(), connectorEntity.getPort(), connectorEntity.getDatabase());

            if (!isServerCompatible(connection)) {
                log.info("must have server version greater than 9.4");
                System.exit(-1);
            }
        } catch (SQLException ex) {
            if (ex.getCause() instanceof ConnectException) {
                while (retryCount > 0) {
                    try {
                        connection = createConnection(connectorEntity.getUsername(), connectorEntity.getPassword(), connectorEntity.getHost(), connectorEntity.getPort(), connectorEntity.getDatabase());
                    } catch (SQLException e) {
                        retryCount--;
                        e.printStackTrace();
                    }
                }
            }
            if (connection == null) {
                throw new IllegalStateException(ex);
            }
        }

        retryCount = RETRY_CONNECTION_COUNT;
        try {
            if (!isReplicationSlotActive(connection, connectorEntity.getCdcInfoEntity().getSlotName())) {
                Connection replicationConnection = openReplicationConnection(connectorEntity.getUsername(), connectorEntity.getPassword(), connectorEntity.getHost(), connectorEntity.getPort(), connectorEntity.getDatabase());

                receiveChangesFromCurrentLsn(connection, replicationConnection, connectorEntity.getDataType(), connectorEntity.getCdcInfoEntity().getSlotName(), connectorEntity.getCdcInfoEntity().getPublicationName(), connectorEntity.getTopicName(), connectorEntity.getId(), connectorEntity.getCdcInfoEntity().getLastAppliedChange(), connectorEntity.getTables());
            }
        } catch (Exception e) {
            if (e instanceof SocketException) {
                while (retryCount > 0) {
                    Optional<ConnectorEntity> connector = connectorService.loadConnector(connectorEntity.getId());
                    connector.ifPresent(this::connectToExistingSlot);
                    retryCount--;
                }
            }
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void receiveChangesFromCurrentLsn(Connection connection,
                                             Connection replicationConnection,
                                             DataTypeEnum dataType,
                                             String slotName,
                                             String publicationName,
                                             String topic,
                                             UUID connectorId,
                                             String lsnString, String tableNames) throws Exception {
        LogSequenceNumber lsn;
        String[] tables = tableNames.split(",");
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
            Optional<ConnectorEntity> connector = connectorService.loadConnector(connectorId);
            if (connector.isPresent() && connector.get().getIsActive()) {
                String changes = toString(buffer);
                boolean containsTableChanges = checkContainsTable(tables, changes);
                if (containsTableChanges) {
                    log.info(changes);
                    connectorService.updateCdcInfo(connectorId, stream.getLastReceiveLSN(), publicationName, slotName, changes);
                    if (dataType == DataTypeEnum.json) {
                        sender.sendJsonAsync(changes, topic);
                    }
                    if (dataType == DataTypeEnum.avro) {
                        sender.sendAvroAsync(changes, topic);
                    }
                    if (dataType == DataTypeEnum.proto) {
                        sender.sendProtoAsync(changes, topic);
                    }
                    if (dataType == DataTypeEnum.bytes) {
                        sender.sendByteAsync(changes.getBytes(), topic);
                    }
                }
                stream.setAppliedLSN(stream.getLastReceiveLSN());
            } else {
                stream.close();
                break;
            }
        }
    }

    private boolean checkContainsTable(String[] tables, String changes) {
        boolean contains = false;
        if (tables.length > 0) {
            for (String table : tables) {
                String tableName = table;
                int index = table.indexOf(".");
                if (index != -1) {
                    tableName = table.substring(index + 1);
                }
                Changes obj = ConverterUtils.toObject(changes);
                String finalTableName = tableName;
                contains = obj.getChange().stream()
                        .map(Changes.Change::getTable)
                        .anyMatch(name -> name.equalsIgnoreCase(finalTableName));
            }
        }
        return contains;
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
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }
}
