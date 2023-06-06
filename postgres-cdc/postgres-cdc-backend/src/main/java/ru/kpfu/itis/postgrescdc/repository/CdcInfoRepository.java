package ru.kpfu.itis.postgrescdc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kpfu.itis.postgrescdc.entity.CdcInfoEntity;

import java.util.UUID;

public interface CdcInfoRepository extends JpaRepository<CdcInfoEntity, UUID> {
}
