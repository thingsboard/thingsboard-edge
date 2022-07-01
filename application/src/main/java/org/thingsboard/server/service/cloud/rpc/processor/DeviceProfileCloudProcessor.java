/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.gen.edge.v1.DeviceProfileDevicesRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;

import java.nio.charset.StandardCharsets;
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
                    if (deviceProfile == null) {
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
                    deviceProfile.setProvisionDeviceKey(deviceProfileUpdateMsg.hasProvisionDeviceKey() ? deviceProfileUpdateMsg.getProvisionDeviceKey() : null);
                    Optional<DeviceProfileData> profileDataOpt =
                            dataDecodingEncodingService.decode(deviceProfileUpdateMsg.getProfileDataBytes().toByteArray());
                    if (profileDataOpt.isPresent()) {
                        deviceProfile.setProfileData(profileDataOpt.get());
                    }
                    if (deviceProfileUpdateMsg.getDefaultRuleChainIdMSB() != 0 &&
                            deviceProfileUpdateMsg.getDefaultRuleChainIdLSB() != 0) {
                        RuleChainId defaultRuleChainId = new RuleChainId(
                                new UUID(deviceProfileUpdateMsg.getDefaultRuleChainIdMSB(), deviceProfileUpdateMsg.getDefaultRuleChainIdLSB()));
                        deviceProfile.setDefaultRuleChainId(defaultRuleChainId);
                    }
                    if (deviceProfileUpdateMsg.getDefaultQueueIdMSB() != 0 &&
                            deviceProfileUpdateMsg.getDefaultQueueIdLSB() != 0) {
                        QueueId defaultQueueId = new QueueId(
                                new UUID(deviceProfileUpdateMsg.getDefaultQueueIdMSB(), deviceProfileUpdateMsg.getDefaultQueueIdLSB()));
                        deviceProfile.setDefaultQueueId(defaultQueueId);
                    }
                    deviceProfileService.saveDeviceProfile(deviceProfile, false);

                    return saveCloudEvent(tenantId, CloudEventType.DEVICE_PROFILE, EdgeEventActionType.DEVICE_PROFILE_DEVICES_REQUEST, deviceProfileId, null);
                } finally {
                    deviceCreationLock.unlock();
                }
            case ENTITY_DELETED_RPC_MESSAGE:
                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
                if (deviceProfile != null) {
                    deviceProfileService.deleteDeviceProfile(tenantId, deviceProfileId);
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
            default:
                return handleUnsupportedMsgType(deviceProfileUpdateMsg.getMsgType());
        }
    }

    public UplinkMsg processDeviceProfileDevicesRequestMsgToCloud(CloudEvent cloudEvent) {
        EntityId deviceProfileId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        DeviceProfileDevicesRequestMsg deviceProfileDevicesRequestMsg = DeviceProfileDevicesRequestMsg.newBuilder()
                .setDeviceProfileIdMSB(deviceProfileId.getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(deviceProfileId.getId().getLeastSignificantBits())
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceProfileDevicesRequestMsg(deviceProfileDevicesRequestMsg);
        return builder.build();
    }
}
