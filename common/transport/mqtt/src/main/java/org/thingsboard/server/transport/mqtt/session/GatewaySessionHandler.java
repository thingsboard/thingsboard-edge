/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.mqtt.session;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.gateway.GatewayMetricsService;

import java.util.Optional;
import java.util.UUID;

import static com.amazonaws.util.StringUtils.UTF8;

/**
 * Created by ashvayka on 19.01.17.
 */
@Slf4j
public class GatewaySessionHandler extends AbstractGatewaySessionHandler<GatewayDeviceSessionContext> {

    private final GatewayMetricsService gatewayMetricsService;

    public GatewaySessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId, boolean overwriteDevicesActivity) {
        super(deviceSessionCtx, sessionId, overwriteDevicesActivity);
        this.gatewayMetricsService = deviceSessionCtx.getContext().getGatewayMetricsService();
    }

    public void onDeviceConnect(MqttPublishMessage mqttMsg) throws AdaptorException {
        if (isJsonPayloadType()) {
            onDeviceConnectJson(mqttMsg);
        } else {
            onDeviceConnectProto(mqttMsg);
        }
    }

    public void onDeviceTelemetry(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payload = mqttMsg.payload();
        if (isJsonPayloadType()) {
            onDeviceTelemetryJson(msgId, payload);
        } else {
            onDeviceTelemetryProto(msgId, payload);
        }
    }

    @Override
    protected GatewayDeviceSessionContext newDeviceSessionCtx(GetOrCreateDeviceFromGatewayResponse msg) {
        return new GatewayDeviceSessionContext(this, msg.getDeviceInfo(), msg.getDeviceProfile(), mqttQoSMap, transportService);
    }

    public void onGatewayDisconnect() {
        this.onDevicesDisconnect();
        gatewayMetricsService.onDeviceDisconnect(gateway.getDeviceId());
    }

    public void onGatewayUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        this.onDeviceUpdate(sessionInfo, device, deviceProfileOpt);
        gatewayMetricsService.onDeviceUpdate(sessionInfo, gateway.getDeviceId());
    }

    public void onGatewayDelete(DeviceId deviceId) {
        gatewayMetricsService.onDeviceDelete(deviceId);
    }

    public void onGatewayLatency(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payloadData = mqttMsg.payload();
        String payload = payloadData.toString(UTF8);
        if (payload == null) {
            log.debug("[{}][{}][{}] Payload is empty!", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
            throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
        }
        long ts = System.currentTimeMillis();
        try {
            gatewayMetricsService.process(deviceSessionCtx.getSessionInfo(), gateway.getDeviceId(), JacksonUtil.fromString(payload, new TypeReference<>() {}), ts);
            ack(msgId, MqttReasonCodes.PubAck.SUCCESS);
        } catch (Throwable t) {
            ackOrClose(msgId);
        }
    }

}
