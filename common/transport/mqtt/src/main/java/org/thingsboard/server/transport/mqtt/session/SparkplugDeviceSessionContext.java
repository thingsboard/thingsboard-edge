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
package org.thingsboard.server.transport.mqtt.session;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugRpcRequestHeader;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.getTsKvProto;

@Slf4j
public class SparkplugDeviceSessionContext extends AbstractGatewayDeviceSessionContext<SparkplugNodeSessionHandler> {

    public SparkplugDeviceSessionContext(SparkplugNodeSessionHandler parent,
                                         TransportDeviceInfo deviceInfo,
                                         DeviceProfile deviceProfile,
                                         ConcurrentMap<MqttTopicMatcher,
                                                 Integer> mqttQoSMap,
                                         TransportService transportService) {
        super(parent, deviceInfo, deviceProfile, mqttQoSMap, transportService);
    }

    @Override
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg notification) {
        log.trace("[{}] Received attributes update notification to sparkplug device", sessionId);
        notification.getSharedUpdatedList().forEach(tsKvProto -> {
            if (getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                        SparkplugMessageType.DCMD, deviceInfo.getDeviceName());
                parent.createSparkplugMqttPublishMsg(tsKvProto,
                        sparkplugTopic.toString(),
                        getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                        .ifPresent(this.parent::writeAndFlush);
            }
        });
    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        log.trace("[{}] Received RPC Request notification to sparkplug device", sessionId);
        try {
            /**
             *  DCMD {"metricName":"MyDeviceMetricText","value":"MyNodeMetric05_String_Value"}
             *  DCMD {"metricName":"MyNodeMetric02_LongInt64","value":2814119464032075444}
             *  DCMD {"metricName":"MyNodeMetric03_Double","value":6336935578763180333}
             *  DCMD {"metricName":"MyNodeMetric04_Float","value":413.18222}
             *  DCMD {"metricName":"Node Control/Rebirth","value":false}
             *  DCMD {"metricName":"MyNodeMetric06_Json_Bytes", "value":[40,47,-49]}
             */
            SparkplugMessageType messageType = SparkplugMessageType.parseMessageType(rpcRequest.getMethodName());
            if (messageType == null) {
                parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                        ThingsboardErrorCode.INVALID_ARGUMENTS, "Unsupported SparkplugMessageType: " + rpcRequest.getMethodName() + rpcRequest.getParams());
                return;
            }
            SparkplugRpcRequestHeader header = JacksonUtil.fromString(rpcRequest.getParams(), SparkplugRpcRequestHeader.class);
            header.setMessageType(messageType.name());
            TransportProtos.TsKvProto tsKvProto = getTsKvProto(header.getMetricName(), header.getValue(), new Date().getTime());
            if (getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                        messageType, deviceInfo.getDeviceName());
                parent.createSparkplugMqttPublishMsg(tsKvProto,
                        sparkplugTopic.toString(),
                        getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                        .ifPresent(payload -> parent.sendToDeviceRpcRequest(payload, rpcRequest, sessionInfo));
            } else {
                parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                        ThingsboardErrorCode.BAD_REQUEST_PARAMS, " Failed send To Device Rpc Request: " +
                                rpcRequest.getMethodName() + ". This device does not have a metricName: [" + tsKvProto.getKv().getKey() + "]");
            }
        } catch (ThingsboardException e) {
            parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS, " Failed send To Device Rpc Request: " +
                            rpcRequest.getMethodName() + ". " + e.getMessage());
        }
    }

}
