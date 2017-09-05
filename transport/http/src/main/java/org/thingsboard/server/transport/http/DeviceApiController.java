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
package org.thingsboard.server.transport.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.core.*;
import org.thingsboard.server.common.msg.session.AdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicAdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicToDeviceActorSessionMsg;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.transport.http.session.HttpSessionCtx;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrew Shvayka
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
public class DeviceApiController {

    @Value("${http.request_timeout}")
    private long defaultTimeout;

    @Autowired(required = false)
    private SessionMsgProcessor processor;

    @Autowired(required = false)
    private DeviceAuthService authService;

    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> getDeviceAttributes(@PathVariable("deviceToken") String deviceToken,
                                                              @RequestParam(value = "clientKeys", required = false, defaultValue = "") String clientKeys,
                                                              @RequestParam(value = "sharedKeys", required = false, defaultValue = "") String sharedKeys) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter);
        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
            GetAttributesRequest request;
            if (StringUtils.isEmpty(clientKeys) && StringUtils.isEmpty(sharedKeys)) {
                request = new BasicGetAttributesRequest(0);
            } else {
                Set<String> clientKeySet = !StringUtils.isEmpty(clientKeys) ? new HashSet<>(Arrays.asList(clientKeys.split(","))) : null;
                Set<String> sharedKeySet = !StringUtils.isEmpty(sharedKeys) ? new HashSet<>(Arrays.asList(sharedKeys.split(","))) : null;
                request = new BasicGetAttributesRequest(0, clientKeySet, sharedKeySet);
            }
            process(ctx, request);
        } else {
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
        }

        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postDeviceAttributes(@PathVariable("deviceToken") String deviceToken,
                                                               @RequestBody String json) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter);
        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
            try {
                process(ctx, JsonConverter.convertToAttributes(new JsonParser().parse(json)));
            } catch (IllegalStateException | JsonSyntaxException ex) {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            }
        } else {
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
        }
        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/telemetry", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postTelemetry(@PathVariable("deviceToken") String deviceToken,
                                                        @RequestBody String json) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter);
        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
            try {
                process(ctx, JsonConverter.convertToTelemetry(new JsonParser().parse(json)));
            } catch (IllegalStateException | JsonSyntaxException ex) {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            }
        } else {
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
        }
        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> subscribeToCommands(@PathVariable("deviceToken") String deviceToken,
                                                              @RequestParam(value = "timeout", required = false, defaultValue = "0") long timeout) {
        return subscribe(deviceToken, timeout, new RpcSubscribeMsg());
    }

    @RequestMapping(value = "/{deviceToken}/rpc/{requestId}", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> replyToCommand(@PathVariable("deviceToken") String deviceToken,
                                                         @PathVariable("requestId") Integer requestId,
                                                         @RequestBody String json) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter);
        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
            try {
                JsonObject response = new JsonParser().parse(json).getAsJsonObject();
                process(ctx, new ToDeviceRpcResponseMsg(requestId, response.toString()));
            } catch (IllegalStateException | JsonSyntaxException ex) {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            }
        } else {
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
        }
        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postRpcRequest(@PathVariable("deviceToken") String deviceToken,
                                                         @RequestBody String json) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter);
        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
            try {
                JsonObject request = new JsonParser().parse(json).getAsJsonObject();
                process(ctx, new ToServerRpcRequestMsg(0,
                        request.get("method").getAsString(),
                        request.get("params").toString()));
            } catch (IllegalStateException | JsonSyntaxException ex) {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            }
        } else {
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
        }
        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/attributes/updates", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> subscribeToAttributes(@PathVariable("deviceToken") String deviceToken,
                                                                @RequestParam(value = "timeout", required = false, defaultValue = "0") long timeout) {
        return subscribe(deviceToken, timeout, new AttributesSubscribeMsg());
    }

    private DeferredResult<ResponseEntity> subscribe(String deviceToken, long timeout, FromDeviceMsg msg) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter, timeout);
        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
            try {
                process(ctx, msg);
            } catch (IllegalStateException | JsonSyntaxException ex) {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            }
        } else {
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
        }
        return responseWriter;
    }

    private HttpSessionCtx getHttpSessionCtx(DeferredResult<ResponseEntity> responseWriter) {
        return getHttpSessionCtx(responseWriter, defaultTimeout);
    }

    private HttpSessionCtx getHttpSessionCtx(DeferredResult<ResponseEntity> responseWriter, long timeout) {
        return new HttpSessionCtx(processor, authService, responseWriter, timeout != 0 ? timeout : defaultTimeout);
    }

    private void process(HttpSessionCtx ctx, FromDeviceMsg request) {
        AdaptorToSessionActorMsg msg = new BasicAdaptorToSessionActorMsg(ctx, request);
        processor.process(new BasicToDeviceActorSessionMsg(ctx.getDevice(), msg));
    }

}
