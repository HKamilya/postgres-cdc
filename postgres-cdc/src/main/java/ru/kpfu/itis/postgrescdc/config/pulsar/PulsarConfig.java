package ru.kpfu.itis.postgrescdc.config.pulsar;

import lombok.SneakyThrows;
import org.apache.pulsar.client.api.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.kpfu.itis.postgrescdc.config.properties.ProducerProperties;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

@EnableConfigurationProperties(ProducerProperties.class)
@Configuration
public class PulsarConfig {

    private final ProducerProperties producerProperties;

    public PulsarConfig(ProducerProperties producerProperties) {
        this.producerProperties = producerProperties;
    }

    @SneakyThrows(PulsarClientException.class)
    @Bean
    public PulsarClient buildClient() {
        return PulsarClient.builder()
                .serviceUrl(producerProperties.getUrl())
                .build();
    }

    @Bean
    public Producer<ConnectorModel> connectorProducer(PulsarClient client) throws PulsarClientException {
        return client.newProducer(Schema.JSON(ConnectorModel.class))
                .topic("connector")
                .producerName("connectors-producer")
                .create();
    }

    @Bean
    public Consumer<ConnectorModel> connectorsConsumer(PulsarClient client, MessageListener<ConnectorModel> notificationMessageListener) throws PulsarClientException {
        return client.newConsumer(Schema.JSON(ConnectorModel.class))
                .topic("connector")
                .subscriptionName("connectors-subscription")
                .messageListener(notificationMessageListener)
                .subscribe();
    }

}
