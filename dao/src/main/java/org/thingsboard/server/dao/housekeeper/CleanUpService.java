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
package org.thingsboard.server.dao.housekeeper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.msg.housekeeper.HousekeeperClient;
import org.thingsboard.server.dao.eventsourcing.ActionCause;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanUpService {

    private final Optional<HousekeeperClient> housekeeperClient;
    private final RelationService relationService;

    private final Set<EntityType> skippedEntities = EnumSet.of(
            EntityType.ALARM, EntityType.QUEUE, EntityType.TB_RESOURCE, EntityType.OTA_PACKAGE,
            EntityType.NOTIFICATION_REQUEST, EntityType.NOTIFICATION_TEMPLATE,
            EntityType.NOTIFICATION_TARGET, EntityType.NOTIFICATION_RULE
    );

    @TransactionalEventListener(fallbackExecution = true) // after transaction commit
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handleEntityDeletionEvent(DeleteEntityEvent<?> event) {
        TenantId tenantId = event.getTenantId();
        EntityId entityId = event.getEntityId();
        EntityType entityType = entityId.getEntityType();
        try {
            log.trace("[{}][{}][{}] Handling entity deletion event", tenantId, entityType, entityId.getId());
            if (shouldCleanUp(entityType, event.getEntity())) {
                cleanUpRelatedData(tenantId, entityId);
            }
            if (entityType == EntityType.USER && event.getCause() != ActionCause.TENANT_DELETION) {
                submitTask(HousekeeperTask.unassignAlarms((User) event.getEntity()));
            }
        } catch (Throwable e) {
            log.error("[{}][{}][{}] Failed to handle entity deletion event", tenantId, entityType, entityId.getId(), e);
        }
    }

    private boolean shouldCleanUp(EntityType entityType, Object entity) {
        return !skippedEntities.contains(entityType) && !(EntityType.ENTITY_GROUP.equals(entityType) && entity instanceof DeviceGroupOtaPackage);
    }

    public void cleanUpRelatedData(TenantId tenantId, EntityId entityId) {
        log.debug("[{}][{}][{}] Cleaning up related data", tenantId, entityId.getEntityType(), entityId.getId());
        relationService.deleteEntityRelations(tenantId, entityId);
        submitTask(HousekeeperTask.deleteAttributes(tenantId, entityId));
        submitTask(HousekeeperTask.deleteTelemetry(tenantId, entityId));
        submitTask(HousekeeperTask.deleteEvents(tenantId, entityId));
        submitTask(HousekeeperTask.deleteAlarms(tenantId, entityId));
        submitTask(HousekeeperTask.deleteCalculatedFields(tenantId, entityId));
        if (Job.SUPPORTED_ENTITY_TYPES.contains(entityId.getEntityType())) {
            submitTask(HousekeeperTask.deleteJobs(tenantId, entityId));
        }
    }

    public void removeTenantEntities(TenantId tenantId, EntityType... entityTypes) {
        for (EntityType entityType : entityTypes) {
            submitTask(HousekeeperTask.deleteTenantEntities(tenantId, entityType));
        }
    }

    private void submitTask(HousekeeperTask task) {
        housekeeperClient.ifPresent(housekeeperClient -> {
            housekeeperClient.submitTask(task);
        });
    }

}
