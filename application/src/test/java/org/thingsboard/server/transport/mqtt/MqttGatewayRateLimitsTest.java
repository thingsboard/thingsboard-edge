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
package org.thingsboard.server.transport.mqtt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.rule.trigger.RateLimitsTrigger;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.thingsboard.server.common.data.limit.LimitedApi.TRANSPORT_MESSAGES_PER_GATEWAY;
import static org.thingsboard.server.common.data.limit.LimitedApi.TRANSPORT_MESSAGES_PER_GATEWAY_DEVICE;

@DaoSqlTest
@TestPropertySource(properties = {
        "service.integrations.supported=ALL",
        "transport.mqtt.enabled=true",
})
public class MqttGatewayRateLimitsTest extends AbstractControllerTest {

    private static final String GATEWAY_TOPIC = "v1/gateway/telemetry";
    private static final String DEVICE_TOPIC = "v1/devices/me/telemetry";
    private static final String DEVICE_A = "DeviceA";
    private static final String DEVICE_B = "DeviceB";

    private static final String DEVICE_PAYLOAD = "{\"temperature\": 42}";

    private Device gateway;
    private String gatewayAccessToken;

    @SpyBean
    private NotificationRuleProcessor notificationRuleProcessor;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        Assert.assertNotNull(tenantProfile);

        DefaultTenantProfileConfiguration profileConfiguration = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();

        profileConfiguration.setTransportGatewayMsgRateLimit(null);
        profileConfiguration.setTransportGatewayTelemetryMsgRateLimit(null);
        profileConfiguration.setTransportGatewayTelemetryDataPointsRateLimit(null);

        profileConfiguration.setTransportGatewayDeviceMsgRateLimit(null);
        profileConfiguration.setTransportGatewayDeviceTelemetryMsgRateLimit(null);
        profileConfiguration.setTransportGatewayDeviceTelemetryDataPointsRateLimit(null);

        doPost("/api/tenantProfile", tenantProfile);

        loginTenantAdmin();
        createGateway();

        Mockito.reset(notificationRuleProcessor);
    }

    @Test
    public void transportGatewayMsgRateLimitTest() throws Exception {
        // Device A 2 msgs success ('create device', 'to device actor'), Device B 'create device success' , 'to device actor' - limited
        transportGatewayRateLimitTest(profileConfiguration -> profileConfiguration.setTransportGatewayMsgRateLimit("3:600"));
    }

    @Test
    public void transportGatewayTelemetryMsgRateLimitTest() throws Exception {
        transportGatewayRateLimitTest(profileConfiguration -> profileConfiguration.setTransportGatewayTelemetryMsgRateLimit("1:600"));
    }

    @Test
    public void transportGatewayTelemetryDataPointsRateLimitTest() throws Exception {
        transportGatewayRateLimitTest(profileConfiguration -> profileConfiguration.setTransportGatewayTelemetryDataPointsRateLimit("1:600"));
    }

    private void transportGatewayRateLimitTest(Consumer<DefaultTenantProfileConfiguration> profileConfiguration) throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        Assert.assertNotNull(tenantProfile);

        profileConfiguration.accept((DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration());

        doPost("/api/tenantProfile", tenantProfile);

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        client.publishAndWait(GATEWAY_TOPIC, getGatewayPayload(DEVICE_A));

        loginTenantAdmin();

        Device deviceA = getDeviceByName(DEVICE_A);

        var deviceATrigger = createRateLimitsTrigger(deviceA, TRANSPORT_MESSAGES_PER_GATEWAY);

        Mockito.verify(notificationRuleProcessor, Mockito.never()).process(eq(deviceATrigger));

        try {
            client.publishAndWait(GATEWAY_TOPIC, getGatewayPayload(DEVICE_B));
        } catch (Exception t) {
        }

        Device deviceB = getDeviceByName(DEVICE_B);

        var deviceBTrigger = createRateLimitsTrigger(deviceB, TRANSPORT_MESSAGES_PER_GATEWAY);

        Mockito.verify(notificationRuleProcessor, Mockito.times(1)).process(deviceBTrigger);

        if (client.isConnected()) {
            client.disconnect();
        }
    }

    @Test
    public void transportGatewayDeviceMsgRateLimitTest() throws Exception {
        transportGatewayDeviceRateLimitTest(profileConfiguration -> profileConfiguration.setTransportGatewayDeviceMsgRateLimit("3:600"));
    }

    @Test
    public void transportGatewayDeviceTelemetryMsgRateLimitTest() throws Exception {
        transportGatewayDeviceRateLimitTest(profileConfiguration -> profileConfiguration.setTransportGatewayDeviceTelemetryMsgRateLimit("1:600"));
    }

    @Test
    public void transportGatewayDeviceTelemetryDataPointsRateLimitTest() throws Exception {
        transportGatewayDeviceRateLimitTest(profileConfiguration -> profileConfiguration.setTransportGatewayDeviceTelemetryDataPointsRateLimit("1:600"));
    }

    private void transportGatewayDeviceRateLimitTest(Consumer<DefaultTenantProfileConfiguration> profileConfiguration) throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        Assert.assertNotNull(tenantProfile);

        profileConfiguration.accept((DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration());

        doPost("/api/tenantProfile", tenantProfile);

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        client.publishAndWait(DEVICE_TOPIC, DEVICE_PAYLOAD.getBytes());
        client.disconnect();

        var gatewayTrigger = createRateLimitsTrigger(gateway, TRANSPORT_MESSAGES_PER_GATEWAY_DEVICE);

        Mockito.verify(notificationRuleProcessor, Mockito.never()).process(eq(gatewayTrigger));

        loginTenantAdmin();

        client = new MqttTestClient();

        try {
            client.connectAndWait(gatewayAccessToken);
            client.publishAndWait(DEVICE_TOPIC, DEVICE_PAYLOAD.getBytes());
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception t) {
        }

        Awaitility.await()
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> Mockito.verify(notificationRuleProcessor, Mockito.times(1)).process(gatewayTrigger));

        if (client.isConnected()) {
            client.disconnect();
        }
    }

    private void createGateway() throws Exception {
        Device device = new Device();
        device.setName("gateway");
        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        additionalInfo.put("gateway", true);
        device.setAdditionalInfo(additionalInfo);
        device = doPost("/api/device", device, Device.class);
        assertNotNull(device);
        var gatewayId = device.getId();
        assertNotNull(gatewayId);

        DeviceCredentials deviceCredentials = doGet("/api/device/" + gatewayId + "/credentials", DeviceCredentials.class);
        assertNotNull(deviceCredentials);
        assertEquals(gatewayId, deviceCredentials.getDeviceId());
        gatewayAccessToken = deviceCredentials.getCredentialsId();
        assertNotNull(gatewayAccessToken);

        this.gateway = device;
    }

    private Device getDeviceByName(String deviceName) throws Exception {
        Device device = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
        assertNotNull(device);
        return device;
    }

    private byte[] getGatewayPayload(String deviceName) {
        return String.format("{\"%s\": [{\"values\": %s}]}", deviceName, DEVICE_PAYLOAD).getBytes();
    }

    private RateLimitsTrigger createRateLimitsTrigger(Device device, LimitedApi limitedApi) {
        return RateLimitsTrigger.builder()
                .tenantId(tenantId)
                .api(limitedApi)
                .limitLevel(device.getId())
                .limitLevelEntityName(device.getName())
                .build();
    }
}
