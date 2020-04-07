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

import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.group.ColumnConfiguration;
import org.thingsboard.server.common.data.group.ColumnType;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SqlDao
@Repository
@Slf4j
public class DefaultGroupEntitiesRepository implements GroupEntitiesRepository {

    private static final Map<EntityType, String> entityTableMap = new HashMap<>();
    static {
        entityTableMap.put(EntityType.ASSET, "asset");
        entityTableMap.put(EntityType.DEVICE, "device");
        entityTableMap.put(EntityType.ENTITY_VIEW, "entity_view");
        entityTableMap.put(EntityType.DASHBOARD, "dashboard");
        entityTableMap.put(EntityType.CUSTOMER, "customer");
        entityTableMap.put(EntityType.USER, "tb_user");
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PageData<ShortEntityView> findGroupEntities(EntityType entityType, UUID groupId,
                                                       List<ColumnConfiguration> columns, PageLink pageLink) {

        List<ColumnMapping> mappings = toColumnMapping(columns);

        String countQuery = String.format("select count(entity0_.id) from %s as entity0_, relation as relation1_ where %s",
                entityTableMap.get(entityType), this.buildWhere(mappings, entityType, groupId, pageLink.getTextSearch()));

        int totalElements = ((BigInteger)entityManager.createNativeQuery(countQuery)
                .getSingleResult()).intValue();

        int aliasCounter = 0;

        String sortOrderProperty = pageLink.getSortOrder() != null ? pageLink.getSortOrder().getProperty() : null;
        String orderBySelection = null;
        List<String> selections = new ArrayList<>();
        String alias = String.format("alias%s", aliasCounter++);
        selections.add(String.format("entity0_.id as %s", alias));
        if (!StringUtils.isEmpty(sortOrderProperty) && sortOrderProperty.equals(EntityField.CREATED_TIME.name().toLowerCase())) {
            orderBySelection = alias;
        }
        for (ColumnMapping column : mappings) {
            String columnSelection = null;
            ColumnType type = column.column.getType();
            if (type == ColumnType.ENTITY_FIELD) {
                if (column.entityField != EntityField.CREATED_TIME) {
                    alias = String.format("alias%s", aliasCounter++);
                    columnSelection = String.format("entity0_.%s as %s", column.entityField.getColumnName(), alias);
                }
            } else if (type.isAttribute()) {
                alias = String.format("alias%s", aliasCounter++);
                columnSelection = String.format("%s as %s", this.buildAttributeSelection(type.getAttributeScope(), column.column.getKey()), alias);
            } else {
                alias = String.format("alias%s", aliasCounter++);
                columnSelection = String.format("%s as %s", this.buildTimeseriesSelection(column.column.getKey()), alias);
            }
            if (columnSelection != null) {
                selections.add(columnSelection);
                if (orderBySelection == null && column.propertyName.equals(sortOrderProperty)) {
                    orderBySelection = alias;
                }
            }
        }

        String criteriaQuery = String.format("select %s from %s as entity0_, relation as relation1_ where %s",
                String.join(", ", selections),
                entityTableMap.get(entityType),
                this.buildWhere(mappings, entityType, groupId, pageLink.getTextSearch()));

        if (orderBySelection != null) {
            criteriaQuery = String.format("%s order by %s", criteriaQuery, orderBySelection);
            if (pageLink.getSortOrder().getDirection() == SortOrder.Direction.ASC) {
                criteriaQuery += " asc";
            } else {
                criteriaQuery += " desc";
            }
        }
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        if (pageLink.getPageSize() > 0) {
            criteriaQuery = String.format("%s limit %s offset %s", criteriaQuery, pageLink.getPageSize(), startIndex);
        }

        List result = entityManager.createNativeQuery(criteriaQuery).getResultList();
        int totalPages = pageLink.getPageSize() > 0 ? (int)Math.ceil((float)totalElements / pageLink.getPageSize()) : 1;
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + result.size();
        List<ShortEntityView> entityViews = convertListToShortEntityView(result, entityType, mappings);
        return new PageData<>(entityViews, totalPages, totalElements, hasNext);
    }

    @Override
    public ShortEntityView findGroupEntity(EntityId entityId, UUID groupId, List<ColumnConfiguration> columns) {
        List<ColumnMapping> mappings = toColumnMapping(columns);

        int aliasCounter = 0;
        List<String> selections = new ArrayList<>();
        String alias = String.format("alias%s", aliasCounter++);
        selections.add(String.format("entity0_.id as %s", alias));
        for (ColumnMapping column : mappings) {
            String columnSelection = null;
            ColumnType type = column.column.getType();
            if (type == ColumnType.ENTITY_FIELD) {
                if (column.entityField != EntityField.CREATED_TIME) {
                    alias = String.format("alias%s", aliasCounter++);
                    columnSelection = String.format("entity0_.%s as %s", column.entityField.getColumnName(), alias);
                }
            } else if (type.isAttribute()) {
                alias = String.format("alias%s", aliasCounter++);
                columnSelection = String.format("%s as %s", this.buildAttributeSelection(type.getAttributeScope(), column.column.getKey()), alias);
            } else {
                alias = String.format("alias%s", aliasCounter++);
                columnSelection = String.format("%s as %s", this.buildTimeseriesSelection(column.column.getKey()), alias);
            }
            if (columnSelection != null) {
                selections.add(columnSelection);
            }
        }

        String criteriaQuery = String.format("select %s from %s as entity0_, relation as relation1_ where %s",
                String.join(", ", selections),
                entityTableMap.get(entityId.getEntityType()),
                this.buildSingleEntityGroupRelationQuery(entityId, groupId));

        try {
            Object result = entityManager.createNativeQuery(criteriaQuery).getSingleResult();
            return toShortEntityView(result, entityId.getEntityType(), mappings);
        } catch (NoResultException e) {
            return null;
        }
    }

    private String buildWhere(List<ColumnMapping> mappings, EntityType entityType, UUID groupId, String searchText) {
        String groupRelationPredicate = this.buildGroupRelationQuery(entityType, groupId);
        String searchPredicate = this.buildSearchQuery(mappings, searchText);
        if (searchPredicate != null) {
            return String.join(" and ", groupRelationPredicate, searchPredicate);
        } else {
            return groupRelationPredicate;
        }
    }

    private String buildGroupRelationQuery(EntityType entityType, UUID groupId) {
        return String.format("entity0_.id=relation1_.to_id" +
                " and relation1_.to_type='%s'" +
                " and relation1_.relation_type_group='%s'" +
                " and relation1_.relation_type='Contains'" +
                " and relation1_.from_type='%s'" +
                " and relation1_.from_id='%s'",
                entityType.name(),
                RelationTypeGroup.FROM_ENTITY_GROUP.name(),
                EntityType.ENTITY_GROUP.name(),
                UUIDConverter.fromTimeUUID(groupId));
    }

    private String buildSingleEntityGroupRelationQuery(EntityId entityId, UUID groupId) {
        return String.format("entity0_.id=relation1_.to_id" +
                        " and relation1_.to_id='%s'" +
                        " and relation1_.to_type='%s'" +
                        " and relation1_.relation_type_group='%s'" +
                        " and relation1_.relation_type='Contains'" +
                        " and relation1_.from_type='%s'" +
                        " and relation1_.from_id='%s'",
                UUIDConverter.fromTimeUUID(entityId.getId()),
                entityId.getEntityType().name(),
                RelationTypeGroup.FROM_ENTITY_GROUP.name(),
                EntityType.ENTITY_GROUP.name(),
                UUIDConverter.fromTimeUUID(groupId));
    }

    private String buildSearchQuery(List<ColumnMapping> mappings, String searchText) {
        List<String> searchPredicates = new ArrayList<>();
        if (!StringUtils.isEmpty(searchText)) {
            searchText = searchText.toLowerCase() + "%";
            for (ColumnMapping column : mappings) {
                if (column.searchable) {
                    ColumnType type = column.column.getType();
                    if (type == ColumnType.ENTITY_FIELD) {
                        searchPredicates.add(this.buildEntitySearch(column.entityField.getColumnName(), searchText));
                    } else if (type.isAttribute()) {
                        searchPredicates.add(this.buildAttributeSearch(type.getAttributeScope(), column.column.getKey(), searchText));
                    } else {
                        //TODO: timeseries
                    }
                }
            }
        }
        if (!searchPredicates.isEmpty()) {
            return String.format("(%s)", String.join(" or ", searchPredicates));
        } else {
            return null;
        }
    }

    private String buildEntitySearch(String columnName, String searchText) {
        return String.format("LOWER(entity0_.%s) LIKE '%s'", columnName, searchText);
    }

    private String buildAttributeSearch(String attributeType, String attributeKey, String searchText) {
        return String.format("entity0_.id in (select attr.entity_id " +
                "from attribute_kv as attr where attr.entity_id = entity0_.id " +
                "and attr.attribute_type='%s' and attr.attribute_key='%s' " +
                "and LOWER(" +
                "coalesce(cast(attr.bool_v as varchar), '') || " +
                "coalesce(attr.str_v, '') || " +
                "coalesce(cast(attr.long_v as varchar), '') || " +
                "coalesce(cast(attr.dbl_v as varchar), '') || " +
                "coalesce(cast(attr.json_v as varchar), ''))" +
                " LIKE '%s')", attributeType, attributeKey, searchText);
    }

    private String buildAttributeSelection(String attributeType,
                                           String attributeKey) {

        return String.format("(select (" +
                "coalesce(cast(attr.bool_v as varchar), '') || " +
                "coalesce(attr.str_v, '') || " +
                "coalesce(cast(attr.long_v as varchar), '') || " +
                "coalesce(cast(attr.dbl_v as varchar), '') || " +
                "coalesce(cast(attr.json_v as varchar), '')) " +
                "from attribute_kv as attr where attr.entity_id = entity0_.id " +
                "and attr.attribute_type='%s' and attr.attribute_key='%s')", attributeType, attributeKey);
    }

    private String buildTimeseriesSelection(String timeseriesKey) {
        // TODO: timeseries
        return "(select '')";
    }

    private List<ShortEntityView> convertListToShortEntityView(List<Object> result, EntityType entityType, List<ColumnMapping> columns) {
        return result.stream().map(obj -> this.toShortEntityView(obj, entityType, columns)).collect(Collectors.toList());
    }

    private ShortEntityView toShortEntityView(Object obj, EntityType entityType, List<ColumnMapping> columns) {
        String id = obj instanceof String ? (String)obj : (String)((Object[]) obj)[0];
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, UUIDConverter.fromString(id));
        ShortEntityView entity = new ShortEntityView(entityId);
        for (ColumnMapping column : columns) {
            if (column.column.getType() == ColumnType.ENTITY_FIELD && column.entityField == EntityField.CREATED_TIME) {
                long timestamp = UUIDs.unixTimestamp(entity.getId().getId());
                entity.put(EntityField.CREATED_TIME.name().toLowerCase(), timestamp + "");
            } else {
                Object value = ((Object[]) obj)[column.propertyIndex];
                entity.put(column.propertyName, value != null ? value.toString() : null);
            }
        }
        return entity;
    }

    private List<ColumnMapping> toColumnMapping(List<ColumnConfiguration> columns) {
        List<ColumnMapping> columnMappings = new ArrayList<>();
        int index = 0;
        Set<String> uniqueProperties = new HashSet<>();
        for (ColumnConfiguration column: columns) {
            ColumnMapping mapping = toColumnMapping(column);
            if (mapping != null) {
                if (uniqueProperties.add(mapping.propertyName)) {
                    if (column.getType() == ColumnType.ENTITY_FIELD && mapping.entityField == EntityField.CREATED_TIME) {
                        mapping.propertyIndex = 0;
                    } else {
                        mapping.propertyIndex = ++index;
                    }
                    columnMappings.add(mapping);
                }
            }
        }
       return columnMappings;
    }

    private ColumnMapping toColumnMapping(ColumnConfiguration column) {
        ColumnMapping mapping = new ColumnMapping();
        mapping.column = column;
        mapping.searchable = false;
        ColumnType type = column.getType();
        if (type == ColumnType.ENTITY_FIELD) {
            mapping.propertyName = column.getKey();
            EntityField entityField = null;
            try {
                entityField = EntityField.valueOf(column.getKey().toUpperCase());
            } catch (Exception ignored) {
                return null;
            }
            mapping.entityField = entityField;
            if (entityField != null) {
                mapping.searchable = entityField.isSearchable();
            }
        } else if (type.isAttribute()) {
            String attributeScope = type.getAttributeScope();
            switch (attributeScope) {
                case DataConstants.CLIENT_SCOPE:
                    mapping.propertyName = "client_"+column.getKey();
                    break;
                case DataConstants.SHARED_SCOPE:
                    mapping.propertyName = "shared_"+column.getKey();
                    break;
                case DataConstants.SERVER_SCOPE:
                    mapping.propertyName = "server_"+column.getKey();
                    break;
            }
            mapping.searchable = true;
        } else {
            mapping.propertyName = "timeseries_"+column.getKey();
            mapping.searchable = true;
        }
        return mapping;
    }

    private static class ColumnMapping {
        ColumnConfiguration column;
        EntityField entityField;
        String propertyName;
        boolean searchable;
        int propertyIndex;
    }

}
