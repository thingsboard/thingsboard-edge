/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.integration.http.controller.loriot;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.controller.BaseIntegrationController;
import org.thingsboard.integration.api.controller.HttpIntegrationMsg;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations/loriot")
@Slf4j
public class LoriotIntegrationController extends BaseIntegrationController {

    @SuppressWarnings("rawtypes")
    @RequestMapping(value = "/{routingKey}")
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> processRequest(
            @PathVariable("routingKey") String routingKey,
            @RequestBody JsonNode msg,
            @RequestHeader Map<String, String> requestHeaders
    ) {
        log.debug("[{}] Received request: {}", routingKey, msg);
        DeferredResult<ResponseEntity> result = new DeferredResult<>();

        ListenableFuture<ThingsboardPlatformIntegration> integrationFuture = api.getIntegrationByRoutingKey(routingKey);

        DonAsynchron.withCallback(integrationFuture, integration -> {
            if (checkIntegrationPlatform(result, integration, IntegrationType.LORIOT)) {
                return;
            }
            api.process(integration, new HttpIntegrationMsg(requestHeaders, msg, result));
        }, failure -> {
            log.trace("[{}] Failed to fetch integration by routing key", routingKey, failure);
            result.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }, api.getCallbackExecutor());

        return result;
    }
}
