/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.http.controller.thingpark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.controller.BaseIntegrationController;
import org.thingsboard.integration.http.thingpark.ThingParkIntegrationMsg;
import org.thingsboard.integration.http.thingpark.ThingParkRequestParameters;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.server.common.data.integration.IntegrationType;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations")
@Slf4j
public class ThingParkIntegrationController extends BaseIntegrationController {

    @SuppressWarnings("rawtypes")
    @RequestMapping("/thingpark/{routingKey}")
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(
            @PathVariable("routingKey") String routingKey,
            @RequestParam Map<String,String> allRequestParams,
            @RequestBody JsonNode msg,
            @RequestHeader Map<String, String> requestHeaders,
            HttpServletRequest request) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        return getResult (allRequestParams, IntegrationType.THINGPARK, request, requestHeaders, routingKey, msg);
    }

    @SuppressWarnings("rawtypes")
    @RequestMapping( "/tpe/{routingKey}")
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequestTPE(
            @PathVariable("routingKey") String routingKey,
            @RequestParam Map<String,String> allRequestParams,
            @RequestBody JsonNode msg,
            @RequestHeader Map<String, String> requestHeaders,
            HttpServletRequest request) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        return getResult (allRequestParams, IntegrationType.TPE, request, requestHeaders, routingKey, msg);
    }

    private DeferredResult<ResponseEntity> getResult(Map<String, String> allRequestParams, IntegrationType typeIntegration,
                                                     HttpServletRequest request, Map<String, String>  requestHeaders, String routingKey, JsonNode msg) {
        final ObjectMapper mapper = new ObjectMapper();
        DeferredResult<ResponseEntity> result = new DeferredResult<>();
        ListenableFuture<ThingsboardPlatformIntegration> integrationFuture = api.getIntegrationByRoutingKey(routingKey);
        DonAsynchron.withCallback(integrationFuture, integration -> {
            if (checkIntegrationPlatform(result, integration, typeIntegration)) {
                return;
            }
            JsonNode jsonNode = mapper.convertValue(allRequestParams, JsonNode.class);
            String asId = jsonNode.has("AS_ID") ? jsonNode.get("AS_ID").asText() : "false";
            String lrnDevEui = jsonNode.has("LrnDevEui") ? jsonNode.get("LrnDevEui").asText() : "false";
            String lrnFPort = jsonNode.has("LrnFPort") ? jsonNode.get("LrnFPort").asText() : "false";
            String lrnInfos = jsonNode.has("LrnInfos") ? jsonNode.get("LrnInfos").asText() : "false";
            String time = jsonNode.has("Time") ? jsonNode.get("Time").asText() : "false";
            String token = jsonNode.has("Token") ? jsonNode.get("Token").asText() : "false";
            ThingParkRequestParameters params = ThingParkRequestParameters.builder()
                    .asId(asId)
                    .lrnDevEui(lrnDevEui)
                    .lrnFPort(lrnFPort)
                    .lrnInfos(lrnInfos)
                    .time(time)
                    .token(token)
                    .build();

            api.process(integration, new ThingParkIntegrationMsg(requestHeaders, msg, params, result));
        }, failure -> {
            log.trace("[{}] Failed to fetch integration by routing key", routingKey, failure);
            result.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }, api.getCallbackExecutor());
        return result;
    }
}
