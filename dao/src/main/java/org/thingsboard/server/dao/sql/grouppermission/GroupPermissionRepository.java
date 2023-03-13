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
package org.thingsboard.server.dao.sql.grouppermission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.GroupPermissionEntity;

import java.util.UUID;

public interface GroupPermissionRepository extends JpaRepository<GroupPermissionEntity, UUID>, JpaSpecificationExecutor<GroupPermissionEntity> {

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
