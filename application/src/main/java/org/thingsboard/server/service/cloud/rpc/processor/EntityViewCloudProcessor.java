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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class EntityViewCloudProcessor extends BaseEdgeProcessor {

    private final Lock entityViewCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processEntityViewMsgFromCloud(TenantId tenantId,
                                                                EntityViewUpdateMsg entityViewUpdateMsg,
                                                                Long queueStartTs) throws ThingsboardException {
        EntityViewId entityViewId = new EntityViewId(new UUID(entityViewUpdateMsg.getIdMSB(), entityViewUpdateMsg.getIdLSB()));
        CustomerId customerId = safeGetCustomerId(entityViewUpdateMsg.getCustomerIdMSB(), entityViewUpdateMsg.getCustomerIdLSB());
        switch (entityViewUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                entityViewCreationLock.lock();
                try {
                    EntityView entityView = entityViewService.findEntityViewById(tenantId, entityViewId);
                    boolean created = false;
                    if (entityView == null) {
                        created = true;
                        entityView = new EntityView();
                        entityView.setTenantId(tenantId);
                        entityView.setId(entityViewId);
                        entityView.setCreatedTime(Uuids.unixTimestamp(entityViewId.getId()));
                    } else {
                        changeOwnerIfRequired(tenantId, customerId, entityViewId);
                    }
                    EntityId entityId = null;
                    switch (entityViewUpdateMsg.getEntityType()) {
                        case DEVICE:
                            entityId = new DeviceId(new UUID(entityViewUpdateMsg.getEntityIdMSB(), entityViewUpdateMsg.getEntityIdLSB()));
                            break;
                        case ASSET:
                            entityId = new AssetId(new UUID(entityViewUpdateMsg.getEntityIdMSB(), entityViewUpdateMsg.getEntityIdLSB()));
                            break;
                    }
                    entityView.setName(entityViewUpdateMsg.getName());
                    entityView.setType(entityViewUpdateMsg.getType());
                    entityView.setEntityId(entityId);
                    entityView.setAdditionalInfo(entityViewUpdateMsg.hasAdditionalInfo()
                            ? JacksonUtil.toJsonNode(entityViewUpdateMsg.getAdditionalInfo()) : null);
                    entityView.setCustomerId(customerId);
                    EntityView savedEntityView = entityViewService.saveEntityView(entityView, false);

                    if (created) {
                        entityGroupService.addEntityToEntityGroupAll(savedEntityView.getTenantId(), savedEntityView.getOwnerId(), savedEntityView.getId());
                    }

                    safeAddToEntityGroup(tenantId, entityViewUpdateMsg, entityViewId);
                    tbClusterService.broadcastEntityStateChangeEvent(savedEntityView.getTenantId(), savedEntityView.getId(),
                            created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
                } finally {
                    entityViewCreationLock.unlock();
                }
                return requestForAdditionalData(tenantId, entityViewId, queueStartTs);
            case ENTITY_DELETED_RPC_MESSAGE:
                if (entityViewUpdateMsg.hasEntityGroupIdMSB() && entityViewUpdateMsg.hasEntityGroupIdLSB()) {
                    UUID entityGroupUUID = safeGetUUID(entityViewUpdateMsg.getEntityGroupIdMSB(),
                            entityViewUpdateMsg.getEntityGroupIdLSB());
                    EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                    entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, entityViewId);
                } else {
                    EntityView entityViewById = entityViewService.findEntityViewById(tenantId, entityViewId);
                    if (entityViewById != null) {
                        entityViewService.deleteEntityView(tenantId, entityViewId);
                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityViewId, ComponentLifecycleEvent.DELETED);
                    }
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
            default:
                return handleUnsupportedMsgType(entityViewUpdateMsg.getMsgType());
        }
    }

    private void safeAddToEntityGroup(TenantId tenantId, EntityViewUpdateMsg entityViewUpdateMsg, EntityViewId entityViewId) {
        if (entityViewUpdateMsg.hasEntityGroupIdMSB() && entityViewUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(entityViewUpdateMsg.getEntityGroupIdMSB(),
                    entityViewUpdateMsg.getEntityGroupIdLSB());
            EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
            safeAddEntityToGroup(tenantId, entityGroupId, entityViewId);
        }
    }

    public UplinkMsg convertEntityViewRequestEventToUplink(CloudEvent cloudEvent) {
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getType(), cloudEvent.getEntityId());
        EntityViewsRequestMsg entityViewsRequestMsg = EntityViewsRequestMsg.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name())
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addEntityViewsRequestMsg(entityViewsRequestMsg);
        return builder.build();
    }
}
