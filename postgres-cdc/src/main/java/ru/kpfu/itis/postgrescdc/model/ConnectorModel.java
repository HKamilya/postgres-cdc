package ru.kpfu.itis.postgrescdc.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ConnectorModel {
    @NotNull
    private PluginEnum plugin;
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

}
