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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.service.state.DeviceStateService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class DeviceUpdateProcessor extends BaseUpdateProcessor {

    @Autowired
    private DeviceStateService deviceStateService;

    private final Lock deviceCreationLock = new ReentrantLock();

    public void onDeviceUpdate(TenantId tenantId, DeviceUpdateMsg deviceUpdateMsg) {
        log.info("onDeviceUpdate {}", deviceUpdateMsg);
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                saveOrUpdateDevice(tenantId, deviceUpdateMsg);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                ListenableFuture<Device> deviceByIdAsyncFuture = deviceService.findDeviceByIdAsync(tenantId, deviceId);
                Futures.transform(deviceByIdAsyncFuture, deviceByIdAsync -> {
                    if (deviceByIdAsync != null) {
                        deviceService.deleteDevice(tenantId, deviceId);
                    }
                    return null;
                }, dbCallbackExecutor);
                break;
            case DEVICE_CONFLICT_RPC_MESSAGE:
                try {
                    deviceCreationLock.lock();
                    String deviceName = deviceUpdateMsg.getName();
                    Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
                    if (deviceByName != null) {
                        deviceByName.setName(RandomStringUtils.randomAlphabetic(15));
                        deviceService.saveDevice(deviceByName);
                        Device deviceCopy = saveOrUpdateDevice(tenantId, deviceUpdateMsg);
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
        }

        requestForAdditionalData(deviceUpdateMsg.getMsgType(), deviceId);

        if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(deviceUpdateMsg.getMsgType()) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(deviceUpdateMsg.getMsgType()) ||
                UpdateMsgType.DEVICE_CONFLICT_RPC_MESSAGE.equals(deviceUpdateMsg.getMsgType())) {
            eventStorage.write(constructDeviceCredentialsRequestMsg(deviceId), edgeEventSaveCallback);
        }
    }

    public void onDeviceCredentialsUpdate(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsUpdateMsg.getDeviceIdMSB(), deviceCredentialsUpdateMsg.getDeviceIdLSB()));
        ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(tenantId, deviceId);

        Futures.addCallback(deviceFuture, new FutureCallback<Device>() {
            @Override
            public void onSuccess(@Nullable Device device) {
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
                        log.error("Can't update device credentials for device [{}], deviceCredentialsUpdateMsg [{}]", device.getName(), deviceCredentialsUpdateMsg, e);
                    }
                    log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                            device.getName(), deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentialsUpdateMsg.getCredentialsValue());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Can't update device credentials for deviceCredentialsUpdateMsg [{}]", deviceCredentialsUpdateMsg, t);
            }
        }, dbCallbackExecutor);
    }

    private UplinkMsg constructDeviceCredentialsRequestMsg(DeviceId deviceId) {
        DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .addAllDeviceCredentialsRequestMsg(Collections.singletonList(deviceCredentialsRequestMsg));
        return builder.build();
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

    private Device saveOrUpdateDevice(TenantId tenantId, DeviceUpdateMsg deviceUpdateMsg) {
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
            device = deviceService.saveDevice(device, created);
            if (created) {
                deviceStateService.onDeviceAdded(device);
            }

            EntityGroupId entityGroupId = new EntityGroupId(new UUID(deviceUpdateMsg.getEntityGroupIdMSB(), deviceUpdateMsg.getEntityGroupIdLSB()));
            addEntityToGroup(tenantId, entityGroupId, device.getId());
        } finally {
            deviceCreationLock.unlock();
        }
        return device;
    }
}
