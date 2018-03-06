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
package org.thingsboard.server.actors.tenant;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.device.DeviceActor;
import org.thingsboard.server.actors.plugin.PluginTerminationMsg;
import org.thingsboard.server.actors.rule.ComplexRuleActorChain;
import org.thingsboard.server.actors.rule.RuleActorChain;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.plugin.PluginManager;
import org.thingsboard.server.actors.shared.plugin.TenantPluginManager;
import org.thingsboard.server.actors.shared.rule.RuleManager;
import org.thingsboard.server.actors.shared.rule.TenantRuleManager;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.extensions.api.device.ToDeviceActorNotificationMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToPluginActorMsg;
import org.thingsboard.server.extensions.api.rules.ToRuleActorMsg;

public class TenantActor extends ContextAwareActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final TenantId tenantId;
    private final RuleManager ruleManager;
    private final PluginManager pluginManager;
    private final Map<DeviceId, ActorRef> deviceActors;

    private TenantActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.ruleManager = new TenantRuleManager(systemContext, tenantId);
        this.pluginManager = new TenantPluginManager(systemContext, tenantId);
        this.deviceActors = new HashMap<>();
    }

    @Override
    public void preStart() {
        logger.info("[{}] Starting tenant actor.", tenantId);
        try {
            ruleManager.init(this.context());
            pluginManager.init(this.context());
            logger.info("[{}] Tenant actor started.", tenantId);
        } catch (Exception e) {
            logger.error(e, "[{}] Unknown failure", tenantId);
        }
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        logger.debug("[{}] Received message: {}", tenantId, msg);
        if (msg instanceof RuleChainDeviceMsg) {
            process((RuleChainDeviceMsg) msg);
        } else if (msg instanceof ToDeviceActorMsg) {
            onToDeviceActorMsg((ToDeviceActorMsg) msg);
        } else if (msg instanceof ToPluginActorMsg) {
            onToPluginMsg((ToPluginActorMsg) msg);
        } else if (msg instanceof ToRuleActorMsg) {
            onToRuleMsg((ToRuleActorMsg) msg);
        } else if (msg instanceof ToDeviceActorNotificationMsg) {
            onToDeviceActorMsg((ToDeviceActorNotificationMsg) msg);
        } else if (msg instanceof ClusterEventMsg) {
            broadcast(msg);
        } else if (msg instanceof ComponentLifecycleMsg) {
            onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
        } else if (msg instanceof PluginTerminationMsg) {
            onPluginTerminated((PluginTerminationMsg) msg);
        } else {
            logger.warning("[{}] Unknown message: {}!", tenantId, msg);
        }
    }

    private void broadcast(Object msg) {
        pluginManager.broadcast(msg);
        deviceActors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    private void onToDeviceActorMsg(ToDeviceActorMsg msg) {
        getOrCreateDeviceActor(msg.getDeviceId()).tell(msg, ActorRef.noSender());
    }

    private void onToDeviceActorMsg(ToDeviceActorNotificationMsg msg) {
        getOrCreateDeviceActor(msg.getDeviceId()).tell(msg, ActorRef.noSender());
    }

    private void onToRuleMsg(ToRuleActorMsg msg) {
        ActorRef target = ruleManager.getOrCreateRuleActor(this.context(), msg.getRuleId());
        target.tell(msg, ActorRef.noSender());
    }

    private void onToPluginMsg(ToPluginActorMsg msg) {
        if (msg.getPluginTenantId().equals(tenantId)) {
            ActorRef pluginActor = pluginManager.getOrCreatePluginActor(this.context(), msg.getPluginId());
            pluginActor.tell(msg, ActorRef.noSender());
        } else {
            context().parent().tell(msg, ActorRef.noSender());
        }
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        Optional<PluginId> pluginId = msg.getPluginId();
        Optional<RuleId> ruleId = msg.getRuleId();
        if (pluginId.isPresent()) {
            ActorRef pluginActor = pluginManager.getOrCreatePluginActor(this.context(), pluginId.get());
            pluginActor.tell(msg, ActorRef.noSender());
        } else if (ruleId.isPresent()) {
            ActorRef target;
            Optional<ActorRef> ref = ruleManager.update(this.context(), ruleId.get(), msg.getEvent());
            if (ref.isPresent()) {
                target = ref.get();
            } else {
                logger.debug("Failed to find actor for rule: [{}]", ruleId);
                return;
            }
            target.tell(msg, ActorRef.noSender());
        } else {
            logger.debug("[{}] Invalid component lifecycle msg.", tenantId);
        }
    }

    private void onPluginTerminated(PluginTerminationMsg msg) {
        pluginManager.remove(msg.getId());
    }

    private void process(RuleChainDeviceMsg msg) {
        ToDeviceActorMsg toDeviceActorMsg = msg.getToDeviceActorMsg();
        ActorRef deviceActor = getOrCreateDeviceActor(toDeviceActorMsg.getDeviceId());
        RuleActorChain tenantChain = ruleManager.getRuleChain(this.context());
        RuleActorChain chain = new ComplexRuleActorChain(msg.getRuleChain(), tenantChain);
        deviceActor.tell(new RuleChainDeviceMsg(toDeviceActorMsg, chain), context().self());
    }

    private ActorRef getOrCreateDeviceActor(DeviceId deviceId) {
        return deviceActors.computeIfAbsent(deviceId, k -> context().actorOf(Props.create(new DeviceActor.ActorCreator(systemContext, tenantId, deviceId))
                .withDispatcher(DefaultActorService.CORE_DISPATCHER_NAME), deviceId.toString()));
    }

    public static class ActorCreator extends ContextBasedCreator<TenantActor> {
        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId) {
            super(context);
            this.tenantId = tenantId;
        }

        @Override
        public TenantActor create() throws Exception {
            return new TenantActor(context, tenantId);
        }
    }

}
