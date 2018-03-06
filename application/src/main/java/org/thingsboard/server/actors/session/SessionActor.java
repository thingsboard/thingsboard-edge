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

import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.japi.Function;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.shared.SessionTimeoutMsg;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.core.ToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.session.ToDeviceActorSessionMsg;
import org.thingsboard.server.common.msg.session.SessionCtrlMsg;
import org.thingsboard.server.common.msg.session.SessionMsg;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.Duration;

public class SessionActor extends ContextAwareActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final SessionId sessionId;
    private AbstractSessionActorMsgProcessor processor;

    private SessionActor(ActorSystemContext systemContext, SessionId sessionId) {
        super(systemContext);
        this.sessionId = sessionId;
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(-1, Duration.Inf(),
                throwable -> {
                    logger.error(throwable, "Unknown session error");
                    if (throwable instanceof Error) {
                        return OneForOneStrategy.escalate();
                    } else {
                        return OneForOneStrategy.resume();
                    }
                });
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        logger.debug("[{}] Processing: {}.", sessionId, msg);
        if (msg instanceof ToDeviceActorSessionMsg) {
            processDeviceMsg((ToDeviceActorSessionMsg) msg);
        } else if (msg instanceof ToDeviceSessionActorMsg) {
            processToDeviceMsg((ToDeviceSessionActorMsg) msg);
        } else if (msg instanceof SessionTimeoutMsg) {
            processTimeoutMsg((SessionTimeoutMsg) msg);
        } else if (msg instanceof SessionCtrlMsg) {
            processSessionCtrlMsg((SessionCtrlMsg) msg);
        } else if (msg instanceof ClusterEventMsg) {
            processClusterEvent((ClusterEventMsg) msg);
        } else {
            logger.warning("[{}] Unknown msg: {}", sessionId, msg);
        }
    }

    private void processClusterEvent(ClusterEventMsg msg) {
        processor.processClusterEvent(context(), msg);
    }

    private void processDeviceMsg(ToDeviceActorSessionMsg msg) {
        initProcessor(msg);
        processor.processToDeviceActorMsg(context(), msg);
    }

    private void processToDeviceMsg(ToDeviceSessionActorMsg msg) {
        processor.processToDeviceMsg(context(), msg.getMsg());
    }

    private void processTimeoutMsg(SessionTimeoutMsg msg) {
        if (processor != null) {
            processor.processTimeoutMsg(context(), msg);
        } else {
            logger.warning("[{}] Can't process timeout msg: {} without processor", sessionId, msg);
        }
    }

    private void processSessionCtrlMsg(SessionCtrlMsg msg) {
        if (processor != null) {
            processor.processSessionCtrlMsg(context(), msg);
        } else if (msg instanceof SessionCloseMsg) {
            AbstractSessionActorMsgProcessor.terminateSession(context(), sessionId);
        } else {
            logger.warning("[{}] Can't process session ctrl msg: {} without processor", sessionId, msg);
        }
    }

    private void initProcessor(ToDeviceActorSessionMsg msg) {
        if (processor == null) {
            SessionMsg sessionMsg = (SessionMsg) msg.getSessionMsg();
            if (sessionMsg.getSessionContext().getSessionType() == SessionType.SYNC) {
                processor = new SyncMsgProcessor(systemContext, logger, sessionId);
            } else {
                processor = new ASyncMsgProcessor(systemContext, logger, sessionId);
            }
        }
    }

    public static class ActorCreator extends ContextBasedCreator<SessionActor> {
        private static final long serialVersionUID = 1L;

        private final SessionId sessionId;

        public ActorCreator(ActorSystemContext context, SessionId sessionId) {
            super(context);
            this.sessionId = sessionId;
        }

        @Override
        public SessionActor create() throws Exception {
            return new SessionActor(context, sessionId);
        }
    }

}
