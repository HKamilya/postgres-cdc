package ru.kpfu.itis.postgrescdc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.springframework.stereotype.Component;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;
import ru.kpfu.itis.postgrescdc.service.replication.Wal2JsonReplicationService;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConnectorsListener implements org.apache.pulsar.client.api.MessageListener<ConnectorModel> {
    private final Wal2JsonReplicationService wal2JsonReplicationService;
    private final ProducerService producerService;

    @Override
    public void received(Consumer<ConnectorModel> consumer, Message<ConnectorModel> msg) {
        try {
            log.info("Topic Name: {}", msg.getTopicName());
            log.info("Message Id: {}", msg.getMessageId());
            log.info("Producer Name: {}", msg.getProducerName());
            log.info("Publish Time: {}", msg.getPublishTime());

            log.info("Message received: {}", new String(msg.getData()));
            log.info("####################################################################################");
            consumer.acknowledge(msg);
            switch (msg.getValue().getDataType()) {
                case bytes -> {
                    if (producerService.createByteProducer(msg.getValue().getTopicName())) {
                        wal2JsonReplicationService.createConnection(msg.getValue());
                    }
                }
                case json -> {
                    if (producerService.createJsonProducer(msg.getValue().getTopicName())) {
                        wal2JsonReplicationService.createConnection(msg.getValue());
                    }
                }
                case avro -> {
                    if (producerService.createAvroProducer(msg.getValue().getTopicName())) {
                        wal2JsonReplicationService.createConnection(msg.getValue());
                    }
                }
                case proto -> {
                    if (producerService.createProtoProducer(msg.getValue().getTopicName())) {
                        wal2JsonReplicationService.createConnection(msg.getValue());
                    }
                }
            }
        } catch (Exception e) {
            consumer.negativeAcknowledge(msg);
        }
    }
}
