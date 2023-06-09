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
package org.thingsboard.server.dao.sql.ota;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.DeviceGroupOtaPackageEntity;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@SqlDao
public class JpaDeviceGroupOtaPackageDao implements DeviceGroupOtaPackageDao {

    private final DeviceGroupOtaPackageRepository deviceGroupOtaPackageRepository;

    @Override
    public DeviceGroupOtaPackage findDeviceGroupOtaPackageById(UUID id) {
        return DaoUtil.getData(deviceGroupOtaPackageRepository.findById(id));
    }

    @Override
    public DeviceGroupOtaPackage findDeviceGroupOtaPackageByGroupIdAndType(UUID groupId, OtaPackageType type) {
        return DaoUtil.getData(deviceGroupOtaPackageRepository.findByGroupIdAndOtaPackageType(groupId, type));
    }

    @Override
    public DeviceGroupOtaPackage saveDeviceGroupOtaPackage(DeviceGroupOtaPackage deviceGroupOtaPackage) {
        if (deviceGroupOtaPackage.getId() == null) {
            UUID uuid = Uuids.timeBased();
            deviceGroupOtaPackage.setId(uuid);
        }
        return DaoUtil.getData(deviceGroupOtaPackageRepository.save(new DeviceGroupOtaPackageEntity(deviceGroupOtaPackage)));
    }

    @Override
    public boolean deleteDeviceGroupOtaPackage(UUID id) {
        deviceGroupOtaPackageRepository.deleteById(id);
        log.debug("Remove request: {}", id);
        return !deviceGroupOtaPackageRepository.existsById(id);
    }
}
