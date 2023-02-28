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
package org.thingsboard.server.service.entitiy.asset;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;

import java.util.List;

import static org.thingsboard.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;

@Service
@AllArgsConstructor
public class DefaultTbAssetService extends AbstractTbEntityService implements TbAssetService {

    private final AssetService assetService;
    private final TbAssetProfileCache assetProfileCache;

    @Override
    public Asset save(Asset asset, EntityGroup entityGroup) throws Exception {
        return save(asset, entityGroup, null);
    }

    @Override
    public Asset save(Asset asset, EntityGroup entityGroup, User user) throws Exception {
        ActionType actionType = asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = asset.getTenantId();
        try {
            if (TB_SERVICE_QUEUE.equals(asset.getType())) {
                throw new ThingsboardException("Unable to save asset with type " + TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            } else if (asset.getAssetProfileId() != null) {
                AssetProfile assetProfile = assetProfileCache.get(tenantId, asset.getAssetProfileId());
                if (assetProfile != null && TB_SERVICE_QUEUE.equals(assetProfile.getName())) {
                    throw new ThingsboardException("Unable to save asset with profile " + TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            Asset savedAsset = checkNotNull(assetService.saveAsset(asset));
            autoCommit(user, savedAsset.getId());
            createOrUpdateGroupEntity(tenantId, savedAsset, entityGroup, actionType, user);
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedAsset.getId(),
                    asset.getId() == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), asset, actionType, user, e);
            throw e;
        }
    }

    @Override
    public ListenableFuture<Void> delete(Asset asset, User user) {
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, assetId);
            assetService.deleteAsset(tenantId, assetId);
            notificationEntityService.notifyDeleteEntity(tenantId, assetId, asset, asset.getCustomerId(),
                    ActionType.DELETED, relatedEdgeIds, user, assetId.toString());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetId, ComponentLifecycleEvent.DELETED);
            return removeAlarmsByEntityId(tenantId, assetId);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), ActionType.DELETED, user, e,
                    assetId.toString());
            throw e;
        }
    }

    @Override
    public ListenableFuture<Void> delete(AssetId assetId, User user) {
        Asset asset = assetService.findAssetById(user.getTenantId(), assetId);
        TenantId tenantId = asset.getTenantId();
        try {
            List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, assetId);
            assetService.deleteAsset(tenantId, assetId);
            notificationEntityService.notifyDeleteEntity(tenantId, assetId, asset, asset.getCustomerId(),
                    ActionType.DELETED, relatedEdgeIds, user, assetId.toString());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetId, ComponentLifecycleEvent.DELETED);
            return removeAlarmsByEntityId(tenantId, assetId);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), ActionType.DELETED, user, e,
                    assetId.toString());
            throw e;
        }
    }
}
