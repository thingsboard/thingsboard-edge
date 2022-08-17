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
package org.thingsboard.server.service.integration.opcua;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.integration.AbstractIntegrationTest;
import org.thingsboard.server.service.integration.opcua.server.TestServer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@DaoSqlTest
public class AbstractBasicOpcUaIntegrationTest extends AbstractIntegrationTest {

    private final static String OPCUA_UPLINK_CONVERTER_FILEPATH = "opcua/default_converter_configuration.json";
    private final static String OPCUA_UPLINK_CONVERTER_NAME = "Default test uplink converter";
    private final static String OPCUA_DOWNLINK_CONVERTER_NAME = "Default test downlink converter";

    private final List<String> expectedNodes = Arrays.asList("Boolean", "Byte", "SByte", "Integer", "Int16", "Int32", "Int64", "UInteger", "UInt16", "UInt32", "UInt64", "Float", "Double", "String", "DateTime", "Guid", "ByteString", "XmlElement", "LocalizedText", "QualifiedName", "NodeId", "Variant", "Duration", "UtcTime");
    protected TestServer server;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
        InputStream resourceAsStream = ObjectNode.class.getClassLoader().getResourceAsStream(OPCUA_UPLINK_CONVERTER_FILEPATH);
        ObjectNode jsonFile = mapper.readValue(resourceAsStream, ObjectNode.class);
        Assert.assertNotNull(jsonFile);

        if (jsonFile.has("configuration") && jsonFile.get("configuration").has("decoder")) {
            createConverter(OPCUA_UPLINK_CONVERTER_NAME, ConverterType.UPLINK, jsonFile.get("configuration"));
        }
        if (jsonFile.has("configuration") && jsonFile.get("configuration").has("encoder")) {
            createConverter(OPCUA_DOWNLINK_CONVERTER_NAME, ConverterType.DOWNLINK, jsonFile.get("configuration"));
        }
        Assert.assertNotNull(uplinkConverter);

        createIntegration("Test OPC-UA integration", IntegrationType.OPC_UA);
        Assert.assertNotNull(integration);

        server = new TestServer();

        startServer();
    }

    private void startServer() throws ExecutionException, InterruptedException {
        server.startup().get();
        log.info("Server started");
        Assert.assertTrue(server.getStarted());
    }

    private void stopServer() throws ExecutionException, InterruptedException {
        server.shutdown().get();
        log.info("Server stopped");
        Assert.assertFalse(server.getStarted());
    }

    @After
    public void afterTest() throws Exception {
        try {
            if (server != null && server.getStarted()) {
                stopServer();
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during stopping OPC-UA server", e);
        }
    }

    @Test
    public void testIntegrationRegularConnect() throws Exception {
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(isIntegrationConnected(startTs, 1, 3000));
    }

    @Test
    public void testIntegrationRegularDisconnect() throws Exception {
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(isIntegrationConnected(startTs, 1, 3000));
        startTs = System.currentTimeMillis();
        stopServer();
        Assert.assertFalse(isIntegrationConnected(startTs, 1, 6000));
    }

    @Test
    public void testIntegrationReconnectAfterServerRestart() throws Exception {
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(isIntegrationConnected(startTs, 1, 3000));
        stopServer();
        startTs = System.currentTimeMillis();
        Assert.assertFalse(isIntegrationConnected(startTs, 1, 6000));
        startServer();
        startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(isIntegrationConnected(startTs, 5, 20000));
    }

    @Test
    public void testIntegrationReconnectToNotStartedServer() throws Exception {
        stopServer();
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertFalse(isIntegrationConnected(startTs, 1, 15000));
        startTs = System.currentTimeMillis();
        startServer();
        Assert.assertTrue(isIntegrationConnected(startTs, 1, 20000));
    }

    @Test
    public void testUplinkProcessing() throws Exception {
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(isIntegrationConnected(startTs, 1, 5000));
        Thread.sleep(10000);
        Device savedDevice = doGet("/api/tenant/devices?deviceName=OPCUA_device", Device.class);
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId().toString() + "/keys/timeseries", new TypeReference<>() {
            });
            if (actualKeys.size() == expectedNodes.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        Assert.assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(expectedNodes);

        Assert.assertEquals(expectedKeySet, actualKeySet);
    }

    @Test
    public void testDownlinkProcessing() throws Exception {
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(isIntegrationConnected(startTs, 1, 5000));


        TransportProtos.IntegrationDownlinkMsgProto downlinkMsgProto = createIntegrationDownlinkMessage();

        downlinkService.onRuleEngineDownlinkMsg(integration.getTenantId(), integration.getId(), downlinkMsgProto, new TbCallback() {
            @Override
            public void onSuccess() {
                log.info("SUCCESS");
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("", t);
            }
        });
        Thread.sleep(20000);
        Device savedDevice = doGet("/api/tenant/devices?deviceName=OPCUA_device", Device.class);
        ObjectNode actualKeys = doGetAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId().toString() + "/values/timeseries?keys=String", ObjectNode.class);
        log.info(actualKeys.toString());
        Assert.assertNotNull(actualKeys);
        Assert.assertTrue(actualKeys.has("String"));
        Assert.assertEquals("New value", actualKeys.get("String").get(0).get("value").asText());

    }

    @Override
    protected JsonNode createIntegrationClientConfiguration() {

        ObjectNode clientConfiguration = JacksonUtil.newObjectNode();
        clientConfiguration.put("host", "localhost");
        clientConfiguration.put("port", 12686);
        clientConfiguration.put("scanPeriodInSeconds", 10);
        clientConfiguration.put("security", "None");
        clientConfiguration.put("timeoutInMillis", 5000);

        ObjectNode identityNode = JacksonUtil.newObjectNode();
        identityNode.put("type", "anonymous");

        clientConfiguration.set("identity", identityNode);

        ArrayNode mappingNode = JacksonUtil.OBJECT_MAPPER.createArrayNode();

        ObjectNode deviceConfigurationNode = JacksonUtil.newObjectNode();
        deviceConfigurationNode.put("deviceNodePattern", "Objects\\.HelloWorld");
        deviceConfigurationNode.put("mappingType", "FQN");

        ArrayNode subscriptionTagsNodes = JacksonUtil.OBJECT_MAPPER.createArrayNode();

        for (String expectedNode : expectedNodes) {
            ObjectNode subscriptionTagNode = JacksonUtil.newObjectNode();
            subscriptionTagNode.put("key", expectedNode);
            subscriptionTagNode.put("path", String.format("ScalarTypes/%s", expectedNode));
            subscriptionTagNode.put("required", Boolean.FALSE);

            subscriptionTagsNodes.add(subscriptionTagNode);
        }

        deviceConfigurationNode.set("subscriptionTags", subscriptionTagsNodes);

        mappingNode.add(deviceConfigurationNode);
        clientConfiguration.set("mapping", mappingNode);

        ObjectNode keystoreNode = JacksonUtil.newObjectNode();
        keystoreNode.put("location", "");
        keystoreNode.put("type", "");
        keystoreNode.put("fileContent", "");
        keystoreNode.put("password", "secret");
        keystoreNode.put("alias", "opc-ua-extension");
        keystoreNode.put("keyPassword", "secret");

        clientConfiguration.set("keystore", keystoreNode);
        clientConfiguration.set("metadata", JacksonUtil.newObjectNode());

        return clientConfiguration;
    }

    private TransportProtos.IntegrationDownlinkMsgProto createIntegrationDownlinkMessage() {
        ObjectNode dataNode = JacksonUtil.newObjectNode();
        ObjectNode writeValuesNode = JacksonUtil.newObjectNode();
        ArrayNode writeValuesArray = JacksonUtil.OBJECT_MAPPER.createArrayNode();
        ObjectNode writeValueNode = JacksonUtil.newObjectNode();
        writeValueNode.put("nodeId", "ns=2;s=HelloWorld/ScalarTypes/String");
        writeValueNode.put("value", "New value");
        writeValuesArray.add(writeValueNode);
        writeValuesNode.set("writeValues", writeValuesArray);
        dataNode.set("data", writeValuesNode);
        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData(new HashMap<>());

        TbMsg tbMsg = TbMsg.newMsg("INTEGRATION_DOWNLINK", new DeviceId(UUID.randomUUID()), tbMsgMetaData, writeValueNode.toString());
        return TransportProtos.IntegrationDownlinkMsgProto.newBuilder()
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setIntegrationIdLSB(integration.getId().getId().getLeastSignificantBits())
                .setIntegrationIdMSB(integration.getId().getId().getMostSignificantBits())
                .setData(TbMsg.toByteString(tbMsg)).build();
    }

    private List<EventInfo> getIntegrationDebugConnectionMessages(long startTs, int eventsCount, long timeout) throws Exception {
        long endTs = startTs + timeout;
        List<EventInfo> connectionMsgs;
        do {
            SortOrder sortOrder = new SortOrder("createdTime", SortOrder.Direction.DESC);
            TimePageLink pageLink = new TimePageLink(100, 0, null, sortOrder, startTs, endTs);
            PageData<EventInfo> events = doGetTypedWithTimePageLink("/api/events/INTEGRATION/{entityId}/DEBUG_INTEGRATION?tenantId={tenantId}&",
                    new TypeReference<PageData<EventInfo>>() {
                    },
                    pageLink, integration.getId(), integration.getTenantId());
            connectionMsgs = events.getData().stream().filter(event -> !"Uplink".equals(event.getBody().get("type").asText())).collect(Collectors.toList());
            if (connectionMsgs.size() >= eventsCount) {
                break;
            }
            Thread.sleep(100);
        }
        while (System.currentTimeMillis() <= endTs);
        return connectionMsgs;
    }

    private Boolean isIntegrationConnected(long startTs, int eventsCount, long timeout) throws Exception {
        List<EventInfo> eventsList = getIntegrationDebugConnectionMessages(startTs, eventsCount, timeout);
        if (eventsList.isEmpty()) {
            return false;
        }
        log.error("Events: {}", eventsList);
        EventInfo event = eventsList.get(0);
        ObjectNode eventBody = (ObjectNode) event.getBody();
        return "CONNECT".equals(eventBody.get("type").asText()) && "SUCCESS".equals(eventBody.get("status").asText());
    }

}
