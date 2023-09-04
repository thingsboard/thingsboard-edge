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
import org.springframework.beans.factory.annotation.Autowired;
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
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbMsgMetaData;
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
                        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAssetProfile(tenantId, assetProfileId, assetProfileUpdateMsg);
                        boolean created = resultPair.getFirst();
                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetProfileId,
                                created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
                        AssetProfile assetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
                        if (!assetProfile.isDefault() && assetProfileUpdateMsg.getDefault()) {
                            assetProfileService.setDefaultAssetProfile(tenantId, assetProfileId);
                        }
                        if (removePreviousProfile) {
                            updateAssets(tenantId, assetProfileId, assetProfileByName.getId());
                            assetProfileService.deleteAssetProfile(tenantId, assetProfileByName.getId());
                            tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetProfileByName.getId(), ComponentLifecycleEvent.DELETED);
                        }
                        if (created) {
                            pushAssetProfileCreatedEventToRuleEngine(tenantId, assetProfileId);
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
                        pushAssetProfileDeletedEventToRuleEngine(tenantId, assetProfile);
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

    private void pushAssetProfileCreatedEventToRuleEngine(TenantId tenantId, AssetProfileId assetProfileId) {
        AssetProfile assetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
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

    @Override
    protected void setDefaultRuleChainId(TenantId tenantId, AssetProfile assetProfile, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        UUID defaultRuleChainUUID = safeGetUUID(assetProfileUpdateMsg.getDefaultRuleChainIdMSB(), assetProfileUpdateMsg.getDefaultRuleChainIdLSB());
        RuleChain ruleChain = null;
        if (defaultRuleChainUUID != null) {
            ruleChain = ruleChainService.findRuleChainById(tenantId, new RuleChainId(defaultRuleChainUUID));
        }
        assetProfile.setDefaultRuleChainId(ruleChain != null ? ruleChain.getId() : null);
    }

    @Override
    protected void setDefaultEdgeRuleChainId(TenantId tenantId, AssetProfile assetProfile, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        // do nothing on edge
    }

    @Override
    protected void setDefaultDashboardId(TenantId tenantId, AssetProfile assetProfile, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        UUID defaultDashboardUUID = safeGetUUID(assetProfileUpdateMsg.getDefaultDashboardIdMSB(), assetProfileUpdateMsg.getDefaultDashboardIdLSB());
        DashboardInfo dashboard = null;
        if (defaultDashboardUUID != null) {
            dashboard = dashboardService.findDashboardInfoById(tenantId, new DashboardId(defaultDashboardUUID));
        }
        assetProfile.setDefaultDashboardId(dashboard != null ? dashboard.getId() : null);
    }
}
