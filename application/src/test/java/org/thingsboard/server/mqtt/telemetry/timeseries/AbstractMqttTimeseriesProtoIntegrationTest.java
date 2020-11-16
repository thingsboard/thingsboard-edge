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
package org.thingsboard.server.mqtt.telemetry.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public abstract class AbstractMqttTimeseriesProtoIntegrationTest extends AbstractMqttTimeseriesIntegrationTest {

    private static final String POST_DATA_TELEMETRY_TOPIC = "proto/telemetry";

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Post Telemetry device proto payload", "Test Post Telemetry gateway proto payload", TransportPayloadType.PROTOBUF, POST_DATA_TELEMETRY_TOPIC, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testPushMqttTelemetry() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        TransportProtos.TsKvListProto tsKvListProto = getTsKvListProto(expectedKeys, 0);
        processTelemetryTest(POST_DATA_TELEMETRY_TOPIC, expectedKeys, tsKvListProto.toByteArray(), false);
    }

    @Test
    public void testPushMqttTelemetryWithTs() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        TransportProtos.TsKvListProto tsKvListProto = getTsKvListProto(expectedKeys, 10000);
        processTelemetryTest(POST_DATA_TELEMETRY_TOPIC, expectedKeys, tsKvListProto.toByteArray(), true);
    }

    @Test
    public void testPushMqttTelemetryGateway() throws Exception {
        TransportApiProtos.GatewayTelemetryMsg.Builder gatewayTelemetryMsgProtoBuilder = TransportApiProtos.GatewayTelemetryMsg.newBuilder();
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        String deviceName1 = "Device A";
        String deviceName2 = "Device B";
        TransportApiProtos.TelemetryMsg deviceATelemetryMsgProto = getDeviceTelemetryMsgProto(deviceName1, expectedKeys, 10000, 20000);
        TransportApiProtos.TelemetryMsg deviceBTelemetryMsgProto = getDeviceTelemetryMsgProto(deviceName2, expectedKeys, 10000, 20000);
        gatewayTelemetryMsgProtoBuilder.addAllMsg(Arrays.asList(deviceATelemetryMsgProto, deviceBTelemetryMsgProto));
        TransportApiProtos.GatewayTelemetryMsg gatewayTelemetryMsg = gatewayTelemetryMsgProtoBuilder.build();
        processGatewayTelemetryTest(MqttTopics.GATEWAY_TELEMETRY_TOPIC, expectedKeys, gatewayTelemetryMsg.toByteArray(), deviceName1, deviceName2);
    }

    @Test
    public void testGatewayConnect() throws Exception {
        String deviceName = "Device A";
        TransportApiProtos.ConnectMsg connectMsgProto = getConnectProto(deviceName);
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        publishMqttMsg(client, connectMsgProto.toByteArray(), MqttTopics.GATEWAY_CONNECT_TOPIC);

        Device device = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
                20,
                100);

        assertNotNull(device);
    }

    private TransportApiProtos.ConnectMsg getConnectProto(String deviceName) {
        TransportApiProtos.ConnectMsg.Builder builder = TransportApiProtos.ConnectMsg.newBuilder();
        builder.setDeviceName(deviceName);
        builder.setDeviceType(TransportPayloadType.PROTOBUF.name());
        return builder.build();
    }

    private TransportApiProtos.TelemetryMsg getDeviceTelemetryMsgProto(String deviceName, List<String> expectedKeys, long firstTs, long secondTs) {
        TransportApiProtos.TelemetryMsg.Builder deviceTelemetryMsgBuilder = TransportApiProtos.TelemetryMsg.newBuilder();
        TransportProtos.TsKvListProto tsKvListProto1 = getTsKvListProto(expectedKeys, firstTs);
        TransportProtos.TsKvListProto tsKvListProto2 = getTsKvListProto(expectedKeys, secondTs);
        TransportProtos.PostTelemetryMsg.Builder msg = TransportProtos.PostTelemetryMsg.newBuilder();
        msg.addAllTsKvList(Arrays.asList(tsKvListProto1, tsKvListProto2));
        deviceTelemetryMsgBuilder.setDeviceName(deviceName);
        deviceTelemetryMsgBuilder.setMsg(msg);
        return deviceTelemetryMsgBuilder.build();
    }

    private TransportProtos.TsKvListProto getTsKvListProto(List<String> expectedKeys, long ts) {
        List<TransportProtos.KeyValueProto> kvProtos = getKvProtos(expectedKeys);
        TransportProtos.TsKvListProto.Builder builder = TransportProtos.TsKvListProto.newBuilder();
        builder.addAllKv(kvProtos);
        builder.setTs(ts);
        return builder.build();
    }
}
