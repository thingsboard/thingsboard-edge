/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.processor.device.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseDeviceProfileProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<DeviceProfile> deviceProfileValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        boolean created = false;
        boolean deviceProfileNameUpdated = false;
        deviceCreationLock.lock();
        try {
            DeviceProfile deviceProfile = constructDeviceProfileFromUpdateMsg(tenantId, deviceProfileId, deviceProfileUpdateMsg);
            if (deviceProfile == null) {
                throw new RuntimeException("[{" + tenantId + "}] deviceProfileUpdateMsg {" + deviceProfileUpdateMsg + "} cannot be converted to device profile");
            }
            DeviceProfile deviceProfileById = edgeCtx.getDeviceProfileService().findDeviceProfileById(tenantId, deviceProfileId);
            if (deviceProfileById == null) {
                created = true;
                deviceProfile.setId(null);
                deviceProfile.setDefault(false);
            } else {
                deviceProfile.setId(deviceProfileId);
                deviceProfile.setDefault(deviceProfileById.isDefault());
            }
            String deviceProfileName = deviceProfile.getName();
            DeviceProfile deviceProfileByName = edgeCtx.getDeviceProfileService().findDeviceProfileByName(tenantId, deviceProfileName);
            if (deviceProfileByName != null && !deviceProfileByName.getId().equals(deviceProfileId)) {
                deviceProfileName = deviceProfileName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] Device profile with name {} already exists. Renaming device profile name to {}",
                        tenantId, deviceProfile.getName(), deviceProfileName);
                deviceProfileNameUpdated = true;
            }
            deviceProfile.setName(deviceProfileName);

            RuleChainId ruleChainId = deviceProfile.getDefaultRuleChainId();
            setDefaultRuleChainId(tenantId, deviceProfile, created ? null : deviceProfileById.getDefaultRuleChainId());
            setDefaultEdgeRuleChainId(deviceProfile, ruleChainId, deviceProfileUpdateMsg);
            setDefaultDashboardId(tenantId, created ? null : deviceProfileById.getDefaultDashboardId(), deviceProfile, deviceProfileUpdateMsg);

            // edge-only: fixed issue where assigned ota packages in device profile is not found in DB during reconnect
            unassignOtaPackageIfNotExist(tenantId, deviceProfile, OtaPackageType.FIRMWARE);
            unassignOtaPackageIfNotExist(tenantId, deviceProfile, OtaPackageType.SOFTWARE);
            //

            deviceProfileValidator.validate(deviceProfile, DeviceProfile::getTenantId);
            if (created) {
                deviceProfile.setId(deviceProfileId);
            }
            edgeCtx.getDeviceProfileService().saveDeviceProfile(deviceProfile, false, true);
        } catch (Exception e) {
            log.error("[{}] Failed to process device profile update msg [{}]", tenantId, deviceProfileUpdateMsg, e);
            throw e;
        } finally {
            deviceCreationLock.unlock();
        }
        return Pair.of(created, deviceProfileNameUpdated);
    }

    private void unassignOtaPackageIfNotExist(TenantId tenantId, DeviceProfile deviceProfile, OtaPackageType otaPackageType) {
        OtaPackageId otaPackageId = (OtaPackageType.FIRMWARE == otaPackageType) ? deviceProfile.getFirmwareId() : deviceProfile.getSoftwareId();

        if (otaPackageId != null) {
            OtaPackageInfo otaPackageInfo = edgeCtx.getOtaPackageService().findOtaPackageInfoById(tenantId, otaPackageId);
            if (otaPackageInfo == null) {
                if (OtaPackageType.FIRMWARE == otaPackageType) {
                    deviceProfile.setFirmwareId(null);
                } else {
                    deviceProfile.setSoftwareId(null);
                }
                log.warn("OtaPackage with ID {} not found in DB, unassigned from device profile with id {}", otaPackageId, deviceProfile.getId());
            }
        }
    }

    protected abstract DeviceProfile constructDeviceProfileFromUpdateMsg(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

    protected abstract void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, RuleChainId ruleChainId);

    protected abstract void setDefaultEdgeRuleChainId(DeviceProfile deviceProfile, RuleChainId ruleChainId, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

    protected abstract void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

}
