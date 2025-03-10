/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.edge.RelatedEdgesService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelatedEdgesSourcingListener {

    private final RelatedEdgesService relatedEdgesService;
    private final EntityGroupService entityGroupService;

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        log.debug("RelatedEdgesSourcingListener initiated");
        executorService = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("related-edges-listener"));
    }

    @PreDestroy
    public void destroy() {
        log.debug("RelatedEdgesSourcingListener destroy");
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent<?> event) {
        executorService.submit(() -> {
            log.trace("[{}] ActionEntityEvent called: {}", event.getTenantId(), event);
            try {
                switch (event.getActionType()) {
                    case ASSIGNED_TO_EDGE, UNASSIGNED_FROM_EDGE -> {
                        if (EntityType.ENTITY_GROUP.equals(event.getEntityId().getEntityType())) {
                            List<EntityId> entityIds = entityGroupService.findAllEntityIdsAsync(event.getTenantId(), (EntityGroupId) event.getEntityId(), new PageLink(Integer.MAX_VALUE)).get();
                            entityIds.forEach(entityId -> relatedEdgesService.publishRelatedEdgeIdsEvictEvent(event.getTenantId(), entityId));
                        }
                        relatedEdgesService.publishRelatedEdgeIdsEvictEvent(event.getTenantId(), event.getEntityId());
                    }
                    case ADDED_TO_ENTITY_GROUP, REMOVED_FROM_ENTITY_GROUP -> {
                        relatedEdgesService.publishRelatedEdgeIdsEvictEvent(event.getTenantId(), event.getEntityId());
                    }
                }
            } catch (Exception e) {
                log.error("[{}] failed to process ActionEntityEvent: {}", event.getTenantId(), event, e);
            }
        });
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent<?> event) {
        executorService.submit(() -> {
            log.trace("[{}] DeleteEntityEvent called: {}", event.getTenantId(), event);
            try {
                if (EntityType.ENTITY_GROUP.equals(event.getEntityId().getEntityType())) {
                    relatedEdgesService.publishEdgeIdsEvictEventByTenantId(event.getTenantId());
                }
                relatedEdgesService.publishRelatedEdgeIdsEvictEvent(event.getTenantId(), event.getEntityId());
            } catch (Exception e) {
                log.error("[{}] failed to process DeleteEntityEvent: {}", event.getTenantId(), event, e);
            }
        });
    }

}
