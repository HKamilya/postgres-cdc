package ru.kpfu.itis.postgrescdc.service;

import org.postgresql.replication.LogSequenceNumber;
import ru.kpfu.itis.postgrescdc.entity.ChangeEntity;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorChangeModel;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectorService {
    void create(ConnectorModel model);

    void updateCdcInfo(UUID connectorId, LogSequenceNumber lastReceiveLSN, String publicationName, String slotName, String changes);

    Optional<ConnectorEntity> loadConnector(UUID id);

    List<ConnectorEntity> findAll();

    void delete(UUID connectorId);

    void deactivate(UUID connectorId);

    void change(UUID id, ConnectorChangeModel model);

    List<ChangeEntity> getChanges(UUID connectorId);
}
