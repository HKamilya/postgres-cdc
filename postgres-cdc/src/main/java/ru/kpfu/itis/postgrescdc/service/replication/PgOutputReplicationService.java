package ru.kpfu.itis.postgrescdc.service.replication;

import org.springframework.scheduling.annotation.Async;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

import java.sql.Connection;
import java.util.UUID;

public interface PgOutputReplicationService extends ReplicationService {

    void receiveChanges(Connection connection, Connection replicationConnection, boolean fromBegin, String slotName, String publicationName, String topic, UUID id) throws Exception;

    void createConnection(ConnectorModel connectorModel);

    @Async
    void connectToExistingSlot(ConnectorEntity connectorEntity);
}
