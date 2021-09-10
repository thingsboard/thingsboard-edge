/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.asset;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.importing.AbstractBulkImportService;
import org.thingsboard.server.service.importing.BulkImportColumnType;
import org.thingsboard.server.service.importing.BulkImportRequest;
import org.thingsboard.server.service.importing.ImportedEntityInfo;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Map;
import java.util.Optional;

@Service
@TbCoreComponent
public class AssetBulkImportService extends AbstractBulkImportService<Asset> {
    private final AssetService assetService;

    public AssetBulkImportService(TelemetrySubscriptionService tsSubscriptionService, TbTenantProfileCache tenantProfileCache,
                                  AccessControlService accessControlService, AccessValidator accessValidator,
                                  EntityActionService entityActionService, TbClusterService clusterService, AssetService assetService) {
        super(tsSubscriptionService, tenantProfileCache, accessControlService, accessValidator, entityActionService, clusterService);
        this.assetService = assetService;
    }

    @Override
    protected Asset saveEntity(BulkImportRequest importRequest, Map<BulkImportColumnType, String> fields, Asset asset, SecurityUser user) {
        return assetService.saveAsset(asset);
    }

    @Override
    protected Asset findOrCreateAndSetFields(BulkImportRequest request, Map<BulkImportColumnType, String> fields, ImportedEntityInfo<Asset> importedEntityInfo, SecurityUser user) {
        Asset asset = new Asset();
        asset.setTenantId(user.getTenantId());
        asset.setCustomerId(request.getCustomerId());

        setAssetFields(asset, fields);

        Asset existingAsset = assetService.findAssetByTenantIdAndName(user.getTenantId(), asset.getName());
        if (existingAsset != null && request.getMapping().getUpdate()) {
            importedEntityInfo.setOldEntity(new Asset(existingAsset));
            importedEntityInfo.setUpdated(true);
            existingAsset.update(asset);
            asset = existingAsset;
        }

        return asset;
    }

    private void setAssetFields(Asset asset, Map<BulkImportColumnType, String> fields) {
        ObjectNode additionalInfo = (ObjectNode) Optional.ofNullable(asset.getAdditionalInfo()).orElseGet(JacksonUtil::newObjectNode);
        fields.forEach((columnType, value) -> {
            switch (columnType) {
                case NAME:
                    asset.setName(value);
                    break;
                case TYPE:
                    asset.setType(value);
                    break;
                case LABEL:
                    asset.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
            }
        });
        asset.setAdditionalInfo(additionalInfo);
    }

}
