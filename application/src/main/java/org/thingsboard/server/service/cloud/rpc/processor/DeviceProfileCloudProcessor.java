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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.device.BaseDeviceProfileProcessor;

import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class DeviceProfileCloudProcessor extends BaseDeviceProfileProcessor {

    @Autowired
    private DeviceProfileService deviceProfileService;

    public ListenableFuture<Void> processDeviceProfileMsgFromCloud(TenantId tenantId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(deviceProfileUpdateMsg.getIdMSB(), deviceProfileUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getSync().set(true);

            switch (deviceProfileUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    deviceCreationLock.lock();
                    try {
                        DeviceProfile deviceProfileMsg = JacksonUtil.fromEdgeString(deviceProfileUpdateMsg.getEntity(), DeviceProfile.class);
                        if (deviceProfileMsg == null) {
                            throw new RuntimeException("[{" + tenantId + "}] deviceProfileUpdateMsg {" + deviceProfileUpdateMsg + "} cannot be converted to device profile");
                        }
                        DeviceProfile deviceProfileByName = deviceProfileService.findDeviceProfileByName(tenantId, deviceProfileMsg.getName());
                        boolean removePreviousProfile = false;
                        if (deviceProfileByName != null && !deviceProfileByName.getId().equals(deviceProfileId)) {
                            renameExistingOnEdgeDeviceProfile(deviceProfileByName);
                            removePreviousProfile = true;
                        }
                        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateDeviceProfile(tenantId, deviceProfileId, deviceProfileUpdateMsg, false);
                        boolean created = resultPair.getFirst();
                        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
                        if (!deviceProfile.isDefault() && deviceProfileMsg.isDefault()) {
                            deviceProfileService.setDefaultDeviceProfile(tenantId, deviceProfileId);
                        }
                        if (removePreviousProfile) {
                            updateDevices(tenantId, deviceProfileId, deviceProfileByName.getId());
                            deviceProfileService.deleteDeviceProfile(tenantId, deviceProfileByName.getId());
                        }
                        if (created) {
                            pushDeviceProfileCreatedEventToRuleEngine(tenantId, deviceProfileId);
                        }
                        notifyCluster(tenantId, deviceProfile, created);
                    } finally {
                        deviceCreationLock.unlock();
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    DeviceProfile deviceProfileToDelete = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
                    if (deviceProfileToDelete != null) {
                        deviceProfileService.deleteDeviceProfile(tenantId, deviceProfileId);
                        tbClusterService.onDeviceProfileDelete(deviceProfileToDelete, null);
                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, deviceProfileId, ComponentLifecycleEvent.DELETED);
                        pushDeviceProfileDeletedEventToRuleEngine(tenantId, deviceProfileToDelete);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(deviceProfileUpdateMsg.getMsgType());
            }
        } finally {
            edgeSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    private void pushDeviceProfileCreatedEventToRuleEngine(TenantId tenantId, DeviceProfileId deviceProfileId) {
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
        pushDeviceProfileEventToRuleEngine(tenantId, deviceProfile, TbMsgType.ENTITY_CREATED);
    }

    private void pushDeviceProfileDeletedEventToRuleEngine(TenantId tenantId, DeviceProfile deviceProfile) {
        pushDeviceProfileEventToRuleEngine(tenantId, deviceProfile, TbMsgType.ENTITY_DELETED);
    }

    private void pushDeviceProfileEventToRuleEngine(TenantId tenantId, DeviceProfile deviceProfile, TbMsgType msgType) {
        try {
            String deviceProfileAsString = JacksonUtil.toString(deviceProfile);
            pushEntityEventToRuleEngine(tenantId, deviceProfile.getId(), null, msgType, deviceProfileAsString, new TbMsgMetaData());
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push device action to rule engine: {}", tenantId, deviceProfile.getId(), msgType.name(), e);
        }
    }

    private void notifyCluster(TenantId tenantId, DeviceProfile deviceProfile, boolean created) {
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
        tbClusterService.onDeviceProfileChange(deviceProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(tenantId, deviceProfile.getId(),
                created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        otaPackageStateService.update(deviceProfile, isFirmwareChanged, isSoftwareChanged);
    }

    private void updateDevices(TenantId tenantId, DeviceProfileId newDeviceProfileId, DeviceProfileId previousDeviceProfileId) {
        PageDataIterable<DeviceInfo> deviceInfosIterable = new PageDataIterable<>(
                link -> deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).deviceProfileId(previousDeviceProfileId).build(), link), 1024);
        deviceInfosIterable.forEach(deviceInfo -> {
            deviceInfo.setDeviceProfileId(newDeviceProfileId);
            deviceService.saveDevice(new Device(deviceInfo));
        });
    }

    private void renameExistingOnEdgeDeviceProfile(DeviceProfile deviceProfileByName) {
        deviceProfileByName.setName(deviceProfileByName.getName() + StringUtils.randomAlphanumeric(15));
        deviceProfileService.saveDeviceProfile(deviceProfileByName);
    }

    public UplinkMsg convertDeviceProfileEventToUplink(CloudEvent cloudEvent, EdgeVersion edgeVersion) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(cloudEvent.getEntityId());
        UplinkMsg msg = null;
        switch (cloudEvent.getAction()) {
            case ADDED:
            case UPDATED:
                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(cloudEvent.getTenantId(), deviceProfileId);
                if (deviceProfile != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    DeviceProfileUpdateMsg deviceProfileUpdateMsg =
                            deviceProfileMsgConstructor.constructDeviceProfileUpdatedMsg(msgType, deviceProfile, edgeVersion);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceProfileUpdateMsg(deviceProfileUpdateMsg).build();
                } else {
                    log.info("Skipping event as device profile was not found [{}]", cloudEvent);
                }
                break;
            case DELETED:
                DeviceProfileUpdateMsg deviceProfileUpdateMsg =
                        deviceProfileMsgConstructor.constructDeviceProfileDeleteMsg(deviceProfileId);
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceProfileUpdateMsg(deviceProfileUpdateMsg).build();
                break;
        }
        return msg;
    }

    @Override
    protected void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile) {
        RuleChainId defaultRuleChainId = deviceProfile.getDefaultEdgeRuleChainId();
        RuleChain ruleChain = null;
        if (defaultRuleChainId != null) {
            ruleChain = ruleChainService.findRuleChainById(tenantId, defaultRuleChainId);
        }
        deviceProfile.setDefaultRuleChainId(ruleChain != null ? ruleChain.getId() : null);
    }

    @Override
    protected void setDefaultEdgeRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, RuleChainId ruleChainId, DeviceProfileUpdateMsg deviceProfileUpdateMsg, boolean isEdgeVersionDeprecated) {
        deviceProfile.setDefaultEdgeRuleChainId(null);
    }

    @Override
    protected void setDefaultDashboardId(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg, boolean isEdgeVersionDeprecated) {
        DashboardId defaultDashboardId = deviceProfile.getDefaultDashboardId();
        DashboardInfo dashboard = null;
        if (defaultDashboardId != null) {
            dashboard = dashboardService.findDashboardInfoById(tenantId, defaultDashboardId);
        }
        deviceProfile.setDefaultDashboardId(dashboard != null ? dashboard.getId() : null);
    }
}
