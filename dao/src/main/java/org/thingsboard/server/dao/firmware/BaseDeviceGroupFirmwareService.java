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
package org.thingsboard.server.dao.firmware;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.firmware.DeviceGroupFirmware;
import org.thingsboard.server.common.data.firmware.FirmwareInfo;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.group.EntityGroupDao;

import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseDeviceGroupFirmwareService implements DeviceGroupFirmwareService {

    private final DeviceGroupFirmwareDao deviceGroupFirmwareDao;
    private final FirmwareDao firmwareDao;
    private final EntityGroupDao entityGroupDao;

    @Override
    public DeviceGroupFirmware findDeviceGroupFirmwareById(UUID id) {
        log.trace("Executing findDeviceGroupFirmwareById [{}]", id);
        validateId(id, "Incorrect deviceGroupFirmwareId" + id);
        return deviceGroupFirmwareDao.findDeviceGroupFirmwareById(id);
    }

    @Override
    public DeviceGroupFirmware findDeviceGroupFirmwareByGroupIdAndFirmwareType(EntityGroupId groupId, FirmwareType firmwareType) {
        log.trace("Executing findDeviceGroupFirmwareByIdAndFirmwareType [{}], [{}]", groupId, firmwareType);
        validateId(groupId, "Incorrect groupId" + groupId);
        return deviceGroupFirmwareDao.findDeviceGroupFirmwareByGroupIdAndFirmwareType(groupId.getId(), firmwareType);
    }

    @Override
    public DeviceGroupFirmware saveDeviceGroupFirmware(TenantId tenantId, DeviceGroupFirmware deviceGroupFirmware) {
        log.trace("Executing saveDeviceGroupFirmware [{}]", deviceGroupFirmware);
        deviceGroupFirmware.setFirmwareUpdateTime(System.currentTimeMillis());
        validate(tenantId, deviceGroupFirmware);
        return deviceGroupFirmwareDao.saveDeviceGroupFirmware(deviceGroupFirmware);
    }

    @Override
    public void deleteDeviceGroupFirmware(UUID id) {
        log.trace("Executing deleteDeviceGroupFirmware [{}]", id);
        validateId(id, "Incorrect deviceGroupFirmwareId" + id);
        deviceGroupFirmwareDao.deleteDeviceGroupFirmware(id);
    }

    private void validate(TenantId tenantId, DeviceGroupFirmware deviceGroupFirmware) {
        if (deviceGroupFirmware.getGroupId() == null) {
            throw new DataValidationException("DeviceGroupFirmware should be assigned to entity group!");
        }

        EntityGroup entityGroup = entityGroupDao.findById(tenantId, deviceGroupFirmware.getGroupId().getId());
        if (entityGroup == null) {
            throw new DataValidationException("Firmware is referencing to non-existent entity group!");
        }

        if (!entityGroup.getType().equals(EntityType.DEVICE)) {
            throw new DataValidationException("DeviceGroupFirmware can be only assigned to the Device group!");
        }

        if (entityGroup.getName().equals("All")) {
            throw new DataValidationException("DeviceGroupFirmware can`t be assigned to the group All!");
        }

        if (deviceGroupFirmware.getFirmwareType() == null) {
            throw new DataValidationException("Type should be specified!");
        }

        if (deviceGroupFirmware.getId() != null) {
            DeviceGroupFirmware oldDeviceGroupFirmware = deviceGroupFirmwareDao.findDeviceGroupFirmwareById(deviceGroupFirmware.getId());
            if (!deviceGroupFirmware.getGroupId().equals(oldDeviceGroupFirmware.getGroupId())) {
                throw new DataValidationException("Updating firmware groupId is prohibited!");
            }
            if (!deviceGroupFirmware.getFirmwareType().equals(oldDeviceGroupFirmware.getFirmwareType())) {
                throw new DataValidationException("Updating firmware type is prohibited!");
            }
        }

        if (deviceGroupFirmware.getFirmwareId() == null) {
            throw new DataValidationException("DeviceGroupFirmware should be assigned to firmware!");
        } else {
            FirmwareInfo firmwareInfo = firmwareDao.findById(tenantId, deviceGroupFirmware.getFirmwareId().getId());
            if (firmwareInfo == null) {
                throw new DataValidationException("Firmware is referencing to non-existent firmware!");
            }
            if (!firmwareInfo.getType().equals(deviceGroupFirmware.getFirmwareType())) {
                throw new DataValidationException("DeviceGroupFirmware firmware type should be the same as Firmware type!");
            }
        }
    }
}
