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
package org.thingsboard.server.dao.sql.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.CustomerEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface CustomerRepository extends PagingAndSortingRepository<CustomerEntity, UUID> {

    @Query("SELECT c FROM CustomerEntity c WHERE c.tenantId = :tenantId " +
            "AND LOWER(c.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<CustomerEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                        @Param("searchText") String searchText,
                                        Pageable pageable);

    CustomerEntity findByTenantIdAndTitle(UUID tenantId, String title);

    @Query("SELECT c FROM CustomerEntity c, " +
            "RelationEntity re " +
            "WHERE c.id = re.toId AND re.toType = 'CUSTOMER' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(c.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<CustomerEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                             @Param("textSearch") String textSearch,
                                             Pageable pageable);

    @Query("SELECT c FROM CustomerEntity c, " +
            "RelationEntity re " +
            "WHERE ((c.id = re.toId AND re.toType = 'CUSTOMER' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP') " +
            "OR (:additionalCustomerIds IS NOT NULL AND c.id in :additionalCustomerIds)) " +
            "AND LOWER(c.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<CustomerEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                              @Param("additionalCustomerIds") List<UUID> additionalCustomerIds,
                                              @Param("textSearch") String textSearch,
                                              Pageable pageable);

    List<CustomerEntity> findCustomersByTenantIdAndIdIn(UUID tenantId, List<UUID> customerIds);

    Long countByTenantId(UUID tenantId);
}
