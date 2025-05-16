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
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNullElseGet;

@Getter(AccessLevel.PACKAGE)
final class MqttPendingSubscription {

    private final Promise<Void> future;
    private final String topic;
    private final Set<MqttPendingHandler> handlers;
    private final MqttSubscribeMessage subscribeMessage;

    @Getter(AccessLevel.NONE)
    private final RetransmissionHandler<MqttSubscribeMessage> retransmissionHandler;

    @Setter(AccessLevel.PACKAGE)
    private boolean sent = false;

    private MqttPendingSubscription(
            Promise<Void> future,
            String topic,
            Set<MqttPendingHandler> handlers,
            MqttSubscribeMessage subscribeMessage,
            String ownerId,
            MqttClientConfig.RetransmissionConfig retransmissionConfig,
            PendingOperation operation
    ) {
        this.future = future;
        this.topic = topic;
        this.handlers = requireNonNullElseGet(handlers, HashSet::new);
        this.subscribeMessage = subscribeMessage;

        retransmissionHandler = new RetransmissionHandler<>(retransmissionConfig, operation, ownerId);
        retransmissionHandler.setOriginalMessage(subscribeMessage);
    }

    record MqttPendingHandler(MqttHandler handler, boolean once) {}

    void addHandler(MqttHandler handler, boolean once) {
        handlers.add(new MqttPendingHandler(handler, once));
    }

    void startRetransmitTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        if (sent) { // If the packet is sent, we can start the retransmission timer
            retransmissionHandler.setHandler((fixedHeader, originalMessage) ->
                    sendPacket.accept(new MqttSubscribeMessage(fixedHeader, originalMessage.variableHeader(), originalMessage.payload())));
            retransmissionHandler.start(eventLoop);
        }
    }

    void onSubackReceived() {
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
        private Set<MqttPendingHandler> handlers;
        private MqttSubscribeMessage subscribeMessage;
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

        Builder handlers(Set<MqttPendingHandler> handlers) {
            this.handlers = handlers;
            return this;
        }

        Builder subscribeMessage(MqttSubscribeMessage subscribeMessage) {
            this.subscribeMessage = subscribeMessage;
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

        MqttPendingSubscription build() {
            return new MqttPendingSubscription(future, topic, handlers, subscribeMessage, ownerId, retransmissionConfig, pendingOperation);
        }

    }

}
