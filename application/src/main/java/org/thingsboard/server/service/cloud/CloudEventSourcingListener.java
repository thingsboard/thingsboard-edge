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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.ActionRelationEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import javax.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.service.entitiy.DefaultTbNotificationEntityService.edgeTypeByActionType;

/**
 * This event listener does not support async event processing because relay on ThreadLocal
 * Another possible approach is to implement a special annotation and a bunch of classes similar to TransactionalApplicationListener
 * This class is the simplest approach to maintain replica synchronization within the single class.
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
    private final EdgeSynchronizationManager edgeSynchronizationManager;

    private List<EntityType> supportableEntityTypes = Arrays.asList(EntityType.DEVICE, EntityType.ALARM);

    @PostConstruct
    public void init() {
        log.info("EdgeEventSourcingListener initiated");
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveEntityEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        if (event.getEntityId() != null && !supportableEntityTypes.contains(event.getEntityId().getEntityType())) {
            return;
        }
        log.trace("[{}] EntitySaveEvent called: {}", event.getEntityId().getEntityType(), event);
        EdgeEventActionType action = Boolean.TRUE.equals(event.getAdded()) ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED;
        tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), event.getEntityId(),
                null, null, action, null);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        if (event.getEntityId() != null && !supportableEntityTypes.contains(event.getEntityId().getEntityType())) {
            return;
        }
        log.trace("[{}] EntityDeleteEvent called: {}", event.getEntityId().getEntityType(), event);
        tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), event.getEntityId(),
                event.getBody(), null, EdgeEventActionType.DELETED, null);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        if (event.getEntityId() != null && !supportableEntityTypes.contains(event.getEntityId().getEntityType())) {
            return;
        }
        log.trace("[{}] EntityActionEvent called: {}", event.getEntityId().getEntityType(), event);
        tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), event.getEntityId(),
                event.getBody(), null, edgeTypeByActionType(event.getActionType()), event.getEntityGroupId());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionRelationEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        log.trace("EntityRelationActionEvent called: {}", event);
        tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), null,
                event.getBody(), CloudEventType.RELATION, edgeTypeByActionType(event.getActionType()), null);
    }
}
