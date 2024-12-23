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
package org.thingsboard.server.service.edge.rpc.processor.device;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructor;

import java.util.UUID;

@Slf4j
public abstract class DeviceEdgeProcessor extends BaseDeviceProcessor implements DeviceProcessor {

    @Override
    public ListenableFuture<Void> processDeviceMsgFromEdge(TenantId tenantId, Edge edge, DeviceUpdateMsg deviceUpdateMsg) {
        log.trace("[{}] executing processDeviceMsgFromEdge [{}] from edge [{}]", tenantId, deviceUpdateMsg, edge.getId());
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (deviceUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    if (deviceUpdateMsg.hasEntityGroupIdMSB() && deviceUpdateMsg.hasEntityGroupIdLSB()) {
                        EntityGroupId entityGroupId = new EntityGroupId(
                                new UUID(deviceUpdateMsg.getEntityGroupIdMSB(), deviceUpdateMsg.getEntityGroupIdLSB()));
                        edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, deviceId);
                    } else {
                        removeDeviceFromEdgeAllDeviceGroup(tenantId, edge, deviceId);
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(deviceUpdateMsg.getMsgType());
            };
        } catch (DataValidationException | ThingsboardException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed devices violated {}", tenantId, deviceUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public ListenableFuture<Void> processDeviceCredentialsMsgFromEdge(TenantId tenantId, EdgeId edgeId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        log.debug("[{}] Executing processDeviceCredentialsMsgFromEdge, deviceCredentialsUpdateMsg [{}]", tenantId, deviceCredentialsUpdateMsg);
        try {
            edgeSynchronizationManager.getEdgeId().set(edgeId);

            updateDeviceCredentials(tenantId, deviceCredentialsUpdateMsg);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
        return Futures.immediateFuture(null);
    }

    private void saveOrUpdateDevice(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg, Edge edge) throws ThingsboardException {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), deviceId);
            pushDeviceCreatedEventToRuleEngine(tenantId, edge, deviceId);
        }
        addDeviceToEdgeAllDeviceGroup(tenantId, edge, deviceId);
        Boolean deviceNameUpdated = resultPair.getSecond();
        if (deviceNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.UPDATED, deviceId, null);
        }
    }

    private void pushDeviceCreatedEventToRuleEngine(TenantId tenantId, Edge edge, DeviceId deviceId) {
        try {
            Device device = edgeCtx.getDeviceService().findDeviceById(tenantId, deviceId);
            String deviceAsString = JacksonUtil.toString(device);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, device.getCustomerId());
            pushEntityEventToRuleEngine(tenantId, deviceId, device.getCustomerId(), TbMsgType.ENTITY_CREATED, deviceAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push device action to rule engine: {}", tenantId, deviceId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    private void addDeviceToEdgeAllDeviceGroup(TenantId tenantId, Edge edge, DeviceId deviceId) {
        try {
            Device device = edgeCtx.getDeviceService().findDeviceById(tenantId, deviceId);
            EntityGroup edgeDeviceGroup = edgeCtx.getEntityGroupService().findOrCreateEdgeAllGroupAsync(tenantId, edge, edge.getName(), device.getOwnerId().getEntityType(), EntityType.DEVICE).get();
            if (edgeDeviceGroup != null) {
                edgeCtx.getEntityGroupService().addEntityToEntityGroup(tenantId, edgeDeviceGroup.getId(), deviceId);
            }
        } catch (Exception e) {
            log.warn("[{}] Can't add device to edge device group, device id [{}]", tenantId, deviceId, e);
            throw new RuntimeException(e);
        }
    }

    private void removeDeviceFromEdgeAllDeviceGroup(TenantId tenantId, Edge edge, DeviceId deviceId) {
        Device deviceToDelete = edgeCtx.getDeviceService().findDeviceById(tenantId, deviceId);
        if (deviceToDelete != null) {
            try {
                EntityGroup edgeDeviceGroup = edgeCtx.getEntityGroupService().findOrCreateEdgeAllGroupAsync(tenantId, edge, edge.getName(), deviceToDelete.getOwnerId().getEntityType(), EntityType.DEVICE).get();
                if (edgeDeviceGroup != null) {
                    edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, edgeDeviceGroup.getId(), deviceToDelete.getId());
                }
            } catch (Exception e) {
                log.warn("[{}] Can't delete device from edge device 'All' group, device id [{}]", tenantId, deviceId, e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public ListenableFuture<Void> processDeviceRpcCallFromEdge(TenantId tenantId, Edge edge, DeviceRpcCallMsg deviceRpcCallMsg) {
        log.trace("[{}] processDeviceRpcCallFromEdge [{}]", tenantId, deviceRpcCallMsg);
        if (deviceRpcCallMsg.hasResponseMsg()) {
            return processDeviceRpcResponseFromEdge(tenantId, deviceRpcCallMsg);
        } else if (deviceRpcCallMsg.hasRequestMsg()) {
            return processDeviceRpcRequestFromEdge(tenantId, edge, deviceRpcCallMsg);
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> processDeviceRpcResponseFromEdge(TenantId tenantId, DeviceRpcCallMsg deviceRpcCallMsg) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        UUID requestUuid = new UUID(deviceRpcCallMsg.getRequestUuidMSB(), deviceRpcCallMsg.getRequestUuidLSB());
        DeviceId deviceId = new DeviceId(new UUID(deviceRpcCallMsg.getDeviceIdMSB(), deviceRpcCallMsg.getDeviceIdLSB()));

        FromDeviceRpcResponse response;
        if (!StringUtils.isEmpty(deviceRpcCallMsg.getResponseMsg().getError())) {
            response = new FromDeviceRpcResponse(requestUuid, null, RpcError.valueOf(deviceRpcCallMsg.getResponseMsg().getError()));
        } else {
            response = new FromDeviceRpcResponse(requestUuid, deviceRpcCallMsg.getResponseMsg().getResponse(), null);
        }
        TbQueueCallback callback = new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                futureToSet.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Can't process push notification to core [{}]", tenantId, deviceRpcCallMsg, t);
                futureToSet.setException(t);
            }
        };
        FromDeviceRpcResponseActorMsg msg =
                new FromDeviceRpcResponseActorMsg(deviceRpcCallMsg.getRequestId(),
                        tenantId,
                        deviceId, response);
        edgeCtx.getClusterService().pushMsgToCore(msg, callback);
        return futureToSet;
    }

    private ListenableFuture<Void> processDeviceRpcRequestFromEdge(TenantId tenantId, Edge edge, DeviceRpcCallMsg deviceRpcCallMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceRpcCallMsg.getDeviceIdMSB(), deviceRpcCallMsg.getDeviceIdLSB()));
        try {
            TbMsgMetaData metaData = new TbMsgMetaData();
            String requestId = Integer.toString(deviceRpcCallMsg.getRequestId());
            metaData.putValue("requestId", requestId);
            metaData.putValue("serviceId", deviceRpcCallMsg.getServiceId());
            metaData.putValue("sessionId", deviceRpcCallMsg.getSessionId());
            metaData.putValue(DataConstants.EDGE_ID, edge.getId().toString());
            Device device = edgeCtx.getDeviceService().findDeviceById(tenantId, deviceId);
            if (device != null) {
                metaData.putValue("deviceName", device.getName());
                metaData.putValue("deviceType", device.getType());
                metaData.putValue(DataConstants.DEVICE_ID, deviceId.getId().toString());
            }
            ObjectNode data = JacksonUtil.newObjectNode();
            data.put("method", deviceRpcCallMsg.getRequestMsg().getMethod());
            data.put("params", deviceRpcCallMsg.getRequestMsg().getParams());
            TbMsg tbMsg = TbMsg.newMsg()
                    .type(TbMsgType.TO_SERVER_RPC_REQUEST)
                    .originator(deviceId)
                    .copyMetaData(metaData)
                    .dataType(TbMsgDataType.JSON)
                    .data(JacksonUtil.toString(data))
                    .build();
            edgeCtx.getClusterService().pushMsgToRuleEngine(tenantId, deviceId, tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.debug("[{}] Successfully send TO_SERVER_RPC_REQUEST to rule engine [{}], deviceRpcCallMsg {}",
                            tenantId, device, deviceRpcCallMsg);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.debug("[{}] Failed to send TO_SERVER_RPC_REQUEST to rule engine [{}], deviceRpcCallMsg {}",
                            tenantId, device, deviceRpcCallMsg, t);
                }
            });
        } catch (IllegalArgumentException e) {
            log.warn("[{}][{}] Failed to push TO_SERVER_RPC_REQUEST to rule engine. deviceRpcCallMsg {}", tenantId, deviceId, deviceRpcCallMsg, e);
        }

        return Futures.immediateFuture(null);
    }

    @Override
    public DownlinkMsg convertDeviceEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        var msgConstructor = (DeviceMsgConstructor) edgeCtx.getDeviceMsgConstructorFactory().getMsgConstructorByEdgeVersion(edgeVersion);
        switch (edgeEvent.getAction()) {
            case ADDED:
            case ADDED_TO_ENTITY_GROUP:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                Device device = edgeCtx.getDeviceService().findDeviceById(edgeEvent.getTenantId(), deviceId);
                if (device != null) {
                    EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DeviceUpdateMsg deviceUpdateMsg = msgConstructor.constructDeviceUpdatedMsg(msgType, device, entityGroupId);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceUpdateMsg(deviceUpdateMsg);
                    DeviceCredentials deviceCredentials = edgeCtx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(edgeEvent.getTenantId(), deviceId);
                    if (deviceCredentials != null) {
                        DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = msgConstructor.constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                        builder.addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsg).build();
                    }
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        DeviceProfile deviceProfile = edgeCtx.getDeviceProfileService().findDeviceProfileById(edgeEvent.getTenantId(), device.getDeviceProfileId());
                        builder.addDeviceProfileUpdateMsg(msgConstructor.constructDeviceProfileUpdatedMsg(msgType, deviceProfile));
                    }
                    return builder.build();
                }
                break;
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
            case UNASSIGNED_FROM_EDGE:
            case CHANGE_OWNER:
                EntityGroupId entityGroupId = edgeEvent.getEntityGroupId() != null ? new EntityGroupId(edgeEvent.getEntityGroupId()) : null;
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceUpdateMsg(msgConstructor.constructDeviceDeleteMsg(deviceId, entityGroupId))
                        .build();
            case CREDENTIALS_UPDATED:
                DeviceCredentials deviceCredentials = edgeCtx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(edgeEvent.getTenantId(), deviceId);
                if (deviceCredentials != null) {
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceCredentialsUpdateMsg(msgConstructor.constructDeviceCredentialsUpdatedMsg(deviceCredentials))
                            .build();
                }
                break;
            case RPC_CALL:
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceRpcCallMsg(msgConstructor.constructDeviceRpcCallMsg(edgeEvent.getEntityId(), edgeEvent.getBody()))
                        .build();
        }
        return null;
    }

    @Override
    public DownlinkMsg convertDeviceGroupOtaEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        DownlinkMsg downlinkMsg = null;
        try {
            DeviceGroupOtaPackage deviceGroupOtaPackage = JacksonUtil.convertValue(edgeEvent.getBody(), DeviceGroupOtaPackage.class);
            if (deviceGroupOtaPackage == null) {
                return null;
            }
            var msgConstructor = (DeviceMsgConstructor) edgeCtx.getDeviceMsgConstructorFactory().getMsgConstructorByEdgeVersion(edgeVersion);
            UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
            downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addDeviceGroupOtaPackageUpdateMsg(msgConstructor.constructDeviceGroupOtaUpdateMsg(msgType, deviceGroupOtaPackage))
                    .build();
        } catch (Exception e) {
            log.error("Can't process device group ota package msg [{}]", edgeEvent, e);
        }
        return downlinkMsg;
    }

}
