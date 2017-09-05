/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.actors.app;

import akka.actor.*;
import akka.actor.SupervisorStrategy.Directive;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.plugin.PluginTerminationMsg;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.plugin.PluginManager;
import org.thingsboard.server.actors.shared.plugin.SystemPluginManager;
import org.thingsboard.server.actors.shared.rule.RuleManager;
import org.thingsboard.server.actors.shared.rule.SystemRuleManager;
import org.thingsboard.server.actors.tenant.RuleChainDeviceMsg;
import org.thingsboard.server.actors.tenant.TenantActor;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.extensions.api.device.ToDeviceActorNotificationMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToPluginActorMsg;
import org.thingsboard.server.extensions.api.rules.ToRuleActorMsg;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AppActor extends ContextAwareActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    public static final TenantId SYSTEM_TENANT = new TenantId(ModelConstants.NULL_UUID);
    private final RuleManager ruleManager;
    private final PluginManager pluginManager;
    private final TenantService tenantService;
    private final Map<TenantId, ActorRef> tenantActors;

    private AppActor(ActorSystemContext systemContext) {
        super(systemContext);
        this.ruleManager = new SystemRuleManager(systemContext);
        this.pluginManager = new SystemPluginManager(systemContext);
        this.tenantService = systemContext.getTenantService();
        this.tenantActors = new HashMap<>();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public void preStart() {
        logger.info("Starting main system actor.");
        try {
            ruleManager.init(this.context());
            pluginManager.init(this.context());

            if (systemContext.isTenantComponentsInitEnabled()) {
                PageDataIterable<Tenant> tenantIterator = new PageDataIterable<>(tenantService::findTenants, ENTITY_PACK_LIMIT);
                for (Tenant tenant : tenantIterator) {
                    logger.debug("[{}] Creating tenant actor", tenant.getId());
                    getOrCreateTenantActor(tenant.getId());
                    logger.debug("Tenant actor created.");
                }
            }

            logger.info("Main system actor started.");
        } catch (Exception e) {
            logger.error(e, "Unknown failure");
        }
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        logger.debug("Received message: {}", msg);
        if (msg instanceof ToDeviceActorMsg) {
            processDeviceMsg((ToDeviceActorMsg) msg);
        } else if (msg instanceof ToPluginActorMsg) {
            onToPluginMsg((ToPluginActorMsg) msg);
        } else if (msg instanceof ToRuleActorMsg) {
            onToRuleMsg((ToRuleActorMsg) msg);
        } else if (msg instanceof ToDeviceActorNotificationMsg) {
            onToDeviceActorMsg((ToDeviceActorNotificationMsg) msg);
        } else if (msg instanceof Terminated) {
            processTermination((Terminated) msg);
        } else if (msg instanceof ClusterEventMsg) {
            broadcast(msg);
        } else if (msg instanceof ComponentLifecycleMsg) {
            onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
        } else if (msg instanceof PluginTerminationMsg) {
            onPluginTerminated((PluginTerminationMsg) msg);
        } else {
            logger.warning("Unknown message: {}!", msg);
        }
    }

    private void onPluginTerminated(PluginTerminationMsg msg) {
        pluginManager.remove(msg.getId());
    }

    private void broadcast(Object msg) {
        pluginManager.broadcast(msg);
        tenantActors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    private void onToRuleMsg(ToRuleActorMsg msg) {
        ActorRef target;
        if (SYSTEM_TENANT.equals(msg.getTenantId())) {
            target = ruleManager.getOrCreateRuleActor(this.context(), msg.getRuleId());
        } else {
            target = getOrCreateTenantActor(msg.getTenantId());
        }
        target.tell(msg, ActorRef.noSender());
    }

    private void onToPluginMsg(ToPluginActorMsg msg) {
        ActorRef target;
        if (SYSTEM_TENANT.equals(msg.getPluginTenantId())) {
            target = pluginManager.getOrCreatePluginActor(this.context(), msg.getPluginId());
        } else {
            target = getOrCreateTenantActor(msg.getPluginTenantId());
        }
        target.tell(msg, ActorRef.noSender());
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        ActorRef target = null;
        if (SYSTEM_TENANT.equals(msg.getTenantId())) {
            if (msg.getPluginId().isPresent()) {
                target = pluginManager.getOrCreatePluginActor(this.context(), msg.getPluginId().get());
            } else if (msg.getRuleId().isPresent()) {
                Optional<ActorRef> ref = ruleManager.update(this.context(), msg.getRuleId().get(), msg.getEvent());
                if (ref.isPresent()) {
                    target = ref.get();
                } else {
                    logger.debug("Failed to find actor for rule: [{}]", msg.getRuleId());
                    return;
                }
            }
        } else {
            target = getOrCreateTenantActor(msg.getTenantId());
        }
        if (target != null) {
            target.tell(msg, ActorRef.noSender());
        }
    }

    private void onToDeviceActorMsg(ToDeviceActorNotificationMsg msg) {
        getOrCreateTenantActor(msg.getTenantId()).tell(msg, ActorRef.noSender());
    }

    private void processDeviceMsg(ToDeviceActorMsg toDeviceActorMsg) {
        TenantId tenantId = toDeviceActorMsg.getTenantId();
        ActorRef tenantActor = getOrCreateTenantActor(tenantId);
        if (toDeviceActorMsg.getPayload().getMsgType().requiresRulesProcessing()) {
            tenantActor.tell(new RuleChainDeviceMsg(toDeviceActorMsg, ruleManager.getRuleChain(this.context())), context().self());
        } else {
            tenantActor.tell(toDeviceActorMsg, context().self());
        }
    }

    private ActorRef getOrCreateTenantActor(TenantId tenantId) {
        return tenantActors.computeIfAbsent(tenantId, k -> context().actorOf(Props.create(new TenantActor.ActorCreator(systemContext, tenantId))
                .withDispatcher(DefaultActorService.CORE_DISPATCHER_NAME), tenantId.toString()));
    }

    private void processTermination(Terminated message) {
        ActorRef terminated = message.actor();
        if (terminated instanceof LocalActorRef) {
            logger.debug("Removed actor: {}", terminated);
        } else {
            throw new IllegalStateException("Remote actors are not supported!");
        }
    }

    public static class ActorCreator extends ContextBasedCreator<AppActor> {
        private static final long serialVersionUID = 1L;

        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        @Override
        public AppActor create() throws Exception {
            return new AppActor(context);
        }
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("1 minute"), new Function<Throwable, Directive>() {
        @Override
        public Directive apply(Throwable t) {
            logger.error(t, "Unknown failure");
            if (t instanceof RuntimeException) {
                return SupervisorStrategy.restart();
            } else {
                return SupervisorStrategy.stop();
            }
        }
    });
}
