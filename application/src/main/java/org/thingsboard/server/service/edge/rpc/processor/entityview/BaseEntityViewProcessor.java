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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.EdgeEntityType;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Slf4j
public abstract class BaseEntityViewProcessor extends BaseEdgeProcessor {

    protected Pair<Boolean, Boolean> saveOrUpdateEntityView(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg, CustomerId customerId) throws ThingsboardException {
        boolean created = false;
        boolean entityViewNameUpdated = false;
        entityViewCreationLock.lock();
        try {
            EntityView entityView = entityViewService.findEntityViewById(tenantId, entityViewId);
            String entityViewName = entityViewUpdateMsg.getName();
            if (entityView == null) {
                created = true;
                entityView = new EntityView();
                entityView.setTenantId(tenantId);
                entityView.setCreatedTime(Uuids.unixTimestamp(entityViewId.getId()));
                EntityView entityViewByName = entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewName);
                if (entityViewByName != null) {
                    entityViewName = entityViewName + "_" + StringUtils.randomAlphanumeric(15);
                    log.warn("Entity view with name {} already exists. Renaming entity view name to {}",
                            entityViewUpdateMsg.getName(), entityViewName);
                    entityViewNameUpdated = true;
                }
            } else {
                changeOwnerIfRequired(tenantId, customerId, entityViewId);
            }
            entityView.setName(entityViewName);
            entityView.setType(entityViewUpdateMsg.getType());
            entityView.setCustomerId(customerId);
            entityView.setAdditionalInfo(entityViewUpdateMsg.hasAdditionalInfo() ?
                    JacksonUtil.toJsonNode(entityViewUpdateMsg.getAdditionalInfo()) : null);

            UUID entityIdUUID = safeGetUUID(entityViewUpdateMsg.getEntityIdMSB(), entityViewUpdateMsg.getEntityIdLSB());
            if (EdgeEntityType.DEVICE.equals(entityViewUpdateMsg.getEntityType())) {
                entityView.setEntityId(entityIdUUID != null ? new DeviceId(entityIdUUID) : null);
            } else if (EdgeEntityType.ASSET.equals(entityViewUpdateMsg.getEntityType())) {
                entityView.setEntityId(entityIdUUID != null ? new AssetId(entityIdUUID) : null);
            }

            entityViewValidator.validate(entityView, EntityView::getTenantId);
            if (created) {
                entityView.setId(entityViewId);
            }
            EntityView savedEntityView = entityViewService.saveEntityView(entityView, false);
            if (created) {
                entityGroupService.addEntityToEntityGroupAll(savedEntityView.getTenantId(), savedEntityView.getOwnerId(), savedEntityView.getId());
            }
            safeAddToEntityGroup(tenantId, entityViewUpdateMsg, entityViewId);
        } finally {
            entityViewCreationLock.unlock();
        }
        return Pair.of(created, entityViewNameUpdated);
    }

    private void safeAddToEntityGroup(TenantId tenantId, EntityViewUpdateMsg entityViewUpdateMsg, EntityViewId entityViewId) {
        if (entityViewUpdateMsg.hasEntityGroupIdMSB() && entityViewUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(entityViewUpdateMsg.getEntityGroupIdMSB(),
                    entityViewUpdateMsg.getEntityGroupIdLSB());
            safeAddEntityToGroup(tenantId, new EntityGroupId(entityGroupUUID), entityViewId);
        }
    }
}
