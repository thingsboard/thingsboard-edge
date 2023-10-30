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
package org.thingsboard.server.dao.sql.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.sql.UserEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @author Valerii Sosliuk
 */
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    UserEntity findByEmail(String email);

    UserEntity findByTenantIdAndEmail(UUID tenantId, String email);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.customerId = :customerId AND u.authority = :authority " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findUsersByAuthority(@Param("tenantId") UUID tenantId,
                                          @Param("customerId") UUID customerId,
                                          @Param("searchText") String searchText,
                                          @Param("authority") Authority authority,
                                          Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.customerId IN (:customerIds) " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findTenantAndCustomerUsers(@Param("tenantId") UUID tenantId,
                                                @Param("customerIds") Collection<UUID> customerIds,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.authority = :authority " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findAllTenantUsersByAuthority(@Param("tenantId") UUID tenantId,
                                                   @Param("searchText") String searchText,
                                                   @Param("authority") Authority authority,
                                                   Pageable pageable);

    @Query("SELECT u FROM UserEntity u, " +
            "RelationEntity re " +
            "WHERE u.id = re.toId AND re.toType = 'USER' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(u.email, CONCAT('%', :textSearch, '%')) = true)")
    Page<UserEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                         @Param("textSearch") String textSearch,
                                         Pageable pageable);

    @Query("SELECT u FROM UserEntity u, " +
            "RelationEntity re " +
            "WHERE u.id = re.toId AND re.toType = 'USER' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(u.email, CONCAT('%', :textSearch, '%')) = true)")
    Page<UserEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    List<UserEntity> findUsersByTenantIdAndIdIn(UUID tenantId, List<UUID> userIds);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("searchText") String searchText,
                                    Pageable pageable);

    Page<UserEntity> findAllByAuthority(Authority authority, Pageable pageable);

    Page<UserEntity> findByAuthorityAndTenantIdIn(Authority authority, Collection<UUID> tenantsIds, Pageable pageable);

    @Query("SELECT u FROM UserEntity u INNER JOIN TenantEntity t ON u.tenantId = t.id AND u.authority = :authority " +
            "INNER JOIN TenantProfileEntity p ON t.tenantProfileId = p.id " +
            "WHERE p.id IN :profiles")
    Page<UserEntity> findByAuthorityAndTenantProfilesIds(@Param("authority") Authority authority,
                                                         @Param("profiles") Collection<UUID> tenantProfilesIds,
                                                         Pageable pageable);

    Long countByTenantId(UUID tenantId);

    @Query("SELECT u FROM UserEntity u WHERE u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p WHERE (p.tenantId = :tenantId OR :tenantId = '13814000-1dd2-11b2-8080-808080808080') " +
            "AND p.roleId IN :rolesIds))")
    Page<UserEntity> findByTenantIdAndRolesIds(@Param("tenantId") UUID tenantId,
                                               @Param("rolesIds") List<UUID> rolesIds,
                                               Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId IN :tenantsIds AND u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p WHERE p.roleId = :roleId))")
    Page<UserEntity> findByTenantsIdsAndRoleId(@Param("tenantsIds") List<UUID> tenantsIds,
                                               @Param("roleId") UUID roleId,
                                               Pageable pageable);

    @Query("SELECT u FROM UserEntity u INNER JOIN TenantEntity t ON u.tenantId = t.id " +
            "WHERE t.tenantProfileId IN :tenantProfilesIds AND u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p WHERE p.roleId = :roleId))")
    Page<UserEntity> findByTenantProfilesIdsAndRoleId(@Param("tenantProfilesIds") List<UUID> tenantProfilesIds,
                                                      @Param("roleId") UUID roleId,
                                                      Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p WHERE p.roleId = :roleId))")
    Page<UserEntity> findByRoleId(@Param("roleId") UUID roleId,
                                  Pageable pageable);

}
