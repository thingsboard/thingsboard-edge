package org.thingsboard.server.service.integration.thingpark;

import lombok.Builder;
import lombok.Data;

/**
 * Created by ashvayka on 18.12.17.
 */
@Data
@Builder
public class ThingParkRequestParameters {

    private String asId;
    private String lrnDevEui;
    private String lrnFPort;
    private String lrnInfos;
    private String time;
    private String token;

}
