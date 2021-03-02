/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.tenant.TenantActor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class AppActor extends ContextAwareActor {

    private final TbTenantProfileCache tenantProfileCache;
    private final TenantService tenantService;
    private final Set<TenantId> deletedTenants;
    private boolean ruleChainsInitialized;

    private AppActor(ActorSystemContext systemContext) {
        super(systemContext);
        this.tenantProfileCache = systemContext.getTenantProfileCache();
        this.tenantService = systemContext.getTenantService();
        this.deletedTenants = new HashSet<>();
    }

    @Override
    protected boolean doProcess(TbActorMsg msg) {
        if (!ruleChainsInitialized) {
            initTenantActors();
            ruleChainsInitialized = true;
            if (msg.getMsgType() != MsgType.APP_INIT_MSG) {
                log.warn("Rule Chains initialized by unexpected message: {}", msg);
            }
        }
        switch (msg.getMsgType()) {
            case APP_INIT_MSG:
                break;
            case PARTITION_CHANGE_MSG:
                ctx.broadcastToChildren(msg);
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg, false);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg, true);
                break;
            case EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG:
                onToTenantActorMsg((EdgeEventUpdateMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    private void initTenantActors() {
        log.info("Starting main system actor.");
        try {
            // This Service may be started for specific tenant only.
            Optional<TenantId> isolatedTenantId = systemContext.getServiceInfoProvider().getIsolatedTenant();
            if (isolatedTenantId.isPresent()) {
                Tenant tenant = systemContext.getTenantService().findTenantById(isolatedTenantId.get());
                if (tenant != null) {
                    log.debug("[{}] Creating tenant actor", tenant.getId());
                    getOrCreateTenantActor(tenant.getId());
                    log.debug("Tenant actor created.");
                } else {
                    log.error("[{}] Tenant with such ID does not exist", isolatedTenantId.get());
                }
            } else if (systemContext.isTenantComponentsInitEnabled()) {
                PageDataIterable<Tenant> tenantIterator = new PageDataIterable<>(tenantService::findTenants, ENTITY_PACK_LIMIT);
                boolean isRuleEngine = systemContext.getServiceInfoProvider().isService(ServiceType.TB_RULE_ENGINE);
                boolean isCore = systemContext.getServiceInfoProvider().isService(ServiceType.TB_CORE);
                for (Tenant tenant : tenantIterator) {
                    TenantProfile tenantProfile = tenantProfileCache.get(tenant.getTenantProfileId());
                    if (isCore || (isRuleEngine && !tenantProfile.isIsolatedTbRuleEngine())) {
                        log.debug("[{}] Creating tenant actor", tenant.getId());
                        getOrCreateTenantActor(tenant.getId());
                        log.debug("[{}] Tenant actor created.", tenant.getId());
                    }
                }
            }
            log.info("Main system actor started.");
        } catch (Exception e) {
            log.warn("Unknown failure", e);
        }
    }

    private void onQueueToRuleEngineMsg(QueueToRuleEngineMsg msg) {
        if (TenantId.SYS_TENANT_ID.equals(msg.getTenantId())) {
            msg.getMsg().getCallback().onFailure(new RuleEngineException("Message has system tenant id!"));
        } else {
            if (!deletedTenants.contains(msg.getTenantId())) {
                getOrCreateTenantActor(msg.getTenantId()).tell(msg);
            } else {
                msg.getMsg().getCallback().onSuccess();
            }
        }
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        TbActorRef target = null;
        if (TenantId.SYS_TENANT_ID.equals(msg.getTenantId())) {
            if (!EntityType.TENANT_PROFILE.equals(msg.getEntityId().getEntityType())) {
                log.warn("Message has system tenant id: {}", msg);
            }
        } else {
            if (EntityType.TENANT.equals(msg.getEntityId().getEntityType())) {
                TenantId tenantId = new TenantId(msg.getEntityId().getId());
                if (msg.getEvent() == ComponentLifecycleEvent.DELETED) {
                    log.info("[{}] Handling tenant deleted notification: {}", msg.getTenantId(), msg);
                    deletedTenants.add(tenantId);
                    ctx.stop(new TbEntityActorId(tenantId));
                } else {
                    target = getOrCreateTenantActor(msg.getTenantId());
                }
            } else {
                target = getOrCreateTenantActor(msg.getTenantId());
            }
        }
        if (target != null) {
            target.tellWithHighPriority(msg);
        } else {
            log.debug("[{}] Invalid component lifecycle msg: {}", msg.getTenantId(), msg);
        }
    }

    private void onToDeviceActorMsg(TenantAwareMsg msg, boolean priority) {
        if (!deletedTenants.contains(msg.getTenantId())) {
            TbActorRef tenantActor = getOrCreateTenantActor(msg.getTenantId());
            if (priority) {
                tenantActor.tellWithHighPriority(msg);
            } else {
                tenantActor.tell(msg);
            }
        } else {
            if (msg instanceof TransportToDeviceActorMsgWrapper) {
                ((TransportToDeviceActorMsgWrapper) msg).getCallback().onSuccess();
            }
        }
    }

    private TbActorRef getOrCreateTenantActor(TenantId tenantId) {
        return ctx.getOrCreateChildActor(new TbEntityActorId(tenantId),
                () -> DefaultActorService.TENANT_DISPATCHER_NAME,
                () -> new TenantActor.ActorCreator(systemContext, tenantId));
    }

    private void onToTenantActorMsg(EdgeEventUpdateMsg msg) {
        TbActorRef target = null;
        if (ModelConstants.SYSTEM_TENANT.equals(msg.getTenantId())) {
            log.warn("Message has system tenant id: {}", msg);
        } else {
            target = getOrCreateTenantActor(msg.getTenantId());
        }
        if (target != null) {
            target.tellWithHighPriority(msg);
        } else {
            log.debug("[{}] Invalid edge event update msg: {}", msg.getTenantId(), msg);
        }
    }

    public static class ActorCreator extends ContextBasedCreator {

        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(new TenantId(EntityId.NULL_UUID));
        }

        @Override
        public TbActor createActor() {
            return new AppActor(context);
        }
    }

}
