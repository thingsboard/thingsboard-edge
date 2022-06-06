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
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.sync.ie.importing.impl.BaseEntityImportService;

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
    protected Role findExistingEntity(TenantId tenantId, Role role, EntityImportSettings importSettings) {
        Role existingRole = super.findExistingEntity(tenantId, role, importSettings);
        if (existingRole == null && importSettings.isFindExistingByName()) {
            if (role.getOwnerId().getEntityType() == EntityType.TENANT) {
                existingRole = roleService.findRoleByTenantIdAndName(tenantId, role.getName()).orElse(null);
            } else {
                existingRole = roleService.findRoleByByTenantIdAndCustomerIdAndName(tenantId,
                        findInternalEntity(tenantId, role.getCustomerId()).getId(), role.getName()).orElse(null);
            }
        }
        return existingRole;
    }

    @Override
    protected Role prepareAndSave(TenantId tenantId, Role role, EntityExportData<Role> exportData, IdProvider idProvider, EntityImportSettings importSettings) {
        return roleService.saveRole(tenantId, role);
    }

    @Override
    protected void onEntitySaved(SecurityUser user, Role savedRole, Role oldRole) throws ThingsboardException {
        super.onEntitySaved(user, savedRole, oldRole);
        userPermissionsService.onRoleUpdated(savedRole);
        entityActionService.sendEntityNotificationMsgToEdgeService(user.getTenantId(), savedRole.getId(),
                oldRole == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }

}
