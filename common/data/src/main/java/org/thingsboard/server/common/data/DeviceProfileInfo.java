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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "image")
public class DeviceProfileInfo extends EntityInfo {

    @ApiModelProperty(position = 3, value = "Either URL or Base64 data of the icon. Used in the mobile application to visualize set of device profiles in the grid view. ")
    private final String image;
    @ApiModelProperty(position = 4, value = "Reference to the dashboard. Used in the mobile application to open the default dashboard when user navigates to device details.")
    private final DashboardId defaultDashboardId;
    @ApiModelProperty(position = 5, value = "Type of the profile. Always 'DEFAULT' for now. Reserved for future use.")
    private final DeviceProfileType type;
    @ApiModelProperty(position = 6, value = "Type of the transport used to connect the device. Default transport supports HTTP, CoAP and MQTT.")
    private final DeviceTransportType transportType;

    @ApiModelProperty(position = 7, value = "Tenant id.")
    private final TenantId tenantId;

    @JsonCreator
    public DeviceProfileInfo(@JsonProperty("id") EntityId id,
                             @JsonProperty("tenantId") TenantId tenantId,
                             @JsonProperty("name") String name,
                             @JsonProperty("image") String image,
                             @JsonProperty("defaultDashboardId") DashboardId defaultDashboardId,
                             @JsonProperty("type") DeviceProfileType type,
                             @JsonProperty("transportType") DeviceTransportType transportType) {
        super(id, name);
        this.tenantId = tenantId;
        this.image = image;
        this.defaultDashboardId = defaultDashboardId;
        this.type = type;
        this.transportType = transportType;
    }

    public DeviceProfileInfo(UUID uuid, UUID tenantId, String name, String image, UUID defaultDashboardId, DeviceProfileType type, DeviceTransportType transportType) {
        super(EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE_PROFILE, uuid), name);
        this.tenantId = new TenantId(tenantId);
        this.image = image;
        this.defaultDashboardId = defaultDashboardId != null ? new DashboardId(defaultDashboardId) : null;
        this.type = type;
        this.transportType = transportType;
    }

    public DeviceProfileInfo(DeviceProfile profile) {
        this(profile.getId(), profile.getTenantId(), profile.getName(), profile.getImage(), profile.getDefaultDashboardId(),
                profile.getType(), profile.getTransportType());
    }

}
