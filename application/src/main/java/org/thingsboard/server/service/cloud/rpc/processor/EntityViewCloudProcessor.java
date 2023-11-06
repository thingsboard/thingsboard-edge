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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.entityview.BaseEntityViewProcessor;

import java.util.UUID;

@Component
@Slf4j
public class EntityViewCloudProcessor extends BaseEntityViewProcessor {

    public ListenableFuture<Void> processEntityViewMsgFromCloud(TenantId tenantId,
                                                                CustomerId edgeCustomerId,
                                                                EntityViewUpdateMsg entityViewUpdateMsg,
                                                                Long queueStartTs) {
        EntityViewId entityViewId = new EntityViewId(new UUID(entityViewUpdateMsg.getIdMSB(), entityViewUpdateMsg.getIdLSB()));

        switch (entityViewUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg, edgeCustomerId, queueStartTs);
                return requestForAdditionalData(tenantId, entityViewId, queueStartTs);
            case ENTITY_DELETED_RPC_MESSAGE:
                EntityView entityViewById = entityViewService.findEntityViewById(tenantId, entityViewId);
                if (entityViewById != null) {
                    entityViewService.deleteEntityView(tenantId, entityViewId, new EdgeId(EdgeId.NULL_UUID));
                    tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityViewId, ComponentLifecycleEvent.DELETED);
                    pushEntityViewDeletedEventToRuleEngine(tenantId, entityViewById);
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
            default:
                return handleUnsupportedMsgType(entityViewUpdateMsg.getMsgType());
        }
    }

    private void saveOrUpdateEntityView(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg, CustomerId edgeCustomerId, Long queueStartTs) {
        CustomerId customerId = safeGetCustomerId(entityViewUpdateMsg.getCustomerIdMSB(), entityViewUpdateMsg.getCustomerIdLSB(), tenantId, edgeCustomerId);
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg, new EdgeId(EdgeId.NULL_UUID), customerId);
        Boolean created = resultPair.getFirst();
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityViewId,
                created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        if (created) {
            pushEntityViewCreatedEventToRuleEngine(tenantId, entityViewId);
        }
        Boolean entityViewNameUpdated = resultPair.getSecond();
        if (entityViewNameUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ENTITY_VIEW, EdgeEventActionType.UPDATED, entityViewId, null, queueStartTs);
        }
    }

    private void pushEntityViewCreatedEventToRuleEngine(TenantId tenantId, EntityViewId entityViewId) {
        EntityView entityView = entityViewService.findEntityViewById(tenantId, entityViewId);
        pushEntityViewEventToRuleEngine(tenantId, entityView, TbMsgType.ENTITY_CREATED);
    }

    private void pushEntityViewDeletedEventToRuleEngine(TenantId tenantId, EntityView entityView) {
        pushEntityViewEventToRuleEngine(tenantId, entityView, TbMsgType.ENTITY_DELETED);
    }

    private void pushEntityViewEventToRuleEngine(TenantId tenantId, EntityView entityView, TbMsgType msgType) {
        try {
            String entityViewAsString = JacksonUtil.toString(entityView);
            pushEntityEventToRuleEngine(tenantId, entityView.getId(), entityView.getCustomerId(), msgType, entityViewAsString, new TbMsgMetaData());
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push entityView action to rule engine: {}", tenantId, entityView.getId(), msgType.name(), e);
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

    public UplinkMsg convertEntityViewEventToUplink(CloudEvent cloudEvent) {
        EntityViewId entityViewId = new EntityViewId(cloudEvent.getEntityId());
        UplinkMsg msg = null;
        switch (cloudEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                EntityView entityView = entityViewService.findEntityViewById(cloudEvent.getTenantId(), entityViewId);
                if (entityView != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    EntityViewUpdateMsg entityViewUpdateMsg =
                            entityViewMsgConstructor.constructEntityViewUpdatedMsg(msgType, entityView);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addEntityViewUpdateMsg(entityViewUpdateMsg).build();
                } else {
                    log.info("Skipping event as entity view was not found [{}]", cloudEvent);
                }
                break;
            case DELETED:
                EntityViewUpdateMsg entityViewUpdateMsg =
                        entityViewMsgConstructor.constructEntityViewDeleteMsg(entityViewId);
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addEntityViewUpdateMsg(entityViewUpdateMsg).build();
                break;
        }
        return msg;
    }
}
