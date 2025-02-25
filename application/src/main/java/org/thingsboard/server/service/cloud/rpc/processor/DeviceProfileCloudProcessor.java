/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
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
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.BaseDeviceProfileProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class DeviceProfileCloudProcessor extends BaseDeviceProfileProcessor {

    public ListenableFuture<Void> processDeviceProfileMsgFromCloud(TenantId tenantId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(deviceProfileUpdateMsg.getIdMSB(), deviceProfileUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (deviceProfileUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    deviceCreationLock.lock();
                    try {
                        DeviceProfile deviceProfileMsg = JacksonUtil.fromString(deviceProfileUpdateMsg.getEntity(), DeviceProfile.class, true);
                        if (deviceProfileMsg == null) {
                            throw new RuntimeException("[{" + tenantId + "}] deviceProfileUpdateMsg {" + deviceProfileUpdateMsg + "} cannot be converted to device profile");
                        }
                        DeviceProfile deviceProfileByName = edgeCtx.getDeviceProfileService().findDeviceProfileByName(tenantId, deviceProfileMsg.getName());
                        boolean removePreviousProfile = false;
                        if (deviceProfileByName != null && !deviceProfileByName.getId().equals(deviceProfileId)) {
                            renameExistingOnEdgeDeviceProfile(deviceProfileByName);
                            removePreviousProfile = true;
                        }
                        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateDeviceProfile(tenantId, deviceProfileId, deviceProfileUpdateMsg);
                        boolean created = resultPair.getFirst();
                        DeviceProfile deviceProfile = edgeCtx.getDeviceProfileService().findDeviceProfileById(tenantId, deviceProfileId);
                        if (!deviceProfile.isDefault() && deviceProfileMsg.isDefault()) {
                            edgeCtx.getDeviceProfileService().setDefaultDeviceProfile(tenantId, deviceProfileId);
                        }
                        if (removePreviousProfile) {
                            updateDevices(tenantId, deviceProfileId, deviceProfileByName.getId());
                            edgeCtx.getDeviceProfileService().deleteDeviceProfile(tenantId, deviceProfileByName.getId());
                        }
                        if (created) {
                            pushDeviceProfileCreatedEventToRuleEngine(tenantId, deviceProfileId);
                        }
                    } finally {
                        deviceCreationLock.unlock();
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    DeviceProfile deviceProfileToDelete = edgeCtx.getDeviceProfileService().findDeviceProfileById(tenantId, deviceProfileId);
                    if (deviceProfileToDelete != null) {
                        edgeCtx.getDeviceProfileService().deleteDeviceProfile(tenantId, deviceProfileId);
                        pushDeviceProfileDeletedEventToRuleEngine(tenantId, deviceProfileToDelete);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(deviceProfileUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    private void pushDeviceProfileCreatedEventToRuleEngine(TenantId tenantId, DeviceProfileId deviceProfileId) {
        DeviceProfile deviceProfile = edgeCtx.getDeviceProfileService().findDeviceProfileById(tenantId, deviceProfileId);
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

    private void updateDevices(TenantId tenantId, DeviceProfileId newDeviceProfileId, DeviceProfileId previousDeviceProfileId) {
        PageDataIterable<DeviceInfo> deviceInfosIterable = new PageDataIterable<>(
                link -> edgeCtx.getDeviceService().findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).deviceProfileId(previousDeviceProfileId).build(), link), 1024);
        deviceInfosIterable.forEach(deviceInfo -> {
            deviceInfo.setDeviceProfileId(newDeviceProfileId);
            edgeCtx.getDeviceService().saveDevice(new Device(deviceInfo));
        });
    }

    private void renameExistingOnEdgeDeviceProfile(DeviceProfile deviceProfileByName) {
        deviceProfileByName.setName(deviceProfileByName.getName() + StringUtils.randomAlphanumeric(15));
        edgeCtx.getDeviceProfileService().saveDeviceProfile(deviceProfileByName);
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                DeviceProfile deviceProfile = edgeCtx.getDeviceProfileService().findDeviceProfileById(cloudEvent.getTenantId(), deviceProfileId);
                if (deviceProfile != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    DeviceProfileUpdateMsg deviceProfileUpdateMsg = EdgeMsgConstructorUtils.constructDeviceProfileUpdatedMsg(msgType, deviceProfile);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceProfileUpdateMsg(deviceProfileUpdateMsg).build();
                } else {
                    log.info("Skipping event as device profile was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                DeviceProfileUpdateMsg deviceProfileUpdateMsg = EdgeMsgConstructorUtils.constructDeviceProfileDeleteMsg(deviceProfileId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceProfileUpdateMsg(deviceProfileUpdateMsg).build();
            }
        }
        return null;
    }

    @Override
    protected void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, RuleChainId ruleChainId) {
        RuleChainId defaultRuleChainId = deviceProfile.getDefaultEdgeRuleChainId();
        RuleChain ruleChain = null;
        if (defaultRuleChainId != null) {
            ruleChain = edgeCtx.getRuleChainService().findRuleChainById(tenantId, defaultRuleChainId);
        }
        deviceProfile.setDefaultRuleChainId(ruleChain != null ? ruleChain.getId() : null);
    }

    @Override
    protected void setDefaultEdgeRuleChainId(DeviceProfile deviceProfile, RuleChainId ruleChainId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        deviceProfile.setDefaultEdgeRuleChainId(null);
    }

    @Override
    protected void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        DashboardId defaultDashboardId = deviceProfile.getDefaultDashboardId();
        DashboardInfo dashboard = null;
        if (defaultDashboardId != null) {
            dashboard = edgeCtx.getDashboardService().findDashboardInfoById(tenantId, defaultDashboardId);
        }
        deviceProfile.setDefaultDashboardId(dashboard != null ? dashboard.getId() : null);
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.DEVICE_PROFILE;
    }

}
