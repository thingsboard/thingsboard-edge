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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.ASSET_PROFILE_TABLE_NAME)
public final class AssetProfileEntity extends BaseSqlEntity<AssetProfile> implements SearchTextEntity<AssetProfile> {

    @Column(name = ModelConstants.ASSET_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.ASSET_PROFILE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.ASSET_PROFILE_IMAGE_PROPERTY)
    private String image;

    @Column(name = ModelConstants.ASSET_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.ASSET_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    @Column(name = ModelConstants.ASSET_PROFILE_DEFAULT_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID defaultRuleChainId;

    @Column(name = ModelConstants.ASSET_PROFILE_DEFAULT_DASHBOARD_ID_PROPERTY)
    private UUID defaultDashboardId;

    @Column(name = ModelConstants.ASSET_PROFILE_DEFAULT_QUEUE_NAME_PROPERTY)
    private String defaultQueueName;

    @Column(name = ModelConstants.ASSET_PROFILE_DEFAULT_EDGE_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID defaultEdgeRuleChainId;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public AssetProfileEntity() {
        super();
    }

    public AssetProfileEntity(AssetProfile assetProfile) {
        if (assetProfile.getId() != null) {
            this.setUuid(assetProfile.getId().getId());
        }
        if (assetProfile.getTenantId() != null) {
            this.tenantId = assetProfile.getTenantId().getId();
        }
        this.setCreatedTime(assetProfile.getCreatedTime());
        this.name = assetProfile.getName();
        this.image = assetProfile.getImage();
        this.description = assetProfile.getDescription();
        this.isDefault = assetProfile.isDefault();
        if (assetProfile.getDefaultRuleChainId() != null) {
            this.defaultRuleChainId = assetProfile.getDefaultRuleChainId().getId();
        }
        if (assetProfile.getDefaultDashboardId() != null) {
            this.defaultDashboardId = assetProfile.getDefaultDashboardId().getId();
        }
        this.defaultQueueName = assetProfile.getDefaultQueueName();
        if (assetProfile.getDefaultEdgeRuleChainId() != null) {
            this.defaultEdgeRuleChainId = assetProfile.getDefaultEdgeRuleChainId().getId();
        }
        if (assetProfile.getExternalId() != null) {
            this.externalId = assetProfile.getExternalId().getId();
        }
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

    @Override
    public AssetProfile toData() {
        AssetProfile assetProfile = new AssetProfile(new AssetProfileId(this.getUuid()));
        assetProfile.setCreatedTime(createdTime);
        if (tenantId != null) {
            assetProfile.setTenantId(TenantId.fromUUID(tenantId));
        }
        assetProfile.setName(name);
        assetProfile.setImage(image);
        assetProfile.setDescription(description);
        assetProfile.setDefault(isDefault);
        assetProfile.setDefaultQueueName(defaultQueueName);
        if (defaultRuleChainId != null) {
            assetProfile.setDefaultRuleChainId(new RuleChainId(defaultRuleChainId));
        }
        if (defaultDashboardId != null) {
            assetProfile.setDefaultDashboardId(new DashboardId(defaultDashboardId));
        }
        if (defaultEdgeRuleChainId != null) {
            assetProfile.setDefaultEdgeRuleChainId(new RuleChainId(defaultEdgeRuleChainId));
        }
        if (externalId != null) {
            assetProfile.setExternalId(new AssetProfileId(externalId));
        }

        return assetProfile;
    }
}
