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
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_ID;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_UPDATE_TIME;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_GROUP_ID;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_ID;
import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_GROUP_OTA_PACKAGE_TABLE_NAME;

@Data
@Entity
@Table(name = DEVICE_GROUP_OTA_PACKAGE_TABLE_NAME)
public class DeviceGroupOtaPackageEntity implements ToData<DeviceGroupOtaPackage> {
    @Id
    @Column(name = DEVICE_GROUP_OTA_PACKAGE_ID, columnDefinition = "uuid")
    private UUID id;

    @Column(name = DEVICE_GROUP_OTA_PACKAGE_GROUP_ID, columnDefinition = "uuid")
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_TYPE)
    private OtaPackageType otaPackageType;

    @Column(name = DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_ID, columnDefinition = "uuid")
    private UUID otaPackageId;

    @Column(name = DEVICE_GROUP_OTA_PACKAGE_FIRMWARE_UPDATE_TIME)
    private long otaPackageUpdateTime;

    public DeviceGroupOtaPackageEntity() {
        super();
    }

    public DeviceGroupOtaPackageEntity(DeviceGroupOtaPackage deviceGroupOtaPackage) {
        this.id = deviceGroupOtaPackage.getId();
        this.groupId = deviceGroupOtaPackage.getGroupId().getId();
        this.otaPackageType = deviceGroupOtaPackage.getOtaPackageType();
        this.otaPackageId = deviceGroupOtaPackage.getOtaPackageId().getId();
        this.otaPackageUpdateTime = deviceGroupOtaPackage.getOtaPackageUpdateTime();
    }

    @Override
    public DeviceGroupOtaPackage toData() {
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setId(id);
        deviceGroupOtaPackage.setGroupId(new EntityGroupId(groupId));
        deviceGroupOtaPackage.setOtaPackageType(otaPackageType);
        deviceGroupOtaPackage.setOtaPackageId(new OtaPackageId(otaPackageId));
        deviceGroupOtaPackage.setOtaPackageUpdateTime(otaPackageUpdateTime);
        return deviceGroupOtaPackage;
    }
}
