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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Component
@Slf4j
public class AssetCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processAssetMsgFromCloud(TenantId tenantId,
                                                           CustomerId edgeCustomerId,
                                                           AssetUpdateMsg assetUpdateMsg,
                                                           Long queueStartTs) {
        AssetId assetId = new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
        switch (assetUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg, edgeCustomerId);
                return requestForAdditionalData(tenantId, assetId, queueStartTs);
            case ENTITY_DELETED_RPC_MESSAGE:
                Asset assetById = assetService.findAssetById(tenantId, assetId);
                if (assetById != null) {
                    assetService.deleteAsset(tenantId, assetId);
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
            default:
                return handleUnsupportedMsgType(assetUpdateMsg.getMsgType());
        }
    }

    private void saveOrUpdateAsset(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg, CustomerId edgeCustomerId) {
        CustomerId customerId = safeGetCustomerId(assetUpdateMsg.getCustomerIdMSB(), assetUpdateMsg.getCustomerIdLSB(), tenantId, edgeCustomerId);
        assetCreationLock.lock();
        try {
            Asset asset = assetService.findAssetById(tenantId, assetId);
            boolean created = false;
            if (asset == null) {
                asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setCreatedTime(Uuids.unixTimestamp(assetId.getId()));
                created = true;
            }
            asset.setName(assetUpdateMsg.getName());
            asset.setType(assetUpdateMsg.getType());
            asset.setLabel(assetUpdateMsg.hasLabel() ? assetUpdateMsg.getLabel() : null);
            asset.setAdditionalInfo(assetUpdateMsg.hasAdditionalInfo() ? JacksonUtil.toJsonNode(assetUpdateMsg.getAdditionalInfo()) : null);
            asset.setCustomerId(customerId);
            UUID assetProfileUUID = safeGetUUID(assetUpdateMsg.getAssetProfileIdMSB(), assetUpdateMsg.getAssetProfileIdLSB());
            asset.setAssetProfileId(assetProfileUUID != null ? new AssetProfileId(assetProfileUUID) : null);
            assetValidator.validate(asset, Asset::getTenantId);
            if (created) {
                asset.setId(assetId);
            }
            assetService.saveAsset(asset, false);
        } finally {
            assetCreationLock.unlock();
        }
    }
}
