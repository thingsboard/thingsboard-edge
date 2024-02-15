/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.housekeeper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.housekeeper.data.HousekeeperTask;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanUpService {

    private final HousekeeperService housekeeperService;
    private final RelationService relationService;

    @TransactionalEventListener(fallbackExecution = true) // todo: consider moving this to HousekeeperService
    public void handleEntityDeletionEvent(DeleteEntityEvent<?> event) {
        TenantId tenantId = event.getTenantId();
        EntityId entityId = event.getEntityId();
        log.trace("[{}] DeleteEntityEvent handler: {}", tenantId, event);

        log.info("[{}][{}][{}] Handling DeleteEntityEvent", tenantId, entityId.getEntityType(), entityId.getId());
        cleanUpRelatedData(tenantId, entityId);
        if (entityId.getEntityType() == EntityType.USER) {
            housekeeperService.submitTask(HousekeeperTask.unassignAlarms((User) event.getEntity()));
        }
    }

    public void cleanUpRelatedData(TenantId tenantId, EntityId entityId) {
        // todo: skipped entities list
        relationService.deleteEntityRelations(tenantId, entityId);
        housekeeperService.submitTask(HousekeeperTask.deleteAttributes(tenantId, entityId));
        housekeeperService.submitTask(HousekeeperTask.deleteTelemetry(tenantId, entityId));
        housekeeperService.submitTask(HousekeeperTask.deleteEvents(tenantId, entityId));
        housekeeperService.submitTask(HousekeeperTask.deleteEntityAlarms(tenantId, entityId));
    }

    public void removeTenantEntities(TenantId tenantId, EntityType... entityTypes) {
        UUID tasksKey = UUID.randomUUID(); // so that all tasks are processed synchronously from one partition
        for (EntityType entityType : entityTypes) {
            housekeeperService.submitTask(tasksKey, HousekeeperTask.deleteEntities(tenantId, entityType));
        }
    }

}
