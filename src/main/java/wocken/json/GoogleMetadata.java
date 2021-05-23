package wocken.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleMetadata {
    @JsonProperty("photoTakenTime")
    PhotoTakenTime photoTakenTime;

    public PhotoTakenTime getPhotoTakenTime() {
        return this.photoTakenTime;
    }
}
