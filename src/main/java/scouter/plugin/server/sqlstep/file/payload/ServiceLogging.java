package scouter.plugin.server.sqlstep.file.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import scouter.util.StringUtil;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceLogging implements ILogging {

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

    SQLStepWrapper history;

    @JsonIgnore
    @Override
    public String toJSONString(ObjectMapper objectMapper) throws IOException {
        return objectMapper.writeValueAsString(this);
    }

    private String quite(String v){
        return "\""+v+"\"";
    }

    @JsonIgnore
    @Override
    public String toCSVString() {
        String first= Stream.of(this.quite(name),
                                this.quite(url),
                                this.quite(requestTime),
                                this.quite(String.valueOf(elapsed)),
                                this.quite(txid),
                                this.quite(StringUtil.emptyToDefault(gxid,"-")),
                                this.quite(StringUtil.emptyToDefault(error,"-")),
                                this.quite(String.valueOf(sqlCallCount)),
                                this.quite(String.valueOf(apiCallCount)),
                                this.quite(String.valueOf(sqlCallTime)),
                                this.quite(String.valueOf(apiCallTime)))
                    .collect(Collectors.joining(","));
        String head = Stream.of(this.quite("-"),
                this.quite("-"),
                this.quite("-"),
                this.quite("-"),
                this.quite("-"),
                this.quite("-"),
                this.quite("-"),
                this.quite("-"),
                this.quite("-"),
                this.quite("-"),
                this.quite("-"))
                .collect(Collectors.joining(","));

        int index = 0;

        StringBuffer csv = new StringBuffer();
        for(SQLStep step : history.steps ){
            String values = Stream.of(this.quite(step.type),
                    this.quite(StringUtil.emptyToDefault(step.open,"")),
                    this.quite(StringUtil.emptyToDefault(step.sql,"")),
                    this.quite(StringUtil.emptyToDefault(step.param,"")),
                    this.quite(StringUtil.emptyToDefault(step.tables,"")),
                    this.quite(StringUtil.emptyToDefault(step.sqlError,"")),
                    this.quite(StringUtil.emptyToDefault(step.apError,"")),
                    this.quite(String.valueOf(elapsed)))
                    .collect(Collectors.joining(","));
            if(index == 0 ){
                csv.append(String.join(",",first,values))
                   .append("\r\n");

            }else{
                csv.append(String.join(",",head,values))
                    .append("\r\n");
            }
            index++;
        }
        return csv.toString();


    }

    @Override
    public Set<String> toCSVHead() {
        return Stream.of(this.quite("name"),
                this.quite("url"),
                this.quite("requestTime"),
                this.quite("elapsed"),
                this.quite("txid"),
                this.quite("gxid"),
                this.quite("error"),
                this.quite("sqlCallCount"),
                this.quite("apiCallCount"),
                this.quite("sqlCallTime"),
                this.quite("apiCallTime"),
                this.quite("stepType"),
                this.quite("stepOpen"),
                this.quite("stepSql"),
                this.quite("stepParam"),
                this.quite("stepTables"),
                this.quite("stepSqlError"),
                this.quite("stepApError")
        )
        .collect(Collectors.toCollection( LinkedHashSet::new ));

    }
}

