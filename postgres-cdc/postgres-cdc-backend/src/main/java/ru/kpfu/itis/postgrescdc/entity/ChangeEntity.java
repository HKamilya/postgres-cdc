package ru.kpfu.itis.postgrescdc.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "cdc", name = "change")
@Getter
@Setter
public class ChangeEntity {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "lsn")
    private String lsn;
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;
    @Column(name = "create_dt")
    private LocalDateTime createDt;
    @Column(name = "change_dt")
    private LocalDateTime changeDt;
    @Column(name = "cdc_info_entity")
    private UUID cdcInfoEntityId;
}
