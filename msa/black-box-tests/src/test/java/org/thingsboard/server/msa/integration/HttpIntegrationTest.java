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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.path.json.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.HTTP;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.downlinkConverterPrototype;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.uplinkConverterPrototype;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfig;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfigWithSecurityEnabled;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfigWithSecurityHeader;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfigWithSecurityHeader2;


@Slf4j
public class HttpIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-123456";
    private static final String SECRET_KEY = "secret-key-123456";

    private final JsonNode CUSTOM_CONVERTER_CONFIGURATION = mapper
            .createObjectNode().put("decoder", "var data = decodeToJson(payload);\n" +
                    "var deviceName = data.deviceName;\n" +
                    "var deviceType = data.deviceType;\n" +
                    "var result = {\n" +
                    "   deviceName: deviceName,\n" +
                    "   deviceType: deviceType,\n" +
                    "   attributes: {\n" +
                    "       model: data.model,\n" +
                    "       serialNumber: data.param2,\n" +
                    "   },\n" +
                    "   telemetry: {\n" +
                    "       temperature: data.temperature\n" +
                    "   }\n" +
                    "};\n" +
                    "function decodeToString(payload) {\n" +
                    "   return String.fromCharCode.apply(String, payload);\n" +
                    "}\n" +
                    "function decodeToJson(payload) {\n" +
                    "   var str = decodeToString(payload);\n" +
                    "   var data = JSON.parse(str);\n" +
                    "   return data;\n" +
                    "}\n" +
                    "return result;");
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
    private WsClient wsClient;
    private RuleChainId defaultRuleChainId;
    @BeforeMethod
    public void setUp()  {
        uplinkConverter = testRestClient.postConverter(uplinkConverterPrototype(CUSTOM_CONVERTER_CONFIGURATION));
        downlinkConverter = testRestClient.postConverter(downlinkConverterPrototype(DOWNLINK_CONVERTER_CONFIGURATION));

        defaultRuleChainId = getDefaultRuleChainId();
    }
    @AfterMethod
    public void tearDown() throws Exception {
        testRestClient.setRootRuleChain(defaultRuleChainId);
        if (wsClient != null) {
            wsClient.closeBlocking();
        }
    }

    @DataProvider(name = "integrationConfigs")
    public Object[][] integrationConfigs() {
        return new Object [][] { new Object[] { defaultConfig(HTTPS_URL) },
                new Object[] { defaultConfigWithSecurityEnabled(HTTPS_URL) },
                new Object[] { defaultConfigWithSecurityHeader(HTTPS_URL)}
        };
    }

    @Test(dataProvider = "integrationConfigs")
    public void checkTelemetryUploadedWithLocalIntegration(JsonNode config) throws Exception {
        integration = Integration.builder()
                .type(HTTP)
                .name("http" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(config)
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

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        if (!config.get("headersFilter").isEmpty()){
            Map<String, Object> securityHeaders = mapper.readValue(config.get("headersFilter").toString(), new TypeReference<Map<String, Object>>() {});
            testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE), securityHeaders);
        } else {
            testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE));
        }

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey(TELEMETRY_KEY).get(1)).isEqualTo(TELEMETRY_VALUE);
    }

    @Test
    public void checkTelemetryUploadedAfterIntegrationConfigUpdated() throws Exception {
        JsonNode config = defaultConfigWithSecurityHeader(HTTPS_URL);
        integration = Integration.builder()
                .type(HTTP)
                .name("http")
                .configuration(config)
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

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        Map<String, Object> securityHeaders = mapper.readValue(config.get("headersFilter").toString(), new TypeReference<Map<String, Object>>() {});
        testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE), securityHeaders);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey(TELEMETRY_KEY).get(1)).isEqualTo(TELEMETRY_VALUE);

        //update integration with new security header
        JsonNode config2 = defaultConfigWithSecurityHeader2(HTTPS_URL);
        integration.setConfiguration(config2);
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 1);

        String temperatureValue2 = "58";
        Map<String, Object> securityHeaders2 = mapper.readValue(config2.get("headersFilter").toString(), new TypeReference<Map<String, Object>>() {});
        testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue2), securityHeaders2);

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> wsClient.getMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(temperatureValue2));

        //update integration with new security disabled
        integration.setConfiguration(defaultConfig(HTTPS_URL));
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 2);

        String temperatureValue3 = "35";
        testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue3));

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> wsClient.getMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(temperatureValue3));
    }

    @Test
    public void checkTelemetryUploadedAfterRemoteIntegrationConfigUpdated() throws Exception {
        JsonNode config = defaultConfigWithSecurityHeader(HTTPS_URL);
        integration = Integration.builder()
                .type(HTTP)
                .name("http")
                .configuration(config)
                .defaultConverterId(uplinkConverter.getId())
                .routingKey(ROUTING_KEY)
                .secret(SECRET_KEY)
                .isRemote(true)
                .enabled(true)
                .debugMode(true)
                .allowCreateDevicesOrAssets(true)
                .build();

        integration = testRestClient.postIntegration(integration);
        waitUntilIntegrationStarted(integration.getId(), integration.getTenantId());

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        Map<String, Object> securityHeaders = mapper.readValue(config.get("headersFilter").toString(), new TypeReference<Map<String, Object>>() {});
        remoteHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE), securityHeaders);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey(TELEMETRY_KEY).get(1)).isEqualTo(TELEMETRY_VALUE);

        //update integration with new security header
        JsonNode config2 = defaultConfigWithSecurityHeader2(HTTPS_URL);
        integration.setConfiguration(config2);
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 1);

        String temperatureValue2 = "58";
        Map<String, Object> securityHeaders2 = mapper.readValue(config2.get("headersFilter").toString(), new TypeReference<Map<String, Object>>() {});
        remoteHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue2), securityHeaders2);

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> wsClient.getMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(temperatureValue2));

        //update integration with new security disabled
        integration.setConfiguration(defaultConfig(HTTPS_URL));
        integration = testRestClient.postIntegration(integration);
        waitForIntegrationEvent(integration, "UPDATED", 2);

        String temperatureValue3 = "35";
        remoteHttpClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue3));

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> wsClient.getMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(temperatureValue3));
    }
    @Test
    public void checkDownlinkMessageWasSent() throws Exception {
        integration = Integration.builder()
                .type(HTTP)
                .name("http" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(defaultConfig(HTTPS_URL))
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

        testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, TELEMETRY_VALUE));

        //create rule chain
        RuleChainId ruleChainId = createRootRuleChainWithIntegrationDownlinkNode(integration.getId());

        JsonNode attributes = mapper.readTree(createPayload().toString());
        testRestClient.saveEntityAttributes(DEVICE, device.getId().toString(), SHARED_SCOPE, attributes);

        RuleChainMetaData ruleChainMetadata = testRestClient.getRuleChainMetadata(ruleChainId);
        RuleNode integrationNode  = ruleChainMetadata.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode")).findFirst().get();
        waitTillRuleNodeReceiveMsg(integrationNode.getId(), EventType.DEBUG_RULE_NODE, integration.getTenantId(), "ATTRIBUTES_UPDATED");

        //check downlink
        String temperatureValue2 = "12";
        JsonPath uplinkResponse2 = testRestClient.postUplinkPayloadForHttpIntegration(integration.getRoutingKey(), createPayloadForUplink(device, temperatureValue2))
                .extract().jsonPath();

        assertThat(uplinkResponse2.getString("booleanKey")).isEqualTo(attributes.get("booleanKey").asText());
        assertThat(uplinkResponse2.getString("stringKey")).isEqualTo(attributes.get("stringKey").asText());
        assertThat(uplinkResponse2.getString("doubleKey")).isEqualTo(attributes.get("doubleKey").asText());
        assertThat(uplinkResponse2.getString("longKey")).isEqualTo(attributes.get("longKey").asText());
    }
    @Override
    protected String getDevicePrototypeSufix() {
        return "http_";
    }

}
