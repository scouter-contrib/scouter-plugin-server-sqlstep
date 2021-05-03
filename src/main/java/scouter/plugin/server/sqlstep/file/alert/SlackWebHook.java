package scouter.plugin.server.sqlstep.file.alert;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SlackWebHook {
    @JsonProperty("text")
    private String text;
    @JsonProperty("channel")
    private String channel;
    @JsonProperty("username")
    private String botName;
    @JsonProperty("icon_emoji")
    private String iconEmoji;
    @JsonProperty("icon_url")
    private String iconURL;
}
