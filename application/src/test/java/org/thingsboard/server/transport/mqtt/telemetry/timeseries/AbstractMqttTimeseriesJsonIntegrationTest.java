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
package org.thingsboard.server.transport.mqtt.telemetry.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
public abstract class AbstractMqttTimeseriesJsonIntegrationTest extends AbstractMqttTimeseriesIntegrationTest {

    private static final String POST_DATA_TELEMETRY_TOPIC = "data/telemetry";

    @Before
    public void beforeTest() throws Exception {
        //do nothing, processBeforeTest will be invoked in particular test methods with different parameters
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testPushTelemetry() throws Exception {
        processBeforeTest("Test Post Telemetry device json payload", "Test Post Telemetry gateway json payload", TransportPayloadType.JSON, POST_DATA_TELEMETRY_TOPIC, null);
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(POST_DATA_TELEMETRY_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes(), false);
    }

    @Test
    public void testPushTelemetryWithTs() throws Exception {
        processBeforeTest("Test Post Telemetry device json payload", "Test Post Telemetry gateway json payload", TransportPayloadType.JSON, POST_DATA_TELEMETRY_TOPIC, null);
        String payloadStr = "{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR + "}";
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadTelemetryTest(POST_DATA_TELEMETRY_TOPIC, expectedKeys, payloadStr.getBytes(), true);
    }

    @Test
    public void testPushTelemetryOnShortTopic() throws Exception {
        processBeforeTest("Test Post Telemetry device json payload", "Test Post Telemetry gateway json payload", TransportPayloadType.JSON, POST_DATA_TELEMETRY_TOPIC, null);
        super.testPushTelemetryOnShortTopic();
    }

    @Test
    public void testPushTelemetryOnShortJsonTopic() throws Exception {
        processBeforeTest("Test Post Telemetry device json payload", "Test Post Telemetry gateway json payload", TransportPayloadType.JSON, POST_DATA_TELEMETRY_TOPIC, null);
        super.testPushTelemetryOnShortJsonTopic();
    }

    @Test
    public void testPushTelemetryGateway() throws Exception {
        processBeforeTest("Test Post Telemetry device json payload", "Test Post Telemetry gateway json payload", TransportPayloadType.JSON, POST_DATA_TELEMETRY_TOPIC, null);
        super.testPushTelemetryGateway();
    }

    @Test
    public void testGatewayConnect() throws Exception {
        processBeforeTest("Test Post Telemetry device json payload", "Test Post Telemetry gateway json payload", TransportPayloadType.JSON, POST_DATA_TELEMETRY_TOPIC, null);
        super.testGatewayConnect();
    }

    @Test
    public void testPushTelemetryWithMalformedPayloadAndSendAckOnErrorEnabled() throws Exception {
        processBeforeTest("Test Post Telemetry device json payload", "Test Post Telemetry gateway json payload", TransportPayloadType.JSON, POST_DATA_TELEMETRY_TOPIC, null, true);
        CountDownLatch latch = new CountDownLatch(1);
        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        TestMqttPublishCallback callback = new TestMqttPublishCallback(latch);
        client.setCallback(callback);
        publishMqttMsg(client, MALFORMED_JSON_PAYLOAD.getBytes(), POST_DATA_TELEMETRY_TOPIC);
        latch.await(3, TimeUnit.SECONDS);
        assertTrue(callback.isPubAckReceived());
    }

    @Test
    public void testPushTelemetryWithMalformedPayloadAndSendAckOnErrorDisabled() throws Exception {
        processBeforeTest("Test Post Telemetry device json payload", "Test Post Telemetry gateway json payload", TransportPayloadType.JSON, POST_DATA_TELEMETRY_TOPIC, null, false);
        CountDownLatch latch = new CountDownLatch(1);
        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        TestMqttPublishCallback callback = new TestMqttPublishCallback(latch);
        client.setCallback(callback);
        publishMqttMsg(client, MALFORMED_JSON_PAYLOAD.getBytes(), POST_DATA_TELEMETRY_TOPIC);
        latch.await(3, TimeUnit.SECONDS);
        assertFalse(callback.isPubAckReceived());
    }

    @Test
    public void testPushTelemetryGatewayWithMalformedPayloadAndSendAckOnErrorEnabled() throws Exception {
        processBeforeTest("Test Post Telemetry device json payload", "Test Post Telemetry gateway json payload", TransportPayloadType.JSON, POST_DATA_TELEMETRY_TOPIC, null, true);
        CountDownLatch latch = new CountDownLatch(1);
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        TestMqttPublishCallback callback = new TestMqttPublishCallback(latch);
        client.setCallback(callback);
        publishMqttMsg(client, MALFORMED_JSON_PAYLOAD.getBytes(), POST_DATA_TELEMETRY_TOPIC);
        latch.await(3, TimeUnit.SECONDS);
        assertTrue(callback.isPubAckReceived());
    }

    @Test
    public void testPushTelemetryGatewayWithMalformedPayloadAndSendAckOnErrorDisabled() throws Exception {
        processBeforeTest("Test Post Telemetry device json payload", "Test Post Telemetry gateway json payload", TransportPayloadType.JSON, POST_DATA_TELEMETRY_TOPIC, null, false);
        CountDownLatch latch = new CountDownLatch(1);
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        TestMqttPublishCallback callback = new TestMqttPublishCallback(latch);
        client.setCallback(callback);
        publishMqttMsg(client, MALFORMED_JSON_PAYLOAD.getBytes(), POST_DATA_TELEMETRY_TOPIC);
        latch.await(3, TimeUnit.SECONDS);
        assertFalse(callback.isPubAckReceived());
    }

}
