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

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.sample.pubSub.TestTopicListener;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.AWS_IOT;
import static org.thingsboard.server.msa.prototypes.AwsIotIntegrationPrototypes.defaultConfig;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.downlinkConverterPrototype;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.uplinkConverterPrototype;

@Slf4j
public class AwsIotIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-1234599";
    private static final String SECRET_KEY = "secret-key-1234599";

    private static final String CONFIG_CONVERTER = "var payloadStr = decodeToString(payload);\n" +
            "var data = decodeToJson(payload);\n" +
            "var result = {};\n" +
            "var topic = metadata['topic'].split('/');\n" +
            "result.deviceName = topic[1];\n" +
            "if (topic[2] == 'temperature'){\n" +
            "    result.deviceType = 'termostat';\n" +
            "    result.telemetry = {temperature: data.value};\n" +
            "}\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "function decodeToJson(payload) {   \n" +
            "   var str = decodeToString(payload);\n" +
            "   var data = JSON.parse(str);\n" +
            "   return data;\n" +
            "}\n" +
            "\n" +
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
                    "            topic: 'sensors/device/upload'\n" +
                    "    }\n" +
                    "};\n" +
                    "return result;");
    private Converter uplinkConverter;
    private Converter downlinkConverter;
    private RuleChainId defaultRuleChainId;
    private static final String CLIENT_ENDPOINT = System.getProperty("blackBoxTests.aws.endpoint", "");;
    private static final String ROOT_CA = System.getProperty("blackBoxTests.aws.rootCA", "");
    private static final String CERTIFICATE = System.getProperty("blackBoxTests.aws.cert", "");
    private static final String PRIVATE_KEY = System.getProperty("blackBoxTests.aws.privateKey", "");
    @BeforeClass
    public void beforeClass() {
        if (Boolean.parseBoolean(System.getProperty("blackBoxTests.integrations.skip", "true"))) {
            throw new SkipException("Aws iot integration tests are skipped");
        }
    }

    @BeforeMethod
    public void setUp() {
        JsonNode configConverter = new ObjectMapper().createObjectNode().put("decoder", CONFIG_CONVERTER);
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
        JsonNode configuration = getIntegrationConfig();
        integration = Integration.builder()
                .type(AWS_IOT)
                .name("aws_iot_" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(configuration)
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

        String value = "13";
        String content = createPayloadForUplink(value).toString();

        //send payload to aws iot instance
        AWSIotMqttClient awsIotClient = getAwsIotClient();
        awsIotClient.connect();
        awsIotClient.publish("sensors/" + device.getName() + "/temperature", content);

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> !testRestClient.getTimeseriesKeys(device.getId()).isEmpty());

        List<TsKvEntry> latestTimeseries = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        assertThat(latestTimeseries).hasSize(1);
        assertThat(latestTimeseries.get(0).getKey()).isEqualTo(TELEMETRY_KEY);
        assertThat(latestTimeseries.get(0).getValue().toString()).isEqualTo(value);

        //upload second telemetry to aws iot instance
        String value2 = "45";
        String content2 = createPayloadForUplink(value2).toString();

        awsIotClient.publish("sensors/" + device.getName() + "/temperature", content2);
        awsIotClient.disconnect();

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> value2.equals(testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY)).get(0).getValue().toString()));
    }

    @Test
    public void checkDownlinkMessageWasSent() throws Exception {
        JsonNode configuration = getIntegrationConfig();
        integration = Integration.builder()
                .type(AWS_IOT)
                .name("aws_iot_" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(configuration)
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

        //subscribe for aws iot topic
        AWSIotMqttClient awsIotClient = getAwsIotClient();
        awsIotClient.connect();

        TopicListener topicListener = new TopicListener("sensors/device/upload", AWSIotQos.QOS0);
        awsIotClient.subscribe(topicListener, true);

        //add downlink node
        RuleChainId ruleChainId = createRootRuleChainWithIntegrationDownlinkNode(integration.getId());

        //create 4 attributes (stringKey, booleanKey, doubleKey, longKey)
        JsonNode attributes = mapper.readTree(createPayload().toString());
        testRestClient.saveEntityAttributes(DEVICE, device.getId().toString(), SHARED_SCOPE, attributes);

        RuleChainMetaData ruleChainMetadata = testRestClient.getRuleChainMetadata(ruleChainId);
        RuleNode downlinkNode = ruleChainMetadata.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode")).findFirst().get();
        waitTillRuleNodeReceiveMsg(downlinkNode.getId(), EventType.DEBUG_RULE_NODE, integration.getTenantId(), "ATTRIBUTES_UPDATED");

        //check downlink message
        JsonNode actual = JacksonUtil.toJsonNode(topicListener.getMessages().take().getStringPayload());

        assertThat(actual.get("stringKey")).isEqualTo(attributes.get("stringKey"));
        assertThat(actual.get("booleanKey")).isEqualTo(attributes.get("booleanKey"));
        assertThat(actual.get("doubleKey")).isEqualTo(attributes.get("doubleKey"));
        assertThat(actual.get("longKey")).isEqualTo(attributes.get("longKey"));
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "mqtt_";
    }

    private static class TopicListener extends TestTopicListener {
        private final BlockingQueue<AWSIotMessage> messages;

        private TopicListener(String topic, AWSIotQos qos) {
            super(topic, qos);
            messages = new ArrayBlockingQueue<>(100);
        }

        @Override
        public void onMessage(AWSIotMessage message) {
            log.info("Message received[{}]", message);
            messages.add(message);
        }

        public BlockingQueue<AWSIotMessage> getMessages() {
            return messages;
        }
    }

    private JsonNode getIntegrationConfig() throws IOException {
        String rootCA =new String(Files.readAllBytes(Paths.get(ROOT_CA))).replace("\n", "\\n");
        String cert = new String(Files.readAllBytes(Paths.get(CERTIFICATE))).replace("\n", "\\n");
        String privateKey = new String(Files.readAllBytes(Paths.get(PRIVATE_KEY))).replace("\n", "\\n");
        return defaultConfig(CLIENT_ENDPOINT, rootCA, cert, privateKey);
    }
    private JsonNode createPayloadForUplink(String value) {
        ObjectNode values = JacksonUtil.newObjectNode();
        values.put("value", value);
        return values;
    }
    private AWSIotMqttClient getAwsIotClient() {
        String clientId = RandomStringUtils.randomAlphanumeric(10);

        SampleUtil.KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(CERTIFICATE, PRIVATE_KEY);
        AWSIotMqttClient awsIotClient = new AWSIotMqttClient(CLIENT_ENDPOINT, clientId, pair.keyStore, pair.keyPassword);

        if (awsIotClient == null) {
            throw new IllegalArgumentException("Failed to construct client due to missing certificate or credentials.");
        }
        return awsIotClient;
    }
}
