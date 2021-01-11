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
package org.thingsboard.server.dao.sql.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.sql.UserEntity;

import java.util.List;
import java.util.UUID;

/**
 * @author Valerii Sosliuk
 */
public interface UserRepository extends PagingAndSortingRepository<UserEntity, UUID> {

    UserEntity findByEmail(String email);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.customerId = :customerId AND u.authority = :authority " +
            "AND LOWER(u.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<UserEntity> findUsersByAuthority(@Param("tenantId") UUID tenantId,
                                          @Param("customerId") UUID customerId,
                                          @Param("searchText") String searchText,
                                          @Param("authority") Authority authority,
                                          Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.authority = :authority " +
            "AND LOWER(u.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
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
            "AND LOWER(u.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<UserEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                         @Param("textSearch") String textSearch,
                                         Pageable pageable);

    @Query("SELECT u FROM UserEntity u, " +
            "RelationEntity re " +
            "WHERE u.id = re.toId AND re.toType = 'USER' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(u.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<UserEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    List<UserEntity> findUsersByTenantIdAndIdIn(UUID tenantId, List<UUID> userIds);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND LOWER(u.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<UserEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("searchText") String searchText,
                                    Pageable pageable);

    Long countByTenantId(UUID tenantId);
}
