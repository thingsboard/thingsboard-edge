/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.dao.group;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
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
import org.thingsboard.server.dao.relation.RelationDao;
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
    public static final String INCORRECT_PARENT_ENTITY_ID = "Incorrect parentEntityId ";
    public static final String INCORRECT_GROUP_TYPE = "Incorrect groupType ";
    public static final String INCORRECT_ENTITY_GROUP_ID = "Incorrect entityGroupId ";
    public static final String INCORRECT_ENTITY_ID = "Incorrect entityId ";
    public static final String UNABLE_TO_FIND_ENTITY_GROUP_BY_ID = "Unable to find entity group by id ";

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Autowired
    private RelationDao relationDao;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TimeseriesService timeseriesService;

    @Override
    public EntityGroup findEntityGroupById(EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupById [{}]", entityGroupId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        return entityGroupDao.findById(entityGroupId.getId());
    }

    @Override
    public ListenableFuture<EntityGroup> findEntityGroupByIdAsync(EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupByIdAsync [{}]", entityGroupId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        return entityGroupDao.findByIdAsync(entityGroupId.getId());
    }

    @Override
    public EntityGroup saveEntityGroup(EntityId parentEntityId, EntityGroup entityGroup) {
        log.trace("Executing saveEntityGroup [{}]", entityGroup);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        new EntityGroupValidator(parentEntityId).validate(entityGroup);
        if (entityGroup.getId() == null && entityGroup.getConfiguration() == null) {
            EntityGroupConfiguration entityGroupConfiguration =
                    EntityGroupConfiguration.createDefaultEntityGroupConfiguration(entityGroup.getType());
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonConfiguration = mapper.valueToTree(entityGroupConfiguration);
            jsonConfiguration.putObject("settings");
            jsonConfiguration.putObject("actions");
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
    public ListenableFuture<Boolean> checkEntityGroup(EntityId parentEntityId, EntityGroup entityGroup) {
        log.trace("Executing checkEntityGroup [{}]", entityGroup);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        return relationService.checkRelation(parentEntityId, entityGroup.getId(),
                ENTITY_GROUP_RELATION_PREFIX + entityGroup.getType().name()
                , RelationTypeGroup.TO_ENTITY_GROUP);
    }

    @Override
    public EntityGroup createEntityGroupAll(EntityId parentEntityId, EntityType groupType) {
        log.trace("Executing createEntityGroupAll, parentEntityId [{}], groupType [{}]", parentEntityId, groupType);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(EntityGroup.GROUP_ALL_NAME);
        entityGroup.setType(groupType);
        return saveEntityGroup(parentEntityId, entityGroup);
    }

    @Override
    public void deleteEntityGroup(EntityGroupId entityGroupId) {
        log.trace("Executing deleteEntityGroup [{}]", entityGroupId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        deleteEntityRelations(entityGroupId);
        entityGroupDao.removeById(entityGroupId.getId());
    }

    @Override
    public ListenableFuture<List<EntityGroup>> findAllEntityGroups(EntityId parentEntityId) {
        log.trace("Executing findAllEntityGroups, parentEntityId [{}]", parentEntityId);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByFrom(parentEntityId, RelationTypeGroup.TO_ENTITY_GROUP);
        return relationsToEntityGroups(relations);
    }

    @Override
    public void deleteAllEntityGroups(EntityId parentEntityId) {
        log.trace("Executing deleteAllEntityGroups, parentEntityId [{}]", parentEntityId);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        ListenableFuture<List<EntityGroup>> entityGroupsFuture = findAllEntityGroups(parentEntityId);
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
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        String relationType = ENTITY_GROUP_RELATION_PREFIX + groupType.name();
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByFromAndType(parentEntityId, relationType, RelationTypeGroup.TO_ENTITY_GROUP);
        return relationsToEntityGroups(relations);
    }

    private ListenableFuture<List<EntityGroup>> relationsToEntityGroups(ListenableFuture<List<EntityRelation>> relations) {
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<EntityGroup>> entityGroupFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                entityGroupFutures.add(entityGroupDao.findByIdAsync(relation.getTo().getId()));
            }
            return Futures.successfulAsList(entityGroupFutures);
        });
    }

    @Override
    public ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndName(EntityId parentEntityId, EntityType groupType, String name) {
        log.trace("Executing findEntityGroupByTypeAndName, parentEntityId [{}], groupType [{}], name [{}]", parentEntityId, groupType, name);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validateString(name, "Incorrect name " + name);
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
    public void addEntityToEntityGroup(EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing addEntityToEntityGroup, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        validateEntityId(entityId, INCORRECT_ENTITY_ID + entityId);
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
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        validateEntityId(entityId, INCORRECT_ENTITY_ID + entityId);
        try {
            Optional<EntityGroup> entityGroup = findEntityGroupByTypeAndName(parentEntityId, entityId.getEntityType(), EntityGroup.GROUP_ALL_NAME).get();
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
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        entityIds.forEach(entityId -> addEntityToEntityGroup(entityGroupId, entityId));
    }

    @Override
    public void removeEntityFromEntityGroup(EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing removeEntityFromEntityGroup, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        validateEntityId(entityId, INCORRECT_ENTITY_ID + entityId);
        relationService.deleteRelation(entityGroupId, entityId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP);
    }

    @Override
    public void removeEntitiesFromEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds) {
        log.trace("Executing removeEntitiesFromEntityGroup, entityGroupId [{}], entityIds [{}]", entityGroupId, entityIds);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        entityIds.forEach(entityId -> removeEntityFromEntityGroup(entityGroupId, entityId));
    }

    @Override
    public EntityView findGroupEntity(EntityGroupId entityGroupId, EntityId entityId,
                                                        BiFunction<EntityView, List<EntityField>, EntityView> transformFunction) {
        log.trace("Executing findGroupEntity, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        validateEntityId(entityId, INCORRECT_ENTITY_ID + entityId);

        try {
            if (!relationService.checkRelation(entityGroupId, entityId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP).get()) {
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
        }

        if (transformFunction == null) {
            throw new IncorrectParameterException("Incorrect transformFunction " + transformFunction);
        }
        EntityGroup entityGroup = findEntityGroupById(entityGroupId);
        if (entityGroup == null) {
            throw new IncorrectParameterException(UNABLE_TO_FIND_ENTITY_GROUP_BY_ID + entityGroupId);
        }
        EntityGroupColumnsInfo columnsInfo = getEntityGroupColumnsInfo(entityGroup);
        return toEntityView(entityId, columnsInfo, transformFunction);
    }

    @Override
    public ListenableFuture<TimePageData<EntityView>>
                            findEntities(EntityGroupId entityGroupId,
                                         TimePageLink pageLink, BiFunction<EntityView, List<EntityField>, EntityView> transformFunction) {
        log.trace("Executing findEntities, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        if (transformFunction == null) {
            throw new IncorrectParameterException("Incorrect transformFunction " + transformFunction);
        }
        EntityGroup entityGroup = findEntityGroupById(entityGroupId);
        if (entityGroup == null) {
            throw new IncorrectParameterException(UNABLE_TO_FIND_ENTITY_GROUP_BY_ID + entityGroupId);
        }
        EntityGroupColumnsInfo columnsInfo = getEntityGroupColumnsInfo(entityGroup);
        ListenableFuture<List<EntityId>> entityIds = findEntityIds(entityGroupId, entityGroup.getType(), pageLink);
        return Futures.transform(entityIds, new Function<List<EntityId>, TimePageData<EntityView>>() {
            @Nullable
            @Override
            public TimePageData<EntityView> apply(@Nullable List<EntityId> entityIds) {
                List<EntityView> entities = new ArrayList<>();
                if (entityIds != null) {
                    entityIds.forEach(entityId -> {
                        EntityView entityView = toEntityView(entityId, columnsInfo, transformFunction);
                        entities.add(entityView);
                    });
                }
                TimePageData<EntityView> result = new TimePageData<>(entities, pageLink);
                result.getData().removeIf(entity -> entity.isSkipEntity());
                return result;
            }
        });
    }

    @Override
    public ListenableFuture<List<EntityId>> findAllEntityIds(EntityGroupId entityGroupId, TimePageLink pageLink) {
        log.trace("Executing findEntities, entityGroupId [{}], pageLink [{}]", entityGroupId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        EntityGroup entityGroup = findEntityGroupById(entityGroupId);
        if (entityGroup == null) {
            throw new IncorrectParameterException(UNABLE_TO_FIND_ENTITY_GROUP_BY_ID + entityGroupId);
        }
        return findEntityIds(entityGroupId, entityGroup.getType(), pageLink);
    }

    @Override
    public ListenableFuture<List<EntityGroupId>> findEntityGroupsForEntity(EntityId entityId) {
        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByToAndType(entityId,
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP);
        return Futures.transform(relations, (Function<List<EntityRelation>, List<EntityGroupId>>) input -> {
            List<EntityGroupId> entityGroupIds = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                entityGroupIds.add(new EntityGroupId(relation.getFrom().getId()));
            }
            return entityGroupIds;
        });
    }

    private ListenableFuture<List<EntityId>> findEntityIds(EntityGroupId entityGroupId, EntityType groupType, TimePageLink pageLink) {
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

    private EntityGroupColumnsInfo getEntityGroupColumnsInfo(EntityGroup entityGroup) {
        JsonNode jsonConfiguration = entityGroup.getConfiguration();
        List<ColumnConfiguration> columns = null;
        if (jsonConfiguration != null) {
            try {
                EntityGroupConfiguration entityGroupConfiguration =
                        new ObjectMapper().treeToValue(jsonConfiguration, EntityGroupConfiguration.class);
                columns = entityGroupConfiguration.getColumns();
            } catch (JsonProcessingException e) {
                log.error("Unable to read entity group configuration", e);
                throw new RuntimeException("Unable to read entity group configuration", e);
            }
        }
        if (columns == null) {
            columns = Collections.emptyList();
        }
        EntityGroupColumnsInfo columnsInfo = new EntityGroupColumnsInfo();
        columns.forEach(column -> {
            if (column.getType() == ColumnType.ENTITY_FIELD) {
                processEntityFieldColumnInfo(column, columnsInfo);
            } else if (column.getType().isAttribute()) {
                processAttributeColumnInfo(column, columnsInfo);
            } else if (column.getType() == ColumnType.TIMESERIES) {
                columnsInfo.timeseriesKeys.add(column.getKey());
            }
        });
        return columnsInfo;
    }

    private void processEntityFieldColumnInfo(ColumnConfiguration column, EntityGroupColumnsInfo columnsInfo) {
        EntityField entityField = null;
        try {
            entityField = EntityField.valueOf(column.getKey().toUpperCase());
        } catch (Exception e) {
        }
        if (entityField != null) {
            if (entityField == EntityField.CREATED_TIME) {
                columnsInfo.commonEntityFields.add(entityField);
            } else {
                columnsInfo.entityFields.add(entityField);
            }
        }
    }

    private void processAttributeColumnInfo(ColumnConfiguration column, EntityGroupColumnsInfo columnsInfo) {
        String scope = column.getType().getAttributeScope();
        List<String> keys = columnsInfo.attributeScopeToKeysMap.get(scope);
        if (keys == null) {
            keys = new ArrayList<>();
            columnsInfo.attributeScopeToKeysMap.put(scope, keys);
        }
        keys.add(column.getKey());
    }

    private EntityView toEntityView(EntityId entityId, EntityGroupColumnsInfo columnsInfo,
                                    BiFunction<EntityView, List<EntityField>, EntityView> transformFunction) {
        EntityView entityView = new EntityView(entityId);
        for (EntityField entityField : columnsInfo.commonEntityFields) {
            if (entityField == EntityField.CREATED_TIME) {
                long timestamp = UUIDs.unixTimestamp(entityId.getId());
                entityView.put(EntityField.CREATED_TIME.name().toLowerCase(), timestamp+"");
            }
        }
        if (!columnsInfo.entityFields.isEmpty()) {
            entityView = transformFunction.apply(entityView, columnsInfo.entityFields);
        }
        if (!entityView.isSkipEntity()) {
            fetchEntityAttributes(entityView, columnsInfo.attributeScopeToKeysMap, columnsInfo.timeseriesKeys);
        }
        return entityView;
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

    private class EntityGroupColumnsInfo {
        List<EntityField> commonEntityFields = new ArrayList<>();
        List<EntityField> entityFields = new ArrayList<>();
        Map<String, List<String>> attributeScopeToKeysMap = new HashMap<>();
        List<String> timeseriesKeys = new ArrayList<>();
    }

    private class EntityGroupValidator extends DataValidator<EntityGroup> {

        private final EntityId parentEntityId;

        EntityGroupValidator(EntityId parentEntityId) {
            this.parentEntityId = parentEntityId;
        }

        @Override
        protected void validateCreate(EntityGroup entityGroup) {
            try {
                findEntityGroupByTypeAndName(this.parentEntityId, entityGroup.getType(), entityGroup.getName()).get().ifPresent(
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
                findEntityGroupByTypeAndName(this.parentEntityId, entityGroup.getType(), entityGroup.getName()).get().ifPresent(
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
