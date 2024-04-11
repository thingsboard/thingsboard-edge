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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.cloud.CloudSynchronizationManager;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.RelationActionEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This event listener does not support async event processing because relay on ThreadLocal
 * Another possible approach is to implement a special annotation and a bunch of classes similar to TransactionalApplicationListener
 * This class is the simplest approach to maintain cloud synchronization within the single class.
 * <p>
 * For async event publishers, you have to decide whether publish event on creating async task in the same thread where dao method called
 * @Autowired
 * EdgeEventSynchronizationManager edgeSynchronizationManager
 * ...
 *   //some async write action make future
 *   if (!edgeSynchronizationManager.isSync()) {
 *     future.addCallback(eventPublisher.publishEvent(...))
 *   }
 * */
@Component
@RequiredArgsConstructor
@Slf4j
public class CloudEventSourcingListener {

    private final TbClusterService tbClusterService;
    private final CloudSynchronizationManager cloudSynchronizationManager;

    private static final List<EntityType> COMMON_ENTITY_TYPES = Arrays.asList(
            EntityType.DEVICE,
            EntityType.DEVICE_PROFILE,
            EntityType.ENTITY_VIEW,
            EntityType.ASSET,
            EntityType.ASSET_PROFILE,
            EntityType.DASHBOARD,
            EntityType.TB_RESOURCE);

    private final List<EntityType> supportableEntityTypes = new ArrayList<>(COMMON_ENTITY_TYPES) {{
        add(EntityType.ALARM);
    }};

    private final List<EntityType> saveEventSupportableEntityTypes = new ArrayList<>(COMMON_ENTITY_TYPES);

    @PostConstruct
    public void init() {
        log.info("EdgeEventSourcingListener initiated");
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveEntityEvent<?> event) {
        if (cloudSynchronizationManager.isSync()) {
            return;
        }
        try {
            if (event.getEntityId() != null && !saveEventSupportableEntityTypes.contains(event.getEntityId().getEntityType())
                    && !(event.getEntity() instanceof AlarmComment)) {
                return;
            }
            log.trace("SaveEntityEvent called: {}", event);
            boolean isCreated = Boolean.TRUE.equals(event.getCreated());
            String body = getBodyMsgForEntityEvent(event.getEntity());
            CloudEventType cloudEventType = getCloudEventTypeForEntityEvent(event.getEntity());
            EdgeEventActionType action = getActionForEntityEvent(event.getEntity(), isCreated);
            tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), event.getEntityId(),
                    body, cloudEventType, action);
        } catch (Exception e) {
            log.error("failed to process SaveEntityEvent: {}", event);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent<?> event) {
        if (cloudSynchronizationManager.isSync()) {
            return;
        }
        try {
            if (event.getEntityId() != null && !supportableEntityTypes.contains(event.getEntityId().getEntityType())
                    && !(event.getEntity() instanceof AlarmComment)) {
                return;
            }
            log.trace("DeleteEntityEvent called: {}", event);
            CloudEventType type = getCloudEventTypeForEntityEvent(event.getEntity());
            EdgeEventActionType actionType = getEdgeEventActionTypeForEntityEvent(event.getEntity());
            tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), event.getEntityId(),
                    JacksonUtil.toString(event.getEntity()), type, actionType);
        } catch (Exception e) {
            log.error("failed to process DeleteEntityEvent: {}", event);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent<?> event) {
        if (cloudSynchronizationManager.isSync()) {
            return;
        }
        try {
            if (event.getEntityId() != null && !supportableEntityTypes.contains(event.getEntityId().getEntityType())) {
                return;
            }
            log.trace("ActionEntityEvent called: {}", event);
            tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), event.getEntityId(),
                    event.getBody(), null, EdgeUtils.getEdgeEventActionTypeByActionType(event.getActionType()));
        } catch (Exception e) {
            log.error("failed to process ActionEntityEvent: {}", event);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(RelationActionEvent event) {
        if (cloudSynchronizationManager.isSync()) {
            return;
        }
        try {
            EntityRelation relation = event.getRelation();
            if (relation == null) {
                log.trace("[{}] skipping RelationActionEvent event in case relation is null: {}", event.getTenantId(), event);
                return;
            }
            if (!RelationTypeGroup.COMMON.equals(relation.getTypeGroup())) {
                log.trace("[{}] skipping RelationActionEvent event in case NOT COMMON relation type group: {}", event.getTenantId(), event);
                return;
            }
            log.trace("RelationActionEvent called: {}", event);
            tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), null,
                    JacksonUtil.toString(event.getRelation()), CloudEventType.RELATION, EdgeUtils.getEdgeEventActionTypeByActionType(event.getActionType()));
        } catch (Exception e) {
            log.error("failed to process RelationActionEvent: {}", event);
        }
    }

    private CloudEventType getCloudEventTypeForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment) {
            return CloudEventType.ALARM_COMMENT;
        }
        return null;
    }

    private EdgeEventActionType getEdgeEventActionTypeForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment) {
            return EdgeEventActionType.DELETED_COMMENT;
        } else if (entity instanceof Alarm) {
            return EdgeEventActionType.ALARM_DELETE;
        }
        return EdgeEventActionType.DELETED;
    }

    private String getBodyMsgForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment) {
            return JacksonUtil.toString(entity);
        }
        return null;
    }

    private EdgeEventActionType getActionForEntityEvent(Object entity, boolean isCreated) {
        if (entity instanceof AlarmComment) {
            return isCreated ? EdgeEventActionType.ADDED_COMMENT : EdgeEventActionType.UPDATED_COMMENT;
        }
        return isCreated ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED;
    }

}
