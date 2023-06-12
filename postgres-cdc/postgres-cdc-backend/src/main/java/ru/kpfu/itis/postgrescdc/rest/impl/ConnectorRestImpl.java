package ru.kpfu.itis.postgrescdc.rest.impl;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.pulsar.shade.io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorChangeModel;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.rest.ConnectorRest;
import ru.kpfu.itis.postgrescdc.service.ConnectorService;
import ru.kpfu.itis.postgrescdc.service.replication.ReplicationService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController()
@RequestMapping("/api/connector")
@Api("Работа с коннекторами")
public class ConnectorRestImpl implements ConnectorRest {
    private final ConnectorService connectorService;
    private final ReplicationService replicationService;

    public ConnectorRestImpl(ConnectorService connectorService, @Qualifier("wal2JsonReplicationService") ReplicationService replicationService) {
        this.connectorService = connectorService;
        this.replicationService = replicationService;
    }

    @Operation(summary = "Получение всех доступных коннекторов")
    @Override
    public List<ConnectorEntity> getConnectors() {
        return connectorService.findAll();
    }

    @Operation(summary = "Добавление нового коннектора")
    @Override
    public ResponseEntity<Object> addConnector(@RequestBody @Valid @NotNull ConnectorModel model) {
        model.setId(UUID.randomUUID());
        connectorService.create(model);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удаление коннектора")
    @Override
    public ResponseEntity<Object> deleteConnector(UUID connectorId) {
        Optional<ConnectorEntity> connectorEntity = connectorService.loadConnector(connectorId);
        if (connectorEntity.isPresent() && connectorEntity.get().getCdcInfoEntity() != null) {
            replicationService.dropReplication(connectorEntity.get().getUsername(), connectorEntity.get().getPassword(), connectorEntity.get().getHost(), connectorEntity.get().getPort(), connectorEntity.get().getDatabase(), connectorEntity.get().getCdcInfoEntity().getSlotName());
        }
        connectorService.delete(connectorId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Деактивация коннектора")
    @Override
    public ResponseEntity<Object> deactivateConnector(UUID connectorId) {
        connectorService.deactivate(connectorId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Изменение коннектора")
    @Override
    public ResponseEntity<Object> changeConnector(UUID id, ConnectorChangeModel model) {
        connectorService.change(id, model);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Получение коннектора")
    @Override
    public ResponseEntity<Object> getConnector(UUID connectorId) {
        return ResponseEntity.ok(connectorService.loadConnector(connectorId));
    }

    @Override
    public ResponseEntity<Object> getChanges(UUID connectorId) {
        return ResponseEntity.ok(connectorService.getChanges(connectorId));
    }
}
