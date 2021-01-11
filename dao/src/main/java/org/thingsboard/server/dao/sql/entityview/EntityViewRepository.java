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
package org.thingsboard.server.dao.sql.entityview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Victor Basanets on 8/31/2017.
 */
public interface EntityViewRepository extends PagingAndSortingRepository<EntityViewEntity, UUID> {

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EntityViewEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.type = :type " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EntityViewEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                 @Param("type") String type,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<EntityViewEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                       @Param("customerId") UUID customerId,
                                                       @Param("searchText") String searchText,
                                                       Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e WHERE e.tenantId = :tenantId " +
            "AND e.customerId = :customerId " +
            "AND e.type = :type " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<EntityViewEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                              @Param("customerId") UUID customerId,
                                                              @Param("type") String type,
                                                              @Param("searchText") String searchText,
                                                              Pageable pageable);

    EntityViewEntity findByTenantIdAndName(UUID tenantId, String name);

    List<EntityViewEntity> findAllByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    @Query("SELECT DISTINCT ev.type FROM EntityViewEntity ev WHERE ev.tenantId = :tenantId")
    List<String> findTenantEntityViewTypes(@Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM EntityViewEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_VIEW' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EntityViewEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_VIEW' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EntityViewEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                            @Param("textSearch") String textSearch,
                                            Pageable pageable);

    @Query("SELECT e FROM EntityViewEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_VIEW' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND e.type = :type " +
            "AND LOWER(e.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EntityViewEntity> findByEntityGroupIdsAndType(@Param("groupIds") List<UUID> groupIds,
                                                   @Param("type") String type,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    List<EntityViewEntity> findEntityViewsByTenantIdAndIdIn(UUID tenantId, List<UUID> entityViewIds);

}
