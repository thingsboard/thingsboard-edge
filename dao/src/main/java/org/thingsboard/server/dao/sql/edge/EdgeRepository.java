/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.edge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EdgeEntity;

import java.util.List;
import java.util.UUID;

public interface EdgeRepository extends PagingAndSortingRepository<EdgeEntity, UUID> {

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EdgeEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                 @Param("customerId") UUID customerId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EdgeEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("textSearch") String textSearch,
                                    Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EdgeEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                           @Param("type") String type,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EdgeEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                        @Param("customerId") UUID customerId,
                                                        @Param("type") String type,
                                                        @Param("textSearch") String textSearch,
                                                        Pageable pageable);

    @Query("SELECT DISTINCT d.type FROM EdgeEntity d WHERE d.tenantId = :tenantId")
    List<String> findTenantEdgeTypes(@Param("tenantId") UUID tenantId);

    EdgeEntity findByTenantIdAndName(UUID tenantId, String name);

    List<EdgeEntity> findEdgesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> edgeIds);

    List<EdgeEntity> findEdgesByTenantIdAndIdIn(UUID tenantId, List<UUID> edgeIds);

    EdgeEntity findByRoutingKey(String routingKey);
}
