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
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.CHIRPSTACK;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.uplinkConverterPrototype;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfig;

public class ChirpStackIntegration extends AbstractIntegrationTest{

    private static final String ROUTING_KEY = "routing-key-chirpstack";
    private static final String SECRET_KEY = "secret-key-chirpstack";

    private WsClient wsClient;

    @AfterMethod
    public void tearDown() throws Exception {
        if (wsClient != null) {
            wsClient.closeBlocking();
        }
    }

    @Test
    public void checkTelemetryUploadedWithLocalIntegration() throws Exception {
        JsonNode configConverter = JacksonUtil.toJsonNode(JSON_CONVERTER_CONFIG.replaceAll("DEVICE_NAME", device.getName()));

        JsonNode integrationConfig = defaultConfig(HTTPS_URL);

        Integration integration = Integration.builder()
                .type(CHIRPSTACK)
                .name("chirpstack" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(integrationConfig)
                .defaultConverterId(testRestClient.postConverter(uplinkConverterPrototype(configConverter, CHIRPSTACK, 2)).getId())
                .routingKey(ROUTING_KEY)
                .secret(SECRET_KEY)
                .isRemote(false)
                .enabled(true)
                .debugSettings(DebugSettings.until(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)))
                .allowCreateDevicesOrAssets(true)
                .build();

        this.integration = testRestClient.postIntegration(integration);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        ObjectNode payloadMsg = createPayloadMsg();

        testRestClient.postUplinkPayloadForHttpBasedIntegration(integration.getRoutingKey(), payloadMsg, CHIRPSTACK);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey("data").get(1)).isEqualTo("Kj8=");
        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo("42");
        assertThat(actualLatestTelemetry.getDataValuesByKey("humidity").get(1)).isEqualTo("63");
        assertThat(actualLatestTelemetry.getDataValuesByKey("snr").get(1)).isEqualTo("11.5");
    }

    @Test
    public void checkAttributesUploadedWithLocalIntegration() throws InterruptedException {
        JsonNode configConverter = JacksonUtil.toJsonNode(JSON_CONVERTER_CONFIG.replaceAll("DEVICE_NAME", device.getName()));

        JsonNode integrationConfig = defaultConfig(HTTPS_URL);

        Integration integration = Integration.builder()
                .type(CHIRPSTACK)
                .name("chirpstack" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(integrationConfig)
                .defaultConverterId(testRestClient.postConverter(uplinkConverterPrototype(configConverter, CHIRPSTACK, 2)).getId())
                .routingKey(ROUTING_KEY)
                .secret(SECRET_KEY)
                .isRemote(false)
                .enabled(true)
                .debugSettings(DebugSettings.until(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)))
                .allowCreateDevicesOrAssets(true)
                .build();

        this.integration = testRestClient.postIntegration(integration);

        ObjectNode payloadMsg = createPayloadMsg();

        testRestClient.postUplinkPayloadForHttpBasedIntegration(integration.getRoutingKey(), payloadMsg, CHIRPSTACK);

        await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<JsonNode> attributes = testRestClient.getEntityAttributeByScopeAndKey(device.getId(), CLIENT_SCOPE, ATTRIBUTE_KEY);
                    Map<String, JsonNode> attributeMap = attributes.stream()
                            .collect(Collectors.toMap(
                                    node -> node.get("key").asText(),
                                    node -> node
                            ));

                    assertThat(attributeMap.get("rssi").get("value").asInt()).isEqualTo(-130);
                    assertThat(attributeMap.get("eui").get("value").asText()).isEqualTo("BE7A123456789");
                    assertThat(attributeMap.get("fPort").get("value").asInt()).isEqualTo(80);
                    return true;
                });
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "chirpstack_";
    }

    private ObjectNode createPayloadMsg() {
        ObjectNode payloadMsg = JacksonUtil.newObjectNode();

        String isoTime = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        payloadMsg.put("time", isoTime);
        payloadMsg.put("data", "Kj8=");

        ObjectNode deviceInfo = payloadMsg.putObject("deviceInfo");
        deviceInfo.put("devEui", "BE7A123456789");
        payloadMsg.put("fPort", 80);

        ArrayNode rxInfo = payloadMsg.putArray("rxInfo");
        ObjectNode rxInfoEntry = rxInfo.addObject();
        rxInfoEntry.put("rssi", -130);
        rxInfoEntry.put("snr", 11.5);

        return payloadMsg;
    }

}
