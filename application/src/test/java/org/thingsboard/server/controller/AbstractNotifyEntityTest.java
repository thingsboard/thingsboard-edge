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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Locale;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.thingsboard.server.service.entitiy.DefaultTbNotificationEntityService.edgeTypeByActionType;

@Slf4j
public abstract class AbstractNotifyEntityTest extends AbstractWebTest {

    @SpyBean
    protected TbClusterService tbClusterService;

    @SpyBean
    protected AuditLogService auditLogService;

    protected void testNotifyEntityAllOneTime(HasName entity, EntityId entityId, EntityId originatorId,
                                              TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                              ActionType actionType, Object... additionalInfo) {
        testSendNotificationMsgToEdgeServiceOneTime(entityId, tenantId, actionType);
        testLogEntityActionOneTime(entity, originatorId, tenantId, customerId, userId, userName, actionType, additionalInfo);
        testPushMsgToRuleEngineOneTime(originatorId, tenantId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityDeleteOneTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                      TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                      ActionType actionType, Object... additionalInfo) {
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionOneTime(entity, originatorId, tenantId, customerId, userId, userName, actionType, additionalInfo);
        testPushMsgToRuleEngineOneTime(entityId, tenantId);
        testBroadcastEntityStateChangeEventOneTime(entityId, tenantId);
        Mockito.reset(tbClusterService, auditLogService);
    }
    protected void testNotifyEntityNeverMsgToEdgeServiceOneTime(HasName entity, EntityId entityId, TenantId tenantId, ActionType actionType) {
        testSendNotificationMsgToEdgeServiceOneTime(entityId, tenantId, actionType);
        testLogEntityActionNever(entityId, entity);
        testPushMsgToRuleEngineNever(entityId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityOneTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                ActionType actionType, Object... additionalInfo) {
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionOneTime(entity, originatorId, tenantId, customerId, userId, userName, actionType, additionalInfo);
        testPushMsgToRuleEngineOneTime(originatorId, tenantId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                                               TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                                               ActionType actionType, Object... additionalInfo) {
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionOneTime(entity, originatorId, tenantId, customerId, userId, userName, actionType, additionalInfo);
        testPushMsgToRuleEngineOneTime(originatorId, tenantId);
        testBroadcastEntityStateChangeEventOneTime(entityId, tenantId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityError(HasName entity, TenantId tenantId,
                                         UserId userId, String userName, ActionType actionType, Exception exp,
                                         Object... additionalInfo) {
        CustomerId customer_NULL_UUID = (CustomerId) EntityIdFactory.getByTypeAndUuid(EntityType.CUSTOMER, ModelConstants.NULL_UUID);
        EntityId entity_NULL_UUID = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(entity.getClass().toString()
                        .substring(entity.getClass().toString().lastIndexOf(".") + 1).toUpperCase(Locale.ENGLISH)),
                ModelConstants.NULL_UUID);
        testNotificationMsgToEdgeServiceNever(entity_NULL_UUID);
        if (additionalInfo.length > 0) {
            Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId),
                    Mockito.eq(customer_NULL_UUID), Mockito.eq(userId), Mockito.eq(userName),
                    Mockito.eq(entity_NULL_UUID), Mockito.any(entity.getClass()), Mockito.eq(actionType),
                    Mockito.argThat(argument ->
                            argument.getMessage().equals(exp.getMessage())), Mockito.eq(additionalInfo));
        } else {
            Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId),
                    Mockito.eq(customer_NULL_UUID), Mockito.eq(userId), Mockito.eq(userName),
                    Mockito.eq(entity_NULL_UUID), Mockito.any(entity.getClass()), Mockito.eq(actionType),
                    Mockito.argThat(argument ->
                        argument.getMessage().equals(exp.getMessage())));
        }
        testPushMsgToRuleEngineNever(entity_NULL_UUID);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityTwoTimesError(HasName entity, TenantId tenantId,
                                         UserId userId, String userName, ActionType actionType, Exception exp,
                                         Object... additionalInfo) {
        CustomerId customer_NULL_UUID = (CustomerId) EntityIdFactory.getByTypeAndUuid(EntityType.CUSTOMER, ModelConstants.NULL_UUID);
        EntityId entity_NULL_UUID = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(entity.getClass().toString()
                        .substring(entity.getClass().toString().lastIndexOf(".") + 1).toUpperCase(Locale.ENGLISH)),
                ModelConstants.NULL_UUID);
        testNotificationMsgToEdgeServiceNever(entity_NULL_UUID);
        if (additionalInfo.length > 0) {
            Mockito.verify(auditLogService, times(2)).logEntityAction(Mockito.eq(tenantId),
                    Mockito.eq(customer_NULL_UUID), Mockito.eq(userId), Mockito.eq(userName),
                    Mockito.eq(entity_NULL_UUID), Mockito.any(entity.getClass()), Mockito.eq(actionType),
                    Mockito.argThat(argument ->
                            argument.getMessage().equals(exp.getMessage())), Mockito.eq(additionalInfo));
        } else {
            Mockito.verify(auditLogService, times(2)).logEntityAction(Mockito.eq(tenantId),
                    Mockito.eq(customer_NULL_UUID), Mockito.eq(userId), Mockito.eq(userName),
                    Mockito.eq(entity_NULL_UUID), Mockito.any(entity.getClass()), Mockito.eq(actionType),
                    Mockito.argThat(argument ->
                        argument.getMessage().equals(exp.getMessage())));
        }
        testPushMsgToRuleEngineNever(entity_NULL_UUID);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityNever(EntityId entityId, HasName entity) {
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionNever(entityId, entity);
        testPushMsgToRuleEngineNever(entityId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    private void testNotificationMsgToEdgeServiceNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdge(Mockito.any(),
                Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());
    }

    private void testLogEntityActionNever(EntityId entityId, HasName entity) {
        Mockito.verify(auditLogService, never()).logEntityAction(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(entity.getClass()),
                Mockito.any(), Mockito.any());
    }

    private void testPushMsgToRuleEngineNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).pushMsgToRuleEngine(Mockito.any(),
                Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any());
    }

    private void testLogEntityActionOneTime(HasName entity, EntityId originatorId, TenantId tenantId, CustomerId customerId,
                                            UserId userId, String userName, ActionType actionType, Object... additionalInfo) {
        if (additionalInfo.length == 0) {
            Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                    Mockito.eq(userId), Mockito.eq(userName), Mockito.eq(originatorId),
                    Mockito.eq(entity), Mockito.eq(actionType), Mockito.isNull());
        } else {
            String additionalInfoStr = extractParameter(String.class, 0, additionalInfo);
            Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                    Mockito.eq(userId), Mockito.eq(userName), Mockito.eq(originatorId),
                    Mockito.eq(entity), Mockito.eq(actionType), Mockito.isNull(), Mockito.eq(additionalInfoStr));
        }
    }

    private void testPushMsgToRuleEngineOneTime(EntityId originatorId, TenantId tenantId) {
        Mockito.verify(tbClusterService, times(1)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                Mockito.eq(originatorId), Mockito.any(TbMsg.class), Mockito.isNull());
    }

    private void testSendNotificationMsgToEdgeServiceOneTime(EntityId entityId, TenantId tenantId, ActionType actionType) {
        Mockito.verify(tbClusterService, times(1)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.eq(entityId), Mockito.isNull(), Mockito.isNull(),
                Mockito.eq(edgeTypeByActionType(actionType)), Mockito.isNull(), Mockito.isNull());
    }

    private void testBroadcastEntityStateChangeEventOneTime(EntityId entityId, TenantId tenantId) {
        Mockito.verify(tbClusterService, times(1)).broadcastEntityStateChangeEvent(Mockito.eq(tenantId),
                Mockito.any(entityId.getClass()), Mockito.any(ComponentLifecycleEvent.class));
    }

    private <T> T extractParameter(Class<T> clazz, int index, Object... additionalInfo) {
        T result = null;
        if (additionalInfo != null && additionalInfo.length > index) {
            Object paramObject = additionalInfo[index];
            if (clazz.isInstance(paramObject)) {
                result = clazz.cast(paramObject);
            }
        }
        return result;
    }
}
