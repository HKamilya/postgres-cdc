package ru.kpfu.itis.postgrescdc.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kpfu.itis.postgrescdc.entity.CdcInfoEntity;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.repository.ConnectorRepository;
import ru.kpfu.itis.postgrescdc.service.ConnectorService;
import ru.kpfu.itis.postgrescdc.service.ProducerService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectorServiceImpl implements ConnectorService {
    private final ProducerService producerService;
    private final ConnectorRepository connectorRepository;

    @Override
    public void createConnector(ConnectorModel model) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

        CdcInfoEntity cdcInfoEntity = new CdcInfoEntity();
        cdcInfoEntity.setId(UUID.randomUUID());
        cdcInfoEntity.setSlotName(model.getSlotName());
        cdcInfoEntity.setPublicationName(model.getPublicationName());
        cdcInfoEntity.setCreateDt(now);
        cdcInfoEntity.setChangeDt(now);

        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(UUID.randomUUID());
        connectorEntity.setHost(model.getHost());
        connectorEntity.setPort(model.getPort());
        connectorEntity.setDatabase(model.getDatabase());
        connectorEntity.setUsername(model.getUser());
        connectorEntity.setPassword(model.getPassword());
        connectorEntity.setPlugin(model.getPlugin());
        connectorEntity.setFromBegin(model.isFromBegin());
        connectorEntity.setForAllTables(model.isForAllTables());
        connectorEntity.setTables(model.getTables());
        connectorEntity.setCreateDt(now);
        connectorEntity.setChangeDt(now);
        connectorEntity.setCdcInfoEntity(cdcInfoEntity);

        connectorRepository.save(connectorEntity);
        model.setId(connectorEntity.getId());
        producerService.createConnector(model);
    }
}
