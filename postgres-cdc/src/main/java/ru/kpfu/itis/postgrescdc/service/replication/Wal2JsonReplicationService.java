package ru.kpfu.itis.postgrescdc.service.replication;

import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.model.PluginEnum;

import java.sql.Connection;

public interface Wal2JsonReplicationService {

    void receiveChanges(Connection connection, Connection replicationConnection, boolean fromBegin, PluginEnum plugin, String slotName, String publicationName, String topic) throws Exception;

    void createConnection(ConnectorModel connectorModel);
}
