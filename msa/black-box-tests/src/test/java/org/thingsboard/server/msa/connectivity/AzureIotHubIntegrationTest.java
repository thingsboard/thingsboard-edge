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
package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.microsoft.azure.sdk.iot.service.DeliveryAcknowledgement;
import com.microsoft.azure.sdk.iot.service.IotHubServiceClientProtocol;
import com.microsoft.azure.sdk.iot.service.Message;
import com.microsoft.azure.sdk.iot.service.ServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.BeforeClass;
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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AzureIotHubIntegrationTest extends AbstractContainerTest {
    private static final String ROUTING_KEY = "routing-key-azure-iot";
    private static final String SECRET_KEY = "secret-key-azure-iot";
    private static final String LOGIN = "tenant@thingsboard.org";
    private static final String PASSWORD = "tenant";
    private static final String HOST_NAME = System.getProperty("blackBoxTests.azureIotHubHostName", "");
    private static final String SAS_KEY = System.getProperty("blackBoxTests.azureIotHubSasKey", "");
    private static final String DEVICE_ID = System.getProperty("blackBoxTests.azureIotHubDeviceId", "");
    private static final String CONFIG_INTEGRATION = "{\"clientConfiguration\":{" +
            "\"host\":\"" + HOST_NAME + ".azure-devices.net\"," +
            "\"port\":8883," +
            "\"cleanSession\":true," +
            "\"ssl\":true," +
            "\"maxBytesInMessage\":32368," +
            "\"connectTimeoutSec\":10," +
            "\"clientId\":\"" + DEVICE_ID + "\"," +
            "\"credentials\":{" +
            "\"type\":\"sas\"," +
            "\"sasKey\":\"" + SAS_KEY + "\"}}," +
            "\"topicFilters\":[{" +
            "\"filter\":\"devices/" + DEVICE_ID + "/messages/devicebound/#\"," +
            "\"qos\":0}]," +
            "\"metadata\":{}}";
    private static final String CONFIG_CONVERTER = "var payloadStr = decodeToString(payload);\n" +
            "var data = JSON.parse(payloadStr);\n" +
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

    @BeforeClass
    public static void setUp() {
        org.junit.Assume.assumeFalse(Boolean.parseBoolean(System.getProperty("blackBoxTests.integrations.skip", "true")));
    }

    @Test
    public void telemetryUploadWithLocalIntegration() throws Exception {
        restClient.login(LOGIN, PASSWORD);
        Device device = createDevice("azure_iot_");

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

        sendMessageToHub();

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet(TELEMETRY_KEY),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, TELEMETRY_VALUE));

        deleteAllObject(device, integration);
    }

    private Integration createIntegration(boolean isRemote) {
        Integration integration = new Integration();
        JsonNode conf = JacksonUtil.toJsonNode(CONFIG_INTEGRATION);
        log.info(conf.toString());
        integration.setConfiguration(conf);
        integration.setName("azure_iot");
        integration.setType(IntegrationType.AZURE_IOT_HUB);
        integration.setRoutingKey(ROUTING_KEY);
        integration.setSecret(SECRET_KEY);
        integration.setEnabled(true);
        integration.setRemote(isRemote);
        integration.setDebugMode(true);
        integration.setAllowCreateDevicesOrAssets(true);
        return integration;
    }

    void sendMessageToHub() throws Exception {
        ServiceClient serviceClient = initServiceClient();
        String payload = createPayloadForUplink().toString();

        Message message = new Message(payload);
        message.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);
        message.setMessageId(UUID.randomUUID().toString());
        message.getProperties().put("content-type", "JSON");
        serviceClient.send(DEVICE_ID, message);
        serviceClient.close();
    }

    private ServiceClient initServiceClient() throws Exception {
        //Event Hub-compatible endpoint
        String connectionString = System.getProperty("blackBoxTests.azureIotHubConnectionString");

        ServiceClient serviceClient = ServiceClient.createFromConnectionString(connectionString, IotHubServiceClientProtocol.AMQPS);
        serviceClient.open();
        return serviceClient;
    }

}
