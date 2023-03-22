package ru.kpfu.itis.postgrescdc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;

import java.util.UUID;

public interface ConnectorRepository extends JpaRepository<ConnectorEntity, UUID> {
}
