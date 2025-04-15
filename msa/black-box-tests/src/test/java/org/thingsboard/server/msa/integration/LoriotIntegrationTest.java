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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.LORIOT;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfig;

@Slf4j
public class LoriotIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-loriot";
    private static final String SECRET_KEY = "secret-key-loriot";

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
        createIntegration(LORIOT, integrationConfig, configConverter, null, ROUTING_KEY, SECRET_KEY, false, 2);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", AbstractContainerTest.CmdsType.TS_SUB_CMDS);

        ObjectNode payloadMsg = createPayloadMsg();

        testRestClient.postUplinkPayloadForHttpBasedIntegration(integration.getRoutingKey(), payloadMsg, LORIOT);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey("data").get(1)).isEqualTo("2A3F");
        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo("42");
        assertThat(actualLatestTelemetry.getDataValuesByKey("humidity").get(1)).isEqualTo("63");
    }

    @Test
    public void checkAttributesUploadedWithLocalIntegration() {
        JsonNode configConverter = JacksonUtil.toJsonNode(JSON_CONVERTER_CONFIG.replaceAll("DEVICE_NAME", device.getName()));
        JsonNode integrationConfig = defaultConfig(HTTPS_URL);
        createIntegration(LORIOT, integrationConfig, configConverter, null, ROUTING_KEY, SECRET_KEY, false, 2);

        ObjectNode payloadMsg = createPayloadMsg();

        testRestClient.postUplinkPayloadForHttpBasedIntegration(integration.getRoutingKey(), payloadMsg, LORIOT);

        await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<JsonNode> attributes = testRestClient.getEntityAttributeByScopeAndKey(device.getId(), CLIENT_SCOPE, "rssi,eui,fPort");
                    Map<String, JsonNode> attributeMap = attributes.stream()
                            .collect(Collectors.toMap(
                                    node -> node.get("key").asText(),
                                    node -> node
                            ));

                    assertThat(attributeMap.get("rssi").get("value").asInt()).isEqualTo(-130);
                    assertThat(attributeMap.get("eui").get("value").asText()).isEqualTo("BE7A123456789");
                    assertThat(attributeMap.get("fPort").get("value").asInt()).isEqualTo(80);
                });
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "loriot_";
    }

    private ObjectNode createPayloadMsg() {
        ObjectNode payloadMsg = JacksonUtil.newObjectNode();
        payloadMsg.put("ts", System.currentTimeMillis());
        payloadMsg.put("data", "2A3F");
        payloadMsg.put("rssi", -130);
        payloadMsg.put("port", 80);
        payloadMsg.put("EUI", "BE7A123456789");
        payloadMsg.put("snr", 11.5);

        return payloadMsg;
    }

}
