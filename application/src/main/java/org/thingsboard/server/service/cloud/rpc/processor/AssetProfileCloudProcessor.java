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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.asset.BaseAssetProfileProcessor;

import java.util.UUID;

@Component
@Slf4j
public class AssetProfileCloudProcessor extends BaseAssetProfileProcessor {

    @Autowired
    private AssetProfileService assetProfileService;

    @Autowired
    private AssetService assetService;

    public ListenableFuture<Void> processAssetProfileMsgFromCloud(TenantId tenantId, AssetProfileUpdateMsg assetProfileUpdateMsg, Long queueStartTs) {
        AssetProfileId assetProfileId = new AssetProfileId(new UUID(assetProfileUpdateMsg.getIdMSB(), assetProfileUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getSync().set(true);

            switch (assetProfileUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    assetCreationLock.lock();
                    try {
                        AssetProfile assetProfileByName = assetProfileService.findAssetProfileByName(tenantId, assetProfileUpdateMsg.getName());
                        boolean removePreviousProfile = false;
                        if (assetProfileByName != null && !assetProfileByName.getId().equals(assetProfileId)) {
                            renamePreviousAssetProfile(assetProfileByName);
                            removePreviousProfile = true;
                        }
                        saveOrUpdateAssetProfile(tenantId, assetProfileId, assetProfileUpdateMsg, queueStartTs);
                        AssetProfile assetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
                        if (!assetProfile.isDefault() && assetProfileUpdateMsg.getDefault()) {
                            assetProfileService.setDefaultAssetProfile(tenantId, assetProfileId);
                        }
                        if (removePreviousProfile) {
                            updateAssets(tenantId, assetProfileId, assetProfileByName.getId());
                            assetProfileService.deleteAssetProfile(tenantId, assetProfileByName.getId());
                            tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetProfileByName.getId(), ComponentLifecycleEvent.DELETED);
                        }
                    } finally {
                        assetCreationLock.unlock();
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    AssetProfile assetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
                    if (assetProfile != null) {
                        assetProfileService.deleteAssetProfile(tenantId, assetProfileId);
                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetProfileId, ComponentLifecycleEvent.DELETED);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(assetProfileUpdateMsg.getMsgType());
            }
        } finally {
            edgeSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    private void saveOrUpdateAssetProfile(TenantId tenantId, AssetProfileId assetProfileId, AssetProfileUpdateMsg assetProfileUpdateMsg, long queueStartTs) {
        boolean created = super.saveOrUpdateAssetProfile(tenantId, assetProfileId, assetProfileUpdateMsg);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetProfileId,
                created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
    }

    private void renamePreviousAssetProfile(AssetProfile assetProfileByName) {
        assetProfileByName.setName(assetProfileByName.getName() + StringUtils.randomAlphanumeric(15));
        assetProfileService.saveAssetProfile(assetProfileByName);
    }

    private void updateAssets(TenantId tenantId, AssetProfileId newAssetProfileId, AssetProfileId previousAssetProfileId) {
        PageDataIterable<AssetInfo> assetInfosIterable = new PageDataIterable<>(
                link -> assetService.findAssetInfosByTenantIdAndAssetProfileId(tenantId, previousAssetProfileId, link), 1024);
        assetInfosIterable.forEach(assetInfo -> {
            assetInfo.setAssetProfileId(newAssetProfileId);
            assetService.saveAsset(new Asset(assetInfo));
        });
    }

    public UplinkMsg convertAssetProfileEventToUplink(CloudEvent cloudEvent) {
        AssetProfileId assetProfileId = new AssetProfileId(cloudEvent.getEntityId());
        UplinkMsg msg = null;
        switch (cloudEvent.getAction()) {
            case ADDED:
            case UPDATED:
                AssetProfile assetProfile = assetProfileService.findAssetProfileById(cloudEvent.getTenantId(), assetProfileId);
                if (assetProfile != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    AssetProfileUpdateMsg assetProfileUpdateMsg =
                            assetProfileMsgConstructor.constructAssetProfileUpdatedMsg(msgType, assetProfile);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAssetProfileUpdateMsg(assetProfileUpdateMsg).build();
                } else {
                    log.info("Skipping event as asset profile was not found [{}]", cloudEvent);
                }
                break;
            case DELETED:
                AssetProfileUpdateMsg assetProfileUpdateMsg =
                        assetProfileMsgConstructor.constructAssetProfileDeleteMsg(assetProfileId);
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetProfileUpdateMsg(assetProfileUpdateMsg).build();
                break;
        }
        return msg;
    }

}
