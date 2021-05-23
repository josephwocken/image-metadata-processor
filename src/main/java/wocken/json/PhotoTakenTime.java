package wocken.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PhotoTakenTime {

    // this is in epoch seconds, add 3 zeros to get to ms
    @JsonProperty("timestamp")
    Integer timestamp;

    public Integer getTimestamp() {
        return this.timestamp;
    }
}
