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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.sync.ie.EntityGroupExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.ArrayList;
import java.util.List;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EntityGroupImportService extends BaseEntityImportService<EntityGroupId, EntityGroup, EntityGroupExportData> {

    private final EntityGroupService entityGroupService;
    private final GroupPermissionService groupPermissionService;
    private final RoleService roleService;
    private final UserPermissionsService userPermissionsService;

    @Override
    protected void setOwner(TenantId tenantId, EntityGroup entityGroup, IdProvider idProvider) {
        if (entityGroup.getOwnerId() instanceof TenantId) {
            entityGroup.setOwnerId(tenantId);
        } else {
            entityGroup.setOwnerId(idProvider.getInternalId(entityGroup.getOwnerId()));
        }
    }

    @Override
    protected EntityGroup findExistingEntity(TenantId tenantId, EntityGroup entityGroup, EntityImportSettings importSettings) {
        EntityGroup existingEntityGroup = super.findExistingEntity(tenantId, entityGroup, importSettings);
        if (existingEntityGroup == null && importSettings.isFindExistingByName()) {
            EntityId ownerId;
            if (entityGroup.getOwnerId().getEntityType() == EntityType.TENANT) {
                ownerId = tenantId;
            } else {
                ownerId = findInternalEntity(tenantId, entityGroup.getOwnerId()).getId();
            }
            existingEntityGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, ownerId,
                    entityGroup.getType(), entityGroup.getName()).orElse(null);
        }
        return existingEntityGroup;
    }

    @Override
    protected EntityGroup prepareAndSave(TenantId tenantId, EntityGroup entityGroup, EntityGroup old, EntityGroupExportData exportData, IdProvider idProvider, EntityImportSettings importSettings) {
        if (entityGroup.getId() == null && entityGroup.isGroupAll()) {
            throw new IllegalArgumentException("Import of new groups with type All is not allowed. " +
                    "Consider enabling import option to find existing entities by name");
        }
        // TODO [viacheslav]: update actions config
        return entityGroupService.saveEntityGroup(tenantId, entityGroup.getOwnerId(), entityGroup);
    }

    @Override
    protected void processAfterSaved(SecurityUser user, EntityImportResult<EntityGroup> importResult, EntityGroupExportData exportData,
                                     IdProvider idProvider, EntityImportSettings importSettings) throws ThingsboardException {
        super.processAfterSaved(user, importResult, exportData, idProvider, importSettings);

        importResult.addSaveReferencesCallback(() -> {
            if (!importSettings.isSaveUserGroupPermissions() || exportData.getPermissions() == null
                    || importResult.getSavedEntity().getType() != EntityType.USER) {
                return;
            }

            EntityGroup userGroup = importResult.getSavedEntity();
            List<GroupPermission> permissions = new ArrayList<>(exportData.getPermissions());

            for (GroupPermission permission : permissions) {
                permission.setId(null);
                permission.setTenantId(user.getTenantId());
                permission.setRoleId(idProvider.getInternalId(permission.getRoleId()));
                permission.setUserGroupId(idProvider.getInternalId(permission.getUserGroupId()));
                permission.setEntityGroupId(idProvider.getInternalId(permission.getEntityGroupId()));
            }

            if (importResult.getOldEntity() != null) {
                List<GroupPermission> existingPermissions = new ArrayList<>(groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(user.getTenantId(), userGroup.getId()));

                for (GroupPermission existingPermission : existingPermissions) {
                    if (permissions.stream().noneMatch(permission -> permissionsEqual(permission, existingPermission))) {
                        if (existingPermission.isPublic()) continue;
                        Role role = roleService.findRoleById(user.getTenantId(), existingPermission.getRoleId());
                        if (role.getOwnerId().equals(TenantId.SYS_TENANT_ID)) continue;

                        exportableEntitiesService.checkPermission(user, existingPermission, EntityType.GROUP_PERMISSION, Operation.DELETE);
                        exportableEntitiesService.checkPermission(user, role, EntityType.ROLE, Operation.READ);
                        exportableEntitiesService.checkPermission(user, existingPermission.getUserGroupId(), Operation.WRITE);
                        if (existingPermission.getEntityGroupId() != null && !existingPermission.getEntityGroupId().isNullUid()) {
                            exportableEntitiesService.checkPermission(user, existingPermission.getEntityGroupId(), Operation.WRITE);
                        }

                        groupPermissionService.deleteGroupPermission(user.getTenantId(), existingPermission.getId());

                        importResult.addSendEventsCallback(() -> {
                            userPermissionsService.onGroupPermissionDeleted(existingPermission);
                            entityActionService.logEntityAction(user, existingPermission.getId(), existingPermission,
                                    null, ActionType.DELETED, null, existingPermission.getId().toString());
                            entityActionService.sendEntityNotificationMsgToEdgeService(user.getTenantId(),
                                    existingPermission.getId(), EdgeEventActionType.DELETED);
                        });
                    } else {
                        permissions.removeIf(permission -> permissionsEqual(permission, existingPermission));
                    }
                }
            }

            for (GroupPermission permission : permissions) {
                if (permission.isPublic()) {
                    throw new IllegalArgumentException("Import of public permissions is not supported");
                }
                Role role = roleService.findRoleById(user.getTenantId(), permission.getRoleId());
                if (role.getOwnerId().equals(TenantId.SYS_TENANT_ID)) continue;

                exportableEntitiesService.checkPermission(user, permission, EntityType.GROUP_PERMISSION, Operation.CREATE);
                exportableEntitiesService.checkPermission(user, role, EntityType.ROLE, Operation.READ);
                exportableEntitiesService.checkPermission(user, permission.getUserGroupId(), Operation.WRITE);
                if (permission.getEntityGroupId() != null && !permission.getEntityGroupId().isNullUid()) {
                    if (role.getType() == RoleType.GENERIC) {
                        throw new IllegalArgumentException("Cannot assign generic role to entity group");
                    }
                    exportableEntitiesService.checkPermission(user, permission.getEntityGroupId(), Operation.WRITE);
                }

                GroupPermission savedPermission = groupPermissionService.saveGroupPermission(user.getTenantId(), permission);

                importResult.addSendEventsCallback(() -> {
                    userPermissionsService.onGroupPermissionUpdated(savedPermission);
                    entityActionService.logEntityAction(user, savedPermission.getId(), savedPermission,
                            null, ActionType.ADDED, null);
                    entityActionService.sendEntityNotificationMsgToEdgeService(user.getTenantId(),
                            savedPermission.getId(), EdgeEventActionType.ADDED);
                });
            }
        });
    }

    private boolean permissionsEqual(GroupPermission first, GroupPermission second) {
        return first.getUserGroupId().equals(second.getUserGroupId())
                && (first.getEntityGroupId() == null ? EntityId.NULL_UUID : first.getEntityGroupId().getId()).equals(
                second.getEntityGroupId() == null ? EntityId.NULL_UUID : second.getEntityGroupId().getId())
                && first.getRoleId().equals(second.getRoleId());
    }

    @Override
    protected void onEntitySaved(SecurityUser user, EntityGroup savedEntityGroup, EntityGroup oldEntityGroup) throws ThingsboardException {
        super.onEntitySaved(user, savedEntityGroup, oldEntityGroup);
        if (oldEntityGroup != null) {
            entityActionService.sendEntityNotificationMsgToEdgeService(user.getTenantId(), savedEntityGroup.getId(), EdgeEventActionType.UPDATED);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_GROUP;
    }

}
