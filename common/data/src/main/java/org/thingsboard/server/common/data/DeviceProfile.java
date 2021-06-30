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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.mapper;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class DeviceProfile extends SearchTextBased<DeviceProfileId> implements HasName, TenantEntity, HasOtaPackage {

    private TenantId tenantId;
    @NoXss
    private String name;
    @NoXss
    private String description;
    private String image;
    private boolean isDefault;
    private DeviceProfileType type;
    private DeviceTransportType transportType;
    private DeviceProfileProvisionType provisionType;
    private RuleChainId defaultRuleChainId;
    private DashboardId defaultDashboardId;
    @NoXss
    private String defaultQueueName;
    @Valid
    private transient DeviceProfileData profileData;
    @JsonIgnore
    private byte[] profileDataBytes;
    @NoXss
    private String provisionDeviceKey;

    private OtaPackageId firmwareId;

    private OtaPackageId softwareId;

    public DeviceProfile() {
        super();
    }

    public DeviceProfile(DeviceProfileId deviceProfileId) {
        super(deviceProfileId);
    }

    public DeviceProfile(DeviceProfile deviceProfile) {
        super(deviceProfile);
        this.tenantId = deviceProfile.getTenantId();
        this.name = deviceProfile.getName();
        this.description = deviceProfile.getDescription();
        this.image = deviceProfile.getImage();
        this.isDefault = deviceProfile.isDefault();
        this.defaultRuleChainId = deviceProfile.getDefaultRuleChainId();
        this.defaultDashboardId = deviceProfile.getDefaultDashboardId();
        this.defaultQueueName = deviceProfile.getDefaultQueueName();
        this.setProfileData(deviceProfile.getProfileData());
        this.provisionDeviceKey = deviceProfile.getProvisionDeviceKey();
        this.firmwareId = deviceProfile.getFirmwareId();
        this.softwareId = deviceProfile.getSoftwareId();
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

    public DeviceProfileData getProfileData() {
        if (profileData != null) {
            return profileData;
        } else {
            if (profileDataBytes != null) {
                try {
                    profileData = mapper.readValue(new ByteArrayInputStream(profileDataBytes), DeviceProfileData.class);
                } catch (IOException e) {
                    log.warn("Can't deserialize device profile data: ", e);
                    return null;
                }
                return profileData;
            } else {
                return null;
            }
        }
    }

    public void setProfileData(DeviceProfileData data) {
        this.profileData = data;
        try {
            this.profileDataBytes = data != null ? mapper.writeValueAsBytes(data) : null;
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize device profile data: ", e);
        }
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.DEVICE_PROFILE;
    }

}
