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

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.audit.ActionType;
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
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.util.List;

public interface TbNotificationEntityService {

    <I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, ActionType actionType, User user,
                                              Exception e, Object... additionalInfo);

    <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, ActionType actionType,
                                                                 User user, Object... additionalInfo);

    <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, ActionType actionType,
                                                                 User user, Exception e, Object... additionalInfo);

    <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                                 ActionType actionType, User user, Object... additionalInfo);

    <E extends HasName, I extends EntityId> void logEntityAction(TenantId tenantId, I entityId, E entity, CustomerId customerId,
                                                                 ActionType actionType, User user, Exception e,
                                                                 Object... additionalInfo);

    <E extends HasName, I extends EntityId> void notifyAddToEntityGroup(TenantId tenantId, I entityId, E entity,
                                                                        CustomerId customerId, EntityGroupId entityGroupId,
                                                                        User user, Object... additionalInfo);

    <E extends HasName, I extends EntityId> void notifyDeleteEntity(TenantId tenantId, I entityId, E entity,
                                                                    CustomerId customerId, ActionType actionType,
                                                                    List<EdgeId> relatedEdgeIds,
                                                                    User user, Object... additionalInfo);

    void notifyDeleteAlarm(TenantId tenantId, Alarm alarm, EntityId originatorId, CustomerId customerId,
                           List<EdgeId> relatedEdgeIds, User user, String body, Object... additionalInfo);

    <E extends HasName, I extends EntityId> void notifyCreateOrUpdateEntity(TenantId tenantId, I entityId, E entity,
                                                                            CustomerId customerId, ActionType actionType,
                                                                            User user, Object... additionalInfo);

    void notifyDeleteRuleChain(TenantId tenantId, RuleChain ruleChain,
                               List<EdgeId> relatedEdgeIds, User user);

    <I extends EntityId> void notifySendMsgToEdgeService(TenantId tenantId, I entityId, EdgeEventType edgeEventType,
                                                         EdgeEventActionType edgeEventActionType);

    <I extends EntityId> void notifySendMsgToEdgeService(TenantId tenantId, I entityId, EdgeEventActionType edgeEventActionType);

    <E extends HasName, I extends EntityId> void notifyAssignOrUnassignEntityToEdge(TenantId tenantId, I entityId,
                                                                                    CustomerId customerId, EdgeId edgeId,
                                                                                    E entity, ActionType actionType,
                                                                                    User user, Object... additionalInfo);

    void notifyCreateOrUpdateTenant(Tenant tenant, ComponentLifecycleEvent event);

    void notifyDeleteTenant(Tenant tenant);

    void notifyDeleteDevice(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                            List<EdgeId> relatedEdgeIds, User user, Object... additionalInfo);

    void notifyUpdateDeviceCredentials(TenantId tenantId, DeviceId deviceId, CustomerId customerId, Device device,
                                       DeviceCredentials deviceCredentials, User user);

    void notifyAssignDeviceToTenant(TenantId tenantId, TenantId newTenantId, DeviceId deviceId, CustomerId customerId,
                                    Device device, Tenant tenant, User user, Object... additionalInfo);

    void notifyCreateOrUpdateOrDeleteEdge(TenantId tenantId, EdgeId edgeId, CustomerId customerId, Edge edge, ActionType actionType,
                                          User user, Object... additionalInfo);

    void notifyCreateOrUpdateAlarm(Alarm alarm, ActionType actionType, User user, Object... additionalInfo);

    void notifyAlarmComment(Alarm alarm, AlarmComment alarmComment, ActionType actionType, User user);


    <E extends HasName, I extends EntityId> void notifyCreateOrUpdateOrDelete(TenantId tenantId, CustomerId customerId,
                                                                              I entityId, E entity, User user,
                                                                              ActionType actionType, boolean sendNotifyMsgToEdge,
                                                                              Exception e, Object... additionalInfo);

    void notifyRelation(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user,
                        ActionType actionType, Object... additionalInfo);
}
