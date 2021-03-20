package scouter.plugin.server.sqlstep.file.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.Set;

@Builder
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JDBCLogging implements ILogging {

    String name;
    String url;
    String requestTime;
    String txid;
    String gxid;
    String error;

    SQLStep history;

    @JsonIgnore
    @Override
    public String toJSONString(ObjectMapper objectMapper) throws IOException {
        return objectMapper.writeValueAsString(this);
    }

    @JsonIgnore
    @Override
    public String toCSVString() {
        return null;
    }

    @Override
    public Set<String> toCSVHead() {
        return null;
    }
}
