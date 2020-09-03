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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.init.SyncEdgeService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeNotificationService implements EdgeNotificationService {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private SyncEdgeService syncEdgeService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    private ExecutorService tsCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public TimePageData<EdgeEvent> findEdgeEvents(TenantId tenantId, EdgeId edgeId, TimePageLink pageLink) {
        return edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink, true);
    }

    @Override
    public Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) throws IOException {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = edgeService.saveEdge(edge);
        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN, ActionType.UPDATED, ruleChainId, null);
        return savedEdge;
    }

    private void saveEdgeEvent(TenantId tenantId,
                               EdgeId edgeId,
                               EdgeEventType edgeEventType,
                               ActionType edgeEventAction,
                               EntityId entityId,
                               JsonNode entityBody) {
        saveEdgeEvent(tenantId, edgeId, edgeEventType, edgeEventAction, entityId, entityBody, null);
    }

    private void saveEdgeEvent(TenantId tenantId,
                               EdgeId edgeId,
                               EdgeEventType edgeEventType,
                               ActionType edgeEventAction,
                               EntityId entityId,
                               JsonNode entityBody,
                               EntityGroupId entityGroupId) {
        log.debug("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], edgeEventType [{}], edgeEventAction[{}], entityId [{}], entityBody [{}], entityGroupId [{}]",
                tenantId, edgeId, edgeEventType, edgeEventAction, entityId, entityBody, entityGroupId);

        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeEventType(edgeEventType);
        edgeEvent.setEdgeEventAction(edgeEventAction.name());
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        if (entityGroupId != null) {
            edgeEvent.setEntityGroupId(entityGroupId.getId());
        }
        edgeEvent.setEntityBody(entityBody);
        edgeEventService.saveAsync(edgeEvent);
    }

    @Override
    public void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        try {
            TenantId tenantId = new TenantId(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
            EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
            switch (edgeEventType) {
                case EDGE:
                    processEdge(tenantId, edgeNotificationMsg);
                    break;
                case USER:
                case ASSET:
                case DEVICE:
                case ENTITY_VIEW:
                case DASHBOARD:
                case CUSTOMER:
                case RULE_CHAIN:
                case SCHEDULER_EVENT:
                case ENTITY_GROUP:
                    processEntity(tenantId, edgeNotificationMsg);
                    break;
                case WIDGETS_BUNDLE:
                case WIDGET_TYPE:
                    processWidgetBundleOrWidgetType(tenantId, edgeNotificationMsg);
                    break;
                case ALARM:
                    processAlarm(tenantId, edgeNotificationMsg);
                    break;
                case ROLE:
                    processRole(tenantId, edgeNotificationMsg);
                    break;
                case GROUP_PERMISSION:
                    processGroupPermission(tenantId, edgeNotificationMsg);
                    break;
                case RELATION:
                    processRelation(tenantId, edgeNotificationMsg);
                    break;
                default:
                    log.debug("Edge event type [{}] is not designed to be pushed to edge", edgeEventType);
            }
        } catch (Exception e) {
            callback.onFailure(e);
            log.error("Can't push to edge updates, edgeNotificationMsg [{}]", edgeNotificationMsg, e);
        } finally {
            callback.onSuccess();
        }
    }

    private void processEdge(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        ActionType edgeEventActionType = ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction());
        EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        ListenableFuture<Edge> edgeFuture;
        switch (edgeEventActionType) {
            case CHANGE_OWNER:
                edgeFuture = edgeService.findEdgeByIdAsync(tenantId, edgeId);
                Futures.addCallback(edgeFuture, new FutureCallback<Edge>() {
                    @Override
                    public void onSuccess(@Nullable Edge edge) {
                        try {
                            if (edge != null) {
                                EntityId previousOwnerId = mapper.readValue(edgeNotificationMsg.getEntityBody(), EntityId.class);
                                if (previousOwnerId != null && EntityType.CUSTOMER.equals(previousOwnerId.getEntityType())) {
                                    saveEdgeEvent(edge.getTenantId(), edge.getId(),
                                            EdgeEventType.CUSTOMER, ActionType.DELETED, previousOwnerId, null);
                                    unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.DEVICE, previousOwnerId);
                                    unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.ASSET, previousOwnerId);
                                    unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.ENTITY_VIEW, previousOwnerId);
                                    unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.USER, previousOwnerId);
                                    unassignEntityGroupsOfPreviousOwnerFromEdge(tenantId, edgeId, EntityType.DASHBOARD, previousOwnerId);
                                    unassignSchedulerEventsOfPreviousOwnerFromEdge(tenantId, edgeId, previousOwnerId);
                                }
                                if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
                                    syncEdgeService.syncEdgeOwner(edge);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to process change owner of edge [{}]", edge, e);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to find edge by id [{}]", tenantId, edgeNotificationMsg, t);
                    }
                }, dbCallbackExecutorService);
                break;
        }
    }

    private void unassignSchedulerEventsOfPreviousOwnerFromEdge(TenantId tenantId, EdgeId edgeId, EntityId previousOwnerId) {
        ListenableFuture<List<SchedulerEventInfo>> future = schedulerEventService.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId, edgeId);
        Futures.addCallback(future, new FutureCallback<List<SchedulerEventInfo>>() {
            @Override
            public void onSuccess(@Nullable List<SchedulerEventInfo> schedulerEventInfos) {
                if (schedulerEventInfos != null && !schedulerEventInfos.isEmpty()) {
                    for (SchedulerEventInfo schedulerEventInfo : schedulerEventInfos) {
                        if (schedulerEventInfo.getOwnerId().equals(previousOwnerId)) {
                            schedulerEventService.unassignSchedulerEventFromEdge(tenantId, schedulerEventInfo.getId(), edgeId);
                            // TODO: voba - remove of the customer should remove scheduler events as well - double check this
                            // saveEdgeEvent(tenantId, edgeId, EdgeEventType.SCHEDULER_EVENT, ActionType.UNASSIGNED_FROM_EDGE, schedulerEventInfo.getId(), null);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("[{}] Failed to find edge scheduler events [{}]", tenantId, edgeId, throwable);
            }
        }, dbCallbackExecutorService);
    }

    private void unassignEntityGroupsOfPreviousOwnerFromEdge(TenantId tenantId, EdgeId edgeId, EntityType groupType, EntityId previousOwnerId) {
        ListenableFuture<List<EntityGroup>> future = entityGroupService.findEdgeEntityGroupsByType(tenantId, edgeId, groupType);
        Futures.addCallback(future, new FutureCallback<List<EntityGroup>>() {
            @Override
            public void onSuccess(@Nullable List<EntityGroup> entityGroups) {
                if (entityGroups != null && !entityGroups.isEmpty()) {
                    for (EntityGroup entityGroup : entityGroups) {
                        if (entityGroup.getOwnerId().equals(previousOwnerId)) {
                            entityGroupService.unassignEntityGroupFromEdge(tenantId, entityGroup.getId(), edgeId, groupType);
                            // TODO: voba - remove of the customer should remove entity groups as well - double check this
                            // saveEdgeEvent(tenantId, edgeId, EdgeEventType.ENTITY_GROUP, ActionType.UNASSIGNED_FROM_EDGE, entityGroup.getId(), null);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("[{}] Failed to find edge entity groups by type [{}][{}]", tenantId, edgeId, groupType, throwable);
            }
        }, dbCallbackExecutorService);
    }

    private void processWidgetBundleOrWidgetType(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        ActionType edgeEventActionType = ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction());
        switch (edgeEventActionType) {
            case ADDED:
            case UPDATED:
            case DELETED:
                EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
                EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(edgeEventType,
                        new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
                TextPageData<Edge> edgesByTenantId = edgeService.findEdgesByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
                if (edgesByTenantId != null && edgesByTenantId.getData() != null && !edgesByTenantId.getData().isEmpty()) {
                    for (Edge edge : edgesByTenantId.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), edgeEventType, edgeEventActionType, entityId, null);
                    }
                }
                break;
        }
    }

    private void processEntity(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        ActionType edgeEventActionType = ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction());
        EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(edgeEventType, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (edgeEventActionType) {
            case ADDED:
            case UPDATED:
            case ADDED_TO_ENTITY_GROUP:
            case CREDENTIALS_UPDATED:
                ListenableFuture<List<EdgeId>> edgeIdsFuture = edgeService.findRelatedEdgeIdsByEntityId(tenantId, entityId, null);
                Futures.transform(edgeIdsFuture, edgeIds -> {
                    if (edgeIds != null && !edgeIds.isEmpty()) {
                        EntityGroupId entityGroupId = constructEntityGroupId(tenantId, edgeNotificationMsg);
                        for (EdgeId edgeId : edgeIds) {
                            try {
                                saveEdgeEvent(tenantId, edgeId, edgeEventType, edgeEventActionType, entityId, null, entityGroupId);
                            } catch (Exception e) {
                                log.error("[{}] Failed to push event to edge, edgeId [{}], edgeEventType [{}], edgeEventActionType [{}], entityId [{}]",
                                        tenantId, edgeId, edgeEventType, edgeEventActionType, entityId, e);
                            }
                        }
                    }
                    return null;
                }, dbCallbackExecutorService);
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
                TextPageData<Edge> edgesByTenantId = edgeService.findEdgesByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
                if (edgesByTenantId != null && edgesByTenantId.getData() != null && !edgesByTenantId.getData().isEmpty()) {
                    EntityGroupId entityGroupId = constructEntityGroupId(tenantId, edgeNotificationMsg);
                    for (Edge edge : edgesByTenantId.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), edgeEventType, edgeEventActionType, entityId, null, entityGroupId);
                    }
                }
                break;
            case ASSIGNED_TO_EDGE:
            case UNASSIGNED_FROM_EDGE:
                EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB()));
                saveEdgeEvent(tenantId, edgeId, edgeEventType, edgeEventActionType, entityId, null);
                if (edgeEventType.equals(EdgeEventType.RULE_CHAIN)) {
                    updateDependentRuleChains(tenantId, new RuleChainId(entityId.getId()), edgeId);
                }
                break;
            case CHANGE_OWNER:
                // TODO: voba
                break;
            case RELATIONS_DELETED:
                // TODO: voba - add support for relations deleted
                break;
        }
    }

    private void processRole(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        ActionType edgeEventActionType = ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction());
        EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(edgeEventType,
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (edgeEventActionType) {
            case ADDED:
            case UPDATED:
                ListenableFuture<Role> roleFuture = roleService.findRoleByIdAsync(tenantId, new RoleId(entityId.getId()));
                Futures.addCallback(roleFuture, new FutureCallback<Role>() {
                    @Override
                    public void onSuccess(@Nullable Role role) {
                        if (role != null) {
                            TextPageData<Edge> edgesByTenantId = edgeService.findEdgesByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
                            if (edgesByTenantId != null && edgesByTenantId.getData() != null && !edgesByTenantId.getData().isEmpty()) {
                                for (Edge edge : edgesByTenantId.getData()) {
                                    if (EntityType.TENANT.equals(role.getOwnerId().getEntityType()) ||
                                            edge.getOwnerId().equals(role.getOwnerId())) {
                                        saveEdgeEvent(tenantId, edge.getId(), edgeEventType, edgeEventActionType, entityId, null);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Failed to find role by id [{}]", edgeNotificationMsg, t);
                    }
                }, dbCallbackExecutorService);
                break;
            case DELETED:
                TextPageData<Edge> edgesByTenantId = edgeService.findEdgesByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
                if (edgesByTenantId != null && edgesByTenantId.getData() != null && !edgesByTenantId.getData().isEmpty()) {
                    for (Edge edge : edgesByTenantId.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), edgeEventType, edgeEventActionType, entityId, null);
                    }
                }
                break;
        }
    }

    private void processGroupPermission(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        ActionType edgeEventActionType = ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction());
        EdgeEventType edgeEventType = EdgeEventType.valueOf(edgeNotificationMsg.getEdgeEventType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(edgeEventType,
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (edgeEventActionType) {
            case ADDED:
            case UPDATED:
                ListenableFuture<GroupPermission> gpFuture = groupPermissionService.findGroupPermissionByIdAsync(tenantId, new GroupPermissionId(entityId.getId()));
                Futures.addCallback(gpFuture, new FutureCallback<GroupPermission>() {
                    @Override
                    public void onSuccess(@Nullable GroupPermission groupPermission) {
                        if (groupPermission != null) {
                            ListenableFuture<List<EdgeId>> edgesFuture = edgeService.findRelatedEdgeIdsByEntityId(tenantId, groupPermission.getUserGroupId(), EntityType.USER.name());
                            Futures.addCallback(edgesFuture, new FutureCallback<List<EdgeId>>() {
                                @Override
                                public void onSuccess(@Nullable List<EdgeId> edgeIds) {
                                    if (edgeIds != null && !edgeIds.isEmpty()) {
                                        for (EdgeId edgeId : edgeIds) {
                                            ListenableFuture<Boolean> checkFuture =
                                                    entityGroupService.checkEdgeEntityGroupById(tenantId, edgeId, groupPermission.getEntityGroupId(), groupPermission.getEntityGroupType());
                                            Futures.addCallback(checkFuture, new FutureCallback<Boolean>() {
                                                @Override
                                                public void onSuccess(@Nullable Boolean exists) {
                                                    if (Boolean.TRUE.equals(exists)) {
                                                        saveEdgeEvent(tenantId, edgeId, edgeEventType, edgeEventActionType, entityId, null);
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Throwable t) {
                                                    log.error("Failed to check edge entity group id [{}]", edgeNotificationMsg, t);
                                                }
                                            }, dbCallbackExecutorService);

                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.error("Failed to find edges by user group id [{}]", edgeNotificationMsg, t);
                                }
                            }, dbCallbackExecutorService);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Failed to find group permission by id [{}]", edgeNotificationMsg, t);
                    }
                }, dbCallbackExecutorService);
                break;
            case DELETED:
                TextPageData<Edge> edgesByTenantId = edgeService.findEdgesByTenantId(tenantId, new TextPageLink(Integer.MAX_VALUE));
                if (edgesByTenantId != null && edgesByTenantId.getData() != null && !edgesByTenantId.getData().isEmpty()) {
                    for (Edge edge : edgesByTenantId.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), edgeEventType, edgeEventActionType, entityId, null);
                    }
                }
                break;
        }
    }

    private void updateDependentRuleChains(TenantId tenantId, RuleChainId processingRuleChainId, EdgeId edgeId) {
        ListenableFuture<TimePageData<RuleChain>> future = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, new TimePageLink(Integer.MAX_VALUE));
        Futures.addCallback(future, new FutureCallback<TimePageData<RuleChain>>() {
            @Override
            public void onSuccess(@Nullable TimePageData<RuleChain> pageData) {
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    for (RuleChain ruleChain : pageData.getData()) {
                        if (!ruleChain.getId().equals(processingRuleChainId)) {
                            List<RuleChainConnectionInfo> connectionInfos =
                                    ruleChainService.loadRuleChainMetaData(ruleChain.getTenantId(), ruleChain.getId()).getRuleChainConnections();
                            if (connectionInfos != null && !connectionInfos.isEmpty()) {
                                for (RuleChainConnectionInfo connectionInfo : connectionInfos) {
                                    if (connectionInfo.getTargetRuleChainId().equals(processingRuleChainId)) {
                                        saveEdgeEvent(tenantId,
                                                edgeId,
                                                EdgeEventType.RULE_CHAIN_METADATA,
                                                ActionType.UPDATED,
                                                ruleChain.getId(),
                                                null);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Exception during updating dependent rule chains on sync!", t);
            }
        }, dbCallbackExecutorService);
    }

    private EntityGroupId constructEntityGroupId(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        if (edgeNotificationMsg.getEntityGroupIdMSB() != 0 && edgeNotificationMsg.getEntityGroupIdLSB() != 0) {
            EntityGroupId entityGroupId = new EntityGroupId(new UUID(edgeNotificationMsg.getEntityGroupIdMSB(), edgeNotificationMsg.getEntityGroupIdLSB()));
            EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, entityGroupId);
            if (entityGroup.isEdgeGroupAll()) {
                return null;
            } else {
                return entityGroupId;
            }
        } else {
            return null;
        }
    }

    private void processAlarm(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        AlarmId alarmId = new AlarmId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        ListenableFuture<Alarm> alarmFuture = alarmService.findAlarmByIdAsync(tenantId, alarmId);
        Futures.transform(alarmFuture, alarm -> {
            if (alarm != null) {
                EdgeEventType edgeEventType = getEdgeQueueTypeByEntityType(alarm.getOriginator().getEntityType());
                if (edgeEventType != null) {
                    ListenableFuture<List<EdgeId>> relatedEdgeIdsByEntityIdFuture = edgeService.findRelatedEdgeIdsByEntityId(tenantId, alarm.getOriginator(), null);
                    Futures.transform(relatedEdgeIdsByEntityIdFuture, relatedEdgeIdsByEntityId -> {
                        if (relatedEdgeIdsByEntityId != null) {
                            for (EdgeId edgeId : relatedEdgeIdsByEntityId) {
                                saveEdgeEvent(tenantId,
                                        edgeId,
                                        EdgeEventType.ALARM,
                                        ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction()),
                                        alarmId,
                                        null);
                            }
                        }
                        return null;
                    }, dbCallbackExecutorService);
                }
            }
            return null;
        }, dbCallbackExecutorService);
    }

    private void processRelation(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) throws JsonProcessingException {
        EntityRelation relation = mapper.readValue(edgeNotificationMsg.getEntityBody(), EntityRelation.class);
        if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
            List<ListenableFuture<List<EdgeId>>> futures = new ArrayList<>();
            futures.add(edgeService.findRelatedEdgeIdsByEntityId(tenantId, relation.getTo(), null));
            futures.add(edgeService.findRelatedEdgeIdsByEntityId(tenantId, relation.getFrom(), null));
            ListenableFuture<List<List<EdgeId>>> combinedFuture = Futures.allAsList(futures);
            Futures.transform(combinedFuture, listOfListsEdgeIds -> {
                Set<EdgeId> uniqueEdgeIds = new HashSet<>();
                if (listOfListsEdgeIds != null && !listOfListsEdgeIds.isEmpty()) {
                    for (List<EdgeId> listOfListsEdgeId : listOfListsEdgeIds) {
                        if (listOfListsEdgeId != null) {
                            uniqueEdgeIds.addAll(listOfListsEdgeId);
                        }
                    }
                }
                if (!uniqueEdgeIds.isEmpty()) {
                    for (EdgeId edgeId : uniqueEdgeIds) {
                        saveEdgeEvent(tenantId,
                                edgeId,
                                EdgeEventType.RELATION,
                                ActionType.valueOf(edgeNotificationMsg.getEdgeEventAction()),
                                null,
                                mapper.valueToTree(relation));
                    }
                }
                return null;
            }, dbCallbackExecutorService);
        }
    }

    private EdgeEventType getEdgeQueueTypeByEntityType(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return EdgeEventType.DEVICE;
            case ASSET:
                return EdgeEventType.ASSET;
            case ENTITY_VIEW:
                return EdgeEventType.ENTITY_VIEW;
            default:
                log.debug("Unsupported entity type: [{}]", entityType);
                return null;
        }
    }
}


