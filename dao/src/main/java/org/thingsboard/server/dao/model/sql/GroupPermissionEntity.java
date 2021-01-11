/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.model.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
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

import static org.thingsboard.server.dao.model.ModelConstants.*;

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
