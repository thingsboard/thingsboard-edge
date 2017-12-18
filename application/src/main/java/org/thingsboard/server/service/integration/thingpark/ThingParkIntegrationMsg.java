package org.thingsboard.server.service.integration.thingpark;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.service.integration.http.HttpIntegrationMsg;

/**
 * Created by ashvayka on 18.12.17.
 */
@Data
public class ThingParkIntegrationMsg extends HttpIntegrationMsg {

    private final ThingParkRequestParameters params;

    public ThingParkIntegrationMsg(JsonNode msg, ThingParkRequestParameters params, DeferredResult<ResponseEntity> callback) {
        super(msg, callback);
        this.params = params;
    }

}
