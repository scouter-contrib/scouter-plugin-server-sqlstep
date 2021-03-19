package scouter.plugin.server.sqlstep.file.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceLogging {

    String name;
    String url;
    String requestTime;
    long elapsed;
    String txid;
    String gxid;
    String error;
    long sqlCallCount;
    long apiCallCount;
    long apiCallTime;
    long sqlCallTime;

    List<JDBCHistory> histories;
}
