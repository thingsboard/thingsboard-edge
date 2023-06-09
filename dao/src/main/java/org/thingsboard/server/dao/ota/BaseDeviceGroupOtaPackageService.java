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
package org.thingsboard.server.dao.ota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseDeviceGroupOtaPackageService implements DeviceGroupOtaPackageService {

    private final DeviceGroupOtaPackageDao DeviceGroupOtaPackageDao;
    private final OtaPackageDao otaPackageDao;
    private final EntityGroupDao entityGroupDao;

    @Override
    public DeviceGroupOtaPackage findDeviceGroupOtaPackageById(UUID id) {
        log.trace("Executing findDeviceGroupOtaPackageById [{}]", id);
        validateId(id, "Incorrect DeviceGroupOtaPackageId" + id);
        return DeviceGroupOtaPackageDao.findDeviceGroupOtaPackageById(id);
    }

    @Override
    public DeviceGroupOtaPackage findDeviceGroupOtaPackageByGroupIdAndType(EntityGroupId groupId, OtaPackageType otaPackageType) {
        log.trace("Executing findDeviceGroupOtaPackageByGroupIdAndType [{}], [{}]", groupId, otaPackageType);
        validateId(groupId, "Incorrect groupId" + groupId);
        return DeviceGroupOtaPackageDao.findDeviceGroupOtaPackageByGroupIdAndType(groupId.getId(), otaPackageType);
    }

    @Override
    public DeviceGroupOtaPackage saveDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage) {
        log.trace("Executing saveDeviceGroupOtaPackage [{}]", deviceGroupOtaPackage);
        deviceGroupOtaPackage.setOtaPackageUpdateTime(System.currentTimeMillis());
        validate(tenantId, deviceGroupOtaPackage);
        return DeviceGroupOtaPackageDao.saveDeviceGroupOtaPackage(deviceGroupOtaPackage);
    }

    @Override
    public void deleteDeviceGroupOtaPackage(UUID id) {
        log.trace("Executing deleteDeviceGroupOtaPackage [{}]", id);
        validateId(id, "Incorrect DeviceGroupOtaPackageId" + id);
        DeviceGroupOtaPackageDao.deleteDeviceGroupOtaPackage(id);
    }

    private void validate(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage) {
        if (deviceGroupOtaPackage.getGroupId() == null) {
            throw new DataValidationException("DeviceGroupOtaPackage should be assigned to entity group!");
        }

        EntityGroup entityGroup = entityGroupDao.findById(tenantId, deviceGroupOtaPackage.getGroupId().getId());
        if (entityGroup == null) {
            throw new DataValidationException("OtaPackage is referencing to non-existent entity group!");
        }

        if (!entityGroup.getType().equals(EntityType.DEVICE)) {
            throw new DataValidationException("DeviceGroupOtaPackage can be only assigned to the Device group!");
        }

        if (entityGroup.getName().equals("All")) {
            throw new DataValidationException("DeviceGroupOtaPackage can`t be assigned to the group All!");
        }

        if (deviceGroupOtaPackage.getOtaPackageType() == null) {
            throw new DataValidationException("Type should be specified!");
        }

        if (deviceGroupOtaPackage.getId() != null) {
            DeviceGroupOtaPackage oldDeviceGroupOtaPackage = DeviceGroupOtaPackageDao.findDeviceGroupOtaPackageById(deviceGroupOtaPackage.getId());
            if (!deviceGroupOtaPackage.getGroupId().equals(oldDeviceGroupOtaPackage.getGroupId())) {
                throw new DataValidationException("Updating groupId is prohibited!");
            }
            if (!deviceGroupOtaPackage.getOtaPackageType().equals(oldDeviceGroupOtaPackage.getOtaPackageType())) {
                throw new DataValidationException("Updating OtaPackageType is prohibited!");
            }
        }

        if (deviceGroupOtaPackage.getOtaPackageId() == null) {
            throw new DataValidationException("DeviceGroupOtaPackage should be assigned to OtaPackage!");
        } else {
            OtaPackageInfo otaPackageInfo = otaPackageDao.findById(tenantId, deviceGroupOtaPackage.getOtaPackageId().getId());
            if (otaPackageInfo == null) {
                throw new DataValidationException("DeviceGroupOtaPackage is referencing to non-existent OtaPackage!");
            }
            if (!otaPackageInfo.getType().equals(deviceGroupOtaPackage.getOtaPackageType())) {
                throw new DataValidationException("DeviceGroupOtaPackage type should be the same as OtaPackage type!");
            }
        }
    }
}
