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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RoleImportService extends BaseEntityImportService<RoleId, Role, EntityExportData<Role>> {

    private final RoleService roleService;
    private final UserPermissionsService userPermissionsService;

    @Override
    protected void setOwner(TenantId tenantId, Role role, IdProvider idProvider) {
        role.setTenantId(tenantId);
        role.setCustomerId(idProvider.getInternalId(role.getCustomerId()));
    }

    @Override
    protected Role findExistingEntity(EntitiesImportCtx ctx, Role role, IdProvider idProvider) {
        Role existingRole = super.findExistingEntity(ctx, role, idProvider);
        if (existingRole == null && ctx.isFindExistingByName()) {
            var tenantId = ctx.getTenantId();
            if (role.getOwnerId() == null || role.getOwnerId().getEntityType() == EntityType.TENANT) {
                existingRole = roleService.findRoleByTenantIdAndName(tenantId, role.getName()).orElse(null);
            } else {
                existingRole = roleService.findRoleByByTenantIdAndCustomerIdAndName(tenantId,
                        idProvider.getInternalId(role.getCustomerId()), role.getName()).orElse(null);
            }
        }
        return existingRole;
    }

    @Override
    protected Role prepare(EntitiesImportCtx ctx, Role entity, Role oldEntity, EntityExportData<Role> exportData, IdProvider idProvider) {
        return entity;
    }

    @Override
    protected Role deepCopy(Role role) {
        return new Role(role);
    }

    @Override
    protected Role saveOrUpdate(EntitiesImportCtx ctx, Role role, EntityExportData<Role> exportData, IdProvider idProvider) {
        return roleService.saveRole(ctx.getTenantId(), role);
    }

    @Override
    protected void onEntitySaved(User user, Role savedRole, Role oldRole) throws ThingsboardException {
        entityNotificationService.notifyCreateOrUpdateOrDelete(savedRole.getTenantId(), null,
                savedRole.getId(), savedRole, user, oldRole == null ? ActionType.ADDED : ActionType.UPDATED, true, true, null);
        userPermissionsService.onRoleUpdated(savedRole);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }

}
