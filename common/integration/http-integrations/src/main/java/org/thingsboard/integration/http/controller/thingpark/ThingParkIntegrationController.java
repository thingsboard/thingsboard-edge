/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.integration.http.controller.thingpark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiOperation;
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
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
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

    @ApiOperation(value = "Process request from ThingPark integrations", hidden = true)
    @SuppressWarnings("rawtypes")
    @RequestMapping("/thingpark/{routingKey}")
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(
            @PathVariable("routingKey") String routingKey,
            @RequestParam Map<String, String> allRequestParams,
            @RequestBody JsonNode msg,
            @RequestHeader Map<String, String> requestHeaders,
            HttpServletRequest request) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        return getResult(allRequestParams, IntegrationType.THINGPARK, requestHeaders, routingKey, msg);
    }

    @ApiOperation(value = "Process request from ThingPark integrations", hidden = true)
    @SuppressWarnings("rawtypes")
    @RequestMapping("/tpe/{routingKey}")
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequestTPE(
            @PathVariable("routingKey") String routingKey,
            @RequestParam Map<String, String> allRequestParams,
            @RequestBody JsonNode msg,
            @RequestHeader Map<String, String> requestHeaders,
            HttpServletRequest request) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        return getResult(allRequestParams, IntegrationType.TPE, requestHeaders, routingKey, msg);
    }

    @SuppressWarnings("unchecked")
    private DeferredResult<ResponseEntity> getResult(Map<String, String> allRequestParams, IntegrationType typeIntegration,
                                                     Map<String, String> requestHeaders, String routingKey, JsonNode msg) {
        DeferredResult<ResponseEntity> result = new DeferredResult<>();

        JsonNode jsonNode = new ObjectMapper().convertValue(allRequestParams, JsonNode.class);
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

        api.process(typeIntegration, routingKey, result, new ThingParkIntegrationMsg(requestHeaders, msg, params, result));

        return result;
    }
}
