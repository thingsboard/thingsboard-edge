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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class AssetProfileCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private AssetProfileService assetProfileService;

    @Autowired
    private AssetService assetService;

    public ListenableFuture<Void> processAssetProfileMsgFromCloud(TenantId tenantId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        AssetProfileId assetProfileId = new AssetProfileId(new UUID(assetProfileUpdateMsg.getIdMSB(), assetProfileUpdateMsg.getIdLSB()));
        switch (assetProfileUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                assetCreationLock.lock();
                try {
                    AssetProfile assetProfileByName = assetProfileService.findAssetProfileByName(tenantId, assetProfileUpdateMsg.getName());
                    boolean removePreviousProfile = false;
                    if (assetProfileByName != null && !assetProfileByName.getId().equals(assetProfileId)) {
                        renamePreviousAssetProfileCreatedByUpdateScript(assetProfileByName);
                        removePreviousProfile = true;
                    }
                    AssetProfile assetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
                    boolean created = false;
                    if (assetProfile == null) {
                        created = true;
                        assetProfile = new AssetProfile();
                        assetProfile.setId(assetProfileId);
                        assetProfile.setCreatedTime(Uuids.unixTimestamp(assetProfileId.getId()));
                        assetProfile.setTenantId(tenantId);
                    }
                    assetProfile.setName(assetProfileUpdateMsg.getName());
                    AssetProfile defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
                    if (defaultAssetProfile != null && assetProfileId.equals(defaultAssetProfile.getId())) {
                        assetProfile.setDefault(true);
                    }
                    assetProfile.setDefaultQueueName(assetProfileUpdateMsg.hasDefaultQueueName() ? assetProfileUpdateMsg.getDefaultQueueName() : null);
                    assetProfile.setDescription(assetProfileUpdateMsg.hasDescription() ? assetProfileUpdateMsg.getDescription() : null);
                    assetProfile.setImage(assetProfileUpdateMsg.hasImage()
                            ? new String(assetProfileUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);

                    UUID defaultRuleChainUUID = safeGetUUID(assetProfileUpdateMsg.getDefaultRuleChainIdMSB(), assetProfileUpdateMsg.getDefaultRuleChainIdLSB());
                    assetProfile.setDefaultRuleChainId(defaultRuleChainUUID != null ? new RuleChainId(defaultRuleChainUUID) : null);

                    UUID defaultDashboardUUID = safeGetUUID(assetProfileUpdateMsg.getDefaultDashboardIdMSB(), assetProfileUpdateMsg.getDefaultDashboardIdLSB());
                    assetProfile.setDefaultDashboardId(defaultDashboardUUID != null ? new DashboardId(defaultDashboardUUID) : null);

                    AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile, false);
                    tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedAssetProfile.getId(),
                            created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
                    if (!assetProfile.isDefault() && assetProfileUpdateMsg.getDefault()) {
                        assetProfileService.setDefaultAssetProfile(tenantId, savedAssetProfile.getId());
                    }
                    if (removePreviousProfile) {
                        updateAssets(tenantId, savedAssetProfile.getId(), assetProfileByName.getId());
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
        return Futures.immediateFuture(null);
    }

    private void renamePreviousAssetProfileCreatedByUpdateScript(AssetProfile assetProfileByName) {
        assetProfileByName.setName(assetProfileByName.getName() + "_old");
        assetProfileService.saveAssetProfile(assetProfileByName);
    }

    private void updateAssets(TenantId tenantId, AssetProfileId newAssetProfileId, AssetProfileId previousAssetProfileId) {
        PageLink pageLink = new PageLink(100);
        PageData<Asset> pageData;
        do {
            pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
            pageData.getData().forEach(asset -> {
                if (previousAssetProfileId.getId().equals(asset.getAssetProfileId().getId())) {
                    asset.setAssetProfileId(newAssetProfileId);
                    assetService.saveAsset(asset);
                }
            });
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext() && !pageData.getData().isEmpty());
    }
}
