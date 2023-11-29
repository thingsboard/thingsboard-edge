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
package org.thingsboard.server.service.edge.rpc.processor.entityview;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Slf4j
public abstract class BaseEntityViewProcessor extends BaseEdgeProcessor {

    protected Pair<Boolean, Boolean> saveOrUpdateEntityView(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg) throws ThingsboardException {
        boolean created = false;
        boolean entityViewNameUpdated = false;
        EntityView entityView = constructEntityViewFromUpdateMsg(tenantId, entityViewId, entityViewUpdateMsg);
        if (entityView == null) {
            throw new RuntimeException("[{" + tenantId + "}] entityViewUpdateMsg {" + entityViewUpdateMsg + "} cannot be converted to entity view");
        }
        EntityView entityViewById = entityViewService.findEntityViewById(tenantId, entityViewId);
        if (entityViewById == null) {
            created = true;
            entityView.setId(null);
        } else {
            entityView.setId(entityViewId);
            changeOwnerIfRequired(tenantId, entityViewById.getCustomerId(), entityViewId);
        }
        String entityViewName = entityView.getName();
        EntityView entityViewByName = entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewName);
        if (entityViewByName != null && !entityViewByName.getId().equals(entityViewId)) {
            entityViewName = entityViewName + "_" + StringUtils.randomAlphanumeric(15);
            log.warn("[{}] Entity view with name {} already exists. Renaming entity view name to {}",
                    tenantId, entityView.getName(), entityViewName);
            entityViewNameUpdated = true;
        }
        entityView.setName(entityViewName);
        setCustomerId(tenantId, created ? null : entityViewById.getCustomerId(), entityView, entityViewUpdateMsg);

        entityViewValidator.validate(entityView, EntityView::getTenantId);
        if (created) {
            entityView.setId(entityViewId);
        }
        EntityView savedEntityView = entityViewService.saveEntityView(entityView, false);
        if (created) {
            entityGroupService.addEntityToEntityGroupAll(savedEntityView.getTenantId(), savedEntityView.getOwnerId(), savedEntityView.getId());
        }
        safeAddToEntityGroup(tenantId, entityViewUpdateMsg, entityViewId);
        return Pair.of(created, entityViewNameUpdated);
    }

    private void safeAddToEntityGroup(TenantId tenantId, EntityViewUpdateMsg entityViewUpdateMsg, EntityViewId entityViewId) {
        if (entityViewUpdateMsg.hasEntityGroupIdMSB() && entityViewUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(entityViewUpdateMsg.getEntityGroupIdMSB(),
                    entityViewUpdateMsg.getEntityGroupIdLSB());
            safeAddEntityToGroup(tenantId, new EntityGroupId(entityGroupUUID), entityViewId);
        }
    }

    protected abstract EntityView constructEntityViewFromUpdateMsg(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg);

    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, EntityView entityView, EntityViewUpdateMsg entityViewUpdateMsg);
}
