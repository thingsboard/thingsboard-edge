/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.gen.edge.v1.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class EntityGroupCloudProcessor extends BaseEdgeProcessor {

    private final Lock entityGroupCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processEntityGroupMsgFromCloud(TenantId tenantId, EntityGroupUpdateMsg entityGroupUpdateMsg) {
        EntityGroupId entityGroupId = new EntityGroupId(new UUID(entityGroupUpdateMsg.getIdMSB(), entityGroupUpdateMsg.getIdLSB()));
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        EntityGroup entityGroup = null;
        if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(entityGroupUpdateMsg.getMsgType()) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(entityGroupUpdateMsg.getMsgType())) {
            entityGroup = JacksonUtil.fromString(entityGroupUpdateMsg.getEntity(), EntityGroup.class, true);
            if (entityGroup == null) {
                throw new RuntimeException("[{" + tenantId + "}] entityGroupUpdateMsg {" + entityGroupUpdateMsg + "} cannot be converted to entity group");
            }
        }
        boolean edgeGroupAll = EdgeUtils.isEdgeGroupAll(entityGroupUpdateMsg.getName());
        if (!edgeGroupAll) {
            switch (entityGroupUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    entityGroupCreationLock.lock();
                    try {
                        edgeCtx.getEntityGroupService().saveEntityGroup(tenantId, entityGroup.getOwnerId(), entityGroup, false);
                    } finally {
                        entityGroupCreationLock.unlock();
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    ListenableFuture<EntityGroup> entityGroupByIdAsyncFuture = edgeCtx.getEntityGroupService().findEntityGroupByIdAsync(tenantId, entityGroupId);
                    ListenableFuture<Void> deleteFuture = Futures.transformAsync(entityGroupByIdAsyncFuture, entityGroupFromDb -> {
                        if (entityGroupFromDb != null) {
                            ListenableFuture<List<EntityId>> entityIdsFuture = edgeCtx.getEntityGroupService().findAllEntityIdsAsync(tenantId, entityGroupId, new TimePageLink(Integer.MAX_VALUE));
                            return Futures.transformAsync(entityIdsFuture, entityIds -> {
                                List<ListenableFuture<Void>> deleteEntitiesFutures = new ArrayList<>();
                                if (entityIds != null && !entityIds.isEmpty()) {
                                    for (EntityId entityId : entityIds) {
                                        ListenableFuture<List<EntityGroupId>> entityGroupsForEntityFuture = edgeCtx.getEntityGroupService().findEntityGroupsForEntityAsync(tenantId, entityId);
                                        deleteEntitiesFutures.add(Futures.transform(entityGroupsForEntityFuture, entityGroupIds -> {
                                            if (entityGroupIds != null && entityGroupIds.contains(entityGroupId) && entityGroupIds.size() == 2) {
                                                deleteEntityById(tenantId, entityId);
                                            }
                                            return null;
                                        }, dbCallbackExecutorService));
                                    }
                                }
                                ListenableFuture<List<Void>> allFuture = Futures.allAsList(deleteEntitiesFutures);
                                return Futures.transform(allFuture, all -> {
                                    edgeCtx.getEntityGroupService().deleteEntityGroup(tenantId, entityGroupId);
                                    return null;
                                }, dbCallbackExecutorService);
                            }, dbCallbackExecutorService);
                        } else {
                            log.trace("[{}] Entity group was not found [{}]", tenantId, entityGroupUpdateMsg);
                            return Futures.immediateFuture(null);
                        }
                    }, dbCallbackExecutorService);
                    futures.add(deleteFuture);
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(entityGroupUpdateMsg.getMsgType());
            }
        }
        if (entityGroup != null) {
            ObjectNode body = JacksonUtil.newObjectNode();
            body.put("type", entityGroup.getType().name());
            futures.add(cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ENTITY_GROUP, EdgeEventActionType.GROUP_ENTITIES_REQUEST,
                    entityGroupId, body, null));
            if (!edgeGroupAll) {
                futures.add(cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ENTITY_GROUP, EdgeEventActionType.GROUP_PERMISSIONS_REQUEST,
                        entityGroupId, body, null));
            }
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private void deleteEntityById(TenantId tenantId, EntityId entityId) {
        switch (entityId.getEntityType()) {
            case DEVICE -> {
                Device deviceToDelete = edgeCtx.getDeviceService().findDeviceById(tenantId, new DeviceId(entityId.getId()));
                if (deviceToDelete != null) {
                    edgeCtx.getDeviceService().deleteDevice(tenantId, deviceToDelete.getId());
                }
            }
            case ASSET -> {
                Asset assetToDelete = edgeCtx.getAssetService().findAssetById(tenantId, new AssetId(entityId.getId()));
                if (assetToDelete != null) {
                    edgeCtx.getAssetService().deleteAsset(tenantId, assetToDelete.getId());
                }
            }
            case ENTITY_VIEW -> {
                EntityView entityViewToDelete = edgeCtx.getEntityViewService().findEntityViewById(tenantId, new EntityViewId(entityId.getId()));
                if (entityViewToDelete != null) {
                    edgeCtx.getEntityViewService().deleteEntityView(tenantId, entityViewToDelete.getId());
                }
            }
            case USER -> {
                User userToDelete = edgeCtx.getUserService().findUserById(tenantId, new UserId(entityId.getId()));
                if (userToDelete != null) {
                    edgeCtx.getUserService().deleteUser(tenantId, userToDelete.getId());
                }
            }
            case DASHBOARD -> {
                Dashboard dashboardToDelete = edgeCtx.getDashboardService().findDashboardById(tenantId, new DashboardId(entityId.getId()));
                if (dashboardToDelete != null) {
                    edgeCtx.getDashboardService().deleteDashboard(tenantId, dashboardToDelete.getId());
                }
            }
        }
    }

    public UplinkMsg processGroupEntitiesRequestMsgToCloud(CloudEvent cloudEvent) {
        EntityId entityGroupId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getType(), cloudEvent.getEntityId());
        String type = cloudEvent.getEntityBody().get("type").asText();
        EntityGroupRequestMsg entityGroupEntitiesRequestMsg = EntityGroupRequestMsg.newBuilder()
                .setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits())
                .setType(type)
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addEntityGroupEntitiesRequestMsg(entityGroupEntitiesRequestMsg);
        return builder.build();
    }

}
