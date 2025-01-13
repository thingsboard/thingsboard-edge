/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.device.BaseDeviceProcessor;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class DeviceCloudProcessor extends BaseDeviceProcessor {

    @Autowired
    private TbCoreDeviceRpcService tbCoreDeviceRpcService;

    public ListenableFuture<Void> processDeviceMsgFromCloud(TenantId tenantId,
                                                            DeviceUpdateMsg deviceUpdateMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (deviceUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    boolean created = saveOrUpdateDeviceFromCloud(tenantId, deviceId, deviceUpdateMsg);
                    return created ? requestForAdditionalData(tenantId, deviceId) : Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    Device deviceById = edgeCtx.getDeviceService().findDeviceById(tenantId, deviceId);
                    if (deviceById != null) {
                        edgeCtx.getDeviceService().deleteDevice(tenantId, deviceId);
                        pushDeviceDeletedEventToRuleEngine(tenantId, deviceById);
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(deviceUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private boolean saveOrUpdateDeviceFromCloud(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            pushDeviceCreatedEventToRuleEngine(tenantId, deviceId);
        }
        Boolean deviceNameUpdated = resultPair.getSecond();
        if (deviceNameUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.DEVICE, EdgeEventActionType.UPDATED, deviceId, null);
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
                    rpcRequest.getDeviceId(), body);
        } catch (Exception e) {
            log.debug("Can't process RPC response [{}] [{}]", rpcRequest, response, e);
        }
    }

    public UplinkMsg convertRpcCallEventToUplink(CloudEvent cloudEvent) {
        log.trace("Executing convertRpcCallEventToUplink, cloudEvent [{}]", cloudEvent);
        DeviceRpcCallMsg rpcResponseMsg = ((DeviceMsgConstructor) edgeCtx.getDeviceMsgConstructorFactory().getMsgConstructorByEdgeVersion(EdgeVersion.V_LATEST))
                .constructDeviceRpcCallMsg(cloudEvent.getEntityId(), cloudEvent.getEntityBody());
        return UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceRpcCallMsg(rpcResponseMsg).build();
    }

    public UplinkMsg convertDeviceEventToUplink(TenantId tenantId, CloudEvent cloudEvent, EdgeVersion edgeVersion) {
        DeviceId deviceId = new DeviceId(cloudEvent.getEntityId());
        var msgConstructor = (DeviceMsgConstructor) edgeCtx.getDeviceMsgConstructorFactory().getMsgConstructorByEdgeVersion(edgeVersion);
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER -> {
                Device device = edgeCtx.getDeviceService().findDeviceById(cloudEvent.getTenantId(), deviceId);
                if (device != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    DeviceUpdateMsg deviceUpdateMsg = msgConstructor.constructDeviceUpdatedMsg(msgType, device);
                    UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceUpdateMsg(deviceUpdateMsg);

                    DeviceCredentials deviceCredentials = edgeCtx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(tenantId, deviceId);
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = msgConstructor.constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                    builder.addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsg);

                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        DeviceProfile deviceProfile = edgeCtx.getDeviceProfileService().findDeviceProfileById(cloudEvent.getTenantId(), device.getDeviceProfileId());
                        builder.addDeviceProfileUpdateMsg(msgConstructor.constructDeviceProfileUpdatedMsg(msgType, deviceProfile));
                    }
                    return builder.build();
                } else {
                    log.info("Skipping event as device was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                DeviceUpdateMsg deviceUpdateMsg = msgConstructor.constructDeviceDeleteMsg(deviceId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceUpdateMsg(deviceUpdateMsg).build();
            }
            case CREDENTIALS_UPDATED -> {
                DeviceCredentials deviceCredentials = edgeCtx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(tenantId, deviceId);
                if (deviceCredentials != null) {
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = msgConstructor.constructDeviceCredentialsUpdatedMsg(deviceCredentials);
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
    protected Device constructDeviceFromUpdateMsg(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg) {
        return JacksonUtil.fromString(deviceUpdateMsg.getEntity(), Device.class, true);
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, Device device, DeviceUpdateMsg deviceUpdateMsg) {
        CustomerId assignedCustomerId = device.getCustomerId();
        Customer customer = null;
        if (assignedCustomerId != null) {
            customer = edgeCtx.getCustomerService().findCustomerById(tenantId, assignedCustomerId);
        }
        device.setCustomerId(customer != null ? customer.getId() : null);
    }

    @Override
    protected DeviceCredentials constructDeviceCredentialsFromUpdateMsg(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        return JacksonUtil.fromString(deviceCredentialsUpdateMsg.getEntity(), DeviceCredentials.class, true);
    }

}
