/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
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
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class DeviceProfileCloudProcessor extends BaseEdgeProcessor {

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
                            deviceProfileName = deviceProfileName + "_" + StringUtils.randomAlphabetic(15);
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
