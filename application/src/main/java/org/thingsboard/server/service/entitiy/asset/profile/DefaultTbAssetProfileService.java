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
package org.thingsboard.server.service.entitiy.asset.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import static org.thingsboard.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbAssetProfileService extends AbstractTbEntityService implements TbAssetProfileService {

    private final AssetProfileService assetProfileService;

    @Override
    public AssetProfile save(AssetProfile assetProfile, User user) throws Exception {
        ActionType actionType = assetProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = assetProfile.getTenantId();
        try {
            if (TB_SERVICE_QUEUE.equals(assetProfile.getName())) {
                throw new ThingsboardException("Unable to save asset profile with name " + TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            } else if (assetProfile.getId() != null) {
                AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfile.getId());
                if (foundAssetProfile != null && TB_SERVICE_QUEUE.equals(foundAssetProfile.getName())) {
                    throw new ThingsboardException("Updating asset profile with name " + TB_SERVICE_QUEUE + " is prohibited!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            AssetProfile savedAssetProfile = checkNotNull(assetProfileService.saveAssetProfile(assetProfile));
            autoCommit(user, savedAssetProfile.getId());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedAssetProfile.getId(),
                    actionType.equals(ActionType.ADDED) ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedAssetProfile.getId(),
                    savedAssetProfile, user, actionType, true, null);
            return savedAssetProfile;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET_PROFILE), assetProfile, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(AssetProfile assetProfile, User user) {
        AssetProfileId assetProfileId = assetProfile.getId();
        TenantId tenantId = assetProfile.getTenantId();
        try {
            assetProfileService.deleteAssetProfile(tenantId, assetProfileId);

            tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetProfileId, ComponentLifecycleEvent.DELETED);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, assetProfileId, assetProfile,
                    user, ActionType.DELETED, true, null, assetProfileId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET_PROFILE), ActionType.DELETED,
                    user, e, assetProfileId.toString());
            throw e;
        }
    }

    @Override
    public AssetProfile setDefaultAssetProfile(AssetProfile assetProfile, AssetProfile previousDefaultAssetProfile, User user) throws ThingsboardException {
        TenantId tenantId = assetProfile.getTenantId();
        AssetProfileId assetProfileId = assetProfile.getId();
        try {
            if (assetProfileService.setDefaultAssetProfile(tenantId, assetProfileId)) {
                if (previousDefaultAssetProfile != null) {
                    previousDefaultAssetProfile = assetProfileService.findAssetProfileById(tenantId, previousDefaultAssetProfile.getId());
                    notificationEntityService.logEntityAction(tenantId, previousDefaultAssetProfile.getId(), previousDefaultAssetProfile,
                            ActionType.UPDATED, user);
                }
                assetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);

                notificationEntityService.logEntityAction(tenantId, assetProfileId, assetProfile, ActionType.UPDATED, user);
            }
            return assetProfile;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET_PROFILE), ActionType.UPDATED,
                    user, e, assetProfileId.toString());
            throw e;
        }
    }
}
