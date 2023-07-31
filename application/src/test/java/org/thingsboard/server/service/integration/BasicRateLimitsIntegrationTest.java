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
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.service.integration.IntegrationDebugMessageStatus.ANY;

@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL",
        "transport.coap.enabled=true",
        "integrations.rate_limits.enabled=true"
})
@Slf4j
@DaoSqlTest
public class BasicRateLimitsIntegrationTest extends AbstractIntegrationTest {

    @SpyBean
    private DefaultIntegrationRateLimitService limitService;

    private static final String HTTP_UPLINK_CONVERTER_FILEPATH = "http/default_converter_configuration.json";
    private static final String HTTP_UPLINK_CONVERTER_NAME = "Default test uplink converter";
    private static final JsonNode TEST_DATA = JacksonUtil.fromString("{\"test\":1}", JsonNode.class);

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();

        InputStream resourceAsStream = ObjectNode.class.getClassLoader().getResourceAsStream(HTTP_UPLINK_CONVERTER_FILEPATH);
        ObjectNode jsonFile = mapper.readValue(resourceAsStream, ObjectNode.class);
        Assert.assertNotNull(jsonFile);

        if (jsonFile.has("configuration") && jsonFile.get("configuration").has("decoder")) {
            createConverter(HTTP_UPLINK_CONVERTER_NAME, ConverterType.UPLINK, jsonFile.get("configuration"));
        }
        Assert.assertNotNull(uplinkConverter);

        createIntegration("Test HTTP integration", IntegrationType.HTTP);
        Assert.assertNotNull(integration);

        enableIntegration();
        waitUntilIntegrationStarted(tenantId, integration.getId());

        Device device = new Device();
        device.setName("http_device");
        device.setType("http");

        doPost("/api/device", device).andExpect(status().isOk());
    }

    @After
    public void afterTest() throws Exception {
        disableIntegration();
        removeIntegration(integration);
    }

    @Override
    protected JsonNode createIntegrationClientConfiguration() {
        ObjectNode clientConfiguration = JacksonUtil.newObjectNode();
        clientConfiguration.put("baseUrl", "127.0.0.1");
        clientConfiguration.set("metadata", JacksonUtil.newObjectNode());
        return clientConfiguration;
    }

    @Test
    public void testIntegrationRateLimitPerTenant() throws Exception {
        long startTime = System.currentTimeMillis();
        ReflectionTestUtils.setField(limitService, "perTenantLimitsConf", "10:1,20:60");
        ReflectionTestUtils.setField(limitService, "perDevicesLimitsConf", "100:1,2000:60");
        ReflectionTestUtils.invokeMethod(limitService, "init");

        repeat(20, i -> {
            try {
                doPost("/api/v1/integrations/http/{routingKey}", TEST_DATA, integration.getRoutingKey()).andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        await().atMost(10, TimeUnit.SECONDS).until(() ->
                getIntegrationDebugMessages(startTime, "Uplink", ANY, 10).size() >= 11
        );

        Mockito.verify(limitService, times(20)).checkLimit(eq(tenantId), any());

        List<EventInfo> events = getIntegrationDebugMessages(startTime, "Uplink", "ERROR", 10);

        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("TENANT rate limits reached!", events.get(0).getBody().get("error").asText());
    }

    @Test
    public void testIntegrationRateLimitPerDevice() throws Exception {
        long startTime = System.currentTimeMillis();
        ReflectionTestUtils.setField(limitService, "perTenantLimitsConf", "100:1,2000:60");
        ReflectionTestUtils.setField(limitService, "perDevicesLimitsConf", "10:1,20:60");
        ReflectionTestUtils.invokeMethod(limitService, "init");

        repeat(20, i -> {
            try {
                doPost("/api/v1/integrations/http/{routingKey}", TEST_DATA, integration.getRoutingKey()).andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        await().atMost(10, TimeUnit.SECONDS).until(() ->
                getIntegrationDebugMessages(startTime, "Uplink", ANY, 10).size() >= 11
        );

        Mockito.verify(limitService, times(20)).checkLimit(eq(tenantId), eq("http_device"), any());

        List<EventInfo> events = getIntegrationDebugMessages(startTime, "Uplink", "ERROR", 10);

        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("DEVICE rate limits reached!", events.get(0).getBody().get("error").asText());
    }

    private void repeat(int n, IntConsumer i) {
        IntStream.range(0, n).forEach(i);
    }

}
