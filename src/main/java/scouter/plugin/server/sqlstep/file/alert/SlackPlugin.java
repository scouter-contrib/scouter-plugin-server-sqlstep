package scouter.plugin.server.sqlstep.file.alert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import scouter.plugin.server.sqlstep.file.payload.ServiceLogging;
import scouter.server.Configure;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

import java.io.IOException;
import java.util.Objects;

@Slf4j
public class SlackPlugin {

    private final ObjectMapper objectMapper;
    private final String botName;
    private final String channel;
    private final String[] exclude;
    Configure conf = Configure.getInstance();
    private final String webhook;
    private final boolean enabled;
    private static final String ext_plugin_sqlstep_alert_slack   = "ext_plugin_sqlstep_alert_slack";
    private static final String ext_plugin_sqlstep_slack_webhook = "ext_plugin_sqlstep_slack_webhook";
    private static final String ext_plugin_sqlstep_slack_botname = "ext_plugin_sqlstep_slack_botname";
    private static final String ext_plugin_sqlstep_slack_channel = "ext_plugin_sqlstep_slack_channel";
    private static final String ext_plugin_sqlstep_slack_agent_exclude_pattern = "ext_plugin_sqlstep_slack_agent_exclude_pattern";


    public SlackPlugin(){
        this.enabled = conf.getBoolean(ext_plugin_sqlstep_alert_slack, true);
        this.webhook = conf.getValue(ext_plugin_sqlstep_slack_webhook, "");
        this.botName = conf.getValue(ext_plugin_sqlstep_slack_botname, "scouter-sqlstep-alert");
        this.channel = conf.getValue(ext_plugin_sqlstep_slack_channel, "");
        this.exclude = StringUtil.split(conf.getValue(ext_plugin_sqlstep_slack_agent_exclude_pattern, ""),",");
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public void execute(ServiceLogging serviceLogging){
        if(!enabled || StringUtil.isEmpty(serviceLogging.getError())){
            return;
        }

        for(String name  : exclude){
            if(serviceLogging.getName().indexOf(name) > -1){
                return;
            }
        }

        final String alert= String.join("\n",

                        "Trigger Time: "+DateUtil.datetime(System.currentTimeMillis()),
                                       "- "+serviceLogging.getName(),
                                       "- "+serviceLogging.getUrl(),
                                       "- " +serviceLogging.getError());

        try {

            final String payload = objectMapper.writeValueAsString(SlackWebHook.builder()
                    .botName(botName)
                    .text(alert)
                    .channel(channel)
                    .build());

            HttpPost post = new HttpPost(webhook);
            post.addHeader("Content-Type", "application/json");
            // charset set utf-8
            post.setEntity(new StringEntity(payload, "utf-8"));
            CloseableHttpClient client = HttpClientBuilder.create().build();
            // send the post request
            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                log.info("Slack message sent to [" + channel + "] successfully.");
            } else {
                log.info("Slack message sent failed. Verify below information.");
                log.info("[WebHookURL] : " + webhook);
                log.info("[Message] : " + payload);
                log.info("[Reason] : " + EntityUtils.toString(response.getEntity(), "UTF-8"));
            }
        }catch (JsonProcessingException e){
            log.error("json processing error: {} ",e);
        } catch (ClientProtocolException e) {
            log.error("client processing error: {} ", e);
        } catch (IOException e) {
            log.error("io error: {} ", e);
        }


    }

}
