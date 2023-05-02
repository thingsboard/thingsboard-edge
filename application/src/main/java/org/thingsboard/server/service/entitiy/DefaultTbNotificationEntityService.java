/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.entitiy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.msg.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.CloudUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTbNotificationEntityService implements TbNotificationEntityService {

    private final EntityActionService entityActionService;
    private final TbClusterService tbClusterService;
    private final GatewayNotificationsService gatewayNotificationsService;

    @Override
    public <I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, ActionType actionType,
                                                     User user, Exception e, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, null, null, actionType, user, e, additionalInfo);
    }

    @Override
    public <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity,
                                                                        ActionType actionType, User user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, null, actionType, user, null, additionalInfo);
    }

    @Override
    public <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity,
                                                                        ActionType actionType, User user, Exception e,
                                                                        Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, null, actionType, user, e, additionalInfo);
    }

    @Override
    public <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                                        ActionType actionType, User user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, null, additionalInfo);
    }

    @Override
    public <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity,
                                                                        CustomerId customerId, ActionType actionType,
                                                                        User user, Exception e, Object... additionalInfo) {
        if (user != null) {
            entityActionService.logEntityAction(user, entityId, entity, customerId, actionType, e, additionalInfo);
        } else if (e == null) {
            entityActionService.pushEntityActionToRuleEngine(entityId, entity, tenantId, customerId, actionType, additionalInfo);
        }
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyAddToEntityGroup(TenantId tenantId, I entityId, E entity,
                                                                               CustomerId customerId, EntityGroupId entityGroupId,
                                                                               User user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, ActionType.ADDED_TO_ENTITY_GROUP, user, null, additionalInfo);
        sendGroupEntityNotificationMsg(tenantId, entityId, EdgeEventActionType.ADDED_TO_ENTITY_GROUP, entityGroupId);
        sendGroupEntityNotificationMsgToCloud(tenantId, entityId,
                        CloudUtils.getCloudEventTypeByEntityType(entityId.getEntityType()),
                        EdgeEventActionType.ADDED_TO_ENTITY_GROUP, entityGroupId);
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyDeleteEntity(TenantId tenantId, I entityId, E entity,
                                                                           CustomerId customerId, ActionType actionType,
                                                                           List<EdgeId> relatedEdgeIds,
                                                                           User user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, additionalInfo);
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) { // send notification to all edges in customer hierarchy
            sendNotificationMsgToEdge(tenantId, null, entityId, null, null, EdgeEventActionType.DELETED);
        } else {
            sendDeleteNotificationMsg(tenantId, entityId, relatedEdgeIds, null);
            sendNotificationMsgToCloud(tenantId, entityId, CloudEventType.valueOf(entityId.getEntityType().name()), EdgeEventActionType.DELETED);
        }
    }

    @Override
    public void notifyDeleteAlarm(TenantId tenantId, Alarm alarm, EntityId originatorId, CustomerId customerId,
                                  List<EdgeId> relatedEdgeIds, User user, String body, Object... additionalInfo) {
        logEntityAction(tenantId, originatorId, alarm, customerId, ActionType.DELETED, user, additionalInfo);
        sendAlarmDeleteNotificationMsg(tenantId, alarm, relatedEdgeIds, body);
        sendAlarmDeleteNotificationMsgToCloud(alarm.getTenantId(), alarm.getId(), alarm);
    }

    @Override
    public void notifyDeleteRuleChain(TenantId tenantId, RuleChain ruleChain, List<EdgeId> relatedEdgeIds, User user) {
        RuleChainId ruleChainId = ruleChain.getId();
        logEntityAction(tenantId, ruleChainId, ruleChain, null, ActionType.DELETED, user, null, ruleChainId.toString());
        if (RuleChainType.EDGE.equals(ruleChain.getType())) {
            sendDeleteNotificationMsg(tenantId, ruleChainId, relatedEdgeIds, null);
        }
    }

    @Override
    public <I extends EntityId> void notifySendMsgToEdgeService(TenantId tenantId, I entityId, EdgeEventType edgeEventType,
                                                                EdgeEventActionType edgeEventActionType) {
        sendNotificationMsgToEdge(tenantId, null, entityId, null, edgeEventType, edgeEventActionType);
    }

    @Override
    public <I extends EntityId> void notifySendMsgToEdgeService(TenantId tenantId, I entityId, EdgeEventActionType edgeEventActionType) {
        sendEntityNotificationMsg(tenantId, entityId, edgeEventActionType, false);
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyAssignOrUnassignEntityToEdge(TenantId tenantId, I entityId,
                                                                                           CustomerId customerId, EdgeId edgeId,
                                                                                           E entity, ActionType actionType,
                                                                                           User user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, null, additionalInfo);
        sendEntityAssignToEdgeNotificationMsg(tenantId, edgeId, entityId, edgeTypeByActionType(actionType));
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyCreateOrUpdateEntity(TenantId tenantId, I entityId,
                                                                                   E entity, CustomerId customerId,
                                                                                   ActionType actionType, User user,
                                                                                   Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, null, additionalInfo);
        if (actionType == ActionType.UPDATED) {
            sendEntityNotificationMsg(tenantId, entityId, EdgeEventActionType.UPDATED, true);
        }
    }

    @Override
    public void notifyCreateOrUpdateTenant(Tenant tenant, ComponentLifecycleEvent event) {
        tbClusterService.onTenantChange(tenant, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenant.getId(), tenant.getId(), event);
    }

    @Override
    public void notifyDeleteTenant(Tenant tenant) {
        tbClusterService.onTenantDelete(tenant, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenant.getId(), tenant.getId(), ComponentLifecycleEvent.DELETED);
    }

    @Override
    public void notifyDeleteDevice(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                                   List<EdgeId> relatedEdgeIds, User user, Object... additionalInfo) {
        gatewayNotificationsService.onDeviceDeleted(device);
        tbClusterService.onDeviceDeleted(device, null);

        notifyDeleteEntity(tenantId, deviceId, device, customerId, ActionType.DELETED, relatedEdgeIds, user, additionalInfo);
    }

    @Override
    public void notifyUpdateDeviceCredentials(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                                              DeviceCredentials deviceCredentials, User user, boolean notifyCloud) {
        tbClusterService.pushMsgToCore(new DeviceCredentialsUpdateNotificationMsg(tenantId, deviceCredentials.getDeviceId(), deviceCredentials), null);
        sendEntityNotificationMsg(tenantId, deviceId, EdgeEventActionType.CREDENTIALS_UPDATED, notifyCloud);
        logEntityAction(tenantId, deviceId, device, customerId, ActionType.CREDENTIALS_UPDATED, user, deviceCredentials);
    }

    @Override
    public void notifyAssignDeviceToTenant(TenantId tenantId, TenantId newTenantId, DeviceId deviceId, CustomerId customerId,
                                           Device device, Tenant tenant, User user, Object... additionalInfo) {
        logEntityAction(tenantId, deviceId, device, customerId, ActionType.ASSIGNED_TO_TENANT, user, additionalInfo);
        pushAssignedFromNotification(tenant, newTenantId, device);
    }

    @Override
    public void notifyCreateOrUpdateOrDeleteEdge(TenantId tenantId, EdgeId edgeId, CustomerId customerId, Edge edge,
                                                 ActionType actionType, User user, Object... additionalInfo) {
        ComponentLifecycleEvent lifecycleEvent;
        switch (actionType) {
            case ADDED:
                lifecycleEvent = ComponentLifecycleEvent.CREATED;
                break;
            case UPDATED:
                lifecycleEvent = ComponentLifecycleEvent.UPDATED;
                break;
            case DELETED:
                lifecycleEvent = ComponentLifecycleEvent.DELETED;
                break;
            default:
                throw new IllegalArgumentException("Unknown actionType: " + actionType);
        }
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, edgeId, lifecycleEvent);
        logEntityAction(tenantId, edgeId, edge, customerId, actionType, user, additionalInfo);
    }

    @Override
    public void notifyCreateOrUpdateAlarm(AlarmInfo alarm, ActionType actionType, User user, boolean notifyCloud, Object... additionalInfo) {
        logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarm, alarm.getCustomerId(), actionType, user, additionalInfo);
        sendEntityNotificationMsg(alarm.getTenantId(), alarm.getId(), edgeTypeByActionType(actionType), notifyCloud);
    }

    @Override
    public void notifyAlarmComment(Alarm alarm, AlarmComment alarmComment, ActionType actionType, User user) {
        logEntityAction(alarm.getTenantId(), alarm.getId(), alarm, alarm.getCustomerId(), actionType, user, alarmComment);
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyCreateOrUpdateOrDelete(TenantId tenantId, CustomerId customerId,
                                                                                     I entityId, E entity, User user,
                                                                                     ActionType actionType, boolean sendNotifyMsgToEdge,
                                                                                     boolean notifyCloud, Exception e,
                                                                                     Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, e, additionalInfo);
        // TODO: @voba - in next release make sync between EDGE and CE notification services
        // move sendNotifyMsgToEdge variable into sendEntityNotificationMsg method
        // remove sendNotificationMsgToCloud duplicate
        if (sendNotifyMsgToEdge) {
            sendEntityNotificationMsg(tenantId, entityId, edgeTypeByActionType(actionType), notifyCloud);
        } else {
            if (notifyCloud) {
                sendNotificationMsgToCloud(tenantId, entityId, edgeTypeByActionType(actionType));
            }
        }
    }

    @Override
    public void notifyRelation(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user,
                               ActionType actionType, Object... additionalInfo) {
        logEntityAction(tenantId, relation.getFrom(), null, customerId, actionType, user, additionalInfo);
        logEntityAction(tenantId, relation.getTo(), null, customerId, actionType, user, additionalInfo);
        if (!EntityType.EDGE.equals(relation.getFrom().getEntityType()) && !EntityType.EDGE.equals(relation.getTo().getEntityType())) {
            sendNotificationMsgToEdge(tenantId, null, null, JacksonUtil.toString(relation),
                    EdgeEventType.RELATION, edgeTypeByActionType(actionType));
            sendNotificationMsgToCloud(tenantId, relation, edgeTypeByActionType(actionType));
        }
    }

    private void sendEntityNotificationMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType action, boolean notifyCloud) {
        sendEntityNotificationMsg(tenantId, entityId, action, null, notifyCloud);
    }

    private void sendEntityNotificationMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType action, String body, boolean notifyCloud) {
        sendNotificationMsgToEdge(tenantId, null, entityId, body, null, action);
        if (notifyCloud) {
            sendNotificationMsgToCloud(tenantId, entityId, action);
        }
    }

    private void sendAlarmDeleteNotificationMsg(TenantId tenantId, Alarm alarm, List<EdgeId> edgeIds, String body) {
        sendDeleteNotificationMsg(tenantId, alarm.getId(), edgeIds, body);
        sendAlarmDeleteNotificationMsgToCloud(tenantId, alarm.getId(), alarm);
    }

    private void sendDeleteNotificationMsg(TenantId tenantId, EntityId entityId, List<EdgeId> edgeIds, String body) {
        if (edgeIds != null && !edgeIds.isEmpty()) {
            for (EdgeId edgeId : edgeIds) {
                sendNotificationMsgToEdge(tenantId, edgeId, entityId, body, null, EdgeEventActionType.DELETED);
            }
        }
    }

    private void sendEntityAssignToEdgeNotificationMsg(TenantId tenantId, EdgeId edgeId, EntityId entityId, EdgeEventActionType action) {
        sendNotificationMsgToEdge(tenantId, edgeId, entityId, null, null, action, null, null);
    }

    private void sendGroupEntityNotificationMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType action, EntityGroupId entityGroupId) {
        sendNotificationMsgToEdge(tenantId, null, entityId, null, null, action, entityId.getEntityType(), entityGroupId);
    }

    private void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body, EdgeEventType type, EdgeEventActionType action,
                                           EntityType entityGroupType, EntityGroupId entityGroupId) {
        tbClusterService.sendNotificationMsgToEdge(tenantId, edgeId, entityId, body, type, action, entityGroupType, entityGroupId);
    }

    private void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body,
                                           EdgeEventType type, EdgeEventActionType action) {
        tbClusterService.sendNotificationMsgToEdge(tenantId, edgeId, entityId, body, type, action);
    }

    private void pushAssignedFromNotification(Tenant currentTenant, TenantId newTenantId, Device assignedDevice) {
        String data = JacksonUtil.toString(JacksonUtil.valueToTree(assignedDevice));
        if (data != null) {
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_ASSIGNED_FROM_TENANT, assignedDevice.getId(),
                    assignedDevice.getCustomerId(), getMetaDataForAssignedFrom(currentTenant), TbMsgDataType.JSON, data);
            tbClusterService.pushMsgToRuleEngine(newTenantId, assignedDevice.getId(), tbMsg, null);
        }
    }

    private TbMsgMetaData getMetaDataForAssignedFrom(Tenant tenant) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("assignedFromTenantId", tenant.getId().getId().toString());
        metaData.putValue("assignedFromTenantName", tenant.getName());
        return metaData;
    }

    public static EdgeEventActionType edgeTypeByActionType(ActionType actionType) {
        switch (actionType) {
            case ADDED:
                return EdgeEventActionType.ADDED;
            case UPDATED:
                return EdgeEventActionType.UPDATED;
            case ALARM_ACK:
                return EdgeEventActionType.ALARM_ACK;
            case ALARM_CLEAR:
                return EdgeEventActionType.ALARM_CLEAR;
            case ALARM_ASSIGNED:
                return EdgeEventActionType.ALARM_ASSIGNED;
            case ALARM_UNASSIGNED:
                return EdgeEventActionType.ALARM_UNASSIGNED;
            case DELETED:
                return EdgeEventActionType.DELETED;
            case RELATION_ADD_OR_UPDATE:
                return EdgeEventActionType.RELATION_ADD_OR_UPDATE;
            case RELATION_DELETED:
                return EdgeEventActionType.RELATION_DELETED;
            case ASSIGNED_TO_EDGE:
                return EdgeEventActionType.ASSIGNED_TO_EDGE;
            case UNASSIGNED_FROM_EDGE:
                return EdgeEventActionType.UNASSIGNED_FROM_EDGE;
            default:
                return null;
        }
    }

    @Override
    public void sendNotificationMsgToCloud(TenantId tenantId, EntityId entityId, EdgeEventActionType cloudEventAction) {
        CloudEventType cloudEventType = CloudUtils.getCloudEventTypeByEntityType(entityId.getEntityType());
        if (cloudEventType != null) {
            sendNotificationMsgToCloud(tenantId, entityId, cloudEventType, cloudEventAction);
        }
    }

    private void sendNotificationMsgToCloud(TenantId tenantId, EntityId entityId, CloudEventType cloudEventType,
                                            EdgeEventActionType cloudEventAction) {
        tbClusterService.sendNotificationMsgToCloud(tenantId, entityId, null, cloudEventType, cloudEventAction, null);
    }

    private void sendAlarmDeleteNotificationMsgToCloud(TenantId tenantId, EntityId entityId, Alarm alarm) {
        try {
            tbClusterService.sendNotificationMsgToCloud(tenantId, entityId, JacksonUtil.OBJECT_MAPPER.writeValueAsString(alarm), CloudEventType.ALARM, EdgeEventActionType.DELETED, null);
        } catch (Exception e) {
            log.warn("Failed to push delete alarm msg to core: {}", alarm, e);
        }
    }

    private void sendGroupEntityNotificationMsgToCloud(TenantId tenantId, EntityId entityId, CloudEventType cloudEventType,
                                                         EdgeEventActionType cloudEventAction, EntityGroupId entityGroupId) {
        tbClusterService.sendNotificationMsgToCloud(tenantId, entityId, null, cloudEventType, cloudEventAction, entityGroupId);
    }

    private void sendNotificationMsgToCloud(TenantId tenantId, EntityRelation relation, EdgeEventActionType cloudEventAction) {
        try {
            tbClusterService.sendNotificationMsgToCloud(tenantId, null, JacksonUtil.OBJECT_MAPPER.writeValueAsString(relation), CloudEventType.RELATION, cloudEventAction, null);
        } catch (Exception e) {
            log.warn("Failed to push relation to core: {}", relation, e);
        }
    }
}
