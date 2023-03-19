package ru.kpfu.itis.postgrescdc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.springframework.stereotype.Component;
import ru.kpfu.itis.postgrescdc.model.Changes;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChangesListener implements org.apache.pulsar.client.api.MessageListener<Changes> {

    @Override
    public void received(Consumer<Changes> consumer, Message<Changes> msg) {
        try {
            log.info("Topic Name: {}", msg.getTopicName());
            log.info("Message Id: {}", msg.getMessageId());
            log.info("Producer Name: {}", msg.getProducerName());
            log.info("Publish Time: {}", msg.getPublishTime());

            log.info("Message received: {}", new String(msg.getData()));
            log.info("####################################################################################");
            consumer.acknowledge(msg);
        } catch (Exception e) {
            consumer.negativeAcknowledge(msg);
        }
    }
}
