/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
import akka.event.LoggingAdapter;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.shared.SessionTimeoutMsg;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.device.DeviceToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.BasicSessionActorToAdaptorMsg;
import org.thingsboard.server.common.msg.session.SessionContext;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.common.msg.session.TransportToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;
import org.thingsboard.server.common.msg.session.ex.SessionException;

import java.util.Optional;

class SyncMsgProcessor extends AbstractSessionActorMsgProcessor {
    private DeviceToDeviceActorMsg pendingMsg;
    private Optional<ServerAddress> currentTargetServer;
    private boolean pendingResponse;

    public SyncMsgProcessor(ActorSystemContext ctx, LoggingAdapter logger, SessionId sessionId) {
        super(ctx, logger, sessionId);
    }

    @Override
    protected void processToDeviceActorMsg(ActorContext ctx, TransportToDeviceSessionActorMsg msg) {
        updateSessionCtx(msg, SessionType.SYNC);
        pendingMsg = toDeviceMsg(msg);
        pendingResponse = true;
        currentTargetServer = forwardToAppActor(ctx, pendingMsg);
        scheduleMsgWithDelay(ctx, new SessionTimeoutMsg(sessionId), getTimeout(systemContext, msg.getSessionMsg().getSessionContext()), ctx.parent());
    }

    public void processTimeoutMsg(ActorContext context, SessionTimeoutMsg msg) {
        if (pendingResponse) {
            try {
                sessionCtx.onMsg(SessionCloseMsg.onTimeout(sessionId));
            } catch (SessionException e) {
                logger.warning("Failed to push session close msg", e);
            }
            terminateSession(context, this.sessionId);
        }
    }

    public void processToDeviceMsg(ActorContext context, ToDeviceMsg msg) {
        try {
            sessionCtx.onMsg(new BasicSessionActorToAdaptorMsg(this.sessionCtx, msg));
            pendingResponse = false;
        } catch (SessionException e) {
            logger.warning("Failed to push session response msg", e);
        }
        terminateSession(context, this.sessionId);
    }

    @Override
    public void processClusterEvent(ActorContext context, ClusterEventMsg msg) {
        if (pendingResponse) {
            Optional<ServerAddress> newTargetServer = forwardToAppActorIfAddressChanged(context, pendingMsg, currentTargetServer);
            if (logger.isDebugEnabled()) {
                if (!newTargetServer.equals(currentTargetServer)) {
                    if (newTargetServer.isPresent()) {
                        logger.debug("[{}] Forwarded msg to new server: {}", sessionId, newTargetServer.get());
                    } else {
                        logger.debug("[{}] Forwarded msg to local server.", sessionId);
                    }
                }
            }
            currentTargetServer = newTargetServer;
        }
    }

    private long getTimeout(ActorSystemContext ctx, SessionContext sessionCtx) {
        return sessionCtx.getTimeout() > 0 ? sessionCtx.getTimeout() : ctx.getSyncSessionTimeout();
    }
}
