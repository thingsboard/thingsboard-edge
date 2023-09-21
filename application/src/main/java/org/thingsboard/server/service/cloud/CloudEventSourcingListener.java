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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.RelationActionEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.service.entitiy.DefaultTbNotificationEntityService.edgeTypeByActionType;

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
    private final EdgeSynchronizationManager edgeSynchronizationManager;

    private final List<EntityType> supportableEntityTypes = Arrays.asList(
            EntityType.DEVICE,
            EntityType.DEVICE_PROFILE,
            EntityType.ALARM,
            EntityType.ENTITY_VIEW,
            EntityType.ASSET,
            EntityType.ASSET_PROFILE,
            EntityType.DASHBOARD);

    @PostConstruct
    public void init() {
        log.info("CloudEventSourcingListener initiated");
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveEntityEvent<?> event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        try {
            if (event.getEntityId() != null && !supportableEntityTypes.contains(event.getEntityId().getEntityType())) {
                return;
            }
            log.trace("SaveEntityEvent called: {}", event);
            EdgeEventActionType action = Boolean.TRUE.equals(event.getAdded()) ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED;
            tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), event.getEntityId(),
                    null, null, action, null);
        } catch (Exception e) {
            log.error("failed to process SaveEntityEvent: {}", event);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent<?> event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        try {
            if (event.getEntityId() != null && !supportableEntityTypes.contains(event.getEntityId().getEntityType())) {
                return;
            }
            log.trace("DeleteEntityEvent called: {}", event);
            tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), event.getEntityId(),
                    JacksonUtil.toString(event.getEntity()), null, EdgeEventActionType.DELETED, null);
        } catch (Exception e) {
            log.error("failed to process DeleteEntityEvent: {}", event);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        try {
            if (event.getEntityId() != null && !supportableEntityTypes.contains(event.getEntityId().getEntityType())) {
                return;
            }
            if (event.getEntityGroup() != null) {
                if (event.getEntityGroup().isGroupAll()) {
                    log.trace("skipping entity in case of 'All' group: {}", event);
                    return;
                }
            }
            log.trace("ActionEntityEvent called: {}", event);
            EntityGroupId entityGroupId = event.getEntityGroup() != null ? event.getEntityGroup().getId() : null;
            tbClusterService.sendNotificationMsgToCloud(event.getTenantId(), event.getEntityId(),
                    event.getBody(), null, edgeTypeByActionType(event.getActionType()), entityGroupId);
        } catch (Exception e) {
            log.error("failed to process ActionEntityEvent: {}", event);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(RelationActionEvent event) {
        if (edgeSynchronizationManager.isSync()) {
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
                    JacksonUtil.toString(event.getRelation()), CloudEventType.RELATION, edgeTypeByActionType(event.getActionType()), null);
        } catch (Exception e) {
            log.error("failed to process RelationActionEvent: {}", event);
        }
    }
}
