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
package org.thingsboard.server.dao.sql.query;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.common.data.query.AssetSearchQueryFilter;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.DeviceSearchQueryFilter;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EdgeSearchQueryFilter;
import org.thingsboard.server.common.data.query.EdgeTypeFilter;
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
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntitySearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.EntityViewSearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SchedulerEventFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.AssetEntity;
import org.thingsboard.server.dao.model.sql.BlobEntityEntity;
import org.thingsboard.server.dao.model.sql.CustomerEntity;
import org.thingsboard.server.dao.model.sql.DashboardEntity;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.EdgeEntity;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.model.sql.SchedulerEventEntity;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.sql.alarm.AlarmRepository;
import org.thingsboard.server.dao.sql.asset.AssetRepository;
import org.thingsboard.server.dao.sql.blob.BlobEntityRepository;
import org.thingsboard.server.dao.sql.customer.CustomerRepository;
import org.thingsboard.server.dao.sql.dashboard.DashboardRepository;
import org.thingsboard.server.dao.sql.device.DeviceRepository;
import org.thingsboard.server.dao.sql.edge.EdgeRepository;
import org.thingsboard.server.dao.sql.entityview.EntityViewRepository;
import org.thingsboard.server.dao.sql.group.EntityGroupRepository;
import org.thingsboard.server.dao.sql.role.RoleRepository;
import org.thingsboard.server.dao.sql.scheduler.SchedulerEventRepository;
import org.thingsboard.server.dao.sql.user.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
            " THEN (select customer_id from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select customer_id from edge where id = entity_id)" +
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
            " THEN (select tenant_id from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select tenant_id from edge where id = entity_id)" +
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
            " THEN (select created_time from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select created_time from edge where id = entity_id)" +
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
            " THEN (select name from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select name from edge where id = entity_id)" +
            " END as name";
    private static final String SELECT_TYPE = " CASE" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select authority from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select type from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select type from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select type from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select type from edge where id = entity_id)" +
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
            " THEN (select name from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select label from edge where id = entity_id)" +
            " END as label";
    private static final String SELECT_ADDITIONAL_INFO = " CASE" +
            " WHEN entity.entity_type = 'TENANT'" +
            " THEN (select additional_info from tenant where id = entity_id)" +
            " WHEN entity.entity_type = 'CUSTOMER' " +
            " THEN (select additional_info from customer where id = entity_id)" +
            " WHEN entity.entity_type = 'USER'" +
            " THEN (select additional_info from tb_user where id = entity_id)" +
            " WHEN entity.entity_type = 'DASHBOARD'" +
            " THEN (select '' from dashboard where id = entity_id)" +
            " WHEN entity.entity_type = 'ASSET'" +
            " THEN (select additional_info from asset where id = entity_id)" +
            " WHEN entity.entity_type = 'DEVICE'" +
            " THEN (select additional_info from device where id = entity_id)" +
            " WHEN entity.entity_type = 'ENTITY_VIEW'" +
            " THEN (select additional_info from entity_view where id = entity.entity_id)" +
            " WHEN entity.entity_type = 'EDGE'" +
            " THEN (select additional_info from edge where id = entity_id)" +
            " END as additional_info";

    public static final String ATTR_READ_FLAG = "attr_read";
    public static final String TS_READ_FLAG = "ts_read";

    private static final String SELECT_RELATED_PARENT_ID = "entity.parent_id AS parent_id";

    private static final String SELECT_API_USAGE_STATE = "(select aus.id, aus.created_time, aus.tenant_id, aus.entity_id, " +
            "coalesce((select title from tenant where id = aus.entity_id), (select title from customer where id = aus.entity_id)) as name " +
            "from api_usage_state as aus)";

    static {
        entityTableMap.put(EntityType.ENTITY_GROUP, "entity_group");
        entityTableMap.put(EntityType.ASSET, "asset");
        entityTableMap.put(EntityType.DEVICE, "device");
        entityTableMap.put(EntityType.ENTITY_VIEW, "entity_view");
        entityTableMap.put(EntityType.DASHBOARD, "dashboard");
        entityTableMap.put(EntityType.CUSTOMER, "customer");
        entityTableMap.put(EntityType.USER, "tb_user");
        entityTableMap.put(EntityType.TENANT, "tenant");
        entityTableMap.put(EntityType.CONVERTER, "converter");
        entityTableMap.put(EntityType.INTEGRATION, "integration");
        entityTableMap.put(EntityType.SCHEDULER_EVENT, "scheduler_event");
        entityTableMap.put(EntityType.BLOB_ENTITY, "blob_entity");
        entityTableMap.put(EntityType.ROLE, "role");
        entityTableMap.put(EntityType.API_USAGE_STATE, SELECT_API_USAGE_STATE);
        entityTableMap.put(EntityType.EDGE, "edge");
        entityTableMap.put(EntityType.RULE_CHAIN, "rule_chain");
        entityTableMap.put(EntityType.DEVICE_PROFILE, "device_profile");
        entityTableMap.put(EntityType.ASSET_PROFILE, "asset_profile");
        entityTableMap.put(EntityType.TENANT_PROFILE, "tenant_profile");
    }

    public static EntityType[] RELATION_QUERY_ENTITY_TYPES = new EntityType[]{
            EntityType.TENANT, EntityType.CUSTOMER, EntityType.USER, EntityType.DASHBOARD, EntityType.ASSET, EntityType.DEVICE,
            EntityType.CONVERTER, EntityType.INTEGRATION, EntityType.ENTITY_VIEW, EntityType.EDGE, EntityType.ROLE, EntityType.SCHEDULER_EVENT, EntityType.BLOB_ENTITY};

    private static final String HIERARCHICAL_GROUPS_QUERY = "select id from entity_group where owner_id in (" +
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
            " FROM customers_ids))";

    private static final String HIERARCHICAL_GROUPS_ALL_QUERY = HIERARCHICAL_GROUPS_QUERY + " and name = 'All'";

    public static final String HIERARCHICAL_SUB_CUSTOMERS_QUERY = "(WITH RECURSIVE customers_ids(id) AS" +
            " (SELECT id id FROM customer WHERE tenant_id = :permissions_tenant_id and id = :permissions_customer_id" +
            " UNION SELECT c.id id FROM customer c, customers_ids parent WHERE c.tenant_id = :permissions_tenant_id" +
            " and c.parent_customer_id = parent.id) SELECT id FROM customers_ids)";

    private static final String HIERARCHICAL_QUERY_TEMPLATE = " FROM (WITH RECURSIVE related_entities(from_id, from_type, to_id, to_type, lvl, path) AS (" +
            " SELECT from_id, from_type, to_id, to_type," +
            "        1 as lvl," +
            "        ARRAY[$in_id] as path" + // initial path
            " FROM relation " +
            " WHERE $in_id $rootIdCondition and $in_type = :relation_root_type and relation_type_group = 'COMMON'" +
            " GROUP BY from_id, from_type, to_id, to_type, lvl, path" +
            " UNION ALL" +
            " SELECT r.from_id, r.from_type, r.to_id, r.to_type," +
            "        (re.lvl + 1) as lvl, " +
            "        (re.path || ARRAY[r.$in_id]) as path" +
            " FROM relation r" +
            " INNER JOIN related_entities re ON" +
            " r.$in_id = re.$out_id and r.$in_type = re.$out_type and" +
            " relation_type_group = 'COMMON' " +
            " AND r.$in_id NOT IN (SELECT * FROM unnest(re.path)) " +
            " %s" +
            " GROUP BY r.from_id, r.from_type, r.to_id, r.to_type, (re.lvl + 1), (re.path || ARRAY[r.$in_id])" +
            " )" +
            " SELECT re.$out_id entity_id, re.$out_type entity_type, $parenIdExp max(r_int.lvl) lvl" +
            " from related_entities r_int" +
            "  INNER JOIN relation re ON re.from_id = r_int.from_id AND re.from_type = r_int.from_type" +
            "                         AND re.to_id = r_int.to_id AND re.to_type = r_int.to_type" +
            "                         AND re.relation_type_group = 'COMMON'" +
            " %s GROUP BY entity_id, entity_type $parenIdSelection) entity";

    private static final String HIERARCHICAL_TO_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "")
            .replace("$parenIdSelection", "")
            .replace("$in", "to").replace("$out", "from")
            .replace("$rootIdCondition", "= :relation_root_id");
    private static final String HIERARCHICAL_TO_MR_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "re.$in_id parent_id, ")
            .replace("$parenIdSelection", ", parent_id")
            .replace("$in", "to").replace("$out", "from")
            .replace("$rootIdCondition", "in (:relation_root_ids)");

    private static final String HIERARCHICAL_FROM_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "")
            .replace("$parenIdSelection", "")
            .replace("$in", "from").replace("$out", "to")
            .replace("$rootIdCondition", "= :relation_root_id");
    private static final String HIERARCHICAL_FROM_MR_QUERY_TEMPLATE = HIERARCHICAL_QUERY_TEMPLATE
            .replace("$parenIdExp", "re.$in_id parent_id, ")
            .replace("$parenIdSelection", ", parent_id")
            .replace("$in", "from").replace("$out", "to")
            .replace("$rootIdCondition", "in (:relation_root_ids)");

    @Getter
    @Value("${sql.relations.max_level:50}")
    int maxLevelAllowed; //This value has to be reasonable small to prevent infinite recursion as early as possible

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final AssetRepository assetRepository;
    private final CustomerRepository customerRepository;
    private final DeviceRepository deviceRepository;
    private final EntityViewRepository entityViewRepository;
    private final EdgeRepository edgeRepository;
    private final UserRepository userRepository;
    private final DashboardRepository dashboardRepository;
    private final EntityGroupRepository entityGroupRepository;
    private final SchedulerEventRepository schedulerEventRepository;
    private final RoleRepository roleRepository;
    private final AlarmRepository alarmRepository;
    private final BlobEntityRepository blobEntityRepository;

    private final DefaultQueryLogComponent queryLog;

    public DefaultEntityQueryRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate,
                                        AssetRepository assetRepository, CustomerRepository customerRepository,
                                        DeviceRepository deviceRepository, EntityViewRepository entityViewRepository,
                                        EdgeRepository edgeRepository,
                                        UserRepository userRepository, DashboardRepository dashboardRepository,
                                        EntityGroupRepository entityGroupRepository, SchedulerEventRepository schedulerEventRepository,
                                        RoleRepository roleRepository, AlarmRepository alarmRepository, BlobEntityRepository blobEntityRepository
            , DefaultQueryLogComponent queryLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.assetRepository = assetRepository;
        this.customerRepository = customerRepository;
        this.deviceRepository = deviceRepository;
        this.entityViewRepository = entityViewRepository;
        this.edgeRepository = edgeRepository;
        this.userRepository = userRepository;
        this.dashboardRepository = dashboardRepository;
        this.entityGroupRepository = entityGroupRepository;
        this.schedulerEventRepository = schedulerEventRepository;
        this.roleRepository = roleRepository;
        this.alarmRepository = alarmRepository;
        this.blobEntityRepository = blobEntityRepository;
        this.queryLog = queryLog;
    }

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityCountQuery query) {
        QueryContext ctx = buildQueryContext(tenantId, customerId, userPermissions, query.getEntityFilter(), TenantId.SYS_TENANT_ID.equals(tenantId));
        if (query.getKeyFilters() == null || query.getKeyFilters().isEmpty()) {
            ctx.append("select count(e.id) from ");
            ctx.append(addEntityTableQuery(ctx, query.getEntityFilter()));
            ctx.append(" e");
            String entityWhereClause = buildEntityWhere(ctx, query.getEntityFilter(), Collections.emptyList());
            if (!entityWhereClause.isEmpty()) {
                ctx.append(" where ");
                ctx.append(entityWhereClause);
            }
            return transactionTemplate.execute(status -> {
                long startTs = System.currentTimeMillis();
                try {
                    return jdbcTemplate.queryForObject(ctx.getQuery(), ctx, Long.class);
                } finally {
                    queryLog.logQuery(ctx, ctx.getQuery(), System.currentTimeMillis() - startTs);
                }
            });
        } else {
            MergedGroupTypePermissionInfo readPermissions = ctx.getSecurityCtx().getMergedReadPermissionsByEntityType();
            if (query.getEntityFilter().getType().equals(EntityFilterType.STATE_ENTITY_OWNER)) {
                if (ctx.getEntityType() == EntityType.TENANT && !ctx.isTenantUser()) {
                    return 0L;
                }
            } else if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
                if (hasNoPermissionsForAllRelationQueryResources(ctx.getSecurityCtx().getMergedReadEntityPermissionsMap())) {
                    return 0L;
                }
            } else if (!readPermissions.isHasGenericRead() && readPermissions.getEntityGroupIds().isEmpty()) {
                return 0L;
            } else if (customerUserIsTryingToAccessTenantEntity(ctx, query.getEntityFilter())) {
                return 0L;
            }

            List<EntityKeyMapping> mappings = EntityKeyMapping.prepareEntityCountKeyMapping(query);

            List<EntityKeyMapping> selectionMapping = mappings.stream().filter(EntityKeyMapping::isSelection)
                    .collect(Collectors.toList());
            List<EntityKeyMapping> entityFieldsSelectionMapping = selectionMapping.stream().filter(mapping -> !mapping.isLatest())
                    .collect(Collectors.toList());

            List<EntityKeyMapping> filterMapping = mappings.stream().filter(EntityKeyMapping::hasFilter)
                    .collect(Collectors.toList());
            List<EntityKeyMapping> entityFieldsFiltersMapping = filterMapping.stream().filter(mapping -> !mapping.isLatest())
                    .collect(Collectors.toList());

            List<EntityKeyMapping> allLatestMappings = mappings.stream().filter(EntityKeyMapping::isLatest)
                    .collect(Collectors.toList());


            String entityWhereClause = DefaultEntityQueryRepository.this.buildEntityWhere(ctx, query.getEntityFilter(), entityFieldsFiltersMapping);
            String latestJoinsCnt = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), allLatestMappings, true);
            String entityFieldsSelection = EntityKeyMapping.buildSelections(entityFieldsSelectionMapping, query.getEntityFilter().getType(), ctx.getEntityType());
            String entityTypeStr;
            if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
                entityTypeStr = "e.entity_type";
            } else if (query.getEntityFilter().getType().equals(EntityFilterType.ENTITY_GROUP_NAME)) {
                entityTypeStr = "'ENTITY_GROUP'";
            } else {
                entityTypeStr = "'" + ctx.getEntityType().name() + "'";
            }
            if (!StringUtils.isEmpty(entityFieldsSelection)) {
                entityFieldsSelection = String.format("e.id id, %s entity_type, %s", entityTypeStr, entityFieldsSelection);
            } else {
                entityFieldsSelection = String.format("e.id id, %s entity_type", entityTypeStr);
            }

            StringBuilder entitiesQuery;
            switch (query.getEntityFilter().getType()) {
                case RELATIONS_QUERY:
                    entitiesQuery = buildRelationsEntitiesQuery(query, ctx, entityWhereClause, entityFieldsSelection);
                    break;
                case ENTITY_GROUP_NAME:
                case ENTITY_GROUP_LIST:
                    entitiesQuery = buildGroupEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                    break;
                case SINGLE_ENTITY:
                    if (ctx.getSecurityCtx().isEntityGroup()) {
                        entitiesQuery = buildGroupEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                    } else {
                        entitiesQuery = buildCommonEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                    }
                    break;
                default:
                    entitiesQuery = buildCommonEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                    break;
            }

            String fromClauseCount = String.format("from (select %s from (%s) entities %s) result %s",
                    "entities.*",
                    entitiesQuery,
                    latestJoinsCnt,
                    "");

            String countQuery = String.format("select count(*) %s", fromClauseCount);

            return transactionTemplate.execute(status -> {
                long startTs = System.currentTimeMillis();
                try {
                    return jdbcTemplate.queryForObject(countQuery, ctx, Long.class);
                } finally {
                    queryLog.logQuery(ctx, countQuery, System.currentTimeMillis() - startTs);
                }
            });
        }
    }

    @Override
    public PageData<EntityData> findEntityDataByQueryInternal(EntityDataQuery query) {
        return findEntityDataByQuery(null, null, null, query, true);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityDataQuery query) {
        return findEntityDataByQuery(tenantId, customerId, userPermissions, query, TenantId.SYS_TENANT_ID.equals(tenantId));
    }

    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityDataQuery query, boolean ignorePermissionCheck) {
        QueryContext ctx = buildQueryContext(tenantId, customerId, userPermissions, query.getEntityFilter(), ignorePermissionCheck);
        MergedGroupTypePermissionInfo readPermissions = ctx.getSecurityCtx().getMergedReadPermissionsByEntityType();
        if (query.getEntityFilter().getType().equals(EntityFilterType.STATE_ENTITY_OWNER)) {
            if (ctx.getEntityType() == EntityType.TENANT && !ctx.isTenantUser()) {
                return new PageData<>();
            }
        } else if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            if (hasNoPermissionsForAllRelationQueryResources(ctx.getSecurityCtx().getMergedReadEntityPermissionsMap())) {
                return new PageData<>();
            }
        } else if (!readPermissions.isHasGenericRead() && readPermissions.getEntityGroupIds().isEmpty()) {
            return new PageData<>();
        } else if (customerUserIsTryingToAccessTenantEntity(ctx, query.getEntityFilter())) {
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

            List<EntityKeyMapping> allLatestMappings = mappings.stream().filter(EntityKeyMapping::isLatest)
                    .collect(Collectors.toList());


            String entityWhereClause = DefaultEntityQueryRepository.this.buildEntityWhere(ctx, query.getEntityFilter(), entityFieldsFiltersMapping);
            String latestJoinsCnt = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), allLatestMappings, true);
            String latestJoinsData = EntityKeyMapping.buildLatestJoins(ctx, query.getEntityFilter(), allLatestMappings, false);
            String textSearchQuery = DefaultEntityQueryRepository.this.buildTextSearchQuery(ctx, selectionMapping, pageLink.getTextSearch());
            String entityFieldsSelection = EntityKeyMapping.buildSelections(entityFieldsSelectionMapping, query.getEntityFilter().getType(), ctx.getEntityType());
            String entityTypeStr;
            if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
                entityTypeStr = "e.entity_type";
            } else if (query.getEntityFilter().getType().equals(EntityFilterType.ENTITY_GROUP_NAME)) {
                entityTypeStr = "'ENTITY_GROUP'";
            } else {
                entityTypeStr = "'" + ctx.getEntityType().name() + "'";
            }

            if (!StringUtils.isEmpty(entityFieldsSelection)) {
                entityFieldsSelection = String.format("e.id id, %s entity_type, %s", entityTypeStr, entityFieldsSelection);
            } else {
                entityFieldsSelection = String.format("e.id id, %s entity_type", entityTypeStr);
            }
            String latestSelection = EntityKeyMapping.buildSelections(latestSelectionMapping, query.getEntityFilter().getType(), ctx.getEntityType());
            String topSelection = "entities.*";
            if (!StringUtils.isEmpty(latestSelection)) {
                topSelection = topSelection + ", " + latestSelection;
            }

            StringBuilder entitiesQuery;
            switch (query.getEntityFilter().getType()) {
                case RELATIONS_QUERY:
                    entitiesQuery = buildRelationsEntitiesQuery(query, ctx, entityWhereClause, entityFieldsSelection);
                    break;
                case ENTITY_GROUP_NAME:
                case ENTITY_GROUP_LIST:
                    entitiesQuery = buildGroupEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                    break;
                case SINGLE_ENTITY:
                    if (ctx.getSecurityCtx().isEntityGroup()) {
                        entitiesQuery = buildGroupEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                    } else {
                        entitiesQuery = buildCommonEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                    }
                    break;
                default:
                    entitiesQuery = buildCommonEntitiesQuery(query, ctx, readPermissions, entityWhereClause, entityFieldsSelection);
                    break;
            }

            String fromClauseCount = String.format("from (select %s from (%s) entities %s) result %s",
                    "entities.*",
                    entitiesQuery,
                    latestJoinsCnt,
                    textSearchQuery);

            String fromClauseData = String.format("from (select %s from (%s) entities %s) result %s",
                    topSelection,
                    entitiesQuery,
                    latestJoinsData,
                    textSearchQuery);

            if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
                //Unfortunately, we need to sacrifice performance in case of full text search, because it is applied to all joined records.
                fromClauseCount = fromClauseData;
            }

            String countQuery = String.format("select count(*) %s", fromClauseCount);

            long startTs = System.currentTimeMillis();
            int totalElements;
            try {
                totalElements = jdbcTemplate.queryForObject(countQuery, ctx, Integer.class);
            } finally {
                queryLog.logQuery(ctx, countQuery, System.currentTimeMillis() - startTs);
            }

            if (totalElements == 0) {
                return new PageData<>();
            }
            String dataQuery = String.format("select * %s", fromClauseData);

            EntityDataSortOrder sortOrder = pageLink.getSortOrder();
            if (sortOrder != null) {
                Optional<EntityKeyMapping> sortOrderMappingOpt = mappings.stream().filter(EntityKeyMapping::isSortOrder).findFirst();
                if (sortOrderMappingOpt.isPresent()) {
                    EntityKeyMapping sortOrderMapping = sortOrderMappingOpt.get();
                    String direction = sortOrder.getDirection() == EntityDataSortOrder.Direction.ASC ? "asc" : "desc";
                    if (sortOrderMapping.getEntityKey().getType() == EntityKeyType.ENTITY_FIELD) {
                        dataQuery = String.format("%s order by %s %s, result.id %s", dataQuery, sortOrderMapping.getValueAlias(), direction, direction);
                    } else {
                        dataQuery = String.format("%s order by %s %s, %s %s, result.id %s", dataQuery,
                                sortOrderMapping.getSortOrderNumAlias(), direction, sortOrderMapping.getSortOrderStrAlias(), direction, direction);
                    }
                }
            }
            int startIndex = pageLink.getPageSize() * pageLink.getPage();
            if (pageLink.getPageSize() > 0) {
                dataQuery = String.format("%s limit %s offset %s", dataQuery, pageLink.getPageSize(), startIndex);
            }
            startTs = System.currentTimeMillis();
            List<Map<String, Object>> rows;
            try {
                rows = jdbcTemplate.queryForList(dataQuery, ctx);
            } finally {
                queryLog.logQuery(ctx, dataQuery, System.currentTimeMillis() - startTs);
            }
            return EntityDataAdapter.createEntityData(pageLink, selectionMapping, rows, totalElements);
        });
    }

    private boolean customerUserIsTryingToAccessTenantEntity(QueryContext ctx, EntityFilter entityFilter) {
        if (ctx.isTenantUser()) {
            return false;
        } else {
            switch (entityFilter.getType()) {
                case SINGLE_ENTITY:
                    SingleEntityFilter seFilter = (SingleEntityFilter) entityFilter;
                    return isSystemOrTenantEntity(seFilter.getSingleEntity().getEntityType());
                case ENTITY_LIST:
                    EntityListFilter elFilter = (EntityListFilter) entityFilter;
                    return isSystemOrTenantEntity(elFilter.getEntityType());
                case ENTITY_NAME:
                    EntityNameFilter enFilter = (EntityNameFilter) entityFilter;
                    return isSystemOrTenantEntity(enFilter.getEntityType());
                case ENTITY_TYPE:
                    EntityTypeFilter etFilter = (EntityTypeFilter) entityFilter;
                    return isSystemOrTenantEntity(etFilter.getEntityType());
                default:
                    return false;
            }
        }
    }

    private boolean isSystemOrTenantEntity(EntityType entityType) {
        switch (entityType) {
            case INTEGRATION:
            case CONVERTER:
            case DEVICE_PROFILE:
            case ASSET_PROFILE:
            case RULE_CHAIN:
            case SCHEDULER_EVENT:
            case TENANT:
            case TENANT_PROFILE:
            case WIDGET_TYPE:
            case WIDGETS_BUNDLE:
                return true;
            default:
                return false;
        }
    }

    private QueryContext buildQueryContext(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityFilter filter, boolean ignorePermissionCheck) {
        QuerySecurityContext securityContext;
        switch (filter.getType()) {
            case STATE_ENTITY_OWNER:
                EntityId ownerId = getOwnerId(tenantId, filter);
                securityContext = new QuerySecurityContext(tenantId, customerId, ownerId.getEntityType(), userPermissions, filter, ownerId, ignorePermissionCheck);
                break;
            case SINGLE_ENTITY:
                SingleEntityFilter seFilter = (SingleEntityFilter) filter;
                EntityId entityId = seFilter.getSingleEntity();
                if (entityId != null && entityId.getEntityType().equals(EntityType.ENTITY_GROUP)) {
                    EntityGroupEntity entityGroupEntity = getEntityGroup(tenantId, entityId);
                    if (entityGroupEntity != null) {
                        securityContext = new QuerySecurityContext(tenantId, customerId, EntityType.ENTITY_GROUP, userPermissions, filter, entityGroupEntity.getType(), ignorePermissionCheck);
                    } else {
                        securityContext = new QuerySecurityContext(tenantId, customerId, resolveEntityType(filter), userPermissions, filter, ignorePermissionCheck);
                    }
                } else {
                    securityContext = new QuerySecurityContext(tenantId, customerId, resolveEntityType(filter), userPermissions, filter, ignorePermissionCheck);
                }
                break;
            default:
                securityContext = new QuerySecurityContext(tenantId, customerId, resolveEntityType(filter), userPermissions, filter, ignorePermissionCheck);
        }
        return new QueryContext(securityContext);
    }

    private EntityGroupEntity getEntityGroup(TenantId tenantId, EntityId entityGroupId) {
        return entityGroupRepository.findById(entityGroupId.getId()).orElse(null);
    }

    private EntityId getOwnerId(TenantId tenantId, EntityFilter queryFilter) {
        StateEntityOwnerFilter filter = (StateEntityOwnerFilter) queryFilter;
        EntityId stateEntityId = filter.getSingleEntity();
        switch (stateEntityId.getEntityType()) {
            case TENANT:
            case RULE_CHAIN:
            case RULE_NODE:
            case INTEGRATION:
            case CONVERTER:
            case WIDGETS_BUNDLE:
            case WIDGET_TYPE:
            case GROUP_PERMISSION:
                return tenantId;
            case ASSET:
                AssetEntity assetEntity = assetRepository.findById(stateEntityId.getId()).orElse(null);
                if (assetEntity != null) {
                    return getOwnerId(assetEntity.getTenantId(), assetEntity.getCustomerId());
                }
                break;
            case CUSTOMER:
                CustomerEntity customerEntity = customerRepository.findById(stateEntityId.getId()).orElse(null);
                if (customerEntity != null) {
                    return getOwnerId(customerEntity.getTenantId(), customerEntity.getParentCustomerId());
                }
                break;
            case DEVICE:
                DeviceEntity deviceEntity = deviceRepository.findById(stateEntityId.getId()).orElse(null);
                if (deviceEntity != null) {
                    return getOwnerId(deviceEntity.getTenantId(), deviceEntity.getCustomerId());
                }
                break;
            case ENTITY_VIEW:
                EntityViewEntity entityView = entityViewRepository.findById(stateEntityId.getId()).orElse(null);
                if (entityView != null) {
                    return getOwnerId(entityView.getTenantId(), entityView.getCustomerId());
                }
                break;
            case EDGE:
                EdgeEntity edgeEntity = edgeRepository.findById(stateEntityId.getId()).orElse(null);
                if (edgeEntity != null) {
                    return getOwnerId(edgeEntity.getTenantId(), edgeEntity.getCustomerId());
                }
                break;
            case USER:
                UserEntity userEntity = userRepository.findById(stateEntityId.getId()).orElse(null);
                if (userEntity != null) {
                    return getOwnerId(userEntity.getTenantId(), userEntity.getCustomerId());
                }
                break;
            case DASHBOARD:
                DashboardEntity dashboardEntity = dashboardRepository.findById(stateEntityId.getId()).orElse(null);
                if (dashboardEntity != null) {
                    return getOwnerId(dashboardEntity.getTenantId(), dashboardEntity.getCustomerId());
                }
                break;
            case ENTITY_GROUP:
                EntityGroupEntity egEntity = entityGroupRepository.findById(stateEntityId.getId()).orElse(null);
                return EntityIdFactory.getByTypeAndUuid(egEntity.getOwnerType(), egEntity.getOwnerId());
            case SCHEDULER_EVENT:
                SchedulerEventEntity seEntity = schedulerEventRepository.findById(stateEntityId.getId()).orElse(null);
                if (seEntity != null) {
                    return getOwnerId(seEntity.getTenantId(), seEntity.getCustomerId());
                }
                break;
            case ROLE:
                RoleEntity rEntity = roleRepository.findById(stateEntityId.getId()).orElse(null);
                if (rEntity != null) {
                    return getOwnerId(rEntity.getTenantId(), rEntity.getCustomerId());
                }
                break;
            case ALARM:
                AlarmEntity aEntity = alarmRepository.findById(stateEntityId.getId()).orElse(null);
                if (aEntity != null) {
                    StateEntityOwnerFilter newFilter = new StateEntityOwnerFilter();
                    newFilter.setSingleEntity(EntityIdFactory.getByTypeAndUuid(aEntity.getOriginatorType(), aEntity.getOriginatorId()));
                    return getOwnerId(tenantId, newFilter);
                }
                break;
            case BLOB_ENTITY:
                BlobEntityEntity bEntity = blobEntityRepository.findById(stateEntityId.getId()).orElse(null);
                if (bEntity != null) {
                    return getOwnerId(bEntity.getTenantId(), bEntity.getCustomerId());
                }
                break;
        }
        return tenantId;
    }

    private EntityId getOwnerId(UUID tenantId, UUID customerId) {
        if (customerId == null || customerId.equals(CustomerId.NULL_UUID)) {
            return new TenantId(tenantId);
        } else {
            return new CustomerId(customerId);
        }
    }

    @Override
    public <T> PageData<T> findInCustomerHierarchyByRootCustomerIdOrOtherGroupIdsAndType(
            TenantId tenantId, CustomerId customerId, EntityType entityType, String type,
            List<EntityGroupId> groupIds, PageLink pageLink, Function<Map<String, Object>, T> rowMapping, boolean mobile) {
        return transactionTemplate.execute(status -> {
            QueryContext ctx = new QueryContext(new QuerySecurityContext(tenantId, customerId, entityType, null, null));
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
                if (entityType.equals(EntityType.CUSTOMER)) {
                    fromClause.append(" e.parent_customer_id in ").append(HIERARCHICAL_SUB_CUSTOMERS_QUERY);
                } else {
                    fromClause.append(" e.customer_id in ").append(HIERARCHICAL_SUB_CUSTOMERS_QUERY);
                }
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
                fromClause.append(" AND ");
                ctx.addStringParameter("type", type);
                fromClause.append(" e.type = :type ");
            }

            if (mobile) {
                if (EntityType.DASHBOARD.equals(entityType)) {
                    fromClause.append(" AND ");
                    ctx.addBooleanParameter("mobileHide", false);
                    fromClause.append(" e.mobile_hide = :mobileHide ");
                }
            }

            if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
                ctx.addStringParameter("textSearch", "%" + pageLink.getTextSearch().toLowerCase() + "%");
                fromClause.append(" AND LOWER(e.").append(ModelConstants.SEARCH_TEXT_PROPERTY).append(") LIKE :textSearch");
            }

            int totalElements = jdbcTemplate.queryForObject(String.format("select count(*) %s", fromClause), ctx, Integer.class);

            String dataQuery = String.format("select e.* %s ", fromClause);

            SortOrder sortOrder = pageLink.getSortOrder();
            if (mobile) {
                if (EntityType.DASHBOARD.equals(entityType)) {
                    dataQuery = String.format("%s order by %s asc NULLS LAST", dataQuery, "mobile_order");
                    if (sortOrder != null) {
                        String directionStr;
                        if (sortOrder.getDirection() == SortOrder.Direction.ASC) {
                            directionStr = "asc";
                        } else {
                            directionStr = "desc";
                        }
                        dataQuery = String.format("%s, %s %s", dataQuery, EntityKeyMapping.getEntityFieldColumnName(sortOrder.getProperty()), directionStr);
                    }
                }
            } else if (sortOrder != null) {
                dataQuery = String.format("%s order by %s", dataQuery, EntityKeyMapping.getEntityFieldColumnName(sortOrder.getProperty()));
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

    private StringBuilder buildRelationsEntitiesQuery(EntityCountQuery query, QueryContext ctx, String entityWhereClause, String entityFieldsSelection) {
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
                entitiesQuery.append(" AND (e.customer_id =:permissions_customer_id OR e.customer_id IN " + HIERARCHICAL_SUB_CUSTOMERS_QUERY + ") ");
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
                        if (permissions.getEntityGroupIds().isEmpty()) {
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
            entitiesQuery.append("(e.entity_type = '").append(entityType.name()).append("' AND ");
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
            entitiesQuery.append(", 1 as ").append(selectionName);
        } else if (hasNoPermissionsForAllRelationQueryResources(permissionsMap)) {
            entitiesQuery.append(", 0 as ").append(selectionName);
        } else {
            entitiesQuery.append(", CASE");
            entitiesQuery.append(" WHEN e.entity_type = 'TENANT' THEN ").append(permissionsMap.get(Resource.resourceFromEntityType(EntityType.TENANT)).isHasGenericRead() ? "1" : "0");
            for (EntityType entityType : EntityGroup.groupTypes) {
                entitiesQuery.append(" WHEN e.entity_type = '").append(entityType.name()).append("' THEN ");
                MergedGroupTypePermissionInfo permissions = permissionsMap.get(Resource.resourceFromEntityType(entityType));
                if (permissions.isHasGenericRead()) {
                    entitiesQuery.append("1");
                } else if (permissions.getEntityGroupIds().isEmpty()) {
                    entitiesQuery.append("0");
                } else {
                    entitiesQuery.append("(CASE WHEN EXISTS ( SELECT rattr.to_id FROM relation rattr WHERE rattr.to_id = e.id AND rattr.to_type = '");
                    entitiesQuery.append(entityType.name());
                    String param = groupIdParamNamePrefix + "_" + entityType.name().toLowerCase();
                    ctx.addUuidListParameter(param,
                            permissions.getEntityGroupIds().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
                    entitiesQuery.append("' and rattr.from_id in (:").append(param).append(") AND rattr.from_type = 'ENTITY_GROUP' " +
                            "AND rattr.relation_type_group = 'FROM_ENTITY_GROUP' AND rattr.relation_type = 'Contains') THEN 1 ELSE 0 END)");
                }
            }
            entitiesQuery.append(" END as ").append(selectionName);
        }
    }

    private StringBuilder buildCommonEntitiesQuery(EntityCountQuery query, QueryContext ctx,
                                                   MergedGroupTypePermissionInfo readPermissions,
                                                   String entityWhereClause, String entityFieldsSelection) {
        StringBuilder entitiesQuery = new StringBuilder();

        MergedGroupTypePermissionInfo readAttrPermissions = ctx.getSecurityCtx().getMergedReadAttrPermissionsByEntityType();
        MergedGroupTypePermissionInfo readTsPermissions = ctx.getSecurityCtx().getMergedReadTsPermissionsByEntityType();

        entitiesQuery.append("select ").append(entityFieldsSelection);

        boolean allReadPermissions = readPermissions.isHasGenericRead() && readAttrPermissions.isHasGenericRead() && readTsPermissions.isHasGenericRead();
        boolean noGroupPermissions = emptyGroups(readPermissions) && emptyGroups(readAttrPermissions) && emptyGroups(readTsPermissions);
        boolean tenantUserWithAllReadPermissions = ctx.isTenantUser() && allReadPermissions;
        boolean customerUserWithAllReadPermissionsAndNoGroupPermissions = !ctx.isTenantUser() && allReadPermissions && noGroupPermissions;
        if (tenantUserWithAllReadPermissions || customerUserWithAllReadPermissionsAndNoGroupPermissions) {
            entitiesQuery.append(", 1 as ").append(ATTR_READ_FLAG);
            entitiesQuery.append(", 1 as ").append(TS_READ_FLAG);
            entitiesQuery.append(" from ")
                    .append(addEntityTableQuery(ctx, query.getEntityFilter()))
                    .append(" e where ")
                    .append(entityWhereClause);
        } else {
            GroupIdsWrapper groupIds = new GroupIdsWrapper(readPermissions, readAttrPermissions, readTsPermissions);
            boolean innerJoin = true;
            boolean hasFilters;
            StringBuilder entityFlagsQuery = new StringBuilder();
            int paramIdx = 0;
            if (ctx.isTenantUser()) {
                if (readPermissions.isHasGenericRead()) {
                    innerJoin = false;
                    hasFilters = addGroupEntitiesByGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, permKey -> !permKey.isAttr() && !permKey.isTs());
                } else {
                    hasFilters = addGroupEntitiesByGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, null);
                }
                if (readAttrPermissions.isHasGenericRead() || readTsPermissions.isHasGenericRead()) {
                    if (hasFilters) {
                        entityFlagsQuery.append(" UNION ALL ");
                    } else {
                        hasFilters = true;
                    }
                    entityFlagsQuery.append(" select e.id to_id, ")
                            .append(boolToIntStr(readPermissions.isHasGenericRead())).append(" as readFlag").append(",")
                            .append(boolToIntStr(readAttrPermissions.isHasGenericRead())).append(" as readAttrFlag").append(",")
                            .append(boolToIntStr(readTsPermissions.isHasGenericRead())).append(" as readTsFlag")
                            .append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
                }
            } else {
                ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
                hasFilters = addGroupEntitiesByGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, null);
                if (hasFilters) {
                    entityFlagsQuery.append(" UNION ALL ");
                } else {
                    hasFilters = true;
                }
                if (!ctx.getEntityType().equals(EntityType.CUSTOMER)) {
                    entityFlagsQuery.append("select e.id to_id, ")
                            .append(boolToIntStr(readPermissions.isHasGenericRead())).append(" as readFlag").append(",")
                            .append(boolToIntStr(readAttrPermissions.isHasGenericRead())).append(" as readAttrFlag").append(",")
                            .append(boolToIntStr(readTsPermissions.isHasGenericRead())).append(" as readTsFlag");
                    entityFlagsQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
                    entityFlagsQuery.append(" where ").append(entityWhereClause);
                    entityFlagsQuery.append(" AND e.customer_id in (").append(HIERARCHICAL_SUB_CUSTOMERS_QUERY).append(")");
                } else {
                    entityFlagsQuery.append("select c.id to_id, ")
                            .append(boolToIntStr(readPermissions.isHasGenericRead())).append(" as readFlag").append(",")
                            .append(boolToIntStr(readAttrPermissions.isHasGenericRead())).append(" as readAttrFlag").append(",")
                            .append(boolToIntStr(readTsPermissions.isHasGenericRead())).append(" as readTsFlag");
                    entityFlagsQuery.append(" from customer c WHERE ");
                    entityFlagsQuery.append(" c.id in ").append(HIERARCHICAL_SUB_CUSTOMERS_QUERY);
                }
            }
            if (innerJoin || hasFilters) {
                entitiesQuery.append(", COALESCE(readAttrFlag, 0) as ").append(ATTR_READ_FLAG);
                entitiesQuery.append(", COALESCE(readTsFlag, 0) as ").append(TS_READ_FLAG);
                entitiesQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
                entitiesQuery.append(innerJoin ? "inner" : "left").append(" join ");
                if (hasFilters) {
                    entitiesQuery.append(" (select to_id as id, max(readFlag) as readFlag, max(readAttrFlag) as readAttrFlag, max(readTsFlag) as readTsFlag ");
                    entitiesQuery.append(" from (");
                    entitiesQuery.append(entityFlagsQuery);
                    entitiesQuery.append(" ) ids group by to_id) entity_flags on e.id = entity_flags.id ");
                } else {
                    entitiesQuery.append(" (select NULL::uuid as id, 0 as readFlag, 0 as readAttrFlag, 0 as readTsFlag) entity_flags on e.id = entity_flags.id ");
                }
                if (innerJoin) {
                    entitiesQuery.append(" and entity_flags.readFlag = 1");
                }
            } else {
                entitiesQuery.append(", 0 as ").append(ATTR_READ_FLAG);
                entitiesQuery.append(", 0 as ").append(TS_READ_FLAG);
                entitiesQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
            }
            entitiesQuery.append(" where ").append(entityWhereClause);
        }
        return entitiesQuery;
    }

    //Created as a workaround of HSQLDB bug with selecting multiple constants with different names.
    private static String boolToIntStr(boolean bool) {
        return bool ? "1" : "0";
    }

    private boolean addGroupIdsFilters(QueryContext ctx, GroupIdsWrapper groupIds, StringBuilder entityFlagsQuery, int paramIdx, Predicate<GroupIdPermKey> keyFilter) {
        boolean first = true;
        for (Map.Entry<GroupIdPermKey, Set<EntityGroupId>> e : groupIds.getGroupIdsMap().entrySet()) {
            GroupIdPermKey permKey = e.getKey();
            if (keyFilter != null && keyFilter.test(permKey)) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                entityFlagsQuery.append(" UNION ALL ");
            }
            ctx.addUuidListParameter("permissions_read_group_ids_" + paramIdx,
                    e.getValue().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
            entityFlagsQuery.append("select ge.id, ")
                    .append(boolToIntStr(permKey.isRead())).append(" as readFlag").append(",")
                    .append(boolToIntStr(permKey.isAttr())).append(" as readAttrFlag").append(",")
                    .append(boolToIntStr(permKey.isTs())).append(" as readTsFlag");
            entityFlagsQuery.append(" from entity_group ge WHERE ");
            entityFlagsQuery.append(" ge.id in (:permissions_read_group_ids_").append(paramIdx++).append(")");
        }
        return !first;
    }


    private boolean addGroupEntitiesByGroupIdsFilters(QueryContext ctx, GroupIdsWrapper groupIds, StringBuilder entityFlagsQuery, int paramIdx, Predicate<GroupIdPermKey> keyFilter) {
        boolean first = true;
        for (Map.Entry<GroupIdPermKey, Set<EntityGroupId>> e : groupIds.getGroupIdsMap().entrySet()) {
            GroupIdPermKey permKey = e.getKey();
            if (keyFilter != null && keyFilter.test(permKey)) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                entityFlagsQuery.append(" UNION ALL ");
            }
            ctx.addUuidListParameter("permissions_read_group_ids_" + paramIdx,
                    e.getValue().stream().map(EntityGroupId::getId).collect(Collectors.toList()));
            entityFlagsQuery.append("select re.to_id, ")
                    .append(boolToIntStr(permKey.isRead())).append(" as readFlag").append(",")
                    .append(boolToIntStr(permKey.isAttr())).append(" as readAttrFlag").append(",")
                    .append(boolToIntStr(permKey.isTs())).append(" as readTsFlag");
            entityFlagsQuery.append(" from relation re WHERE re.relation_type_group = 'FROM_ENTITY_GROUP' AND re.relation_type = 'Contains'");
            entityFlagsQuery.append(" AND re.from_id in (:permissions_read_group_ids_").append(paramIdx++).append(")");
            entityFlagsQuery.append(" AND re.from_type = 'ENTITY_GROUP'");
        }
        return !first;
    }

    private boolean emptyGroups(MergedGroupTypePermissionInfo permissions) {
        return permissions.getEntityGroupIds() == null || permissions.getEntityGroupIds().isEmpty();
    }

    private StringBuilder buildGroupEntitiesQuery(EntityCountQuery query, QueryContext ctx,
                                                  MergedGroupTypePermissionInfo readPermissions, String entityWhereClause, String entityFieldsSelection) {
        StringBuilder entitiesQuery = new StringBuilder();

        MergedGroupTypePermissionInfo readAttrPermissions = ctx.getSecurityCtx().getMergedReadAttrPermissionsByEntityType();
        MergedGroupTypePermissionInfo readTsPermissions = ctx.getSecurityCtx().getMergedReadTsPermissionsByEntityType();

        entitiesQuery.append("select ").append(entityFieldsSelection);

        boolean allReadPermissions = readPermissions.isHasGenericRead() && readAttrPermissions.isHasGenericRead() && readTsPermissions.isHasGenericRead();
        boolean noGroupPermissions = emptyGroups(readPermissions) && emptyGroups(readAttrPermissions) && emptyGroups(readTsPermissions);
        boolean tenantUserWithAllReadPermissions = ctx.isTenantUser() && allReadPermissions;
        boolean customerUserWithAllReadPermissionsAndNoGroupPermissions = !ctx.isTenantUser() && allReadPermissions && noGroupPermissions;
        if (tenantUserWithAllReadPermissions || customerUserWithAllReadPermissionsAndNoGroupPermissions) {
            entitiesQuery.append(", 1 as ").append(ATTR_READ_FLAG);
            entitiesQuery.append(", 1 as ").append(TS_READ_FLAG);
            entitiesQuery.append(" from ")
                    .append(addEntityTableQuery(ctx, query.getEntityFilter()))
                    .append(" e where ")
                    .append(entityWhereClause);
        } else {
            GroupIdsWrapper groupIds = new GroupIdsWrapper(readPermissions, readAttrPermissions, readTsPermissions);
            boolean innerJoin = true;
            boolean hasFilters;
            StringBuilder entityFlagsQuery = new StringBuilder();
            int paramIdx = 0;
            if (ctx.isTenantUser()) {
                if (readPermissions.isHasGenericRead()) {
                    innerJoin = false;
                    hasFilters = addGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, permKey -> !permKey.isAttr() && !permKey.isTs());
                } else {
                    hasFilters = addGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, null);
                }
                if (readAttrPermissions.isHasGenericRead() || readTsPermissions.isHasGenericRead()) {
                    if (hasFilters) {
                        entityFlagsQuery.append(" UNION ALL ");
                    } else {
                        hasFilters = true;
                    }
                    entityFlagsQuery.append(" select e.id id, ")
                            .append(boolToIntStr(readPermissions.isHasGenericRead())).append(" as readFlag").append(",")
                            .append(boolToIntStr(readAttrPermissions.isHasGenericRead())).append(" as readAttrFlag").append(",")
                            .append(boolToIntStr(readTsPermissions.isHasGenericRead())).append(" as readTsFlag")
                            .append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
                }
            } else {
                ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
                hasFilters = addGroupIdsFilters(ctx, groupIds, entityFlagsQuery, paramIdx, null);
                if (hasFilters) {
                    entityFlagsQuery.append(" UNION ALL ");
                } else {
                    hasFilters = true;
                }
                entityFlagsQuery.append("select ge.id, ")
                        .append(boolToIntStr(readPermissions.isHasGenericRead())).append(" as readFlag").append(",")
                        .append(boolToIntStr(readAttrPermissions.isHasGenericRead())).append(" as readAttrFlag").append(",")
                        .append(boolToIntStr(readTsPermissions.isHasGenericRead())).append(" as readTsFlag");
                entityFlagsQuery.append(" from entity_group ge WHERE");
                entityFlagsQuery.append(" ge.id in (").append(HIERARCHICAL_GROUPS_ALL_QUERY).append(" and type = '").append(ctx.getEntityType()).append("')");
            }
            if (innerJoin || hasFilters) {
                entitiesQuery.append(", COALESCE(readAttrFlag, 0) as ").append(ATTR_READ_FLAG);
                entitiesQuery.append(", COALESCE(readTsFlag, 0) as ").append(TS_READ_FLAG);
                entitiesQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
                entitiesQuery.append(innerJoin ? "inner" : "left").append(" join ");
                if (hasFilters) {
                    entitiesQuery.append(" (select id as id, max(readFlag) as readFlag, max(readAttrFlag) as readAttrFlag, max(readTsFlag) as readTsFlag ");
                    entitiesQuery.append(" from (");
                    entitiesQuery.append(entityFlagsQuery);
                    entitiesQuery.append(" ) ids group by id) entity_flags on e.id = entity_flags.id ");
                } else {
                    entitiesQuery.append(" (select NULL::uuid as id, 0 as readFlag, 0 as readAttrFlag, 0 as readTsFlag) entity_flags on e.id = entity_flags.id ");
                }
                if (innerJoin) {
                    entitiesQuery.append(" and entity_flags.readFlag = 1");
                }
            } else {
                entitiesQuery.append(", 0 as ").append(ATTR_READ_FLAG);
                entitiesQuery.append(", 0 as ").append(TS_READ_FLAG);
                entitiesQuery.append(" from ").append(addEntityTableQuery(ctx, query.getEntityFilter())).append(" e ");
            }
            entitiesQuery.append(" where ").append(entityWhereClause);
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

    private String buildEntityWhere(QueryContext ctx, EntityFilter entityFilter, List<EntityKeyMapping> entityFieldsFilters) {
        String permissionQuery = this.buildPermissionQuery(ctx, entityFilter);
        String entityFilterQuery = this.buildEntityFilterQuery(ctx, entityFilter);
        String entityFieldsQuery = EntityKeyMapping.buildQuery(ctx, entityFieldsFilters, entityFilter.getType());
        String result = permissionQuery;
        if (!entityFilterQuery.isEmpty()) {
            if (!result.isEmpty()) {
                result += " and (" + entityFilterQuery + ")";
            } else {
                result = "(" + entityFilterQuery + ")";
            }
        }
        if (!entityFieldsQuery.isEmpty()) {
            if (!result.isEmpty()) {
                result += " and (" + entityFieldsQuery + ")";
            } else {
                result = "(" + entityFieldsQuery + ")";
            }
        }
        return result;
    }

    private String buildPermissionQuery(QueryContext ctx, EntityFilter entityFilter) {
        if (ctx.isIgnorePermissionCheck()) {
            return "1=1";
        }
        switch (entityFilter.getType()) {
            case RELATIONS_QUERY:
                return "";
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
            case ENTITY_VIEW_SEARCH_QUERY:
            case EDGE_SEARCH_QUERY:
            case ENTITY_GROUP:
            case ENTITY_GROUP_NAME:
            case SCHEDULER_EVENT:
                return this.defaultPermissionQuery(ctx, entityFilter);
            case API_USAGE_STATE:
                CustomerId filterCustomerId = ((ApiUsageStateFilter) entityFilter).getCustomerId();
                if (ctx.getCustomerId() != null && !ctx.getCustomerId().isNullUid()) {
                    if (filterCustomerId != null && !filterCustomerId.equals(ctx.getCustomerId())) {
                        throw new SecurityException("Customer is not allowed to query other customer's data");
                    }
                    filterCustomerId = ctx.getCustomerId();
                }

                ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
                if (filterCustomerId != null) {
                    ctx.addUuidParameter("permissions_customer_id", filterCustomerId.getId());
                    return "e.tenant_id=:permissions_tenant_id and e.entity_id=:permissions_customer_id";
                } else {
                    return "e.tenant_id=:permissions_tenant_id and e.entity_id=:permissions_tenant_id";
                }
            default:
                if (ctx.getEntityType() == EntityType.TENANT) {
                    ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
                    return "e.id=:permissions_tenant_id";
                } else {
                    return this.defaultPermissionQuery(ctx, entityFilter);
                }
        }
    }

    private String defaultPermissionQuery(QueryContext ctx, EntityFilter entityFilter) {
        ctx.addUuidParameter("permissions_tenant_id", ctx.getTenantId().getId());
        QuerySecurityContext securityCtx = ctx.getSecurityCtx();
        if (!securityCtx.isTenantUser() && securityCtx.hasGeneric(Operation.READ) && securityCtx.getMergedReadPermissionsByEntityType().getEntityGroupIds().isEmpty()) {
            ctx.addUuidParameter("permissions_customer_id", ctx.getCustomerId().getId());
            if (ctx.getEntityType() == EntityType.CUSTOMER && entityFilter.getType() != EntityFilterType.ENTITY_GROUP_LIST
                    && entityFilter.getType() != EntityFilterType.ENTITY_GROUP_NAME) {
                return "e.tenant_id=:permissions_tenant_id and e.id in " + HIERARCHICAL_SUB_CUSTOMERS_QUERY;
            } else if (ctx.getEntityType() == EntityType.API_USAGE_STATE) {
                return "e.tenant_id=:permissions_tenant_id and e.entity_id in " + HIERARCHICAL_SUB_CUSTOMERS_QUERY;
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
            case ENTITY_GROUP_NAME:
                return this.entityGroupNameQuery(ctx, (EntityGroupNameFilter) entityFilter);
            case SCHEDULER_EVENT:
                return this.schedulerEventQuery(ctx, (SchedulerEventFilter) entityFilter);
            case ASSET_TYPE:
            case DEVICE_TYPE:
            case ENTITY_VIEW_TYPE:
            case EDGE_TYPE:
                return this.typeQuery(ctx, entityFilter);
            case STATE_ENTITY_OWNER:
                return this.singleEntityByStateOwner(ctx);
            case ENTITY_GROUP:
            case ENTITY_GROUP_LIST:
            case ENTITIES_BY_GROUP_NAME:
            case RELATIONS_QUERY:
            case DEVICE_SEARCH_QUERY:
            case ASSET_SEARCH_QUERY:
            case ENTITY_VIEW_SEARCH_QUERY:
            case EDGE_SEARCH_QUERY:
            case API_USAGE_STATE:
            case ENTITY_TYPE:
                return "";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String schedulerEventQuery(QueryContext ctx, SchedulerEventFilter entityFilter) {
        String query = "";
        if (StringUtils.isNotBlank(entityFilter.getEventType())) {
            ctx.addStringParameter("entity_filter_scheduler_event_type", entityFilter.getEventType());
            query = "e.type=:entity_filter_scheduler_event_type";
        }
        if (entityFilter.getOriginator() != null) {
            ctx.addUuidParameter("entity_filter_scheduler_originator", entityFilter.getOriginator().getId());
            query = (StringUtils.isEmpty(query) ? "" : query + " AND ") + "e.originator_id=:entity_filter_scheduler_originator";
        }

        return query;
    }

    private String addEntityTableQuery(QueryContext ctx, EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case ENTITY_GROUP:
                return entityGroupQuery(ctx, (EntityGroupFilter) entityFilter);
            case ENTITIES_BY_GROUP_NAME:
                return entityByGroupNameQuery(ctx, (EntitiesByGroupNameFilter) entityFilter);
            case ENTITY_GROUP_LIST:
                return entityGroupQueryByGroupList(ctx, (EntityGroupListFilter) entityFilter);
            case ENTITY_GROUP_NAME:
                return entityGroupQueryByGroupName(ctx, (EntityGroupNameFilter) entityFilter);
            case SINGLE_ENTITY:
                if (ctx.getSecurityCtx().isEntityGroup()) {
                    return entityGroupQueryById(ctx, (SingleEntityFilter) entityFilter);
                } else {
                    return entityTableMap.get(ctx.getEntityType());
                }
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
            case EDGE_SEARCH_QUERY:
                EdgeSearchQueryFilter edgeQuery = (EdgeSearchQueryFilter) entityFilter;
                return entitySearchQuery(ctx, edgeQuery, EntityType.EDGE, edgeQuery.getEdgeTypes());
            default:
                return entityTableMap.get(ctx.getEntityType());
        }
    }

    private String entityGroupQueryByGroupList(QueryContext ctx, EntityGroupListFilter entityFilter) {
        EntityType entityType = entityFilter.getGroupType();
        String select = "SELECT * ," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN (select tenant_id from customer where id = owner_id) ELSE owner_id END as tenant_id," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN owner_id END as customer_id" +
                " FROM entity_group WHERE type = :entity_group_type and id in (:entity_group_list_ids)";
        ctx.addStringParameter("entity_group_type", entityType.name());
        ctx.addUuidListParameter("entity_group_list_ids", entityFilter.getEntityGroupList().stream().map(UUID::fromString).collect(Collectors.toList()));
        return "(" + select + ")";
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

    private String entityGroupQueryByGroupName(QueryContext ctx, EntityGroupNameFilter entityFilter) {
        EntityType entityType = entityFilter.getGroupType();
        String select = "SELECT * ," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN (select tenant_id from customer where id = owner_id) ELSE owner_id END as tenant_id," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN owner_id END as customer_id" +
                " FROM entity_group WHERE type = :entity_group_type";
        ctx.addStringParameter("entity_group_type", entityType.name());
        if (StringUtils.isNotEmpty(entityFilter.getEntityGroupNameFilter())) {
            select = select + " and LOWER(name) LIKE LOWER(concat(:entity_group_name_prefix, '%%'))";
            ctx.addStringParameter("entity_group_name_prefix", entityFilter.getEntityGroupNameFilter());
        }
        return "(" + select + ")";
    }

    private String entityGroupQueryById(QueryContext ctx, SingleEntityFilter entityFilter) {
        EntityId entityId = entityFilter.getSingleEntity();
        String select = "SELECT * ," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN (select tenant_id from customer where id = owner_id) ELSE owner_id END as tenant_id," +
                " CASE WHEN owner_type = 'CUSTOMER' THEN owner_id END as customer_id" +
                " FROM entity_group WHERE id = :entity_group_id";
        ctx.addUuidParameter("entity_group_id", entityId.getId());
        return "(" + select + ")";
    }

    private String entityByGroupNameQuery(QueryContext ctx, EntitiesByGroupNameFilter entityFilter) {
        EntityType entityType = entityFilter.getGroupType();
        String selectFields = "SELECT * FROM " + entityTableMap.get(entityType);
        MergedGroupTypePermissionInfo groupTypePermissionInfo = ctx.getSecurityCtx().getMergedReadGroupPermissionsByEntityType();
        String where;
        if (groupTypePermissionInfo.isHasGenericRead() || !groupTypePermissionInfo.getEntityGroupIds().isEmpty()) {
            EntityId customOwnerId = entityFilter.getOwnerId();
            if (customOwnerId != null && !customOwnerId.getEntityType().equals(EntityType.TENANT) && !customOwnerId.getEntityType().equals(EntityType.CUSTOMER)) {
                customOwnerId = null;
            }
            String allowedGroupIdsSelect = "(";
            boolean genericPartAdded = false;
            if (groupTypePermissionInfo.isHasGenericRead()) {
                if (customOwnerId == null || ctx.getOwnerId().equals(customOwnerId.getId())) {
                    // No custom owner - just select a list of groups that belong to current owner
                    allowedGroupIdsSelect += "owner_id = :where_group_owner_id";
                    ctx.addUuidParameter("where_group_owner_id", ctx.getOwnerId());
                    genericPartAdded = true;
                } else if (ctx.isTenantUser()) {
                    // Tenant user with different custom owner id. We need to check that this is our customer.
                    allowedGroupIdsSelect += "owner_id in (select id from customer where id = :where_group_owner_id and tenant_id = :where_real_tenant_id)";
                    ctx.addUuidParameter("where_group_owner_id", customOwnerId.getId());
                    ctx.addUuidParameter("where_real_tenant_id", ctx.getOwnerId());
                    genericPartAdded = true;
                } else if (customOwnerId.getEntityType().equals(EntityType.CUSTOMER)) {
                    // Customer user with different custom owner id. We need to check the hierarchy now
                    allowedGroupIdsSelect += "owner_id in (select id from customer where id = :where_group_owner_id and tenant_id = :where_real_tenant_id and id in ";
                    allowedGroupIdsSelect += "(WITH RECURSIVE customers_ids(id) AS" +
                            " (SELECT id id FROM customer WHERE tenant_id = :where_real_tenant_id and id = :where_real_owner_id" +
                            " UNION SELECT c.id id FROM customer c, customers_ids parent WHERE c.tenant_id = :where_real_tenant_id" +
                            " and c.parent_customer_id = parent.id) SELECT id FROM customers_ids)";
                    allowedGroupIdsSelect += ")";
                    ctx.addUuidParameter("where_group_owner_id", customOwnerId.getId());
                    ctx.addUuidParameter("where_real_tenant_id", ctx.getTenantId().getId());
                    ctx.addUuidParameter("where_real_owner_id", ctx.getOwnerId());
                    genericPartAdded = true;
                }
            }
            if (!groupTypePermissionInfo.getEntityGroupIds().isEmpty()) {
                if (genericPartAdded) {
                    allowedGroupIdsSelect += " or ";
                }
                allowedGroupIdsSelect += "id in (:where_group_ids)";
                ctx.addUuidListParameter("where_group_ids",
                        groupTypePermissionInfo.getEntityGroupIds().stream()
                                .map(EntityGroupId::getId).collect(Collectors.toList()));
            }
            allowedGroupIdsSelect += ")";

            where = " WHERE id in (SELECT to_id from relation where from_id in " +
                    "(select id from entity_group where name=:where_group_name and type='" + entityType.name() + "' and " + allowedGroupIdsSelect + " limit 1)" +
                    " and from_type = '" + EntityType.ENTITY_GROUP.name() + "'" +
                    " and relation_type_group='" + RelationTypeGroup.FROM_ENTITY_GROUP + "' and relation_type='" + EntityRelation.CONTAINS_TYPE + "')";
            ctx.addStringParameter("where_group_name", entityFilter.getEntityGroupNameFilter());
        } else {
            where = " WHERE false";
        }

        return "( " + selectFields + where + ")";
    }

    private String entitySearchQuery(QueryContext ctx, EntitySearchQueryFilter entityFilter, EntityType entityType, List<String> types) {
        EntityId rootId = entityFilter.getRootEntity();
        String lvlFilter = getLvlFilter(entityFilter.getMaxLevel());
        String selectFields = "SELECT tenant_id, customer_id, id, created_time, type, name, additional_info "
                + (entityType.equals(EntityType.ENTITY_VIEW) ? "" : ", label ")
                + "FROM " + entityType.name() + " WHERE id in ( SELECT entity_id";
        String from = getQueryTemplate(entityFilter.getDirection(), false);
        String whereFilter = " WHERE";
        if (!StringUtils.isEmpty(entityFilter.getRelationType())) {
            ctx.addStringParameter("where_relation_type", entityFilter.getRelationType());
            whereFilter += " re.relation_type = :where_relation_type AND";
        }
        String toOrFrom = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from");
        whereFilter += " re." + (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from") + "_type = :where_entity_type";
        if (entityFilter.isFetchLastLevelOnly()) {
            String fromOrTo = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "from" : "to");
            StringBuilder notExistsPart = new StringBuilder();
            notExistsPart.append(" NOT EXISTS (SELECT 1 from relation nr ")
                    .append(whereFilter.replaceAll("re\\.", "nr\\."))
                    .append(" and ")
                    .append("nr.").append(fromOrTo).append("_id").append(" = re.").append(toOrFrom).append("_id")
                    .append(" and ")
                    .append("nr.").append(fromOrTo).append("_type").append(" = re.").append(toOrFrom).append("_type");

            notExistsPart.append(")");
            whereFilter += " and ( r_int.lvl = " + entityFilter.getMaxLevel() + " OR " + notExistsPart.toString() + ")";
        }
        from = String.format(from, lvlFilter, whereFilter);
        String query = "( " + selectFields + from + ")";
        if (types != null && !types.isEmpty()) {
            query += " and type in (:relation_sub_types)";
            ctx.addStringListParameter("relation_sub_types", types);
        }
        query += " )";
        ctx.addUuidParameter("relation_root_id", rootId.getId());
        ctx.addStringParameter("relation_root_type", rootId.getEntityType().name());
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
                SELECT_ADDRESS + ", " + SELECT_ADDRESS_2 + ", " + SELECT_ZIP + ", " + SELECT_PHONE + ", " +
                SELECT_ADDITIONAL_INFO + (entityFilter.isMultiRoot() ? (", " + SELECT_RELATED_PARENT_ID) : "") +
                ", entity.entity_type as entity_type";
        String from = getQueryTemplate(entityFilter.getDirection(), entityFilter.isMultiRoot());

        if (entityFilter.isMultiRoot()) {
            ctx.addUuidListParameter("relation_root_ids", entityFilter.getMultiRootEntityIds().stream().map(UUID::fromString).collect(Collectors.toList()));
            ctx.addStringParameter("relation_root_type", entityFilter.getMultiRootEntitiesType().name());
        } else {
            ctx.addUuidParameter("relation_root_id", rootId.getId());
            ctx.addStringParameter("relation_root_type", rootId.getEntityType().name());
        }

        StringBuilder whereFilter = new StringBuilder();
        boolean noConditions = true;
        boolean single = entityFilter.getFilters() != null && entityFilter.getFilters().size() == 1;
        if (entityFilter.getFilters() != null && !entityFilter.getFilters().isEmpty()) {
            int entityTypeFilterIdx = 0;
            for (RelationEntityTypeFilter etf : entityFilter.getFilters()) {
                String etfCondition = buildEtfCondition(ctx, etf, entityFilter.getDirection(), entityTypeFilterIdx++);
                if (!etfCondition.isEmpty()) {
                    if (noConditions) {
                        noConditions = false;
                    } else {
                        whereFilter.append(" OR ");
                    }
                    if (!single) {
                        whereFilter.append(" (");
                    }
                    whereFilter.append(etfCondition);
                    if (!single) {
                        whereFilter.append(" )");
                    }
                }
            }
        }
        if (noConditions) {
            whereFilter.append(" re.")
                    .append(entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from")
                    .append("_type in (:where_entity_types").append(")");
            ctx.addStringListParameter("where_entity_types", Arrays.stream(RELATION_QUERY_ENTITY_TYPES).map(EntityType::name).collect(Collectors.toList()));
        }

        if (!noConditions && !single) {
            whereFilter = new StringBuilder().append("(").append(whereFilter).append(")");
        }

        if (entityFilter.isFetchLastLevelOnly()) {
            String toOrFrom = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "to" : "from");
            String fromOrTo = (entityFilter.getDirection().equals(EntitySearchDirection.FROM) ? "from" : "to");

            StringBuilder notExistsPart = new StringBuilder();
            notExistsPart.append(" NOT EXISTS (SELECT 1 from relation nr WHERE ");
            notExistsPart
                    .append("nr.").append(fromOrTo).append("_id").append(" = re.").append(toOrFrom).append("_id")
                    .append(" and ")
                    .append("nr.").append(fromOrTo).append("_type").append(" = re.").append(toOrFrom).append("_type")
                    .append(" and ")
                    .append(whereFilter.toString().replaceAll("re\\.", "nr\\."));

            notExistsPart.append(")");
            whereFilter.append(" and ( r_int.lvl = ").append(entityFilter.getMaxLevel()).append(" OR ").append(notExistsPart.toString()).append(")");
        }
        from = String.format(from, lvlFilter, " WHERE " + whereFilter);
        return "( " + selectFields + from + ")";
    }

    private String buildEtfCondition(QueryContext ctx, RelationEntityTypeFilter etf, EntitySearchDirection direction, int entityTypeFilterIdx) {
        StringBuilder whereFilter = new StringBuilder();
        String relationType = etf.getRelationType();
        List<EntityType> entityTypes = etf.getEntityTypes();
        List<String> whereEntityTypes;
        if (entityTypes == null || entityTypes.isEmpty()) {
            whereEntityTypes = Collections.emptyList();
        } else {
            whereEntityTypes = etf.getEntityTypes().stream().map(EntityType::name).collect(Collectors.toList());
        }
        boolean hasRelationType = !StringUtils.isEmpty(relationType);
        if (hasRelationType) {
            ctx.addStringParameter("where_relation_type" + entityTypeFilterIdx, relationType);
            whereFilter.append("re.relation_type = :where_relation_type").append(entityTypeFilterIdx).append(" and ");
        }

        whereFilter.append("re.")
                .append(direction.equals(EntitySearchDirection.FROM) ? "to" : "from")
                .append("_type in (:where_entity_types").append(entityTypeFilterIdx).append(")");
        if (!whereEntityTypes.isEmpty()) {
            ctx.addStringListParameter("where_entity_types" + entityTypeFilterIdx, whereEntityTypes);
        } else {
            ctx.addStringListParameter("where_entity_types" + entityTypeFilterIdx,
                    Arrays.stream(RELATION_QUERY_ENTITY_TYPES).map(EntityType::name).collect(Collectors.toList()));
        }
        return whereFilter.toString();
    }

    String getLvlFilter(int maxLevel) {
        return "and re.lvl <= " + (getMaxLevel(maxLevel) - 1);
    }

    int getMaxLevel(int maxLevel) {
        return (maxLevel <= 0 || maxLevel > this.maxLevelAllowed) ? this.maxLevelAllowed : maxLevel;
    }

    private String getQueryTemplate(EntitySearchDirection direction, boolean isMultiRoot) {
        String from;
        if (direction.equals(EntitySearchDirection.FROM)) {
            from = isMultiRoot ? HIERARCHICAL_FROM_MR_QUERY_TEMPLATE : HIERARCHICAL_FROM_QUERY_TEMPLATE;
        } else {
            from = isMultiRoot ? HIERARCHICAL_TO_MR_QUERY_TEMPLATE : HIERARCHICAL_TO_QUERY_TEMPLATE;
        }
        return from;
    }

    private String buildTextSearchQuery(QueryContext ctx, List<EntityKeyMapping> selectionMapping, String searchText) {
        if (!StringUtils.isEmpty(searchText) && !selectionMapping.isEmpty()) {
            String lowerSearchText = "%" + searchText.toLowerCase() + "%";
            ctx.addStringParameter("lowerSearchTextParam", lowerSearchText);
            List<String> searchAliases = selectionMapping.stream().filter(EntityKeyMapping::isSearchable).map(EntityKeyMapping::getValueAlias).collect(Collectors.toList());
            String searchAliasesExpression;
            if (searchAliases.size() > 1) {
                searchAliasesExpression = "CONCAT(" + String.join(" , ", searchAliases) + ")";
            } else {
                searchAliasesExpression = searchAliases.get(0);
            }
            return String.format(" WHERE LOWER(%s) LIKE :%s", searchAliasesExpression, "lowerSearchTextParam");
        } else {
            return "";
        }
    }

    private String singleEntityByStateOwner(QueryContext ctx) {
        ctx.addUuidParameter("entity_filter_single_owner_id", ctx.getStateEntityOwnerId().getId());
        return "e.id=:entity_filter_single_owner_id";
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
            if (filter.getEntityNameFilter().startsWith("%") || filter.getEntityNameFilter().endsWith("%")) {
                return "lower(e.search_text) like lower(:entity_filter_name_filter)";
            }
            return "lower(e.search_text) like lower(concat(:entity_filter_name_filter, '%%'))";
        } else {
            return "";
        }
    }

    private String entityGroupNameQuery(QueryContext ctx, EntityGroupNameFilter filter) {
        ctx.addStringParameter("entity_filter_group_name_filter", filter.getEntityGroupNameFilter());
        if (filter.getEntityGroupNameFilter().startsWith("%") || filter.getEntityGroupNameFilter().endsWith("%")) {
            return "lower(e.name) like lower(:entity_filter_group_name_filter)";
        }
        return "lower(e.name) like lower(concat(:entity_filter_group_name_filter, '%%'))";
    }

    private String typeQuery(QueryContext ctx, EntityFilter filter) {
        List<String> types;
        String name;
        switch (filter.getType()) {
            case ASSET_TYPE:
                types = ((AssetTypeFilter) filter).getAssetTypes();
                name = ((AssetTypeFilter) filter).getAssetNameFilter();
                break;
            case DEVICE_TYPE:
                types = ((DeviceTypeFilter) filter).getDeviceTypes();
                name = ((DeviceTypeFilter) filter).getDeviceNameFilter();
                break;
            case ENTITY_VIEW_TYPE:
                types = ((EntityViewTypeFilter) filter).getEntityViewTypes();
                name = ((EntityViewTypeFilter) filter).getEntityViewNameFilter();
                break;
            case EDGE_TYPE:
                types = ((EdgeTypeFilter) filter).getEdgeTypes();
                name = ((EdgeTypeFilter) filter).getEdgeNameFilter();
                break;
            default:
                throw new RuntimeException("Not supported!");
        }
        String typesFilter = "e.type in (:entity_filter_type_query_types)";
        ctx.addStringListParameter("entity_filter_type_query_types", types);
        if (!StringUtils.isEmpty(name)) {
            ctx.addStringParameter("entity_filter_type_query_name", name);
            if (name.startsWith("%") || name.endsWith("%")) {
                return typesFilter + " and lower(e.search_text) like lower(:entity_filter_type_query_name)";
            }
            return typesFilter + " and lower(e.search_text) like lower(concat(:entity_filter_type_query_name, '%%'))";
        } else {
            return typesFilter;
        }
    }

    public static EntityType resolveEntityType(EntityFilter entityFilter) {
        switch (entityFilter.getType()) {
            case SINGLE_ENTITY:
                return ((SingleEntityFilter) entityFilter).getSingleEntity().getEntityType();
            case ENTITY_GROUP:
                return ((EntityGroupFilter) entityFilter).getGroupType();
            case ENTITY_LIST:
                return ((EntityListFilter) entityFilter).getEntityType();
            case ENTITY_NAME:
                return ((EntityNameFilter) entityFilter).getEntityType();
            case ENTITY_TYPE:
                return ((EntityTypeFilter) entityFilter).getEntityType();
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
            case EDGE_TYPE:
            case EDGE_SEARCH_QUERY:
                return EntityType.EDGE;
            case RELATIONS_QUERY:
                RelationsQueryFilter rgf = (RelationsQueryFilter) entityFilter;
                return rgf.isMultiRoot() ? rgf.getMultiRootEntitiesType() : rgf.getRootEntity().getEntityType();
            case API_USAGE_STATE:
                return EntityType.API_USAGE_STATE;
            case SCHEDULER_EVENT:
                return EntityType.SCHEDULER_EVENT;
            default:
                throw new RuntimeException("Not implemented!");
        }
    }
}
