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
package org.thingsboard.server.service.sync.importing.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.exporting.data.GroupEntityExportData;
import org.thingsboard.server.service.sync.importing.EntityImportSettings;
import org.thingsboard.server.utils.ThrowingRunnable;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseGroupEntityImportService<I extends EntityId, E extends ExportableEntity<I> & GroupEntity<I>, D extends GroupEntityExportData<E>> extends BaseEntityImportService<I, E, D> {

    @Autowired
    private EntityGroupService entityGroupService;

    @Override
    protected ThrowingRunnable processAfterSavedAndGetCallback(SecurityUser user, E savedEntity, E oldEntity, D exportData, EntityImportSettings importSettings, NewIdProvider idProvider) throws ThingsboardException {
        ThrowingRunnable callback = getCallback(user, savedEntity, oldEntity);

        if (importSettings.isAddToEntityGroups() && importSettings.isUpdateReferencesToOtherEntities()) {
            List<EntityGroupId> entityGroupsIds = exportData.getEntityGroupsIds().stream()
                    .map(entityGroupId -> idProvider.get(e -> entityGroupId))
                    .collect(Collectors.toList());

            for (EntityGroupId entityGroupId : entityGroupsIds) {
                EntityGroup entityGroup = exportableEntitiesService.findEntityByTenantIdAndId(user.getTenantId(), entityGroupId);

                exportableEntitiesService.checkPermission(user, entityGroup, EntityType.ENTITY_GROUP, Operation.READ);
                if (oldEntity == null) {
                    exportableEntitiesService.checkPermission(user, savedEntity, entityGroupId, Operation.CREATE);
                } else {
                    exportableEntitiesService.checkPermission(user, savedEntity, entityGroupId, Operation.WRITE);
                    exportableEntitiesService.checkPermission(user, entityGroup, EntityType.ENTITY_GROUP, Operation.ADD_TO_GROUP);
                }

                if (!entityGroupService.isEntityInGroup(savedEntity.getId(), entityGroupId)) {
                    entityGroupService.addEntityToEntityGroup(user.getTenantId(), entityGroupId, savedEntity.getId());
                    callback = callback.andThen(() -> {
                        entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity,
                                savedEntity.getCustomerId(), ActionType.ADDED_TO_ENTITY_GROUP, null,
                                savedEntity.getId().toString(), entityGroupId.toString(), entityGroup.getName());
                        entityActionService.sendGroupEntityNotificationMsgToEdgeService(user.getTenantId(), savedEntity.getId(), entityGroupId, EdgeEventActionType.ADDED_TO_ENTITY_GROUP);
                    });
                }
            }
        }

        return callback;
    }

    @Override
    protected ThrowingRunnable getCallback(SecurityUser user, E savedEntity, E oldEntity) {
        return super.getCallback(user, savedEntity, oldEntity).andThen(() -> {
            if (oldEntity != null) {
                entityActionService.sendEntityNotificationMsgToEdgeService(user.getTenantId(), savedEntity.getId(), EdgeEventActionType.UPDATED);
            }
        });
    }

}
