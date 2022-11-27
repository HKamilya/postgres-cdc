package ru.kpfu.itis.postgrescdc.rest;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

@Path("/connectors")
public interface ConnectorRest {

    @Path("")
    @GET
    List<String> getConnectors();


    @Path("")
    @POST
    List<String> addConnectors();
}
