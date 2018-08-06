/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import akka.actor.ActorInitializationException;
import akka.actor.ActorRef;
import akka.actor.InvalidActorNameException;
import akka.actor.LocalActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.SessionTimeoutMsg;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.SessionAwareMsg;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.core.ActorSystemToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.core.SessionCloseMsg;
import org.thingsboard.server.common.msg.session.SessionCtrlMsg;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;

public class SessionManagerActor extends ContextAwareActor {

    private static final int INITIAL_SESSION_MAP_SIZE = 1024;

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final Map<String, ActorRef> sessionActors;

    SessionManagerActor(ActorSystemContext systemContext) {
        super(systemContext);
        this.sessionActors = new HashMap<>(INITIAL_SESSION_MAP_SIZE);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        //TODO Move everything here, to work with TbActorMsg
        return false;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof SessionCtrlMsg) {
            onSessionCtrlMsg((SessionCtrlMsg) msg);
        } else if (msg instanceof SessionAwareMsg) {
            forwardToSessionActor((SessionAwareMsg) msg);
        } else if (msg instanceof SessionTerminationMsg) {
            onSessionTermination((SessionTerminationMsg) msg);
        } else if (msg instanceof Terminated) {
            onTermination((Terminated) msg);
        } else if (msg instanceof SessionTimeoutMsg) {
            onSessionTimeout((SessionTimeoutMsg) msg);
        } else if (msg instanceof ClusterEventMsg) {
            broadcast(msg);
        }
    }

    private void broadcast(Object msg) {
        sessionActors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    private void onSessionTimeout(SessionTimeoutMsg msg) {
        String sessionIdStr = msg.getSessionId().toUidStr();
        ActorRef sessionActor = sessionActors.get(sessionIdStr);
        if (sessionActor != null) {
            sessionActor.tell(msg, ActorRef.noSender());
        }
    }

    private void onSessionCtrlMsg(SessionCtrlMsg msg) {
        String sessionIdStr = msg.getSessionId().toUidStr();
        ActorRef sessionActor = sessionActors.get(sessionIdStr);
        if (sessionActor != null) {
            sessionActor.tell(msg, ActorRef.noSender());
        }
    }

    private void onSessionTermination(SessionTerminationMsg msg) {
        String sessionIdStr = msg.getId().toUidStr();
        ActorRef sessionActor = sessionActors.remove(sessionIdStr);
        if (sessionActor != null) {
            log.debug("[{}] Removed session actor.", sessionIdStr);
            //TODO: onSubscriptionUpdate device actor about session close;
        } else {
            log.debug("[{}] Session actor was already removed.", sessionIdStr);
        }
    }

    private void forwardToSessionActor(SessionAwareMsg msg) {
        if (msg instanceof ActorSystemToDeviceSessionActorMsg || msg instanceof SessionCloseMsg) {
            String sessionIdStr = msg.getSessionId().toUidStr();
            ActorRef sessionActor = sessionActors.get(sessionIdStr);
            if (sessionActor != null) {
                sessionActor.tell(msg, ActorRef.noSender());
            } else {
                log.debug("[{}] Session actor was already removed.", sessionIdStr);
            }
        } else {
            try {
                getOrCreateSessionActor(msg.getSessionId()).tell(msg, self());
            } catch (InvalidActorNameException e) {
                log.info("Invalid msg : {}", msg);
            }
        }
    }

    private ActorRef getOrCreateSessionActor(SessionId sessionId) {
        String sessionIdStr = sessionId.toUidStr();
        ActorRef sessionActor = sessionActors.get(sessionIdStr);
        if (sessionActor == null) {
            log.debug("[{}] Creating session actor.", sessionIdStr);
            sessionActor = context().actorOf(
                    Props.create(new SessionActor.ActorCreator(systemContext, sessionId)).withDispatcher(DefaultActorService.SESSION_DISPATCHER_NAME),
                    sessionIdStr);
            sessionActors.put(sessionIdStr, sessionActor);
            log.debug("[{}] Created session actor.", sessionIdStr);
        }
        return sessionActor;
    }

    private void onTermination(Terminated message) {
        ActorRef terminated = message.actor();
        if (terminated instanceof LocalActorRef) {
            log.info("Removed actor: {}.", terminated);
            //TODO: cleanup session actors map
        } else {
            throw new IllegalStateException("Remote actors are not supported!");
        }
    }

    public static class ActorCreator extends ContextBasedCreator<SessionManagerActor> {
        private static final long serialVersionUID = 1L;

        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        @Override
        public SessionManagerActor create() throws Exception {
            return new SessionManagerActor(context);
        }
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("1 minute"), new Function<Throwable, SupervisorStrategy.Directive>() {
        @Override
        public SupervisorStrategy.Directive apply(Throwable t) {
            logger.error(t, "Unknown failure");
            return SupervisorStrategy.stop();
        }
    });
}
