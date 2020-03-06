/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.blob;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.BlobEntityInfoEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface BlobEntityInfoRepository extends CrudRepository<BlobEntityInfoEntity, String> {

    List<BlobEntityInfoEntity> findBlobEntitiesByTenantIdAndIdIn(String tenantId, List<String> blobEntityIds);

    @Query("SELECT b FROM BlobEntityInfoEntity b WHERE " +
            "b.tenantId = :tenantId " +
            "AND (:startId IS NULL OR b.id >= :startId) " +
            "AND (:endId IS NULL OR b.id <= :endId) " +
            "AND (LOWER(b.searchText) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<BlobEntityInfoEntity> findByTenantId(
            @Param("tenantId") String tenantId,
            @Param("textSearch") String textSearch,
            @Param("startId") String startId,
            @Param("endId") String endId,
            Pageable pageable);

    @Query("SELECT b FROM BlobEntityInfoEntity b WHERE " +
            "b.tenantId = :tenantId " +
            "AND b.type = :type " +
            "AND (:startId IS NULL OR b.id >= :startId) " +
            "AND (:endId IS NULL OR b.id <= :endId) " +
            "AND (LOWER(b.searchText) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<BlobEntityInfoEntity> findByTenantIdAndType(
            @Param("tenantId") String tenantId,
            @Param("type") String type,
            @Param("textSearch") String textSearch,
            @Param("startId") String startId,
            @Param("endId") String endId,
            Pageable pageable);

    @Query("SELECT b FROM BlobEntityInfoEntity b WHERE " +
            "b.tenantId = :tenantId " +
            "AND b.customerId = :customerId " +
            "AND (:startId IS NULL OR b.id >= :startId) " +
            "AND (:endId IS NULL OR b.id <= :endId) " +
            "AND (LOWER(b.searchText) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<BlobEntityInfoEntity> findByTenantIdAndCustomerId(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId,
            @Param("textSearch") String textSearch,
            @Param("startId") String startId,
            @Param("endId") String endId,
            Pageable pageable);

    @Query("SELECT b FROM BlobEntityInfoEntity b WHERE " +
            "b.tenantId = :tenantId " +
            "AND b.customerId = :customerId " +
            "AND b.type = :type " +
            "AND (:startId IS NULL OR b.id >= :startId) " +
            "AND (:endId IS NULL OR b.id <= :endId) " +
            "AND (LOWER(b.searchText) LIKE LOWER(CONCAT(:textSearch, '%')))"
    )
    Page<BlobEntityInfoEntity> findByTenantIdAndCustomerIdAndType(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId,
            @Param("type") String type,
            @Param("textSearch") String textSearch,
            @Param("startId") String startId,
            @Param("endId") String endId,
            Pageable pageable);

}
