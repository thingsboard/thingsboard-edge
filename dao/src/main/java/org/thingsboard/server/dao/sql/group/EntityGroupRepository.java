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
package org.thingsboard.server.dao.sql.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;

import java.util.List;
import java.util.UUID;

public interface EntityGroupRepository extends JpaRepository<EntityGroupEntity, UUID> {

    String TENANT_ID_FILTER = "((e.ownerId = :tenantId AND e.ownerType = 'TENANT') OR " +
            "(e.ownerId in (SELECT c.id FROM CustomerEntity c where c.tenantId = :tenantId) and e.ownerType = 'CUSTOMER'))";

    @Query("SELECT e FROM EntityGroupEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityGroupEntity> findEntityGroupsByType(@Param("parentEntityId") UUID parentEntityId,
                                                   @Param("parentEntityType") EntityType parentEntityType,
                                                   @Param("groupType") EntityType groupType,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e " +
            "WHERE " +
            TENANT_ID_FILTER +
            "AND e.type = :groupType " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityGroupEntity> findEntityGroupsByType(
            @Param("tenantId") UUID tenantId,
            @Param("groupType") EntityType groupType,
            @Param("textSearch") String textSearch,
            Pageable toPageable);

    @Query("SELECT e FROM EntityGroupEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType " +
            "AND e.name = :name")
    EntityGroupEntity findEntityGroupByTypeAndName(@Param("parentEntityId") UUID parentEntityId,
                                                   @Param("parentEntityType") EntityType parentEntityType,
                                                   @Param("groupType") EntityType groupType,
                                                   @Param("name") String name);

    @Query("SELECT e FROM EntityGroupEntity e, RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'TO_ENTITY_GROUP' " +
            "AND re.fromId = :parentEntityId AND re.fromType = :parentEntityType " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityGroupEntity> findAllEntityGroupsByParentRelation(@Param("parentEntityId") UUID parentEntityId,
                                                                @Param("parentEntityType") String parentEntityType,
                                                                @Param("textSearch") String textSearch,
                                                                Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityGroupEntity> findAllEntityGroups(@Param("parentEntityId") UUID parentEntityId,
                                                @Param("parentEntityType") EntityType parentEntityType,
                                                @Param("textSearch") String textSearch,
                                                Pageable pageable);

    @Query("SELECT re.toId " +
            "FROM RelationEntity re " +
            "WHERE re.toType = :groupType " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP'")
    Page<UUID> findGroupEntityIds(@Param("groupId") UUID groupId,
                                  @Param("groupType") String groupType,
                                  Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :edgeId AND re.fromType = 'EDGE'")
    Page<EntityGroupEntity> findEdgeEntityGroupsByType(@Param("edgeId") UUID edgeId,
                                                       @Param("relationType") String relationType,
                                                       Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e WHERE " +
            "e.externalId = :externalId AND " + TENANT_ID_FILTER)
    EntityGroupEntity findByTenantIdAndExternalId(@Param("tenantId") UUID tenantId, @Param("externalId") UUID externalId);

    @Query("SELECT e FROM EntityGroupEntity e WHERE " + TENANT_ID_FILTER)
    Page<EntityGroupEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT externalId FROM EntityGroupEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}
