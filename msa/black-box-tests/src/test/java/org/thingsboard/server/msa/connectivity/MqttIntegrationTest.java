package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
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
    private static final String routingKey = "routing-key-123456";
    private static final String secretKey = "secret-key-123456";
    private static final String login = "tenant@thingsboard.org";
    private static final String password = "tenant";
    private static final String key = "temperature";
    private static final String value = "42";
    private static String host = "broker";
    private static Integer port = 1883;
    private static final String topic = "tb/mqtt/device";
    private static final String deviceName = "mqtt_device";

    private static final String CONFIG_INTEGRATION = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"" + host + "\",\n" +
            "    \"port\":" + port + " ,\n" +
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
            "      \"filter\": \"" + topic + "\",\n" +
            "      \"qos\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metadata\": {}\n" +
            "}";

    private static final JsonNode CUSTOM_UPLINK_CONVERTER_CONFIGURATION = new ObjectMapper()
            .createObjectNode().put("decoder",
                    "var payloadStr = decodeToString(payload);\n" +
                            "var data = JSON.parse(payloadStr);\n" +
                            "var topicPattern = '" + topic + "';\n" +
                            "\n" +
                            "var deviceName =  '" + deviceName + "';\n" +
                            "var deviceType = 'default';\n" +
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
                            "return result;");

    @Test
    public void telemetryUploadWithIntegration() throws Exception {

        restClient.login(login, password);
        Device device = createDevice(deviceName);
        getBrokerAddress();
        boolean isRemote = false;
        Converter savedConverter = createUplink();
        Integration integration = createIntegration(isRemote);
        integration.setDefaultConverterId(savedConverter.getId());
        restClient.saveIntegration(integration);
        log.info(restClient.getIntegrationByRoutingKey(routingKey).get().toString());

        log.info(String.valueOf(restClient.getTenantDevices("default", new PageLink(1024)).getData().size()));
        Thread.sleep(10000);

        anotherVariable();

        log.info(restClient.getDeviceById(device.getId()).get().toString());
        while (restClient.getTimeseriesKeys(device.getId()).isEmpty()) {
            log.info("!" + restClient.getLatestTimeseries(device.getId(), List.of(key)).toString());
            for (int i = 0; i < 1e9; i++) {}
        }
        List<TsKvEntry> latestTimeseries = restClient.getLatestTimeseries(device.getId(), List.of(key));
        log.info("!" + latestTimeseries.toString());
        log.info("!!!!!" + restClient.getTimeseriesKeys(device.getId()));
        log.info(restClient.getTenantDevices("default", new PageLink(1024)).getData().toString());
        log.info(String.valueOf(restClient.getTenantDevices("", new PageLink(1024)).getData().size()));
        Assert.assertFalse(latestTimeseries.isEmpty());
        Assert.assertEquals(key,  latestTimeseries.get(0).getKey());
        Assert.assertEquals(value,  latestTimeseries.get(0).getValue().toString());

        restClient.deleteDevice(device.getId());
        ConverterId idForDelete = integration.getDefaultConverterId();
        restClient.deleteIntegration(restClient.getIntegrationByRoutingKey(routingKey).get().getId());
        restClient.deleteConverter(idForDelete);
    }

    @Test
    public void telemetryUploadWithRemoteIntegration() throws Exception {
        restClient.login(login, password);
        Device device = createDevice("mqtt_device");
        getBrokerAddress();
        boolean isRemote = true;
        Converter savedConverter = createUplink();
        Integration integration = createIntegration(isRemote);
        integration.setDefaultConverterId(savedConverter.getId());
        restClient.saveIntegration(integration);
        log.info(restClient.getIntegrationByRoutingKey(routingKey).get().toString());

        anotherVariable();

        IntegrationId integrationId = restClient.getIntegrationByRoutingKey(routingKey).get().getId();
        TenantId tenantId = restClient.getIntegrations(new PageLink(1024)).getData().get(0).getTenantId();
        boolean isConnected = false;
        for (int i=0; i<50; i++) {
            Thread.sleep(500);
            PageData<Event> events = restClient.getEvents(integrationId, tenantId, new TimePageLink(1024));
            if (events.getData().isEmpty()) continue;
            isConnected = true;
        }
        Assert.assertTrue("RPC have not connected to TB", isConnected);

        List<TsKvEntry> latestTimeseries = restClient.getLatestTimeseries(device.getId(), List.of(key));
        log.info("!" + latestTimeseries.toString());
        log.info("!!!!!" + restClient.getTimeseriesKeys(device.getId()));
        log.info(restClient.getTenantDevices("default", new PageLink(1024)).getData().toString());
        log.info(String.valueOf(restClient.getTenantDevices("", new PageLink(1024)).getData().size()));
        Assert.assertFalse(latestTimeseries.isEmpty());
        Assert.assertEquals(key,  latestTimeseries.get(0).getKey());
        Assert.assertEquals(value,  latestTimeseries.get(0).getValue().toString());

        restClient.deleteDevice(device.getId());
        ConverterId idForDelete = integration.getDefaultConverterId();
        restClient.deleteIntegration(integrationId);
        restClient.deleteConverter(idForDelete);
    }

    private void getBrokerAddress() {
        host = ContainerTestSuite.getTestContainer().getServiceHost("broker", 1883);
        port = ContainerTestSuite.getTestContainer().getServicePort("broker", 1883);
        log.info("{}:{}", host, port);
    }
    private Integration createIntegration(boolean isRemote) throws JsonProcessingException {
        Integration integration = new Integration();
        JsonNode conf = mapper.readTree(CONFIG_INTEGRATION);
        log.info(conf.toString());
        integration.setConfiguration(conf);
        integration.setName("mqtt");
        integration.setType(IntegrationType.MQTT);
        integration.setRoutingKey(routingKey);
        integration.setSecret(secretKey);
        integration.setEnabled(true);
        integration.setRemote(isRemote);
        integration.setDebugMode(true);
        integration.setAllowCreateDevicesOrAssets(true);
        log.info(integration.toString());
        return integration;
    }

    private Converter createUplink() {
        Converter converter = new Converter();
        converter.setName("My converter" + RandomStringUtils.randomAlphanumeric(7));
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_UPLINK_CONVERTER_CONFIGURATION);
        return restClient.saveConverter(converter);
    }

    protected JsonNode createPayloadForUplink() throws JsonProcessingException {
        JsonObject values = new JsonObject();
        values.addProperty(key, value);
        return mapper.readTree(values.toString());
    }

    public void anotherVariable() throws MqttException, InterruptedException, JsonProcessingException {
        String content = createPayloadForUplink().toString();
        int qos = 0;

        log.info("START MY BOY");
        String broker = "tcp://" + host + ":" + port;
        String subClientId = RandomStringUtils.randomAlphanumeric(10);
        MemoryPersistence persistence = new MemoryPersistence();

        MqttClient sampleClientSubs = new MqttClient(broker, subClientId, persistence);

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setKeepAliveInterval(30);
        log.info(connOpts.toString());
        connOpts.setCleanSession(true);

        log.info("checking");
        log.info("Mqtt Connecting to broker: " + broker);

        sampleClientSubs.connect(connOpts);
        log.info("Mqtt");
        AtomicBoolean check = new AtomicBoolean(false);
        sampleClientSubs.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                log.info(throwable.getMessage());
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) {
                check.set(mqttMessage.toString().equals(content));
                log.info("s = {}, message = {}", s, mqttMessage);
            }

            @SneakyThrows
            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                check.set(true);
                log.info(iMqttDeliveryToken.getMessage().toString());
            }
        });
        sampleClientSubs.subscribe(topic);
        log.info("Subscribed");
        log.info("Listening");


        try {
            String prodClientId = RandomStringUtils.randomAlphanumeric(10);
            MqttClient sampleClient = new MqttClient(broker, prodClientId, persistence);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            log.info("Connecting to broker: " + broker);
            sampleClient.connect(options);
            log.info("Connected to broker");
            log.info("Publishing message:" + content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            message.setRetained(true);
            sampleClient.publish(topic, message);
            log.info("Message published");
            sampleClient.disconnect();
            sampleClient.close();
        } catch (MqttException me) {
            log.info("reason " + me.getReasonCode());
            log.info("msg " + me.getMessage());
            log.info("loc " + me.getLocalizedMessage());
            log.info("cause " + me.getCause());
            log.info("excep " + me);
            me.printStackTrace();
        }
        while (!check.get()) {
            Thread.sleep(500);
            log.info(String.valueOf(sampleClientSubs.isConnected()));
            log.info("wait message");
        }
    }

}
