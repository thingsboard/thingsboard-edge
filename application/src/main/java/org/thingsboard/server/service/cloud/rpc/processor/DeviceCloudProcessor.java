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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class DeviceCloudProcessor extends BaseCloudProcessor {

    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    public ListenableFuture<Void> processDeviceMsgFromCloud(TenantId tenantId,
                                                            CustomerId edgeCustomerId,
                                                            DeviceUpdateMsg deviceUpdateMsg,
                                                            Long queueStartTs) {
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg, edgeCustomerId);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                Device deviceById = deviceService.findDeviceById(tenantId, deviceId);
                if (deviceById != null) {
                    deviceService.deleteDevice(tenantId, deviceId);
                }
                break;
            case ENTITY_MERGE_RPC_MESSAGE:
                deviceCreationLock.lock();
                try {
                    String deviceName = deviceUpdateMsg.getName();
                    if (deviceUpdateMsg.hasConflictName()) {
                        deviceName = deviceUpdateMsg.getConflictName();
                    }
                    Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
                    if (deviceByName != null) {
                        deviceByName.setName(deviceUpdateMsg.getName());
                        deviceService.saveDevice(deviceByName);
                    }
                } finally {
                    deviceCreationLock.unlock();
                }
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(deviceUpdateMsg.getMsgType());
        }

        SettableFuture<Void> futureToSet = SettableFuture.create();
        Futures.addCallback(requestForAdditionalData(tenantId, deviceUpdateMsg.getMsgType(), deviceId, queueStartTs), new FutureCallback<>() {
            @Override
            public void onSuccess(Void ignored) {
                try {
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(deviceUpdateMsg.getMsgType()) ||
                            UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(deviceUpdateMsg.getMsgType())) {
                        cloudEventService.saveCloudEvent(tenantId, CloudEventType.DEVICE, EdgeEventActionType.CREDENTIALS_REQUEST,
                                deviceId, null, queueStartTs);
                    } else if (UpdateMsgType.ENTITY_MERGE_RPC_MESSAGE.equals(deviceUpdateMsg.getMsgType())) {
                        cloudEventService.saveCloudEvent(tenantId, CloudEventType.DEVICE, EdgeEventActionType.CREDENTIALS_UPDATED,
                                deviceId, null, 0L);
                    }
                    futureToSet.set(null);
                } catch (Exception e) {
                    log.error("Failed to save credential updated cloud event, deviceUpdateMsg [{}]", deviceUpdateMsg, e);
                    futureToSet.setException(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to request for additional data, deviceUpdateMsg [{}]", deviceUpdateMsg, t);
                futureToSet.setException(t);
            }
        }, dbCallbackExecutor);
        return futureToSet;
    }

    public ListenableFuture<Void> processDeviceCredentialsMsgFromCloud(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsUpdateMsg.getDeviceIdMSB(), deviceCredentialsUpdateMsg.getDeviceIdLSB()));
        Device device = deviceService.findDeviceById(tenantId, deviceId);

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
                String errMsg = String.format("Can't update device credentials for device [%s], deviceCredentialsUpdateMsg [%s]",
                        device.getName(), deviceCredentialsUpdateMsg);
                log.error(errMsg, e);
                return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
            }
        }
        return Futures.immediateFuture(null);
    }

    private void saveOrUpdateDevice(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg, CustomerId edgeCustomerId) {
        deviceCreationLock.lock();
        try {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            boolean created = false;
            String deviceName = deviceUpdateMsg.getName();
            if (device == null) {
                created = true;
                device = new Device();
                device.setTenantId(tenantId);
                device.setCreatedTime(Uuids.unixTimestamp(deviceId.getId()));
                Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
                if (deviceByName != null) {
                    deviceName = deviceName + "_" + RandomStringUtils.randomAlphabetic(15);
                    log.warn("Device with name {} already exists on the edge. Renaming device name to {}",
                            deviceUpdateMsg.getName(), deviceName);
                }
            }
            device.setName(deviceName);
            device.setType(deviceUpdateMsg.getType());
            device.setLabel(deviceUpdateMsg.hasLabel() ? deviceUpdateMsg.getLabel() : null);
            device.setAdditionalInfo(deviceUpdateMsg.hasAdditionalInfo()
                    ? JacksonUtil.toJsonNode(deviceUpdateMsg.getAdditionalInfo()) : null);

            UUID deviceProfileUUID = safeGetUUID(deviceUpdateMsg.getDeviceProfileIdMSB(), deviceUpdateMsg.getDeviceProfileIdLSB());
            device.setDeviceProfileId(deviceProfileUUID != null ? new DeviceProfileId(deviceProfileUUID) : null);

            device.setCustomerId(safeGetCustomerId(tenantId, deviceUpdateMsg.getCustomerIdMSB(), deviceUpdateMsg.getCustomerIdLSB(), edgeCustomerId));

            Optional<DeviceData> deviceDataOpt =
                    dataDecodingEncodingService.decode(deviceUpdateMsg.getDeviceDataBytes().toByteArray());
            device.setDeviceData(deviceDataOpt.orElse(null));

            UUID firmwareUUID = safeGetUUID(deviceUpdateMsg.getFirmwareIdMSB(), deviceUpdateMsg.getFirmwareIdLSB());
            device.setFirmwareId(firmwareUUID != null ? new OtaPackageId(firmwareUUID) : null);

            UUID softwareUUID = safeGetUUID(deviceUpdateMsg.getSoftwareIdMSB(), deviceUpdateMsg.getSoftwareIdLSB());
            device.setSoftwareId(softwareUUID != null ? new OtaPackageId(softwareUUID) : null);

            if (created) {
                deviceValidator.validate(device, Device::getTenantId);
                device.setId(deviceId);
            } else {
                deviceValidator.validate(device, Device::getTenantId);
            }
            Device savedDevice = deviceService.saveDevice(device, false);
            if (created) {
                DeviceCredentials deviceCredentials = new DeviceCredentials();
                deviceCredentials.setDeviceId(new DeviceId(savedDevice.getUuidId()));
                deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
                deviceCredentials.setCredentialsId(RandomStringUtils.randomAlphanumeric(20));
                deviceCredentialsService.createDeviceCredentials(device.getTenantId(), deviceCredentials);
            }
            tbClusterService.onDeviceUpdated(savedDevice, created ? null : device, false, false);
        } finally {
            deviceCreationLock.unlock();
        }
    }

    public ListenableFuture<Void> processDeviceRpcCallFromCloud(TenantId tenantId, DeviceRpcCallMsg deviceRpcCallMsg) {
        log.trace("[{}] processDeviceRpcCallFromCloud [{}]", tenantId, deviceRpcCallMsg);
        if (deviceRpcCallMsg.hasResponseMsg()) {
            return processDeviceRpcResponseFromCloud(tenantId, deviceRpcCallMsg);
        } else if (deviceRpcCallMsg.hasRequestMsg()) {
            return processDeviceRpcRequestFromCloud(tenantId, deviceRpcCallMsg);
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> processDeviceRpcResponseFromCloud(TenantId tenantId, DeviceRpcCallMsg deviceRpcCallMsg) {
        UUID sessionId = UUID.fromString(deviceRpcCallMsg.getSessionId());
        String serviceId = deviceRpcCallMsg.getServiceId();
        int requestId = deviceRpcCallMsg.getRequestId();
        TransportProtos.ToServerRpcResponseMsg.Builder responseMsgBuilder = TransportProtos.ToServerRpcResponseMsg.newBuilder()
                .setRequestId(requestId);
        if (StringUtils.isNotBlank(deviceRpcCallMsg.getResponseMsg().getError())) {
            responseMsgBuilder.setError(deviceRpcCallMsg.getResponseMsg().getError());
        } else {
            responseMsgBuilder.setPayload(deviceRpcCallMsg.getResponseMsg().getResponse());
        }
        TransportProtos.ToTransportMsg msg = TransportProtos.ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToServerResponse(responseMsgBuilder.build())
                .build();
        tbClusterService.pushNotificationToTransport(serviceId, msg, null);

        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> processDeviceRpcRequestFromCloud(TenantId tenantId, DeviceRpcCallMsg deviceRpcCallMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceRpcCallMsg.getDeviceIdMSB(), deviceRpcCallMsg.getDeviceIdLSB()));
        UUID requestUUID = new UUID(deviceRpcCallMsg.getRequestUuidMSB(), deviceRpcCallMsg.getRequestUuidLSB());
        boolean oneWay = deviceRpcCallMsg.getOneway();
        long expTime = deviceRpcCallMsg.getExpirationTime();
        boolean persisted = deviceRpcCallMsg.hasPersisted() && deviceRpcCallMsg.getPersisted();
        int retries = deviceRpcCallMsg.hasRetries() ? deviceRpcCallMsg.getRetries() : 1;
        String additionalInfo = deviceRpcCallMsg.hasAdditionalInfo() ? deviceRpcCallMsg.getAdditionalInfo() : null;
        ToDeviceRpcRequestBody body = new ToDeviceRpcRequestBody(deviceRpcCallMsg.getRequestMsg().getMethod(),
                deviceRpcCallMsg.getRequestMsg().getParams());

        ToDeviceRpcRequest rpcRequest = new ToDeviceRpcRequest(requestUUID,
                tenantId,
                deviceId,
                oneWay,
                expTime,
                body,
                persisted,
                retries,
                additionalInfo);

        // @voba - changes to be in sync with cloud version
        SecurityUser dummySecurityUser = new SecurityUser();
        tbCoreDeviceRpcService.processRestApiRpcRequest(rpcRequest,
                fromDeviceRpcResponse -> reply(rpcRequest, deviceRpcCallMsg.getRequestId(), fromDeviceRpcResponse),
                dummySecurityUser);
        return Futures.immediateFuture(null);
    }

    private void reply(ToDeviceRpcRequest rpcRequest, int requestId, FromDeviceRpcResponse response) {
        try {
            Optional<RpcError> rpcError = response.getError();
            ObjectNode body = JacksonUtil.OBJECT_MAPPER.createObjectNode();
            body.put("requestUUID", rpcRequest.getId().toString());
            body.put("expirationTime", rpcRequest.getExpirationTime());
            body.put("oneway", rpcRequest.isOneway());
            body.put("requestId", requestId);
            if (rpcError.isPresent()) {
                RpcError error = rpcError.get();
                body.put("error", error.name());
            } else {
                body.put("response", response.getResponse().orElse("{}"));
            }
            cloudEventService.saveCloudEvent(rpcRequest.getTenantId(), CloudEventType.DEVICE, EdgeEventActionType.RPC_CALL,
                    rpcRequest.getDeviceId(), body, 0L);
        } catch (Exception e) {
            log.debug("Can't process RPC response [{}] [{}]", rpcRequest, response, e);
        }
    }

    public UplinkMsg convertRpcCallEventToUplink(CloudEvent cloudEvent) {
        log.trace("Executing convertRpcCallEventToUplink, cloudEvent [{}]", cloudEvent);
        DeviceRpcCallMsg rpcResponseMsg = deviceMsgConstructor.constructDeviceRpcCallMsg(cloudEvent.getEntityId(), cloudEvent.getEntityBody());
        return UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceRpcCallMsg(rpcResponseMsg).build();
    }

    public UplinkMsg convertDeviceEventToUplink(TenantId tenantId, CloudEvent cloudEvent) {
        DeviceId deviceId = new DeviceId(cloudEvent.getEntityId());
        UplinkMsg msg = null;
        switch (cloudEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Device device = deviceService.findDeviceById(cloudEvent.getTenantId(), deviceId);
                if (device != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    DeviceUpdateMsg deviceUpdateMsg =
                            deviceMsgConstructor.constructDeviceUpdatedMsg(msgType, device, null);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceUpdateMsg(deviceUpdateMsg).build();
                } else {
                    log.info("Skipping event as device was not found [{}]", cloudEvent);
                }
                break;
            case DELETED:
                DeviceUpdateMsg deviceUpdateMsg =
                        deviceMsgConstructor.constructDeviceDeleteMsg(deviceId);
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceUpdateMsg(deviceUpdateMsg).build();
                break;
            case CREDENTIALS_UPDATED:
                DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
                if (deviceCredentials != null) {
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg =
                            deviceMsgConstructor.constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsg).build();
                } else {
                    log.info("Skipping event as device credentials was not found [{}]", cloudEvent);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported edge action type [" + cloudEvent.getAction() + "]");
        }
        return msg;
    }

}
