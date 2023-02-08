package ru.kpfu.itis.postgrescdc.rest;

import org.springframework.http.ResponseEntity;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

import java.util.List;

public interface ConnectorRest {

    List<String> getConnectors();

    ResponseEntity<Object> addConnectors(ConnectorModel model);
}
