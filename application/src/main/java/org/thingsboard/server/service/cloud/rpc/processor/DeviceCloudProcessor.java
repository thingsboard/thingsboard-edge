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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.edge.rpc.processor.device.BaseDeviceProcessor;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class DeviceCloudProcessor extends BaseDeviceProcessor {

    public ListenableFuture<Void> processDeviceMsgFromCloud(TenantId tenantId,
                                                            DeviceUpdateMsg deviceUpdateMsg,
                                                            Long queueStartTs) throws ThingsboardException {
        log.trace("[{}] executing processDeviceMsgFromCloud [{}]", tenantId, deviceUpdateMsg);
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg, queueStartTs);
                return Futures.transformAsync(requestForAdditionalData(tenantId, deviceId, queueStartTs),
                        ignored -> cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.DEVICE, EdgeEventActionType.CREDENTIALS_REQUEST,
                        deviceId, null, null, queueStartTs), dbCallbackExecutorService);
            case ENTITY_DELETED_RPC_MESSAGE:
                if (deviceUpdateMsg.hasEntityGroupIdMSB() && deviceUpdateMsg.hasEntityGroupIdLSB()) {
                    UUID entityGroupUUID = safeGetUUID(deviceUpdateMsg.getEntityGroupIdMSB(),
                            deviceUpdateMsg.getEntityGroupIdLSB());
                    EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                    entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, deviceId);
                } else {
                    Device deviceById = deviceService.findDeviceById(tenantId, deviceId);
                    if (deviceById != null) {
                        deviceService.deleteDevice(tenantId, deviceId);
                    }
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
            default:
                return handleUnsupportedMsgType(deviceUpdateMsg.getMsgType());
        }
    }

    private void saveOrUpdateDevice(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg, Long queueStartTs) throws ThingsboardException {
        CustomerId customerId = safeGetCustomerId(deviceUpdateMsg.getCustomerIdMSB(), deviceUpdateMsg.getCustomerIdLSB());
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg, customerId);
        Boolean deviceNameUpdated = resultPair.getSecond();
        if (deviceNameUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.DEVICE, EdgeEventActionType.UPDATED, deviceId, null, null, queueStartTs);
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
                    rpcRequest.getDeviceId(), body, null, 0L);
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
            case ADDED_TO_ENTITY_GROUP:
                Device device = deviceService.findDeviceById(cloudEvent.getTenantId(), deviceId);
                if (device != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    EntityGroupId entityGroupId = cloudEvent.getEntityGroupId() != null ? new EntityGroupId(cloudEvent.getEntityGroupId()) : null;
                    DeviceUpdateMsg deviceUpdateMsg =
                            deviceMsgConstructor.constructDeviceUpdatedMsg(msgType, device, entityGroupId);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceUpdateMsg(deviceUpdateMsg).build();
                } else {
                    log.info("Skipping event as device was not found [{}]", cloudEvent);
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
                EntityGroupId entityGroupId = cloudEvent.getEntityGroupId() != null ? new EntityGroupId(cloudEvent.getEntityGroupId()) : null;
                DeviceUpdateMsg deviceUpdateMsg =
                        deviceMsgConstructor.constructDeviceDeleteMsg(deviceId, entityGroupId);
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
        }
        return msg;
    }

}
