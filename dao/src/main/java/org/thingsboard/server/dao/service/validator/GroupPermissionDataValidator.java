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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.grouppermission.GroupPermissionDao;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
@AllArgsConstructor
public class GroupPermissionDataValidator extends DataValidator<GroupPermission> {

    private final TenantService tenantService;
    private final EntityGroupDao entityGroupDao;
    private final RoleDao roleDao;
    private final GroupPermissionDao groupPermissionDao;

    @Override
    protected void validateCreate(TenantId tenantId, GroupPermission groupPermission) {
    }

    @Override
    protected GroupPermission validateUpdate(TenantId tenantId, GroupPermission groupPermission) {
        return groupPermissionDao.findById(tenantId, groupPermission.getId().getId());
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, GroupPermission groupPermission) {
        if (StringUtils.isEmpty(groupPermission.getName())) {
            throw new DataValidationException("Group Permission name should be specified!");
        }
        if (groupPermission.getTenantId() == null) {
            throw new DataValidationException("Group Permission should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(groupPermission.getTenantId())) {
                throw new DataValidationException("Group Permission is referencing to non-existent tenant!");
            }
        }
        if (groupPermission.getUserGroupId() == null || groupPermission.getUserGroupId().isNullUid()) {
            throw new DataValidationException("Group Permission userGroupId should be specified!");
        } else {
            EntityGroup entityGroup = entityGroupDao.findById(tenantId, groupPermission.getUserGroupId().getId());
            if (entityGroup == null) {
                throw new DataValidationException("Group Permission is referencing to non-existent user group!");
            } else if (entityGroup.getType() != EntityType.USER) {
                throw new DataValidationException("Group Permission is referencing to user group with non user group type!");
            }
        }
        if (groupPermission.getRoleId() == null || groupPermission.getRoleId().isNullUid()) {
            throw new DataValidationException("Group Permission roleId should be specified!");
        } else {
            Role role = roleDao.findById(tenantId, groupPermission.getRoleId().getId());
            if (role == null) {
                throw new DataValidationException("Group Permission is referencing to non-existent role!");
            }
        }
        if (groupPermission.getEntityGroupId() == null) {
            groupPermission.setEntityGroupId(new EntityGroupId(EntityId.NULL_UUID));
        }

        if (!groupPermission.getEntityGroupId().isNullUid()) {
            EntityGroup entityGroup = entityGroupDao.findById(tenantId, groupPermission.getEntityGroupId().getId());
            if (entityGroup == null) {
                throw new DataValidationException("Group Permission is referencing to non-existent entity group!");
            }
            groupPermission.setEntityGroupType(entityGroup.getType());
        } else {
            groupPermission.setEntityGroupType(null);
        }
    }
}
