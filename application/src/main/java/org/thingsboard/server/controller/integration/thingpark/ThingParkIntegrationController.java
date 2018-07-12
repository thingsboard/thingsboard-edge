/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.controller.integration.thingpark;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.controller.integration.BaseIntegrationController;
import org.thingsboard.server.service.integration.ThingsboardPlatformIntegration;
import org.thingsboard.server.service.integration.http.thingpark.ThingParkIntegrationMsg;
import org.thingsboard.server.service.integration.http.thingpark.ThingParkRequestParameters;

import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/v1/integrations/thingpark")
@Slf4j
public class ThingParkIntegrationController extends BaseIntegrationController {

    @SuppressWarnings("rawtypes")
    @RequestMapping(value = "/{routingKey}")
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(
            @PathVariable("routingKey") String routingKey,
            @RequestParam(value = "AS_ID", required = false) String asId,
            @RequestParam(value = "LrnDevEui") String lrnDevEui,
            @RequestParam(value = "LrnFPort") String lrnFPort,
            @RequestParam(value = "LrnInfos", required = false) String lrnInfos,
            @RequestParam(value = "Time", required = false) String time,
            @RequestParam(value = "Token", required = false) String token,
            @RequestBody JsonNode msg,
            @RequestHeader Map<String, String> requestHeaders
    ) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        DeferredResult<ResponseEntity> result = new DeferredResult<>();

        Optional<ThingsboardPlatformIntegration> integration = integrationService.getIntegrationByRoutingKey(routingKey);

        if (!integration.isPresent()) {
            result.setResult(new ResponseEntity<>(HttpStatus.NOT_FOUND));
            return result;
        }

        if (integration.get().getConfiguration().getType() != IntegrationType.THINGPARK) {
            result.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            return result;
        }

        ThingParkRequestParameters params = ThingParkRequestParameters.builder()
                .asId(asId)
                .lrnDevEui(lrnDevEui)
                .lrnFPort(lrnFPort)
                .lrnInfos(lrnInfos)
                .time(time)
                .token(token)
                .build();

        process(integration.get(), new ThingParkIntegrationMsg(requestHeaders, msg, params, result));

        return result;
    }

}
