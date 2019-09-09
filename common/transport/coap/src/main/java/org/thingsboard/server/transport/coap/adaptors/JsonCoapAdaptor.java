/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.transport.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.ClaimDeviceMsg;
import org.thingsboard.server.gen.transport.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.PostAttributeMsg;
import org.thingsboard.server.gen.transport.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.SessionInfoProto;
import org.thingsboard.server.gen.transport.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.ToServerRpcRequestMsg;
import org.thingsboard.server.gen.transport.ToServerRpcResponseMsg;
import org.thingsboard.server.transport.coap.CoapTransportResource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component("JsonCoapAdaptor")
@Slf4j
public class JsonCoapAdaptor implements CoapTransportAdaptor {

    @Override
    public PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound) throws AdaptorException {
        String payload = validatePayload(sessionId, inbound, false);
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound) throws AdaptorException {
        String payload = validatePayload(sessionId, inbound, false);
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException {
        List<String> queryElements = inbound.getOptions().getUriQuery();
        GetAttributeRequestMsg.Builder result = GetAttributeRequestMsg.newBuilder();
        if (queryElements != null && queryElements.size() > 0) {
            Set<String> clientKeys = toKeys(queryElements, "clientKeys");
            Set<String> sharedKeys = toKeys(queryElements, "sharedKeys");
            if (clientKeys != null) {
                result.addAllClientAttributeNames(clientKeys);
            }
            if (sharedKeys != null) {
                result.addAllSharedAttributeNames(sharedKeys);
            }
        }
        return result.build();
    }

    @Override
    public ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound) throws AdaptorException {
        Optional<Integer> requestId = CoapTransportResource.getRequestId(inbound);
        String payload = validatePayload(sessionId, inbound, false);
        JsonObject response = new JsonParser().parse(payload).getAsJsonObject();
        return ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId.orElseThrow(() -> new AdaptorException("Request id is missing!")))
                .setPayload(response.toString()).build();
    }

    @Override
    public ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException {
        String payload = validatePayload(sessionId, inbound, false);
        return JsonConverter.convertToServerRpcRequest(new JsonParser().parse(payload), 0);
    }

    @Override
    public ClaimDeviceMsg convertToClaimDevice(UUID sessionId, Request inbound, SessionInfoProto sessionInfo) throws AdaptorException {
        DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        String payload = validatePayload(sessionId, inbound, true);
        try {
            return JsonConverter.convertToClaimDeviceProto(deviceId, payload);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public Response convertToPublish(CoapTransportResource.CoapSessionListener session, AttributeUpdateNotificationMsg msg) throws AdaptorException {
        return getObserveNotification(session.getNextSeqNumber(), JsonConverter.toJson(msg));
    }

    @Override
    public Response convertToPublish(CoapTransportResource.CoapSessionListener session, ToDeviceRpcRequestMsg msg) throws AdaptorException {
        return getObserveNotification(session.getNextSeqNumber(), JsonConverter.toJson(msg, true));
    }

    @Override
    public Response convertToPublish(CoapTransportResource.CoapSessionListener coapSessionListener, ToServerRpcResponseMsg msg) throws AdaptorException {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        JsonElement result = JsonConverter.toJson(msg);
        response.setPayload(result.toString());
        return response;
    }

    @Override
    public Response convertToPublish(CoapTransportResource.CoapSessionListener session, GetAttributeResponseMsg msg) throws AdaptorException {
        if (msg.getClientAttributeListCount() == 0 && msg.getSharedAttributeListCount() == 0 && msg.getDeletedAttributeKeysCount() == 0) {
            return new Response(CoAP.ResponseCode.NOT_FOUND);
        } else {
            Response response = new Response(CoAP.ResponseCode.CONTENT);
            JsonObject result = JsonConverter.toJson(msg);
            response.setPayload(result.toString());
            return response;
        }
    }

    private Response getObserveNotification(int seqNumber, JsonElement json) {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.getOptions().setObserve(seqNumber);
        response.setPayload(json.toString());
        return response;
    }

    private String validatePayload(UUID sessionId, Request inbound, boolean isEmptyPayloadAllowed) throws AdaptorException {
        String payload = inbound.getPayloadString();
        if (payload == null) {
            log.warn("[{}] Payload is empty!", sessionId);
            if (!isEmptyPayloadAllowed) {
                throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
            }
        }
        return payload;
    }

    private Set<String> toKeys(List<String> queryElements, String attributeName) throws AdaptorException {
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

}
