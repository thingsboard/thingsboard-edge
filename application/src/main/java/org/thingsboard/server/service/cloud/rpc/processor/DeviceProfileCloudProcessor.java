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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class DeviceProfileCloudProcessor extends BaseCloudProcessor {

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    public ListenableFuture<Void> processDeviceProfileMsgFromCloud(TenantId tenantId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(deviceProfileUpdateMsg.getIdMSB(), deviceProfileUpdateMsg.getIdLSB()));
        switch (deviceProfileUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                deviceCreationLock.lock();
                try {
                    DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
                    String deviceProfileName = deviceProfileUpdateMsg.getName();
                    boolean created = false;
                    if (deviceProfile == null) {
                        created = true;
                        deviceProfile = new DeviceProfile();
                        deviceProfile.setId(deviceProfileId);
                        deviceProfile.setCreatedTime(Uuids.unixTimestamp(deviceProfileId.getId()));
                        deviceProfile.setTenantId(tenantId);
                        DeviceProfile deviceProfileByName = deviceProfileService.findDeviceProfileByName(tenantId, deviceProfileName);
                        if (deviceProfileByName != null) {
                            deviceProfileName = deviceProfileName + "_" + RandomStringUtils.randomAlphabetic(15);
                            log.warn("Device profile with name {} already exists on the edge. Renaming device profile name to {}",
                                    deviceProfileUpdateMsg.getName(), deviceProfileName);
                        }
                    }
                    deviceProfile.setName(deviceProfileName);
                    deviceProfile.setDescription(deviceProfileUpdateMsg.hasDescription() ? deviceProfileUpdateMsg.getDescription() : null);
                    deviceProfile.setDefault(deviceProfileUpdateMsg.getDefault());
                    deviceProfile.setType(DeviceProfileType.valueOf(deviceProfileUpdateMsg.getType()));
                    deviceProfile.setTransportType(deviceProfileUpdateMsg.hasTransportType()
                            ? DeviceTransportType.valueOf(deviceProfileUpdateMsg.getTransportType()) : DeviceTransportType.DEFAULT);
                    deviceProfile.setImage(deviceProfileUpdateMsg.hasImage()
                            ? new String(deviceProfileUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);
                    deviceProfile.setProvisionType(deviceProfileUpdateMsg.hasProvisionType()
                            ? DeviceProfileProvisionType.valueOf(deviceProfileUpdateMsg.getProvisionType()) : DeviceProfileProvisionType.DISABLED);
                    deviceProfile.setProvisionDeviceKey(deviceProfileUpdateMsg.hasProvisionDeviceKey()
                            ? deviceProfileUpdateMsg.getProvisionDeviceKey() : null);

                    Optional<DeviceProfileData> profileDataOpt =
                            dataDecodingEncodingService.decode(deviceProfileUpdateMsg.getProfileDataBytes().toByteArray());
                    deviceProfile.setProfileData(profileDataOpt.orElse(null));

                    UUID defaultRuleChainUUID = safeGetUUID(deviceProfileUpdateMsg.getDefaultRuleChainIdMSB(), deviceProfileUpdateMsg.getDefaultRuleChainIdLSB());
                    deviceProfile.setDefaultRuleChainId(defaultRuleChainUUID != null ? new RuleChainId(defaultRuleChainUUID) : null);

                    UUID defaultDashboardUUID = safeGetUUID(deviceProfileUpdateMsg.getDefaultDashboardIdMSB(), deviceProfileUpdateMsg.getDefaultDashboardIdLSB());
                    deviceProfile.setDefaultDashboardId(defaultDashboardUUID != null ? new DashboardId(defaultDashboardUUID) : null);

                    String defaultQueueName = StringUtils.isNotBlank(deviceProfileUpdateMsg.getDefaultQueueName())
                            ? deviceProfileUpdateMsg.getDefaultQueueName() : null;
                    deviceProfile.setDefaultQueueName(defaultQueueName);

                    UUID firmwareUUID = safeGetUUID(deviceProfileUpdateMsg.getFirmwareIdMSB(), deviceProfileUpdateMsg.getFirmwareIdLSB());
                    deviceProfile.setFirmwareId(firmwareUUID != null ? new OtaPackageId(firmwareUUID) : null);

                    UUID softwareUUID = safeGetUUID(deviceProfileUpdateMsg.getSoftwareIdMSB(), deviceProfileUpdateMsg.getSoftwareIdLSB());
                    deviceProfile.setSoftwareId(softwareUUID != null ? new OtaPackageId(softwareUUID) : null);

                    DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile, false);

                    // TODO: @voba - move this part to device profile notification service
                    notifyCluster(tenantId, deviceProfile, created, savedDeviceProfile);

                    break;
                } finally {
                    deviceCreationLock.unlock();
                }
            case ENTITY_DELETED_RPC_MESSAGE:
                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
                if (deviceProfile != null) {
                    deviceProfileService.deleteDeviceProfile(tenantId, deviceProfileId);
                    tbClusterService.onDeviceProfileDelete(deviceProfile, null);
                    tbClusterService.broadcastEntityStateChangeEvent(tenantId, deviceProfileId, ComponentLifecycleEvent.DELETED);
                }
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(deviceProfileUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }

    private void notifyCluster(TenantId tenantId, DeviceProfile deviceProfile, boolean created, DeviceProfile savedDeviceProfile) {
        boolean isFirmwareChanged = false;
        boolean isSoftwareChanged = false;
        if (!created) {
            DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfile.getId());
            if (!Objects.equals(deviceProfile.getFirmwareId(), oldDeviceProfile.getFirmwareId())) {
                isFirmwareChanged = true;
            }
            if (!Objects.equals(deviceProfile.getSoftwareId(), oldDeviceProfile.getSoftwareId())) {
                isSoftwareChanged = true;
            }
        }
        tbClusterService.onDeviceProfileChange(savedDeviceProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedDeviceProfile.getId(),
                created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        otaPackageStateService.update(savedDeviceProfile, isFirmwareChanged, isSoftwareChanged);
    }

}
