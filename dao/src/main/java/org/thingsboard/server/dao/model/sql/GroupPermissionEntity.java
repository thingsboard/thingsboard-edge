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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_ENTITY_GROUP_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_ENTITY_GROUP_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_IS_PUBLIC_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_ROLE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_USER_GROUP_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.GROUP_PERMISSION_TABLE_FAMILY_NAME)
@Slf4j
public class GroupPermissionEntity extends BaseSqlEntity<GroupPermission> {

    @Column(name = GROUP_PERMISSION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = GROUP_PERMISSION_USER_GROUP_ID_PROPERTY)
    private UUID userGroupId;

    @Column(name = GROUP_PERMISSION_ENTITY_GROUP_ID_PROPERTY)
    private UUID entityGroupId;

    @Column(name = GROUP_PERMISSION_ROLE_ID_PROPERTY)
    private UUID roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = GROUP_PERMISSION_ENTITY_GROUP_TYPE_PROPERTY)
    private EntityType entityGroupType;

    @Column(name = GROUP_PERMISSION_IS_PUBLIC_PROPERTY)
    private boolean isPublic;

    private static final ObjectMapper mapper = new ObjectMapper();

    public GroupPermissionEntity() {
        super();
    }

    public GroupPermissionEntity(GroupPermission groupPermission) {
        this.createdTime = groupPermission.getCreatedTime();
        if (groupPermission.getId() != null) {
            this.setUuid(groupPermission.getId().getId());
        }
        if (groupPermission.getTenantId() != null) {
            this.tenantId = groupPermission.getTenantId().getId();
        }
        if (groupPermission.getRoleId() != null) {
            this.roleId = groupPermission.getRoleId().getId();
        }
        if (groupPermission.getUserGroupId() != null) {
            this.userGroupId = groupPermission.getUserGroupId().getId();
        }
        if (groupPermission.getEntityGroupId() != null) {
            this.entityGroupId = groupPermission.getEntityGroupId().getId();
            this.entityGroupType = groupPermission.getEntityGroupType();
        }
        this.isPublic = groupPermission.isPublic();
    }

    @Override
    public GroupPermission toData() {
        GroupPermission groupPermission = new GroupPermission(new GroupPermissionId(getUuid()));
        groupPermission.setCreatedTime(this.createdTime);
        if (tenantId != null) {
            groupPermission.setTenantId(new TenantId(tenantId));
        }
        if (roleId != null) {
            groupPermission.setRoleId(new RoleId(roleId));
        }
        if (userGroupId != null) {
            groupPermission.setUserGroupId(new EntityGroupId(userGroupId));
        }
        if (entityGroupId != null && entityGroupType != null) {
            groupPermission.setEntityGroupId(new EntityGroupId(entityGroupId));
            groupPermission.setEntityGroupType(entityGroupType);
        }
        groupPermission.setPublic(isPublic);
        return groupPermission;
    }
}
