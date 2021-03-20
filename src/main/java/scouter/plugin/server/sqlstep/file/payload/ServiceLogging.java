package scouter.plugin.server.sqlstep.file.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import scouter.util.StringUtil;

import java.io.IOException;
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

    @JsonIgnore
    @Override
    public String toCSVString() {


        String first= Stream.of(name,url,requestTime,elapsed,txid,gxid,error,sqlCallCount,apiCallCount,sqlCallTime)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        String head = Stream.of("-","-","-","-","-","-","-","-","-","-")
                            .collect(Collectors.joining(","));

        int index = 0;

        StringBuffer csv = new StringBuffer();
        for(SQLStep step : history.steps ){
            String values = Stream.of(step.type,
                      step.open,
                      step.sql,
                      step.param,
                      step.tables,
                      StringUtil.emptyToDefault(step.sqlError,"-"),
                      StringUtil.emptyToDefault(step.apError,""),
                      String.valueOf(elapsed)).collect(Collectors.joining(","));
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
}

