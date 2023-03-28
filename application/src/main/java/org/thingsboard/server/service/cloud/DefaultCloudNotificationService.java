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
package org.thingsboard.server.service.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.CloudUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityGroupId;
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
            case ADDED_TO_ENTITY_GROUP:
            case DELETED:
            case REMOVED_FROM_ENTITY_GROUP:
                EntityGroupId entityGroupId = null;
                if (cloudNotificationMsg.getEntityGroupIdMSB() != 0 && cloudNotificationMsg.getEntityGroupIdLSB() != 0) {
                    entityGroupId = new EntityGroupId(
                            new UUID(cloudNotificationMsg.getEntityGroupIdMSB(), cloudNotificationMsg.getEntityGroupIdLSB()));
                }
                try {
                    cloudEventService.saveCloudEvent(tenantId, cloudEventType, cloudEventActionType, entityId, null, entityGroupId, 0L);
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
                        null,
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
                null,
                0L);
    }
}


