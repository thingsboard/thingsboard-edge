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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Objects;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceProfileImportService extends BaseEntityImportService<DeviceProfileId, DeviceProfile, EntityExportData<DeviceProfile>> {

    private final DeviceProfileService deviceProfileService;
    private final OtaPackageStateService otaPackageStateService;

    @Override
    protected void setOwner(TenantId tenantId, DeviceProfile deviceProfile, IdProvider idProvider) {
        deviceProfile.setTenantId(tenantId);
    }

    @Override
    protected DeviceProfile prepareAndSave(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfile old, EntityExportData<DeviceProfile> exportData, IdProvider idProvider, EntityImportSettings importSettings) {
        deviceProfile.setDefaultRuleChainId(idProvider.getInternalId(deviceProfile.getDefaultRuleChainId()));
        deviceProfile.setDefaultDashboardId(idProvider.getInternalId(deviceProfile.getDefaultDashboardId()));
        deviceProfile.setFirmwareId(idProvider.getInternalId(deviceProfile.getFirmwareId()));
        deviceProfile.setSoftwareId(idProvider.getInternalId(deviceProfile.getSoftwareId()));
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    @Override
    protected void onEntitySaved(SecurityUser user, DeviceProfile savedDeviceProfile, DeviceProfile oldDeviceProfile) throws ThingsboardException {
        super.onEntitySaved(user, savedDeviceProfile, oldDeviceProfile);
        clusterService.onDeviceProfileChange(savedDeviceProfile, null);
        clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedDeviceProfile.getId(),
                oldDeviceProfile == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        entityActionService.sendEntityNotificationMsgToEdgeService(user.getTenantId(), savedDeviceProfile.getId(),
                oldDeviceProfile == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);
        otaPackageStateService.update(savedDeviceProfile,
                oldDeviceProfile != null && !Objects.equals(oldDeviceProfile.getFirmwareId(), savedDeviceProfile.getFirmwareId()),
                oldDeviceProfile != null && !Objects.equals(oldDeviceProfile.getSoftwareId(), savedDeviceProfile.getSoftwareId()));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE_PROFILE;
    }

}
