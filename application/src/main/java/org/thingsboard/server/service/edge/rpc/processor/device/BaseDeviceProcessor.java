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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.Optional;
import java.util.UUID;

@Slf4j
public abstract class BaseDeviceProcessor extends BaseEdgeProcessor {

    @Autowired
    protected DataDecodingEncodingService dataDecodingEncodingService;

    protected Pair<Boolean, Boolean> saveOrUpdateDevice(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg, CustomerId customerId) throws ThingsboardException {
        boolean created = false;
        boolean deviceNameUpdated = false;
        deviceCreationLock.lock();
        try {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            String deviceName = deviceUpdateMsg.getName();
            if (device == null) {
                created = true;
                device = new Device();
                device.setTenantId(tenantId);
                device.setCreatedTime(Uuids.unixTimestamp(deviceId.getId()));
                Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
                if (deviceByName != null) {
                    deviceName = deviceName + "_" + StringUtils.randomAlphabetic(15);
                    log.warn("Device with name {} already exists. Renaming device name to {}",
                            deviceUpdateMsg.getName(), deviceName);
                    deviceNameUpdated = true;
                }
            } else {
                changeOwnerIfRequired(tenantId, customerId, deviceId);
            }
            device.setName(deviceName);
            device.setType(deviceUpdateMsg.getType());
            device.setLabel(deviceUpdateMsg.hasLabel() ? deviceUpdateMsg.getLabel() : null);
            device.setAdditionalInfo(deviceUpdateMsg.hasAdditionalInfo()
                    ? JacksonUtil.toJsonNode(deviceUpdateMsg.getAdditionalInfo()) : null);

            UUID deviceProfileUUID = safeGetUUID(deviceUpdateMsg.getDeviceProfileIdMSB(), deviceUpdateMsg.getDeviceProfileIdLSB());
            device.setDeviceProfileId(deviceProfileUUID != null ? new DeviceProfileId(deviceProfileUUID) : null);

            device.setCustomerId(customerId);

            Optional<DeviceData> deviceDataOpt =
                    dataDecodingEncodingService.decode(deviceUpdateMsg.getDeviceDataBytes().toByteArray());
            device.setDeviceData(deviceDataOpt.orElse(null));

            UUID firmwareUUID = safeGetUUID(deviceUpdateMsg.getFirmwareIdMSB(), deviceUpdateMsg.getFirmwareIdLSB());
            device.setFirmwareId(firmwareUUID != null ? new OtaPackageId(firmwareUUID) : null);

            UUID softwareUUID = safeGetUUID(deviceUpdateMsg.getSoftwareIdMSB(), deviceUpdateMsg.getSoftwareIdLSB());
            device.setSoftwareId(softwareUUID != null ? new OtaPackageId(softwareUUID) : null);
            deviceValidator.validate(device, Device::getTenantId);
            if (created) {
                device.setId(deviceId);
            }
            Device savedDevice = deviceService.saveDevice(device, false);
            if (created) {
                DeviceCredentials deviceCredentials = new DeviceCredentials();
                deviceCredentials.setDeviceId(new DeviceId(savedDevice.getUuidId()));
                deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
                deviceCredentials.setCredentialsId(StringUtils.randomAlphanumeric(20));
                deviceCredentialsService.createDeviceCredentials(device.getTenantId(), deviceCredentials);
                entityGroupService.addEntityToEntityGroupAll(savedDevice.getTenantId(), savedDevice.getOwnerId(), savedDevice.getId());
            }
            safeAddToEntityGroup(tenantId, deviceUpdateMsg, deviceId);
            tbClusterService.onDeviceUpdated(savedDevice, created ? null : device, false, false);
        } finally {
            deviceCreationLock.unlock();
        }
        return Pair.of(created, deviceNameUpdated);
    }

    private void safeAddToEntityGroup(TenantId tenantId, DeviceUpdateMsg deviceUpdateMsg, DeviceId deviceId) {
        if (deviceUpdateMsg.hasEntityGroupIdMSB() && deviceUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(deviceUpdateMsg.getEntityGroupIdMSB(),
                    deviceUpdateMsg.getEntityGroupIdLSB());
            safeAddEntityToGroup(tenantId, new EntityGroupId(entityGroupUUID), deviceId);
        }
    }

    public ListenableFuture<Void> processDeviceCredentialsMsg(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        log.debug("[{}] Executing processDeviceCredentialsMsg, deviceCredentialsUpdateMsg [{}]", tenantId, deviceCredentialsUpdateMsg);
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsUpdateMsg.getDeviceIdMSB(), deviceCredentialsUpdateMsg.getDeviceIdLSB()));
        ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(tenantId, deviceId);
        return Futures.transform(deviceFuture, device -> {
            if (device != null) {
                log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                        device.getName(), deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentialsUpdateMsg.getCredentialsValue());
                try {
                    DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
                    deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(deviceCredentialsUpdateMsg.getCredentialsType()));
                    deviceCredentials.setCredentialsId(deviceCredentialsUpdateMsg.getCredentialsId());
                    deviceCredentials.setCredentialsValue(deviceCredentialsUpdateMsg.hasCredentialsValue()
                            ? deviceCredentialsUpdateMsg.getCredentialsValue() : null);
                    deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
                } catch (Exception e) {
                    log.error("Can't update device credentials for device [{}], deviceCredentialsUpdateMsg [{}]",
                            device.getName(), deviceCredentialsUpdateMsg, e);
                    throw new RuntimeException(e);
                }
            } else {
                log.warn("Can't find device by id [{}], deviceCredentialsUpdateMsg [{}]", deviceId, deviceCredentialsUpdateMsg);
            }
            return null;
        }, dbCallbackExecutorService);
    }
}
