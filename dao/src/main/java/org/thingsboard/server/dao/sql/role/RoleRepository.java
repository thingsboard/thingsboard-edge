/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.dao.sql.role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.model.sql.RoleEntity;

import java.util.List;
import java.util.UUID;

public interface RoleRepository extends CrudRepository<RoleEntity, UUID> {

    RoleEntity findByTenantIdAndCustomerIdAndName(UUID tenantId, UUID customerId, String name);

    @Query("SELECT r FROM RoleEntity r WHERE r.tenantId = :tenantId " +
            "AND r.customerId = :customerId " +
            "AND LOWER(r.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<RoleEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                 @Param("customerId") UUID customerId,
                                                 @Param("searchText") String searchText,
                                                 Pageable pageable);

    @Query("SELECT r FROM RoleEntity r WHERE r.tenantId = :tenantId " +
            "AND r.customerId = :customerId AND r.type = :type " +
            "AND LOWER(r.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<RoleEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                        @Param("customerId") UUID customerId,
                                                        @Param("type") RoleType type,
                                                        @Param("searchText") String searchText,
                                                        Pageable pageable);

    List<RoleEntity> findRolesByTenantIdAndIdIn(UUID tenantId, List<UUID> roleIds);

}
