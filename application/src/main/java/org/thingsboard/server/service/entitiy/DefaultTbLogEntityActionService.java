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
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.service.action.EntityActionService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTbLogEntityActionService implements TbLogEntityActionService {

    private final EntityActionService entityActionService;

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
    public void logEntityRelationAction(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user,
                                        ActionType actionType, Exception e, Object... additionalInfo) {
        logEntityAction(tenantId, relation.getFrom(), null, customerId, actionType, user, e, additionalInfo);
        logEntityAction(tenantId, relation.getTo(), null, customerId, actionType, user, e, additionalInfo);
    }
}
