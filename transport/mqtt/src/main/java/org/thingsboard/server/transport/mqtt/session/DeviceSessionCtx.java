/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.transport.mqtt.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.session.SessionActorToAdaptorMsg;
import org.thingsboard.server.common.msg.session.SessionCtrlMsg;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;
import org.thingsboard.server.common.msg.session.ex.SessionException;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class DeviceSessionCtx extends DeviceAwareSessionContext {

    private final MqttTransportAdaptor adaptor;
    private final MqttSessionId sessionId;
    private ChannelHandlerContext channel;
    private volatile boolean allowAttributeResponses;
    private AtomicInteger msgIdSeq = new AtomicInteger(0);

    public DeviceSessionCtx(SessionMsgProcessor processor, DeviceAuthService authService, MqttTransportAdaptor adaptor) {
        super(processor, authService);
        this.adaptor = adaptor;
        this.sessionId = new MqttSessionId();
    }

    @Override
    public SessionType getSessionType() {
        return SessionType.ASYNC;
    }

    @Override
    public void onMsg(SessionActorToAdaptorMsg msg) throws SessionException {
        try {
            adaptor.convertToAdaptorMsg(this, msg).ifPresent(this::pushToNetwork);
        } catch (AdaptorException e) {
            //TODO: close channel with disconnect;
            logAndWrap(e);
        }
    }

    private void logAndWrap(AdaptorException e) throws SessionException {
        log.warn("Failed to convert msg: {}", e.getMessage(), e);
        throw new SessionException(e);
    }

    private void pushToNetwork(MqttMessage msg) {
        channel.writeAndFlush(msg);
    }

    @Override
    public void onMsg(SessionCtrlMsg msg) throws SessionException {
        if (msg instanceof SessionCloseMsg) {
            pushToNetwork(new MqttMessage(new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0)));
            channel.close();
        }
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public long getTimeout() {
        return 0;
    }

    @Override
    public SessionId getSessionId() {
        return sessionId;
    }

    public void setChannel(ChannelHandlerContext channel) {
        this.channel = channel;
    }

    public void setAllowAttributeResponses() {
        allowAttributeResponses = true;
    }

    public void setDisallowAttributeResponses() {
        allowAttributeResponses = false;
    }

    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }
}
