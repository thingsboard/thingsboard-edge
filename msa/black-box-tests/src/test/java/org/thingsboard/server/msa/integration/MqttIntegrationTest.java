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
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.ContainerTestSuite;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MqttIntegrationTest extends AbstractContainerTest {
    public static final String SERVICE_NAME = "broker";
    public static final int SERVICE_PORT = 1883;
    private static final String ROUTING_KEY = "routing-key-1234567";
    private static final String SECRET_KEY = "secret-key-1234567";
    private static final String LOGIN = "tenant@thingsboard.org";
    private static final String PASSWORD = "tenant";
    private static final String TOPIC = "tb/mqtt/device";
    private static final String CONFIG_INTEGRATION = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"" + SERVICE_NAME + "\",\n" +
            "    \"port\":" + SERVICE_PORT + " ,\n" +
            "    \"cleanSession\": true,\n" +
            "    \"ssl\": false,\n" +
            "    \"connectTimeoutSec\": 30,\n" +
            "    \"clientId\": \"\",\n" +
            "    \"maxBytesInMessage\": 32368,\n" +
            "    \"credentials\": {\n" +
            "      \"type\": \"anonymous\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"downlinkTopicPattern\": \"${topic}\",\n" +
            "  \"topicFilters\": [\n" +
            "    {\n" +
            "      \"filter\": \"" + TOPIC + "\",\n" +
            "      \"qos\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metadata\": {}\n" +
            "}";
    private static final String CONFIG_CONVERTER = "var payloadStr = decodeToString(payload);\n" +
            "var data = JSON.parse(payloadStr);\n" +
            "var topicPattern = '" + TOPIC + "';\n" +
            "\n" +
            "var deviceName =  '" + "DEVICE_NAME" + "';\n" +
            "var deviceType = 'DEFAULT';\n" +
            "var result = {\n" +
            "   deviceName: deviceName,\n" +
            "   deviceType: deviceType,\n" +
            "   telemetry: {\n" +
            "       temperature: data.temperature,\n" +
            "   }\n" +
            "};\n" +
            "\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "\n" +
            "function decodeToJson(payload) {\n" +
            "   var str = decodeToString(payload);\n" +
            "\n" +
            "   var data = JSON.parse(str);\n" +
            "   return data;\n" +
            "}\n" +
            "return result;";
    private String host;
    private Integer port;

    @Before
    public void setUp() throws Exception {
        host = ContainerTestSuite.getTestContainer().getServiceHost(SERVICE_NAME, SERVICE_PORT);
        port = ContainerTestSuite.getTestContainer().getServicePort(SERVICE_NAME, SERVICE_PORT);
    }

    @Test
    public void telemetryUploadWithLocalIntegration() throws Exception {

        restClient.login(LOGIN, PASSWORD);
        Device device = createDevice("mqtt_");
        JsonNode configConverter = new ObjectMapper().createObjectNode().put("decoder",
                CONFIG_CONVERTER.replaceAll("DEVICE_NAME", device.getName()));
        boolean isRemote = false;
        Converter savedConverter = createUplink(configConverter);
        Integration integration = createIntegration(isRemote);
        integration.setDefaultConverterId(savedConverter.getId());
        integration = restClient.saveIntegration(integration);

        sendMessageToBroker();

        boolean hasTelemetry = false;
        for (int i = 0; i < CONNECT_TRY_COUNT; i++) {
            Thread.sleep(CONNECT_TIMEOUT_MS);
            if (restClient.getTimeseriesKeys(device.getId()).isEmpty()) continue;
            hasTelemetry = true;
            break;
        }
        Assert.assertTrue("Device doesn't has telemetry", hasTelemetry);

        List<TsKvEntry> latestTimeseries = restClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        Assert.assertFalse(latestTimeseries.isEmpty());
        Assert.assertEquals(TELEMETRY_KEY, latestTimeseries.get(0).getKey());
        Assert.assertEquals(TELEMETRY_VALUE, latestTimeseries.get(0).getValue().toString());

        deleteAllObject(device, integration);
    }

    @Test
    public void telemetryUploadWithRemoteIntegration() throws Exception {
        restClient.login(LOGIN, PASSWORD);
        Device device = createDevice("mqtt_");
        JsonNode configConverter = new ObjectMapper().createObjectNode().put("decoder",
                CONFIG_CONVERTER.replaceAll("DEVICE_NAME", device.getName()));
        boolean isRemote = true;
        Converter savedConverter = createUplink(configConverter);
        Integration integration = createIntegration(isRemote);
        integration.setDefaultConverterId(savedConverter.getId());
        integration = restClient.saveIntegration(integration);
        sendMessageToBroker();

        TenantId tenantId = restClient.getIntegrations(new PageLink(1024)).getData().get(0).getTenantId();
        boolean isConnected = false;
        for (int i = 0; i < CONNECT_TRY_COUNT; i++) {
            Thread.sleep(CONNECT_TIMEOUT_MS);
            PageData<EventInfo> events = restClient.getEvents(integration.getId(), tenantId, new TimePageLink(1024));
            if (events.getData().isEmpty()) continue;
            isConnected = true;
            break;
        }
        Assert.assertTrue("RPC have not connected to TB", isConnected);

        boolean hasTelemetry = false;
        for (int i = 0; i < CONNECT_TRY_COUNT; i++) {
            Thread.sleep(CONNECT_TIMEOUT_MS);
            if (restClient.getTimeseriesKeys(device.getId()).isEmpty()) continue;
            hasTelemetry = true;
            break;
        }
        Assert.assertTrue("Device doesn't has telemetry", hasTelemetry);

        List<TsKvEntry> latestTimeseries = restClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        Assert.assertEquals(TELEMETRY_KEY, latestTimeseries.get(0).getKey());
        Assert.assertEquals(TELEMETRY_VALUE, latestTimeseries.get(0).getValue().toString());

        deleteAllObject(device, integration);
    }

    private Integration createIntegration(boolean isRemote) throws JsonProcessingException {
        Integration integration = new Integration();
        JsonNode conf = mapper.readTree(CONFIG_INTEGRATION);
        log.info(conf.toString());
        integration.setConfiguration(conf);
        integration.setName("mqtt");
        integration.setType(IntegrationType.MQTT);
        integration.setRoutingKey(ROUTING_KEY);
        integration.setSecret(SECRET_KEY);
        integration.setEnabled(true);
        integration.setRemote(isRemote);
        integration.setDebugMode(true);
        integration.setAllowCreateDevicesOrAssets(true);
        return integration;
    }

    void sendMessageToBroker() throws MqttException, InterruptedException, JsonProcessingException {
        String content = createPayloadForUplink().toString();
        int qos = 0;

        String broker = "tcp://" + host + ":" + port;
        String subClientId = StringUtils.randomAlphanumeric(10);
        MemoryPersistence persistence = new MemoryPersistence();

        MqttClient sampleClientSubs = new MqttClient(broker, subClientId, persistence);

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setKeepAliveInterval(30);
        connOpts.setCleanSession(true);

        sampleClientSubs.connect(connOpts);
        AtomicBoolean check = new AtomicBoolean(false);
        sampleClientSubs.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                log.trace(throwable.getMessage());
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) {
                check.set(mqttMessage.toString().equals(content));
                log.trace("s = {}, message = {}", s, mqttMessage);
            }

            @SneakyThrows
            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                check.set(true);
                log.trace(iMqttDeliveryToken.getMessage().toString());
            }
        });
        sampleClientSubs.subscribe(TOPIC);

        try {
            String prodClientId = StringUtils.randomAlphanumeric(10);
            MqttClient sampleClient = new MqttClient(broker, prodClientId, persistence);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            sampleClient.connect(options);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            message.setRetained(true);
            sampleClient.publish(TOPIC, message);
            sampleClient.disconnect();
            sampleClient.close();
        } catch (MqttException me) {
            me.printStackTrace();
        }
        for (int i = 0; i < CONNECT_TRY_COUNT; i++) {
            Thread.sleep(CONNECT_TIMEOUT_MS);
            if (check.get()) break;
        }
        Assert.assertTrue("Broker doesn't get message", check.get());
    }

}
