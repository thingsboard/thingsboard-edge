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
package org.thingsboard.server.dao.sql.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface EntityGroupRepository extends CrudRepository<EntityGroupEntity, String> {

    List<EntityGroupEntity> findEntityGroupsByIdIn(List<String> entityGroupIds);

    @Query("SELECT e FROM EntityGroupEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'TO_ENTITY_GROUP' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :parentEntityId AND re.fromType = :parentEntityType")
    List<EntityGroupEntity> findEntityGroupsByType(@Param("parentEntityId") String parentEntityId,
                                                   @Param("parentEntityType") String parentEntityType,
                                                   @Param("relationType") String relationType);

    @Query("SELECT e FROM EntityGroupEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'TO_ENTITY_GROUP' " +
            "AND re.relationType = :relationType " +
            "AND re.fromId = :parentEntityId AND re.fromType = :parentEntityType " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT(:textSearch, '%'))")
    Page<EntityGroupEntity> findEntityGroupsByTypeAndPageLink(@Param("parentEntityId") String parentEntityId,
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
    EntityGroupEntity findEntityGroupByTypeAndName(@Param("parentEntityId") String parentEntityId,
                                                   @Param("parentEntityType") String parentEntityType,
                                                   @Param("relationType") String relationType,
                                                   @Param("name") String name);

    @Query("SELECT e FROM EntityGroupEntity e, " +
            "RelationEntity re " +
            "WHERE e.id = re.toId AND re.toType = 'ENTITY_GROUP' " +
            "AND re.relationTypeGroup = 'TO_ENTITY_GROUP' " +
            "AND re.fromId = :parentEntityId AND re.fromType = :parentEntityType")
    List<EntityGroupEntity> findAllEntityGroups(@Param("parentEntityId") String parentEntityId,
                                                @Param("parentEntityType") String parentEntityType);

}
