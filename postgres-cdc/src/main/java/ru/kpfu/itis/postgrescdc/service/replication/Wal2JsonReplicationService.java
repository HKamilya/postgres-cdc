package ru.kpfu.itis.postgrescdc.service.replication;

import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.model.DataTypeEnum;

import java.sql.Connection;
import java.util.UUID;

public interface Wal2JsonReplicationService extends ReplicationService {

    void receiveChanges(Connection connection, Connection replicationConnection, boolean fromBegin, DataTypeEnum plugin, String slotName, String publicationName, String topic, UUID connectorId, String tableNames) throws Exception;

    void createConnection(ConnectorModel connectorModel);

    void connectToExistingSlot(ConnectorEntity connectorEntity);
}
