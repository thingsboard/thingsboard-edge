/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.DeviceExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceImportService extends BaseGroupEntityImportService<DeviceId, Device, DeviceExportData> {

    private final DeviceService deviceService;

    @Override
    protected void setOwner(TenantId tenantId, Device device, IdProvider idProvider) {
        device.setTenantId(tenantId);
        device.setCustomerId(idProvider.getInternalId(device.getCustomerId()));
    }

    @Override
    protected Device prepareAndSave(TenantId tenantId, Device device, Device old, DeviceExportData exportData, IdProvider idProvider, EntityImportSettings importSettings) {
        device.setDeviceProfileId(idProvider.getInternalId(device.getDeviceProfileId()));
        device.setFirmwareId(idProvider.getInternalId(device.getFirmwareId()));
        device.setSoftwareId(idProvider.getInternalId(device.getSoftwareId()));
        if (exportData.getCredentials() != null && importSettings.isSaveCredentials()) {
            exportData.getCredentials().setId(null);
            exportData.getCredentials().setDeviceId(null);
            return deviceService.saveDeviceWithCredentials(device, exportData.getCredentials());
        } else {
            return deviceService.saveDevice(device);
        }
    }

    @Override
    protected void onEntitySaved(SecurityUser user, Device savedDevice, Device oldDevice) throws ThingsboardException {
        super.onEntitySaved(user, savedDevice, oldDevice);
        clusterService.onDeviceUpdated(savedDevice, oldDevice);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}
