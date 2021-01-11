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
package org.thingsboard.server.dao.sql.integration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.IntegrationEntity;

import java.util.List;
import java.util.UUID;

public interface IntegrationRepository extends CrudRepository<IntegrationEntity, UUID> {

    @Query("SELECT a FROM IntegrationEntity a WHERE a.tenantId = :tenantId " +
            "AND LOWER(a.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<IntegrationEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                                      @Param("searchText") String searchText,
                                                      Pageable pageable);

    IntegrationEntity findByRoutingKey(String routingKey);

    List<IntegrationEntity> findByConverterId(UUID converterId);

    List<IntegrationEntity> findIntegrationsByTenantIdAndIdIn(UUID tenantId, List<UUID> integrationIds);

    Long countByTenantId(UUID tenantId);

}
