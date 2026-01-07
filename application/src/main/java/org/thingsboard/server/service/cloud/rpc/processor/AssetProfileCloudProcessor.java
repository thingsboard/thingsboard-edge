/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.BaseAssetService;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.BaseAssetProfileProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class AssetProfileCloudProcessor extends BaseAssetProfileProcessor {

    public ListenableFuture<Void> processAssetProfileMsgFromCloud(TenantId tenantId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        AssetProfileId assetProfileId = new AssetProfileId(new UUID(assetProfileUpdateMsg.getIdMSB(), assetProfileUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (assetProfileUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    assetCreationLock.lock();
                    try {
                        AssetProfile assetProfileMsg = JacksonUtil.fromString(assetProfileUpdateMsg.getEntity(), AssetProfile.class, true);
                        if (assetProfileMsg == null) {
                            throw new RuntimeException("[{" + tenantId + "}] assetProfileUpdateMsg {" + assetProfileUpdateMsg + "} cannot be converted to asset profile");
                        }
                        AssetProfile assetProfileByName = edgeCtx.getAssetProfileService().findAssetProfileByName(tenantId, assetProfileMsg.getName());
                        boolean removePreviousProfile = false;
                        if (assetProfileByName != null && !assetProfileByName.getId().equals(assetProfileId)) {
                            renamePreviousAssetProfile(assetProfileByName);
                            removePreviousProfile = true;
                        }
                        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAssetProfile(tenantId, assetProfileId, assetProfileUpdateMsg);
                        boolean created = resultPair.getFirst();
                        AssetProfile assetProfile = edgeCtx.getAssetProfileService().findAssetProfileById(tenantId, assetProfileId);
                        if (!assetProfile.isDefault() && assetProfileMsg.isDefault()) {
                            edgeCtx.getAssetProfileService().setDefaultAssetProfile(tenantId, assetProfileId);
                        }
                        if (removePreviousProfile) {
                            updateAssets(tenantId, assetProfileId, assetProfileByName.getId());
                            edgeCtx.getAssetProfileService().deleteAssetProfile(tenantId, assetProfileByName.getId());
                        }
                        if (created) {
                            pushAssetProfileCreatedEventToRuleEngine(tenantId, assetProfileId);
                            return requestForCalculatedFieldData(tenantId, assetProfileId);
                        }
                    } finally {
                        assetCreationLock.unlock();
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    AssetProfile assetProfile = edgeCtx.getAssetProfileService().findAssetProfileById(tenantId, assetProfileId);
                    if (assetProfile != null) {
                        edgeCtx.getAssetProfileService().deleteAssetProfile(tenantId, assetProfileId);
                        pushAssetProfileDeletedEventToRuleEngine(tenantId, assetProfile);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(assetProfileUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    private void pushAssetProfileCreatedEventToRuleEngine(TenantId tenantId, AssetProfileId assetProfileId) {
        AssetProfile assetProfile = edgeCtx.getAssetProfileService().findAssetProfileById(tenantId, assetProfileId);
        pushAssetProfileEventToRuleEngine(tenantId, assetProfile, TbMsgType.ENTITY_CREATED);
    }

    private void pushAssetProfileDeletedEventToRuleEngine(TenantId tenantId, AssetProfile assetProfile) {
        pushAssetProfileEventToRuleEngine(tenantId, assetProfile, TbMsgType.ENTITY_DELETED);
    }

    private void pushAssetProfileEventToRuleEngine(TenantId tenantId, AssetProfile assetProfile, TbMsgType msgType) {
        try {
            String assetProfileAsString = JacksonUtil.toString(assetProfile);
            pushEntityEventToRuleEngine(tenantId, assetProfile.getId(), null, msgType, assetProfileAsString, new TbMsgMetaData());
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push asset profile action to rule engine: {}", tenantId, assetProfile.getId(), msgType.name(), e);
        }
    }

    private void renamePreviousAssetProfile(AssetProfile assetProfileByName) {
        assetProfileByName.setName(assetProfileByName.getName() + StringUtils.randomAlphanumeric(15));
        edgeCtx.getAssetProfileService().saveAssetProfile(assetProfileByName);
    }

    private void updateAssets(TenantId tenantId, AssetProfileId newAssetProfileId, AssetProfileId previousAssetProfileId) {
        PageDataIterable<AssetInfo> assetInfosIterable = new PageDataIterable<>(
                link -> edgeCtx.getAssetService().findAssetInfosByTenantIdAndAssetProfileId(tenantId, previousAssetProfileId, link), 1024);
        assetInfosIterable.forEach(assetInfo -> {
            assetInfo.setAssetProfileId(newAssetProfileId);
            edgeCtx.getAssetService().saveAsset(new Asset(assetInfo));
        });
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        AssetProfileId assetProfileId = new AssetProfileId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                AssetProfile assetProfile = edgeCtx.getAssetProfileService().findAssetProfileById(cloudEvent.getTenantId(), assetProfileId);
                if (assetProfile != null && !BaseAssetService.TB_SERVICE_QUEUE.equals(assetProfile.getName())) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    AssetProfileUpdateMsg assetProfileUpdateMsg = EdgeMsgConstructorUtils.constructAssetProfileUpdatedMsg(msgType, assetProfile);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAssetProfileUpdateMsg(assetProfileUpdateMsg).build();
                } else {
                    log.info("Skipping event as asset profile was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                AssetProfileUpdateMsg assetProfileUpdateMsg = EdgeMsgConstructorUtils.constructAssetProfileDeleteMsg(assetProfileId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetProfileUpdateMsg(assetProfileUpdateMsg).build();
            }
        }
        return null;
    }

    @Override
    protected void setDefaultRuleChainId(TenantId tenantId, AssetProfile assetProfile, RuleChainId ruleChainId) {
        RuleChainId defaultRuleChainId = assetProfile.getDefaultEdgeRuleChainId();
        RuleChain ruleChain = null;
        if (defaultRuleChainId != null) {
            ruleChain = edgeCtx.getRuleChainService().findRuleChainById(tenantId, defaultRuleChainId);
        }
        assetProfile.setDefaultRuleChainId(ruleChain != null ? ruleChain.getId() : null);
    }

    @Override
    protected void setDefaultEdgeRuleChainId(AssetProfile assetProfile, RuleChainId ruleChainId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        assetProfile.setDefaultEdgeRuleChainId(null);
    }

    @Override
    protected void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, AssetProfile assetProfile, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        DashboardId defaultDashboardId = assetProfile.getDefaultDashboardId();
        DashboardInfo dashboard = null;
        if (defaultDashboardId != null) {
            dashboard = edgeCtx.getDashboardService().findDashboardInfoById(tenantId, defaultDashboardId);
        }
        assetProfile.setDefaultDashboardId(dashboard != null ? dashboard.getId() : null);
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.ASSET_PROFILE;
    }

}
