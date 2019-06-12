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
package org.thingsboard.server.transport.mqtt.adaptors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.PostAttributeMsg;
import org.thingsboard.server.gen.transport.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.ToServerRpcRequestMsg;
import org.thingsboard.server.gen.transport.ToServerRpcResponseMsg;
import org.thingsboard.server.transport.mqtt.MqttTopics;
import org.thingsboard.server.transport.mqtt.session.MqttDeviceAwareSessionContext;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Component("JsonMqttAdaptor")
@Slf4j
public class JsonMqttAdaptor implements MqttTransportAdaptor {

    private static final Gson GSON = new Gson();
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);

    @Override
    public PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String payload = validatePayload(ctx.getSessionId(), inbound.payload());
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String payload = validatePayload(ctx.getSessionId(), inbound.payload());
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public GetAttributeRequestMsg convertToGetAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        try {
            GetAttributeRequestMsg.Builder result = GetAttributeRequestMsg.newBuilder();
            result.setRequestId(Integer.valueOf(topicName.substring(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX.length())));
            String payload = inbound.payload().toString(UTF8);
            JsonElement requestBody = new JsonParser().parse(payload);
            Set<String> clientKeys = toStringSet(requestBody, "clientKeys");
            Set<String> sharedKeys = toStringSet(requestBody, "sharedKeys");
            if (clientKeys != null) {
                result.addAllClientAttributeNames(clientKeys);
            }
            if (sharedKeys != null) {
                result.addAllSharedAttributeNames(sharedKeys);
            }
            return result.build();
        } catch (RuntimeException e) {
            log.warn("Failed to decode get attributes request", e);
            throw new AdaptorException(e);
        }
    }

    @Override
    public ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        try {
            Integer requestId = Integer.valueOf(topicName.substring(MqttTopics.DEVICE_RPC_RESPONSE_TOPIC.length()));
            String payload = inbound.payload().toString(UTF8);
            return ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(payload).build();
        } catch (RuntimeException e) {
            log.warn("Failed to decode get attributes request", e);
            throw new AdaptorException(e);
        }
    }

    @Override
    public ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        String payload = validatePayload(ctx.getSessionId(), inbound.payload());
        try {
            Integer requestId = Integer.valueOf(topicName.substring(MqttTopics.DEVICE_RPC_REQUESTS_TOPIC.length()));
            return JsonConverter.convertToServerRpcRequest(new JsonParser().parse(payload), requestId);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, GetAttributeResponseMsg responseMsg) throws AdaptorException {
        if (!StringUtils.isEmpty(responseMsg.getError())) {
            throw new AdaptorException(responseMsg.getError());
        } else {
            Integer requestId = responseMsg.getRequestId();
            if (requestId >= 0) {
                return Optional.of(createMqttPublishMsg(ctx,
                        MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX + requestId,
                        JsonConverter.toJson(responseMsg)));
            }
            return Optional.empty();
        }
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, GetAttributeResponseMsg responseMsg) throws AdaptorException {
        if (!StringUtils.isEmpty(responseMsg.getError())) {
            throw new AdaptorException(responseMsg.getError());
        } else {
            JsonObject result = JsonConverter.getJsonObjectForGateway(deviceName, responseMsg);
            return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, result));
        }
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_ATTRIBUTES_TOPIC, JsonConverter.toJson(notificationMsg)));
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException {
        JsonObject result = JsonConverter.getJsonObjectForGateway(deviceName, notificationMsg);
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, result));
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC + rpcRequest.getRequestId(), JsonConverter.toJson(rpcRequest, false)));
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_RPC_TOPIC, JsonConverter.toGatewayJson(deviceName, rpcRequest)));
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, ToServerRpcResponseMsg rpcResponse) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC + rpcResponse.getRequestId(), JsonConverter.toJson(rpcResponse)));
    }

    private MqttPublishMessage createMqttPublishMsg(MqttDeviceAwareSessionContext ctx, String topic, JsonElement json) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, ctx.getQoSForTopic(topic), false, 0);
        MqttPublishVariableHeader header = new MqttPublishVariableHeader(topic, ctx.nextMsgId());
        ByteBuf payload = ALLOCATOR.buffer();
        payload.writeBytes(GSON.toJson(json).getBytes(UTF8));
        return new MqttPublishMessage(mqttFixedHeader, header, payload);
    }

    private Set<String> toStringSet(JsonElement requestBody, String name) {
        JsonElement element = requestBody.getAsJsonObject().get(name);
        if (element != null) {
            return new HashSet<>(Arrays.asList(element.getAsString().split(",")));
        } else {
            return null;
        }
    }

    public static JsonElement validateJsonPayload(UUID sessionId, ByteBuf payloadData) throws AdaptorException {
        String payload = validatePayload(sessionId, payloadData);
        try {
            return new JsonParser().parse(payload);
        } catch (JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    private static String validatePayload(UUID sessionId, ByteBuf payloadData) throws AdaptorException {
        try {
            String payload = payloadData.toString(UTF8);
            if (payload == null) {
                log.warn("[{}] Payload is empty!", sessionId);
                throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
            }
            return payload;
        } finally {
            payloadData.release();
        }
    }

}
