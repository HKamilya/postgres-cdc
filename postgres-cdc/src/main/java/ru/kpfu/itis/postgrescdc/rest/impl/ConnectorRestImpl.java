package ru.kpfu.itis.postgrescdc.rest.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.rest.ConnectorRest;
import ru.kpfu.itis.postgrescdc.service.SenderService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController("/connector")
@RequiredArgsConstructor
public class ConnectorRestImpl implements ConnectorRest {
    private final SenderService service;

    @GetMapping("")
    @Override
    public List<String> getConnectors() {
        return Collections.emptyList();
    }

    @PostMapping("")
    @Override
    public ResponseEntity<Object> addConnectors(@RequestBody @Valid @NotNull ConnectorModel model) {
        model.setId(UUID.randomUUID());
        service.createConnector(model);
        return ResponseEntity.ok().build();
    }
}
