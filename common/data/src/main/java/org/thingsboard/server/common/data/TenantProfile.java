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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

@ApiModel
@Data
@ToString(exclude = {"profileDataBytes"})
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class TenantProfile extends BaseDataWithAdditionalInfo<TenantProfileId> implements HasName {

    private static final long serialVersionUID = 3021989561267192281L;

    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 3, value = "Name of the tenant profile", example = "High Priority Tenants")
    private String name;
    @NoXss
    @ApiModelProperty(position = 4, value = "Description of the tenant profile", example = "Any text")
    private String description;
    @ApiModelProperty(position = 5, value = "Default Tenant profile to be used.", example = "true")
    private boolean isDefault;
    @ApiModelProperty(position = 6, value = "If enabled, will push all messages related to this tenant and processed by the rule engine into separate queue. " +
            "Useful for complex microservices deployments, to isolate processing of the data for specific tenants", example = "true")
    private boolean isolatedTbRuleEngine;
    @ApiModelProperty(position = 7, value = "Complex JSON object that contains profile settings: queue configs, max devices, max assets, rate limits, etc.")
    private transient TenantProfileData profileData;
    @JsonIgnore
    private byte[] profileDataBytes;

    public TenantProfile() {
        super();
    }

    public TenantProfile(TenantProfileId tenantProfileId) {
        super(tenantProfileId);
    }

    public TenantProfile(TenantProfile tenantProfile) {
        super(tenantProfile);
        this.name = tenantProfile.getName();
        this.description = tenantProfile.getDescription();
        this.isDefault = tenantProfile.isDefault();
        this.isolatedTbRuleEngine = tenantProfile.isIsolatedTbRuleEngine();
        this.setProfileData(tenantProfile.getProfileData());
    }

    @ApiModelProperty(position = 1, value = "JSON object with the tenant profile Id. " +
            "Specify this field to update the tenant profile. " +
            "Referencing non-existing tenant profile Id will cause error. " +
            "Omit this field to create new tenant profile.")
    @Override
    public TenantProfileId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the tenant profile creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public String getName() {
        return name;
    }

    public TenantProfileData getProfileData() {
        if (profileData != null) {
            return profileData;
        } else {
            if (profileDataBytes != null) {
                try {
                    profileData = mapper.readValue(new ByteArrayInputStream(profileDataBytes), TenantProfileData.class);
                } catch (IOException e) {
                    log.warn("Can't deserialize tenant profile data: ", e);
                    return createDefaultTenantProfileData();
                }
                return profileData;
            } else {
                return createDefaultTenantProfileData();
            }
        }
    }

    @JsonIgnore
    public Optional<DefaultTenantProfileConfiguration> getProfileConfiguration() {
        return Optional.ofNullable(getProfileData().getConfiguration())
                .filter(profileConfiguration -> profileConfiguration instanceof DefaultTenantProfileConfiguration)
                .map(profileConfiguration -> (DefaultTenantProfileConfiguration) profileConfiguration);
    }

    @JsonIgnore
    public DefaultTenantProfileConfiguration getDefaultProfileConfiguration() {
        return getProfileConfiguration().orElse(null);
    }

    public TenantProfileData createDefaultTenantProfileData() {
        TenantProfileData tpd = new TenantProfileData();
        tpd.setConfiguration(new DefaultTenantProfileConfiguration());
        this.profileData = tpd;
        return tpd;
    }

    public void setProfileData(TenantProfileData data) {
        this.profileData = data;
        try {
            this.profileDataBytes = data != null ? mapper.writeValueAsBytes(data) : null;
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize tenant profile data: ", e);
        }
    }

}
