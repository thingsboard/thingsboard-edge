/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.coap.adaptors;

import java.util.*;

import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.msg.core.*;
import org.thingsboard.server.common.msg.kv.AttributesKVMsg;
import org.thingsboard.server.common.msg.session.AdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicAdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionActorToAdaptorMsg;
import org.thingsboard.server.common.msg.session.SessionContext;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.common.msg.session.ex.ProcessingTimeoutException;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.thingsboard.server.transport.coap.CoapTransportResource;
import org.thingsboard.server.transport.coap.session.CoapSessionCtx;

@Component("JsonCoapAdaptor")
@Slf4j
public class JsonCoapAdaptor implements CoapTransportAdaptor {

    @Override
    public AdaptorToSessionActorMsg convertToActorMsg(CoapSessionCtx ctx, SessionMsgType type, Request inbound) throws AdaptorException {
        FromDeviceMsg msg = null;
        switch (type) {
            case POST_TELEMETRY_REQUEST:
                msg = convertToTelemetryUploadRequest(ctx, inbound);
                break;
            case POST_ATTRIBUTES_REQUEST:
                msg = convertToUpdateAttributesRequest(ctx, inbound);
                break;
            case GET_ATTRIBUTES_REQUEST:
                msg = convertToGetAttributesRequest(ctx, inbound);
                break;
            case SUBSCRIBE_RPC_COMMANDS_REQUEST:
                msg = new RpcSubscribeMsg();
                break;
            case UNSUBSCRIBE_RPC_COMMANDS_REQUEST:
                msg = new RpcUnsubscribeMsg();
                break;
            case TO_DEVICE_RPC_RESPONSE:
                msg = convertToDeviceRpcResponse(ctx, inbound);
                break;
            case TO_SERVER_RPC_REQUEST:
                msg = convertToServerRpcRequest(ctx, inbound);
                break;
            case SUBSCRIBE_ATTRIBUTES_REQUEST:
                msg = new AttributesSubscribeMsg();
                break;
            case UNSUBSCRIBE_ATTRIBUTES_REQUEST:
                msg = new AttributesUnsubscribeMsg();
                break;
            default:
                log.warn("[{}] Unsupported msg type: {}!", ctx.getSessionId(), type);
                throw new AdaptorException(new IllegalArgumentException("Unsupported msg type: " + type + "!"));
        }
        return new BasicAdaptorToSessionActorMsg(ctx, msg);
    }

    private FromDeviceMsg convertToDeviceRpcResponse(CoapSessionCtx ctx, Request inbound) throws AdaptorException {
        Optional<Integer> requestId = CoapTransportResource.getRequestId(inbound);
        String payload = validatePayload(ctx, inbound);
        JsonObject response = new JsonParser().parse(payload).getAsJsonObject();
        return new ToDeviceRpcResponseMsg(
                requestId.orElseThrow(() -> new AdaptorException("Request id is missing!")),
                response.get("response").toString());
    }

    private FromDeviceMsg convertToServerRpcRequest(CoapSessionCtx ctx, Request inbound) throws AdaptorException {

        String payload = validatePayload(ctx, inbound);

        return JsonConverter.convertToServerRpcRequest(new JsonParser().parse(payload), 0);
    }

    @Override
    public Optional<Response> convertToAdaptorMsg(CoapSessionCtx ctx, SessionActorToAdaptorMsg source) throws AdaptorException {
        ToDeviceMsg msg = source.getMsg();
        switch (msg.getSessionMsgType()) {
            case STATUS_CODE_RESPONSE:
            case TO_DEVICE_RPC_RESPONSE_ACK:
                return Optional.of(convertStatusCodeResponse((StatusCodeResponse) msg));
            case GET_ATTRIBUTES_RESPONSE:
                return Optional.of(convertGetAttributesResponse((GetAttributesResponse) msg));
            case ATTRIBUTES_UPDATE_NOTIFICATION:
                return Optional.of(convertNotificationResponse(ctx, (AttributesUpdateNotification) msg));
            case TO_DEVICE_RPC_REQUEST:
                return Optional.of(convertToDeviceRpcRequest(ctx, (ToDeviceRpcRequestMsg) msg));
            case TO_SERVER_RPC_RESPONSE:
                return Optional.of(convertToServerRpcResponse(ctx, (ToServerRpcResponseMsg) msg));
            case RULE_ENGINE_ERROR:
                return Optional.of(convertToRuleEngineErrorResponse(ctx, (RuleEngineErrorMsg) msg));
            default:
                log.warn("[{}] Unsupported msg type: {}!", source.getSessionId(), msg.getSessionMsgType());
                throw new AdaptorException(new IllegalArgumentException("Unsupported msg type: " + msg.getSessionMsgType() + "!"));
        }
    }

    private Response convertToRuleEngineErrorResponse(CoapSessionCtx ctx, RuleEngineErrorMsg msg) {
        ResponseCode status = ResponseCode.INTERNAL_SERVER_ERROR;
        switch (msg.getError()) {
            case QUEUE_PUT_TIMEOUT:
                status = ResponseCode.GATEWAY_TIMEOUT;
                break;
            default:
                if (msg.getInSessionMsgType() == SessionMsgType.TO_SERVER_RPC_REQUEST) {
                    status = ResponseCode.BAD_REQUEST;
                }
                break;
        }
        Response response = new Response(status);
        response.setPayload(JsonConverter.toErrorJson(msg.getErrorMsg()).toString());
        return response;
    }

    private Response convertNotificationResponse(CoapSessionCtx ctx, AttributesUpdateNotification msg) {
        return getObserveNotification(ctx, JsonConverter.toJson(msg.getData(), false));
    }

    private Response convertToDeviceRpcRequest(CoapSessionCtx ctx, ToDeviceRpcRequestMsg msg) {
        return getObserveNotification(ctx, JsonConverter.toJson(msg, true));
    }

    private Response getObserveNotification(CoapSessionCtx ctx, JsonObject json) {
        Response response = new Response(ResponseCode.CONTENT);
        response.getOptions().setObserve(ctx.nextSeqNumber());
        response.setPayload(json.toString());
        return response;
    }

    private AttributesUpdateRequest convertToUpdateAttributesRequest(SessionContext ctx, Request inbound) throws AdaptorException {
        String payload = validatePayload(ctx, inbound);
        try {
            return JsonConverter.convertToAttributes(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    private FromDeviceMsg convertToGetAttributesRequest(SessionContext ctx, Request inbound) throws AdaptorException {
        List<String> queryElements = inbound.getOptions().getUriQuery();
        if (queryElements != null && queryElements.size() > 0) {
            Set<String> clientKeys = toKeys(ctx, queryElements, "clientKeys");
            Set<String> sharedKeys = toKeys(ctx, queryElements, "sharedKeys");
            return new BasicGetAttributesRequest(0, clientKeys, sharedKeys);
        } else {
            return new BasicGetAttributesRequest(0);
        }
    }

    private Set<String> toKeys(SessionContext ctx, List<String> queryElements, String attributeName) throws AdaptorException {
        String keys = null;
        for (String queryElement : queryElements) {
            String[] queryItem = queryElement.split("=");
            if (queryItem.length == 2 && queryItem[0].equals(attributeName)) {
                keys = queryItem[1];
            }
        }
        if (keys != null && !StringUtils.isEmpty(keys)) {
            return new HashSet<>(Arrays.asList(keys.split(",")));
        } else {
            return null;
        }
    }

    private TelemetryUploadRequest convertToTelemetryUploadRequest(SessionContext ctx, Request inbound) throws AdaptorException {
        String payload = validatePayload(ctx, inbound);
        try {
            return JsonConverter.convertToTelemetry(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    private Response convertStatusCodeResponse(StatusCodeResponse msg) {
        if (msg.isSuccess()) {
            Optional<Integer> code = msg.getData();
            if (code.isPresent() && code.get() == 200) {
                return new Response(ResponseCode.VALID);
            } else {
                return new Response(ResponseCode.CREATED);
            }
        } else {
            return convertError(msg.getError());
        }
    }

    private String validatePayload(SessionContext ctx, Request inbound) throws AdaptorException {
        String payload = inbound.getPayloadString();
        if (payload == null) {
            log.warn("[{}] Payload is empty!", ctx.getSessionId());
            throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
        }
        return payload;
    }

    private Response convertToServerRpcResponse(SessionContext ctx, ToServerRpcResponseMsg msg) {
        if (msg.isSuccess()) {
            Response response = new Response(ResponseCode.CONTENT);
            JsonElement result = JsonConverter.toJson(msg);
            response.setPayload(result.toString());
            return response;
        } else {
            return convertError(Optional.of(new RuntimeException("Server RPC response is empty!")));
        }
    }

    private Response convertGetAttributesResponse(GetAttributesResponse msg) {
        if (msg.isSuccess()) {
            Optional<AttributesKVMsg> payload = msg.getData();
            if (!payload.isPresent() || (payload.get().getClientAttributes().isEmpty() && payload.get().getSharedAttributes().isEmpty())) {
                return new Response(ResponseCode.NOT_FOUND);
            } else {
                Response response = new Response(ResponseCode.CONTENT);
                JsonObject result = JsonConverter.toJson(payload.get(), false);
                response.setPayload(result.toString());
                return response;
            }
        } else {
            return convertError(msg.getError());
        }
    }

    private Response convertError(Optional<Exception> exception) {
        if (exception.isPresent()) {
            log.warn("Converting exception: {}", exception.get().getMessage(), exception.get());
            if (exception.get() instanceof ProcessingTimeoutException) {
                return new Response(ResponseCode.SERVICE_UNAVAILABLE);
            } else {
                return new Response(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new Response(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

}
