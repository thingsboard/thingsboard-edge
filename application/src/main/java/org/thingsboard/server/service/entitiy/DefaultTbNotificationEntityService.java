/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.msg.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.CloudUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbNotificationEntityService implements TbNotificationEntityService {
    private static final ObjectMapper json = new ObjectMapper();

    private final EntityActionService entityActionService;
    private final TbClusterService tbClusterService;
    private final GatewayNotificationsService gatewayNotificationsService;

    @Override
    public <E extends HasName, I extends EntityId> void notifyEntity(TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                                     ActionType actionType, SecurityUser user, Exception e,
                                                                     Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, e, additionalInfo);
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyAddToEntityGroup(TenantId tenantId, I entityId, E entity,
                                                                               CustomerId customerId, EntityGroupId entityGroupId,
                                                                               SecurityUser user, Object... additionalInfo) {
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
                                                                           SecurityUser user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, additionalInfo);
        sendDeleteNotificationMsg(tenantId, entityId, entity,  relatedEdgeIds);
        sendNotificationMsgToCloudService(tenantId, entityId, CloudEventType.valueOf(entityId.getEntityType().name()), EdgeEventActionType.DELETED);
    }

    @Override
    public <E extends HasName, I extends EntityId> void notifyCreateOrUpdateEntity(TenantId tenantId, I entityId,
                                                                                   CustomerId customerId, E entity,
                                                                                   ActionType actionType, SecurityUser user,
                                                                                   Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, additionalInfo);

        if (actionType == ActionType.UPDATED) {
            sendEntityNotificationMsg(tenantId, entityId, EdgeEventActionType.UPDATED);
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
                                   List<EdgeId> relatedEdgeIds, SecurityUser user, Object... additionalInfo) {
        gatewayNotificationsService.onDeviceDeleted(device);
        tbClusterService.onDeviceDeleted(device, null);

        notifyDeleteEntity(tenantId, deviceId, device, customerId, ActionType.DELETED, relatedEdgeIds, user, false, additionalInfo);
    }

    @Override
    public void notifyUpdateDeviceCredentials(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                                              DeviceCredentials deviceCredentials, SecurityUser user) {
        tbClusterService.pushMsgToCore(new DeviceCredentialsUpdateNotificationMsg(tenantId, deviceCredentials.getDeviceId(), deviceCredentials), null);
        sendEntityNotificationMsg(tenantId, deviceId, EdgeEventActionType.CREDENTIALS_UPDATED);
        logEntityAction(tenantId, deviceId, device, customerId, ActionType.CREDENTIALS_UPDATED, user, deviceCredentials);
        sendNotificationMsgToCloudService(tenantId, device.getId(), CloudEventType.DEVICE, EdgeEventActionType.CREDENTIALS_UPDATED);
    }

    @Override
    public void notifyAssignDeviceToTenant(TenantId tenantId, TenantId newTenantId, DeviceId deviceId, CustomerId customerId,
                                           Device device, Tenant tenant, SecurityUser user, Object... additionalInfo) {
        logEntityAction(tenantId, deviceId, device, customerId, ActionType.ASSIGNED_TO_TENANT, user, additionalInfo);
        pushAssignedFromNotification(tenant, newTenantId, device);
    }

    @Override
    public void notifyEdge(TenantId tenantId, EdgeId edgeId, CustomerId customerId, Edge edge, ActionType actionType,
                           SecurityUser user, Object... additionalInfo) {
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
    public void notifyCreateOrUpdateAlarm(Alarm alarm, ActionType actionType, SecurityUser user, Object... additionalInfo) {
        logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarm, alarm.getCustomerId(), actionType, user, additionalInfo);
        sendEntityNotificationMsg(alarm.getTenantId(), alarm.getId(), edgeTypeByActionType(actionType));
        sendNotificationMsgToCloudService(alarm.getTenantId(), alarm.getId(), edgeTypeByActionType(actionType));
    }

    @Override
    public void notifyDeleteAlarm(Alarm alarm, SecurityUser user, List<EdgeId> relatedEdgeIds) {
        logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarm, alarm.getCustomerId(), ActionType.ALARM_DELETE, user, null);
        sendAlarmDeleteNotificationMsg(alarm, relatedEdgeIds);
        sendAlarmDeleteNotificationMsgToCloudService(alarm.getTenantId(), alarm.getId(), alarm);
    }

    @Override
    public void notifyDeleteCustomer(Customer customer, SecurityUser user, List<EdgeId> edgeIds) {
        logEntityAction(customer.getTenantId(), customer.getId(), customer, customer.getId(), ActionType.DELETED, user, null);
        sendDeleteNotificationMsg(customer.getTenantId(), customer.getId(), customer, edgeIds);
        tbClusterService.broadcastEntityStateChangeEvent(customer.getTenantId(), customer.getId(), ComponentLifecycleEvent.DELETED);
    }

    private <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                                         ActionType actionType, SecurityUser user, Object... additionalInfo) {
        logEntityAction(tenantId, entityId, entity, customerId, actionType, user, null, additionalInfo);
    }

    private <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                                         ActionType actionType, SecurityUser user, Exception e, Object... additionalInfo) {
        if (user != null) {
            entityActionService.logEntityAction(user, entityId, entity, customerId, actionType, e, additionalInfo);
        } else if (e == null) {
            entityActionService.pushEntityActionToRuleEngine(entityId, entity, tenantId, customerId, actionType, null, additionalInfo);
        }
    }

    protected void sendChangeOwnerNotificationMsg(TenantId tenantId, EntityId entityId, List<EdgeId> edgeIds, EntityId previousOwnerId) {
        if (edgeIds != null && !edgeIds.isEmpty()) {
            for (EdgeId edgeId : edgeIds) {
                String body = null;
                if (EntityType.EDGE.equals(entityId.getEntityType())) {
                    try {
                        body = json.writeValueAsString(previousOwnerId);
                    } catch (Exception e) {
                        log.warn("[{}][{}] Failed to push change owner event to core: {} {}", tenantId, entityId, previousOwnerId, e);
                    }
                    tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityId, ComponentLifecycleEvent.UPDATED);
                }
                sendNotificationMsgToEdgeService(tenantId, edgeId, entityId, body, EdgeEventActionType.CHANGE_OWNER);
            }
        }
    }

    protected void sendRelationNotificationMsg(TenantId tenantId, EntityRelation relation, EdgeEventActionType action) {
        try {
            if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                    !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
                sendNotificationMsgToEdgeService(tenantId, null, null, json.writeValueAsString(relation), EdgeEventType.RELATION, action, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to push relation to core: {}", relation, e);
        }
    }

    protected void sendDeleteNotificationMsg(TenantId tenantId, EntityId entityId, List<EdgeId> edgeIds) {
        sendDeleteNotificationMsg(tenantId, entityId, edgeIds, null);
    }

    protected <E extends HasName, I extends EntityId> void sendDeleteNotificationMsg(TenantId tenantId, I entityId, E entity, List<EdgeId> edgeIds) {
        try {
            sendDeleteNotificationMsg(tenantId, entityId,  edgeIds, null);
        } catch (Exception e) {
            log.warn("Failed to push delete " + entity.getClass().getName() + " msg to core: {}", entity, e);
        }
    }

    private void sendDeleteNotificationMsg(TenantId tenantId, EntityId entityId, List<EdgeId> edgeIds, String body) {
        if (edgeIds != null && !edgeIds.isEmpty()) {
            for (EdgeId edgeId : edgeIds) {
                sendNotificationMsgToEdgeService(tenantId, edgeId, entityId, body, EdgeEventActionType.DELETED);
            }
        }
    }

    protected void sendAlarmDeleteNotificationMsg(Alarm alarm, List<EdgeId> relatedEdgeIds) {
        try {
            sendDeleteNotificationMsg(alarm.getTenantId(), alarm.getId(), relatedEdgeIds, json.writeValueAsString(alarm));
        } catch (Exception e) {
            log.warn("Failed to push delete alarm msg to core: {}", alarm, e);
        }
    }

    protected void sendEntityNotificationMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType action) {
        sendNotificationMsgToEdgeService(tenantId, null, entityId, null, action);
    }

    protected void sendEntityAssignToEdgeNotificationMsg(TenantId tenantId, EdgeId edgeId, EntityId entityId, EdgeEventActionType action) {
        sendNotificationMsgToEdgeService(tenantId, edgeId, entityId, null, action);
    }

    protected void sendEntityAssignToEdgeNotificationMsg(TenantId tenantId, EdgeId edgeId, EntityId entityId, EntityType groupType, EdgeEventActionType action) {
        sendNotificationMsgToEdgeService(tenantId, edgeId, entityId, null, null, action, groupType, null);
    }

    protected void sendGroupEntityNotificationMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType action,
                                                  EntityGroupId entityGroupId) {
        sendNotificationMsgToEdgeService(tenantId, null, entityId, null, null, action, entityId.getEntityType(), entityGroupId);
    }

    private void sendNotificationMsgToEdgeService(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body,
                                                  EdgeEventActionType action) {
        sendNotificationMsgToEdgeService(tenantId, edgeId, entityId, body, null, action, null, null);
    }

    private void sendNotificationMsgToEdgeService(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body,
                                                  EdgeEventType type, EdgeEventActionType action,
                                                  EntityType entityGroupType, EntityGroupId entityGroupId) {
        tbClusterService.sendNotificationMsgToEdgeService(tenantId, edgeId, entityId, body, type, action, entityGroupType, entityGroupId);
    }

    private void pushAssignedFromNotification(Tenant currentTenant, TenantId newTenantId, Device assignedDevice) {
        String data = entityToStr(assignedDevice);
        if (data != null) {
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_ASSIGNED_FROM_TENANT, assignedDevice.getId(), assignedDevice.getCustomerId(), getMetaDataForAssignedFrom(currentTenant), TbMsgDataType.JSON, data);
            tbClusterService.pushMsgToRuleEngine(newTenantId, assignedDevice.getId(), tbMsg, null);
        }
    }

    private TbMsgMetaData getMetaDataForAssignedFrom(Tenant tenant) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("assignedFromTenantId", tenant.getId().getId().toString());
        metaData.putValue("assignedFromTenantName", tenant.getName());
        return metaData;
    }

    private <E extends HasName> String entityToStr(E entity) {
        try {
            return json.writeValueAsString(json.valueToTree(entity));
        } catch (JsonProcessingException e) {
            log.warn("[{}] Failed to convert entity to string!", entity, e);
        }
        return null;
    }

    private EdgeEventActionType edgeTypeByActionType(ActionType actionType) {
        switch (actionType) {
            case ADDED:
                return EdgeEventActionType.ADDED;
            case UPDATED:
                return EdgeEventActionType.UPDATED;
            case ALARM_ACK:
                return EdgeEventActionType.ALARM_ACK;
            case ALARM_CLEAR:
                return EdgeEventActionType.ALARM_CLEAR;
            case DELETED:
                return EdgeEventActionType.DELETED;
            default:
                return null;
        }
    }

    protected void sendNotificationMsgToCloudService(TenantId tenantId, EntityRelation relation, EdgeEventActionType cloudEventAction) {
        try {
            tbClusterService.sendNotificationMsgToCloudService(tenantId, null, json.writeValueAsString(relation), CloudEventType.RELATION, cloudEventAction, null);
        } catch (Exception e) {
            log.warn("Failed to push relation to core: {}", relation, e);
        }
    }

    protected void sendNotificationMsgToCloudService(TenantId tenantId, EntityId entityId, EdgeEventActionType cloudEventAction) {
        CloudEventType cloudEventType = CloudUtils.getCloudEventTypeByEntityType(entityId.getEntityType());
        if (cloudEventType != null) {
            tbClusterService.sendNotificationMsgToCloudService(tenantId, entityId, null, cloudEventType, cloudEventAction, null);
        }
    }

    protected void sendAlarmDeleteNotificationMsgToCloudService(TenantId tenantId, EntityId entityId, Alarm alarm) {
        try {
            tbClusterService.sendNotificationMsgToCloudService(tenantId, entityId, json.writeValueAsString(alarm), CloudEventType.ALARM, EdgeEventActionType.DELETED, null);
        } catch (Exception e) {
            log.warn("Failed to push delete alarm msg to core: {}", alarm, e);
        }
    }

    protected void sendNotificationMsgToCloudService(TenantId tenantId, EntityId entityId, CloudEventType cloudEventType,
                                                     EdgeEventActionType cloudEventAction) {
        tbClusterService.sendNotificationMsgToCloudService(tenantId, entityId, null, cloudEventType, cloudEventAction, null);
    }

    protected void sendGroupEntityNotificationMsgToCloud(TenantId tenantId, EntityId entityId, CloudEventType cloudEventType,
                                                         EdgeEventActionType cloudEventAction, EntityGroupId entityGroupId) {
        tbClusterService.sendNotificationMsgToCloudService(tenantId, entityId, null, cloudEventType, cloudEventAction, entityGroupId);
    }
}
