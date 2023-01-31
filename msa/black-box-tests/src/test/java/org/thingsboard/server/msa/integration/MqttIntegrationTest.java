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
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.msa.TestProperties;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.MQTT;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.downlinkConverterPrototype;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.uplinkConverterPrototype;
import static org.thingsboard.server.msa.prototypes.MQTTIntegrationPrototypes.configWithBasicCreds;
import static org.thingsboard.server.msa.prototypes.MQTTIntegrationPrototypes.defaultConfig;

@Slf4j
public class MqttIntegrationTest extends AbstractIntegrationTest {
    public static final String SERVICE_NAME = "broker";
    public static final int SERVICE_PORT = 1883;
    private static final String ROUTING_KEY = "routing-key-1234567";
    private static final String SECRET_KEY = "secret-key-1234567";
    private static final String TOPIC = "tb/mqtt/device";
    private static final String DOWNLINK_TOPIC = "tb/mqtt/device/upload";

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

    private final JsonNode DOWNLINK_CONVERTER_CONFIGURATION = mapper
            .createObjectNode().put("encoder", "var data = {};\n" +
                    "data.booleanKey = msg.booleanKey;\n" +
                    "data.stringKey = msg.stringKey;\n" +
                    "data.doubleKey = msg.doubleKey;\n" +
                    "data.longKey = msg.longKey;\n" +
                    "\n" +
                    "data.devSerialNumber = metadata['ss_serialNumber'];\n" +
                    "var result = {\n" +
                    "    contentType: \"JSON\",\n" +
                    "    data: JSON.stringify(data),\n" +
                    "    metadata: {\n" +
                    "            topic: metadata['deviceType']+'/'+metadata['deviceName']+'/upload'\n" +
                    "    }\n" +
                    "};\n" +
                    "return result;");

    private Converter uplinkConverter;
    private Converter downlinkConverter;
    private RuleChainId defaultRuleChainId;

    @BeforeMethod
    public void setUp() {
        JsonNode configConverter = new ObjectMapper().createObjectNode().put("decoder",
                CONFIG_CONVERTER.replaceAll("DEVICE_NAME", device.getName()));
        uplinkConverter = testRestClient.postConverter(uplinkConverterPrototype(configConverter));
        downlinkConverter = testRestClient.postConverter(downlinkConverterPrototype(DOWNLINK_CONVERTER_CONFIGURATION));
        defaultRuleChainId = getDefaultRuleChainId();
    }

    @AfterMethod
    public void tearDown()  {
        testRestClient.setRootRuleChain(defaultRuleChainId);
    }
    @Test
    public void telemetryUploadWithLocalIntegration() throws Exception {
        integration = Integration.builder()
                .type(MQTT)
                .name("mqtt" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(defaultConfig(SERVICE_NAME, SERVICE_PORT, TOPIC))
                .defaultConverterId(uplinkConverter.getId())
                .routingKey(ROUTING_KEY)
                .secret(SECRET_KEY)
                .isRemote(false)
                .enabled(true)
                .debugMode(true)
                .allowCreateDevicesOrAssets(true)
                .build();

        integration = testRestClient.postIntegration(integration);
        waitUntilIntegrationStarted(integration.getId(), integration.getTenantId());

        sendMessageToBroker();

        boolean hasTelemetry = false;
        for (int i = 0; i < CONNECT_TRY_COUNT; i++) {
            Thread.sleep(CONNECT_TIMEOUT_MS);
            if (testRestClient.getTimeseriesKeys(device.getId()).isEmpty()) continue;
            hasTelemetry = true;
            break;
        }
        Assert.assertTrue(hasTelemetry);

        List<TsKvEntry> latestTimeseries = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        Assert.assertFalse(latestTimeseries.isEmpty());
        Assert.assertEquals(TELEMETRY_KEY, latestTimeseries.get(0).getKey());
        Assert.assertEquals(TELEMETRY_VALUE, latestTimeseries.get(0).getValue().toString());
    }
    @Test
    public void telemetryUploadWithBasicCreds() throws Exception {
        integration = Integration.builder()
                .type(MQTT)
                .name("mqtt" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(configWithBasicCreds(SERVICE_NAME, SERVICE_PORT, TOPIC))
                .defaultConverterId(uplinkConverter.getId())
                .routingKey(ROUTING_KEY)
                .secret(SECRET_KEY)
                .isRemote(false)
                .enabled(true)
                .debugMode(true)
                .allowCreateDevicesOrAssets(true)
                .build();

        integration = testRestClient.postIntegration(integration);
        waitUntilIntegrationStarted(integration.getId(), integration.getTenantId());

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setKeepAliveInterval(30);
        connOpts.setCleanSession(true);
        connOpts.setUserName("userName");
        connOpts.setPassword("pass".toCharArray());

        sendMessageToBroker(connOpts);

        boolean hasTelemetry = false;
        for (int i = 0; i < CONNECT_TRY_COUNT; i++) {
            Thread.sleep(CONNECT_TIMEOUT_MS);
            if (testRestClient.getTimeseriesKeys(device.getId()).isEmpty()) continue;
            hasTelemetry = true;
            break;
        }
        Assert.assertTrue(hasTelemetry);

        List<TsKvEntry> latestTimeseries = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        Assert.assertFalse(latestTimeseries.isEmpty());
        Assert.assertEquals(TELEMETRY_KEY, latestTimeseries.get(0).getKey());
        Assert.assertEquals(TELEMETRY_VALUE, latestTimeseries.get(0).getValue().toString());
    }

    @Test
    public void telemetryUploadWithRemoteIntegration() throws Exception {
        integration = Integration.builder()
                .type(MQTT)
                .name("mqtt" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(defaultConfig(SERVICE_NAME, SERVICE_PORT, TOPIC))
                .defaultConverterId(uplinkConverter.getId())
                .routingKey(ROUTING_KEY)
                .secret(SECRET_KEY)
                .isRemote(false)
                .enabled(true)
                .debugMode(true)
                .allowCreateDevicesOrAssets(true)
                .build();

        integration = testRestClient.postIntegration(integration);
        waitUntilIntegrationStarted(integration.getId(), integration.getTenantId());

        sendMessageToBroker();

        boolean hasTelemetry = false;
        for (int i = 0; i < CONNECT_TRY_COUNT; i++) {
            Thread.sleep(CONNECT_TIMEOUT_MS);
            if (testRestClient.getTimeseriesKeys(device.getId()).isEmpty()) continue;
            hasTelemetry = true;
            break;
        }
        Assert.assertTrue(hasTelemetry);

        List<TsKvEntry> latestTimeseries = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        Assert.assertEquals(TELEMETRY_KEY, latestTimeseries.get(0).getKey());
        Assert.assertEquals(TELEMETRY_VALUE, latestTimeseries.get(0).getValue().toString());
    }
    @Test
    public void checkDownlinkMessageWasSent() throws Exception {
        integration = Integration.builder()
                .type(MQTT)
                .name("mqtt" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(defaultConfig(SERVICE_NAME, SERVICE_PORT, DOWNLINK_TOPIC))
                .defaultConverterId(uplinkConverter.getId())
                .downlinkConverterId(downlinkConverter.getId())
                .routingKey(ROUTING_KEY)
                .secret(SECRET_KEY)
                .isRemote(false)
                .enabled(true)
                .debugMode(true)
                .allowCreateDevicesOrAssets(true)
                .build();

        integration = testRestClient.postIntegration(integration);
        waitUntilIntegrationStarted(integration.getId(), integration.getTenantId());

        sendMessageToBroker();

        boolean hasTelemetry = false;
        for (int i = 0; i < CONNECT_TRY_COUNT; i++) {
            Thread.sleep(CONNECT_TIMEOUT_MS);
            if (testRestClient.getTimeseriesKeys(device.getId()).isEmpty()) continue;
            hasTelemetry = true;
            break;
        }
        Assert.assertTrue(hasTelemetry);

        List<TsKvEntry> latestTimeseries = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        Assert.assertEquals(TELEMETRY_KEY, latestTimeseries.get(0).getKey());
        Assert.assertEquals(TELEMETRY_VALUE, latestTimeseries.get(0).getValue().toString());

        //check downlink uploaded after attribute updated
        MqttMessageListener messageListener = new MqttMessageListener();
        MqttClient client = new MqttClient(TestProperties.getMqttBrokerUrl(), StringUtils.randomAlphanumeric(10), new MemoryPersistence());
        client.connect();
        client.subscribe(DOWNLINK_TOPIC, messageListener);

        RuleChainId ruleChainId = createRootRuleChainWithIntegrationDownlinkNode(integration.getId());

        JsonNode attributes = mapper.readTree(createPayload().toString());
        testRestClient.saveEntityAttributes(DEVICE, device.getId().toString(), SHARED_SCOPE, attributes);

        RuleChainMetaData ruleChainMetadata = testRestClient.getRuleChainMetadata(ruleChainId);
        RuleNode integrationNode  = ruleChainMetadata.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode")).findFirst().get();
        waitTillRuleNodeReceiveMsg(integrationNode.getId(), EventType.DEBUG_RULE_NODE, integration.getTenantId(), "ATTRIBUTES_UPDATED");

        //check downlink
        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> { return messageListener.getEvents().size() == 1; });;
        BlockingQueue<MqttEvent> events = messageListener.getEvents();
        JsonNode actual = JacksonUtil.toJsonNode(Objects.requireNonNull(events.poll()).message);

        assertThat(actual.get("stringKey")).isEqualTo(attributes.get("stringKey"));
        assertThat(actual.get("booleanKey")).isEqualTo(attributes.get("booleanKey"));
        assertThat(actual.get("doubleKey")).isEqualTo(attributes.get("doubleKey"));
        assertThat(actual.get("longKey")).isEqualTo(attributes.get("longKey"));
    }

    void sendMessageToBroker() throws MqttException, InterruptedException {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setKeepAliveInterval(30);
        connOpts.setCleanSession(true);

        sendMessageToBroker(connOpts);
    }
    void sendMessageToBroker(MqttConnectOptions connOpts) throws MqttException, InterruptedException {
        String content = createPayloadForUplink().toString();
        int qos = 0;

        String subClientId = StringUtils.randomAlphanumeric(10);
        MemoryPersistence persistence = new MemoryPersistence();

        MqttClient sampleClientSubs = new MqttClient(TestProperties.getMqttBrokerUrl(), subClientId, persistence);

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
            MqttClient sampleClient = new MqttClient(TestProperties.getMqttBrokerUrl(), prodClientId, persistence);
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
        Assert.assertTrue(check.get());
    }

    @Data
    private class MqttMessageListener implements IMqttMessageListener {
        private final BlockingQueue<MqttEvent> events;

        private MqttMessageListener() {
            events = new ArrayBlockingQueue<>(100);
        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) {
            log.info("MQTT message [{}], topic [{}]", mqttMessage.toString(), s);
            events.add(new MqttEvent(s, mqttMessage.toString()));
        }

        public BlockingQueue<MqttEvent> getEvents() {
            return events;
        }
    }

    @Data
    private class MqttEvent {
        private final String topic;
        private final String message;
    }
    @Override
    protected String getDevicePrototypeSufix() {
        return "mqtt_";
    }
}
