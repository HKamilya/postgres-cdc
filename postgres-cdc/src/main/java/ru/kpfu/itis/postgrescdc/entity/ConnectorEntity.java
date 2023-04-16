package ru.kpfu.itis.postgrescdc.entity;

import lombok.Getter;
import lombok.Setter;
import ru.kpfu.itis.postgrescdc.model.DataTypeEnum;
import ru.kpfu.itis.postgrescdc.model.PluginEnum;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "cdc", name = "connector")
@Getter
@Setter
public class ConnectorEntity {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "data_type")
    private DataTypeEnum dataType;
    @Column(name = "plugin")
    private PluginEnum plugin;
    @Column(name = "host")
    private String host;
    @Column(name = "port")
    private String port;
    @Column(name = "database")
    private String database;
    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
    @Column(name = "from_begin")
    private Boolean fromBegin;
    @Column(name = "for_all_tables")
    private Boolean forAllTables;
    @Column(name = "schema")
    private String schema;
    @Column(name = "tables")
    private String tables;
    @Column(name = "topic_name")
    private String topicName;
    @Column(name = "create_dt")
    private LocalDateTime createDt;
    @Column(name = "change_dt")
    private LocalDateTime changeDt;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cdc_info_id")
    private CdcInfoEntity cdcInfoEntity;
    @Column(name = "is_active")
    private Boolean isActive;

}
