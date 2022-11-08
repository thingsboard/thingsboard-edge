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
package org.thingsboard.server.msa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.awaitility.Awaitility;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import java.net.URI;
import java.util.*;

@Slf4j
@Listeners(TestListener.class)
public abstract class AbstractContainerTest {
    protected static final String HTTPS_URL = "https://localhost";
    protected static final String WSS_URL = "wss://localhost";
    protected static String rpcURLHttp;
    protected static String TB_TOKEN;
    protected static RestClient restClient;
    protected static RestClient rpcHTTPRestClient;
    protected static long timeoutMultiplier = 1;
    protected ObjectMapper mapper = new ObjectMapper();
    protected JsonParser jsonParser = new JsonParser();
    protected static final String TELEMETRY_KEY = "temperature";
    protected static final String TELEMETRY_VALUE = "42";
    protected static final int CONNECT_TRY_COUNT = 50;
    protected static final int CONNECT_TIMEOUT_MS = 500;
    private static final ContainerTestSuite containerTestSuite = ContainerTestSuite.getInstance();
    protected static TestRestClient testRestClient;

    @BeforeSuite
    public void beforeSuite() {
        String rpcHost = ContainerTestSuite.getTestContainer().getServiceHost("tb-pe-http-integration", 8082);
        Integer rpcPort = ContainerTestSuite.getTestContainer().getServicePort("tb-pe-http-integration", 8082);
        rpcURLHttp = "http://" + rpcHost + ":" + rpcPort;
        rpcHTTPRestClient = new RestClient(rpcURLHttp);
        if ("false".equals(System.getProperty("runLocal", "false"))) {
            containerTestSuite.start();
        }
        testRestClient = new TestRestClient(TestProperties.getBaseUrl());
        if (!"kafka".equals(System.getProperty("blackBoxTests.queue", "kafka"))) {
            timeoutMultiplier = 10;
        }
    }

    @AfterSuite
    public void afterSuite() {
        if (containerTestSuite.isActive()) {
            containerTestSuite.stop();
        }
    }

    protected WsClient subscribeToWebSocket(DeviceId deviceId, String scope, CmdsType property) throws Exception {
        String webSocketUrl = TestProperties.getWebSocketUrl();
        WsClient wsClient = new WsClient(new URI(webSocketUrl + "/api/ws/plugins/telemetry?token=" + testRestClient.getToken()), timeoutMultiplier);
        if (webSocketUrl.matches("^(wss)://.*$")) {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
            wsClient.setSocketFactory(builder.build().getSocketFactory());
        }
        wsClient.connectBlocking();

        JsonObject cmdsObject = new JsonObject();
        cmdsObject.addProperty("entityType", EntityType.DEVICE.name());
        cmdsObject.addProperty("entityId", deviceId.toString());
        cmdsObject.addProperty("scope", scope);
        cmdsObject.addProperty("cmdId", new Random().nextInt(100));

        JsonArray cmd = new JsonArray();
        cmd.add(cmdsObject);
        JsonObject wsRequest = new JsonObject();
        wsRequest.add(property.toString(), cmd);
        wsClient.send(wsRequest.toString());
        wsClient.waitForFirstReply();
        return wsClient;
    }

    protected Map<String, Long> getExpectedLatestValues(long ts) {
        return ImmutableMap.<String, Long>builder()
                .put("booleanKey", ts)
                .put("stringKey", ts)
                .put("doubleKey", ts)
                .put("longKey", ts)
                .build();
    }

    protected boolean verify(WsTelemetryResponse wsTelemetryResponse, String key, Long expectedTs, String expectedValue) {
        List<Object> list = wsTelemetryResponse.getDataValuesByKey(key);
        return expectedTs.equals(list.get(0)) && expectedValue.equals(list.get(1));
    }

    protected boolean verify(WsTelemetryResponse wsTelemetryResponse, String key, String expectedValue) {
        List<Object> list = wsTelemetryResponse.getDataValuesByKey(key);
        return expectedValue.equals(list.get(1));
    }

    protected JsonObject createGatewayConnectPayload(String deviceName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("device", deviceName);
        return payload;
    }

    protected JsonObject createGatewayPayload(String deviceName, long ts) {
        JsonObject payload = new JsonObject();
        payload.add(deviceName, createGatewayTelemetryArray(ts));
        return payload;
    }

    protected JsonArray createGatewayTelemetryArray(long ts) {
        JsonArray telemetryArray = new JsonArray();
        if (ts > 0)
            telemetryArray.add(createPayload(ts));
        else
            telemetryArray.add(createPayload());
        return telemetryArray;
    }

    protected JsonObject createPayload(long ts) {
        JsonObject values = createPayload();
        JsonObject payload = new JsonObject();
        payload.addProperty("ts", ts);
        payload.add("values", values);
        return payload;
    }

    protected JsonObject createPayload() {
        JsonObject values = new JsonObject();
        values.addProperty("stringKey", "value1");
        values.addProperty("booleanKey", true);
        values.addProperty("doubleKey", 42.0);
        values.addProperty("longKey", 73L);

        return values;
    }

    protected Converter createUplink(JsonNode config) {
        Converter converter = new Converter();
        converter.setName("My converter" + StringUtils.randomAlphanumeric(7));
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(config);
        return restClient.saveConverter(converter);
    }

    protected void deleteAllObject(Device device, Integration integration) {
        restClient.deleteDevice(device.getId());
        ConverterId idForDelete = integration.getDefaultConverterId();
        restClient.deleteIntegration(integration.getId());
        restClient.deleteConverter(idForDelete);
    }

    protected enum CmdsType {
        TS_SUB_CMDS("tsSubCmds"),
        HISTORY_CMDS("historyCmds"),
        ATTR_SUB_CMDS("attrSubCmds");

        private final String text;

        CmdsType(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private static HttpComponentsClientHttpRequestFactory getRequestFactoryForSelfSignedCert() throws Exception {
        SSLContextBuilder builder = SSLContexts.custom();
        builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
        SSLContext sslContext = builder.build();
        SSLConnectionSocketFactory sslSelfSigned = new SSLConnectionSocketFactory(sslContext, (s, sslSession) -> true);

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory>create()
                .register("https", sslSelfSigned)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    protected JsonNode createPayloadForUplink() {
        ObjectNode values = JacksonUtil.newObjectNode();
        values.put(TELEMETRY_KEY, TELEMETRY_VALUE);
        return values;
    }

    protected Integration createIntegration(IntegrationType type, String config, JsonNode converterConfig,
                                            String routingKey, String secretKey, boolean isRemote) {
        Integration integration = new Integration();
        JsonNode conf = JacksonUtil.toJsonNode(config);
        integration.setConfiguration(conf);
        integration.setDefaultConverterId(createUplink(converterConfig).getId());
        integration.setName(type.name().toLowerCase());
        integration.setType(type);
        integration.setRoutingKey(routingKey);
        integration.setSecret(secretKey);
        integration.setEnabled(true);
        integration.setRemote(isRemote);
        integration.setDebugMode(true);
        integration.setAllowCreateDevicesOrAssets(true);

        integration = restClient.saveIntegration(integration);

        IntegrationId integrationId = integration.getId();
        TenantId tenantId = integration.getTenantId();

        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = restClient.getEvents(integrationId, EventType.LC_EVENT, tenantId, new TimePageLink(1024));
                    if (events.getData().isEmpty()) {
                        return false;
                    }

                    EventInfo event = events.getData().stream().max(Comparator.comparingLong(EventInfo::getCreatedTime)).orElse(null);
                    return event != null
                            && "STARTED".equals(event.getBody().get("event").asText())
                            && "true".equals(event.getBody().get("success").asText());
                });

        return integration;
    }
}
