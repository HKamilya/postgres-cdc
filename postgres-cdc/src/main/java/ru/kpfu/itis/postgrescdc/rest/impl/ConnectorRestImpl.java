package ru.kpfu.itis.postgrescdc.rest.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.kpfu.itis.postgrescdc.service.*;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.rest.ConnectorRest;
import ru.kpfu.itis.postgrescdc.service.Wal2JsonConnectorService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@RestController("/connector")
@RequiredArgsConstructor
public class ConnectorRestImpl implements ConnectorRest {
    private final Wal2JsonConnectorService wal2JsonConnectorService;
    private final PgOutputConnectorService pgOutputConnectorService;
    private final ProtoConnectorService protoConnectorService;

    @GetMapping("")
    @Override
    public List<String> getConnectors() {
        return Collections.emptyList();
    }

    @PostMapping("")
    @Override
    public void addConnectors(@RequestBody @Valid @NotNull ConnectorModel model) {
        switch (model.getPlugin()) {
            case pgoutput -> pgOutputConnectorService.createConnection(model);
            case wal2json -> wal2JsonConnectorService.createConnection(model);
            case decoderbufs -> protoConnectorService.createConnection(model);
        }
    }
}
