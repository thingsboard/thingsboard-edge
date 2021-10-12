/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;


@Slf4j
public class HttpIntegrationTest extends AbstractContainerTest {

    private static final String routingKey = "routing-key-123456";
    private static final String secretKey = "secret-key-123456";
    private static final String login = "tenant@thingsboard.org";
    private static final String password = "tenant";
    private static final String key = "temperature";
    private static final String value = "42";
    private static final String config = " {\"baseUrl\":\"" + HTTPS_URL + "\"," +
            "\"replaceNoContentToOk\":true," +
            "\"enableSecurity\":false," +
            "\"downlinkUrl\":\"https://api.thingpark.com/thingpark/lrc/rest/downlink\"," +
            "\"loriotDownlinkUrl\":\"https://eu1.loriot.io/1/rest\"," +
            "\"createLoriotOutput\":false," +
            "\"sendDownlink\":false," +
            "\"server\":\"eu1\"," +
            "\"appId\":\"\"," +
            "\"enableSecurityNew\":false," +
            "\"asId\":\"\"," +
            "\"asIdNew\":\"\"," +
            "\"asKey\":\"\"," +
            "\"clientIdNew\":\"\"," +
            "\"clientSecret\":\"\"," +
            "\"maxTimeDiffInSeconds\":60," +
            "\"httpEndpoint\":\"\"," +
            "\"headersFilter\":{}," +
            "\"token\":\"\"," +
            "\"credentials\":{\"type\":\"basic\",\"email\":\"\",\"password\":\"\",\"token\":\"\"}," +
            "\"metadata\":{}}";
    private static final JsonNode CUSTOM_CONVERTER_CONFIGURATION = new ObjectMapper()
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

    @Test
    public void telemetryUploadWithIntegration() throws Exception {
        restClient.login(login, password);
        Device device = createDevice("http_");
        boolean isRemote = false;
        Integration integration = createIntegration(isRemote);
        createUplink();
        integration.setDefaultConverterId(restClient.getConverters(new PageLink(1024)).getData().get(0).getId());
        restClient.saveIntegration(integration);
        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        ResponseEntity uplinkResponse = restClient.getRestTemplate().
                postForEntity(HTTPS_URL + "/api/v1/integrations/http/" + integration.getRoutingKey(),
                        createPayloadForUplink(device.getName(), device.getType()),
                        ResponseEntity.class);
        Assert.assertTrue(uplinkResponse.getStatusCode().is2xxSuccessful());
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        Assert.assertTrue(verify(actualLatestTelemetry, key, value));
        wsClient.closeBlocking();
        restClient.deleteDevice(device.getId());
        ConverterId idForDelete = integration.getDefaultConverterId();
        restClient.deleteIntegration(restClient.getIntegrationByRoutingKey(routingKey).get().getId());
        restClient.deleteConverter(idForDelete);
    }

    @Test
    public void telemetryUploadWithRemoteIntegration() throws Exception {
        restClient.login(login, password);
        Device device = createDevice("http_");
        boolean isRemote = true;
        Integration integration = createIntegration(isRemote);
        createUplink();
        integration.setDefaultConverterId(restClient.getConverters(new PageLink(1024)).getData().get(0).getId());
        restClient.saveIntegration(integration);
        Thread.sleep(15000);
        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        ResponseEntity uplinkResponse = rpcRestClient.getRestTemplate().
                postForEntity(rpcURL + "/api/v1/integrations/http/" + integration.getRoutingKey(),
                        createPayloadForUplink(device.getName(), device.getType()),
                        ResponseEntity.class);
        Assert.assertTrue(uplinkResponse.getStatusCode().is2xxSuccessful());
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        Assert.assertTrue(verify(actualLatestTelemetry, key, value));
        wsClient.closeBlocking();
        restClient.deleteDevice(device.getId());
        ConverterId idForDelete = integration.getDefaultConverterId();
        restClient.deleteIntegration(restClient.getIntegrationByRoutingKey(routingKey).get().getId());
        restClient.deleteConverter(idForDelete);
    }

    private Integration createIntegration(boolean isRemote) throws JsonProcessingException {
        Integration integration = new Integration();
        JsonNode conf = mapper.readTree(config);
        integration.setConfiguration(conf);
        integration.setName("HTTP INTEGRATION" + RandomStringUtils.randomAlphanumeric(7));
        integration.setType(IntegrationType.HTTP);
        integration.setRoutingKey(routingKey);
        integration.setSecret(secretKey);
        integration.setEnabled(true);
        integration.setRemote(isRemote);
        integration.setDebugMode(true);
        return integration;
    }

    private void createUplink() {
        Converter converter = new Converter();
        converter.setName("My converter" + RandomStringUtils.randomAlphanumeric(7));
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        restClient.saveConverter(converter);
    }

    protected JsonNode createPayloadForUplink(String name, String type) throws JsonProcessingException {
        JsonObject values = new JsonObject();
        values.addProperty("deviceName", name);
        values.addProperty("deviceType", type);
        values.addProperty(key, value);
        return mapper.readTree(values.toString());
    }


}
