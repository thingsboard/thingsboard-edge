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
package org.thingsboard.server.transport.mqtt.mqttv5;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Data
public class MqttV5TestCallback implements MqttCallback {

    protected CountDownLatch subscribeLatch;
    protected final CountDownLatch deliveryLatch;
    protected int qoS;
    protected byte[] payloadBytes;
    protected String awaitSubTopic;
    protected boolean pubAckReceived;
    protected MqttMessage lastReceivedMessage;

    public MqttV5TestCallback() {
        this.subscribeLatch = new CountDownLatch(1);
        this.deliveryLatch = new CountDownLatch(1);
    }

    public MqttV5TestCallback(int subscribeCount) {
        this.subscribeLatch = new CountDownLatch(subscribeCount);
        this.deliveryLatch = new CountDownLatch(1);
    }

    public MqttV5TestCallback(String awaitSubTopic) {
        this.subscribeLatch = new CountDownLatch(1);
        this.deliveryLatch = new CountDownLatch(1);
        this.awaitSubTopic = awaitSubTopic;
    }

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
        if (mqttDisconnectResponse.getException() != null) {
            log.warn("connectionLost: ", mqttDisconnectResponse.getException());
            deliveryLatch.countDown();
        }
        log.warn("Disconnected with reason: {}", mqttDisconnectResponse.getReasonString());
    }

    @Override
    public void mqttErrorOccurred(MqttException e) {
        log.warn("Error occurred:", e);
    }

    @Override
    public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
        lastReceivedMessage = mqttMessage;
        if (awaitSubTopic == null) {
            log.warn("messageArrived on topic: {}", requestTopic);
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        } else {
            messageArrivedOnAwaitSubTopic(requestTopic, mqttMessage);
        }
    }

    protected void messageArrivedOnAwaitSubTopic(String requestTopic, MqttMessage mqttMessage) {
        log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
        if (awaitSubTopic.equals(requestTopic)) {
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        }
    }

    @Override
    public void deliveryComplete(IMqttToken iMqttToken) {
        log.warn("delivery complete: {}", iMqttToken.getResponse());
        pubAckReceived = iMqttToken.getResponse().getType() == MqttWireMessage.MESSAGE_TYPE_PUBACK;
        deliveryLatch.countDown();
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
