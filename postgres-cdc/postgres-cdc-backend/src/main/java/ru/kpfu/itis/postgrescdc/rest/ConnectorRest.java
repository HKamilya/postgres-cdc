package ru.kpfu.itis.postgrescdc.rest;

import org.apache.pulsar.shade.io.swagger.annotations.Api;
import org.apache.pulsar.shade.io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorChangeModel;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

import java.util.List;
import java.util.UUID;

@Api("Работа с коннекторами")
public interface ConnectorRest {

    @ApiOperation("Получение всех доступных коннекторов")
    @GetMapping("")
    List<ConnectorEntity> getConnectors();

    @ApiOperation("Добавление нового коннектора")
    @PostMapping("")
    ResponseEntity<Object> addConnector(ConnectorModel model);

    @ApiOperation("Удаление коннектора")
    @DeleteMapping("/{connectorId}")
    ResponseEntity<Object> deleteConnector(@PathVariable("connectorId") UUID connectorId);

    @ApiOperation("Деактивация коннектора")
    @PutMapping("/{connectorId}")
    ResponseEntity<Object> deactivateConnector(@PathVariable("connectorId") UUID connectorId);

    @ApiOperation("Изменение коннектора")
    @PostMapping("/{connectorId}")
    ResponseEntity<Object> changeConnector(@PathVariable("connectorId") UUID connectorId, ConnectorChangeModel model);

    @ApiOperation("Получение коннектора")
    @GetMapping("/{connectorId}")
    ResponseEntity<Object> getConnector(@PathVariable("connectorId") UUID connectorId);

    @ApiOperation("Получение журнала изменений")
    @GetMapping("/{connectorId}/changes")
    ResponseEntity<Object> getChanges(@PathVariable("connectorId") UUID connectorId);
}

