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
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.mapper;

@ApiModel
@Data
@ToString(exclude = {"image", "profileDataBytes"})
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class DeviceProfile extends BaseDataWithAdditionalInfo<DeviceProfileId> implements HasName, TenantEntity, HasOtaPackage, HasRuleEngineProfile, ExportableEntity<DeviceProfileId> {

    private static final long serialVersionUID = 6998485460273302018L;

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id that owns the profile.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 4, value = "Unique Device Profile Name in scope of Tenant.", example = "Moisture Sensor")
    private String name;
    @NoXss
    @ApiModelProperty(position = 11, value = "Device Profile description. ")
    private String description;
    @Length(fieldName = "image", max = 1000000)
    @ApiModelProperty(position = 12, value = "Either URL or Base64 data of the icon. Used in the mobile application to visualize set of device profiles in the grid view. ")
    private String image;
    private boolean isDefault;
    @ApiModelProperty(position = 16, value = "Type of the profile. Always 'DEFAULT' for now. Reserved for future use.")
    private DeviceProfileType type;
    @ApiModelProperty(position = 14, value = "Type of the transport used to connect the device. Default transport supports HTTP, CoAP and MQTT.")
    private DeviceTransportType transportType;
    @ApiModelProperty(position = 15, value = "Provisioning strategy.")
    private DeviceProfileProvisionType provisionType;
    @ApiModelProperty(position = 7, value = "Reference to the rule chain. " +
            "If present, the specified rule chain will be used to process all messages related to device, including telemetry, attribute updates, etc. " +
            "Otherwise, the root rule chain will be used to process those messages.")
    private RuleChainId defaultRuleChainId;
    @ApiModelProperty(position = 6, value = "Reference to the dashboard. Used in the mobile application to open the default dashboard when user navigates to device details.")
    private DashboardId defaultDashboardId;

    @NoXss
    @ApiModelProperty(position = 8, value = "Rule engine queue name. " +
            "If present, the specified queue will be used to store all unprocessed messages related to device, including telemetry, attribute updates, etc. " +
            "Otherwise, the 'Main' queue will be used to store those messages.")
    private String defaultQueueName;
    @Valid
    private transient DeviceProfileData profileData;
    @JsonIgnore
    private byte[] profileDataBytes;
    @NoXss
    @ApiModelProperty(position = 13, value = "Unique provisioning key used by 'Device Provisioning' feature.")
    private String provisionDeviceKey;

    @ApiModelProperty(position = 9, value = "Reference to the firmware OTA package. If present, the specified package will be used as default device firmware. ")
    private OtaPackageId firmwareId;
    @ApiModelProperty(position = 10, value = "Reference to the software OTA package. If present, the specified package will be used as default device software. ")
    private OtaPackageId softwareId;

    @ApiModelProperty(position = 17, value = "Reference to the edge rule chain. " +
            "If present, the specified edge rule chain will be used on the edge to process all messages related to device, including telemetry, attribute updates, etc. " +
            "Otherwise, the edge root rule chain will be used to process those messages.")
    private RuleChainId defaultEdgeRuleChainId;

    private DeviceProfileId externalId;

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
        this.defaultEdgeRuleChainId = deviceProfile.getDefaultEdgeRuleChainId();
        this.externalId = deviceProfile.getExternalId();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the device profile Id. " +
            "Specify this field to update the device profile. " +
            "Referencing non-existing device profile Id will cause error. " +
            "Omit this field to create new device profile.")
    @Override
    public DeviceProfileId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the profile creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 5, value = "Used to mark the default profile. Default profile is used when the device profile is not specified during device creation.")
    public boolean isDefault(){
        return isDefault;
    }

    @ApiModelProperty(position = 16, value = "Complex JSON object that includes addition device profile configuration (transport, alarm rules, etc).")
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
