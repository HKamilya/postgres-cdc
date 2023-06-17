package ru.kpfu.itis.postgrescdc.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
public class ConnectorModel {
    private UUID id;
    @NotNull
    private DataTypeEnum dataType;
    @NotNull
    private String host;
    private String port;
    @NotNull
    private String database;
    @NotNull
    private String user;
    @NotNull
    private String password;
    private boolean fromBegin;
    private boolean forAllTables;
    private String tables;
    private String slotName;
    private String publicationName;
    private String topicName;
    private boolean saveChanges;

}
