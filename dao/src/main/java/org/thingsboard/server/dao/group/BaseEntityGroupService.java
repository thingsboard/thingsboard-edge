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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.dao.service.Validator.*;

@Service
@Slf4j
public class BaseEntityGroupService extends AbstractEntityService implements EntityGroupService {

    public static final String ENTITY_GROUP_RELATION_PREFIX = "ENTITY_GROUP_";

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Override
    public EntityGroup findEntityGroupById(EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupById [{}]", entityGroupId);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        return entityGroupDao.findById(entityGroupId.getId());
    }

    @Override
    public ListenableFuture<EntityGroup> findEntityGroupByIdAsync(EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupByIdAsync [{}]", entityGroupId);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        return entityGroupDao.findByIdAsync(entityGroupId.getId());
    }

    @Override
    public EntityGroup saveEntityGroup(EntityId parentEntityId, EntityGroup entityGroup) {
        log.trace("Executing saveEntityGroup [{}]", entityGroup);
        validateEntityId(parentEntityId, "Incorrect parentEntityId " + parentEntityId);
        new EntityGroupValidator(parentEntityId).validate(entityGroup);
        EntityGroup savedEntityGroup = entityGroupDao.save(entityGroup);
        if (entityGroup.getId() == null) {
            EntityRelation entityRelation = new EntityRelation();
            entityRelation.setFrom(parentEntityId);
            entityRelation.setTo(savedEntityGroup.getId());
            entityRelation.setTypeGroup(RelationTypeGroup.TO_ENTITY_GROUP);
            entityRelation.setType(ENTITY_GROUP_RELATION_PREFIX+savedEntityGroup.getType().name());
            relationService.saveRelation(entityRelation);
        }
        return savedEntityGroup;
    }

    @Override
    public EntityGroup createEntityGroupAll(EntityId parentEntityId, EntityType groupType) {
        log.trace("Executing createEntityGroupAll, parentEntityId [{}], groupType [{}]", parentEntityId, groupType);
        validateEntityId(parentEntityId, "Incorrect parentEntityId " + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException("Incorrect groupType " + groupType);
        }
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(EntityGroup.GROUP_ALL_NAME);
        entityGroup.setType(groupType);
        return saveEntityGroup(parentEntityId, entityGroup);
    }

    @Override
    public void deleteEntityGroup(EntityGroupId entityGroupId) {
        log.trace("Executing deleteEntityGroup [{}]", entityGroupId);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        deleteEntityRelations(entityGroupId);
        entityGroupDao.removeById(entityGroupId.getId());
    }

    @Override
    public ListenableFuture<List<EntityGroup>> findAllEntityGroups(EntityId parentEntityId) {
        log.trace("Executing findAllEntityGroups, parentEntityId [{}]", parentEntityId);
        validateEntityId(parentEntityId, "Incorrect parentEntityId " + parentEntityId);
        return entityGroupDao.findAllEntityGroups(parentEntityId);
    }

    @Override
    public void deleteAllEntityGroups(EntityId parentEntityId) {
        log.trace("Executing deleteAllEntityGroups, parentEntityId [{}]", parentEntityId);
        validateEntityId(parentEntityId, "Incorrect parentEntityId " + parentEntityId);
        ListenableFuture<List<EntityGroup>> entityGroupsFuture = entityGroupDao.findAllEntityGroups(parentEntityId);
        try {
            List<EntityGroup> entityGroups = entityGroupsFuture.get();
            entityGroups.forEach(entityGroup -> deleteEntityGroup(entityGroup.getId()));
        } catch (InterruptedException | ExecutionException e) {
            log.error("Unable to delete entity groups", e);
        }
    }

    @Override
    public ListenableFuture<List<EntityGroup>> findEntityGroupsByType(EntityId parentEntityId, EntityType groupType) {
        log.trace("Executing findEntityGroupsByType, parentEntityId [{}], groupType [{}]", parentEntityId, groupType);
        validateEntityId(parentEntityId, "Incorrect parentEntityId " + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException("Incorrect groupType " + groupType);
        }
        return entityGroupDao.findEntityGroupsByType(parentEntityId, groupType);
    }

    @Override
    public ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndName(EntityId parentEntityId, EntityType groupType, String name) {
        log.trace("Executing findEntityGroupByTypeAndName, parentEntityId [{}], groupType [{}], name [{}]", parentEntityId, groupType, name);
        validateEntityId(parentEntityId, "Incorrect parentEntityId " + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException("Incorrect groupType " + groupType);
        }
        validateString(name, "Incorrect name " + name);
        return entityGroupDao.findEntityGroupByTypeAndName(parentEntityId, groupType, name);
    }

    @Override
    public void addEntityToEntityGroup(EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing addEntityToEntityGroup, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        validateEntityId(entityId, "Incorrect entityId " + entityId);
        EntityRelation entityRelation = new EntityRelation();
        entityRelation.setFrom(entityGroupId);
        entityRelation.setTo(entityId);
        entityRelation.setTypeGroup(RelationTypeGroup.FROM_ENTITY_GROUP);
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);
        relationService.saveRelation(entityRelation);
    }

    @Override
    public void addEntityToEntityGroupAll(EntityId parentEntityId, EntityId entityId) {
        log.trace("Executing addEntityToEntityGroupAll, parentEntityId [{}], entityId [{}]", parentEntityId, entityId);
        validateEntityId(parentEntityId, "Incorrect parentEntityId " + parentEntityId);
        validateEntityId(entityId, "Incorrect entityId " + entityId);
        try {
            Optional<EntityGroup> entityGroup = entityGroupDao.findEntityGroupByTypeAndName(parentEntityId, entityId.getEntityType(), EntityGroup.GROUP_ALL_NAME).get();
            if (entityGroup.isPresent()) {
                addEntityToEntityGroup(entityGroup.get().getId(), entityId);
            } else {
                throw new DataValidationException("Group All of type " + entityId.getEntityType() + " is absent for entityId " + parentEntityId);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Unable to add entity to group All", e);
        }
    }

    @Override
    public void addEntitiesToEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds) {
        log.trace("Executing addEntitiesToEntityGroup, entityGroupId [{}], entityIds [{}]", entityGroupId, entityIds);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        entityIds.forEach(entityId -> addEntityToEntityGroup(entityGroupId, entityId));
    }

    @Override
    public void removeEntityFromEntityGroup(EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing removeEntityFromEntityGroup, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        validateEntityId(entityId, "Incorrect entityId " + entityId);
        relationService.deleteRelation(entityGroupId, entityId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP);
    }

    @Override
    public void removeEntitiesFromEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds) {
        log.trace("Executing removeEntitiesFromEntityGroup, entityGroupId [{}], entityIds [{}]", entityGroupId, entityIds);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        entityIds.forEach(entityId -> removeEntityFromEntityGroup(entityGroupId, entityId));
    }

    @Override
    public <T extends BaseData<? extends UUIDBased>> ListenableFuture<TimePageData<T>> findEntities(EntityGroupId entityGroupId, EntityType groupType,
                                                                                                    TimePageLink pageLink, Function<EntityId, T> transformFunction) {
        log.trace("Executing findEntities, entityGroupId [{}], groupType [{}], pageLink [{}]", entityGroupId, groupType, pageLink);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        if (groupType == null) {
            throw new IncorrectParameterException("Incorrect groupType " + groupType);
        }
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        if (transformFunction == null) {
            throw new IncorrectParameterException("Incorrect transformFunction " + transformFunction);
        }
        ListenableFuture<List<EntityId>> entityIds = entityGroupDao.findEntityIds(entityGroupId, groupType, pageLink);
        return Futures.transform(entityIds, new Function<List<EntityId>, TimePageData<T>>() {
            @Nullable
            @Override
            public TimePageData<T> apply(@Nullable List<EntityId> entityIds) {
                List<T> entities = new ArrayList<>();
                entityIds.forEach(entityId -> entities.add(transformFunction.apply(entityId)));
                return new TimePageData<>(entities, pageLink);
            }
        });
    }

    private class EntityGroupValidator extends DataValidator<EntityGroup> {

        private final EntityId parentEntityId;

        EntityGroupValidator(EntityId parentEntityId) {
            this.parentEntityId = parentEntityId;
        }

        @Override
        protected void validateCreate(EntityGroup entityGroup) {
            try {
                entityGroupDao.findEntityGroupByTypeAndName(this.parentEntityId, entityGroup.getType(), entityGroup.getName()).get().ifPresent(
                        d -> {
                            throw new DataValidationException("Entity group with such name already present in " +
                                    this.parentEntityId.getEntityType().toString() + "!");
                        }
                );
            } catch (InterruptedException | ExecutionException e) {
                log.error("Unable to validate creation of entity group.", e);
            }
        }

        @Override
        protected void validateUpdate(EntityGroup entityGroup) {
            try {
                entityGroupDao.findEntityGroupByTypeAndName(this.parentEntityId, entityGroup.getType(), entityGroup.getName()).get().ifPresent(
                        d -> {
                            if (!d.getId().equals(entityGroup.getId())) {
                                throw new DataValidationException("Entity group with such name already present in " +
                                        this.parentEntityId.getEntityType().toString() + "!");
                            }
                        }
                );
            } catch (InterruptedException | ExecutionException e) {
                log.error("Unable to validate update of entity group.", e);
            }
        }

        @Override
        protected void validateDataImpl(EntityGroup entityGroup) {
            if (entityGroup.getType() == null) {
                throw new DataValidationException("Entity group type should be specified!");
            }
            if (StringUtils.isEmpty(entityGroup.getName())) {
                throw new DataValidationException("Entity group name should be specified!");
            }
        }
    }

}
