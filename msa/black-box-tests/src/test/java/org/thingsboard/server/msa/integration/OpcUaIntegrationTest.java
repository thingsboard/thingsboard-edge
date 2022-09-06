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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.concurrent.TimeUnit;

@Slf4j
public class OpcUaIntegrationTest extends AbstractContainerTest {
    private static final String ROUTING_KEY = "routing-key-opc-ua";
    private static final String SECRET_KEY = "secret-key-opc-ua";
    private static final String LOGIN = "tenant@thingsboard.org";
    private static final String PASSWORD = "tenant";
    private static final String CONFIG_INTEGRATION = "{\"clientConfiguration\":{" +
            "\"applicationName\":\"\"," +
            "\"applicationUri\":\"\"," +
            "\"host\":\"qa-integrations.thingsboard.io\"," +
            "\"port\":50000," +
            "\"scanPeriodInSeconds\":10," +
            "\"timeoutInMillis\":5000," +
            "\"security\":\"None\"," +
            "\"identity\":{\"type\":\"anonymous\"}," +
            "\"mapping\":[{" +
            "\"deviceNodePattern\":\"Objects\\\\.Boiler \\\\#\\\\d+\"," +
            "\"mappingType\":\"FQN\"," +
            "\"subscriptionTags\":[{" +
            "\"key\":\"BoilerStatus\"," +
            "\"path\":\"BoilerStatus\"," +
            "\"required\":false}]}]," +
            "\"keystore\":{" +
            "\"location\":\"\"," +
            "\"type\":\"\"," +
            "\"fileContent\":\"\"," +
            "\"password\":\"secret\"," +
            "\"alias\":\"opc-ua-extension\"," +
            "\"keyPassword\":\"secret\"}}," +
            "\"metadata\":{}}";
    private static final String CONFIG_CONVERTER = "var data = decodeToJson(payload);\n" +
            "var deviceName =  '" + "DEVICE_NAME" + "';\n" +
            "var deviceType = 'DEFAULT';\n" +
            "\n" +
            "var result = {\n" +
            "   deviceName: deviceName,\n" +
            "   deviceType: deviceType,\n" +
            "   telemetry: {\n" +
            "   }\n" +
            "};\n" +
            "\n" +
            "var boilerStatus = data.BoilerStatus;\n" +
            "\n" +
            "\n" +
            "if (data.BoilerStatus) {\n" +
            "    result.telemetry.boilerStatus = boilerStatus;\n" +
            "}\n" +
            "\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "\n" +
            "function decodeToJson(payload) {\n" +
            "   var str = decodeToString(payload);\n" +
            "   var data = JSON.parse(str);\n" +
            "   return data;\n" +
            "}\n" +
            "\n" +
            "return result;";

    @Test
    public void telemetryUploadWithLocalIntegration() throws Exception {
        restClient.login(LOGIN, PASSWORD);
        Device device = createDevice("opc_ua_");

        JsonNode configConverter = new ObjectMapper().createObjectNode().put("decoder",
                CONFIG_CONVERTER.replaceAll("DEVICE_NAME", device.getName()));
        Converter savedConverter = createUplink(configConverter);
        Integration integration = createIntegration(false);
        integration.setDefaultConverterId(savedConverter.getId());
        integration = restClient.saveIntegration(integration);

        IntegrationId integrationId = integration.getId();
        TenantId tenantId = integration.getTenantId();

        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = restClient.getEvents(integrationId, tenantId, new TimePageLink(1024));
                    return !events.getData().isEmpty();
                });

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet("boilerStatus"), actualLatestTelemetry.getLatestValues().keySet());

//        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, TELEMETRY_VALUE));

        deleteAllObject(device, integration);
    }

    private Integration createIntegration(boolean isRemote) {
        Integration integration = new Integration();
        JsonNode conf = JacksonUtil.toJsonNode(CONFIG_INTEGRATION);
        log.info(conf.toString());
        integration.setConfiguration(conf);
        integration.setName("opc_ua");
        integration.setType(IntegrationType.OPC_UA);
        integration.setRoutingKey(ROUTING_KEY);
        integration.setSecret(SECRET_KEY);
        integration.setEnabled(true);
        integration.setRemote(isRemote);
        integration.setDebugMode(true);
        integration.setAllowCreateDevicesOrAssets(true);
        return integration;
    }

}
