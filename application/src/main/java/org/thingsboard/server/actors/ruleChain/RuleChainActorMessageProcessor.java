/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.actors.ruleChain;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.sun.istack.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.common.MultipleTbQueueTbMsgCallbackWrapper;
import org.thingsboard.server.queue.common.TbQueueTbMsgCallbackWrapper;
import org.thingsboard.server.service.queue.TbClusterService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class RuleChainActorMessageProcessor extends ComponentMsgProcessor<RuleChainId> {

    private final ActorRef parent;
    private final ActorRef self;
    private final Map<RuleNodeId, RuleNodeCtx> nodeActors;
    private final Map<RuleNodeId, List<RuleNodeRelation>> nodeRoutes;
    private final RuleChainService service;
    private final TbClusterService clusterService;
    private final DeviceCredentialsService deviceCredentialsService;
    private String ruleChainName;

    private RuleNodeId firstId;
    private RuleNodeCtx firstNode;
    private boolean started;

    RuleChainActorMessageProcessor(TenantId tenantId, RuleChain ruleChain, ActorSystemContext systemContext
            , ActorRef parent, ActorRef self) {
        super(systemContext, tenantId, ruleChain.getId());
        this.ruleChainName = ruleChain.getName();
        this.parent = parent;
        this.self = self;
        this.nodeActors = new HashMap<>();
        this.nodeRoutes = new HashMap<>();
        this.service = systemContext.getRuleChainService();
        this.clusterService = systemContext.getClusterService();
        this.deviceCredentialsService = systemContext.getDeviceCredentialsService();
    }

    @Override
    public String getComponentName() {
        return null;
    }

    @Override
    public void start(ActorContext context) {
        if (!started) {
            RuleChain ruleChain = service.findRuleChainById(tenantId, entityId);
            if (ruleChain != null) {
                List<RuleNode> ruleNodeList = service.getRuleChainNodes(tenantId, entityId);
                log.trace("[{}][{}] Starting rule chain with {} nodes", tenantId, entityId, ruleNodeList.size());
                // Creating and starting the actors;
                for (RuleNode ruleNode : ruleNodeList) {
                    log.trace("[{}][{}] Creating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    ActorRef ruleNodeActor = createRuleNodeActor(context, ruleNode);
                    nodeActors.put(ruleNode.getId(), new RuleNodeCtx(tenantId, self, ruleNodeActor, ruleNode));
                }
                initRoutes(ruleChain, ruleNodeList);
                started = true;
            }
        } else {
            onUpdate(context);
        }
    }

    @Override
    public void onUpdate(ActorContext context) {
        RuleChain ruleChain = service.findRuleChainById(tenantId, entityId);
        if (ruleChain != null) {
            ruleChainName = ruleChain.getName();
            List<RuleNode> ruleNodeList = service.getRuleChainNodes(tenantId, entityId);
            log.trace("[{}][{}] Updating rule chain with {} nodes", tenantId, entityId, ruleNodeList.size());
            for (RuleNode ruleNode : ruleNodeList) {
                RuleNodeCtx existing = nodeActors.get(ruleNode.getId());
                if (existing == null) {
                    log.trace("[{}][{}] Creating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    ActorRef ruleNodeActor = createRuleNodeActor(context, ruleNode);
                    nodeActors.put(ruleNode.getId(), new RuleNodeCtx(tenantId, self, ruleNodeActor, ruleNode));
                } else {
                    log.trace("[{}][{}] Updating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    existing.setSelf(ruleNode);
                    existing.getSelfActor().tell(new ComponentLifecycleMsg(tenantId, existing.getSelf().getId(), ComponentLifecycleEvent.UPDATED), self);
                }
            }

            Set<RuleNodeId> existingNodes = ruleNodeList.stream().map(RuleNode::getId).collect(Collectors.toSet());
            List<RuleNodeId> removedRules = nodeActors.keySet().stream().filter(node -> !existingNodes.contains(node)).collect(Collectors.toList());
            removedRules.forEach(ruleNodeId -> {
                log.trace("[{}][{}] Removing rule node [{}]", tenantId, entityId, ruleNodeId);
                RuleNodeCtx removed = nodeActors.remove(ruleNodeId);
                removed.getSelfActor().tell(new ComponentLifecycleMsg(tenantId, removed.getSelf().getId(), ComponentLifecycleEvent.DELETED), self);
            });

            initRoutes(ruleChain, ruleNodeList);
        }
    }

    @Override
    public void stop(ActorContext context) {
        log.trace("[{}][{}] Stopping rule chain with {} nodes", tenantId, entityId, nodeActors.size());
        nodeActors.values().stream().map(RuleNodeCtx::getSelfActor).forEach(context::stop);
        nodeActors.clear();
        nodeRoutes.clear();
        context.stop(self);
        started = false;
    }

    @Override
    public void onPartitionChangeMsg(PartitionChangeMsg msg) {
        nodeActors.values().stream().map(RuleNodeCtx::getSelfActor).forEach(actorRef -> actorRef.tell(msg, self));
    }

    private ActorRef createRuleNodeActor(ActorContext context, RuleNode ruleNode) {
        String dispatcherName = tenantId.getId().equals(EntityId.NULL_UUID) ?
                DefaultActorService.SYSTEM_RULE_DISPATCHER_NAME : DefaultActorService.TENANT_RULE_DISPATCHER_NAME;
        return context.actorOf(
                Props.create(new RuleNodeActor.ActorCreator(systemContext, tenantId, entityId, ruleNode.getName(), ruleNode.getId()))
                        .withDispatcher(dispatcherName), ruleNode.getId().toString());
    }

    private void initRoutes(RuleChain ruleChain, List<RuleNode> ruleNodeList) {
        nodeRoutes.clear();
        // Populating the routes map;
        for (RuleNode ruleNode : ruleNodeList) {
            List<EntityRelation> relations = service.getRuleNodeRelations(TenantId.SYS_TENANT_ID, ruleNode.getId());
            log.trace("[{}][{}][{}] Processing rule node relations [{}]", tenantId, entityId, ruleNode.getId(), relations.size());
            if (relations.size() == 0) {
                nodeRoutes.put(ruleNode.getId(), Collections.emptyList());
            } else {
                for (EntityRelation relation : relations) {
                    log.trace("[{}][{}][{}] Processing rule node relation [{}]", tenantId, entityId, ruleNode.getId(), relation.getTo());
                    if (relation.getTo().getEntityType() == EntityType.RULE_NODE) {
                        RuleNodeCtx ruleNodeCtx = nodeActors.get(new RuleNodeId(relation.getTo().getId()));
                        if (ruleNodeCtx == null) {
                            throw new IllegalArgumentException("Rule Node [" + relation.getFrom() + "] has invalid relation to Rule node [" + relation.getTo() + "]");
                        }
                    }
                    nodeRoutes.computeIfAbsent(ruleNode.getId(), k -> new ArrayList<>())
                            .add(new RuleNodeRelation(ruleNode.getId(), relation.getTo(), relation.getType()));
                }
            }
        }

        firstId = ruleChain.getFirstRuleNodeId();
        firstNode = nodeActors.get(firstId);
        state = ComponentLifecycleState.ACTIVE;
    }

    void onQueueToRuleEngineMsg(QueueToRuleEngineMsg envelope) {
        TbMsg msg = envelope.getTbMsg();
        log.trace("[{}][{}] Processing message [{}]: {}", entityId, firstId, msg.getId(), msg);
        if (envelope.getRelationTypes() == null || envelope.getRelationTypes().isEmpty()) {
            try {
                checkActive(envelope.getTbMsg());
                RuleNodeId targetId = msg.getRuleNodeId();
                RuleNodeCtx targetCtx;
                if (targetId == null) {
                    targetCtx = firstNode;
                    msg = msg.copyWithRuleChainId(entityId);
                } else {
                    targetCtx = nodeActors.get(targetId);
                }
                if (targetCtx != null) {
                    log.trace("[{}][{}] Pushing message to target rule node", entityId, targetId);
                    pushMsgToNode(targetCtx, msg, "");
                    pushUpdatesToCloud(msg);
                } else {
                    log.trace("[{}][{}] Rule node does not exist. Probably old message", entityId, targetId);
                    msg.getCallback().onSuccess();
                }
            } catch (RuleNodeException rne) {
                envelope.getTbMsg().getCallback().onFailure(rne);
            } catch (Exception e) {
                envelope.getTbMsg().getCallback().onFailure(new RuleEngineException(e.getMessage()));
            }
        } else {
            onTellNext(envelope.getTbMsg(), envelope.getTbMsg().getRuleNodeId(), envelope.getRelationTypes(), envelope.getFailureMessage());
        }
    }

    void onRuleChainToRuleChainMsg(RuleChainToRuleChainMsg envelope) {
        try {
            checkActive(envelope.getMsg());
            if (firstNode != null) {
                pushMsgToNode(firstNode, envelope.getMsg(), envelope.getFromRelationType());
            } else {
                envelope.getMsg().getCallback().onSuccess();
            }
        } catch (RuleNodeException e) {
            log.debug("Rule Chain is not active. Current state [{}] for processor [{}][{}] tenant [{}]", state, entityId.getEntityType(), entityId, tenantId);
        }
    }

    void onTellNext(RuleNodeToRuleChainTellNextMsg envelope) {
        onTellNext(envelope.getMsg(), envelope.getOriginator(), envelope.getRelationTypes(), envelope.getFailureMessage());
    }

    private void onTellNext(TbMsg msg, RuleNodeId originatorNodeId, Set<String> relationTypes, String failureMessage) {
        try {
            checkActive(msg);
            EntityId entityId = msg.getOriginator();
            TopicPartitionInfo tpi = systemContext.resolve(ServiceType.TB_RULE_ENGINE, tenantId, entityId);
            List<RuleNodeRelation> relations = nodeRoutes.get(originatorNodeId).stream()
                    .filter(r -> contains(relationTypes, r.getType()))
                    .collect(Collectors.toList());
            int relationsCount = relations.size();
            if (relationsCount == 0) {
                log.trace("[{}][{}][{}] No outbound relations to process", tenantId, entityId, msg.getId());
                if (relationTypes.contains(TbRelationTypes.FAILURE)) {
                    RuleNodeCtx ruleNodeCtx = nodeActors.get(originatorNodeId);
                    if (ruleNodeCtx != null) {
                        msg.getCallback().onFailure(new RuleNodeException(failureMessage, ruleChainName, ruleNodeCtx.getSelf()));
                    } else {
                        log.debug("[{}] Failure during message processing by Rule Node [{}]. Enable and see debug events for more info", entityId, originatorNodeId.getId());
                        msg.getCallback().onFailure(new RuleEngineException("Failure during message processing by Rule Node [" + originatorNodeId.getId().toString() + "]"));
                    }
                } else {
                    msg.getCallback().onSuccess();
                }
            } else if (relationsCount == 1) {
                for (RuleNodeRelation relation : relations) {
                    log.trace("[{}][{}][{}] Pushing message to single target: [{}]", tenantId, entityId, msg.getId(), relation.getOut());
                    pushToTarget(tpi, msg, relation.getOut(), relation.getType());
                }
            } else {
                MultipleTbQueueTbMsgCallbackWrapper callbackWrapper = new MultipleTbQueueTbMsgCallbackWrapper(relationsCount, msg.getCallback());
                log.trace("[{}][{}][{}] Pushing message to multiple targets: [{}]", tenantId, entityId, msg.getId(), relations);
                for (RuleNodeRelation relation : relations) {
                    EntityId target = relation.getOut();
                    putToQueue(tpi, msg, callbackWrapper, target);
                }
            }
        } catch (RuleNodeException rne) {
            msg.getCallback().onFailure(rne);
        } catch (Exception e) {
            msg.getCallback().onFailure(new RuleEngineException("onTellNext - " + e.getMessage()));
        }
    }

    private void putToQueue(TopicPartitionInfo tpi, TbMsg msg, TbQueueCallback callbackWrapper, EntityId target) {
        switch (target.getEntityType()) {
            case RULE_NODE:
                putToQueue(tpi, msg.copyWithRuleNodeId(entityId, new RuleNodeId(target.getId())), callbackWrapper);
                break;
            case RULE_CHAIN:
                putToQueue(tpi, msg.copyWithRuleChainId(new RuleChainId(target.getId())), callbackWrapper);
                break;
        }
    }

    private void pushToTarget(TopicPartitionInfo tpi, TbMsg msg, EntityId target, String fromRelationType) {
        if (tpi.isMyPartition()) {
            switch (target.getEntityType()) {
                case RULE_NODE:
                    pushMsgToNode(nodeActors.get(new RuleNodeId(target.getId())), msg, fromRelationType);
                    break;
                case RULE_CHAIN:
                    parent.tell(new RuleChainToRuleChainMsg(new RuleChainId(target.getId()), entityId, msg, fromRelationType), self);
                    break;
            }
        } else {
            putToQueue(tpi, msg, new TbQueueTbMsgCallbackWrapper(msg.getCallback()), target);
        }
    }

    private void putToQueue(TopicPartitionInfo tpi, TbMsg newMsg, TbQueueCallback callbackWrapper) {
        ToRuleEngineMsg toQueueMsg = ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(newMsg))
                .build();
        clusterService.pushMsgToRuleEngine(tpi, newMsg.getId(), toQueueMsg, callbackWrapper);
    }

    private boolean contains(Set<String> relationTypes, String type) {
        if (relationTypes == null) {
            return true;
        }
        for (String relationType : relationTypes) {
            if (relationType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    private void pushMsgToNode(RuleNodeCtx nodeCtx, TbMsg msg, String fromRelationType) {
        if (nodeCtx != null) {
            nodeCtx.getSelfActor().tell(new RuleChainToRuleNodeMsg(new DefaultTbContext(systemContext, nodeCtx), msg, fromRelationType), self);
        } else {
            log.error("[{}][{}] RuleNodeCtx is empty", entityId, ruleChainName);
            msg.getCallback().onFailure(new RuleEngineException("Rule Node CTX is empty"));
        }
    }

    private void pushUpdatesToCloud(TbMsg tbMsg) {
        switch (tbMsg.getOriginator().getEntityType()) {
            case DEVICE:
                pushDeviceUpdatesToCloud(tbMsg);
                break;
            case ALARM:
                pushAlarmUpdatesToCloud(tbMsg);
                break;
        }
    }

    private void pushDeviceUpdatesToCloud(TbMsg tbMsg) {
        UpdateMsgType updateMsgType = null;
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_CREATED:
                updateMsgType = UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
                break;
            case DataConstants.ENTITY_UPDATED:
                updateMsgType = UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
                break;
            case DataConstants.ENTITY_DELETED:
                updateMsgType = UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
                break;
        }
        if (updateMsgType != null) {
            try {
                Device device = mapper.readValue(tbMsg.getData(), Device.class);
                UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                        .addAllDeviceUpdateMsg(Collections.singletonList(constructDeviceUpdateMsg(device, updateMsgType)));
                systemContext.getEdgeEventStorage().write(builder.build(), edgeEventSaveCallback);
            } catch (IOException e) {
                log.error("Can't push to edge updates, entity type [{}], data [{}]", tbMsg.getOriginator().getEntityType(), tbMsg.getData(), e);
            }
        }
    }

    private void pushAlarmUpdatesToCloud(TbMsg tbMsg) {
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_CREATED:
            case DataConstants.ENTITY_UPDATED:
            case DataConstants.ENTITY_DELETED:
            case DataConstants.ALARM_ACK:
            case DataConstants.ALARM_CLEAR:
                try {
                    UpdateMsgType updateMsgType = getUpdateMsgTypeByTbMsgType(tbMsg.getType());
                    Alarm alarm = mapper.readValue(tbMsg.getData(), Alarm.class);
                    systemContext.getEdgeEventStorage().write(constructAlarmUpdateMsg(alarm, updateMsgType), edgeEventSaveCallback);
                } catch (IOException e) {
                    log.error("Can't push to edge updates, entity type [{}], data [{}]", tbMsg.getOriginator().getEntityType(), tbMsg.getData(), e);
                }
        }
    }

    private UpdateMsgType getUpdateMsgTypeByTbMsgType(String tbMsgType) {
        switch (tbMsgType) {
            case DataConstants.ENTITY_CREATED:
                return UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DataConstants.ENTITY_UPDATED:
                return UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case DataConstants.ENTITY_DELETED:
                return UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case DataConstants.ALARM_ACK:
                return UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case DataConstants.ALARM_CLEAR:
                return UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default:
                log.debug("Unsupported tbMsgType [{}]", tbMsgType);
                return null;
        }
    }

    private DeviceUpdateMsg constructDeviceUpdateMsg(Device device, UpdateMsgType msgType) {
        DeviceUpdateMsg.Builder builder = DeviceUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(device.getId().getId().getMostSignificantBits())
                .setIdLSB(device.getId().getId().getLeastSignificantBits())
                .setName(device.getName())
                .setType(device.getType());
        if (device.getLabel() != null) {
            builder.setLabel(device.getLabel());
        }
        return builder.build();
    }

    private UplinkMsg constructAlarmUpdateMsg(Alarm alarm, UpdateMsgType updateMsgType) {
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .addAllAlarmUpdateMsg(Collections.singletonList(constructAlarmUpdatedMsg(alarm, updateMsgType)));
        return builder.build();
    }

    private AlarmUpdateMsg constructAlarmUpdatedMsg(Alarm alarm, UpdateMsgType msgType) {
        String entityName = null;
        switch (alarm.getOriginator().getEntityType()) {
            case DEVICE:
                entityName = systemContext.getDeviceService().findDeviceById(alarm.getTenantId(), new DeviceId(alarm.getOriginator().getId())).getName();
                break;
            case ASSET:
                entityName = systemContext.getAssetService().findAssetById(alarm.getTenantId(), new AssetId(alarm.getOriginator().getId())).getName();
                break;
            case ENTITY_VIEW:
                entityName = systemContext.getEntityViewService().findEntityViewById(alarm.getTenantId(), new EntityViewId(alarm.getOriginator().getId())).getName();
                break;
            default:
                log.debug("Unsupported tbMsgType [{}]", alarm.getOriginator().getEntityType());
                return null;
        }
        AlarmUpdateMsg.Builder builder = AlarmUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setName(alarm.getName())
                .setType(alarm.getName())
                .setOriginatorName(entityName)
                .setOriginatorType(alarm.getOriginator().getEntityType().name())
                .setSeverity(alarm.getSeverity().name())
                .setStatus(alarm.getStatus().name())
                .setStartTs(alarm.getStartTs())
                .setEndTs(alarm.getEndTs())
                .setAckTs(alarm.getAckTs())
                .setClearTs(alarm.getClearTs())
                .setDetails(JacksonUtil.toString(alarm.getDetails()))
                .setPropagate(alarm.isPropagate());
        return builder.build();
    }

    private IntegrationCallback<Void> edgeEventSaveCallback = new IntegrationCallback<Void>() {
        @Override
        public void onSuccess(@Nullable Void aVoid) {
            log.debug("Event saved successfully!");
        }

        @Override
        public void onError(Throwable t) {
            log.debug("Failure during event save", t);
        }
    };

    @Override
    protected RuleNodeException getInactiveException() {
        RuleNode firstRuleNode = firstNode != null ? firstNode.getSelf() : null;
        return new RuleNodeException("Rule Chain is not active!  Failed to initialize.", ruleChainName, firstRuleNode);
    }
}
