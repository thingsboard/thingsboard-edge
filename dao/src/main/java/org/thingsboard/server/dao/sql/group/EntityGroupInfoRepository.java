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
import org.thingsboard.server.dao.model.sql.EntityGroupInfoEntity;

import java.util.List;
import java.util.UUID;

public interface EntityGroupInfoRepository extends JpaRepository<EntityGroupInfoEntity, UUID> {

    @Query("SELECT e FROM EntityGroupInfoEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityGroupInfoEntity> findEntityGroupsByType(@Param("parentEntityId") UUID parentEntityId,
                                                       @Param("parentEntityType") EntityType parentEntityType,
                                                       @Param("groupType") EntityType groupType,
                                                       @Param("textSearch") String textSearch,
                                                       Pageable pageable);

    @Query("SELECT e FROM EntityGroupInfoEntity e " +
            "WHERE e.ownerId IN :ownerIds " +
            "AND e.type = :groupType " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityGroupInfoEntity> findEntityGroupsByOwnerIdsAndType(@Param("ownerIds") List<UUID> ownerIds,
                                                                  @Param("groupType") EntityType groupType,
                                                                  @Param("textSearch") String textSearch,
                                                                  Pageable pageable);

    @Query("SELECT e FROM EntityGroupInfoEntity e " +
            "WHERE e.id IN :entityGroupIds " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityGroupInfoEntity> findEntityGroupsByIds(@Param("entityGroupIds") List<UUID> entityGroupIds,
                                                      @Param("textSearch") String textSearch,
                                                      Pageable pageable);

    @Query("SELECT e FROM EntityGroupInfoEntity e " +
            "WHERE ((e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType) " +
            "OR (e.id IN :entityGroupIds)) " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<EntityGroupInfoEntity> findEntityGroupsByTypeOrIds(@Param("parentEntityId") UUID parentEntityId,
                                                            @Param("parentEntityType") EntityType parentEntityType,
                                                            @Param("groupType") EntityType groupType,
                                                            @Param("entityGroupIds") List<UUID> entityGroupIds,
                                                            @Param("textSearch") String textSearch,
                                                            Pageable pageable);

    @Query(value = "SELECT e.* FROM (select ev.*, ev.owner_ids as ownerids, ev.created_time as createdtime from entity_group_info_view ev) e, relation re " +
            "WHERE e.id = re.to_id AND re.to_type = 'ENTITY_GROUP' " +
            "AND re.relation_type_group = 'EDGE' " +
            "AND re.relation_type = :relationType " +
            "AND re.from_id = :edgeId AND re.from_type = 'EDGE' " +
            "AND exists (select 1 from json_array_elements(owner_ids) as owners " +
            "WHERE owners->>'id' = :ownerId " +
            "AND owners->>'entityType' = :ownerType) " +
            "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%')))",
            countQuery = "SELECT count(e.id) FROM entity_group_info_view e, relation re " +
                    "WHERE e.id = re.to_id AND re.to_type = 'ENTITY_GROUP' " +
                    "AND re.relation_type_group = 'EDGE' " +
                    "AND re.relation_type = :relationType " +
                    "AND re.from_id = :edgeId AND re.from_type = 'EDGE' " +
                    "AND exists (select 1 from json_array_elements(owner_ids) as owners " +
                    "WHERE owners->>'id' = :ownerId " +
                    "AND owners->>'entityType' = :ownerType) " +
                    "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :textSearch, '%')))",
            nativeQuery = true)
    Page<EntityGroupInfoEntity> findEdgeEntityGroupsByOwnerIdAndType(@Param("edgeId") UUID edgeId,
                                                                     @Param("ownerId") String ownerId,
                                                                     @Param("ownerType") String ownerType,
                                                                     @Param("relationType") String relationType,
                                                                     @Param("textSearch") String textSearch,
                                                                     Pageable pageable);

    @Query("SELECT e FROM EntityGroupInfoEntity e " +
            "WHERE e.ownerId = :parentEntityId " +
            "AND e.ownerType = :parentEntityType " +
            "AND e.type = :groupType " +
            "AND e.name = :name")
    EntityGroupInfoEntity findEntityGroupByTypeAndName(@Param("parentEntityId") UUID parentEntityId,
                                                       @Param("parentEntityType") EntityType parentEntityType,
                                                       @Param("groupType") EntityType groupType,
                                                       @Param("name") String name);
}
