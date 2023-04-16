package ru.kpfu.itis.postgrescdc.service.impl;

import lombok.RequiredArgsConstructor;
import org.postgresql.replication.LogSequenceNumber;
import org.springframework.stereotype.Service;
import ru.kpfu.itis.postgrescdc.entity.CdcInfoEntity;
import ru.kpfu.itis.postgrescdc.entity.ChangeEntity;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.model.DataTypeEnum;
import ru.kpfu.itis.postgrescdc.model.PluginEnum;
import ru.kpfu.itis.postgrescdc.repository.CdcInfoRepository;
import ru.kpfu.itis.postgrescdc.repository.ChangeRepository;
import ru.kpfu.itis.postgrescdc.repository.ConnectorRepository;
import ru.kpfu.itis.postgrescdc.service.ConnectorService;
import ru.kpfu.itis.postgrescdc.service.ProducerService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectorServiceImpl implements ConnectorService {
    private final ProducerService producerService;
    private final ConnectorRepository connectorRepository;
    private final ChangeRepository changeRepository;
    private final CdcInfoRepository cdcInfoRepository;

    @Override
    public void createConnector(ConnectorModel model) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

        CdcInfoEntity cdcInfoEntity = new CdcInfoEntity();
        cdcInfoEntity.setId(UUID.randomUUID());
        cdcInfoEntity.setSlotName(model.getSlotName());
        cdcInfoEntity.setPublicationName(model.getPublicationName());
        cdcInfoEntity.setCreateDt(now);
        cdcInfoEntity.setChangeDt(now);
        cdcInfoRepository.save(cdcInfoEntity);
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(UUID.randomUUID());
        connectorEntity.setHost(model.getHost());
        connectorEntity.setPort(model.getPort());
        connectorEntity.setDatabase(model.getDatabase());
        connectorEntity.setUsername(model.getUser());
        connectorEntity.setPassword(model.getPassword());
        if (model.getDataType() == DataTypeEnum.bytes) {
            connectorEntity.setPlugin(PluginEnum.pgoutput);
        } else {
            connectorEntity.setPlugin(PluginEnum.wal2json);
        }
        connectorEntity.setDataType(model.getDataType());
        connectorEntity.setFromBegin(model.isFromBegin());
        connectorEntity.setForAllTables(model.isForAllTables());
        connectorEntity.setTables(model.getTables());
        connectorEntity.setTopicName(model.getTopicName());
        connectorEntity.setCreateDt(now);
        connectorEntity.setChangeDt(now);
        connectorEntity.setCdcInfoEntity(cdcInfoEntity);
        connectorEntity.setIsActive(true);

        connectorRepository.save(connectorEntity);
        model.setId(connectorEntity.getId());
        producerService.createConnector(model);
    }


    @Override
    public void updateCdcInfo(UUID connectorId, LogSequenceNumber lastReceiveLSN, String publicationName, String slotName, String changes) {
        Optional<ConnectorEntity> connectorEntityOptional = connectorRepository.findById(connectorId);
        if (connectorEntityOptional.isPresent()) {
            ConnectorEntity connector = connectorEntityOptional.get();
            LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
            CdcInfoEntity cdcInfoEntity = connector.getCdcInfoEntity();
            UUID lastLsnId = UUID.randomUUID();
            if (connector.getCdcInfoEntity() == null) {
                cdcInfoEntity = new CdcInfoEntity();
                cdcInfoEntity.setId(UUID.randomUUID());
                cdcInfoEntity.setSlotName(slotName);
                cdcInfoEntity.setPublicationName(publicationName);
                cdcInfoEntity.setCreateDt(now);
                cdcInfoEntity.setChangeDt(now);
                connector.setCdcInfoEntity(cdcInfoEntity);
                connector.setChangeDt(now);
                connectorRepository.save(connector);
            }
            ChangeEntity changeEntity = new ChangeEntity();
            changeEntity.setId(lastLsnId);
            changeEntity.setCdcInfoEntityId(cdcInfoEntity.getId());
            changeEntity.setLsn(lastReceiveLSN.asString());
            changeEntity.setChanges(changes);
            changeEntity.setCreateDt(now);
            changeEntity.setChangeDt(now);
            changeRepository.save(changeEntity);
            cdcInfoEntity.setLastAppliedChange(changeEntity);
            cdcInfoEntity.setChangeDt(now);
            cdcInfoRepository.save(cdcInfoEntity);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Optional<ConnectorEntity> loadConnector(UUID id) {
        return connectorRepository.findById(id);
    }

    @Override
    public List<ConnectorEntity> findAll() {
        return connectorRepository.findAll();
    }

    @Override
    public void delete(UUID connectorId) {
        connectorRepository.deleteById(connectorId);
    }

    @Override
    public void deactivate(UUID connectorId) {
        Optional<ConnectorEntity> connector = connectorRepository.findById(connectorId);
        if (connector.isPresent()) {
            connector.get().setIsActive(false);
            connector.get().setChangeDt(LocalDateTime.now(Clock.systemUTC()));
            connectorRepository.save(connector.get());
        } else {
            throw new IllegalArgumentException("connector with id is not exists");
        }
    }
}
