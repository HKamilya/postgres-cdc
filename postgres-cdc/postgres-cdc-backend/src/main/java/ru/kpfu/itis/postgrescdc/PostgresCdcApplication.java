package ru.kpfu.itis.postgrescdc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;
import ru.kpfu.itis.postgrescdc.repository.ConnectorRepository;
import ru.kpfu.itis.postgrescdc.service.replication.Wal2JsonReplicationService;

import java.util.List;

@EnableAsync
@SpringBootApplication
@EnableJpaRepositories(basePackages = "ru.kpfu.itis.postgrescdc.repository")
@EntityScan("ru.kpfu.itis.postgrescdc.entity")
public class PostgresCdcApplication implements CommandLineRunner {

    @Autowired
    private Wal2JsonReplicationService wal2JsonReplicationService;
    @Autowired
    private ConnectorRepository connectorRepository;

    public static void main(String[] args) {
        SpringApplication.run(PostgresCdcApplication.class, args);
    }

    @Override
    public void run(String... args) {
        List<ConnectorEntity> allByIsActiveIsTrue = connectorRepository.findAllByIsActiveIsTrue();
        for (ConnectorEntity connectorEntity : allByIsActiveIsTrue) {
            wal2JsonReplicationService.connectToExistingSlot(connectorEntity);
        }
    }
}
