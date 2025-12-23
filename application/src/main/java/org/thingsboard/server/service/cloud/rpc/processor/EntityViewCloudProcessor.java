/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.entityview.BaseEntityViewProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class EntityViewCloudProcessor extends BaseEntityViewProcessor {

    public ListenableFuture<Void> processEntityViewMsgFromCloud(TenantId tenantId,
                                                                EntityViewUpdateMsg entityViewUpdateMsg) {
        EntityViewId entityViewId = new EntityViewId(new UUID(entityViewUpdateMsg.getIdMSB(), entityViewUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            return switch (entityViewUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    boolean created = saveOrUpdateEntityViewFromCloud(tenantId, entityViewId, entityViewUpdateMsg);
                    yield created ? requestForAdditionalData(tenantId, entityViewId) : Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    deleteEntityView(tenantId, entityViewId);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(entityViewUpdateMsg.getMsgType());
            };
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private boolean saveOrUpdateEntityViewFromCloud(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            pushEntityViewCreatedEventToRuleEngine(tenantId, entityViewId);
        }
        Boolean entityViewNameUpdated = resultPair.getSecond();
        if (entityViewNameUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ENTITY_VIEW, EdgeEventActionType.UPDATED, entityViewId, null);
        }
        return created;
    }

    private void pushEntityViewCreatedEventToRuleEngine(TenantId tenantId, EntityViewId entityViewId) {
        EntityView entityView = edgeCtx.getEntityViewService().findEntityViewById(tenantId, entityViewId);
        pushEntityEventToRuleEngine(tenantId, entityView, TbMsgType.ENTITY_CREATED);
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        EntityViewId entityViewId = new EntityViewId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER -> {
                EntityView entityView = edgeCtx.getEntityViewService().findEntityViewById(cloudEvent.getTenantId(), entityViewId);
                if (entityView != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    EntityViewUpdateMsg entityViewUpdateMsg = EdgeMsgConstructorUtils.constructEntityViewUpdatedMsg(msgType, entityView);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addEntityViewUpdateMsg(entityViewUpdateMsg).build();
                } else {
                    log.info("Skipping event as entity view was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                EntityViewUpdateMsg entityViewUpdateMsg = EdgeMsgConstructorUtils.constructEntityViewDeleteMsg(entityViewId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addEntityViewUpdateMsg(entityViewUpdateMsg).build();
            }
        }
        return null;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, EntityView entityView, EntityViewUpdateMsg entityViewUpdateMsg) {
        if (isCustomerNotExists(tenantId, entityView.getCustomerId())) {
            entityView.setCustomerId(null);
        }
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.ENTITY_VIEW;
    }

}
