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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.constructor.entityview.EntityViewMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.BaseEntityViewProcessor;

import java.util.UUID;

@Component
@Slf4j
public class EntityViewCloudProcessor extends BaseEntityViewProcessor {

    public ListenableFuture<Void> processEntityViewMsgFromCloud(TenantId tenantId,
                                                                EntityViewUpdateMsg entityViewUpdateMsg,
                                                                EdgeVersion edgeVersion,
                                                                Long queueStartTs) throws ThingsboardException {
        EntityViewId entityViewId = new EntityViewId(new UUID(entityViewUpdateMsg.getIdMSB(), entityViewUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (entityViewUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg, edgeVersion, queueStartTs);
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
                        pushEntityViewDeletedEventToRuleEngine(tenantId, entityViewById);
                    }
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
            default:
                return handleUnsupportedMsgType(entityViewUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void saveOrUpdateEntityView(TenantId tenantId, EntityViewId entityViewId, EntityViewUpdateMsg entityViewUpdateMsg, EdgeVersion edgeVersion, Long queueStartTs) throws ThingsboardException {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateEntityView(tenantId, entityViewId, entityViewUpdateMsg, edgeVersion);
        Boolean created = resultPair.getFirst();
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityViewId,
                created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        if (created) {
            pushEntityViewCreatedEventToRuleEngine(tenantId, entityViewId);
        }
        Boolean entityViewNameUpdated = resultPair.getSecond();
        if (entityViewNameUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ENTITY_VIEW, EdgeEventActionType.UPDATED, entityViewId, null, null, queueStartTs);
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

    public UplinkMsg convertEntityViewEventToUplink(CloudEvent cloudEvent, EdgeVersion edgeVersion) {
        EntityViewId entityViewId = new EntityViewId(cloudEvent.getEntityId());
        UplinkMsg msg = null;
        EntityGroupId entityGroupId = cloudEvent.getEntityGroupId() != null ? new EntityGroupId(cloudEvent.getEntityGroupId()) : null;
        switch (cloudEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ADDED_TO_ENTITY_GROUP:
                EntityView entityView = entityViewService.findEntityViewById(cloudEvent.getTenantId(), entityViewId);
                if (entityView != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    EntityViewUpdateMsg entityViewUpdateMsg = ((EntityViewMsgConstructor)
                            entityViewMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructEntityViewUpdatedMsg(msgType, entityView, entityGroupId);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addEntityViewUpdateMsg(entityViewUpdateMsg).build();
                } else {
                    log.info("Skipping event as entity view was not found [{}]", cloudEvent);
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
                EntityViewUpdateMsg entityViewUpdateMsg = ((EntityViewMsgConstructor)
                        entityViewMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructEntityViewDeleteMsg(entityViewId, entityGroupId);
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addEntityViewUpdateMsg(entityViewUpdateMsg).build();
                break;
        }
        return msg;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, EntityView entityView, EntityViewUpdateMsg entityViewUpdateMsg, EdgeVersion edgeVersion) {
        if (isCustomerNotExists(tenantId, entityView.getCustomerId())) {
            entityView.setCustomerId(null);
        }
    }
}
