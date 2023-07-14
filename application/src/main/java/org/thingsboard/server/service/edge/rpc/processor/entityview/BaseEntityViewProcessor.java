/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            edgeSynchronizationManager.getSync().set(true);

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
            edgeSynchronizationManager.getSync().remove();
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
