/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.asset;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.asset.AssetMsgConstructor;

import java.util.UUID;

@Slf4j
public abstract class AssetEdgeProcessor extends BaseAssetProcessor implements AssetProcessor {

    @Override
    public ListenableFuture<Void> processAssetMsgFromEdge(TenantId tenantId, Edge edge, AssetUpdateMsg assetUpdateMsg) {
        log.trace("[{}] executing processAssetMsgFromEdge [{}] from edge [{}]", tenantId, assetUpdateMsg, edge.getId());
        AssetId assetId = new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (assetUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    Asset assetToDelete = assetService.findAssetById(tenantId, assetId);
                    if (assetToDelete != null) {
                        assetService.unassignAssetFromEdge(tenantId, assetId, edge.getId());
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(assetUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
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

    private void saveOrUpdateAsset(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), assetId);
            pushAssetCreatedEventToRuleEngine(tenantId, edge, assetId);
            assetService.assignAssetToEdge(tenantId, assetId, edge.getId());
        }
        Boolean assetNameUpdated = resultPair.getSecond();
        if (assetNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ASSET, EdgeEventActionType.UPDATED, assetId, null);
        }
    }

    private void pushAssetCreatedEventToRuleEngine(TenantId tenantId, Edge edge, AssetId assetId) {
        try {
            Asset asset = assetService.findAssetById(tenantId, assetId);
            String assetAsString = JacksonUtil.toString(asset);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, asset.getCustomerId());
            pushEntityEventToRuleEngine(tenantId, assetId, asset.getCustomerId(), TbMsgType.ENTITY_CREATED, assetAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push asset action to rule engine: {}", tenantId, assetId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    @Override
    public DownlinkMsg convertAssetEventToDownlink(EdgeEvent edgeEvent, EdgeId edgeId, EdgeVersion edgeVersion) {
        AssetId assetId = new AssetId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED, ASSIGNED_TO_EDGE, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER -> {
                Asset asset = assetService.findAssetById(edgeEvent.getTenantId(), assetId);
                if (asset != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    AssetUpdateMsg assetUpdateMsg = ((AssetMsgConstructor)
                            assetMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructAssetUpdatedMsg(msgType, asset);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAssetUpdateMsg(assetUpdateMsg);
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        AssetProfile assetProfile = assetProfileService.findAssetProfileById(edgeEvent.getTenantId(), asset.getAssetProfileId());
                        assetProfile = checkIfAssetProfileDefaultFieldsAssignedToEdge(edgeEvent.getTenantId(), edgeId, assetProfile, edgeVersion);
                        builder.addAssetProfileUpdateMsg(((AssetMsgConstructor) assetMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                                .constructAssetProfileUpdatedMsg(msgType, assetProfile));
                    }
                    downlinkMsg = builder.build();
                }
            }
            case DELETED, UNASSIGNED_FROM_EDGE -> {
                AssetUpdateMsg assetUpdateMsg = ((AssetMsgConstructor)
                        assetMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructAssetDeleteMsg(assetId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetUpdateMsg(assetUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

}
