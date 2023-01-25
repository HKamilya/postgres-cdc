package ru.kpfu.itis.postgrescdc.rest;

import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

import java.util.List;

public interface ConnectorRest {

    List<String> getConnectors();

    void addConnectors(ConnectorModel model);
}
