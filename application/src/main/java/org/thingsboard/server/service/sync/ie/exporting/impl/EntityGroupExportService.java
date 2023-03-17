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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityGroupExportData;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EntityGroupExportService extends BaseEntityExportService<EntityGroupId, EntityGroup, EntityGroupExportData> {

    private final GroupPermissionService groupPermissionService;
    private final RoleService roleService;

    @Override
    protected void setAdditionalExportData(EntitiesExportCtx<?> ctx, EntityGroup entityGroup, EntityGroupExportData exportData) throws ThingsboardException {
        super.setAdditionalExportData(ctx, entityGroup, exportData);
        var exportSettings = ctx.getSettings();
        exportData.setGroupEntities(exportSettings.isExportGroupEntities());
        if (exportSettings.isExportPermissions() && entityGroup.getType() == EntityType.USER) {
            List<GroupPermission> permissions = groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(ctx.getTenantId(), entityGroup.getId()).stream()
                    .filter(permission -> {
                        Role role = roleService.findRoleById(ctx.getTenantId(), permission.getRoleId());
                        return !role.getOwnerId().equals(TenantId.SYS_TENANT_ID);
                    })
                    .peek(permission -> {
                        permission.setUserGroupId(getExternalIdOrElseInternal(ctx, permission.getUserGroupId()));
                        permission.setRoleId(getExternalIdOrElseInternal(ctx, permission.getRoleId()));
                        permission.setEntityGroupId(getExternalIdOrElseInternal(ctx, permission.getEntityGroupId()));
                    })
                    .collect(Collectors.toList());
            exportData.setPermissions(permissions);
        }
        replaceUuidsRecursively(ctx, JacksonUtil.getSafely(exportData.getEntity().getConfiguration(), "actions"), Collections.singleton("id"));
    }

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, EntityGroup entityGroup, EntityGroupExportData exportData) {
        if (entityGroup.getOwnerId().getEntityType() == EntityType.CUSTOMER) {
            entityGroup.setOwnerId(getExternalIdOrElseInternal(ctx, entityGroup.getOwnerId()));
        }
    }

    @Override
    protected EntityGroupExportData newExportData() {
        return new EntityGroupExportData();
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ENTITY_GROUP);
    }

}
