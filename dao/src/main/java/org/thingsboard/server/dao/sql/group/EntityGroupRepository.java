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
package org.thingsboard.server.dao.sql.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.model.sql.AssetEntity;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;

import java.util.List;
import java.util.UUID;

public interface EntityGroupRepository extends CrudRepository<EntityGroupEntity, UUID> {

    List<EntityGroupEntity> findEntityGroupsByIdIn(List<UUID> entityGroupIds);

    @Query("SELECT e FROM EntityGroupEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'TO_ENTITY_GROUP' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :parentEntityId AND re.fromType = :parentEntityType")
    List<EntityGroupEntity> findEntityGroupsByType(@Param("parentEntityId") UUID parentEntityId,
                                                   @Param("parentEntityType") String parentEntityType,
                                                   @Param("relationType") String relationType);

    @Query("SELECT e FROM EntityGroupEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'TO_ENTITY_GROUP' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :parentEntityId AND re.fromType = :parentEntityType " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EntityGroupEntity> findEntityGroupsByTypeAndPageLink(@Param("parentEntityId") UUID parentEntityId,
                                                              @Param("parentEntityType") String parentEntityType,
                                                              @Param("relationType") String relationType,
                                                              @Param("textSearch") String textSearch,
                                                              Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e, " +
            "RelationEntity re " +
            "WHERE e.name = :name " +
            "AND e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'TO_ENTITY_GROUP' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :parentEntityId AND re.fromType = :parentEntityType")
    EntityGroupEntity findEntityGroupByTypeAndName(@Param("parentEntityId") UUID parentEntityId,
                                                   @Param("parentEntityType") String parentEntityType,
                                                   @Param("relationType") String relationType,
                                                   @Param("name") String name);

    @Query("SELECT e FROM EntityGroupEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'TO_ENTITY_GROUP' " +
            "AND re.fromId = :parentEntityId AND re.fromType = :parentEntityType")
    List<EntityGroupEntity> findAllEntityGroups(@Param("parentEntityId") UUID parentEntityId,
                                                @Param("parentEntityType") String parentEntityType);

    @Query("SELECT re.toId " +
           "FROM RelationEntity re " +
           "WHERE re.toType = :groupType " +
           "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
           "AND re.relationType = 'Contains' " +
           "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP'")
    Page<UUID> findGroupEntityIds(@Param("groupId") UUID groupId,
                                  @Param("groupType") String groupType,
                                  Pageable pageable);


    @Query("SELECT CASE WHEN (count(re) = 1) " +
            "THEN true " +
            "ELSE false END " +
            "FROM " +
            "RelationEntity re " +
            "WHERE re.fromId = :entityGroupId " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.toId = :entityId")
    boolean isEntityInGroup(@Param("entityId") UUID entityId,
                            @Param("entityGroupId") UUID entityGroupId);
}
