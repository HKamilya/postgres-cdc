package ru.kpfu.itis.postgrescdc.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "cdc", name = "cdc_info")
@Getter
@Setter
public class CdcInfoEntity {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "publication_name")
    private String publicationName;
    @Column(name = "slot_name")
    private String slotName;
    @Column(name = "last_applied_change_id")
    private UUID lastAppliedChangeId;
    @Column(name = "create_dt")
    private LocalDateTime createDt;
    @Column(name = "change_dt")
    private LocalDateTime changeDt;
}
