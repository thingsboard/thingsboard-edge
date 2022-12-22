/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;

import java.util.UUID;

@Component
@Slf4j
public class AssetCloudProcessor extends BaseCloudProcessor {

    public ListenableFuture<Void> processAssetMsgFromCloud(TenantId tenantId,
                                                           AssetUpdateMsg assetUpdateMsg,
                                                           Long queueStartTs) {
        AssetId assetId = new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
        switch (assetUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                saveOrUpdateAsset(tenantId, assetId, assetUpdateMsg);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                if (assetUpdateMsg.hasEntityGroupIdMSB() && assetUpdateMsg.hasEntityGroupIdLSB()) {
                    UUID entityGroupUUID = safeGetUUID(assetUpdateMsg.getEntityGroupIdMSB(),
                            assetUpdateMsg.getEntityGroupIdLSB());
                    EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                    entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, assetId);
                } else {
                    Asset assetById = assetService.findAssetById(tenantId, assetId);
                    if (assetById != null) {
                        assetService.deleteAsset(tenantId, assetId);
                    }
                }
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(assetUpdateMsg.getMsgType());
        }

        return Futures.transform(requestForAdditionalData(tenantId, assetUpdateMsg.getMsgType(), assetId, queueStartTs), future -> null, dbCallbackExecutor);
    }

    private void saveOrUpdateAsset(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg) {
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
            asset.setCustomerId(safeGetCustomerId(assetUpdateMsg.getCustomerIdMSB(), assetUpdateMsg.getCustomerIdLSB()));
            if (assetUpdateMsg.hasAssetProfileIdMSB() && assetUpdateMsg.hasAssetProfileIdLSB()) {
                AssetProfileId assetProfileId = new AssetProfileId(
                        new UUID(assetUpdateMsg.getAssetProfileIdMSB(),
                                assetUpdateMsg.getAssetProfileIdLSB()));
                asset.setAssetProfileId(assetProfileId);
            }
            if (created) {
                assetValidator.validate(asset, Asset::getTenantId);
                asset.setId(assetId);
            } else {
                assetValidator.validate(asset, Asset::getTenantId);
            }
            Asset savedAsset = assetService.saveAsset(asset, false);
            if (created) {
                entityGroupService.addEntityToEntityGroupAll(savedAsset.getTenantId(), savedAsset.getOwnerId(), savedAsset.getId());
            }
            addToEntityGroup(tenantId, assetUpdateMsg, assetId);
        } finally {
            assetCreationLock.unlock();
        }
    }

    private void addToEntityGroup(TenantId tenantId, AssetUpdateMsg assetUpdateMsg, AssetId assetId) {
        if (assetUpdateMsg.hasEntityGroupIdMSB() && assetUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(assetUpdateMsg.getEntityGroupIdMSB(),
                    assetUpdateMsg.getEntityGroupIdLSB());
            EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
            addEntityToGroup(tenantId, entityGroupId, assetId);
        }
    }
}
