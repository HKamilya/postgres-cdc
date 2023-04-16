package ru.kpfu.itis.postgrescdc.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
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
    @ManyToOne
    @JoinColumn(name = "last_applied_change_id")
    private ChangeEntity lastAppliedChange;
    @Column(name = "create_dt")
    private LocalDateTime createDt;
    @Column(name = "change_dt")
    private LocalDateTime changeDt;
}
