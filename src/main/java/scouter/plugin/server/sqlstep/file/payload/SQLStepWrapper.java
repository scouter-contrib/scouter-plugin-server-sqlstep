package scouter.plugin.server.sqlstep.file.payload;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
@Builder
@Getter
@ToString
public class SQLStepWrapper {
    List<SQLStep> steps;
    String type;
}
