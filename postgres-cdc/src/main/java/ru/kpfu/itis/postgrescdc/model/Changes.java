package ru.kpfu.itis.postgrescdc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class Changes {
    private List<Change> change;

    @Getter
    @Setter
    public static class Change {
        private String kind;
        private String schema;
        private String table;
        @JsonProperty("columnnames")
        private List<String> columnNames;
        @JsonProperty("columntypes")
        private List<String> columnTypes;
        @JsonProperty("columnvalues")
        private List<Object> columnValues;
    }
}

