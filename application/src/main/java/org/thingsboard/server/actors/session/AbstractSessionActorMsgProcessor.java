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
package org.thingsboard.server.actors.session;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.actors.shared.SessionTimeoutMsg;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.device.BasicDeviceToDeviceActorMsg;
import org.thingsboard.server.common.msg.device.DeviceToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.AdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.common.msg.session.SessionContext;
import org.thingsboard.server.common.msg.session.SessionCtrlMsg;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.common.msg.session.TransportToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;

import java.util.Optional;

abstract class AbstractSessionActorMsgProcessor extends AbstractContextAwareMsgProcessor {

    protected final SessionId sessionId;
    protected SessionContext sessionCtx;
    protected DeviceToDeviceActorMsg deviceToDeviceActorMsgPrototype;

    protected AbstractSessionActorMsgProcessor(ActorSystemContext ctx, LoggingAdapter logger, SessionId sessionId) {
        super(ctx, logger);
        this.sessionId = sessionId;
    }

    protected abstract void processToDeviceActorMsg(ActorContext ctx, TransportToDeviceSessionActorMsg msg);

    protected abstract void processTimeoutMsg(ActorContext context, SessionTimeoutMsg msg);

    protected abstract void processToDeviceMsg(ActorContext context, ToDeviceMsg msg);

    public abstract void processClusterEvent(ActorContext context, ClusterEventMsg msg);

    protected void processSessionCtrlMsg(ActorContext ctx, SessionCtrlMsg msg) {
        if (msg instanceof SessionCloseMsg) {
            cleanupSession(ctx);
            terminateSession(ctx, sessionId);
        }
    }

    protected void cleanupSession(ActorContext ctx) {
    }

    protected void updateSessionCtx(TransportToDeviceSessionActorMsg msg, SessionType type) {
        sessionCtx = msg.getSessionMsg().getSessionContext();
        deviceToDeviceActorMsgPrototype = new BasicDeviceToDeviceActorMsg(msg, type);
    }

    protected DeviceToDeviceActorMsg toDeviceMsg(TransportToDeviceSessionActorMsg msg) {
        AdaptorToSessionActorMsg adaptorMsg = msg.getSessionMsg();
        return new BasicDeviceToDeviceActorMsg(deviceToDeviceActorMsgPrototype, adaptorMsg.getMsg());
    }

    protected Optional<DeviceToDeviceActorMsg> toDeviceMsg(FromDeviceMsg msg) {
        if (deviceToDeviceActorMsgPrototype != null) {
            return Optional.of(new BasicDeviceToDeviceActorMsg(deviceToDeviceActorMsgPrototype, msg));
        } else {
            return Optional.empty();
        }
    }

    protected Optional<ServerAddress> forwardToAppActor(ActorContext ctx, DeviceToDeviceActorMsg toForward) {
        Optional<ServerAddress> address = systemContext.getRoutingService().resolveById(toForward.getDeviceId());
        forwardToAppActor(ctx, toForward, address);
        return address;
    }

    protected Optional<ServerAddress> forwardToAppActorIfAddressChanged(ActorContext ctx, DeviceToDeviceActorMsg toForward, Optional<ServerAddress> oldAddress) {

        Optional<ServerAddress> newAddress = systemContext.getRoutingService().resolveById(toForward.getDeviceId());
        if (!newAddress.equals(oldAddress)) {
            getAppActor().tell(new SendToClusterMsg(toForward.getDeviceId(), toForward
                    .toOtherAddress(systemContext.getRoutingService().getCurrentServer())), ctx.self());
        }
        return newAddress;
    }

    protected void forwardToAppActor(ActorContext ctx, DeviceToDeviceActorMsg toForward, Optional<ServerAddress> address) {
        if (address.isPresent()) {
            systemContext.getRpcService().tell(systemContext.getEncodingService().convertToProtoDataMessage(address.get(),
                    toForward.toOtherAddress(systemContext.getRoutingService().getCurrentServer())));
        } else {
            getAppActor().tell(toForward, ctx.self());
        }
    }

    public static void terminateSession(ActorContext ctx, SessionId sessionId) {
        ctx.parent().tell(new SessionTerminationMsg(sessionId), ActorRef.noSender());
        ctx.stop(ctx.self());
    }

    public DeviceId getDeviceId() {
        return deviceToDeviceActorMsgPrototype.getDeviceId();
    }
}
