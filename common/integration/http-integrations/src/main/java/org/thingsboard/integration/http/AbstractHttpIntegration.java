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
package org.thingsboard.integration.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.controller.HttpIntegrationMsg;

/**
 * Created by ashvayka on 04.12.17.
 */
@Slf4j
public abstract class AbstractHttpIntegration<T extends HttpIntegrationMsg> extends AbstractIntegration<T> {

    @Override
    public void process(T msg) {
        if (!this.configuration.isEnabled()) {
            msg.getCallback().setResult(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Integration is disabled"));
            return;
        }
        String status = "OK";
        Exception exception = null;
        try {
            ResponseEntity httpResponse = doProcess(msg);
            if (!httpResponse.getStatusCode().is2xxSuccessful()) {
                status = httpResponse.getStatusCode().name();
            }
            try {
                msg.getCallback().setResult(httpResponse);
            } catch (Exception e) {
                log.error("Failed to send response from integration to original HTTP request", e);
            }
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            exception = e;
            status = "ERROR";
            msg.getCallback().setResult(new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR));
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context,  getTypeUplink(msg) , getUplinkContentType(), mapper.writeValueAsString(msg.getMsg()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    protected abstract ResponseEntity doProcess(T msg) throws Exception;

    protected static ResponseEntity fromStatus(HttpStatus status) {
        return new ResponseEntity<>(status);
    }
    protected abstract String getTypeUplink(T msg) ;

}
