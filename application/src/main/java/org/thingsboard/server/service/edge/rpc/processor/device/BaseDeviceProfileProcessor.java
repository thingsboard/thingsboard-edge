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
package org.thingsboard.server.service.edge.rpc.processor.device;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public abstract class BaseDeviceProfileProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    protected Pair<Boolean, Boolean> saveOrUpdateDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        boolean created = false;
        boolean deviceProfileNameUpdated = false;
        deviceCreationLock.lock();
        try {
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
            String deviceProfileName = deviceProfileUpdateMsg.getName();
            if (deviceProfile == null) {
                created = true;
                deviceProfile = new DeviceProfile();
                deviceProfile.setTenantId(tenantId);
                deviceProfile.setCreatedTime(Uuids.unixTimestamp(deviceProfileId.getId()));
            }
            DeviceProfile deviceProfileByName = deviceProfileService.findDeviceProfileByName(tenantId, deviceProfileName);
            if (deviceProfileByName != null && !deviceProfileByName.getId().equals(deviceProfileId)) {
                deviceProfileName = deviceProfileName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] Device profile with name {} already exists. Renaming device profile name to {}",
                        tenantId, deviceProfileUpdateMsg.getName(), deviceProfileName);
                deviceProfileNameUpdated = true;
            }
            deviceProfile.setName(deviceProfileName);
            deviceProfile.setDescription(deviceProfileUpdateMsg.hasDescription() ? deviceProfileUpdateMsg.getDescription() : null);
            deviceProfile.setType(DeviceProfileType.valueOf(deviceProfileUpdateMsg.getType()));
            deviceProfile.setTransportType(deviceProfileUpdateMsg.hasTransportType()
                    ? DeviceTransportType.valueOf(deviceProfileUpdateMsg.getTransportType()) : DeviceTransportType.DEFAULT);
            deviceProfile.setImage(deviceProfileUpdateMsg.hasImage()
                    ? new String(deviceProfileUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);
            deviceProfile.setProvisionType(deviceProfileUpdateMsg.hasProvisionType()
                    ? DeviceProfileProvisionType.valueOf(deviceProfileUpdateMsg.getProvisionType()) : DeviceProfileProvisionType.DISABLED);
            deviceProfile.setProvisionDeviceKey(deviceProfileUpdateMsg.hasProvisionDeviceKey()
                    ? deviceProfileUpdateMsg.getProvisionDeviceKey() : null);
            deviceProfile.setDefaultQueueName(deviceProfileUpdateMsg.getDefaultQueueName());

            Optional<DeviceProfileData> profileDataOpt =
                    dataDecodingEncodingService.decode(deviceProfileUpdateMsg.getProfileDataBytes().toByteArray());
            deviceProfile.setProfileData(profileDataOpt.orElse(null));

            setDefaultRuleChainId(tenantId, deviceProfile, deviceProfileUpdateMsg);
            setDefaultEdgeRuleChainId(tenantId, deviceProfile, deviceProfileUpdateMsg);
            setDefaultDashboardId(tenantId, deviceProfile, deviceProfileUpdateMsg);

            String defaultQueueName = StringUtils.isNotBlank(deviceProfileUpdateMsg.getDefaultQueueName())
                    ? deviceProfileUpdateMsg.getDefaultQueueName() : null;
            deviceProfile.setDefaultQueueName(defaultQueueName);

            UUID firmwareUUID = safeGetUUID(deviceProfileUpdateMsg.getFirmwareIdMSB(), deviceProfileUpdateMsg.getFirmwareIdLSB());
            deviceProfile.setFirmwareId(firmwareUUID != null ? new OtaPackageId(firmwareUUID) : null);

            UUID softwareUUID = safeGetUUID(deviceProfileUpdateMsg.getSoftwareIdMSB(), deviceProfileUpdateMsg.getSoftwareIdLSB());
            deviceProfile.setSoftwareId(softwareUUID != null ? new OtaPackageId(softwareUUID) : null);

            deviceProfileValidator.validate(deviceProfile, DeviceProfile::getTenantId);
            if (created) {
                deviceProfile.setId(deviceProfileId);
            }
            deviceProfileService.saveDeviceProfile(deviceProfile, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process device profile update msg [{}]", tenantId, deviceProfileUpdateMsg, e);
            throw e;
        }  finally {
            deviceCreationLock.unlock();
        }
        return Pair.of(created, deviceProfileNameUpdated);
    }

    protected abstract void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

    protected abstract void setDefaultEdgeRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg);

    protected abstract void setDefaultDashboardId(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg);
}
