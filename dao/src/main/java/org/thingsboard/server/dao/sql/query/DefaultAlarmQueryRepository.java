/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DefaultAlarmQueryRepository implements AlarmQueryRepository {

    private static final Map<String, String> alarmFieldColumnMap = new HashMap<>();

    static {
        alarmFieldColumnMap.put("createdTime", ModelConstants.CREATED_TIME_PROPERTY);
        alarmFieldColumnMap.put("ackTs", ModelConstants.ALARM_ACK_TS_PROPERTY);
        alarmFieldColumnMap.put("ackTime", ModelConstants.ALARM_ACK_TS_PROPERTY);
        alarmFieldColumnMap.put("clearTs", ModelConstants.ALARM_CLEAR_TS_PROPERTY);
        alarmFieldColumnMap.put("clearTime", ModelConstants.ALARM_CLEAR_TS_PROPERTY);
        alarmFieldColumnMap.put("details", ModelConstants.ADDITIONAL_INFO_PROPERTY);
        alarmFieldColumnMap.put("endTs", ModelConstants.ALARM_END_TS_PROPERTY);
        alarmFieldColumnMap.put("endTime", ModelConstants.ALARM_END_TS_PROPERTY);
        alarmFieldColumnMap.put("startTs", ModelConstants.ALARM_START_TS_PROPERTY);
        alarmFieldColumnMap.put("startTime", ModelConstants.ALARM_START_TS_PROPERTY);
        alarmFieldColumnMap.put("status", ModelConstants.ALARM_STATUS_PROPERTY);
        alarmFieldColumnMap.put("type", ModelConstants.ALARM_TYPE_PROPERTY);
        alarmFieldColumnMap.put("severity", ModelConstants.ALARM_SEVERITY_PROPERTY);
        alarmFieldColumnMap.put("originatorId", ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY);
        alarmFieldColumnMap.put("originatorType", ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY);
        alarmFieldColumnMap.put("originator", "originator_name");
    }

    private static final String SELECT_ORIGINATOR_NAME = " COALESCE(CASE" +
            " WHEN a.originator_type = " + EntityType.TENANT.ordinal() +
            " THEN (select title from tenant where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.CUSTOMER.ordinal() +
            " THEN (select title from customer where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.USER.ordinal() +
            " THEN (select email from tb_user where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.DASHBOARD.ordinal() +
            " THEN (select title from dashboard where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.ASSET.ordinal() +
            " THEN (select name from asset where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.DEVICE.ordinal() +
            " THEN (select name from device where id = a.originator_id)" +
            " WHEN a.originator_type = " + EntityType.ENTITY_VIEW.ordinal() +
            " THEN (select name from entity_view where id = a.originator_id)" +
            " END, 'Deleted') as originator_name";

    private static final String FIELDS_SELECTION = "select a.id as id," +
            " a.created_time as created_time," +
            " a.ack_ts as ack_ts," +
            " a.clear_ts as clear_ts," +
            " a.additional_info as additional_info," +
            " a.end_ts as end_ts," +
            " a.originator_id as originator_id," +
            " a.originator_type as originator_type," +
            " a.propagate as propagate," +
            " a.severity as severity," +
            " a.start_ts as start_ts," +
            " a.status as status, " +
            " a.tenant_id as tenant_id, " +
            " a.propagate_relation_types as propagate_relation_types, " +
            " a.type as type," + SELECT_ORIGINATOR_NAME + ", ";

    private static final String JOIN_RELATIONS = "left join relation r on r.relation_type_group = 'ALARM' and r.relation_type = 'ANY' and a.id = r.to_id and r.from_id in (:entity_ids)";

    protected final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    private final DefaultQueryLogComponent queryLog;

    public DefaultAlarmQueryRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate, DefaultQueryLogComponent queryLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.queryLog = queryLog;
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, CustomerId customerId, MergedUserPermissions mergedUserPermissions,
                                                               AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        return transactionTemplate.execute(status -> {
            AlarmDataPageLink pageLink = query.getPageLink();
            QueryContext ctx = new QueryContext(new QuerySecurityContext(tenantId, customerId, EntityType.ALARM, mergedUserPermissions, null));
            ctx.addUuidListParameter("entity_ids", orderedEntityIds.stream().map(EntityId::getId).collect(Collectors.toList()));
            StringBuilder selectPart = new StringBuilder(FIELDS_SELECTION);
            StringBuilder fromPart = new StringBuilder(" from alarm a ");
            StringBuilder wherePart = new StringBuilder(" where ");
            StringBuilder sortPart = new StringBuilder(" order by ");
            boolean addAnd = false;
            if (pageLink.isSearchPropagatedAlarms()) {
                selectPart.append(" CASE WHEN r.from_id IS NULL THEN a.originator_id ELSE r.from_id END as entity_id ");
                fromPart.append(JOIN_RELATIONS);
                wherePart.append(buildPermissionsQuery(tenantId, customerId, ctx, mergedUserPermissions));
                addAnd = true;
            } else {
                selectPart.append(" a.originator_id as entity_id ");
            }
            EntityDataSortOrder sortOrder = pageLink.getSortOrder();
            if (sortOrder != null && sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
                String sortOrderKey = sortOrder.getKey().getKey();
                sortPart.append(alarmFieldColumnMap.getOrDefault(sortOrderKey, sortOrderKey))
                        .append(" ").append(sortOrder.getDirection().name());
                if (pageLink.isSearchPropagatedAlarms()) {
                    wherePart.append(" and (a.originator_id in (:entity_ids) or r.from_id IS NOT NULL)");
                } else {
                    addAndIfNeeded(wherePart, addAnd);
                    addAnd = true;
                    wherePart.append(" a.originator_id in (:entity_ids)");
                }
            } else {
                fromPart.append(" inner join (select * from (VALUES");
                int entityIdIdx = 0;
                int lastEntityIdIdx = orderedEntityIds.size() - 1;
                for (EntityId entityId : orderedEntityIds) {
                    fromPart.append("(uuid('").append(entityId.getId().toString()).append("'), ").append(entityIdIdx).append(")");
                    if (entityIdIdx != lastEntityIdIdx) {
                        fromPart.append(",");
                    } else {
                        fromPart.append(")");
                    }
                    entityIdIdx++;
                }
                fromPart.append(" as e(id, priority)) e ");
                if (pageLink.isSearchPropagatedAlarms()) {
                    fromPart.append("on (r.from_id IS NULL and a.originator_id = e.id) or (r.from_id IS NOT NULL and r.from_id = e.id)");
                } else {
                    fromPart.append("on a.originator_id = e.id");
                }
                sortPart.append("e.priority");
            }

            long startTs;
            long endTs;
            if (pageLink.getTimeWindow() > 0) {
                endTs = System.currentTimeMillis();
                startTs = endTs - pageLink.getTimeWindow();
            } else {
                startTs = pageLink.getStartTs();
                endTs = pageLink.getEndTs();
            }

            if (startTs > 0) {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                ctx.addLongParameter("startTime", startTs);
                wherePart.append("a.created_time >= :startTime");
            }

            if (endTs > 0) {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                ctx.addLongParameter("endTime", endTs);
                wherePart.append("a.created_time <= :endTime");
            }

            if (pageLink.getTypeList() != null && !pageLink.getTypeList().isEmpty()) {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                ctx.addStringListParameter("alarmTypes", pageLink.getTypeList());
                wherePart.append("a.type in (:alarmTypes)");
            }

            if (pageLink.getSeverityList() != null && !pageLink.getSeverityList().isEmpty()) {
                addAndIfNeeded(wherePart, addAnd);
                addAnd = true;
                ctx.addStringListParameter("alarmSeverities", pageLink.getSeverityList().stream().map(AlarmSeverity::name).collect(Collectors.toList()));
                wherePart.append("a.severity in (:alarmSeverities)");
            }

            if (pageLink.getStatusList() != null && !pageLink.getStatusList().isEmpty()) {
                Set<AlarmStatus> statusSet = toStatusSet(pageLink.getStatusList());
                if (!statusSet.isEmpty()) {
                    addAndIfNeeded(wherePart, addAnd);
                    ctx.addStringListParameter("alarmStatuses", statusSet.stream().map(AlarmStatus::name).collect(Collectors.toList()));
                    wherePart.append(" a.status in (:alarmStatuses)");
                }
            }

            String textSearchQuery = buildTextSearchQuery(ctx, query.getAlarmFields(), pageLink.getTextSearch());
            String mainQuery = selectPart.toString() + fromPart.toString() + wherePart.toString();
            if (!textSearchQuery.isEmpty()) {
                mainQuery = String.format("select * from (%s) a WHERE %s", mainQuery, textSearchQuery);
            }
            String countQuery = String.format("select count(*) from (%s) result", mainQuery);
            long queryTs = System.currentTimeMillis();
            int totalElements;
            try {
                totalElements = jdbcTemplate.queryForObject(countQuery, ctx, Integer.class);
            } finally {
                queryLog.logQuery(ctx, countQuery, System.currentTimeMillis() - queryTs);
            }
            if (totalElements == 0) {
                return AlarmDataAdapter.createAlarmData(pageLink, Collections.emptyList(), totalElements, orderedEntityIds);
            }

            String dataQuery = mainQuery + sortPart;

            int startIndex = pageLink.getPageSize() * pageLink.getPage();
            if (pageLink.getPageSize() > 0) {
                dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
            }
            queryTs = System.currentTimeMillis();
            List<Map<String, Object>> rows;
            try {
                rows = jdbcTemplate.queryForList(dataQuery, ctx);
            } finally {
                queryLog.logQuery(ctx, dataQuery, System.currentTimeMillis() - queryTs);
            }
            return AlarmDataAdapter.createAlarmData(pageLink, rows, totalElements, orderedEntityIds);
        });
    }

    private String buildTextSearchQuery(QueryContext ctx, List<EntityKey> selectionMapping, String searchText) {
        if (!StringUtils.isEmpty(searchText) && selectionMapping != null && !selectionMapping.isEmpty()) {
            String lowerSearchText = searchText.toLowerCase() + "%";
            List<String> searchPredicates = selectionMapping.stream()
                    .map(mapping -> alarmFieldColumnMap.get(mapping.getKey()))
                    .filter(Objects::nonNull)
                    .map(mapping -> {
                                String paramName = mapping + "_lowerSearchText";
                                ctx.addStringParameter(paramName, lowerSearchText);
                                return String.format("LOWER(cast(%s as varchar)) LIKE concat('%%', :%s, '%%')", mapping, paramName);
                            }
                    ).collect(Collectors.toList());
            return String.format("%s", String.join(" or ", searchPredicates));
        } else {
            return "";
        }
    }

    private String buildPermissionsQuery(TenantId tenantId, CustomerId customerId, QueryContext ctx, MergedUserPermissions mergedUserPermissions) {
        StringBuilder permissionsQuery = new StringBuilder();
        ctx.addUuidParameter("permissions_tenant_id", tenantId.getId());
        permissionsQuery.append(" a.tenant_id = :permissions_tenant_id ");
//        if (customerId != null && !customerId.isNullUid()) {
//            ctx.addUuidParameter("permissions_customer_id", customerId.getId());
//            permissionsQuery.append(" and (");
//            permissionsQuery.append("(a.originator_type = '").append(EntityType.DEVICE.ordinal())
//                    .append("' and exists (select 1 from device cd where cd.id = a.originator_id and ")
//                    .append("(cd.customer_id in ").append(DefaultEntityQueryRepository.HIERARCHICAL_SUB_CUSTOMERS_QUERY);
//            addGroupPermissionsIfAny(EntityType.DEVICE, "cd", ctx, mergedUserPermissions, permissionsQuery);
//            permissionsQuery.append(")))");
//            permissionsQuery.append(" or ");
//            permissionsQuery.append("(a.originator_type = '").append(EntityType.ASSET.ordinal())
//                    .append("' and exists (select 1 from asset ca where ca.id = a.originator_id and (ca.customer_id in ")
//                    .append(DefaultEntityQueryRepository.HIERARCHICAL_SUB_CUSTOMERS_QUERY);
//            addGroupPermissionsIfAny(EntityType.ASSET, "ca", ctx, mergedUserPermissions, permissionsQuery);
//            permissionsQuery.append(")))");
//            permissionsQuery.append(" or ");
//            permissionsQuery.append("(a.originator_type = '").append(EntityType.CUSTOMER.ordinal())
//                    .append("' and exists (select 1 from customer cc where cc.id = a.originator_id and (cc.id in ")
//                    .append(DefaultEntityQueryRepository.HIERARCHICAL_SUB_CUSTOMERS_QUERY);
//            addGroupPermissionsIfAny(EntityType.CUSTOMER, "cc", ctx, mergedUserPermissions, permissionsQuery);
//            permissionsQuery.append(")))");
//            permissionsQuery.append(" or ");
//            permissionsQuery.append("(a.originator_type = '").append(EntityType.USER.ordinal())
//                    .append("' and exists (select 1 from tb_user cu where cu.id = a.originator_id and (cu.customer_id in ")
//                    .append(DefaultEntityQueryRepository.HIERARCHICAL_SUB_CUSTOMERS_QUERY);
//            addGroupPermissionsIfAny(EntityType.USER, "cu", ctx, mergedUserPermissions, permissionsQuery);
//            permissionsQuery.append(")))");
//            permissionsQuery.append(" or ");
//            permissionsQuery.append("(a.originator_type = '").append(EntityType.ENTITY_VIEW.ordinal())
//                    .append("' and exists (select 1 from entity_view cv where cv.id = a.originator_id and (cv.customer_id in")
//                    .append(DefaultEntityQueryRepository.HIERARCHICAL_SUB_CUSTOMERS_QUERY);
//            addGroupPermissionsIfAny(EntityType.ENTITY_VIEW, "cv", ctx, mergedUserPermissions, permissionsQuery);
//            permissionsQuery.append(")))");
//            permissionsQuery.append(")");
//        } else if (!mergedUserPermissions.hasGenericPermission(Resource.ALL, Operation.READ)) {
//            permissionsQuery.append(" and (");
//            boolean atLeastOne = false;
//            if (addTenantPermissionsCheck(EntityType.DEVICE, ModelConstants.DEVICE_FAMILY_NAME, "td", ctx, mergedUserPermissions, permissionsQuery, atLeastOne)) {
//                atLeastOne = true;
//            }
//            if (addTenantPermissionsCheck(EntityType.ASSET, ModelConstants.ASSET_COLUMN_FAMILY_NAME, "ta", ctx, mergedUserPermissions, permissionsQuery, atLeastOne)) {
//                atLeastOne = true;
//            }
//            if (addTenantPermissionsCheck(EntityType.CUSTOMER, ModelConstants.CUSTOMER_COLUMN_FAMILY_NAME, "tc", ctx, mergedUserPermissions, permissionsQuery, atLeastOne)) {
//                atLeastOne = true;
//            }
//            if (addTenantPermissionsCheck(EntityType.USER, ModelConstants.USER_PG_HIBERNATE_COLUMN_FAMILY_NAME, "tu", ctx, mergedUserPermissions, permissionsQuery, atLeastOne)) {
//                atLeastOne = true;
//            }
//            if (addTenantPermissionsCheck(EntityType.ENTITY_VIEW, ModelConstants.ENTITY_VIEW_TABLE_FAMILY_NAME, "tev", ctx, mergedUserPermissions, permissionsQuery, atLeastOne)) {
//                atLeastOne = true;
//            }
//            if (!atLeastOne) {
//                permissionsQuery.append(" false");
//            }
//            permissionsQuery.append(")");
//        }
        return permissionsQuery.toString();
    }

    private boolean addTenantPermissionsCheck(EntityType entityType, String tableName, String alias, QueryContext ctx,
                                              MergedUserPermissions mergedUserPermissions, StringBuilder permissionsQuery,
                                              boolean addOr) {
        if (mergedUserPermissions.hasGenericPermission(Resource.resourceFromEntityType(entityType), Operation.READ)) {
            if (addOr) {
                permissionsQuery.append(" or ");
            }
            permissionsQuery.append(" a.originator_type = '").append(entityType.ordinal()).append("'");
            return true;
        } else {
            return addTenantGroupPermissionsCheck(entityType, tableName, alias, ctx, mergedUserPermissions, permissionsQuery, addOr);
        }
    }

    private boolean addTenantGroupPermissionsCheck(EntityType entityType, String tableName, String alias, QueryContext ctx, MergedUserPermissions mergedUserPermissions, StringBuilder permissionsQuery, boolean addOr) {
        MergedGroupTypePermissionInfo entityGroupPermissions = mergedUserPermissions.getGroupPermissionsByEntityTypeAndOperation(entityType, Operation.READ);
        if (entityGroupPermissions.getEntityGroupIds() != null && !entityGroupPermissions.getEntityGroupIds().isEmpty()) {
            if (addOr) {
                permissionsQuery.append(" or ");
            }
            permissionsQuery.append("exists (select 1 from ").append(tableName).append(" ").append(alias).append(" where ").append(alias).append(".id = a.originator_id and ");
            String queryParamName = entityType.name().toLowerCase() + "GroupPermissions";
            permissionsQuery.append(alias).append(".id in (select to_id from relation where from_type = 'ENTITY_GROUP' ")
                    .append("and to_type = '").append(entityType.name()).append("' and relation_type_group = 'FROM_ENTITY_GROUP' and from_id in (:").append(queryParamName).append("))");
            ctx.addUuidListParameter(queryParamName, entityGroupPermissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
            permissionsQuery.append(")");
            return true;
        } else {
            return false;
        }
    }

    private void addGroupPermissionsIfAny(EntityType entityType, String alias, QueryContext ctx, MergedUserPermissions mergedUserPermissions, StringBuilder permissionsQuery) {
        MergedGroupTypePermissionInfo entityGroupPermissions = mergedUserPermissions.getGroupPermissionsByEntityTypeAndOperation(entityType, Operation.READ);
        if (entityGroupPermissions.getEntityGroupIds() != null && !entityGroupPermissions.getEntityGroupIds().isEmpty()) {
            String queryParamName = entityType.name().toLowerCase() + "GroupPermissions";
            permissionsQuery.append(" OR ");
            permissionsQuery.append(alias).append(".id in (select to_id from relation where from_type = 'ENTITY_GROUP' ")
                    .append("and to_type = '").append(entityType.name()).append("' and relation_type_group = 'FROM_ENTITY_GROUP' and from_id in (:").append(queryParamName).append("))");
            ctx.addUuidListParameter(queryParamName, entityGroupPermissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
        }
    }

    private Set<AlarmStatus> toStatusSet(List<AlarmSearchStatus> statusList) {
        Set<AlarmStatus> result = new HashSet<>();
        for (AlarmSearchStatus searchStatus : statusList) {
            switch (searchStatus) {
                case ACK:
                    result.add(AlarmStatus.ACTIVE_ACK);
                    result.add(AlarmStatus.CLEARED_ACK);
                    break;
                case UNACK:
                    result.add(AlarmStatus.ACTIVE_UNACK);
                    result.add(AlarmStatus.CLEARED_UNACK);
                    break;
                case CLEARED:
                    result.add(AlarmStatus.CLEARED_ACK);
                    result.add(AlarmStatus.CLEARED_UNACK);
                    break;
                case ACTIVE:
                    result.add(AlarmStatus.ACTIVE_ACK);
                    result.add(AlarmStatus.ACTIVE_UNACK);
                    break;
                default:
                    break;
            }
            if (searchStatus == AlarmSearchStatus.ANY || result.size() == AlarmStatus.values().length) {
                result.clear();
                return result;
            }
        }
        return result;
    }

    private void addAndIfNeeded(StringBuilder wherePart, boolean addAnd) {
        if (addAnd) {
            wherePart.append(" and ");
        }
    }
}
