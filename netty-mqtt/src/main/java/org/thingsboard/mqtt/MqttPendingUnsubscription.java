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

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.function.Consumer;

@Getter(AccessLevel.PACKAGE)
final class MqttPendingUnsubscription {

    private final Promise<Void> future;
    private final String topic;

    @Getter(AccessLevel.NONE)
    private final RetransmissionHandler<MqttUnsubscribeMessage> retransmissionHandler;

    private MqttPendingUnsubscription(
            Promise<Void> future,
            String topic,
            MqttUnsubscribeMessage unsubscribeMessage,
            String ownerId,
            MqttClientConfig.RetransmissionConfig retransmissionConfig,
            PendingOperation operation
    ) {
        this.future = future;
        this.topic = topic;

        retransmissionHandler = new RetransmissionHandler<>(retransmissionConfig, operation, ownerId);
        retransmissionHandler.setOriginalMessage(unsubscribeMessage);
    }

    void startRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        retransmissionHandler.setHandler((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttUnsubscribeMessage(fixedHeader, originalMessage.variableHeader(), originalMessage.payload())));
        retransmissionHandler.start(eventLoop);
    }

    void onUnsubackReceived() {
        retransmissionHandler.stop();
    }

    void onChannelClosed() {
        retransmissionHandler.stop();
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private Promise<Void> future;
        private String topic;
        private MqttUnsubscribeMessage unsubscribeMessage;
        private String ownerId;
        private PendingOperation pendingOperation;
        private MqttClientConfig.RetransmissionConfig retransmissionConfig;

        Builder future(Promise<Void> future) {
            this.future = future;
            return this;
        }

        Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        Builder unsubscribeMessage(MqttUnsubscribeMessage unsubscribeMessage) {
            this.unsubscribeMessage = unsubscribeMessage;
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

        MqttPendingUnsubscription build() {
            return new MqttPendingUnsubscription(future, topic, unsubscribeMessage, ownerId, retransmissionConfig, pendingOperation);
        }

    }

}
