package scouter.plugin.server.sqlstep.file.payload;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class JDBCHistory {

    String open;
    String sql;
    String param;
    String tables;
    String sqlError;
    String apError;
    long elapsed;
}
