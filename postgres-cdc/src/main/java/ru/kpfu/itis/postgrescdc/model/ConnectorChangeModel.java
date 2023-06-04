package ru.kpfu.itis.postgrescdc.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
public class ConnectorChangeModel {
    private UUID id;
    @NotNull
    private String host;
    private String port;
    @NotNull
    private String database;
    @NotNull
    private String user;
    @NotNull
    private String password;
    private String topicName;
    private boolean saveChanges;

}
