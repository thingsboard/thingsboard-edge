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
package org.thingsboard.server.service.edge.rpc.processor.asset;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class AssetEdgeProcessor extends BaseAssetProcessor implements AssetProcessor {

    @Override
    public ListenableFuture<Void> processAssetMsgFromEdge(TenantId tenantId, Edge edge, AssetUpdateMsg assetUpdateMsg) {
        log.trace("[{}] executing processAssetMsgFromEdge [{}] from edge [{}]", tenantId, assetUpdateMsg, edge.getId());
        AssetId assetId = new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (assetUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    if (assetUpdateMsg.hasEntityGroupIdMSB() && assetUpdateMsg.hasEntityGroupIdLSB()) {
                        EntityGroupId entityGroupId = new EntityGroupId(
                                new UUID(assetUpdateMsg.getEntityGroupIdMSB(), assetUpdateMsg.getEntityGroupIdLSB()));
                        edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, assetId);
                    } else {
                        removeAssetFromEdgeAllAssetGroup(tenantId, edge, assetId);
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(assetUpdateMsg.getMsgType());
            };
        } catch (DataValidationException | ThingsboardException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed asset violated {}", tenantId, assetUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    private void saveOrUpdateAsset(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg, Edge edge) throws ThingsboardException {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), assetId);
            pushAssetCreatedEventToRuleEngine(tenantId, edge, assetId);
        }
        addAssetToEdgeAllAssetGroup(tenantId, edge, assetId);
        Boolean assetNameUpdated = resultPair.getSecond();
        if (assetNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET, EdgeEventActionType.UPDATED, assetId, null);
        }
    }

    private void pushAssetCreatedEventToRuleEngine(TenantId tenantId, Edge edge, AssetId assetId) {
        try {
            Asset asset = edgeCtx.getAssetService().findAssetById(tenantId, assetId);
            String assetAsString = JacksonUtil.toString(asset);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, asset.getCustomerId());
            pushEntityEventToRuleEngine(tenantId, assetId, asset.getCustomerId(), TbMsgType.ENTITY_CREATED, assetAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push asset action to rule engine: {}", tenantId, assetId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    private void removeAssetFromEdgeAllAssetGroup(TenantId tenantId, Edge edge, AssetId assetId) {
        Asset assetToDelete = edgeCtx.getAssetService().findAssetById(tenantId, assetId);
        if (assetToDelete != null) {
            try {
                EntityGroup edgeAssetGroup = edgeCtx.getEntityGroupService().findOrCreateEdgeAllGroupAsync(tenantId, edge, edge.getName(), assetToDelete.getOwnerId().getEntityType(), EntityType.ASSET).get();
                if (edgeAssetGroup != null) {
                    edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, edgeAssetGroup.getId(), assetToDelete.getId());
                }
            } catch (Exception e) {
                log.warn("[{}] Can't delete asset from edge asset 'All' group, asset id [{}]", tenantId, assetId, e);
                throw new RuntimeException(e);
            }
        }
    }

    private void addAssetToEdgeAllAssetGroup(TenantId tenantId, Edge edge, AssetId assetId) {
        try {
            Asset asset = edgeCtx.getAssetService().findAssetById(tenantId, assetId);
            EntityGroup edgeAssetGroup = edgeCtx.getEntityGroupService().findOrCreateEdgeAllGroupAsync(tenantId, edge, edge.getName(), asset.getOwnerId().getEntityType(), EntityType.ASSET).get();
            if (edgeAssetGroup != null) {
                edgeCtx.getEntityGroupService().addEntityToEntityGroup(tenantId, edgeAssetGroup.getId(), assetId);
            }
        } catch (Exception e) {
            log.warn("Can't add asset to edge asset group, asset id [{}]", assetId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent) {
        AssetId assetId = new AssetId(edgeEvent.getEntityId());
        EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
        switch (edgeEvent.getAction()) {
            case ADDED, ADDED_TO_ENTITY_GROUP, UPDATED, ASSIGNED_TO_EDGE -> {
                Asset asset = edgeCtx.getAssetService().findAssetById(edgeEvent.getTenantId(), assetId);
                if (asset != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    AssetUpdateMsg assetUpdateMsg = EdgeMsgConstructorUtils.constructAssetUpdatedMsg(msgType, asset, entityGroupId);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAssetUpdateMsg(assetUpdateMsg);
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        AssetProfile assetProfile = edgeCtx.getAssetProfileService().findAssetProfileById(edgeEvent.getTenantId(), asset.getAssetProfileId());
                        builder.addAssetProfileUpdateMsg(EdgeMsgConstructorUtils.constructAssetProfileUpdatedMsg(msgType, assetProfile));
                    }
                    return builder.build();
                }
            }
            case DELETED, REMOVED_FROM_ENTITY_GROUP, UNASSIGNED_FROM_EDGE, CHANGE_OWNER -> {
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetUpdateMsg(EdgeMsgConstructorUtils.constructAssetDeleteMsg(assetId, entityGroupId))
                        .build();
            }
        }
        return null;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, Asset asset, AssetUpdateMsg assetUpdateMsg) {
        CustomerId customerUUID = asset.getCustomerId() != null ? asset.getCustomerId() : customerId;
        asset.setCustomerId(customerUUID);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ASSET;
    }

}
