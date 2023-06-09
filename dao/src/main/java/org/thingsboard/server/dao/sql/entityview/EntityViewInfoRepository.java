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
package org.thingsboard.server.dao.sql.entityview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EntityViewInfoEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

public interface EntityViewInfoRepository extends JpaRepository<EntityViewInfoEntity, UUID> {

    @Query("SELECT evi FROM EntityViewInfoEntity evi " +
            "WHERE evi.tenantId = :tenantId " +
            "AND (LOWER(evi.name) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "OR LOWER(evi.ownerName) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    Page<EntityViewInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                              @Param("searchText") String searchText,
                                              Pageable pageable);

    @Query("SELECT evi FROM EntityViewInfoEntity evi " +
            "WHERE evi.tenantId = :tenantId " +
            "AND evi.type = :type " +
            "AND (LOWER(evi.name) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "OR LOWER(evi.ownerName) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    Page<EntityViewInfoEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                     @Param("type") String type,
                                                     @Param("searchText") String searchText,
                                                     Pageable pageable);

    @Query("SELECT evi FROM EntityViewInfoEntity evi " +
            "WHERE evi.tenantId = :tenantId AND (evi.customerId IS NULL OR evi.customerId = '13814000-1dd2-11b2-8080-808080808080') " +
            "AND LOWER(evi.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<EntityViewInfoEntity> findTenantEntityViewsByTenantId(@Param("tenantId") UUID tenantId,
                                                               @Param("searchText") String searchText,
                                                               Pageable pageable);

    @Query("SELECT evi FROM EntityViewInfoEntity evi " +
            "WHERE evi.tenantId = :tenantId AND (evi.customerId IS NULL OR evi.customerId = '13814000-1dd2-11b2-8080-808080808080') " +
            "AND evi.type = :type " +
            "AND LOWER(evi.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<EntityViewInfoEntity> findTenantEntityViewsByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                                      @Param("type") String type,
                                                                      @Param("searchText") String searchText,
                                                                      Pageable pageable);

    @Query("SELECT evi FROM EntityViewInfoEntity evi WHERE evi.tenantId = :tenantId AND evi.customerId = :customerId " +
            "AND LOWER(evi.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<EntityViewInfoEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                           @Param("customerId") UUID customerId,
                                                           @Param("searchText") String searchText,
                                                           Pageable pageable);

    @Query("SELECT evi FROM EntityViewInfoEntity evi WHERE evi.tenantId = :tenantId AND evi.customerId = :customerId " +
            "AND evi.type = :type " +
            "AND LOWER(evi.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<EntityViewInfoEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                                  @Param("customerId") UUID customerId,
                                                                  @Param("type") String type,
                                                                  @Param("searchText") String searchText,
                                                                  Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select ev.id, ev.created_time, ev.entity_id, ev.entity_type, ev.tenant_id, ev.customer_id, " +
            "ev.type, ev.name, ev.keys, ev.start_ts, ev.end_ts, ev.additional_info, ev.external_id, ev.groups, " +
            "c.title as owner_name from entity_view_info_view ev " +
            "LEFT JOIN customer c on c.id = ev.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "OR LOWER(e.owner_name) LIKE LOWER(CONCAT('%', :searchText, '%')))",
            countQuery = "SELECT count(e.id) FROM entity_view e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "OR LOWER(c.title) LIKE LOWER(CONCAT('%', :searchText, '%')))",
            nativeQuery = true)
    Page<EntityViewInfoEntity> findByTenantIdAndCustomerIdIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                                @Param("customerId") UUID customerId,
                                                                                @Param("searchText") String searchText,
                                                                                Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select ev.id, ev.created_time, ev.entity_id, ev.entity_type, ev.tenant_id, ev.customer_id, " +
            "ev.type, ev.name, ev.keys, ev.start_ts, ev.end_ts, ev.additional_info, ev.external_id, ev.groups, " +
            "c.title as owner_name from entity_view_info_view ev " +
            "LEFT JOIN customer c on c.id = ev.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND e.type = :type " +
            "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "OR LOWER(e.owner_name) LIKE LOWER(CONCAT('%', :searchText, '%')))",
            countQuery = "SELECT count(e.id) FROM entity_view e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND e.type = :type " +
                    "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "OR LOWER(c.title) LIKE LOWER(CONCAT('%', :searchText, '%')))",
            nativeQuery = true)
    Page<EntityViewInfoEntity> findByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                                       @Param("customerId") UUID customerId,
                                                                                       @Param("type") String type,
                                                                                       @Param("searchText") String searchText,
                                                                                       Pageable pageable);
}
