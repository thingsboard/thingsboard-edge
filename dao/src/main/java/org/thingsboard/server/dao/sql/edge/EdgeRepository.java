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
package org.thingsboard.server.dao.sql.edge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EdgeEntity;

import java.util.List;
import java.util.UUID;

public interface EdgeRepository extends JpaRepository<EdgeEntity, UUID> {

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EdgeEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                 @Param("customerId") UUID customerId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EdgeEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("textSearch") String textSearch,
                                    Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EdgeEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                           @Param("type") String type,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EdgeEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                        @Param("customerId") UUID customerId,
                                                        @Param("type") String type,
                                                        @Param("textSearch") String textSearch,
                                                        Pageable pageable);

    @Query("SELECT ee FROM EdgeEntity ee, RelationEntity re WHERE ee.tenantId = :tenantId " +
            "AND ee.id = re.fromId AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType AND re.toId = :entityId AND re.toType = :entityType " +
            "AND LOWER(ee.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<EdgeEntity> findByTenantIdAndEntityId(@Param("tenantId") UUID tenantId,
                                               @Param("entityId") UUID entityId,
                                               @Param("entityType") String entityType,
                                               @Param("relationType") String relationType,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query("SELECT DISTINCT ee.id FROM EdgeEntity ee, RelationEntity re WHERE ee.tenantId = :tenantId " +
            "AND ee.id = re.fromId AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType AND re.toId IN (:entityIds) AND re.toType = :entityType")
    Page<UUID> findEdgeIdsByTenantIdAndEntityIds(@Param("tenantId") UUID tenantId,
                                                 @Param("entityIds") List<UUID> entityIds,
                                                 @Param("entityType") String entityType,
                                                 @Param("relationType") String relationType,
                                                 Pageable pageable);

    @Query("SELECT DISTINCT d.type FROM EdgeEntity d WHERE d.tenantId = :tenantId")
    List<String> findTenantEdgeTypes(@Param("tenantId") UUID tenantId);

    EdgeEntity findByTenantIdAndName(UUID tenantId, String name);

    List<EdgeEntity> findEdgesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> edgeIds);

    List<EdgeEntity> findEdgesByTenantIdAndIdIn(UUID tenantId, List<UUID> edgeIds);

    EdgeEntity findByRoutingKey(String routingKey);

    @Query("SELECT e FROM EdgeEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'EDGE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EdgeEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                         @Param("textSearch") String textSearch,
                                         Pageable pageable);

    @Query("SELECT e FROM EdgeEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'EDGE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EdgeEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    @Query("SELECT e FROM EdgeEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'EDGE' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND e.type = :type " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EdgeEntity> findByEntityGroupIdsAndType(@Param("groupIds") List<UUID> groupIds,
                                                 @Param("type") String type,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);
}
