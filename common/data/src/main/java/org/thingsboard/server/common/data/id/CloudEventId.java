package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class CloudEventId extends UUIDBased {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public CloudEventId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static CloudEventId fromString(String cloudEventId) {
        return new CloudEventId(UUID.fromString(cloudEventId));
    }
}