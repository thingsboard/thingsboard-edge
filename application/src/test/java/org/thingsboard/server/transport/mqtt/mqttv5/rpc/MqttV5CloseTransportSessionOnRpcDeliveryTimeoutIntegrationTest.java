/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.mqtt.mqttv5.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestCallback;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "actors.rpc.close_session_on_rpc_delivery_timeout=true",
        "transport.mqtt.timeout=100",
})
public class MqttV5CloseTransportSessionOnRpcDeliveryTimeoutIntegrationTest extends AbstractMqttV5RpcTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("RPC test device")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testOneWayRpcCloseSessionOnRpcDeliveryTimeout() throws Exception {
        testCloseSessionOnRpcDeliveryTimeout("oneway");
    }

    @Test
    public void testTwoWayRpcCloseSessionOnRpcDeliveryTimeout() throws Exception {
        testCloseSessionOnRpcDeliveryTimeout("twoway");
    }

    private void testCloseSessionOnRpcDeliveryTimeout(String rpcType) throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.enableManualAcks();
        client.connectAndWait(accessToken);
        MqttV5NoAckTestCallback callback = new MqttV5NoAckTestCallback();
        client.setCallback(callback);
        client.subscribeAndWait(DEVICE_RPC_REQUESTS_SUB_TOPIC, MqttQoS.AT_LEAST_ONCE);

        String expectedReceivedPayload = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        long expirationTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1},\"persistent\":true,\"retries\":0,\"expirationTime\": " + expirationTime + "}";
        String result = doPostAsync("/api/rpc/" + rpcType + "/" + savedDevice.getId(), setGpioRequest, String.class, status().isOk());

        assertThat(result).isNotNull();
        JsonNode response = JacksonUtil.toJsonNode(result);
        assertThat(response).isNotNull();
        assertThat(response.hasNonNull("rpcId")).isTrue();

        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(callback.getQoS()).isEqualTo(MqttQoS.AT_LEAST_ONCE.value());
        assertThat(JacksonUtil.fromBytes(callback.getPayloadBytes()))
                .isEqualTo(JacksonUtil.toJsonNode(expectedReceivedPayload));

        callback.getDisconnectLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(callback.getReturnCode()).isEqualTo(MqttReturnCode.RETURN_CODE_ADMINISTRITIVE_ACTION);

        Rpc persistedRpc = doGet("/api/rpc/persistent/" + response.get("rpcId").asText(), Rpc.class);
        assertThat(persistedRpc).isNotNull();
        assertThat(persistedRpc.getStatus()).isEqualTo(RpcStatus.QUEUED);
        assertThat(persistedRpc.getResponse()).isInstanceOf(NullNode.class);
        assertThat(client.isConnected()).isFalse();
    }

    @Getter
    private static class MqttV5NoAckTestCallback extends MqttV5TestCallback {

        private int returnCode;

        public CountDownLatch getDisconnectLatch() {
            return super.getDeliveryLatch();
        }

        @Override
        public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
            log.warn("MqttDisconnectResponse: {}", mqttDisconnectResponse);
            returnCode = mqttDisconnectResponse.getReturnCode();
            getDisconnectLatch().countDown();
        }

        @Override
        public void mqttErrorOccurred(MqttException e) {
            log.warn("Error occurred:", e);
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}", requestTopic);
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttToken iMqttToken) {
            // should be never called, Since we're never going to send a response back to server.
            log.warn("delivery complete: {}", iMqttToken.getResponse());
            pubAckReceived = iMqttToken.getResponse().getType() == MqttWireMessage.MESSAGE_TYPE_PUBACK;
            getDisconnectLatch().countDown();
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            log.warn("Connect completed: reconnect - {}, serverURI - {}", reconnect, serverURI);
        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties mqttProperties) {
            log.warn("Auth package received: reasonCode - {}, mqtt properties - {}", reasonCode, mqttProperties);
        }


    }

}
