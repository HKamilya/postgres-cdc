package ru.kpfu.itis.postgrescdc.rest.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.rest.ConnectorRest;
import ru.kpfu.itis.postgrescdc.service.ConnectorService;
import ru.kpfu.itis.postgrescdc.service.replication.ReplicationService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController("/connector")
public class ConnectorRestImpl implements ConnectorRest {
    private final ConnectorService connectorService;
    private final ReplicationService replicationService;

    public ConnectorRestImpl(ConnectorService connectorService, @Qualifier("wal2JsonReplicationService") ReplicationService replicationService) {
        this.connectorService = connectorService;
        this.replicationService = replicationService;
    }

    @Override
    public List<ConnectorEntity> getConnectors() {
        return connectorService.findAll();
    }

    @Override
    public ResponseEntity<Object> addConnectors(@RequestBody @Valid @NotNull ConnectorModel model) {
        model.setId(UUID.randomUUID());
        connectorService.createConnector(model);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Object> deleteConnector(@PathVariable("connectorId") UUID connectorId) {
        Optional<ConnectorEntity> connectorEntity = connectorService.loadConnector(connectorId);
        if (connectorEntity.isPresent() && connectorEntity.get().getCdcInfoEntity() != null) {
            replicationService.dropReplication(connectorEntity.get().getUsername(), connectorEntity.get().getPassword(), connectorEntity.get().getHost(), connectorEntity.get().getPort(), connectorEntity.get().getDatabase(), connectorEntity.get().getCdcInfoEntity().getSlotName());
        }
        connectorService.delete(connectorId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Object> deactivateConnector(@PathVariable("connectorId") UUID connectorId) {
        connectorService.deactivate(connectorId);
        return ResponseEntity.ok().build();
    }
}
