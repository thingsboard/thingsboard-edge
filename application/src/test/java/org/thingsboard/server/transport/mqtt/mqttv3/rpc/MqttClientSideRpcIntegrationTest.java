/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.mqtt.mqttv3.rpc;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.limits.GatewaySessionLimits;
import org.thingsboard.server.transport.mqtt.limits.SessionLimits;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestSubscribeOnTopicCallback;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_RESPONSE_SUB_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_RESPONSE_TOPIC;

@DaoSqlTest
public class MqttClientSideRpcIntegrationTest extends AbstractMqttIntegrationTest {

    @Value("${transport.mqtt.netty.max_payload_size}")
    private Integer maxPayloadSize;

    @Value("${transport.mqtt.msg_queue_size_per_device_limit}")
    private int maxInflightMessages;

    @Test
    public void getSessionLimitsRpcForDeviceTest() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        DefaultTenantProfileConfiguration profileConfiguration = tenantProfile.getDefaultProfileConfiguration();

        profileConfiguration.setTransportDeviceMsgRateLimit("20:600");
        profileConfiguration.setTransportDeviceTelemetryMsgRateLimit("10:600");
        profileConfiguration.setTransportDeviceTelemetryDataPointsRateLimit("40:600");

        doPost("/api/tenantProfile", tenantProfile);

        var expectedLimits = new SessionLimits();
        var deviceLimits = new SessionLimits.SessionRateLimits(profileConfiguration.getTransportDeviceMsgRateLimit(),
                profileConfiguration.getTransportDeviceTelemetryMsgRateLimit(),
                profileConfiguration.getTransportDeviceTelemetryDataPointsRateLimit());
        expectedLimits.setRateLimits(deviceLimits);
        expectedLimits.setMaxPayloadSize(maxPayloadSize);
        expectedLimits.setMaxInflightMessages(maxInflightMessages);

        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Get Service Configuration")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);

        MqttTestCallback callback = new MqttTestSubscribeOnTopicCallback(DEVICE_RPC_RESPONSE_TOPIC + "1");
        client.setCallback(callback);
        client.subscribeAndWait(DEVICE_RPC_RESPONSE_SUB_TOPIC, MqttQoS.AT_MOST_ONCE);

        client.publishAndWait(DEVICE_RPC_REQUESTS_TOPIC + "1", "{\"method\":\"getSessionLimits\",\"params\":{}}".getBytes());

        assertThat(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await callback").isTrue();

        var payload = callback.getPayloadBytes();
        SessionLimits actualLimits = JacksonUtil.fromBytes(payload, SessionLimits.class);
        assertEquals(expectedLimits, actualLimits);

        client.disconnect();
    }

    @Test
    public void getSessionLimitsRpcForGatewayTest() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        DefaultTenantProfileConfiguration profileConfiguration = tenantProfile.getDefaultProfileConfiguration();

        profileConfiguration.setTransportGatewayMsgRateLimit("100:600");
        profileConfiguration.setTransportGatewayTelemetryMsgRateLimit("50:600");
        profileConfiguration.setTransportGatewayTelemetryDataPointsRateLimit("200:600");

        profileConfiguration.setTransportGatewayDeviceMsgRateLimit("20:600");
        profileConfiguration.setTransportGatewayDeviceTelemetryMsgRateLimit("10:600");
        profileConfiguration.setTransportGatewayDeviceTelemetryDataPointsRateLimit("40:600");

        doPost("/api/tenantProfile", tenantProfile);

        var expectedLimits = new GatewaySessionLimits();
        var gatewayLimits = new SessionLimits.SessionRateLimits(profileConfiguration.getTransportGatewayMsgRateLimit(),
                profileConfiguration.getTransportGatewayTelemetryMsgRateLimit(),
                profileConfiguration.getTransportGatewayTelemetryDataPointsRateLimit());
        var gatewayDeviceLimits = new SessionLimits.SessionRateLimits(profileConfiguration.getTransportGatewayDeviceMsgRateLimit(),
                profileConfiguration.getTransportGatewayDeviceTelemetryMsgRateLimit(),
                profileConfiguration.getTransportGatewayDeviceTelemetryDataPointsRateLimit());
        expectedLimits.setGatewayRateLimits(gatewayLimits);
        expectedLimits.setRateLimits(gatewayDeviceLimits);
        expectedLimits.setMaxPayloadSize(maxPayloadSize);
        expectedLimits.setMaxInflightMessages(maxInflightMessages);

        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .gatewayName("Test Get Service Configuration Gateway")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);

        MqttTestCallback callback = new MqttTestSubscribeOnTopicCallback(DEVICE_RPC_RESPONSE_TOPIC + "1");
        client.setCallback(callback);
        client.subscribeAndWait(DEVICE_RPC_RESPONSE_SUB_TOPIC, MqttQoS.AT_MOST_ONCE);

        client.publishAndWait(DEVICE_RPC_REQUESTS_TOPIC + "1", "{\"method\":\"getSessionLimits\",\"params\":{}}".getBytes());

        assertTrue(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        var payload = callback.getPayloadBytes();
        SessionLimits actualLimits = JacksonUtil.fromBytes(payload, GatewaySessionLimits.class);
        assertEquals(expectedLimits, actualLimits);

        client.disconnect();
    }
}
