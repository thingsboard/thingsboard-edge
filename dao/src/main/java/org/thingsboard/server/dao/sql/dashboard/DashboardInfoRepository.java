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
package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SUB_CUSTOMERS_QUERY;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DashboardInfoRepository extends JpaRepository<DashboardInfoEntity, UUID> {

    DashboardInfoEntity findFirstByTenantIdAndTitle(UUID tenantId, String title);

    @Query("SELECT di FROM DashboardInfoEntity di " +
            "WHERE di.tenantId = :tenantId " +
            "AND (LOWER(di.searchText) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "OR LOWER(di.ownerName) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    Page<DashboardInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                             @Param("searchText") String searchText,
                                             Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di " +
            "WHERE di.tenantId = :tenantId AND (di.customerId IS NULL OR di.customerId = '13814000-1dd2-11b2-8080-808080808080') " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DashboardInfoEntity> findTenantDashboardsByTenantId(@Param("tenantId") UUID tenantId,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di WHERE di.tenantId = :tenantId " +
            "AND di.mobileHide = false " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DashboardInfoEntity> findMobileByTenantId(@Param("tenantId") UUID tenantId,
                                                      @Param("searchText") String searchText,
                                                      Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di WHERE di.tenantId = :tenantId AND di.customerId = :customerId " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DashboardInfoEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                             @Param("customerId") UUID customerId,
                                                             @Param("searchText") String searchText,
                                                             Pageable pageable);

    @Query(value = "SELECT e.*, e.owner_name as ownername, e.created_time as createdtime " +
            "FROM (select d.*, c.title as owner_name from dashboard d LEFT JOIN customer c on c.id = d.customer_id AND c.id != :customerId) e " +
            "WHERE" + SUB_CUSTOMERS_QUERY +
            "AND (LOWER(e.search_text) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "OR LOWER(e.owner_name) LIKE LOWER(CONCAT('%', :searchText, '%')))",
            countQuery = "SELECT count(e.id) FROM dashboard e " +
                    "LEFT JOIN customer c on c.id = e.customer_id AND c.id != :customerId " +
                    "WHERE" + SUB_CUSTOMERS_QUERY +
                    "AND (LOWER(e.search_text) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                    "OR LOWER(c.title) LIKE LOWER(CONCAT('%', :searchText, '%')))",
            nativeQuery = true)
    Page<DashboardInfoEntity> findByTenantIdAndCustomerIdIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                               @Param("customerId") UUID customerId,
                                                                               @Param("searchText") String searchText,
                                                                               Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di, RelationEntity re WHERE di.tenantId = :tenantId " +
            "AND di.mobileHide = false " +
            "AND di.id = re.toId AND re.toType = 'DASHBOARD' AND re.relationTypeGroup = 'DASHBOARD' " +
            "AND re.relationType = 'Contains' AND re.fromId = :customerId AND re.fromType = 'CUSTOMER' " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DashboardInfoEntity> findMobileByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                   @Param("customerId") UUID customerId,
                                                                   @Param("searchText") String searchText,
                                                                   Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di, " +
            "RelationEntity re " +
            "WHERE di.id = re.toId AND re.toType = 'DASHBOARD' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DashboardInfoEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                                     @Param("textSearch") String textSearch,
                                                     Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di, " +
            "RelationEntity re " +
            "WHERE di.id = re.toId AND re.toType = 'DASHBOARD' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DashboardInfoEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                                      @Param("textSearch") String textSearch,
                                                      Pageable pageable);

    @Query("SELECT di FROM DashboardInfoEntity di, " +
            "RelationEntity re " +
            "WHERE di.id = re.toId AND di.mobileHide = false AND re.toType = 'DASHBOARD' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<DashboardInfoEntity> findMobileByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                                            @Param("textSearch") String textSearch,
                                                            Pageable pageable);

    List<DashboardInfoEntity> findByIdIn(List<UUID> dashboardIds);

    @Query("SELECT di FROM DashboardInfoEntity di, RelationEntity re WHERE di.tenantId = :tenantId " +
            "AND di.id = re.toId AND re.toType = 'DASHBOARD' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND LOWER(di.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<DashboardInfoEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                         @Param("edgeId") UUID edgeId,
                                                         @Param("searchText") String searchText,
                                                         Pageable pageable);

}
