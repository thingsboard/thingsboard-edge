/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CloudUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.cloud.CloudEvent;
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
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultCloudNotificationService implements CloudNotificationService {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private CloudEventService cloudEventService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

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

    private void saveCloudEvent(TenantId tenantId,
                                CloudEventType cloudEventType,
                                EdgeEventActionType cloudEventAction,
                                EntityId entityId,
                                JsonNode entityBody,
                                EntityId entityGroupId) throws ExecutionException, InterruptedException {
        log.debug("Pushing event to cloud queue. tenantId [{}], cloudEventType [{}], " +
                        "cloudEventAction[{}], entityId [{}], entityBody [{}], entityGroupId [{}]",
                tenantId, cloudEventType, cloudEventAction, entityId, entityBody, entityGroupId);

        CloudEvent cloudEvent = new CloudEvent();
        cloudEvent.setTenantId(tenantId);
        cloudEvent.setCloudEventType(cloudEventType);
        cloudEvent.setCloudEventAction(cloudEventAction.name());
        if (entityId != null) {
            cloudEvent.setEntityId(entityId.getId());
        }
        if (entityGroupId != null) {
            cloudEvent.setEntityGroupId(entityGroupId.getId());
        }
        cloudEvent.setEntityBody(entityBody);
        cloudEventService.saveAsync(cloudEvent).get();
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
            String errMsg = String.format("Can't push to cloud updates, cloudNotificationMsg [%s]", cloudNotificationMsg);
            log.error(errMsg, e);
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
                saveCloudEvent(tenantId, cloudEventType, cloudEventActionType, entityId, null, entityGroupId);
                break;
        }
    }

    private void processAlarm(TenantId tenantId, TransportProtos.CloudNotificationMsgProto cloudNotificationMsg) throws Exception {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction());
        AlarmId alarmId = new AlarmId(new UUID(cloudNotificationMsg.getEntityIdMSB(), cloudNotificationMsg.getEntityIdLSB()));
        switch (actionType) {
            case DELETED:
                Alarm deletedAlarm = mapper.readValue(cloudNotificationMsg.getEntityBody(), Alarm.class);
                saveCloudEvent(tenantId,
                        CloudEventType.ALARM,
                        actionType,
                        alarmId,
                        mapper.valueToTree(deletedAlarm),
                        null);
                break;
            default:
                // TODO: @voba - improve performance by using async method properly
                Alarm alarm = alarmService.findAlarmByIdAsync(tenantId, alarmId).get();
                if (alarm != null) {
                    CloudEventType cloudEventType = CloudUtils.getCloudEventTypeByEntityType(alarm.getOriginator().getEntityType());
                    if (cloudEventType != null) {
                        saveCloudEvent(tenantId,
                                CloudEventType.ALARM,
                                EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction()),
                                alarmId,
                                null,
                                null);
                    }
                }
        }
    }

    private void processRelation(TenantId tenantId, TransportProtos.CloudNotificationMsgProto cloudNotificationMsg) throws Exception {
        EntityRelation relation = mapper.readValue(cloudNotificationMsg.getEntityBody(), EntityRelation.class);
        saveCloudEvent(tenantId,
                CloudEventType.RELATION,
                EdgeEventActionType.valueOf(cloudNotificationMsg.getCloudEventAction()),
                null,
                mapper.valueToTree(relation),
                null);
    }
}


