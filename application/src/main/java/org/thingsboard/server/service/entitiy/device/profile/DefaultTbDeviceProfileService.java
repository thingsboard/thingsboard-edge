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
package org.thingsboard.server.service.entitiy.device.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.ota.OtaPackageStateService;

import java.util.Objects;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbDeviceProfileService extends AbstractTbEntityService implements TbDeviceProfileService {
    private final DeviceProfileService deviceProfileService;
    private final OtaPackageStateService otaPackageStateService;

    @Override
    public DeviceProfile save(DeviceProfile deviceProfile, User user) throws Exception {
        ActionType actionType = deviceProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = deviceProfile.getTenantId();
        try {
            boolean isFirmwareChanged = false;
            boolean isSoftwareChanged = false;

            if (actionType.equals(ActionType.UPDATED)) {
                DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfile.getId());
                if (!Objects.equals(deviceProfile.getFirmwareId(), oldDeviceProfile.getFirmwareId())) {
                    isFirmwareChanged = true;
                }
                if (!Objects.equals(deviceProfile.getSoftwareId(), oldDeviceProfile.getSoftwareId())) {
                    isSoftwareChanged = true;
                }
            }
            DeviceProfile savedDeviceProfile = checkNotNull(deviceProfileService.saveDeviceProfile(deviceProfile));
            autoCommit(user, savedDeviceProfile.getId());
            tbClusterService.onDeviceProfileChange(savedDeviceProfile, null);
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedDeviceProfile.getId(),
                    actionType.equals(ActionType.ADDED) ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            otaPackageStateService.update(savedDeviceProfile, isFirmwareChanged, isSoftwareChanged);

            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedDeviceProfile.getId(),
                    savedDeviceProfile, user, actionType, true, false, null);
            return savedDeviceProfile;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), deviceProfile, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(DeviceProfile deviceProfile, User user) throws ThingsboardException {
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        TenantId tenantId = deviceProfile.getTenantId();
        try {
            deviceProfileService.deleteDeviceProfile(tenantId, deviceProfileId);

            tbClusterService.onDeviceProfileDelete(deviceProfile, null);
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, deviceProfileId, ComponentLifecycleEvent.DELETED);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, deviceProfileId, deviceProfile,
                    user, ActionType.DELETED, true, false, null, deviceProfileId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), ActionType.DELETED,
                    user, e, deviceProfileId.toString());
            throw e;
        }
    }

    @Override
    public DeviceProfile setDefaultDeviceProfile(DeviceProfile deviceProfile, DeviceProfile previousDefaultDeviceProfile, User user) throws ThingsboardException {
        TenantId tenantId = deviceProfile.getTenantId();
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        try {
            if (deviceProfileService.setDefaultDeviceProfile(tenantId, deviceProfileId)) {
                if (previousDefaultDeviceProfile != null) {
                    previousDefaultDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, previousDefaultDeviceProfile.getId());
                    notificationEntityService.logEntityAction(tenantId, previousDefaultDeviceProfile.getId(), previousDefaultDeviceProfile,
                            ActionType.UPDATED, user);
                }
                deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);

                notificationEntityService.logEntityAction(tenantId, deviceProfileId, deviceProfile, ActionType.UPDATED, user);
            }
            return deviceProfile;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), ActionType.UPDATED,
                    user, e, deviceProfileId.toString());
            throw e;
        }
    }
}
