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
package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DashboardInfoRepository extends PagingAndSortingRepository<DashboardInfoEntity, UUID> {

    @Query("SELECT di FROM DashboardInfoEntity di WHERE di.tenantId = :tenantId " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<DashboardInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                             @Param("searchText") String searchText,
                                             Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di, " +
            "RelationEntity re " +
            "WHERE di.id = re.toId AND re.toType = 'DASHBOARD' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<DashboardInfoEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                                  @Param("textSearch") String textSearch,
                                                  Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di, " +
            "RelationEntity re " +
            "WHERE di.id = re.toId AND re.toType = 'DASHBOARD' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<DashboardInfoEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    List<DashboardInfoEntity> findByIdIn(List<UUID> dashboardIds);

}
