package ru.kpfu.itis.postgrescdc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CdcInfoRepository extends JpaRepository<CdcInfoRepository, UUID> {
}
