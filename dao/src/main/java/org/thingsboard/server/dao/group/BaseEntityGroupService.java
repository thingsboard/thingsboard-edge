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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.group.*;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

import static org.thingsboard.server.dao.service.Validator.*;

@Service
@Slf4j
public class BaseEntityGroupService extends AbstractEntityService implements EntityGroupService {

    public static final String ENTITY_GROUP_RELATION_PREFIX = "ENTITY_GROUP_";

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TimeseriesService timeseriesService;

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
        if (entityGroup.getId() == null && entityGroup.getConfiguration() == null) {
            EntityGroupConfiguration entityGroupConfiguration =
                    EntityGroupConfiguration.createDefaultEntityGroupConfiguration(entityGroup.getType());
            JsonNode jsonConfiguration = new ObjectMapper().valueToTree(entityGroupConfiguration);
            entityGroup.setConfiguration(jsonConfiguration);
        }
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
    public ListenableFuture<TimePageData<EntityView>>
                            findEntities(EntityGroupId entityGroupId,
                                         TimePageLink pageLink, BiFunction<EntityView, List<EntityField>, EntityView> transformFunction) {
        log.trace("Executing findEntities, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        if (transformFunction == null) {
            throw new IncorrectParameterException("Incorrect transformFunction " + transformFunction);
        }
        EntityGroup entityGroup = findEntityGroupById(entityGroupId);
        if (entityGroup == null) {
            throw new IncorrectParameterException("Unable to find entity group by id " + entityGroupId);
        }
        List<ColumnConfiguration> columns = getEntityGroupColumns(entityGroup);
        List<EntityField> commonEntityFields = new ArrayList<>();
        List<EntityField> entityFields = new ArrayList<>();
        Map<String, List<String>> attributeScopeToKeysMap = new HashMap<>();
        List<String> timeseriesKeys = new ArrayList<>();
        columns.forEach(column -> {
            if (column.getType() == ColumnType.ENTITY_FIELD) {
                EntityField entityField = null;
                try {
                    entityField = EntityField.valueOf(column.getKey().toUpperCase());
                } catch (Exception e) {}
                if (entityField != null) {
                    if (entityField == EntityField.CREATED_TIME) {
                        commonEntityFields.add(entityField);
                    } else {
                        entityFields.add(entityField);
                    }
                }
            } else if (column.getType().isAttribute()) {
                String scope = column.getType().getAttributeScope();
                List<String> keys = attributeScopeToKeysMap.get(scope);
                if (keys == null) {
                    keys = new ArrayList<>();
                    attributeScopeToKeysMap.put(scope, keys);
                }
                keys.add(column.getKey());
            } else if (column.getType() == ColumnType.TIMESERIES) {
                timeseriesKeys.add(column.getKey());
            }
        });
        ListenableFuture<List<EntityId>> entityIds = entityGroupDao.findEntityIds(entityGroupId, entityGroup.getType(), pageLink);
        return Futures.transform(entityIds, new Function<List<EntityId>, TimePageData<EntityView>>() {
            @Nullable
            @Override
            public TimePageData<EntityView> apply(@Nullable List<EntityId> entityIds) {
                List<EntityView> entities = new ArrayList<>();
                entityIds.forEach(entityId -> {
                    EntityView entityView = new EntityView(entityId);
                    for (EntityField entityField : commonEntityFields) {
                        if (entityField == EntityField.CREATED_TIME) {
                            long timestamp = UUIDs.unixTimestamp(entityId.getId());
                            entityView.put(EntityField.CREATED_TIME.name().toLowerCase(), timestamp+"");
                        }
                    }
                    if (!entityFields.isEmpty()) {
                        entityView = transformFunction.apply(entityView, entityFields);
                    }
                    fetchEntityAttributes(entityView, attributeScopeToKeysMap, timeseriesKeys);
                    entities.add(entityView);
                });
                return new TimePageData<>(entities, pageLink);
            }
        });
    }

    private List<ColumnConfiguration> getEntityGroupColumns(EntityGroup entityGroup) {
        JsonNode jsonConfiguration = entityGroup.getConfiguration();
        if (jsonConfiguration != null) {
            try {
                EntityGroupConfiguration entityGroupConfiguration =
                        new ObjectMapper().treeToValue(jsonConfiguration, EntityGroupConfiguration.class);
                List<ColumnConfiguration> columns = entityGroupConfiguration.getColumns();
                if (columns == null) {
                    return Collections.emptyList();
                } else {
                    return columns;
                }
            } catch (JsonProcessingException e) {
                log.error("Unable to read entity group configuration", e);
                throw new RuntimeException("Unable to read entity group configuration", e);
            }
        }
        return Collections.emptyList();
    }

    private void fetchEntityAttributes(EntityView entityView,
                                       Map<String, List<String>> attributeScopeToKeysMap,
                                       List<String> timeseriesKeys) {
        EntityId entityId = entityView.getId();
        attributeScopeToKeysMap.forEach( (scope, attributeKeys) -> {
            try {
                List<AttributeKvEntry> attributeKvEntries = attributesService.find(entityId, scope, attributeKeys).get();
                attributeKvEntries.forEach(attributeKvEntry -> {
                    entityView.put(attributeKvEntry.getKey(), attributeKvEntry.getValueAsString());
                });
            } catch (InterruptedException | ExecutionException e) {
                log.error("Unable to fetch entity attributes", e);
            }
        });
        if (!timeseriesKeys.isEmpty()) {
            try {
                List<TsKvEntry> tsKvEntries = timeseriesService.findLatest(entityId, timeseriesKeys).get();
                tsKvEntries.forEach(tsKvEntry -> {
                    entityView.put(tsKvEntry.getKey(), tsKvEntry.getValueAsString());
                });
            } catch (InterruptedException | ExecutionException e) {
                log.error("Unable to fetch entity latest timeseries", e);
            }
        }
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
