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
package org.thingsboard.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;

@Getter(AccessLevel.PACKAGE)
final class MqttPendingPublish {

    private final int messageId;
    private final Promise<Void> future;
    private final ByteBuf payload;
    private final MqttPublishMessage message;
    private final MqttQoS qos;

    @Getter(AccessLevel.NONE)
    private final RetransmissionHandler<MqttPublishMessage> publishRetransmissionHandler;
    @Getter(AccessLevel.NONE)
    private final RetransmissionHandler<MqttMessage> pubrelRetransmissionHandler;

    @Setter(AccessLevel.PACKAGE)
    private boolean sent = false;

    private MqttPendingPublish(
            int messageId,
            Promise<Void> future,
            ByteBuf payload,
            MqttPublishMessage message,
            MqttQoS qos,
            String ownerId,
            MqttClientConfig.RetransmissionConfig retransmissionConfig,
            PendingOperation pendingOperation
    ) {
        this.messageId = messageId;
        this.future = future;
        this.payload = payload;
        this.message = message;
        this.qos = qos;

        publishRetransmissionHandler = new RetransmissionHandler<>(retransmissionConfig, pendingOperation, ownerId);
        publishRetransmissionHandler.setOriginalMessage(message);
        pubrelRetransmissionHandler = new RetransmissionHandler<>(retransmissionConfig, pendingOperation, ownerId);
    }

    void startPublishRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        publishRetransmissionHandler.setHandler(((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttPublishMessage(fixedHeader, originalMessage.variableHeader(), payload.retain()))));
        publishRetransmissionHandler.start(eventLoop);
    }

    void onPubackReceived() {
        publishRetransmissionHandler.stop();
    }

    void setPubrelMessage(MqttMessage pubrelMessage) {
        pubrelRetransmissionHandler.setOriginalMessage(pubrelMessage);
    }

    void startPubrelRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        pubrelRetransmissionHandler.setHandler((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttMessage(fixedHeader, originalMessage.variableHeader())));
        pubrelRetransmissionHandler.start(eventLoop);
    }

    void onPubcompReceived() {
        pubrelRetransmissionHandler.stop();
    }

    void onChannelClosed() {
        publishRetransmissionHandler.stop();
        pubrelRetransmissionHandler.stop();
        if (payload != null) {
            payload.release();
        }
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private int messageId;
        private Promise<Void> future;
        private ByteBuf payload;
        private MqttPublishMessage message;
        private MqttQoS qos;
        private String ownerId;
        private MqttClientConfig.RetransmissionConfig retransmissionConfig;
        private PendingOperation pendingOperation;

        Builder messageId(int messageId) {
            this.messageId = messageId;
            return this;
        }

        Builder future(Promise<Void> future) {
            this.future = future;
            return this;
        }

        Builder payload(ByteBuf payload) {
            this.payload = payload;
            return this;
        }

        Builder message(MqttPublishMessage message) {
            this.message = message;
            return this;
        }

        Builder qos(MqttQoS qos) {
            this.qos = qos;
            return this;
        }

        Builder ownerId(String ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        Builder retransmissionConfig(MqttClientConfig.RetransmissionConfig retransmissionConfig) {
            this.retransmissionConfig = retransmissionConfig;
            return this;
        }

        Builder pendingOperation(PendingOperation pendingOperation) {
            this.pendingOperation = pendingOperation;
            return this;
        }

        MqttPendingPublish build() {
            return new MqttPendingPublish(messageId, future, payload, message, qos, ownerId, retransmissionConfig, pendingOperation);
        }

    }

}
