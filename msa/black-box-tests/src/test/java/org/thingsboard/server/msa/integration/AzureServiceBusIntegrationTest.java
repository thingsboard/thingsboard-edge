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

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.AZURE_SERVICE_BUS;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.downlinkConverterPrototype;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.uplinkConverterPrototype;

@Slf4j
public class AzureServiceBusIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-azure-service-bus";
    private static final String SECRET_KEY = "secret-key-azure-service-bus";
    protected static final String HUMIDITY_KEY = "humidity";
    private static final String CONNECTION_STRING = System.getProperty("blackBoxTests.azureServiceBusConnectionString", "");
    private static final String TOPIC_NAME = System.getProperty("blackBoxTests.azureServiceBusTopicName", "");
    private static final String SUBSCRIPTION_NAME = System.getProperty("blackBoxTests.azureServiceBusSubName", "");
    private static final String DOWNLINK_CONNECTION_STRING = System.getProperty("blackBoxTests.azureServiceBusDownlinkConnectionString", "");
    private static final String DOWNLINK_TOPIC_NAME = System.getProperty("blackBoxTests.azureServiceBusDownlinkTopicName", "");
    private static final String DOWNLINK_TOPIC_SUB_NAME = System.getProperty("blackBoxTests.azureServiceBusDownlinkSubName", "");
    private static final String INTEGRATION_CONFIG = "{\"clientConfiguration\":{" +
            "\"connectionString\":\"" + CONNECTION_STRING + "\"," +
            "\"topicName\":\"" + TOPIC_NAME + "\"," +
            "\"subName\":\"" + SUBSCRIPTION_NAME + "\"," +
            "\"downlinkConnectionString\":\"" + DOWNLINK_CONNECTION_STRING + "\"," +
            "\"downlinkTopicName\":\"" + DOWNLINK_TOPIC_NAME + "\"}," +
            "\"metadata\":{}}";

    private static final String TEXT_CONVERTER_CONFIG = "var strArray = decodeToString(payload);\n" +
            "var payloadArray = strArray.replace(/\\\"/g, \"\").replace(/\\s/g, \"\").replace(/\\\\n/g, \"\").split(',');\n" +
            "var telemetryPayload = {};\n" +
            "for (var i = 2; i < payloadArray.length; i = i + 2) {\n" +
            "    var telemetryKey = payloadArray[i];\n" +
            "    var telemetryValue = parseFloat(payloadArray[i + 1]);\n" +
            "    telemetryPayload[telemetryKey] = telemetryValue;\n" +
            "}\n" +
            "var result = {\n" +
            "    deviceName: payloadArray[0],\n" +
            "    deviceType: payloadArray[1],\n" +
            "    telemetry: telemetryPayload,\n" +
            "    attributes: {}\n" +
            "  };\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "return result;";

    private final JsonNode DOWNLINK_CONVERTER_CONFIG = mapper
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
                    "        deviceId: 'myDevice'\n" +
                    "    }\n" +
                    "};\n" +
                    "return result;");

    private final BlockingQueue<String> messageList = new ArrayBlockingQueue<>(100);
    private Converter uplinkConverter;
    private Converter downlinkConverter;
    private RuleChainId defaultRuleChainId;

    @BeforeClass
    public static void beforeClass() {
        if (Boolean.parseBoolean(System.getProperty("blackBoxTests.integrations.skip", "true"))) {
            throw new SkipException("AzureServiceBusIntegrationTest is skipped");
        }
    }
    @AfterMethod
    public void tearDown()  {
        testRestClient.setRootRuleChain(defaultRuleChainId);
    }

    @BeforeMethod
    public void setUp() {
        JsonNode configConverter = new ObjectMapper().createObjectNode().put("decoder", TEXT_CONVERTER_CONFIG);
        uplinkConverter = testRestClient.postConverter(uplinkConverterPrototype(configConverter));
        downlinkConverter = testRestClient.postConverter(downlinkConverterPrototype(DOWNLINK_CONVERTER_CONFIG));
        defaultRuleChainId = getDefaultRuleChainId();
    }

    @Test
    public void telemetryUploadWithLocalIntegration() throws Exception {
        integration = Integration.builder()
                .type(AZURE_SERVICE_BUS)
                .name("service_bus_" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(JacksonUtil.toJsonNode(INTEGRATION_CONFIG))
                .defaultConverterId(uplinkConverter.getId())
                .routingKey(ROUTING_KEY + RandomStringUtils.randomAlphanumeric(5))
                .secret(SECRET_KEY + RandomStringUtils.randomAlphanumeric(5))
                .isRemote(false)
                .enabled(true)
                .debugMode(true)
                .allowCreateDevicesOrAssets(true)
                .build();

        integration = testRestClient.postIntegration(integration);
        waitUntilIntegrationStarted(integration.getId(), integration.getTenantId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        String temp = "27.7";
        String humidity = "67";
        sendMessageToServiceBusTopic(device, temp, humidity);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(2, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet(TELEMETRY_KEY, HUMIDITY_KEY),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, temp));
        Assert.assertTrue(verify(actualLatestTelemetry, HUMIDITY_KEY, humidity));
    }

    @Test
    public void checkDownlinkMessageWasSent() throws Exception {
        integration = Integration.builder()
                .type(AZURE_SERVICE_BUS)
                .name("service_bus_" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(JacksonUtil.toJsonNode(INTEGRATION_CONFIG))
                .defaultConverterId(uplinkConverter.getId())
                .downlinkConverterId(downlinkConverter.getId())
                .routingKey(ROUTING_KEY + RandomStringUtils.randomAlphanumeric(5))
                .secret(SECRET_KEY + RandomStringUtils.randomAlphanumeric(5))
                .isRemote(false)
                .enabled(true)
                .debugMode(true)
                .allowCreateDevicesOrAssets(true)
                .build();

        integration = testRestClient.postIntegration(integration);
        waitUntilIntegrationStarted(integration.getId(), integration.getTenantId());

        //subscribe for service bus topic
        try(ServiceBusProcessorClient serviceBusProcessorClient = getDownlinkProcessorClient()) {
            serviceBusProcessorClient.start();

            //add downlink node
            RuleChainId ruleChainId = createRootRuleChainWithIntegrationDownlinkNode(integration.getId());

            //create 4 attributes (stringKey, booleanKey, doubleKey, longKey)
            JsonNode attributes = mapper.readTree(createPayload().toString());
            testRestClient.saveEntityAttributes(DEVICE, device.getId().toString(), SHARED_SCOPE, attributes);

            RuleChainMetaData ruleChainMetadata = testRestClient.getRuleChainMetadata(ruleChainId);
            RuleNode downlinkNode = ruleChainMetadata.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode")).findFirst().get();
            waitTillRuleNodeReceiveMsg(downlinkNode.getId(), EventType.DEBUG_RULE_NODE, integration.getTenantId(), "ATTRIBUTES_UPDATED");

            //check downlink message
            Awaitility
                    .await()
                    .alias("Get message from azure service bus topic")
                    .atMost(20, TimeUnit.SECONDS)
                    .until(() -> { return messageList.size() == 1; });;

            JsonNode actual = JacksonUtil.toJsonNode(messageList.poll());

            assertThat(actual.get("stringKey")).isEqualTo(attributes.get("stringKey"));
            assertThat(actual.get("booleanKey")).isEqualTo(attributes.get("booleanKey"));
            assertThat(actual.get("doubleKey")).isEqualTo(attributes.get("doubleKey"));
            assertThat(actual.get("longKey")).isEqualTo(attributes.get("longKey"));
        }
    }

    private ServiceBusProcessorClient getDownlinkProcessorClient() {
        return new ServiceBusClientBuilder()
                .connectionString(DOWNLINK_CONNECTION_STRING)
                .processor()
                .topicName(DOWNLINK_TOPIC_NAME)
                .subscriptionName(DOWNLINK_TOPIC_SUB_NAME)
                .processMessage(this::processMessage)
                .processError(error -> log.error("It was trouble when receiving: " + error.getException().getMessage()))
                .buildProcessorClient();
    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        messageList.add(new String(message.getBody().toBytes()));
    }

    void sendMessageToServiceBusTopic(Device device, String temp, String humidity) {
        try (ServiceBusSenderClient serviceBusSenderClient = new ServiceBusClientBuilder()
                .connectionString(CONNECTION_STRING)
                .sender()
                .topicName(TOPIC_NAME)
                .buildClient()){
            serviceBusSenderClient.sendMessage(new ServiceBusMessage(String.format( "%s,default,temperature,%s,humidity,%s", device.getName(), temp, humidity)));
        }
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "azure_service_bus_";
    }
}
