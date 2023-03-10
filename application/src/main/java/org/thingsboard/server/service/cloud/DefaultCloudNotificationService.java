/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.CloudUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
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

    private ExecutorService tsCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public void pushNotificationToCloud(TransportProtos.CloudNotificationMsgProto cloudNotificationMsg, TbCallback callback) {
        try {
            TenantId tenantId = new TenantId(new UUID(cloudNotificationMsg.getTenantIdMSB(), cloudNotificationMsg.getTenantIdLSB()));
            CloudEventType cloudEventType = CloudEventType.valueOf(cloudNotificationMsg.getCloudEventType());
            switch (cloudEventType) {
                // TODO: voba - handle cloud updates
                case EDGE:
                case ASSET:
                case DEVICE:
                case ENTITY_VIEW:
                case DASHBOARD:
                case RULE_CHAIN:
                    processEntity(tenantId, cloudNotificationMsg);
                    break;
                case ALARM:
                    processAlarm(tenantId, cloudNotificationMsg);
                    break;
                case RELATION:
                    processRelation(tenantId, cloudNotificationMsg);
                    break;
                default:
                    log.debug("Cloud event type [{}] is not designed to be pushed to cloud", cloudEventType);
            }
        } catch (Exception e) {
            log.error("Can't push to cloud updates, cloudNotificationMsg [{}]", cloudNotificationMsg, e);
            callback.onFailure(e);
        } finally {
            callback.onSuccess();
        }
    }

    private void processEntity(TenantId tenantId, TransportProtos.CloudNotificationMsgProto cloudNotificationMsg) throws Exception {
        EdgeEventActionType cloudEventActionType = EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction());
        CloudEventType cloudEventType = CloudEventType.valueOf(cloudNotificationMsg.getCloudEventType());
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEventType, new UUID(cloudNotificationMsg.getEntityIdMSB(), cloudNotificationMsg.getEntityIdLSB()));
        switch (cloudEventActionType) {
            case ADDED:
            case UPDATED:
            case CREDENTIALS_UPDATED:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
            case DELETED:
                try {
                    cloudEventService.saveCloudEvent(tenantId, cloudEventType, cloudEventActionType, entityId, null, 0L);
                } catch (Exception e) {
                    log.error("[{}] Failed to push event to cloud [{}], cloudEventType [{}], cloudEventActionType [{}], entityId [{}]",
                            tenantId, cloudEventType, cloudEventActionType, entityId, e);
                }
                break;
        }
    }

    private void processAlarm(TenantId tenantId, TransportProtos.CloudNotificationMsgProto cloudNotificationMsg) throws Exception {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction());
        AlarmId alarmId = new AlarmId(new UUID(cloudNotificationMsg.getEntityIdMSB(), cloudNotificationMsg.getEntityIdLSB()));
        switch (actionType) {
            case DELETED:
                Alarm deletedAlarm = JacksonUtil.OBJECT_MAPPER.readValue(cloudNotificationMsg.getEntityBody(), Alarm.class);
                cloudEventService.saveCloudEvent(tenantId,
                        CloudEventType.ALARM,
                        actionType,
                        alarmId,
                        JacksonUtil.OBJECT_MAPPER.valueToTree(deletedAlarm),
                        0L);
                break;
            default:
                // TODO: @voba - improve performance by using async method properly
                Alarm alarm = alarmService.findAlarmByIdAsync(tenantId, alarmId).get();
                if (alarm != null) {
                    CloudEventType cloudEventType = CloudUtils.getCloudEventTypeByEntityType(alarm.getOriginator().getEntityType());
                    if (cloudEventType != null) {
                        cloudEventService.saveCloudEvent(tenantId,
                                CloudEventType.ALARM,
                                EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction()),
                                alarmId,
                                null,
                                0L);
                    }
                }
        }
    }

    private void processRelation(TenantId tenantId, TransportProtos.CloudNotificationMsgProto cloudNotificationMsg) throws Exception {
        EntityRelation relation = JacksonUtil.OBJECT_MAPPER.readValue(cloudNotificationMsg.getEntityBody(), EntityRelation.class);
        cloudEventService.saveCloudEvent(tenantId,
                CloudEventType.RELATION,
                EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction()),
                null,
                JacksonUtil.OBJECT_MAPPER.valueToTree(relation),
                0L);
    }
}


