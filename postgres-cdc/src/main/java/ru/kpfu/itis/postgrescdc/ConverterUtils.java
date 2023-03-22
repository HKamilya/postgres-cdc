package ru.kpfu.itis.postgrescdc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.kpfu.itis.postgrescdc.model.Changes;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConverterUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Changes toObject(String jsonString) {
        Changes changes;
        try {
            changes = mapper.readValue(jsonString, Changes.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
        return changes;
    }

    public static Struct toProto(String changes) {
        Struct.Builder structBuilder = Struct.newBuilder();
        Struct build = null;
        try {
            JsonFormat.parser().merge(changes, structBuilder);
            build = structBuilder.build();
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException(e);
        }
        return build;
    }
}
