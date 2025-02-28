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
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.integration.IntegrationType.LORIOT;
import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.uplinkConverterPrototype;
import static org.thingsboard.server.msa.prototypes.HttpIntegrationConfigPrototypes.defaultConfig;

@Slf4j
public class LoriotIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-loriot";
    private static final String SECRET_KEY = "secret-key-loriot";

    private final String JSON_CONVERTER_CONFIG = """
            {
                "isDevice": true,
                "name": "DEVICE_NAME",
                "profile": "default",
                "customer": null,
                "group": null,
                "attributes": [
                    "eui",
                    "fPort",
                    "rssi"
                ],
                "telemetry": [
                    "data"
                ],
                "scriptLang": "TBEL",
                "decoder": "",
                "tbelDecoder": "var payloadStr = decodeToString(payload);\\nvar result = {\\n    attributes: {},\\n    telemetry: {\\n        ts: metadata.ts,\\n        values: {\\n            temperature: payload[0],\\n            humidity: payload[1]\\n        }\\n    }\\n};\\n\\nreturn result;",
                "encoder": null,
                "tbelEncoder": null,
                "updateOnlyKeys": [
                    "fPort",
                    "frequency",
                    "vdd",
                    "eui",
                    "ack",
                    "dr"
                ]
            }
            """;

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
                .type(LORIOT)
                .name("loriot_" + RandomStringUtils.randomAlphanumeric(7))
                .configuration(integrationConfig)
                .defaultConverterId(testRestClient.postConverter(uplinkConverterPrototype(configConverter, LORIOT, 2)).getId())
                .routingKey(ROUTING_KEY)
                .secret(SECRET_KEY)
                .isRemote(false)
                .enabled(true)
                .debugSettings(DebugSettings.until(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)))
                .allowCreateDevicesOrAssets(true)
                .build();

        this.integration = testRestClient.postIntegration(integration);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", AbstractContainerTest.CmdsType.TS_SUB_CMDS);

        ObjectNode payloadMsg = JacksonUtil.newObjectNode();
        payloadMsg.put("ts", System.currentTimeMillis());
        payloadMsg.put("data", "2A3F");
        payloadMsg.put("rssi", "-130");
        payloadMsg.put("port", 80);
        payloadMsg.put("EUI", "BE7A123456789");

        testRestClient.postUplinkPayloadForHttpBasedIntegration(integration.getRoutingKey(), payloadMsg, LORIOT);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        assertThat(actualLatestTelemetry.getDataValuesByKey("data").get(1)).isEqualTo("2A3F");
        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo("42");
        assertThat(actualLatestTelemetry.getDataValuesByKey("humidity").get(1)).isEqualTo("63");
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "loriot_";
    }
}
