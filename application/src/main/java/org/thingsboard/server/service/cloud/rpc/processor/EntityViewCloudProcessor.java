// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
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
                                                                EntityViewUpdateMsg entityViewUpdateMsg) throws ThingsboardException {
        EntityViewId entityViewId = new EntityViewId(new UUID(entityViewUpdateMsg.getIdMSB(), entityViewUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            return switch (entityViewUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    boolean created = saveOrUpdateEntityViewFromCloud(tenantId, entityViewId, entityViewUpdateMsg);
                    yield created ? requestForAdditionalData(tenantId, entityViewId) : Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    if (entityViewUpdateMsg.hasEntityGroupIdMSB() && entityViewUpdateMsg.hasEntityGroupIdLSB()) {
                        UUID entityGroupUUID = safeGetUUID(entityViewUpdateMsg.getEntityGroupIdMSB(),
                                entityViewUpdateMsg.getEntityGroupIdLSB());
                        EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                        edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, entityViewId);
                        yield removeEntityIfInSingleAllGroup(tenantId, entityViewId, () -> edgeCtx.getEntityViewService().deleteEntityView(tenantId, entityViewId));
                    } else {
                        deleteEntityView(tenantId, entityViewId);
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(entityViewUpdateMsg.getMsgType());
            };
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private boolean saveOrUpdateEntityViewFromCloud(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg) throws ThingsboardException {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            pushEntityViewCreatedEventToRuleEngine(tenantId, entityViewId);
        }
        Boolean entityViewNameUpdated = resultPair.getSecond();
        if (entityViewNameUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ENTITY_VIEW, EdgeEventActionType.UPDATED, entityViewId, null, null);
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
        EntityGroupId entityGroupId = cloudEvent.getEntityGroupId() != null ? new EntityGroupId(cloudEvent.getEntityGroupId()) : null;
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED, ADDED_TO_ENTITY_GROUP -> {
                EntityView entityView = edgeCtx.getEntityViewService().findEntityViewById(cloudEvent.getTenantId(), entityViewId);
                if (entityView != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    EntityViewUpdateMsg entityViewUpdateMsg = EdgeMsgConstructorUtils.constructEntityViewUpdatedMsg(msgType, entityView, entityGroupId);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addEntityViewUpdateMsg(entityViewUpdateMsg).build();
                } else {
                    log.info("Skipping event as entity view was not found [{}]", cloudEvent);
                }
            }
            case DELETED, REMOVED_FROM_ENTITY_GROUP -> {
                EntityViewUpdateMsg entityViewUpdateMsg = EdgeMsgConstructorUtils.constructEntityViewDeleteMsg(entityViewId, entityGroupId);
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
