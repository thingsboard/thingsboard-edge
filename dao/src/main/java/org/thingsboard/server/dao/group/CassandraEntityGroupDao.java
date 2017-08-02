/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

package org.thingsboard.server.dao.group;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.model.nosql.EntityGroupEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractModelDao;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_COLUMN_FAMILY_NAME;

@Component
@Slf4j
@NoSqlDao
public class CassandraEntityGroupDao extends CassandraAbstractModelDao<EntityGroupEntity, EntityGroup> implements EntityGroupDao {

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<EntityGroupEntity> getColumnFamilyClass() {
        return EntityGroupEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ENTITY_GROUP_COLUMN_FAMILY_NAME;
    }

    @Override
    public EntityGroup save(EntityGroup entityGroup) {
        log.debug("Save entityGroup [{}] ", entityGroup);
        return super.save(entityGroup);
    }

    @Override
    public ListenableFuture<List<EntityGroup>> findAllEntityGroups(EntityId parentEntityId) {
        log.trace("Try to find all entity groups by entity [{}]", parentEntityId);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByFrom(parentEntityId, RelationTypeGroup.TO_ENTITY_GROUP);
        return Futures.transform(relations, (AsyncFunction<List<EntityRelation>, List<EntityGroup>>) input -> {
            List<ListenableFuture<EntityGroup>> entityGroupFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                entityGroupFutures.add(findByIdAsync(relation.getTo().getId()));
            }
            return Futures.successfulAsList(entityGroupFutures);
        });
    }

    @Override
    public ListenableFuture<List<EntityGroup>> findEntityGroupsByType(EntityId parentEntityId, EntityType groupType) {
        log.trace("Try to find all entity groups by entity [{}] and group type [{}]", parentEntityId, groupType);
        String relationType = BaseEntityGroupService.ENTITY_GROUP_RELATION_PREFIX + groupType.name();
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByFromAndType(parentEntityId, relationType, RelationTypeGroup.TO_ENTITY_GROUP);
        return Futures.transform(relations, (AsyncFunction<List<EntityRelation>, List<EntityGroup>>) input -> {
            List<ListenableFuture<EntityGroup>> entityGroupFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                entityGroupFutures.add(findByIdAsync(relation.getTo().getId()));
            }
            return Futures.successfulAsList(entityGroupFutures);
        });
    }

    @Override
    public ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndName(EntityId parentEntityId, EntityType groupType, String name) {
        log.trace("Try to find entity group by entity [{}], group type [{}] and name [{}]", parentEntityId, groupType, name);
        ListenableFuture<List<EntityGroup>> entityGroups = findEntityGroupsByType(parentEntityId, groupType);
        return Futures.transform(entityGroups, (Function<List<EntityGroup>, Optional<EntityGroup>>) input -> {
            for (EntityGroup entityGroup : input) {
                if (entityGroup.getName().equals(name)) {
                    return Optional.of(entityGroup);
                }
            }
            return Optional.empty();
        });
    }

    @Override
    public ListenableFuture<List<EntityId>> findEntityIds(EntityGroupId entityGroupId, EntityType groupType, TimePageLink pageLink) {
        log.trace("Try to find entity ids by entityGroupId [{}], groupType [{}] and pageLink [{}]", entityGroupId, groupType, pageLink);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findRelations(entityGroupId,
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP, groupType, pageLink);
        return Futures.transform(relations, (Function<List<EntityRelation>, List<EntityId>>) input -> {
            List<EntityId> entityIds = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                entityIds.add(relation.getTo());
            }
            return entityIds;
        });
    }

}
