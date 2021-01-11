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
package org.thingsboard.server.dao.sql.blob;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.BlobEntityInfoEntity;
import org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity;

import java.util.List;
import java.util.UUID;

public interface BlobEntityInfoRepository extends CrudRepository<BlobEntityInfoEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity(b, c.title, c.additionalInfo) " +
            "FROM BlobEntityInfoEntity b " +
            "LEFT JOIN CustomerEntity c on c.id = b.customerId " +
            "WHERE b.id = :blobEntityId")
    BlobEntityWithCustomerInfoEntity findBlobEntityWithCustomerInfoById(@Param("blobEntityId") UUID blobEntityId);

    List<BlobEntityInfoEntity> findBlobEntitiesByTenantIdAndIdIn(UUID tenantId, List<UUID> blobEntityIds);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity(b, c.title, c.additionalInfo) " +
            "FROM BlobEntityInfoEntity b " +
            "LEFT JOIN CustomerEntity c on c.id = b.customerId " +
            "WHERE b.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR b.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR b.createdTime <= :endTime) " +
            "AND (LOWER(b.searchText) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<BlobEntityWithCustomerInfoEntity> findByTenantId(
            @Param("tenantId") UUID tenantId,
            @Param("textSearch") String textSearch,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity(b, c.title, c.additionalInfo) " +
            "FROM BlobEntityInfoEntity b " +
            "LEFT JOIN CustomerEntity c on c.id = b.customerId " +
            "WHERE b.tenantId = :tenantId " +
            "AND b.type = :type " +
            "AND (:startTime IS NULL OR b.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR b.createdTime <= :endTime) " +
            "AND (LOWER(b.searchText) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<BlobEntityWithCustomerInfoEntity> findByTenantIdAndType(
            @Param("tenantId") UUID tenantId,
            @Param("type") String type,
            @Param("textSearch") String textSearch,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity(b, c.title, c.additionalInfo) " +
            "FROM BlobEntityInfoEntity b " +
            "LEFT JOIN CustomerEntity c on c.id = b.customerId " +
            "WHERE b.tenantId = :tenantId " +
            "AND b.customerId = :customerId " +
            "AND (:startTime IS NULL OR b.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR b.createdTime <= :endTime) " +
            "AND (LOWER(b.searchText) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<BlobEntityWithCustomerInfoEntity> findByTenantIdAndCustomerId(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId,
            @Param("textSearch") String textSearch,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity(b, c.title, c.additionalInfo) " +
            "FROM BlobEntityInfoEntity b " +
            "LEFT JOIN CustomerEntity c on c.id = b.customerId " +
            "WHERE b.tenantId = :tenantId " +
            "AND b.customerId = :customerId " +
            "AND b.type = :type " +
            "AND (:startTime IS NULL OR b.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR b.createdTime <= :endTime) " +
            "AND (LOWER(b.searchText) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<BlobEntityWithCustomerInfoEntity> findByTenantIdAndCustomerIdAndType(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId,
            @Param("type") String type,
            @Param("textSearch") String textSearch,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            Pageable pageable);

}
