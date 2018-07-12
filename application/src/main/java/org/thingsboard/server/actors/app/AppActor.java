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
package org.thingsboard.server.actors.app;

import akka.actor.ActorRef;
import akka.actor.LocalActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.ruleChain.RuleChainManagerActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.rulechain.SystemRuleChainManager;
import org.thingsboard.server.actors.tenant.TenantActor;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.core.BasicActorSystemToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.device.DeviceToDeviceActorMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.tenant.TenantService;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AppActor extends RuleChainManagerActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    public static final TenantId SYSTEM_TENANT = new TenantId(ModelConstants.NULL_UUID);
    private final TenantService tenantService;
    private final Map<TenantId, ActorRef> tenantActors;

    private AppActor(ActorSystemContext systemContext) {
        super(systemContext, new SystemRuleChainManager(systemContext));
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
            initRuleChains();

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
    protected boolean process(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case SEND_TO_CLUSTER_MSG:
                onPossibleClusterMsg((SendToClusterMsg) msg);
                break;
            case CLUSTER_EVENT_MSG:
                broadcast(msg);
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case SERVICE_TO_RULE_ENGINE_MSG:
                onServiceToRuleEngineMsg((ServiceToRuleEngineMsg) msg);
                break;
            case DEVICE_SESSION_TO_DEVICE_ACTOR_MSG:
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case REMOTE_TO_RULE_CHAIN_TELL_NEXT_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg);
                break;
            case ACTOR_SYSTEM_TO_DEVICE_SESSION_ACTOR_MSG:
                onToDeviceSessionMsg((BasicActorSystemToDeviceSessionActorMsg) msg);
            default:
                return false;
        }
        return true;
    }

    private void onToDeviceSessionMsg(BasicActorSystemToDeviceSessionActorMsg msg) {
        systemContext.getSessionManagerActor().tell(msg, self());
    }

    private void onPossibleClusterMsg(SendToClusterMsg msg) {
        Optional<ServerAddress> address = systemContext.getRoutingService().resolveById(msg.getEntityId());
        if (address.isPresent()) {
            systemContext.getRpcService().tell(
                    systemContext.getEncodingService().convertToProtoDataMessage(address.get(), msg.getMsg()));
        } else {
            self().tell(msg.getMsg(), ActorRef.noSender());
        }
    }

    private void onServiceToRuleEngineMsg(ServiceToRuleEngineMsg msg) {
        if (SYSTEM_TENANT.equals(msg.getTenantId())) {
            //TODO: ashvayka handle this.
        } else {
            getOrCreateTenantActor(msg.getTenantId()).tell(msg, self());
        }
    }

    @Override
    protected void broadcast(Object msg) {
        super.broadcast(msg);
        tenantActors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        ActorRef target;
        if (SYSTEM_TENANT.equals(msg.getTenantId())) {
            target = getEntityActorRef(msg.getEntityId());
        } else {
            target = getOrCreateTenantActor(msg.getTenantId());
        }
        if (target != null) {
            target.tell(msg, ActorRef.noSender());
        } else {
            logger.debug("Invalid component lifecycle msg: {}", msg);
        }
    }

    private void onToDeviceActorMsg(TenantAwareMsg msg) {
        getOrCreateTenantActor(msg.getTenantId()).tell(msg, ActorRef.noSender());
    }

    private void processDeviceMsg(DeviceToDeviceActorMsg deviceToDeviceActorMsg) {
        TenantId tenantId = deviceToDeviceActorMsg.getTenantId();
        ActorRef tenantActor = getOrCreateTenantActor(tenantId);
        if (deviceToDeviceActorMsg.getPayload().getMsgType().requiresRulesProcessing()) {
//            tenantActor.tell(new RuleChainDeviceMsg(deviceToDeviceActorMsg, ruleManager.getRuleChain(this.context())), context().self());
        } else {
            tenantActor.tell(deviceToDeviceActorMsg, context().self());
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
