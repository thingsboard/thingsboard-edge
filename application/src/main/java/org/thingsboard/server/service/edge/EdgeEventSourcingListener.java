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
package org.thingsboard.server.service.edge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.ActionRelationEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import javax.annotation.PostConstruct;

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
public class EdgeEventSourcingListener {

    private final TbClusterService tbClusterService;
    private final EdgeSynchronizationManager edgeSynchronizationManager;

    @PostConstruct
    public void init() {
        log.info("EdgeEventSourcingListener initiated");
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveEntityEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        log.trace("[{}] EntitySaveEvent called: {}", event.getEntityId().getEntityType(), event);
        EdgeEventActionType action = Boolean.TRUE.equals(event.getAdded()) ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED;
        tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, event.getEntityId(),
                null, null, action);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        log.trace("[{}] EntityDeleteEvent called: {}", event.getEntityId().getEntityType(), event);
        tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), event.getEdgeId(), event.getEntityId(),
                null, null, EdgeEventActionType.DELETED);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        log.trace("[{}] EntityActionEvent called: {}", event.getEntityId().getEntityType(), event);
        tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), event.getEdgeId(), event.getEntityId(),
                event.getBody(), null, edgeTypeByActionType(event.getActionType()));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionRelationEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        log.trace("EntityRelationActionEvent called: {}", event);
        tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, null,
                event.getBody(), EdgeEventType.RELATION, edgeTypeByActionType(event.getActionType()));
    }
}
