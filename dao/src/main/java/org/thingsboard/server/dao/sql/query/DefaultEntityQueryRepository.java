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
package org.thingsboard.server.dao.sql.query;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.AssetSearchQueryFilter;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.DeviceSearchQueryFilter;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityGroupListFilter;
import org.thingsboard.server.common.data.query.EntityGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntitySearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityViewSearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.EntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@SqlDao
@Repository
@Slf4j
public class DefaultEntityQueryRepository implements EntityQueryRepository {
    private static final Map<EntityType, String> entityTableMap = new HashMap<>();
    private static final String SELECT_PHONE = " CASE WHEN entity.entity_type = 'TENANT' THEN (select phone from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' THEN (select phone from customer where id = entity_id) END as phone";
    private static final String SELECT_ZIP = " CASE WHEN entity.entity_type = 'TENANT' THEN (select zip from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' THEN (select zip from customer where id = entity_id) END as zip";
    private static final String SELECT_ADDRESS_2 = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select address2 from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select address2 from customer where id = entity_id) END as address2";
    private static final String SELECT_ADDRESS = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select address from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select address from customer where id = entity_id) END as address";
    private static final String SELECT_CITY = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select city from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select city from customer where id = entity_id) END as city";
    private static final String SELECT_STATE = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select state from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select state from customer where id = entity_id) END as state";
    private static final String SELECT_COUNTRY = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select country from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select country from customer where id = entity_id) END as country";
    private static final String SELECT_TITLE = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select title from tenant where id = entity_id) WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select title from customer where id = entity_id) END as title";
    private static final String SELECT_LAST_NAME = " CASE WHEN entity.entity_type = 'USER'" +
            " THEN (select last_name from tb_user where id = entity_id) END as last_name";
    private static final String SELECT_FIRST_NAME = " CASE WHEN entity.entity_type = 'USER'" +
            " THEN (select first_name from tb_user where id = entity_id) END as first_name";
    private static final String SELECT_REGION = " CASE WHEN entity.entity_type = 'TENANT'" +
            " THEN (select region from tenant where id = entity_id) END as region";
    private static final String SELECT_EMAIL = " CASE" +
            " WHEN entity.entity_type = 'TENANT'" +
            " THEN (select email from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select email from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select email from tb_user where id = entity_id)" +
            " END as email";
    private static final String SELECT_CUSTOMER_ID = "CASE" +
            " WHEN entity.entity_type = 'TENANT'" +
            " THEN UUID('" + TenantId.NULL_UUID + "')" +
            " WHEN entity.entity_type = 'CUSTOMER' THEN entity_id" +
            " WHEN entity.entity_type = 'ROLE'" +
            " THEN (select customer_id from role where id = entity_id)" +
            " WHEN entity.entity_type = 'SCHEDULER_EVENT'" +
            " THEN (select customer_id from scheduler_event where id = entity_id)" +
            " WHEN entity.entity_type = 'BLOB_ENTITY'" +
            " THEN (select customer_id from blob_entity where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select customer_id from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'DASHBOARD'" +
            //TODO: parse assigned customers or use contains?
            " THEN NULL" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select customer_id from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select customer_id from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select customer_id from entity_view where id = entity_id)" +
            " END as customer_id";
    private static final String SELECT_TENANT_ID = "SELECT CASE" +
            " WHEN entity.entity_type = 'TENANT' THEN entity_id" +
            " WHEN entity.entity_type = 'INTEGRATION'" +
            " THEN (select tenant_id from integration where id = entity_id)" +
            " WHEN entity.entity_type = 'CONVERTER'" +
            " THEN (select tenant_id from converter where id = entity_id)" +
            " WHEN entity.entity_type = 'ROLE'" +
            " THEN (select tenant_id from role where id = entity_id)" +
            " WHEN entity.entity_type = 'SCHEDULER_EVENT'" +
            " THEN (select tenant_id from scheduler_event where id = entity_id)" +
            " WHEN entity.entity_type = 'BLOB_ENTITY'" +
            " THEN (select tenant_id from blob_entity where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER'" +
            " THEN (select tenant_id from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select tenant_id from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'DASHBOARD'" +
            " THEN (select tenant_id from dashboard where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select tenant_id from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select tenant_id from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select tenant_id from entity_view where id = entity_id)" +
            " END as tenant_id";
    private static final String SELECT_CREATED_TIME = " CASE" +
            " WHEN entity.entity_type = 'TENANT'" +
            " THEN (select created_time from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'INTEGRATION'" +
            " THEN (select created_time from integration where id = entity_id)" +
            " WHEN entity.entity_type = 'CONVERTER'" +
            " THEN (select created_time from converter where id = entity_id)" +
            " WHEN entity.entity_type = 'ROLE'" +
            " THEN (select created_time from role where id = entity_id)" +
            " WHEN entity.entity_type = 'SCHEDULER_EVENT'" +
            " THEN (select created_time from scheduler_event where id = entity_id)" +
            " WHEN entity.entity_type = 'BLOB_ENTITY'" +
            " THEN (select created_time from blob_entity where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select created_time from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select created_time from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'DASHBOARD'" +
            " THEN (select created_time from dashboard where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select created_time from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select created_time from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select created_time from entity_view where id = entity_id)" +
            " END as created_time";
    private static final String SELECT_NAME = " CASE" +
            " WHEN entity.entity_type = 'TENANT'" +
            " THEN (select title from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'INTEGRATION'" +
            " THEN (select name from integration where id = entity_id)" +
            " WHEN entity.entity_type = 'CONVERTER'" +
            " THEN (select name from converter where id = entity_id)" +
            " WHEN entity.entity_type = 'ROLE'" +
            " THEN (select name from role where id = entity_id)" +
            " WHEN entity.entity_type = 'SCHEDULER_EVENT'" +
            " THEN (select name from scheduler_event where id = entity_id)" +
            " WHEN entity.entity_type = 'BLOB_ENTITY'" +
            " THEN (select name from blob_entity where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select title from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select CONCAT (first_name, ' ', last_name) from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'DASHBOARD'" +
            " THEN (select title from dashboard where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select name from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select name from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select name from entity_view where id = entity_id)" +
            " END as name";
    private static final String SELECT_TYPE = " CASE" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select authority from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select type from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select type from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select type from entity_view where id = entity_id)" +
            " WHEN entity.entity_type = 'SCHEDULER_EVENT'" +
            " THEN (select type from scheduler_event where id = entity_id)" +
            " WHEN entity.entity_type = 'BLOB_ENTITY'" +
            " THEN (select type from blob_entity where id = entity_id)" +
            " ELSE entity.entity_type END as type";
    private static final String SELECT_LABEL = " CASE" +
            " WHEN entity.entity_type = 'TENANT'" +
            " THEN (select title from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'INTEGRATION'" +
            " THEN (select name from integration where id = entity_id)" +
            " WHEN entity.entity_type = 'CONVERTER'" +
            " THEN (select name from converter where id = entity_id)" +
            " WHEN entity.entity_type = 'ROLE'" +
            " THEN (select name from role where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select title from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select CONCAT (first_name, ' ', last_name) from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'DASHBOARD'" +
            " THEN (select title from dashboard where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select label from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select label from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select name from entity_view where id = entity_id)" +
            " END as label";

    public static final String ATTR_READ_FLAG = "attr_read";
    public static final String TS_READ_FLAG = "ts_read";

    static {
        entityTableMap.put(EntityType.ENTITY_GROUP, "entity_group");
        entityTableMap.put(EntityType.ASSET, "asset");
        entityTableMap.put(EntityType.DEVICE, "device");
        entityTableMap.put(EntityType.ENTITY_VIEW, "entity_view");
        entityTableMap.put(EntityType.DASHBOARD, "dashboard");
        entityTableMap.put(EntityType.CUSTOMER, "customer");
        entityTableMap.put(EntityType.USER, "tb_user");
        entityTableMap.put(EntityType.TENANT, "tenant");
    }

    public static EntityType[] RELATION_QUERY_ENTITY_TYPES = new EntityType[]{
            EntityType.TENANT, EntityType.CUSTOMER, EntityType.USER, EntityType.DASHBOARD, EntityType.ASSET, EntityType.DEVICE,
            EntityType.CONVERTER, EntityType.INTEGRATION, EntityType.ENTITY_VIEW, EntityType.ROLE, EntityType.SCHEDULER_EVENT, EntityType.BLOB_ENTITY};

    private static final String HIERARCHICAL_GROUPS_ALL_QUERY = "select id from entity_group where owner_id in (" +
            " (WITH RECURSIVE customers_ids(id) AS" +
            " (SELECT id id" +
            " FROM customer" +
            " WHERE tenant_id = :permissions_tenant_id" +
            " and id = :permissions_customer_id" +
            " UNION" +
            " SELECT c.id id" +
            " FROM customer c," +
            " customers_ids parent" +
            " WHERE c.tenant_id = :permissions_tenant_id" +
            " and c.parent_customer_id = parent.id)" +
            " SELECT id" +
            " FROM customers_ids)) and name = 'All'";

    private static final String HIERARCHICAL_SUB_CUSTOMERS_QUERY = "(WITH RECURSIVE customers_ids(id) AS" +
            " (SELECT id id FROM customer WHERE tenant_id = :permissions_tenant_id and id = :permissions_customer_id" +
            " UNION SELECT c.id id FROM customer c, customers_ids parent WHERE c.tenant_id = :permissions_tenant_id" +
            " and c.parent_customer_id = parent.id) SELECT id FROM customers_ids)";

    private static final String HIERARCHICAL_QUERY_TEMPLATE = " FROM (WITH RECURSIVE related_entities(from_id, from_type, to_id, to_type, relation_type, lvl) AS (" +
            " SELECT from_id, from_type, to_id, to_type, relation_type, 1 as lvl" +
            " FROM relation" +
            " WHERE $in_id = :relation_root_id and $in_type = :relation_root_type and relation_type_group = 'COMMON'" +
            " UNION ALL" +
            " SELECT r.from_id, r.from_type, r.to_id, r.to_type, r.relation_type, lvl + 1" +
            " FROM relation r" +
            " INNER JOIN related_entities re ON" +
            " r.$in_id = re.$out_id and r.$in_type = re.$out_type and" +
            " relation_type_group = 'COMMON' %s)" +
            " SELECT re.$out_id entity_id, re.$out_type entity_type, re.lvl lvl" +
            " from related_entities re" +
            " %s ) entity";
    private static final String HIERARCHICAL_TO_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE.replace("$in", "to").replace("$out", "from");
    private static final String HIERARCHICAL_FROM_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE.replace("$in", "from").replace("$out", "to");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public DefaultEntityQueryRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityCountQuery query) {
        EntityType entityType = resolveEntityType(query.getEntityFilter());
        QueryContext ctx = new QueryContext(new QuerySecurityContext(tenantId, customerId, entityType, userPermissions));
        ctx.append("select count(e.id) from ");
        ctx.append(addEntityTableQuery(ctx, query.getEntityFilter()));
        ctx.append(" e");
        String entityWhereClause = buildEntityWhere(ctx, query.getEntityFilter(), Collections.emptyList());
        if (!entityWhereClause.isEmpty()) {
            ctx.append(" where ");
            ctx.append(entityWhereClause);
        }
        //TODO 3.1: remove this before release
        if (log.isTraceEnabled()) {
            log.trace("QUERY: {}", ctx.getQuery());
        }
        if (log.isTraceEnabled()) {
            Arrays.asList(ctx.getParameterNames()).forEach(param -> log.trace("QUERY PARAM: {}->{}", param, ctx.getValue(param)));
        }
        return transactionTemplate.execute(status -> jdbcTemplate.queryForObject(ctx.getQuery(), ctx, Long.class));
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityDataQuery query) {
        EntityType entityType = resolveEntityType(query.getEntityFilter());
        QueryContext ctx = new QueryContext(new QuerySecurityContext(tenantId, customerId, entityType, userPermissions));
        MergedGroupTypePermissionInfo readPermissions = ctx.getSecurityCtx().getMergedReadPermissionsByEntityType();
        if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            if (hasNoPermissionsForAllRelationQueryResources(ctx.getSecurityCtx().getMergedReadEntityPermissionsMap())) {
                return new PageData<>();
            }
        } else if (!readPermissions.isHasGenericRead() && readPermissions.getEntityGroupIds().isEmpty()) {
            return new PageData<>();
        }
        return transactionTemplate.execute(status -> {
            EntityDataPageLink pageLink = query.getPageLink();

            List<EntityKeyMapping> mappings = EntityKeyMapping.prepareKeyMapping(query);

            List<EntityKeyMapping> selectionMapping = mappings.stream().filter(EntityKeyMapping::isSelection)
                    .collect(Collectors.toList());
            List<EntityKeyMapping> entityFieldsSelectionMapping = selectionMapping.stream().filter(mapping -> !mapping.isLatest())
                    .collect(Collectors.toList());
            List<EntityKeyMapping> latestSelectionMapping = selectionMapping.stream().filter(EntityKeyMapping::isLatest)
                    .collect(Collectors.toList());

            List<EntityKeyMapping> filterMapping = mappings.stream().filter(EntityKeyMapping::hasFilter)
                    .collect(Collectors.toList());
            List<EntityKeyMapping> entityFieldsFiltersMapping = filterMapping.stream().filter(mapping -> !mapping.isLatest())
                    .collect(Collectors.toList());
            List<EntityKeyMapping> latestFiltersMapping = filterMapping.stream().filter(EntityKeyMapping::isLatest)
                    .collect(Collectors.toList());

            List<EntityKeyMapping> allLatestMappings = mappings.stream().filter(EntityKeyMapping::isLatest)
                    .collect(Collectors.toList());


            String entityWhereClause = DefaultEntityQueryRepository.this.buildEntityWhere(ctx, query.getEntityFilter(), entityFieldsFiltersMapping);
            String latestJoins = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), entityType, allLatestMappings);
            String whereClause = DefaultEntityQueryRepository.this.buildWhere(ctx, latestFiltersMapping, query.getEntityFilter().getType());
            String textSearchQuery = DefaultEntityQueryRepository.this.buildTextSearchQuery(ctx, selectionMapping, pageLink.getTextSearch());
            String entityFieldsSelection = EntityKeyMapping.buildSelections(entityFieldsSelectionMapping, query.getEntityFilter().getType(), entityType);
            String entityTypeStr;
            if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
                entityTypeStr = "e.entity_type";
            } else {
                entityTypeStr = "'" + entityType.name() + "'";
            }
            if (!StringUtils.isEmpty(entityFieldsSelection)) {
                entityFieldsSelection = String.format("e.id id, %s entity_type, %s", entityTypeStr, entityFieldsSelection);
            } else {
                entityFieldsSelection = String.format("e.id id, %s entity_type", entityTypeStr);
            }
            String latestSelection = EntityKeyMapping.buildSelections(latestSelectionMapping, query.getEntityFilter().getType(), entityType);
            String topSelection = "entities.*";
            if (!StringUtils.isEmpty(latestSelection)) {
                topSelection = topSelection + ", " + latestSelection;
            }

            StringBuilder entitiesQuery;
            if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
                entitiesQuery = buildRelationsEntitiesQuery(query, ctx, entityWhereClause, entityFieldsSelection);
            } else {
                entitiesQuery = buildCommonEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
            }

            String fromClause = String.format("from (select %s from (%s) entities %s %s) result %s",
                    topSelection,
                    entitiesQuery,
                    latestJoins,
                    whereClause,
                    textSearchQuery);

            int totalElements = jdbcTemplate.queryForObject(String.format("select count(*) %s", fromClause), ctx, Integer.class);

            String dataQuery = String.format("select * %s", fromClause);

            EntityDataSortOrder sortOrder = pageLink.getSortOrder();
            if (sortOrder != null) {
                Optional<EntityKeyMapping> sortOrderMappingOpt = mappings.stream().filter(EntityKeyMapping::isSortOrder).findFirst();
                if (sortOrderMappingOpt.isPresent()) {
                    EntityKeyMapping sortOrderMapping = sortOrderMappingOpt.get();
                    dataQuery = String.format("%s order by %s", dataQuery, sortOrderMapping.getValueAlias());
                    if (sortOrder.getDirection() == EntityDataSortOrder.Direction.ASC) {
                        dataQuery += " asc";
                    } else {
                        dataQuery += " desc";
                    }
                }
            }
            int startIndex = pageLink.getPageSize() * pageLink.getPage();
            if (pageLink.getPageSize() > 0) {
                dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
            }
            //TODO 3.1: remove this before release
            if (log.isTraceEnabled()) {
                log.trace("QUERY: {}", dataQuery);
            }
            if (log.isTraceEnabled()) {
                Arrays.asList(ctx.getParameterNames()).forEach(param -> log.trace("QUERY PARAM: {}->{}", param, ctx.getValue(param)));
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataQuery, ctx);
            return EntityDataAdapter.createEntityData(pageLink, selectionMapping, rows, totalElements);
        });
    }

    @Override
    public <T> PageData<T> findInCustomerHierarchyByRootCustomerIdOrOtherGroupIdsAndType(TenantId tenantId, CustomerId customerId, EntityType entityType, String type,
                                                                                         List<EntityGroupId> groupIds, PageLink pageLink, Function<Map<String, Object>, T> rowMapping) {
        return transactionTemplate.execute(status -> {
            QueryContext ctx = new QueryContext(new QuerySecurityContext(tenantId, customerId, entityType, null));
            StringBuilder fromClause = new StringBuilder();

            fromClause.append("FROM ");
            fromClause.append(entityTableMap.get(ctx.getEntityType()));
            fromClause.append(" as e");
            fromClause.append(" WHERE ");

            boolean customerIdSet = customerId != null && !customerId.isNullUid();
            boolean typeSet = type != null && !type.trim().isEmpty();
            boolean groupIdsSet = groupIds != null && !groupIds.isEmpty();
            ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
            fromClause.append(" e.tenant_id = :permissions_tenant_id ");

            if (customerIdSet) {
                ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
                fromClause.append(" AND ");
                if (groupIdsSet) {
                    fromClause.append("(");
                }
                fromClause.append(" e.customer_id in ").append(HIERARCHICAL_SUB_CUSTOMERS_QUERY);
                if (groupIdsSet) {
                    ctx.addUuidListParameter("group_ids", groupIds.stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                    fromClause.append(" OR e.id in ");
                    fromClause.append(" ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                    fromClause.append(entityType.name());
                    fromClause.append("' and rattr.from_id in (:").append("group_ids").append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                            "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains')");
                    fromClause.append(")");
                }
            } else if (groupIdsSet) {
                fromClause.append(" AND ");
                ctx.addUuidListParameter("group_ids", groupIds.stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                fromClause.append("e.id in ");
                fromClause.append("( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                fromClause.append(entityType.name());
                fromClause.append("' and rattr.from_id in (:").append("group_ids").append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                        "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains')");
                fromClause.append(")");
            }
            if (typeSet) {
                ctx.addStringParameter("type", type);
                fromClause.append(" e.type = ").append(type);
            }

            int totalElements = jdbcTemplate.queryForObject(String.format("select count(*) %s", fromClause), ctx, Integer.class);

            String dataQuery = String.format("select e.* %s ", fromClause);

            SortOrder sortOrder = pageLink.getSortOrder();
            if (sortOrder != null) {
                dataQuery = String.format("%s order by %s", dataQuery, sortOrder.getProperty());
                if (sortOrder.getDirection() == SortOrder.Direction.ASC) {
                    dataQuery += " asc";
                } else {
                    dataQuery += " desc";
                }
            }
            int startIndex = pageLink.getPageSize() * pageLink.getPage();
            if (pageLink.getPageSize() > 0) {
                dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
            }
            //TODO 3.1: remove this before release
            if (log.isTraceEnabled()) {
                log.trace("QUERY: {}", dataQuery);
            }
            if (log.isTraceEnabled()) {
                Arrays.asList(ctx.getParameterNames()).forEach(param -> log.trace("QUERY PARAM: {}->{}", param, ctx.getValue(param)));
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataQuery, ctx);

            int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
            boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + rows.size();
            List<T> entitiesData = rows.stream().map(rowMapping).collect(Collectors.toList());
            return new PageData<>(entitiesData, totalPages, totalElements, hasNext);
        });
    }

    private StringBuilder buildRelationsEntitiesQuery(EntityDataQuery query, QueryContext ctx, String entityWhereClause, String entityFieldsSelection) {
        StringBuilder entitiesQuery = new StringBuilder();

        Map<Resource, MergedGroupTypePermissionInfo> readPermMap = ctx.getSecurityCtx().getMergedReadEntityPermissionsMap();
        Map<Resource, MergedGroupTypePermissionInfo> readAttrPermMap = ctx.getSecurityCtx().getMergedReadAttrPermissionsMap();
        Map<Resource, MergedGroupTypePermissionInfo> readTsPermMap = ctx.getSecurityCtx().getMergedReadTsPermissionsMap();
        entitiesQuery.append("select ").append(entityFieldsSelection);

        buildPermissionsSelect(ctx, entitiesQuery, readAttrPermMap, "permissions_read_attr_group_ids", ATTR_READ_FLAG);
        buildPermissionsSelect(ctx, entitiesQuery, readTsPermMap, "permissions_read_ts_group_ids", TS_READ_FLAG);

        entitiesQuery.append(" FROM ")
                .append(addEntityTableQuery(ctx, query.getEntityFilter()))
                .append(" e WHERE ");

        ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
        ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());

        if (hasGenericForAllRelationQueryResources(readPermMap) && noGroupPermissionsForAllRelationQueryResources(readPermMap)) {
            entitiesQuery.append(" e.tenant_id =:permissions_tenant_id ");
            if (!ctx.isTenantUser()) {
                entitiesQuery.append(" AND e.customer_id =:permissions_customer_id ");
            }
            if (!entityWhereClause.isEmpty()) {
                entitiesQuery.append(" AND ").append(entityWhereClause);
            }
        } else {
            entitiesQuery.append(" e.tenant_id =:permissions_tenant_id AND ");
            entitiesQuery.append("(");
            // Entity is Tenant;
            addTenantEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.TENANT);
            entitiesQuery.append(" OR ");
            addTenantEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.INTEGRATION);
            entitiesQuery.append(" OR ");
            addTenantEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.CONVERTER);
            entitiesQuery.append(" OR ");
            addCustomerEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.ROLE);
            entitiesQuery.append(" OR ");
            addCustomerEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.BLOB_ENTITY);
            entitiesQuery.append(" OR ");
            addCustomerEntityCheck(ctx, entitiesQuery, readPermMap, EntityType.SCHEDULER_EVENT);
            // Entity is one of group entities;
            for (EntityType entityType : EntityGroup.groupTypes) {
                entitiesQuery.append(" OR (e.entity_type = '").append(entityType.name()).append("'");
                MergedGroupTypePermissionInfo permissions = readPermMap.get(Resource.resourceFromEntityType(entityType));
                if (ctx.isTenantUser()) {
                    if (permissions.isHasGenericRead()) {
                        //Do nothing here. Query is ok.
                    } else if (permissions.getEntityGroupIds().isEmpty()) {
                        entitiesQuery.append(" AND FALSE");
                    } else {
                        entitiesQuery.append(" AND EXISTS ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                        entitiesQuery.append(entityType.name());
                        String param = "permissions_read_group_ids_" + entityType.name().toLowerCase();
                        ctx.addUuidListParameter(param,
                                permissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                        entitiesQuery.append("' and rattr.from_id in (:").append(param).append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                                "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains')");
                    }
                } else {
                    String customerIdField = entityType == EntityType.CUSTOMER ? "id" : "customer_id";
                    if (permissions.isHasGenericRead()) {
                        if (permissions.getEntityGroupIds().isEmpty()) {
                            entitiesQuery.append(" AND e.").append(customerIdField).append(" in ").append(HIERARCHICAL_SUB_CUSTOMERS_QUERY);
                        } else {
                            entitiesQuery.append(" AND (e.").append(customerIdField).append(" in ").append(HIERARCHICAL_SUB_CUSTOMERS_QUERY);
                            entitiesQuery.append(" OR EXISTS ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                            entitiesQuery.append(entityType.name());
                            String param = "permissions_read_group_ids_" + entityType.name().toLowerCase();
                            ctx.addUuidListParameter(param,
                                    permissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                            entitiesQuery.append("' and rattr.from_id in (:").append(param).append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                                    "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains'))");
                        }
                    } else {
                        entitiesQuery.append(" AND EXISTS ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                        entitiesQuery.append(entityType.name());
                        String param = "permissions_read_group_ids_" + entityType.name().toLowerCase();
                        ctx.addUuidListParameter(param,
                                permissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                        entitiesQuery.append("' and rattr.from_id in (:").append(param).append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                                "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains'))");
                    }
                }
                entitiesQuery.append(")");
            }
            entitiesQuery.append(")");
            if (!entityWhereClause.isEmpty()) {
                entitiesQuery.append(" AND ").append(entityWhereClause);
            }
        }

        return entitiesQuery;
    }

    private void addTenantEntityCheck(QueryContext ctx, StringBuilder entitiesQuery, Map<Resource, MergedGroupTypePermissionInfo> readPermMap, EntityType entityType) {
        entitiesQuery.append("(e.entity_type = '").append(entityType.name()).append("' AND ");
        entitiesQuery.append(ctx.isTenantUser() && readPermMap.get(Resource.resourceFromEntityType(entityType)).isHasGenericRead() ? "true" : "false");
        entitiesQuery.append(")");
    }

    private void addCustomerEntityCheck(QueryContext ctx, StringBuilder entitiesQuery, Map<Resource, MergedGroupTypePermissionInfo> readPermMap, EntityType entityType) {
        if (ctx.isTenantUser()) {
            entitiesQuery.append("(e.entity_type = '").append(entityType.name()).append("' AND ");
            entitiesQuery.append(readPermMap.get(Resource.resourceFromEntityType(entityType)).isHasGenericRead() ? "true" : "false");
            entitiesQuery.append(")");
        } else {
            entitiesQuery.append("(e.entity_type = 'ROLE' AND ");
            if (readPermMap.get(Resource.resourceFromEntityType(entityType)).isHasGenericRead()) {
                entitiesQuery.append("e.customer_id in ").append(HIERARCHICAL_SUB_CUSTOMERS_QUERY);
            } else {
                entitiesQuery.append("FALSE");
            }
            entitiesQuery.append(")");
        }
    }

    private void buildPermissionsSelect(QueryContext ctx, StringBuilder entitiesQuery,
                                        Map<Resource, MergedGroupTypePermissionInfo> permissionsMap,
                                        String groupIdParamNamePrefix, String selectionName) {
        if (hasGenericForAllRelationQueryResources(permissionsMap)) {
            entitiesQuery.append(", true as ").append(selectionName);
        } else if (hasNoPermissionsForAllRelationQueryResources(permissionsMap)) {
            entitiesQuery.append(", false as ").append(selectionName);
        } else {
            entitiesQuery.append(", CASE");
            entitiesQuery.append(" WHEN e.entity_type = 'TENANT' THEN ").append(permissionsMap.get(Resource.resourceFromEntityType(EntityType.TENANT)).isHasGenericRead() ? "true" : "false");
            for (EntityType entityType : EntityGroup.groupTypes) {
                entitiesQuery.append(" WHEN e.entity_type = '").append(entityType.name()).append("' THEN ");
                MergedGroupTypePermissionInfo permissions = permissionsMap.get(Resource.resourceFromEntityType(entityType));
                if (permissions.isHasGenericRead()) {
                    entitiesQuery.append("true");
                } else if (permissions.getEntityGroupIds().isEmpty()) {
                    entitiesQuery.append("false");
                } else {
                    entitiesQuery.append("(CASE WHEN EXISTS ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                    entitiesQuery.append(entityType.name());
                    String param = groupIdParamNamePrefix + "_" + entityType.name().toLowerCase();
                    ctx.addUuidListParameter(param,
                            permissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                    entitiesQuery.append("' and rattr.from_id in (:").append(param).append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                            "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains') THEN true ELSE false END)");
                }
            }
            entitiesQuery.append("END as ").append(selectionName);
        }
    }

    private StringBuilder buildCommonEntitiesQuery(EntityDataQuery query, QueryContext ctx, MergedGroupTypePermissionInfo readPermissions, String entityWhereClause, String entityFieldsSelection) {
        StringBuilder entitiesQuery = new StringBuilder();

        //TODO: validate filter types that select groups and not entities.
        MergedGroupTypePermissionInfo readAttrPermissions = ctx.getSecurityCtx().getMergedReadAttrPermissionsByEntityType();
        MergedGroupTypePermissionInfo readTsPermissions = ctx.getSecurityCtx().getMergedReadTsPermissionsByEntityType();
        entitiesQuery.append("select ").append(entityFieldsSelection);

        if (!readAttrPermissions.isHasGenericRead()) {
            buildPermissionsSelect(ctx, entitiesQuery, readAttrPermissions.getEntityGroupIds(),
                    "permissions_read_attr_group_ids", ATTR_READ_FLAG);
        }
        if (!readTsPermissions.isHasGenericRead()) {
            buildPermissionsSelect(ctx, entitiesQuery, readTsPermissions.getEntityGroupIds(),
                    "permissions_read_ts_group_ids", TS_READ_FLAG);
        }

        if (readPermissions.isHasGenericRead()) {
            if (ctx.isTenantUser() || readPermissions.getEntityGroupIds().isEmpty()) {
                entitiesQuery.append(" from ")
                        .append(addEntityTableQuery(ctx, query.getEntityFilter()))
                        .append(" e where ")
                        .append(entityWhereClause);
            } else {
                entitiesQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter()));
                entitiesQuery.append(" e WHERE ");
                entitiesQuery.append(" e.id in (select re.to_id from relation re WHERE");
                entitiesQuery.append(" re.relation_type_group = 'FROM_ENTITY_GROUP' AND re.relation_type = 'Contains'");
                ctx.addUuidListParameter("permissions_read_group_ids",
                        readPermissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
                entitiesQuery.append(" AND ( re.from_id in (:permissions_read_group_ids) OR re.from_id in (");
                entitiesQuery.append(HIERARCHICAL_GROUPS_ALL_QUERY);
                if (!query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
                    entitiesQuery.append(" and type='").append(ctx.getEntityType()).append("'");
                }
                entitiesQuery.append("))");
                entitiesQuery.append(" AND re.from_type = 'ENTITY_GROUP'");
                entitiesQuery.append(" ) AND ").append(entityWhereClause);
            }
        } else {
            entitiesQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter()));
            entitiesQuery.append(" e WHERE ");
            entitiesQuery.append(" e.id in (select re.to_id from relation re WHERE");
            entitiesQuery.append(" re.relation_type_group = 'FROM_ENTITY_GROUP' AND re.relation_type = 'Contains'");
            ctx.addUuidListParameter("permissions_read_group_ids",
                    readPermissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
            entitiesQuery.append(" AND re.from_id in (:permissions_read_group_ids)");
            entitiesQuery.append(" AND re.from_type = 'ENTITY_GROUP'");
            entitiesQuery.append(" ) AND ").append(entityWhereClause);
        }
        return entitiesQuery;
    }

    private boolean hasGenericForAllRelationQueryResources(Map<Resource, MergedGroupTypePermissionInfo> permissionsMap) {
        if (!permissionsMap.get(Resource.resourceFromEntityType(EntityType.TENANT)).isHasGenericRead()) {
            return false;
        }
        for (EntityType entityType : EntityGroup.groupTypes) {
            if (!permissionsMap.get(Resource.resourceFromEntityType(entityType)).isHasGenericRead()) {
                return false;
            }
            if (!permissionsMap.get(Resource.groupResourceFromGroupType(entityType)).isHasGenericRead()) {
                return false;
            }
        }
        return true;
    }

    private boolean noGroupPermissionsForAllRelationQueryResources(Map<Resource, MergedGroupTypePermissionInfo> permissionsMap) {
        for (EntityType entityType : EntityGroup.groupTypes) {
            if (!permissionsMap.get(Resource.resourceFromEntityType(entityType)).getEntityGroupIds().isEmpty()) {
                return false;
            }
            if (!permissionsMap.get(Resource.groupResourceFromGroupType(entityType)).getEntityGroupIds().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasNoPermissionsForAllRelationQueryResources(Map<Resource, MergedGroupTypePermissionInfo> permissionsMap) {
        if (permissionsMap.get(Resource.resourceFromEntityType(EntityType.TENANT)).isHasGenericRead()) {
            return false;
        }
        for (EntityType entityType : EntityGroup.groupTypes) {
            if (permissionsMap.get(Resource.resourceFromEntityType(entityType)).isHasGenericRead() ||
                    !permissionsMap.get(Resource.resourceFromEntityType(entityType)).getEntityGroupIds().isEmpty()) {
                return false;
            }
            if (permissionsMap.get(Resource.groupResourceFromGroupType(entityType)).isHasGenericRead() ||
                    !permissionsMap.get(Resource.groupResourceFromGroupType(entityType)).getEntityGroupIds().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void buildPermissionsSelect(QueryContext ctx, StringBuilder entitiesQuery, List<EntityGroupId> groupIds, String groupIdParamName, String selectionName) {
        entitiesQuery.append(",");
        if (!groupIds.isEmpty()) {
            ctx.addUuidListParameter(groupIdParamName,
                    groupIds.stream().map(EntityGroupId::getId).collect(Collectors.toList()));
            entitiesQuery.append(idBelongsToGroupsSelection(groupIdParamName, selectionName));
        } else {
            entitiesQuery.append("false as ").append(selectionName);
        }
    }

    private String idBelongsToGroupsSelection(String groupIdParamName, String selectionName) {
        return String.format("CASE WHEN EXISTS ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.from_id in (:%s) AND rattr.from_type = 'ENTITY_GROUP' " +
                "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains') THEN true ELSE false END as %s", groupIdParamName, selectionName);
    }

    private String buildEntityWhere(QueryContext ctx, EntityFilter entityFilter, List<EntityKeyMapping> entityFieldsFilters) {
        String permissionQuery = this.buildPermissionQuery(ctx, entityFilter);
        String entityFilterQuery = this.buildEntityFilterQuery(ctx, entityFilter);
        String entityFieldsQuery = EntityKeyMapping.buildQuery(ctx, entityFieldsFilters, entityFilter.getType());
        String result = permissionQuery;
        if (!entityFilterQuery.isEmpty()) {
            if (!result.isEmpty()) {
                result += " and " + entityFilterQuery;
            } else {
                result = entityFilterQuery;
            }
        }
        if (!entityFieldsQuery.isEmpty()) {
            if (!result.isEmpty()) {
                result += " and " + entityFieldsQuery;
            } else {
                result = entityFieldsQuery;
            }
        }
        return result;
    }

    private String buildPermissionQuery(QueryContext ctx, EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case RELATIONS_QUERY:
                return "";
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
            case ENTITY_VIEW_SEARCH_QUERY:
            case ENTITY_GROUP:
            case ENTITY_GROUP_NAME:
                return this.defaultPermissionQuery(ctx);
            default:
                if (ctx.getEntityType() == EntityType.TENANT) {
                    ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
                    return "e.id=:permissions_tenant_id";
                } else {
                    return this.defaultPermissionQuery(ctx);
                }
        }
    }

    private String defaultPermissionQuery(QueryContext ctx) {
        ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
        QuerySecurityContext securityCtx = ctx.getSecurityCtx();
        if (!securityCtx.isTenantUser() && securityCtx.hasGeneric(Operation.READ) && securityCtx.getMergedReadPermissionsByEntityType().getEntityGroupIds().isEmpty()) {
            ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
            if (ctx.getEntityType() == EntityType.CUSTOMER) {
                return "e.tenant_id=:permissions_tenant_id and e.id in " + HIERARCHICAL_SUB_CUSTOMERS_QUERY;
            } else {
                return "e.tenant_id=:permissions_tenant_id and e.customer_id in " + HIERARCHICAL_SUB_CUSTOMERS_QUERY;
            }
        }
        return "e.tenant_id=:permissions_tenant_id";
    }

    private String buildEntityFilterQuery(QueryContext ctx, EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return this.singleEntityQuery(ctx, (SingleEntityFilter) entityFilter);
            case ENTITY_LIST:
                return this.entityListQuery(ctx, (EntityListFilter) entityFilter);
            case ENTITY_NAME:
                return this.entityNameQuery(ctx, (EntityNameFilter) entityFilter);
            case ENTITY_GROUP_LIST:
                return this.entityGroupListQuery(ctx, (EntityGroupListFilter) entityFilter);
            case ENTITY_GROUP_NAME:
                return this.entityGroupNameQuery(ctx, (EntityGroupNameFilter) entityFilter);
            case ASSET_TYPE:
            case DEVICE_TYPE:
            case ENTITY_VIEW_TYPE:
                return this.typeQuery(ctx, entityFilter);
            case ENTITY_GROUP:
            case ENTITIES_BY_GROUP_NAME:
            case STATE_ENTITY_OWNER:
            case RELATIONS_QUERY:
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
            case ENTITY_VIEW_SEARCH_QUERY:
                return "";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String addEntityTableQuery(QueryContext ctx, EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case ENTITY_GROUP:
                return entityGroupQuery(ctx, (EntityGroupFilter) entityFilter);
            case ENTITIES_BY_GROUP_NAME:
                return entityByGroupNameQuery(ctx, (EntitiesByGroupNameFilter) entityFilter);
            case STATE_ENTITY_OWNER:
                throw new RuntimeException("TODO: Not implemented!");
            case RELATIONS_QUERY:
                return relationQuery(ctx, (RelationsQueryFilter) entityFilter);
            case DEVICE_SEARCH_QUERY:
                DeviceSearchQueryFilter deviceQuery = (DeviceSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, deviceQuery, EntityType.DEVICE, deviceQuery.getDeviceTypes());
            case ASSET_SEARCH_QUERY:
                AssetSearchQueryFilter assetQuery = (AssetSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, assetQuery, EntityType.ASSET, assetQuery.getAssetTypes());
            case ENTITY_VIEW_SEARCH_QUERY:
                EntityViewSearchQueryFilter entityViewQuery = (EntityViewSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, entityViewQuery, EntityType.ENTITY_VIEW, entityViewQuery.getEntityViewTypes());
            default:
                return entityTableMap.get(ctx.getEntityType());
        }
    }

    private String entityGroupQuery(QueryContext ctx, EntityGroupFilter entityFilter) {
        EntityType entityType = entityFilter.getGroupType();
        EntityGroupId entityGroupId = new EntityGroupId(UUID.fromString(entityFilter.getEntityGroup()));
        String selectFields = "SELECT * FROM " + entityTableMap.get(entityType);
        String from = " WHERE id in (SELECT to_id from relation where from_id = :where_group_id and from_type = '" + EntityType.ENTITY_GROUP.name() + "'" +
                "and relation_type_group='" + RelationTypeGroup.FROM_ENTITY_GROUP + "' and relation_type='" + EntityRelation.CONTAINS_TYPE + "')";
        ctx.addUuidParameter("where_group_id", entityGroupId.getId());
        return "( " + selectFields + from + ")";
    }

    private String entityByGroupNameQuery(QueryContext ctx, EntitiesByGroupNameFilter entityFilter) {
        //TODO: 3.1 Need to check that user has access to this group before running query.
        throw new RuntimeException("Not implemented!");
    }

    private String entitySearchQuery(QueryContext ctx, EntitySearchQueryFilter entityFilter, EntityType entityType, List<String> types) {
        EntityId rootId = entityFilter.getRootEntity();
        //TODO: fetch last level only.
        //TODO: fetch distinct records.
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = "SELECT tenant_id, customer_id, id, created_time, type, name, label FROM " + entityType.name() + " WHERE id in ( SELECT entity_id";
        String from = getQueryTemplate(entityFilter.getDirection());
        String whereFilter = " WHERE re.relation_type = :where_relation_type AND re.to_type = :where_entity_type";

        from = String.format(from, lvlFilter, whereFilter);
        String query = "( " + selectFields + from + ")";
        if (types != null && !types.isEmpty()) {
            query += " and type in (:relation_sub_types)";
            ctx.addStringListParameter("relation_sub_types", types);
        }
        query += " )";
        ctx.addUuidParameter("relation_root_id", rootId.getId());
        ctx.addStringParameter("relation_root_type", rootId.getEntityType().name());
        ctx.addStringParameter("where_relation_type", entityFilter.getRelationType());
        ctx.addStringParameter("where_entity_type", entityType.name());
        return query;
    }

    private String relationQuery(QueryContext ctx, RelationsQueryFilter entityFilter) {
        EntityId rootId = entityFilter.getRootEntity();
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = SELECT_TENANT_ID + ", " + SELECT_CUSTOMER_ID
                + ", " + SELECT_CREATED_TIME + ", " +
                " entity.entity_id as id,"
                + SELECT_TYPE + ", " + SELECT_NAME + ", " + SELECT_LABEL + ", " +
                SELECT_FIRST_NAME + ", " + SELECT_LAST_NAME + ", " + SELECT_EMAIL + ", " + SELECT_REGION + ", " +
                SELECT_TITLE + ", " + SELECT_COUNTRY + ", " + SELECT_STATE + ", " + SELECT_CITY + ", " +
                SELECT_ADDRESS + ", " + SELECT_ADDRESS_2 + ", " + SELECT_ZIP + ", " + SELECT_PHONE +
                ", entity.entity_type as entity_type";
        String from = getQueryTemplate(entityFilter.getDirection());
        ctx.addUuidParameter("relation_root_id", rootId.getId());
        ctx.addStringParameter("relation_root_type", rootId.getEntityType().name());

        StringBuilder whereFilter;
        if (entityFilter.getFilters() != null && !entityFilter.getFilters().isEmpty()) {
            whereFilter = new StringBuilder(" WHERE ");
            boolean first = true;
            boolean single = entityFilter.getFilters().size() == 1;
            int entityTypeFilterIdx = 0;
            for (EntityTypeFilter etf : entityFilter.getFilters()) {
                if (first) {
                    first = false;
                } else {
                    whereFilter.append(" OR ");
                }
                String relationType = etf.getRelationType();
                if (!single) {
                    whereFilter.append(" (");
                }
                List<String> whereEntityTypes = etf.getEntityTypes().stream().map(EntityType::name).collect(Collectors.toList());
                whereFilter
                        .append(" re.relation_type = :where_relation_type").append(entityTypeFilterIdx);
                whereFilter.append(" and re.")
                        .append(entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from")
                        .append("_type in (:where_entity_types").append(entityTypeFilterIdx).append(")");
                if (!single) {
                    whereFilter.append(" )");
                }
                ctx.addStringParameter("where_relation_type" + entityTypeFilterIdx, relationType);
                if (!whereEntityTypes.isEmpty()) {
                    ctx.addStringListParameter("where_entity_types" + entityTypeFilterIdx, whereEntityTypes);
                } else {
                    ctx.addStringListParameter("where_entity_types" + entityTypeFilterIdx, Arrays.stream(RELATION_QUERY_ENTITY_TYPES).map(EntityType::name).collect(Collectors.toList()));
                }
                entityTypeFilterIdx++;
            }
        } else {
            whereFilter = new StringBuilder(" WHERE ");
            whereFilter.append("re.")
                    .append(entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from")
                    .append("_type in (:where_entity_types").append(")");
            ctx.addStringListParameter("where_entity_types", Arrays.stream(RELATION_QUERY_ENTITY_TYPES).map(EntityType::name).collect(Collectors.toList()));

        }
        from = String.format(from, lvlFilter, whereFilter);
        return "( " + selectFields + from + ")";
    }

    private String getLvlFilter(int maxLevel) {
        return maxLevel > 0 ? ("and lvl <= " + (maxLevel - 1)) : "";
    }

    private String getQueryTemplate(EntitySearchDirection direction) {
        String from;
        if (direction.equals(EntitySearchDirection.FROM)) {
            from = HIERARCHICAL_FROM_QUERY_TEMPLATE;
        } else {
            from = HIERARCHICAL_TO_QUERY_TEMPLATE;
        }
        return from;
    }

    private String buildWhere(QueryContext ctx, List<EntityKeyMapping> latestFiltersMapping, EntityFilterType filterType) {
        String latestFilters = EntityKeyMapping.buildQuery(ctx, latestFiltersMapping, filterType);
        if (!StringUtils.isEmpty(latestFilters)) {
            return String.format("where %s", latestFilters);
        } else {
            return "";
        }
    }

    private String buildTextSearchQuery(QueryContext ctx, List<EntityKeyMapping> selectionMapping, String searchText) {
        if (!StringUtils.isEmpty(searchText) && !selectionMapping.isEmpty()) {
            String lowerSearchText = searchText.toLowerCase() + "%";
            List<String> searchPredicates = selectionMapping.stream().map(mapping -> {
                        String paramName = mapping.getValueAlias() + "_lowerSearchText";
                        ctx.addStringParameter(paramName, lowerSearchText);
                        return String.format("LOWER(%s) LIKE concat('%%', :%s, '%%')", mapping.getValueAlias(), paramName);
                    }
            ).collect(Collectors.toList());
            return String.format(" WHERE %s", String.join(" or ", searchPredicates));
        } else {
            return "";
        }
    }

    private String singleEntityQuery(QueryContext ctx, SingleEntityFilter filter) {
        ctx.addUuidParameter("entity_filter_single_entity_id", filter.getSingleEntity().getId());
        return "e.id=:entity_filter_single_entity_id";
    }

    private String entityListQuery(QueryContext ctx, EntityListFilter filter) {
        ctx.addUuidListParameter("entity_filter_entity_ids", filter.getEntityList().stream().map(UUID::fromString).collect(Collectors.toList()));
        return "e.id in (:entity_filter_entity_ids)";
    }

    private String entityNameQuery(QueryContext ctx, EntityNameFilter filter) {
        if (!StringUtils.isEmpty(filter.getEntityNameFilter())) {
            ctx.addStringParameter("entity_filter_name_filter", filter.getEntityNameFilter());
            return "lower(e.search_text) like lower(concat(:entity_filter_name_filter, '%%'))";
        } else {
            return "";
        }
    }

    private String entityGroupListQuery(QueryContext ctx, EntityGroupListFilter filter) {
        ctx.addUuidListParameter("entity_filter_entity_group_ids", filter.getEntityGroupList().stream().map(UUID::fromString).collect(Collectors.toList()));
        return "e.id in (:entity_filter_entity_group_ids)";
    }

    private String entityGroupNameQuery(QueryContext ctx, EntityGroupNameFilter filter) {
        ctx.addStringParameter("entity_filter_group_name_filter", filter.getEntityGroupNameFilter());
        return "lower(e.name) like lower(concat(:entity_filter_group_name_filter, '%%'))";
    }

    private String typeQuery(QueryContext ctx, EntityFilter filter) {
        String type;
        String name;
        switch (filter.getType()) {
            case ASSET_TYPE:
                type = ((AssetTypeFilter) filter).getAssetType();
                name = ((AssetTypeFilter) filter).getAssetNameFilter();
                break;
            case DEVICE_TYPE:
                type = ((DeviceTypeFilter) filter).getDeviceType();
                name = ((DeviceTypeFilter) filter).getDeviceNameFilter();
                break;
            case ENTITY_VIEW_TYPE:
                type = ((EntityViewTypeFilter) filter).getEntityViewType();
                name = ((EntityViewTypeFilter) filter).getEntityViewNameFilter();
                break;
            default:
                throw new RuntimeException("Not supported!");
        }
        ctx.addStringParameter("entity_filter_type_query_type", type);
        ctx.addStringParameter("entity_filter_type_query_name", name);
        return "e.type = :entity_filter_type_query_type and lower(e.search_text) like lower(concat(:entity_filter_type_query_name, '%%'))";
    }

    private EntityType resolveEntityType(EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return ((SingleEntityFilter) entityFilter).getSingleEntity().getEntityType();
            case ENTITY_GROUP:
                return ((EntityGroupFilter) entityFilter).getGroupType();
            case ENTITY_LIST:
                return ((EntityListFilter) entityFilter).getEntityType();
            case ENTITY_NAME:
                return ((EntityNameFilter) entityFilter).getEntityType();
            case ENTITY_GROUP_LIST:
            case ENTITY_GROUP_NAME:
                return EntityType.ENTITY_GROUP;
            case ENTITIES_BY_GROUP_NAME:
                return ((EntitiesByGroupNameFilter) entityFilter).getGroupType();
            case STATE_ENTITY_OWNER:
                throw new RuntimeException("TODO: Not implemented!");
            case ASSET_TYPE:
            case ASSET_SEARCH_QUERY:
                return EntityType.ASSET;
            case DEVICE_TYPE:
            case DEVICE_SEARCH_QUERY:
                return EntityType.DEVICE;
            case ENTITY_VIEW_TYPE:
            case ENTITY_VIEW_SEARCH_QUERY:
                return EntityType.ENTITY_VIEW;
            case RELATIONS_QUERY:
                return ((RelationsQueryFilter) entityFilter).getRootEntity().getEntityType();
            default:
                throw new RuntimeException("Not implemented!");
        }
    }
}
