/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.mqtt.attributes.updates;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.transport.mqtt.attributes.AbstractMqttAttributesIntegrationTest;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttAttributesUpdatesIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    private static final String RESPONSE_ATTRIBUTES_PAYLOAD_DELETED = "{\"deleted\":[\"attribute5\"]}";

    private static String getResponseGatewayAttributesUpdatedPayload() {
        return "{\"device\":\"" + "Gateway Device Subscribe to attribute updates" + "\"," +
                "\"data\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
    }

    private static String getResponseGatewayAttributesDeletedPayload() {
        return "{\"device\":\"" + "Gateway Device Subscribe to attribute updates" + "\",\"data\":{\"deleted\":[\"attribute5\"]}}";
    }

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Subscribe to attribute updates", "Gateway Test Subscribe to attribute updates", TransportPayloadType.JSON, null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processTestSubscribeToAttributesUpdates();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerGateway() throws Exception {
        processGatewayTestSubscribeToAttributesUpdates();
    }

    protected void processTestSubscribeToAttributesUpdates() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        TestMqttCallback onUpdateCallback = getTestMqttCallback();
        client.setCallback(onUpdateCallback);

        client.subscribe(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        onUpdateCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateUpdateAttributesResponse(onUpdateCallback);

        TestMqttCallback onDeleteCallback = getTestMqttCallback();
        client.setCallback(onDeleteCallback);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        onDeleteCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateDeleteAttributesResponse(onDeleteCallback);
    }

    protected void validateUpdateAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(POST_ATTRIBUTES_PAYLOAD), JacksonUtil.toJsonNode(response));
    }

    protected void validateDeleteAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(RESPONSE_ATTRIBUTES_PAYLOAD_DELETED), JacksonUtil.toJsonNode(response));
    }

    protected void processGatewayTestSubscribeToAttributesUpdates() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        TestMqttCallback onUpdateCallback = getTestMqttCallback();
        client.setCallback(onUpdateCallback);

        Device device = new Device();
        device.setName("Gateway Device Subscribe to attribute updates");
        device.setType("default");

        byte[] connectPayloadBytes = getConnectPayloadBytes();

        publishMqttMsg(client, connectPayloadBytes, MqttTopics.GATEWAY_CONNECT_TOPIC);

        Device savedDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + "Gateway Device Subscribe to attribute updates", Device.class),
                20,
                100);

        assertNotNull(savedDevice);

        client.subscribe(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        onUpdateCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateGatewayUpdateAttributesResponse(onUpdateCallback);

        TestMqttCallback onDeleteCallback = getTestMqttCallback();
        client.setCallback(onDeleteCallback);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        onDeleteCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateGatewayDeleteAttributesResponse(onDeleteCallback);

    }

    protected void validateGatewayUpdateAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        String s = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(getResponseGatewayAttributesUpdatedPayload(), s);
    }

    protected void validateGatewayDeleteAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        String s = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(s, getResponseGatewayAttributesDeletedPayload());
    }

    protected byte[] getConnectPayloadBytes() {
        String connectPayload = "{\"device\": \"Gateway Device Subscribe to attribute updates\", \"type\": \"" + TransportPayloadType.JSON.name() + "\"}";
        return connectPayload.getBytes();
    }
}
