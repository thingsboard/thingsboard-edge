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
package org.thingsboard.server.transport.mqtt.mqttv3;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.thingsboard.server.common.data.StringUtils;

import java.util.concurrent.TimeUnit;

public class MqttTestClient {

    private static final String MQTT_URL = "tcp://localhost:1883";
    private static final int TIMEOUT = 30; // seconds
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(TIMEOUT);

    private final MqttAsyncClient client;

    public void setCallback(MqttTestCallback callback) {
        client.setCallback(callback);
    }

    public MqttTestClient() throws MqttException {
        this.client = createClient();
    }

    public MqttTestClient(String clientId) throws MqttException {
        this.client = createClient(clientId);
    }

    public void connectAndWait(String userName, String password) throws MqttException {
        IMqttToken connect = connect(userName, password);
        connect.waitForCompletion(TIMEOUT_MS);
    }

    public void connectAndWait(String userName) throws MqttException {
        connectAndWait(userName, null);
    }

    public void connectAndWait() throws MqttException {
        connectAndWait(null, null);
    }

    private IMqttToken connect(String userName, String password) throws MqttException {
        if (client == null) {
            throw new RuntimeException("Failed to connect! MqttAsyncClient is not initialized!");
        }
        MqttConnectOptions options = new MqttConnectOptions();
        if (StringUtils.isNotEmpty(userName)) {
            options.setUserName(userName);
        }
        if (StringUtils.isNotEmpty(password)) {
            options.setPassword(password.toCharArray());
        }
        return client.connect(options);
    }

    public void disconnectAndWait() throws MqttException {
        disconnect().waitForCompletion(TIMEOUT_MS);
    }

    public IMqttToken disconnect() throws MqttException {
        return client.disconnect();
    }

    public void disconnectForcibly() throws MqttException {
        client.disconnectForcibly(TIMEOUT_MS);
    }

    public void publishAndWait(String topic, byte[] payload) throws MqttException {
        publish(topic, payload).waitForCompletion(TIMEOUT_MS);
    }

    public IMqttDeliveryToken publish(String topic, byte[] payload) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        return client.publish(topic, message);
    }

    public void subscribeAndWait(String topic, MqttQoS qoS) throws MqttException {
        subscribe(topic, qoS).waitForCompletion(TIMEOUT_MS);
    }

    public IMqttToken subscribe(String topic, MqttQoS qoS) throws MqttException {
        return client.subscribe(topic, qoS.value());
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void enableManualAcks() {
        client.setManualAcks(true);
    }

    public void messageArrivedComplete(MqttMessage mqttMessage) throws MqttException {
        client.messageArrivedComplete(mqttMessage.getId(), mqttMessage.getQos());
    }

    private MqttAsyncClient createClient() throws MqttException {
        return createClient(null);
    }

    private MqttAsyncClient createClient(String clientId) throws MqttException {
        if (StringUtils.isEmpty(clientId)) {
            clientId = MqttAsyncClient.generateClientId();
        }
        return new MqttAsyncClient(MQTT_URL, clientId, new MemoryPersistence());
    }

}
