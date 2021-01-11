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
package org.thingsboard.server.dao.sql.grouppermission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.GroupPermissionEntity;

import java.util.UUID;

public interface GroupPermissionRepository extends CrudRepository<GroupPermissionEntity, UUID>, JpaSpecificationExecutor<GroupPermissionEntity> {

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId"
    )
    Page<GroupPermissionEntity> findByTenantId(
            @Param("tenantId") UUID tenantId,
            Pageable pageable);

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId " +
            "AND g.userGroupId = :userGroupId"
    )
    Page<GroupPermissionEntity> findByTenantIdAndUserGroupId(
            @Param("tenantId") UUID tenantId,
            @Param("userGroupId") UUID userGroupId,
            Pageable pageable);

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId " +
            "AND g.userGroupId = :userGroupId " +
            "AND g.roleId = :roleId"
    )
    Page<GroupPermissionEntity> findByTenantIdAndUserGroupIdAndRoleId(
            @Param("tenantId") UUID tenantId,
            @Param("userGroupId") UUID userGroupId,
            @Param("roleId") UUID roleId,
            Pageable pageable);

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId " +
            "AND g.entityGroupId = :entityGroupId " +
            "AND g.userGroupId = :userGroupId " +
            "AND g.roleId = :roleId"
    )
    Page<GroupPermissionEntity> findByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(
            @Param("tenantId") UUID tenantId,
            @Param("entityGroupId") UUID entityGroupId,
            @Param("userGroupId") UUID userGroupId,
            @Param("roleId") UUID roleId,
            Pageable pageable);

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId " +
            "AND g.entityGroupId = :entityGroupId"
    )
    Page<GroupPermissionEntity> findByTenantIdAndEntityGroupId(
            @Param("tenantId") UUID tenantId,
            @Param("entityGroupId") UUID entityGroupId,
            Pageable pageable);

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId " +
            "AND g.roleId = :roleId"
    )
    Page<GroupPermissionEntity> findByTenantIdAndRoleId(
            @Param("tenantId") UUID tenantId,
            @Param("roleId") UUID roleId,
            Pageable pageable);
}
