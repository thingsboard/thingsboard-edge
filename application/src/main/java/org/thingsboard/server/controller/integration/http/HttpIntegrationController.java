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
package org.thingsboard.server.controller.integration.http;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.controller.integration.BaseIntegrationController;
import org.thingsboard.server.service.integration.ThingsboardPlatformIntegration;
import org.thingsboard.server.service.integration.http.HttpIntegrationMsg;

import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/v1/integrations/http")
@Slf4j
public class HttpIntegrationController extends BaseIntegrationController {

    private static final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("rawtypes")
    @RequestMapping(value = "/{routingKey}", method = {RequestMethod.POST})
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(
            @PathVariable("routingKey") String routingKey,
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

        if (integration.get().getConfiguration().getType() != IntegrationType.HTTP) {
            result.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            return result;
        }

        process(integration.get(), new HttpIntegrationMsg(requestHeaders, msg, result));

        return result;
    }

    @SuppressWarnings("rawtypes")
    @RequestMapping(value = "/{routingKey}", method = {RequestMethod.GET})
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> checkStatus(@PathVariable("routingKey") String routingKey,
                                                      @RequestParam Map<String, String> requestParams,
                                                      @RequestHeader Map<String, String> requestHeaders) {
        log.debug("[{}] Received status check request", routingKey);
        DeferredResult<ResponseEntity> result = new DeferredResult<>();

        Optional<ThingsboardPlatformIntegration> integration = integrationService.getIntegrationByRoutingKey(routingKey);

        if (!integration.isPresent()) {
            result.setResult(new ResponseEntity<>(HttpStatus.NOT_FOUND));
            return result;
        }

        if (integration.get().getConfiguration().getType() != IntegrationType.HTTP) {
            result.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            return result;
        }

        if (requestParams.size() > 0) {
            ObjectNode msg = mapper.createObjectNode();
            requestParams.forEach(msg::put);
            process(integration.get(), new HttpIntegrationMsg(requestHeaders, msg, result));
        } else {
            result.setResult(new ResponseEntity<>(HttpStatus.OK));
        }

        return result;
    }

}
