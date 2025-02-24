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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceGroupOtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.device.BaseDeviceProcessor;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class DeviceCloudProcessor extends BaseDeviceProcessor {

    @Autowired
    protected DeviceGroupOtaPackageService deviceGroupOtaPackageService;

    @Autowired
    private TbCoreDeviceRpcService tbCoreDeviceRpcService;

    public ListenableFuture<Void> processDeviceMsgFromCloud(TenantId tenantId, DeviceUpdateMsg deviceUpdateMsg) throws ThingsboardException {
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            return switch (deviceUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    boolean created = saveOrUpdateDeviceFromCloud(tenantId, deviceId, deviceUpdateMsg);
                    yield created ? requestForAdditionalData(tenantId, deviceId) : Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    deviceCreationLock.lock();
                    try {
                        if (deviceUpdateMsg.hasEntityGroupIdMSB() && deviceUpdateMsg.hasEntityGroupIdLSB()) {
                            UUID entityGroupUUID = safeGetUUID(deviceUpdateMsg.getEntityGroupIdMSB(),
                                    deviceUpdateMsg.getEntityGroupIdLSB());
                            EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                            edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, deviceId);
                            yield removeEntityIfInSingleAllGroup(tenantId, deviceId, () -> edgeCtx.getDeviceService().deleteDevice(tenantId, deviceId));
                        } else {
                            Device deviceById = edgeCtx.getDeviceService().findDeviceById(tenantId, deviceId);
                            if (deviceById != null) {
                                edgeCtx.getDeviceService().deleteDevice(tenantId, deviceId);
                                pushDeviceDeletedEventToRuleEngine(tenantId, deviceById);
                            }
                        }
                        yield Futures.immediateFuture(null);
                    } finally {
                        deviceCreationLock.unlock();
                    }
                }
                default -> handleUnsupportedMsgType(deviceUpdateMsg.getMsgType());
            };
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private boolean saveOrUpdateDeviceFromCloud(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg) throws ThingsboardException {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            pushDeviceCreatedEventToRuleEngine(tenantId, deviceId);
        }
        Boolean deviceNameUpdated = resultPair.getSecond();
        if (deviceNameUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.DEVICE, EdgeEventActionType.UPDATED, deviceId, null, null);
        }
        return created;
    }

    private void pushDeviceCreatedEventToRuleEngine(TenantId tenantId, DeviceId deviceId) {
        Device device = edgeCtx.getDeviceService().findDeviceById(tenantId, deviceId);
        pushDeviceEventToRuleEngine(tenantId, device, TbMsgType.ENTITY_CREATED);
    }

    private void pushDeviceDeletedEventToRuleEngine(TenantId tenantId, Device device) {
        pushDeviceEventToRuleEngine(tenantId, device, TbMsgType.ENTITY_DELETED);
    }

    private void pushDeviceEventToRuleEngine(TenantId tenantId, Device device, TbMsgType msgType) {
        try {
            String deviceAsString = JacksonUtil.toString(device);
            pushEntityEventToRuleEngine(tenantId, device.getId(), device.getCustomerId(), msgType, deviceAsString, new TbMsgMetaData());
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push device action to rule engine: {}", tenantId, device.getId(), msgType.name(), e);
        }
    }

    public ListenableFuture<Void> processDeviceCredentialsMsgFromCloud(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        try {
            cloudSynchronizationManager.getSync().set(true);

            updateDeviceCredentials(tenantId, deviceCredentialsUpdateMsg);
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<Void> processDeviceRpcCallFromCloud(TenantId tenantId, DeviceRpcCallMsg deviceRpcCallMsg) {
        log.trace("[{}] processDeviceRpcCallFromCloud [{}]", tenantId, deviceRpcCallMsg);
        if (deviceRpcCallMsg.hasResponseMsg()) {
            return processDeviceRpcResponseFromCloud(deviceRpcCallMsg);
        } else if (deviceRpcCallMsg.hasRequestMsg()) {
            return processDeviceRpcRequestFromCloud(tenantId, deviceRpcCallMsg);
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> processDeviceRpcResponseFromCloud(DeviceRpcCallMsg deviceRpcCallMsg) {
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
        edgeCtx.getClusterService().pushNotificationToTransport(serviceId, msg, null);

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

        // edge: changes to be in sync with cloud version
        SecurityUser dummySecurityUser = new SecurityUser();
        tbCoreDeviceRpcService.processRestApiRpcRequest(rpcRequest,
                fromDeviceRpcResponse -> reply(rpcRequest, deviceRpcCallMsg.getRequestId(), fromDeviceRpcResponse),
                dummySecurityUser);
        return Futures.immediateFuture(null);
    }

    private void reply(ToDeviceRpcRequest rpcRequest, int requestId, FromDeviceRpcResponse response) {
        try {
            Optional<RpcError> rpcError = response.getError();
            ObjectNode body = JacksonUtil.newObjectNode();
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
                    rpcRequest.getDeviceId(), body, null);
        } catch (Exception e) {
            log.debug("Can't process RPC response [{}] [{}]", rpcRequest, response, e);
        }
    }

    public UplinkMsg convertRpcCallEventToUplink(CloudEvent cloudEvent) {
        log.trace("Executing convertRpcCallEventToUplink, cloudEvent [{}]", cloudEvent);
        DeviceRpcCallMsg rpcResponseMsg = EdgeMsgConstructorUtils.constructDeviceRpcCallMsg(cloudEvent.getEntityId(), cloudEvent.getEntityBody());
        return UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceRpcCallMsg(rpcResponseMsg).build();
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        DeviceId deviceId = new DeviceId(cloudEvent.getEntityId());
        EntityGroupId entityGroupId = cloudEvent.getEntityGroupId() != null ? new EntityGroupId(cloudEvent.getEntityGroupId()) : null;
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED, ADDED_TO_ENTITY_GROUP -> {
                Device device = edgeCtx.getDeviceService().findDeviceById(cloudEvent.getTenantId(), deviceId);
                if (device != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    DeviceUpdateMsg deviceUpdateMsg = EdgeMsgConstructorUtils.constructDeviceUpdatedMsg(msgType, device, entityGroupId);
                    UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceUpdateMsg(deviceUpdateMsg);

                    DeviceCredentials deviceCredentials = edgeCtx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(cloudEvent.getTenantId(), deviceId);
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = EdgeMsgConstructorUtils.constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                    builder.addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsg).build();

                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        DeviceProfile deviceProfile = edgeCtx.getDeviceProfileService().findDeviceProfileById(cloudEvent.getTenantId(), device.getDeviceProfileId());
                        builder.addDeviceProfileUpdateMsg(EdgeMsgConstructorUtils.constructDeviceProfileUpdatedMsg(msgType, deviceProfile));
                    }
                    return builder.build();
                } else {
                    log.info("Skipping event as device was not found [{}]", cloudEvent);
                }
            }
            case DELETED, REMOVED_FROM_ENTITY_GROUP -> {
                DeviceUpdateMsg deviceUpdateMsg = EdgeMsgConstructorUtils.constructDeviceDeleteMsg(deviceId, entityGroupId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceUpdateMsg(deviceUpdateMsg).build();
            }
            case CREDENTIALS_UPDATED -> {
                DeviceCredentials deviceCredentials = edgeCtx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(cloudEvent.getTenantId(), deviceId);
                if (deviceCredentials != null) {
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = EdgeMsgConstructorUtils.constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsg).build();
                } else {
                    log.info("Skipping event as device credentials was not found [{}]", cloudEvent);
                }
            }
        }
        return null;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, Device device, DeviceUpdateMsg deviceUpdateMsg) {
        if (isCustomerNotExists(tenantId, device.getCustomerId())) {
            device.setCustomerId(null);
        }
    }

    public ListenableFuture<Void> processDeviceGroupOtaPackageFromCloud(TenantId tenantId, DeviceGroupOtaPackageUpdateMsg deviceGroupOtaPackageUpdateMsg) {
        log.trace("[{}] processDeviceGroupOtaPackageFromCloud [{}]", tenantId, deviceGroupOtaPackageUpdateMsg);
        DeviceGroupOtaPackage deviceGroupOtaPackage = JacksonUtil.fromString(deviceGroupOtaPackageUpdateMsg.getEntity(), DeviceGroupOtaPackage.class, true);
        if (deviceGroupOtaPackage == null) {
            throw new RuntimeException("[{" + tenantId + "}] deviceGroupOtaPackageUpdateMsg {" + deviceGroupOtaPackageUpdateMsg + "} cannot be converted to device group ota package");
        }
        switch (deviceGroupOtaPackageUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage, false);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                deviceGroupOtaPackageService.deleteDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
                break;
            case UNRECOGNIZED:
            default:
                return handleUnsupportedMsgType(deviceGroupOtaPackageUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.DEVICE;
    }

}
