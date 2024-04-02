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
package org.thingsboard.server.service.cloud;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.CloudUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultCloudNotificationService implements CloudNotificationService {

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private CloudEventService cloudEventService;

    private ExecutorService dbCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        dbCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("cloud-notifications"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (dbCallBackExecutor != null) {
            dbCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public void pushNotificationToCloud(TransportProtos.CloudNotificationMsgProto cloudNotificationMsg, TbCallback callback) {
        try {
            TenantId tenantId = new TenantId(new UUID(cloudNotificationMsg.getTenantIdMSB(), cloudNotificationMsg.getTenantIdLSB()));
            CloudEventType cloudEventType = CloudEventType.valueOf(cloudNotificationMsg.getCloudEventType());
            ListenableFuture<Void> future;
            switch (cloudEventType) {
                // TODO: voba - handle cloud updates
                case EDGE, ASSET, DEVICE, ASSET_PROFILE, DEVICE_PROFILE, ENTITY_VIEW, DASHBOARD, RULE_CHAIN, TB_RESOURCE ->
                        future = processEntity(tenantId, cloudNotificationMsg);
                case ALARM -> future = processAlarm(tenantId, cloudNotificationMsg);
                case RELATION -> future = processRelation(tenantId, cloudNotificationMsg);
                case ALARM_COMMENT -> future = processAlarmComment(tenantId, cloudNotificationMsg);
                default -> {
                    log.warn("Cloud event type [{}] is not designed to be pushed to cloud", cloudEventType);
                    future = Futures.immediateFuture(null);
                }
            }
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Void unused) {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    callBackFailure(cloudNotificationMsg, callback, throwable);
                }
            }, dbCallBackExecutor);
        } catch (Exception e) {
            callBackFailure(cloudNotificationMsg, callback, e);
        }
    }

    private void callBackFailure(TransportProtos.CloudNotificationMsgProto cloudNotificationMsg, TbCallback callback, Throwable throwable) {
        log.error("Can't push to cloud updates, cloudNotificationMsg [{}]", cloudNotificationMsg, throwable);
        callback.onFailure(throwable);
    }


    private ListenableFuture<Void> processEntity(TenantId tenantId, TransportProtos.CloudNotificationMsgProto cloudNotificationMsg) {
        EdgeEventActionType cloudEventActionType = EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction());
        CloudEventType cloudEventType = CloudEventType.valueOf(cloudNotificationMsg.getCloudEventType());
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEventType, new UUID(cloudNotificationMsg.getEntityIdMSB(), cloudNotificationMsg.getEntityIdLSB()));
        return switch (cloudEventActionType) {
            case ADDED, UPDATED, CREDENTIALS_UPDATED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER, DELETED ->
                    cloudEventService.saveCloudEventAsync(tenantId, cloudEventType, cloudEventActionType, entityId, null, 0L);
            default -> Futures.immediateFuture(null);
        };
    }

    private ListenableFuture<Void> processAlarm(TenantId tenantId, TransportProtos.CloudNotificationMsgProto cloudNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction());
        AlarmId alarmId = new AlarmId(new UUID(cloudNotificationMsg.getEntityIdMSB(), cloudNotificationMsg.getEntityIdLSB()));
        if (EdgeEventActionType.DELETED.equals(actionType) || EdgeEventActionType.ALARM_DELETE.equals(actionType)) {
            Alarm deletedAlarm = JacksonUtil.fromString(cloudNotificationMsg.getEntityBody(), Alarm.class);
            return cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ALARM, actionType, alarmId, JacksonUtil.valueToTree(deletedAlarm), 0L);
        }
        ListenableFuture<Alarm> future = alarmService.findAlarmByIdAsync(tenantId, alarmId);
        return Futures.transformAsync(future, alarm -> {
            if (alarm != null) {
                CloudEventType cloudEventType = CloudUtils.getCloudEventTypeByEntityType(alarm.getOriginator().getEntityType());
                if (cloudEventType != null) {
                    return cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ALARM, EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction()), alarmId, null, 0L);
                }
            }
            return Futures.immediateFuture(null);
        }, dbCallBackExecutor);
    }

    public ListenableFuture<Void> processAlarmComment(TenantId tenantId, TransportProtos.CloudNotificationMsgProto cloudNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction());
        AlarmId alarmId = new AlarmId(new UUID(cloudNotificationMsg.getEntityIdMSB(), cloudNotificationMsg.getEntityIdLSB()));
        AlarmComment alarmComment = JacksonUtil.fromString(cloudNotificationMsg.getEntityBody(), AlarmComment.class);
        if (alarmComment == null) {
            return Futures.immediateFuture(null);
        }
        return cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.ALARM_COMMENT, actionType, alarmId, JacksonUtil.valueToTree(alarmComment), 0L);
    }

    private ListenableFuture<Void> processRelation(TenantId tenantId, TransportProtos.CloudNotificationMsgProto cloudNotificationMsg) {
        EntityRelation relation = JacksonUtil.fromString(cloudNotificationMsg.getEntityBody(), EntityRelation.class);
        return cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.RELATION, EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction()), null, JacksonUtil.valueToTree(relation), 0L);
    }

}
