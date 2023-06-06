package ru.kpfu.itis.postgrescdc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kpfu.itis.postgrescdc.entity.ConnectorEntity;

import java.util.List;
import java.util.UUID;

public interface ConnectorRepository extends JpaRepository<ConnectorEntity, UUID> {

    List<ConnectorEntity> findAllByIsActiveIsTrue();
}
