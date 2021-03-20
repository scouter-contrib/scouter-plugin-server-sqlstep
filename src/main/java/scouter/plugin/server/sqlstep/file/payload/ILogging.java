package scouter.plugin.server.sqlstep.file.payload;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;

public interface ILogging {
    String toJSONString(ObjectMapper objectMapper) throws IOException;
    String toCSVString();
    Set<String> toCSVHead();
}
