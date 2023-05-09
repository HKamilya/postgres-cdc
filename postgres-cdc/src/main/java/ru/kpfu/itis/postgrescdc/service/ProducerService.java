package ru.kpfu.itis.postgrescdc.service;


import com.google.protobuf.Struct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.impl.schema.JSONSchema;
import org.springframework.stereotype.Service;
import ru.kpfu.itis.postgrescdc.ConverterUtils;
import ru.kpfu.itis.postgrescdc.model.Changes;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProducerService {
    private final Producer<ConnectorModel> connectorProducer;
    private final PulsarClient pulsarClient;

    public void sendJsonAsync(String jsonChanges, String topic) {
        Changes changes = ConverterUtils.toObject(jsonChanges);
        try (Producer<Changes> producer = pulsarClient.newProducer(JSONSchema.of(Changes.class))
                .topic(topic)
                .producerName(UUID.randomUUID().toString())
                .create()) {
            producer.sendAsync(changes).thenAccept(msgId -> log.info("Json message with ID {} successfully sent", msgId));
            producer.flush();
        } catch (PulsarClientException e) {
            throw new IllegalStateException(e);
        }
    }

    public void sendAvroAsync(String jsonChanges, String topic) {
        Changes changes = ConverterUtils.toObject(jsonChanges);
        try (Producer<Changes> producer = pulsarClient.newProducer(Schema.AVRO(Changes.class))
                .topic(topic)
                .producerName(UUID.randomUUID().toString())
                .create()) {
            producer.sendAsync(changes).thenAccept(msgId -> log.info("Avro message with ID {} successfully sent", msgId));
            producer.flush();
        } catch (PulsarClientException e) {
            throw new IllegalStateException(e);
        }
    }

    public void sendProtoAsync(String jsonChanges, String topic) {
        Struct struct = ConverterUtils.toProto(jsonChanges);
        try (Producer<byte[]> producer = pulsarClient.newProducer(Schema.BYTES)
                .topic(topic)
                .producerName(UUID.randomUUID().toString())
                .create()) {
            producer.sendAsync(struct.toByteArray()).thenAccept(msgId -> log.info("Proto message with ID {} successfully sent", msgId));
            producer.flush();
        } catch (PulsarClientException e) {
            throw new IllegalStateException(e);
        }
    }

    public void sendByteAsync(byte[] byteChanges, String topic) {
        try (Producer<byte[]> producer = pulsarClient.newProducer(Schema.BYTES)
                .topic(topic)
                .producerName(UUID.randomUUID().toString())
                .create()) {
            producer.sendAsync(byteChanges).thenAccept(msgId -> log.info("Byte message with ID {} successfully sent", msgId));
            producer.flush();
        } catch (PulsarClientException e) {
            throw new IllegalStateException(e);
        }
    }

    public void createConnector(ConnectorModel connectorModel) {
        connectorProducer.sendAsync(connectorModel).thenAccept(msgId -> log.info("Connector message with ID {} successfully sent", msgId));
    }


    public boolean createJsonProducer(String topic) {
        try {
            Producer<Changes> producer = pulsarClient.newProducer(JSONSchema.of(Changes.class))
                    .topic(topic)
                    .producerName(UUID.randomUUID().toString())
                    .create();
            return producer.isConnected();
        } catch (PulsarClientException e) {
            throw new IllegalStateException(e);
        }

    }

    public boolean createAvroProducer(String topic) {
        try {
            Producer<Changes> producer = pulsarClient.newProducer(Schema.AVRO(Changes.class))
                    .topic(topic)
                    .producerName(UUID.randomUUID().toString())
                    .create();
            return producer.isConnected();
        } catch (PulsarClientException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean createProtoProducer(String topic) {
        try {
            Producer<byte[]> producer = pulsarClient.newProducer(Schema.BYTES)
                    .topic(topic)
                    .producerName(UUID.randomUUID().toString())
                    .create();
            return producer.isConnected();
        } catch (PulsarClientException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean createByteProducer(String topic) {
        try {
            Producer<byte[]> producer = pulsarClient.newProducer(Schema.BYTES)
                    .topic(topic)
                    .producerName(UUID.randomUUID().toString())
                    .create();
            return producer.isConnected();
        } catch (PulsarClientException e) {
            throw new IllegalStateException(e);
        }
    }
}
