/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ashvayka on 19.01.17.
 */
@Slf4j
public class GatewayDeviceSessionCtx extends MqttDeviceAwareSessionContext implements SessionMsgListener {

    private final GatewaySessionHandler parent;
    private final SessionInfoProto sessionInfo;

    public GatewayDeviceSessionCtx(GatewaySessionHandler parent, DeviceInfoProto deviceInfo, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap) {
        super(UUID.randomUUID(), mqttQoSMap);
        this.parent = parent;
        this.sessionInfo = SessionInfoProto.newBuilder()
                .setNodeId(parent.getNodeId())
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(deviceInfo.getDeviceIdMSB())
                .setDeviceIdLSB(deviceInfo.getDeviceIdLSB())
                .setTenantIdMSB(deviceInfo.getTenantIdMSB())
                .setTenantIdLSB(deviceInfo.getTenantIdLSB())
                .setDeviceName(deviceInfo.getDeviceName())
                .setDeviceType(deviceInfo.getDeviceType())
                .build();
        setDeviceInfo(deviceInfo);
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public int nextMsgId() {
        return parent.nextMsgId();
    }

    SessionInfoProto getSessionInfo() {
        return sessionInfo;
    }

    @Override
    public void onGetAttributesResponse(GetAttributeResponseMsg response) {
        try {
            parent.getAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), response).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onAttributeUpdate(AttributeUpdateNotificationMsg notification) {
        try {
            parent.getAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), notification).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onRemoteSessionCloseCommand(SessionCloseNotificationProto sessionCloseNotification) {
        parent.deregisterSession(getDeviceInfo().getDeviceName());
    }

    @Override
    public void onToDeviceRpcRequest(ToDeviceRpcRequestMsg request) {
        try {
            parent.getAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), request).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse) {
        // This feature is not supported in the TB IoT Gateway yet.
    }
}
