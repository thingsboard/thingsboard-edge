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
package org.thingsboard.integration.http;

import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.controller.HttpIntegrationMsg;
import org.thingsboard.integration.api.util.ConvertUtil;
import org.thingsboard.integration.api.util.ExceptionUtil;

import javax.script.ScriptException;

/**
 * Created by ashvayka on 04.12.17.
 */
@Slf4j
public abstract class AbstractHttpIntegration<T extends HttpIntegrationMsg<?>> extends AbstractIntegration<T> {

    @Override
    public void process(T msg) {
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
            handleException(msg, e);
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, getTypeUplink(msg), msg.getContentType(),
                        ConvertUtil.toDebugMessage(msg.getContentType(), msg.getMsgInBytes()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    private void handleException(T msg, Exception e) {
        HttpStatus status;
        Exception se = ExceptionUtil.lookupExceptionInCause(e, ScriptException.class, JsonParseException.class);
        if (se != null) {
            e = se;
            status = HttpStatus.BAD_REQUEST;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        msg.getCallback().setResult(context.isExceptionStackTraceEnabled() ? new ResponseEntity<>(toString(e), status) : new ResponseEntity<>(status));
    }

    protected abstract ResponseEntity doProcess(T msg) throws Exception;

    protected static ResponseEntity fromStatus(HttpStatus status) {
        return new ResponseEntity<>(status);
    }

    protected abstract String getTypeUplink(T msg);

    @Override
    public void destroy() {

    }
}
