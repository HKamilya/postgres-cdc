package ru.kpfu.itis.postgrescdc.service.replication;

import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

import java.sql.Connection;

public interface ProtoReplicationService {

    void receiveChanges(Connection connection, Connection replicationConnection, boolean fromBegin, String slotName, String publicationName, String topic) throws Exception;

    void createConnection(ConnectorModel connectorModel);
}
