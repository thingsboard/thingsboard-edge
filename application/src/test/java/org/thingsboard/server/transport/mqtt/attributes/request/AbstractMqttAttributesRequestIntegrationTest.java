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
package org.thingsboard.server.transport.mqtt.attributes.request;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.transport.mqtt.attributes.AbstractMqttAttributesIntegrationTest;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttAttributesRequestIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Request attribute values from the server", "Gateway Test Request attribute values from the server", null, null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testRequestAttributesValuesFromTheServer() throws Exception {
        processTestRequestAttributesValuesFromTheServer();
    }

    @Test
    public void testRequestAttributesValuesFromTheServerGateway() throws Exception {
        processTestGatewayRequestAttributesValuesFromTheServer();
    }

    protected void processTestRequestAttributesValuesFromTheServer() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        postAttributesAndSubscribeToTopic(savedDevice, client);

        Thread.sleep(1000);

        TestMqttCallback callback = getTestMqttCallback();
        client.setCallback(callback);

        validateResponse(client, callback.getLatch(), callback);
    }

    protected void processTestGatewayRequestAttributesValuesFromTheServer() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        postGatewayDeviceClientAttributes(client);

        Device savedDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + "Gateway Device Request Attributes", Device.class),
                20,
                100);

        assertNotNull(savedDevice);

        Thread.sleep(1000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        Thread.sleep(1000);

        client.subscribe(MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, MqttQoS.AT_LEAST_ONCE.value());

        TestMqttCallback clientAttributesCallback = getTestMqttCallback();
        client.setCallback(clientAttributesCallback);
        validateClientResponseGateway(client, clientAttributesCallback);

        TestMqttCallback sharedAttributesCallback = getTestMqttCallback();
        client.setCallback(sharedAttributesCallback);
        validateSharedResponseGateway(client, sharedAttributesCallback);
    }

    protected void postAttributesAndSubscribeToTopic(Device savedDevice, MqttAsyncClient client) throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        client.publish(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, new MqttMessage(POST_ATTRIBUTES_PAYLOAD.getBytes()));
        client.subscribe(MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttQoS.AT_MOST_ONCE.value());
    }

    protected void postGatewayDeviceClientAttributes(MqttAsyncClient client) throws Exception {
        String postClientAttributes = "{\"" + "Gateway Device Request Attributes" + "\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, new MqttMessage(postClientAttributes.getBytes()));
    }

    protected void validateResponse(MqttAsyncClient client, CountDownLatch latch, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        String payloadStr = "{\"clientKeys\":\"" + keys + "\", \"sharedKeys\":\"" + keys + "\"}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX + "1", mqttMessage);
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"client\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}},\"shared\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }

    protected void validateClientResponseGateway(MqttAsyncClient client, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String payloadStr = "{\"id\": 1, \"device\": \"" + "Gateway Device Request Attributes" + "\", \"client\": true, \"keys\": [\"attribute1\", \"attribute2\", \"attribute3\", \"attribute4\", \"attribute5\"]}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, mqttMessage);
        callback.getLatch().await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"id\":1,\"device\":\"" + "Gateway Device Request Attributes" + "\",\"values\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }

    protected void validateSharedResponseGateway(MqttAsyncClient client, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String payloadStr = "{\"id\": 1, \"device\": \"" + "Gateway Device Request Attributes" + "\", \"client\": false, \"keys\": [\"attribute1\", \"attribute2\", \"attribute3\", \"attribute4\", \"attribute5\"]}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, mqttMessage);
        callback.getLatch().await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"id\":1,\"device\":\"" + "Gateway Device Request Attributes" + "\",\"values\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }
}
