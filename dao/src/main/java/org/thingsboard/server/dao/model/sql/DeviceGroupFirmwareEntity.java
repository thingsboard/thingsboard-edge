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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import org.thingsboard.server.common.data.firmware.DeviceGroupFirmware;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_FIRMWARE_FIRMWARE_ID;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_FIRMWARE_FIRMWARE_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_FIRMWARE_FIRMWARE_UPDATE_TIME;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_FIRMWARE_GROUP_ID;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_FIRMWARE_ID;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_FIRMWARE_TABLE_NAME;

@Data
@Entity
@Table(name = DEVICE_GROUP_FIRMWARE_TABLE_NAME)
public class DeviceGroupFirmwareEntity implements ToData<DeviceGroupFirmware> {
    @Id
    @Column(name = DEVICE_GROUP_FIRMWARE_ID, columnDefinition = "uuid")
    private UUID id;

    @Column(name = DEVICE_GROUP_FIRMWARE_GROUP_ID, columnDefinition = "uuid")
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = DEVICE_GROUP_FIRMWARE_FIRMWARE_TYPE)
    private FirmwareType firmwareType;

    @Column(name = DEVICE_GROUP_FIRMWARE_FIRMWARE_ID, columnDefinition = "uuid")
    private UUID firmwareId;

    @Column(name = DEVICE_GROUP_FIRMWARE_FIRMWARE_UPDATE_TIME)
    private long firmwareUpdateTime;

    public DeviceGroupFirmwareEntity() {
        super();
    }

    public DeviceGroupFirmwareEntity(DeviceGroupFirmware deviceGroupFirmware) {
        this.id = deviceGroupFirmware.getId();
        this.groupId = deviceGroupFirmware.getGroupId().getId();
        this.firmwareType = deviceGroupFirmware.getFirmwareType();
        this.firmwareId = deviceGroupFirmware.getFirmwareId().getId();
        this.firmwareUpdateTime = deviceGroupFirmware.getFirmwareUpdateTime();
    }

    @Override
    public DeviceGroupFirmware toData() {
        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();
        deviceGroupFirmware.setId(id);
        deviceGroupFirmware.setGroupId(new EntityGroupId(groupId));
        deviceGroupFirmware.setFirmwareType(firmwareType);
        deviceGroupFirmware.setFirmwareId(new FirmwareId(firmwareId));
        deviceGroupFirmware.setFirmwareUpdateTime(firmwareUpdateTime);
        return deviceGroupFirmware;
    }
}
