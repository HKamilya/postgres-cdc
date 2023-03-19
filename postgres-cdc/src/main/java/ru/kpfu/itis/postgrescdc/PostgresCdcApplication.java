package ru.kpfu.itis.postgrescdc;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableJpaRepositories(basePackages = "ru.kpfu.itis.postgrescdc.repository")
@EntityScan("ru.kpfu.itis.postgrescdc.entity")
public class PostgresCdcApplication implements CommandLineRunner {

//    @Autowired
//    private PulsarClient pulsarClient;
//    @Autowired
//    private MessageListener messageListener;

    public static void main(String[] args) {
        SpringApplication.run(PostgresCdcApplication.class, args);
    }

    @Override
    public void run(String... args) {
        checkConnectors();
    }

    public void checkConnectors() {
//        try {
//            pulsarClient.newConsumer(Schema.JSON(ConnectorModel.class))
//                    .topic("connectors")
//                    .subscriptionName(UUID.randomUUID().toString())
//                    .messageListener(messageListener)
//                    .subscriptionType(SubscriptionType.Exclusive)
//                    .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
//                    .subscribe();
//        } catch (PulsarClientException e) {
//            throw new IllegalStateException(e);
//        }
    }
}
