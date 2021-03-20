package scouter.plugin.server.sqlstep.file.payload;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class SQLStep {

    public String type;
    public String open;
    public String sql;
    public String param;
    public String tables;
    public String sqlError;
    public String apError;
    public long elapsed;
}
