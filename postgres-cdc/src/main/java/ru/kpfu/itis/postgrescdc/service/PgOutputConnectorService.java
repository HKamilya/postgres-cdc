package ru.kpfu.itis.postgrescdc.service;

import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

import java.sql.Connection;

public interface PgOutputConnectorService {

    void receiveChangesOccursBeforeStartReplication(Connection connection, Connection replicationConnection, boolean fromBegin) throws Exception;

    void createConnection(ConnectorModel connectorModel);
}
