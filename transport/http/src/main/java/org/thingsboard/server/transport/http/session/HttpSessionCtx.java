/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.transport.http.session;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.core.*;
import org.thingsboard.server.common.msg.session.*;
import org.thingsboard.server.common.msg.session.ex.SessionException;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;

import java.util.function.Consumer;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class HttpSessionCtx extends DeviceAwareSessionContext {

    private final SessionId sessionId;
    private final long timeout;
    private final DeferredResult<ResponseEntity> responseWriter;

    public HttpSessionCtx(SessionMsgProcessor processor, DeviceAuthService authService, DeferredResult<ResponseEntity> responseWriter, long timeout) {
        super(processor, authService);
        this.sessionId = new HttpSessionId();
        this.responseWriter = responseWriter;
        this.timeout = timeout;
    }

    @Override
    public SessionType getSessionType() {
        return SessionType.SYNC;
    }

    @Override
    public void onMsg(SessionActorToAdaptorMsg source) throws SessionException {
        ToDeviceMsg msg = source.getMsg();
        switch (msg.getMsgType()) {
            case GET_ATTRIBUTES_RESPONSE:
                reply((GetAttributesResponse) msg);
                return;
            case STATUS_CODE_RESPONSE:
                reply((StatusCodeResponse) msg);
                return;
            case ATTRIBUTES_UPDATE_NOTIFICATION:
                reply((AttributesUpdateNotification) msg);
                return;
            case TO_DEVICE_RPC_REQUEST:
                reply((ToDeviceRpcRequestMsg) msg);
                return;
            case TO_SERVER_RPC_RESPONSE:
                reply((ToServerRpcResponseMsg) msg);
                return;
            case RULE_ENGINE_ERROR:
                reply((RuleEngineErrorMsg) msg);
                return;
        }
    }

    private void reply(RuleEngineErrorMsg msg) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        switch (msg.getError()) {
            case PLUGIN_TIMEOUT:
                status = HttpStatus.REQUEST_TIMEOUT;
                break;
            default:
                if (msg.getInMsgType() == MsgType.TO_SERVER_RPC_REQUEST) {
                    status = HttpStatus.BAD_REQUEST;
                }
                break;
        }
        responseWriter.setResult(new ResponseEntity<>(JsonConverter.toErrorJson(msg.getErrorMsg()).toString(), status));
    }

    private <T> void reply(ResponseMsg<? extends T> msg, Consumer<T> f) {
        if (!msg.getError().isPresent()) {
            f.accept(msg.getData().get());
        } else {
            Exception e = msg.getError().get();
            responseWriter.setResult(new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    private void reply(ToDeviceRpcRequestMsg msg) {
        responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg, true).toString(), HttpStatus.OK));
    }

    private void reply(ToServerRpcResponseMsg msg) {
        responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
    }

    private void reply(AttributesUpdateNotification msg) {
        responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg.getData(), false).toString(), HttpStatus.OK));
    }

    private void reply(GetAttributesResponse msg) {
        reply(msg, payload -> {
            if (payload.getClientAttributes().isEmpty() && payload.getSharedAttributes().isEmpty()) {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_FOUND));
            } else {
                JsonObject result = JsonConverter.toJson(payload, false);
                responseWriter.setResult(new ResponseEntity<>(result.toString(), HttpStatus.OK));
            }
        });
    }

    private void reply(StatusCodeResponse msg) {
        reply(msg, payload -> {
            if (payload == 0) {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
            } else {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.valueOf(payload)));
            }
        });
    }

    @Override
    public void onMsg(SessionCtrlMsg msg) throws SessionException {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    @Override
    public SessionId getSessionId() {
        return sessionId;
    }
}
