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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ASSET_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_LABEL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ASSET_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractAssetEntity<T extends Asset> extends BaseSqlEntity<T> implements SearchTextEntity<T> {

    @Column(name = ASSET_TENANT_ID_PROPERTY, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = ASSET_CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    @Column(name = ASSET_NAME_PROPERTY)
    private String name;

    @Column(name = ASSET_TYPE_PROPERTY)
    private String type;

    @Column(name = ASSET_LABEL_PROPERTY)
    private String label;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Type(type = "json")
    @Column(name = ModelConstants.ASSET_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.ASSET_ASSET_PROFILE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID assetProfileId;

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public AbstractAssetEntity() {
        super();
    }

    public AbstractAssetEntity(Asset asset) {
        if (asset.getId() != null) {
            this.setUuid(asset.getId().getId());
        }
        this.setCreatedTime(asset.getCreatedTime());
        if (asset.getTenantId() != null) {
            this.tenantId = asset.getTenantId().getId();
        }
        if (asset.getCustomerId() != null) {
            this.customerId = asset.getCustomerId().getId();
        }
        if (asset.getAssetProfileId() != null) {
            this.assetProfileId = asset.getAssetProfileId().getId();
        }
        this.name = asset.getName();
        this.type = asset.getType();
        this.label = asset.getLabel();
        this.additionalInfo = asset.getAdditionalInfo();
        if (asset.getExternalId() != null) {
            this.externalId = asset.getExternalId().getId();
        }
    }

    public AbstractAssetEntity(AssetEntity assetEntity) {
        this.setId(assetEntity.getId());
        this.setCreatedTime(assetEntity.getCreatedTime());
        this.tenantId = assetEntity.getTenantId();
        this.customerId = assetEntity.getCustomerId();
        this.assetProfileId = assetEntity.getAssetProfileId();
        this.type = assetEntity.getType();
        this.name = assetEntity.getName();
        this.label = assetEntity.getLabel();
        this.searchText = assetEntity.getSearchText();
        this.additionalInfo = assetEntity.getAdditionalInfo();
        this.externalId = assetEntity.getExternalId();
    }

    @Override
    public String getSearchTextSource() {
        return name;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

    protected Asset toAsset() {
        Asset asset = new Asset(new AssetId(id));
        asset.setCreatedTime(createdTime);
        if (tenantId != null) {
            asset.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            asset.setCustomerId(new CustomerId(customerId));
        }
        if (assetProfileId != null) {
            asset.setAssetProfileId(new AssetProfileId(assetProfileId));
        }
        asset.setName(name);
        asset.setType(type);
        asset.setLabel(label);
        asset.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            asset.setExternalId(new AssetId(externalId));
        }
        return asset;
    }

}
