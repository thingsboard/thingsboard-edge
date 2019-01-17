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
package org.thingsboard.server.actors.tenant;

import akka.actor.ActorInitializationException;
import akka.actor.ActorRef;
import akka.actor.LocalActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.japi.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.device.DeviceActorCreator;
import org.thingsboard.server.actors.device.DeviceActorToRuleEngineMsg;
import org.thingsboard.server.actors.ruleChain.RuleChainManagerActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.rulechain.TenantRuleChainManager;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.DeviceAwareMsg;
import org.thingsboard.server.common.msg.aware.RuleChainAwareMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.integration.PlatformIntegrationService;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;

public class TenantActor extends RuleChainManagerActor {

    private final TenantId tenantId;
    private final BiMap<DeviceId, ActorRef> deviceActors;

    private TenantActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext, new TenantRuleChainManager(systemContext, tenantId));
        this.tenantId = tenantId;
        this.deviceActors = HashBiMap.create();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public void preStart() {
        log.info("[{}] Starting tenant actor.", tenantId);
        try {
            initRuleChains();
            log.info("[{}] Tenant actor started.", tenantId);
        } catch (Exception e) {
            log.warn("[{}] Unknown failure", tenantId);
            log.warn("Failure:", e);
        }
    }

    @Override
    public void postStop() {
        log.info("[{}] Stopping tenant actor.", tenantId);
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
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((DeviceAwareMsg) msg);
                break;
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
            case REMOTE_TO_RULE_CHAIN_TELL_NEXT_MSG:
                onRuleChainMsg((RuleChainAwareMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    private void onServiceToRuleEngineMsg(ServiceToRuleEngineMsg msg) {
        if (ruleChainManager.getRootChainActor() != null) {
            ruleChainManager.getRootChainActor().tell(msg, self());
        } else {
            log.info("[{}] No Root Chain: {}", tenantId, msg);
        }
    }

    private void onDeviceActorToRuleEngineMsg(DeviceActorToRuleEngineMsg msg) {
        if (ruleChainManager.getRootChainActor() != null) {
            ruleChainManager.getRootChainActor().tell(msg, self());
        } else {
            log.info("[{}] No Root Chain: {}", tenantId, msg);
        }
    }

    private void onRuleChainMsg(RuleChainAwareMsg msg) {
        ruleChainManager.getOrCreateActor(context(), msg.getRuleChainId()).tell(msg, self());
    }

    private void onToDeviceActorMsg(DeviceAwareMsg msg) {
        getOrCreateDeviceActor(msg.getDeviceId()).tell(msg, ActorRef.noSender());
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        if (msg.getEntityId().getEntityType() == EntityType.INTEGRATION) {
            IntegrationId integrationId = new IntegrationId(msg.getEntityId().getId());
            PlatformIntegrationService platformIntegrationService = systemContext.getPlatformIntegrationService();
            if (msg.getEvent() == ComponentLifecycleEvent.DELETED) {
                platformIntegrationService.deleteIntegration(integrationId);
            } else {
                Integration integration = systemContext.getIntegrationService().findIntegrationById(msg.getTenantId(), integrationId);
                if (msg.getEvent() == ComponentLifecycleEvent.CREATED) {
                    platformIntegrationService.createIntegration(integration);
                } else {
                    platformIntegrationService.updateIntegration(integration);
                }
            }
        } else if (msg.getEntityId().getEntityType() == EntityType.CONVERTER) {
            ConverterId converterById = new ConverterId(msg.getEntityId().getId());
            DataConverterService dataConverterService = systemContext.getDataConverterService();
            if (msg.getEvent() == ComponentLifecycleEvent.DELETED) {
                dataConverterService.deleteConverter(converterById);
            } else {
                Converter converter = systemContext.getConverterService().findConverterById(tenantId, converterById);
                if (msg.getEvent() == ComponentLifecycleEvent.CREATED) {
                    dataConverterService.createConverter(converter);
                } else {
                    dataConverterService.updateConverter(converter);
                }
            }

        } else {
            ActorRef target = getEntityActorRef(msg.getEntityId());
            if (target != null) {
                if (msg.getEntityId().getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChain ruleChain = systemContext.getRuleChainService().
                            findRuleChainById(tenantId, new RuleChainId(msg.getEntityId().getId()));
                    ruleChainManager.visit(ruleChain, target);
                }
                target.tell(msg, ActorRef.noSender());
            } else {
                log.debug("[{}] Invalid component lifecycle msg: {}", tenantId, msg);
            }
        }
    }

    private ActorRef getOrCreateDeviceActor(DeviceId deviceId) {
        return deviceActors.computeIfAbsent(deviceId, k -> {
            log.debug("[{}][{}] Creating device actor.", tenantId, deviceId);
            ActorRef deviceActor = context().actorOf(Props.create(new DeviceActorCreator(systemContext, tenantId, deviceId))
                            .withDispatcher(DefaultActorService.CORE_DISPATCHER_NAME)
                    , deviceId.toString());
            context().watch(deviceActor);
            log.debug("[{}][{}] Created device actor: {}.", tenantId, deviceId, deviceActor);
            return deviceActor;
        });
    }

    @Override
    protected void processTermination(Terminated message) {
        ActorRef terminated = message.actor();
        if (terminated instanceof LocalActorRef) {
            boolean removed = deviceActors.inverse().remove(terminated) != null;
            if (removed) {
                log.debug("[{}] Removed actor:", terminated);
            } else {
                log.warn("[{}] Removed actor was not found in the device map!");
            }
        } else {
            throw new IllegalStateException("Remote actors are not supported!");
        }
    }

    public static class ActorCreator extends ContextBasedCreator<TenantActor> {
        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId) {
            super(context);
            this.tenantId = tenantId;
        }

        @Override
        public TenantActor create() {
            return new TenantActor(context, tenantId);
        }
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("1 minute"), new Function<Throwable, SupervisorStrategy.Directive>() {
        @Override
        public SupervisorStrategy.Directive apply(Throwable t) {
            log.warn("[{}] Unknown failure", tenantId, t);
            if (t instanceof ActorInitializationException) {
                return SupervisorStrategy.stop();
            } else {
                return SupervisorStrategy.resume();
            }
        }
    });

}
