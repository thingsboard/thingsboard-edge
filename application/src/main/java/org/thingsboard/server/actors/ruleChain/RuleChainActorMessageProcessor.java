/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rpc.api.RpcCallback;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.device.DeviceActorToRuleEngineMsg;
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
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class RuleChainActorMessageProcessor extends ComponentMsgProcessor<RuleChainId> {

    private static final long DEFAULT_CLUSTER_PARTITION = 0L;
    private final ActorRef parent;
    private final ActorRef self;
    private final Map<RuleNodeId, RuleNodeCtx> nodeActors;
    private final Map<RuleNodeId, List<RuleNodeRelation>> nodeRoutes;
    private final RuleChainService service;

    private RuleNodeId firstId;
    private RuleNodeCtx firstNode;
    private boolean started;
    private String ruleChainName;

    RuleChainActorMessageProcessor(TenantId tenantId, RuleChainId ruleChainId, ActorSystemContext systemContext
            , ActorRef parent, ActorRef self) {
        super(systemContext, tenantId, ruleChainId);
        this.parent = parent;
        this.self = self;
        this.nodeActors = new HashMap<>();
        this.nodeRoutes = new HashMap<>();
        this.service = systemContext.getRuleChainService();
        this.ruleChainName = ruleChainId.toString();
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
                ruleChainName = ruleChain.getName();
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
    public void onClusterEventMsg(ClusterEventMsg msg) {
        nodeActors.values().stream().map(RuleNodeCtx::getSelfActor).forEach(actorRef -> actorRef.tell(msg, self));
    }

    private ActorRef createRuleNodeActor(ActorContext context, RuleNode ruleNode) {
        String dispatcherName = tenantId.getId().equals(EntityId.NULL_UUID) ?
                DefaultActorService.SYSTEM_RULE_DISPATCHER_NAME : DefaultActorService.TENANT_RULE_DISPATCHER_NAME;
        return context.actorOf(
                Props.create(new RuleNodeActor.ActorCreator(systemContext, tenantId, entityId, ruleNode.getId()))
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

    void onServiceToRuleEngineMsg(ServiceToRuleEngineMsg envelope) {
        log.trace("[{}][{}] Processing message [{}]: {}", entityId, firstId, envelope.getTbMsg().getId(), envelope.getTbMsg());
        checkActive();
        if (firstNode != null) {
            log.trace("[{}][{}] Pushing message to first rule node", entityId, firstId);
            pushMsgToNode(firstNode, enrichWithRuleChainId(envelope.getTbMsg()), "");
        }
    }

    void onDeviceActorToRuleEngineMsg(DeviceActorToRuleEngineMsg envelope) {
        checkActive();
        if (firstNode != null) {
            pushMsgToNode(firstNode, enrichWithRuleChainId(envelope.getTbMsg()), "");
        }
    }

    void onRuleChainToRuleChainMsg(RuleChainToRuleChainMsg envelope) {
        checkActive();
        if (envelope.isEnqueue()) {
            if (firstNode != null) {
                pushMsgToNode(firstNode, enrichWithRuleChainId(envelope.getMsg()), envelope.getFromRelationType());
            }
        } else {
            if (firstNode != null) {
                pushMsgToNode(firstNode, envelope.getMsg(), envelope.getFromRelationType());
            } else {
//                TODO: Ack this message in Kafka
//                TbMsg msg = envelope.getMsg();
//                EntityId ackId = msg.getRuleNodeId() != null ? msg.getRuleNodeId() : msg.getRuleChainId();
//                queue.ack(tenantId, envelope.getMsg(), ackId.getId(), msg.getClusterPartition());
            }
        }
    }

    void onTellNext(RuleNodeToRuleChainTellNextMsg envelope) {
        checkActive();
        TbMsg msg = envelope.getMsg();
        EntityId originatorEntityId = msg.getOriginator();
        Optional<ServerAddress> address = systemContext.getRoutingService().resolveById(originatorEntityId);

        if (address.isPresent()) {
            onRemoteTellNext(address.get(), envelope);
        } else {
            onLocalTellNext(envelope);
        }
    }

    private void onRemoteTellNext(ServerAddress serverAddress, RuleNodeToRuleChainTellNextMsg envelope) {
        TbMsg msg = envelope.getMsg();
        log.debug("Forwarding [{}] msg to remote server [{}] due to changed originator id: [{}]", msg.getId(), serverAddress, msg.getOriginator());
        envelope = new RemoteToRuleChainTellNextMsg(envelope, tenantId, entityId);
        systemContext.getRpcService().tell(systemContext.getEncodingService().convertToProtoDataMessage(serverAddress, envelope));
    }

    private void onLocalTellNext(RuleNodeToRuleChainTellNextMsg envelope) {
        TbMsg msg = envelope.getMsg();
        RuleNodeId originatorNodeId = envelope.getOriginator();
        List<RuleNodeRelation> relations = nodeRoutes.get(originatorNodeId).stream()
                .filter(r -> contains(envelope.getRelationTypes(), r.getType()))
                .collect(Collectors.toList());
        int relationsCount = relations.size();
        EntityId ackId = msg.getRuleNodeId() != null ? msg.getRuleNodeId() : msg.getRuleChainId();
        if (relationsCount == 0) {
            log.trace("[{}][{}][{}] No outbound relations to process", tenantId, entityId, msg.getId());
            if (ackId != null) {
//                TODO: Ack this message in Kafka
//                queue.ack(tenantId, msg, ackId.getId(), msg.getClusterPartition());
            }
        } else if (relationsCount == 1) {
            for (RuleNodeRelation relation : relations) {
                log.trace("[{}][{}][{}] Pushing message to single target: [{}]", tenantId, entityId, msg.getId(), relation.getOut());
                pushToTarget(msg, relation.getOut(), relation.getType());
            }
        } else {
            for (RuleNodeRelation relation : relations) {
                EntityId target = relation.getOut();
                log.trace("[{}][{}][{}] Pushing message to multiple targets: [{}]", tenantId, entityId, msg.getId(), relation.getOut());
                switch (target.getEntityType()) {
                    case RULE_NODE:
                        enqueueAndForwardMsgCopyToNode(msg, target, relation.getType());
                        break;
                    case RULE_CHAIN:
                        enqueueAndForwardMsgCopyToChain(msg, target, relation.getType());
                        break;
                }
            }
            //TODO: Ideally this should happen in async way when all targets confirm that the copied messages are successfully written to corresponding target queues.
            if (ackId != null) {
//                TODO: Ack this message in Kafka
//                queue.ack(tenantId, msg, ackId.getId(), msg.getClusterPartition());
            }
        }
    }

//    void onAckMsg(RuleNodeToRuleChainAckMsg envelope) {
//        TbMsg msg = envelope.getMsg();
//        EntityId ackId = msg.getRuleNodeId() != null ? msg.getRuleNodeId() : msg.getRuleChainId();
//        queue.ack(tenantId, tenantId, msg, ackId.getId(), msg.getClusterPartition());
//    }

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

    private void enqueueAndForwardMsgCopyToChain(TbMsg msg, EntityId target, String fromRelationType) {
        RuleChainId targetRCId = new RuleChainId(target.getId());
        TbMsg copyMsg = msg.copy(UUIDs.timeBased(), targetRCId, null, DEFAULT_CLUSTER_PARTITION);
        parent.tell(new RuleChainToRuleChainMsg(new RuleChainId(target.getId()), entityId, copyMsg, fromRelationType, true), self);
    }

    private void enqueueAndForwardMsgCopyToNode(TbMsg msg, EntityId target, String fromRelationType) {
        RuleNodeId targetId = new RuleNodeId(target.getId());
        RuleNodeCtx targetNodeCtx = nodeActors.get(targetId);
        TbMsg copy = msg.copy(UUIDs.timeBased(), entityId, targetId, DEFAULT_CLUSTER_PARTITION);
        pushMsgToNode(targetNodeCtx, copy, fromRelationType);
    }

    private void pushToTarget(TbMsg msg, EntityId target, String fromRelationType) {
        switch (target.getEntityType()) {
            case RULE_NODE:
                pushMsgToNode(nodeActors.get(new RuleNodeId(target.getId())), msg, fromRelationType);
                break;
            case RULE_CHAIN:
                parent.tell(new RuleChainToRuleChainMsg(new RuleChainId(target.getId()), entityId, msg, fromRelationType, false), self);
                break;
        }
    }

    private void pushMsgToNode(RuleNodeCtx nodeCtx, TbMsg msg, String fromRelationType) {
        if (nodeCtx != null) {
            nodeCtx.getSelfActor().tell(new RuleChainToRuleNodeMsg(new DefaultTbContext(systemContext, nodeCtx), msg, fromRelationType), self);
        }
        pushUpdatesToCloud(msg);
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
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_CREATED:
                try {
                    Device device = mapper.readValue(tbMsg.getData(), Device.class);
                    systemContext.getEdgeEventStorage().write(constructDeviceUpdateMsg(device), edgeEventSaveCallback);
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
                log.debug("Unsupported tbMsgType [{}]",  tbMsgType);
                return null;
        }
    }

    private UplinkMsg constructDeviceUpdateMsg(Device device) {
        DeviceUpdateMsg deviceUpdateMsg = DeviceUpdateMsg.newBuilder()
                .setName(device.getName())
                .setType(device.getType())
                .setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE)
                .build();

        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .addAllDeviceUpdateMsg(Collections.singletonList(deviceUpdateMsg));
        return builder.build();
    }

    private UplinkMsg constructAlarmUpdateMsg(Alarm alarm, UpdateMsgType updateMsgType) {
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .addAllAlarmUpdatemsg(Collections.singletonList(constructAlarmUpdatedMsg(alarm, updateMsgType)));
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

    private RpcCallback<Void> edgeEventSaveCallback = new RpcCallback<Void>() {
        @Override
        public void onSuccess(@Nullable Void aVoid) {
            log.debug("Event saved successfully!");
        }

        @Override
        public void onError(Throwable t) {
            log.debug("Failure during event save", t);
        }
    };

    private TbMsg enrichWithRuleChainId(TbMsg tbMsg) {
        // We don't put firstNodeId because it may change over time;
        return new TbMsg(tbMsg.getId(), tbMsg.getType(), tbMsg.getOriginator(), tbMsg.getMetaData().copy(), tbMsg.getData(), entityId, null, systemContext.getQueuePartitionId());
    }

}
