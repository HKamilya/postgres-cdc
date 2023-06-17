package ru.kpfu.itis.postgrescdc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kpfu.itis.postgrescdc.entity.ChangeEntity;

import java.util.List;
import java.util.UUID;

public interface ChangeRepository extends JpaRepository<ChangeEntity, UUID> {
    List<ChangeEntity> findAllByCdcInfoEntityIdOrderByCreateDt(UUID id);
}
