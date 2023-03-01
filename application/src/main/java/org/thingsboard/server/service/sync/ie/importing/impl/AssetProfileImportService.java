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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AssetProfileImportService extends BaseEntityImportService<AssetProfileId, AssetProfile, EntityExportData<AssetProfile>> {

    private final AssetProfileService assetProfileService;

    @Override
    protected void setOwner(TenantId tenantId, AssetProfile assetProfile, IdProvider idProvider) {
        assetProfile.setTenantId(tenantId);
    }

    @Override
    protected AssetProfile prepare(EntitiesImportCtx ctx, AssetProfile assetProfile, AssetProfile old, EntityExportData<AssetProfile> exportData, IdProvider idProvider) {
        assetProfile.setDefaultRuleChainId(idProvider.getInternalId(assetProfile.getDefaultRuleChainId()));
        assetProfile.setDefaultDashboardId(idProvider.getInternalId(assetProfile.getDefaultDashboardId()));
        return assetProfile;
    }

    @Override
    protected AssetProfile saveOrUpdate(EntitiesImportCtx ctx, AssetProfile assetProfile, EntityExportData<AssetProfile> exportData, IdProvider idProvider) {
        return assetProfileService.saveAssetProfile(assetProfile);
    }

    @Override
    protected void onEntitySaved(User user, AssetProfile savedAssetProfile, AssetProfile oldAssetProfile) throws ThingsboardException {
        clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedAssetProfile.getId(),
                oldAssetProfile == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        entityNotificationService.notifyCreateOrUpdateOrDelete(savedAssetProfile.getTenantId(), null,
                savedAssetProfile.getId(), savedAssetProfile, user, oldAssetProfile == null ? ActionType.ADDED : ActionType.UPDATED, true, false, null);
    }

    @Override
    protected AssetProfile deepCopy(AssetProfile assetProfile) {
        return new AssetProfile(assetProfile);
    }

    @Override
    protected void cleanupForComparison(AssetProfile assetProfile) {
        super.cleanupForComparison(assetProfile);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET_PROFILE;
    }

}
