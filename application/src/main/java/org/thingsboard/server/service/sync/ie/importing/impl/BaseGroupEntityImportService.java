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
package org.thingsboard.server.service.sync.ie.importing.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.ie.GroupEntityExportData;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.service.security.model.SecurityUser;

public abstract class BaseGroupEntityImportService<I extends EntityId, E extends ExportableEntity<I> & GroupEntity<I>, D extends GroupEntityExportData<E>> extends BaseEntityImportService<I, E, D> {

    @Autowired
    private EntityGroupService entityGroupService;

    @Override
    protected void processAfterSaved(SecurityUser user, EntityImportResult<E> importResult, D exportData,
                                     IdProvider idProvider, EntityImportSettings importSettings) throws ThingsboardException {
        super.processAfterSaved(user, importResult, exportData, idProvider, importSettings);

        importResult.addSaveReferencesCallback(() -> {
//            if (!importSettings.isUpdateEntityGroups() || exportData.getEntityGroupsIds() == null) {
//                return;
//            }
//
//            E savedEntity = importResult.getSavedEntity();
//            E oldEntity = importResult.getOldEntity();
//
//            List<EntityGroupId> entityGroupsIds = exportData.getEntityGroupsIds().stream()
//                    .map(idProvider::getInternalId)
//                    .collect(Collectors.toList());
//
//            if (oldEntity != null) {
//                List<EntityGroupId> existingEntityGroupsIds;
//                try {
//                    existingEntityGroupsIds = entityGroupService.findEntityGroupsForEntity(user.getTenantId(), savedEntity.getId()).get();
//                } catch (InterruptedException | ExecutionException e) {
//                    throw new RuntimeException(e);
//                }
//
//                for (EntityGroupId existingEntityGroupId : existingEntityGroupsIds) {
//                    if (!entityGroupsIds.contains(existingEntityGroupId)) {
//                        EntityGroup existingEntityGroup = exportableEntitiesService.findEntityByTenantIdAndId(user.getTenantId(), existingEntityGroupId);
//                        if (existingEntityGroup.isGroupAll()) continue;
//
//                        exportableEntitiesService.checkPermission(user, savedEntity, existingEntityGroupId, Operation.WRITE);
//                        exportableEntitiesService.checkPermission(user, existingEntityGroup, EntityType.ENTITY_GROUP, Operation.REMOVE_FROM_GROUP);
//
//                        entityGroupService.removeEntityFromEntityGroup(user.getTenantId(), existingEntityGroupId, savedEntity.getId());
//
//                        importResult.addSendEventsCallback(() -> {
//                            entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity,
//                                    savedEntity.getCustomerId(), ActionType.REMOVED_FROM_ENTITY_GROUP, null,
//                                    savedEntity.getId().toString(), existingEntityGroupId.toString(), existingEntityGroup.getName());
//                            entityActionService.sendGroupEntityNotificationMsgToEdgeService(user.getTenantId(), savedEntity.getId(),
//                                    existingEntityGroupId, EdgeEventActionType.REMOVED_FROM_ENTITY_GROUP);
//                        });
//                    }
//                }
//
//                entityGroupsIds.removeAll(existingEntityGroupsIds);
//            }
//
//            for (EntityGroupId entityGroupId : entityGroupsIds) {
//                EntityGroup entityGroup = exportableEntitiesService.findEntityByTenantIdAndId(user.getTenantId(), entityGroupId);
//                if (entityGroup.isGroupAll()) continue;
//                if (entityGroup.getType() != savedEntity.getEntityType()) {
//                    throw new IllegalArgumentException("Cannot add entity to group of different entity type");
//                }
//
//                exportableEntitiesService.checkPermission(user, entityGroup, EntityType.ENTITY_GROUP, Operation.READ);
//                exportableEntitiesService.checkPermission(user, savedEntity, entityGroup.getId(), Operation.WRITE);
//                exportableEntitiesService.checkPermission(user, entityGroup, EntityType.ENTITY_GROUP, Operation.ADD_TO_GROUP);
//
//                entityGroupService.addEntityToEntityGroup(user.getTenantId(), entityGroup.getId(), savedEntity.getId());
//
//                importResult.addSendEventsCallback(() -> {
//                    entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity,
//                            savedEntity.getCustomerId(), ActionType.ADDED_TO_ENTITY_GROUP, null,
//                            savedEntity.getId().toString(), entityGroup.getId().toString(), entityGroup.getName());
//                    entityActionService.sendGroupEntityNotificationMsgToEdgeService(user.getTenantId(), savedEntity.getId(),
//                            entityGroup.getId(), EdgeEventActionType.ADDED_TO_ENTITY_GROUP);
//                });
//            }
        });
    }

    @Override
    protected void onEntitySaved(SecurityUser user, E savedEntity, E oldEntity) throws ThingsboardException {
        super.onEntitySaved(user, savedEntity, oldEntity);
        if (oldEntity != null) {
            entityActionService.sendEntityNotificationMsgToEdgeService(user.getTenantId(), savedEntity.getId(), EdgeEventActionType.UPDATED);
        }
    }

}
