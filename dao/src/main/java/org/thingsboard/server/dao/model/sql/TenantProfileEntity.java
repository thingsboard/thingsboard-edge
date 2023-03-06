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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonBinaryType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name = ModelConstants.TENANT_PROFILE_COLUMN_FAMILY_NAME)
public final class TenantProfileEntity extends BaseSqlEntity<TenantProfile> implements SearchTextEntity<TenantProfile> {

    @Column(name = ModelConstants.TENANT_PROFILE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.TENANT_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.TENANT_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    @Column(name = ModelConstants.TENANT_PROFILE_ISOLATED_TB_RULE_ENGINE)
    private boolean isolatedTbRuleEngine;

    @Type(type = "jsonb")
    @Column(name = ModelConstants.TENANT_PROFILE_PROFILE_DATA_PROPERTY, columnDefinition = "jsonb")
    private JsonNode profileData;

    public TenantProfileEntity() {
        super();
    }

    public TenantProfileEntity(TenantProfile tenantProfile) {
        if (tenantProfile.getId() != null) {
            this.setUuid(tenantProfile.getId().getId());
        }
        this.setCreatedTime(tenantProfile.getCreatedTime());
        this.name = tenantProfile.getName();
        this.description = tenantProfile.getDescription();
        this.isDefault = tenantProfile.isDefault();
        this.isolatedTbRuleEngine = tenantProfile.isIsolatedTbRuleEngine();
        this.profileData = JacksonUtil.convertValue(tenantProfile.getProfileData(), ObjectNode.class);
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
    public TenantProfile toData() {
        TenantProfile tenantProfile = new TenantProfile(new TenantProfileId(this.getUuid()));
        tenantProfile.setCreatedTime(createdTime);
        tenantProfile.setName(name);
        tenantProfile.setDescription(description);
        tenantProfile.setDefault(isDefault);
        tenantProfile.setIsolatedTbRuleEngine(isolatedTbRuleEngine);
        tenantProfile.setProfileData(JacksonUtil.convertValue(profileData, TenantProfileData.class));
        return tenantProfile;
    }

}
