package ru.kpfu.itis.postgrescdc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kpfu.itis.postgrescdc.entity.ChangeEntity;

import java.util.UUID;

public interface ChangeRepository extends JpaRepository<ChangeEntity, UUID> {
}
