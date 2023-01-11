/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class EntityViewCloudProcessor extends BaseCloudProcessor {

    private final Lock entityViewCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processEntityViewMsgFromCloud(TenantId tenantId,
                                                                CustomerId edgeCustomerId,
                                                                EntityViewUpdateMsg entityViewUpdateMsg,
                                                                Long queueStartTs) {
        EntityViewId entityViewId = new EntityViewId(new UUID(entityViewUpdateMsg.getIdMSB(), entityViewUpdateMsg.getIdLSB()));
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
                    entityView.setCustomerId(safeGetCustomerId(tenantId, entityViewUpdateMsg.getCustomerIdMSB(), entityViewUpdateMsg.getCustomerIdLSB(), edgeCustomerId));
                    EntityView savedEntityView = entityViewService.saveEntityView(entityView, false);

                    tbClusterService.broadcastEntityStateChangeEvent(savedEntityView.getTenantId(), savedEntityView.getId(),
                            created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
                } finally {
                    entityViewCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                EntityView entityViewById = entityViewService.findEntityViewById(tenantId, entityViewId);
                if (entityViewById != null) {
                    entityViewService.deleteEntityView(tenantId, entityViewId);
                    tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityViewId, ComponentLifecycleEvent.DELETED);
                }
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(entityViewUpdateMsg.getMsgType());
        }
        return Futures.transform(requestForAdditionalData(tenantId, entityViewUpdateMsg.getMsgType(), entityViewId, queueStartTs), future -> null, dbCallbackExecutor);
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
