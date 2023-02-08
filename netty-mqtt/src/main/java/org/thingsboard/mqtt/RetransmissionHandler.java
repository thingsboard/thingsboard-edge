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
package org.thingsboard.mqtt;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
final class RetransmissionHandler<T extends MqttMessage> {

    private volatile boolean stopped;
    private final PendingOperation pendingOperation;
    private ScheduledFuture<?> timer;
    private int timeout = 10;
    private BiConsumer<MqttFixedHeader, T> handler;
    private T originalMessage;

    void start(EventLoop eventLoop) {
        if (eventLoop == null) {
            throw new NullPointerException("eventLoop");
        }
        if (this.handler == null) {
            throw new NullPointerException("handler");
        }
        this.timeout = 10;
        this.startTimer(eventLoop);
    }

    private void startTimer(EventLoop eventLoop) {
        if (stopped || pendingOperation.isCanceled()) {
            return;
        }
        this.timer = eventLoop.schedule(() -> {
            if (stopped || pendingOperation.isCanceled()) {
                return;
            }
            this.timeout += 5;
            boolean isDup = this.originalMessage.fixedHeader().isDup();
            if (this.originalMessage.fixedHeader().messageType() == MqttMessageType.PUBLISH && this.originalMessage.fixedHeader().qosLevel() != MqttQoS.AT_MOST_ONCE) {
                isDup = true;
            }
            MqttFixedHeader fixedHeader = new MqttFixedHeader(this.originalMessage.fixedHeader().messageType(), isDup, this.originalMessage.fixedHeader().qosLevel(), this.originalMessage.fixedHeader().isRetain(), this.originalMessage.fixedHeader().remainingLength());
            handler.accept(fixedHeader, originalMessage);
            startTimer(eventLoop);
        }, timeout, TimeUnit.SECONDS);
    }

    void stop() {
        stopped = true;
        if (this.timer != null) {
            this.timer.cancel(true);
        }
    }

    void setHandle(BiConsumer<MqttFixedHeader, T> runnable) {
        this.handler = runnable;
    }

    void setOriginalMessage(T originalMessage) {
        this.originalMessage = originalMessage;
    }
}
