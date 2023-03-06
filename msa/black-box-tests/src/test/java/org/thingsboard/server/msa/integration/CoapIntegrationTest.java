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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.testcontainers.containers.ContainerState;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;

import static ch.qos.logback.core.encoder.ByteArrayUtil.hexStringToByteArray;
import static org.thingsboard.server.msa.TestProperties.getRemoteCoapHost;
import static org.thingsboard.server.msa.TestProperties.getRemoteCoapPort;

@Slf4j
public class CoapIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-coap";
    private static final String SECRET_KEY = "secret-key-coap";
    private static final String TOKEN = ROUTING_KEY;
    private static final String CONFIG_INTEGRATION = "{\"clientConfiguration\":{" +
            "\"baseUrl\":\"coap://localhost\"," +
            "\"dtlsBaseUrl\":\"coaps://localhost\"," +
            "\"securityMode\":\"NO_SECURE\"," +
            "\"coapEndpoint\":\"coap://localhost/i/" + TOKEN + "\"," +
            "\"dtlsCoapEndpoint\":\"coaps://localhost/i/" + TOKEN + "\"},\"metadata\":{}}";
    private static final String JSON_CONVERTER_CONFIG = "var data = decodeToJson(payload);\n" +
            "var deviceName =  '" + "DEVICE_NAME" + "';\n" +
            "var deviceType = 'DEFAULT';\n" +
            "var data = decodeToJson(payload);\n" +
            "var result = {\n" +
            "   deviceName: deviceName,\n" +
            "   deviceType: deviceType,\n" +
            "   attributes: {},\n" +
            "   telemetry: {\n" +
            "       temperature: data.temperature,\n" +
            "       humidity: data.humidity\n" +
            "   }\n" +
            "};\n" +
            "\n" +
            "/** Helper functions **/\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "function decodeToJson(payload) {\n" +
            "   // covert payload to string.\n" +
            "   var str = decodeToString(payload);\n" +
            "   var data = JSON.parse(str);\n" +
            "   return data;\n" +
            "}\n" +
            "return result;";

    private static final String TEXT_CONVERTER_CONFIG = "var strArray = decodeToString(payload);\n" +
            "var payloadArray = strArray.replace(/\\\"/g, \"\").replace(/\\s/g, \"\").split(',');\n" +
            "var telemetryKey = payloadArray[2];\n" +
            "var telemetryValue = payloadArray[3]; \n" +
            "var telemetryPayload = {};\n" +
            "telemetryPayload[telemetryKey] = telemetryValue;\n" +
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

    private static final String BINARY_CONVERTER_CONFIG = "var payloadStr = decodeToString(payload);\n" +
            "\n" +
            "var deviceName = payloadStr.substring(0,12);\n" +
            "var deviceType = payloadStr.substring(12,19);\n" +
            "\n" +
            "var result = {\n" +
            "   deviceName: deviceName,\n" +
            "   deviceType: deviceType,\n" +
            "   attributes: {},\n" +
            "   telemetry: {\n" +
            "       temperature: parseFloat(payloadStr.substring(19,25))\n" +
            "   }\n" +
            "};\n" +
            "\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "\n" +
            "return result;";

    private RuleChainId defaultRuleChainId;
    @BeforeMethod
    public void setUp()  {
        defaultRuleChainId = getDefaultRuleChainId();
    }
    @AfterMethod
    public void afterMethod() {
        testRestClient.setRootRuleChain(defaultRuleChainId);
        afterIntegrationTest();
        device = null;

        if (containerTestSuite.isActive()) {
            ContainerState tcpIntegrationContainer = containerTestSuite.getTestContainer().getContainerByServiceName("tb-pe-coap-integration_1").get();
            tcpIntegrationContainer.getDockerClient().restartContainerCmd(tcpIntegrationContainer.getContainerId()).exec();
        }
    }
    @Test
    public void checkTelemetryUploadedWithJsonConverter() throws Exception {
        JsonNode configConverter = new ObjectMapper().createObjectNode().put("decoder",
                JSON_CONVERTER_CONFIG.replaceAll("DEVICE_NAME", device.getName()));
        integration = createIntegration(
                IntegrationType.COAP, CONFIG_INTEGRATION, configConverter, ROUTING_KEY, SECRET_KEY, true);

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        sendMessageToIntegration(createPayloadForUplink().toString().getBytes(), MediaTypeRegistry.APPLICATION_JSON);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet(TELEMETRY_KEY),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, TELEMETRY_VALUE));
    }
    @Test
    public void checkTelemetryUploadedWithTextConverter() throws Exception {
        JsonNode configConverter = new ObjectMapper().createObjectNode().put("decoder", TEXT_CONVERTER_CONFIG);
        integration = createIntegration(
                IntegrationType.COAP, CONFIG_INTEGRATION, configConverter, ROUTING_KEY, SECRET_KEY, true);

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        String textPayload = device.getName() + ",default,temperature,25.7";
        sendMessageToIntegration(textPayload.getBytes(), MediaTypeRegistry.TEXT_PLAIN);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet(TELEMETRY_KEY),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, "25.7"));
    }
    @Test
    public void checkTelemetryUploadedWithBinaryConverter() throws Exception {
        JsonNode configConverter = new ObjectMapper().createObjectNode().put("decoder", BINARY_CONVERTER_CONFIG);
        integration = createIntegration(
                IntegrationType.COAP, CONFIG_INTEGRATION, configConverter, ROUTING_KEY, SECRET_KEY, true);

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        String hexString = DatatypeConverter.printHexBinary((device.getName() + "default25.7").getBytes());
        byte[] bytes = hexStringToByteArray(hexString);
        sendMessageToIntegration(bytes, MediaTypeRegistry.APPLICATION_OCTET_STREAM);

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet(TELEMETRY_KEY),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, TELEMETRY_KEY, "25.7"));
    }

    private void sendMessageToIntegration(byte[] payload, int format) throws ConnectorException, IOException {
        CoapClient coapClient = new CoapClient("coap", getRemoteCoapHost(), getRemoteCoapPort(), "i", TOKEN);
        coapClient.post(payload, format);
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "coap_";
    }
}
