/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.processor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.RpcError;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.CloudType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.service.state.DeviceStateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class DeviceProcessor extends BaseProcessor {

    @Autowired
    private DeviceStateService deviceStateService;

    private final Lock deviceCreationLock = new ReentrantLock();

    public ListenableFuture<Void> onDeviceUpdate(TenantId tenantId, CustomerId customerId, DeviceUpdateMsg deviceUpdateMsg, CloudType cloudType) {
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                saveOrUpdateDevice(tenantId, customerId, deviceUpdateMsg, cloudType);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                UUID entityGroupUUID = safeGetUUID(deviceUpdateMsg.getEntityGroupIdMSB(), deviceUpdateMsg.getEntityGroupIdLSB());
                if (entityGroupUUID != null) {
                    EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                    entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, deviceId);
                } else {
                    Device deviceById = deviceService.findDeviceById(tenantId, deviceId);
                    if (deviceById != null) {
                        deviceService.deleteDevice(tenantId, deviceId);
                    }
                }
                break;
            case DEVICE_CONFLICT_RPC_MESSAGE:
                try {
                    deviceCreationLock.lock();
                    String deviceName = deviceUpdateMsg.getName();
                    Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
                    if (deviceByName != null) {
                        deviceByName.setName(RandomStringUtils.randomAlphabetic(15));
                        deviceService.saveDevice(deviceByName);
                        Device deviceCopy = saveOrUpdateDevice(tenantId, customerId, deviceUpdateMsg, cloudType);
                        ListenableFuture<List<Void>> future = updateOrCopyDeviceRelatedEntities(tenantId, deviceByName, deviceCopy);
                        Futures.transform(future, list -> {
                            log.debug("Related entities copied, removing origin device [{}]", deviceByName.getId());
                            deviceService.deleteDevice(tenantId, deviceByName.getId());
                            return null;
                        }, MoreExecutors.directExecutor());
                    }
                } finally {
                    deviceCreationLock.unlock();
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
                return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type" + deviceUpdateMsg.getMsgType()));
        }

        ListenableFuture<Void> aDRF = Futures.transform(requestForAdditionalData(tenantId, deviceUpdateMsg.getMsgType(), deviceId), future -> null, dbCallbackExecutor);

        ListenableFuture<ListenableFuture<Void>> t = Futures.transform(aDRF, aDR -> {
            ListenableFuture<CloudEvent> f = Futures.immediateFuture(null);
            if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(deviceUpdateMsg.getMsgType()) ||
                    UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(deviceUpdateMsg.getMsgType()) ||
                    UpdateMsgType.DEVICE_CONFLICT_RPC_MESSAGE.equals(deviceUpdateMsg.getMsgType())) {
                f = saveCloudEvent(tenantId, CloudEventType.DEVICE, ActionType.CREDENTIALS_REQUEST, deviceId, null);
            }
            return Futures.transform(f, tmp -> null, dbCallbackExecutor);
        }, dbCallbackExecutor);

        return Futures.transform(t, tt -> null, dbCallbackExecutor);
    }

    public ListenableFuture<Void> onDeviceCredentialsUpdate(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsUpdateMsg.getDeviceIdMSB(), deviceCredentialsUpdateMsg.getDeviceIdLSB()));
        Device device = deviceService.findDeviceById(tenantId, deviceId);

        if (device != null) {
            log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                    device.getName(), deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentialsUpdateMsg.getCredentialsValue());
            try {
                DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
                deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(deviceCredentialsUpdateMsg.getCredentialsType()));
                deviceCredentials.setCredentialsId(deviceCredentialsUpdateMsg.getCredentialsId());
                deviceCredentials.setCredentialsValue(deviceCredentialsUpdateMsg.getCredentialsValue());
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            } catch (Exception e) {
                log.error("Can't update device credentials for device [{}], deviceCredentialsUpdateMsg [{}]",
                        device.getName(), deviceCredentialsUpdateMsg, e);
                return Futures.immediateFailedFuture(
                        new RuntimeException("Can't update device credentials for device " +
                                device.getName() +", deviceCredentialsUpdateMsg " + deviceCredentialsUpdateMsg,
                                e));
            }
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<List<Void>> updateOrCopyDeviceRelatedEntities(TenantId tenantId, Device origin, Device destination) {
        updateAuditLogs(tenantId, origin, destination);
        updateEvents(tenantId, origin, destination);
        ArrayList<ListenableFuture<Void>> futures = new ArrayList<>();
        futures.add(updateEntityViews(tenantId, origin, destination));
        futures.add(updateAlarms(tenantId, origin, destination));
        futures.add(copyAttributes(tenantId, origin, destination));
        futures.add(copyRelations(tenantId, origin, destination, EntitySearchDirection.FROM));
        futures.add(copyRelations(tenantId, origin, destination, EntitySearchDirection.TO));
        return Futures.allAsList(futures);
    }

    private ListenableFuture<Void> copyRelations(TenantId tenantId, Device origin, Device destination, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(origin.getId(), direction, -1, false));
        ListenableFuture<List<EntityRelation>> relationsByQueryFuture = relationService.findByQuery(tenantId, query);
        return Futures.transform(relationsByQueryFuture, relationsByQuery -> {
            if (relationsByQuery != null && !relationsByQuery.isEmpty()) {
                for (EntityRelation relation : relationsByQuery) {
                    if (EntitySearchDirection.FROM.equals(direction)) {
                        relation.setFrom(destination.getId());
                    } else {
                        relation.setTo(destination.getId());
                    }
                    relationService.saveRelationAsync(tenantId, relation);
                }
            }
            log.debug("Related [{}] relations copied, origin [{}], destination [{}]", direction.name(), origin.getId(), destination.getId());
            return null;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> updateEntityViews(TenantId tenantId, Device origin, Device destination) {
        ListenableFuture<List<EntityView>> entityViewsFuture = entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, origin.getId());
        return Futures.transform(entityViewsFuture, entityViews -> {
            if (entityViews != null && !entityViews.isEmpty()) {
                for (EntityView entityView : entityViews) {
                    entityView.setEntityId(destination.getId());
                    entityViewService.saveEntityView(entityView);
                }
            }
            log.debug("Related entity views updated, origin [{}], destination [{}]", origin.getId(), destination.getId());
            return null;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> copyAttributes(TenantId tenantId, Device origin, Device destination) {
        ListenableFuture<List<AttributeKvEntry>> allFuture = attributesService.findAll(tenantId, origin.getId(), DataConstants.SERVER_SCOPE);
        return Futures.transform(allFuture, attributes -> {
            if (attributes != null && !attributes.isEmpty()) {
                attributesService.save(tenantId, destination.getId(), DataConstants.SERVER_SCOPE, attributes);
            }
            log.debug("Related attributes copied, origin [{}], destination [{}]", origin.getId(), destination.getId());
            return null;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> updateAlarms(TenantId tenantId, Device origin, Device destination) {
        ListenableFuture<TimePageData<AlarmInfo>> alarmsFuture = alarmService.findAlarms(tenantId, new AlarmQuery(origin.getId(), new TimePageLink(Integer.MAX_VALUE), null, null, false));
        return Futures.transform(alarmsFuture, alarms -> {
            if (alarms != null && alarms.getData() != null && !alarms.getData().isEmpty()) {
                for (AlarmInfo alarm : alarms.getData()) {
                    alarm.setOriginator(destination.getId());
                    alarmService.createOrUpdateAlarm(alarm);
                }
            }
            log.debug("Related alarms updated, origin [{}], destination [{}]", origin.getId(), destination.getId());
            return null;
        }, MoreExecutors.directExecutor());
    }

    private Device saveOrUpdateDevice(TenantId tenantId, CustomerId customerId, DeviceUpdateMsg deviceUpdateMsg, CloudType cloudType) {
        Device device;
        try {
            deviceCreationLock.lock();
            DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
            device = deviceService.findDeviceById(tenantId, deviceId);
            boolean created = false;
            if (device == null) {
                device = new Device();
                device.setTenantId(tenantId);
                device.setId(deviceId);
                created = true;
            }
            device.setName(deviceUpdateMsg.getName());
            device.setType(deviceUpdateMsg.getType());
            device.setLabel(deviceUpdateMsg.getLabel());
            CustomerId deviceCustomerId = safeSetCustomerId(deviceUpdateMsg, cloudType, device);
            device = deviceService.saveDevice(device, created);
            if (created) {
                deviceStateService.onDeviceAdded(device);
            }
            addToEntityGroup(tenantId, customerId, deviceUpdateMsg, cloudType, deviceId, deviceCustomerId);
        } finally {
            deviceCreationLock.unlock();
        }
        return device;
    }

    private CustomerId safeSetCustomerId(DeviceUpdateMsg deviceUpdateMsg, CloudType cloudType, Device device) {
        CustomerId deviceCustomerId = safeGetCustomerId(deviceUpdateMsg.getCustomerIdMSB(), deviceUpdateMsg.getCustomerIdLSB());
        if (CloudType.PE.equals(cloudType)) {
            device.setCustomerId(deviceCustomerId);
        }
        return deviceCustomerId;
    }

    private void addToEntityGroup(TenantId tenantId, CustomerId customerId, DeviceUpdateMsg deviceUpdateMsg, CloudType cloudType, DeviceId deviceId, CustomerId deviceCustomerId) {
        if (CloudType.CE.equals(cloudType)) {
            if (deviceCustomerId != null && deviceCustomerId.equals(customerId)) {
                EntityGroup customerDevicesEntityGroup =
                        entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(tenantId, customerId, EntityType.DEVICE);
                entityGroupService.addEntityToEntityGroup(tenantId, customerDevicesEntityGroup.getId(), deviceId);
            }
            if ((deviceCustomerId == null || deviceCustomerId.isNullUid()) &&
                    (customerId != null && !customerId.isNullUid())) {
                EntityGroup customerDevicesEntityGroup =
                        entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(tenantId, customerId, EntityType.DEVICE);
                entityGroupService.removeEntityFromEntityGroup(tenantId, customerDevicesEntityGroup.getId(), deviceId);
            }
        } else {
            UUID entityGroupUUID = safeGetUUID(deviceUpdateMsg.getEntityGroupIdMSB(), deviceUpdateMsg.getEntityGroupIdLSB());
            if (entityGroupUUID != null) {
                EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                addEntityToGroup(tenantId, entityGroupId, deviceId);
            }
        }
    }

    public ListenableFuture<Void> onDeviceRpcRequest(TenantId tenantId, DeviceRpcCallMsg deviceRpcRequestMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceRpcRequestMsg.getDeviceIdMSB(), deviceRpcRequestMsg.getDeviceIdLSB()));
        boolean oneWay = deviceRpcRequestMsg.getOneway();
        long expTime = deviceRpcRequestMsg.getExpirationTime();

        ToDeviceRpcRequestBody body = new ToDeviceRpcRequestBody(deviceRpcRequestMsg.getRequestMsg().getMethod(),
                deviceRpcRequestMsg.getRequestMsg().getParams());

        UUID requestUUID = new UUID(deviceRpcRequestMsg.getRequestIdMSB(), deviceRpcRequestMsg.getRequestIdLSB());
        ToDeviceRpcRequest rpcRequest = new ToDeviceRpcRequest(requestUUID,
                tenantId,
                deviceId,
                oneWay,
                expTime,
                body
        );

        deviceRpcService.processRestApiRpcRequest(rpcRequest,
                fromDeviceRpcResponse -> reply(rpcRequest, deviceRpcRequestMsg.getOriginServiceId(), fromDeviceRpcResponse));
        return Futures.immediateFuture(null);
    }

    public void reply(ToDeviceRpcRequest rpcRequest, String originServiceId, FromDeviceRpcResponse response) {
        try {
            Optional<RpcError> rpcError = response.getError();
            ObjectNode body = mapper.createObjectNode();
            body.put("requestUUID", rpcRequest.getId().toString());
            body.put("expirationTime", rpcRequest.getExpirationTime());
            body.put("oneway", rpcRequest.isOneway());
            body.put("originServiceId", originServiceId);
            if (rpcError.isPresent()) {
                RpcError error = rpcError.get();
                body.put("error", error.name());
            } else {
                body.put("response", response.getResponse().orElse("{}"));
            }
            saveCloudEvent(rpcRequest.getTenantId(), CloudEventType.DEVICE, ActionType.RPC_CALL, rpcRequest.getDeviceId(), body);
        } catch (Exception e) {
            log.debug("Can't process RPC response [{}] [{}]", rpcRequest, response);
        }
    }

}
