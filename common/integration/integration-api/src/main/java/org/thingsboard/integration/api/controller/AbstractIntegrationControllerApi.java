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
package org.thingsboard.integration.api.controller;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.integration.api.IntegrationControllerApi;
import org.thingsboard.integration.api.IntegrationHttpMsgProcessor;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.concurrent.Executor;

@Slf4j
public abstract class AbstractIntegrationControllerApi implements IntegrationControllerApi {

    @Override
    public <T> void process(IntegrationType type, String routingKey, DeferredResult<ResponseEntity> result, T msg) {
        process(type, routingKey, result, msg, (integration, tmpResult, tmpMsg) -> integration.process(tmpMsg));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T> void process(IntegrationType type, String routingKey, DeferredResult<ResponseEntity> result, T msg, IntegrationHttpMsgProcessor<T> processor) {
        ListenableFuture<ThingsboardPlatformIntegration> integrationFuture = getIntegrationByRoutingKey(routingKey);

        DonAsynchron.withCallback(integrationFuture, integration -> {
            var theIntegration = (ThingsboardPlatformIntegration<T>)  integration;
            if (checkIntegrationPlatform(result, theIntegration, type)) {
                return;
            }
            processor.process(theIntegration, result, msg);
        }, failure -> {
            log.trace("[{}] Failed to fetch integration by routing key", routingKey, failure);
            result.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }, getCallbackExecutor());
    }

    @SuppressWarnings({"rawtypes"})
    private static boolean checkIntegrationPlatform(DeferredResult<ResponseEntity> result, ThingsboardPlatformIntegration integration, IntegrationType type) {
        if (integration == null) {
            result.setResult(new ResponseEntity<>(HttpStatus.NOT_FOUND));
            return true;
        }
        if (integration.getConfiguration().getType() != type) {
            result.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            return true;
        }
        return false;
    }

    protected abstract Executor getCallbackExecutor();

    protected abstract ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String routingKey);

}
