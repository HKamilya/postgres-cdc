package ru.kpfu.itis.postgrescdc.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

import java.util.List;
import java.util.UUID;

public interface ConnectorRest {

    @GetMapping("")
    List<ConnectorEntity> getConnectors();

    @PostMapping("")
    ResponseEntity<Object> addConnectors(ConnectorModel model);

    @DeleteMapping("/{connectorId}")
    ResponseEntity<Object> deleteConnector(UUID connectorId);

    @PutMapping("/{connectorId}")
    ResponseEntity<Object> deactivateConnector(UUID connectorId);
}
