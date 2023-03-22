package ru.kpfu.itis.postgrescdc.service;

import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

public interface ConnectorService {
    void createConnector(ConnectorModel model);
}
