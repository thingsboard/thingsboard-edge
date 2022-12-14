/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.msa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Random;


@Slf4j
@Listeners(TestListener.class)
public abstract class AbstractContainerTest {
    protected static final String HTTPS_URL = "https://localhost";

    protected final static String TEST_PROVISION_DEVICE_KEY = "test_provision_key";
    protected final static String TEST_PROVISION_DEVICE_SECRET = "test_provision_secret";
    protected static long timeoutMultiplier = 1;
    protected ObjectMapper mapper = new ObjectMapper();
    protected static final String TELEMETRY_KEY = "temperature";
    protected static final String TELEMETRY_VALUE = "42";
    protected static final int CONNECT_TRY_COUNT = 50;
    protected static final int CONNECT_TIMEOUT_MS = 500;
    protected static final ContainerTestSuite containerTestSuite = ContainerTestSuite.getInstance();
    protected static TestRestClient testRestClient;
    protected static TestRestClient remoteHttpClient;

    @BeforeSuite
    public void beforeSuite() {
        if ("false".equals(System.getProperty("runLocal", "false"))) {
            containerTestSuite.start();
        }
        testRestClient = new TestRestClient(TestProperties.getBaseUrl());
        remoteHttpClient = new TestRestClient(TestProperties.getRemoteHttpUrl());
        if (!"kafka".equals(System.getProperty("blackBoxTests.queue", "kafka"))) {
            timeoutMultiplier = 10;
        }
    }

    @AfterSuite()
    public void afterSuite() {
        if (containerTestSuite.isActive()) {
            containerTestSuite.stop();
        }
    }

    protected WsClient subscribeToWebSocket(DeviceId deviceId, String scope, CmdsType property) throws Exception {
        String webSocketUrl = TestProperties.getWebSocketUrl();
        WsClient wsClient = new WsClient(new URI(webSocketUrl + "/api/ws/plugins/telemetry?token=" + testRestClient.getToken()), timeoutMultiplier);
        if (webSocketUrl.matches("^(wss)://.*$")) {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
            wsClient.setSocketFactory(builder.build().getSocketFactory());
        }
        wsClient.connectBlocking();

        JsonObject cmdsObject = new JsonObject();
        cmdsObject.addProperty("entityType", EntityType.DEVICE.name());
        cmdsObject.addProperty("entityId", deviceId.toString());
        cmdsObject.addProperty("scope", scope);
        cmdsObject.addProperty("cmdId", new Random().nextInt(100));

        JsonArray cmd = new JsonArray();
        cmd.add(cmdsObject);
        JsonObject wsRequest = new JsonObject();
        wsRequest.add(property.toString(), cmd);
        wsClient.send(wsRequest.toString());
        wsClient.waitForFirstReply();
        return wsClient;
    }

    protected Map<String, Long> getExpectedLatestValues(long ts) {
        return ImmutableMap.<String, Long>builder()
                .put("booleanKey", ts)
                .put("stringKey", ts)
                .put("doubleKey", ts)
                .put("longKey", ts)
                .build();
    }

    protected boolean verify(WsTelemetryResponse wsTelemetryResponse, String key, Long expectedTs, String expectedValue) {
        List<Object> list = wsTelemetryResponse.getDataValuesByKey(key);
        return expectedTs.equals(list.get(0)) && expectedValue.equals(list.get(1));
    }

    protected boolean verify(WsTelemetryResponse wsTelemetryResponse, String key, String expectedValue) {
        List<Object> list = wsTelemetryResponse.getDataValuesByKey(key);
        return expectedValue.equals(list.get(1));
    }

    protected JsonObject createGatewayConnectPayload(String deviceName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("device", deviceName);
        return payload;
    }

    protected JsonObject createGatewayPayload(String deviceName, long ts) {
        JsonObject payload = new JsonObject();
        payload.add(deviceName, createGatewayTelemetryArray(ts));
        return payload;
    }

    protected JsonArray createGatewayTelemetryArray(long ts) {
        JsonArray telemetryArray = new JsonArray();
        if (ts > 0)
            telemetryArray.add(createPayload(ts));
        else
            telemetryArray.add(createPayload());
        return telemetryArray;
    }

    protected JsonObject createPayload(long ts) {
        JsonObject values = createPayload();
        JsonObject payload = new JsonObject();
        payload.addProperty("ts", ts);
        payload.add("values", values);
        return payload;
    }

    protected JsonObject createPayload() {
        JsonObject values = new JsonObject();
        values.addProperty("stringKey", "value1");
        values.addProperty("booleanKey", true);
        values.addProperty("doubleKey", 42.6);
        values.addProperty("longKey", 73L);

        return values;
    }

    protected Converter createUplink(JsonNode config) {
        Converter converter = new Converter();
        converter.setName("My converter" + StringUtils.randomAlphanumeric(7));
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(config);
        return testRestClient.postConverter(converter);
    }

    protected enum CmdsType {
        TS_SUB_CMDS("tsSubCmds"),
        HISTORY_CMDS("historyCmds"),
        ATTR_SUB_CMDS("attrSubCmds");

        private final String text;

        CmdsType(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    protected JsonNode createPayloadForUplink(Device device, String temperatureValue) throws JsonProcessingException {
        JsonObject values = new JsonObject();
        values.addProperty("deviceName", device.getName());
        values.addProperty("deviceType", device.getType());
        values.addProperty(TELEMETRY_KEY, temperatureValue);
        return mapper.readTree(values.toString());
    }

    protected JsonNode createPayloadForUplink() {
        ObjectNode values = JacksonUtil.newObjectNode();
        values.put(TELEMETRY_KEY, TELEMETRY_VALUE);
        return values;
    }
    protected DeviceProfile updateDeviceProfileWithProvisioningStrategy(DeviceProfile deviceProfile, DeviceProfileProvisionType provisionType) {
        DeviceProfileProvisionConfiguration provisionConfiguration;
        String testProvisionDeviceKey = TEST_PROVISION_DEVICE_KEY;
        deviceProfile.setProvisionType(provisionType);
        switch(provisionType) {
            case ALLOW_CREATE_NEW_DEVICES:
                provisionConfiguration = new AllowCreateNewDevicesDeviceProfileProvisionConfiguration(TEST_PROVISION_DEVICE_SECRET);
                break;
            case CHECK_PRE_PROVISIONED_DEVICES:
                provisionConfiguration = new CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration(TEST_PROVISION_DEVICE_SECRET);
                break;
            default:
            case DISABLED:
                testProvisionDeviceKey = null;
                provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
                break;
        }
        DeviceProfileData deviceProfileData = deviceProfile.getProfileData();
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setProvisionDeviceKey(testProvisionDeviceKey);
        return testRestClient.postDeviceProfile(deviceProfile);
    }

}
