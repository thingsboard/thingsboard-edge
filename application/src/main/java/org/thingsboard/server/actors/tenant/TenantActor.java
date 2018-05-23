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

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.Function;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.device.DeviceActor;
import org.thingsboard.server.actors.device.DeviceActorToRuleEngineMsg;
import org.thingsboard.server.actors.ruleChain.RuleChainManagerActor;
import org.thingsboard.server.actors.ruleChain.RuleChainToRuleChainMsg;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.rulechain.TenantRuleChainManager;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.DeviceAwareMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;

public class TenantActor extends RuleChainManagerActor {

    private final TenantId tenantId;
    private final Map<DeviceId, ActorRef> deviceActors;

    private TenantActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext, new TenantRuleChainManager(systemContext, tenantId));
        this.tenantId = tenantId;
        this.deviceActors = new HashMap<>();
    }


    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public void preStart() {
        logger.info("[{}] Starting tenant actor.", tenantId);
        try {
            initRuleChains();
            logger.info("[{}] Tenant actor started.", tenantId);
        } catch (Exception e) {
            logger.error(e, "[{}] Unknown failure", tenantId);
        }
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case CLUSTER_EVENT_MSG:
                broadcast(msg);
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case SERVICE_TO_RULE_ENGINE_MSG:
                onServiceToRuleEngineMsg((ServiceToRuleEngineMsg) msg);
                break;
            case DEVICE_ACTOR_TO_RULE_ENGINE_MSG:
                onDeviceActorToRuleEngineMsg((DeviceActorToRuleEngineMsg) msg);
                break;
            case DEVICE_SESSION_TO_DEVICE_ACTOR_MSG:
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((DeviceAwareMsg) msg);
                break;
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
                onRuleChainMsg((RuleChainToRuleChainMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    protected void broadcast(Object msg) {
        super.broadcast(msg);
        deviceActors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    private void onServiceToRuleEngineMsg(ServiceToRuleEngineMsg msg) {
        ruleChainManager.getRootChainActor().tell(msg, self());
    }

    private void onDeviceActorToRuleEngineMsg(DeviceActorToRuleEngineMsg msg) {
        ruleChainManager.getRootChainActor().tell(msg, self());
    }

    private void onRuleChainMsg(RuleChainToRuleChainMsg msg) {
        ruleChainManager.getOrCreateActor(context(), msg.getTarget()).tell(msg, self());
    }


    private void onToDeviceActorMsg(DeviceAwareMsg msg) {
        getOrCreateDeviceActor(msg.getDeviceId()).tell(msg, ActorRef.noSender());
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        ActorRef target = getEntityActorRef(msg.getEntityId());
        if (target != null) {
            if (msg.getEntityId().getEntityType() == EntityType.RULE_CHAIN) {
                RuleChain ruleChain = systemContext.getRuleChainService().
                        findRuleChainById(new RuleChainId(msg.getEntityId().getId()));
                ruleChainManager.visit(ruleChain, target);
            }
            target.tell(msg, ActorRef.noSender());
        } else {
            logger.debug("Invalid component lifecycle msg: {}", msg);
        }
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

    private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("1 minute"), new Function<Throwable, SupervisorStrategy.Directive>() {
        @Override
        public SupervisorStrategy.Directive apply(Throwable t) {
            logger.error(t, "Unknown failure");
            return SupervisorStrategy.resume();
        }
    });

}
