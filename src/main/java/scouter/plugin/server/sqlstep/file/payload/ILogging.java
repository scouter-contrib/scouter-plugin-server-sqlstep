package scouter.plugin.server.sqlstep.file.payload;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public interface ILogging {
    String toJSONString(ObjectMapper objectMapper) throws IOException;
    String toCSVString();
}
