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
package org.thingsboard.server.dao.event;

import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.EventEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;
import static org.thingsboard.server.dao.model.ModelConstants.*;

@Component
@Slf4j
@NoSqlDao
public class CassandraBaseEventDao extends CassandraAbstractSearchTimeDao<EventEntity, Event> implements EventDao {

    private final TenantId systemTenantId = new TenantId(NULL_UUID);

    @Override
    protected Class<EventEntity> getColumnFamilyClass() {
        return EventEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return EVENT_COLUMN_FAMILY_NAME;
    }

    @Value("${cassandra.query.events_ttl:0}")
    private int eventsTtl;

    @Value("${cassandra.query.debug_events_ttl:0}")
    private int debugEventsTtl;

    @Override
    public Event save(TenantId tenantId, Event event) {
        try {
            return saveAsync(event).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Could not save EventEntity", e);
        }
    }

    @Override
    public ListenableFuture<Event> saveAsync(Event event) {
        log.debug("Save event [{}] ", event);
        if (event.getTenantId() == null) {
            log.trace("Save system event with predefined id {}", systemTenantId);
            event.setTenantId(systemTenantId);
        }
        if (event.getId() == null) {
            event.setId(new EventId(UUIDs.timeBased()));
        }
        if (StringUtils.isEmpty(event.getUid())) {
            event.setUid(event.getId().toString());
        }
        ListenableFuture<Optional<Event>> optionalSave = saveAsync(event.getTenantId(), new EventEntity(event), false, eventsTtl);
        return Futures.transform(optionalSave, opt -> opt.orElse(null));
    }

    @Override
    public Optional<Event> saveIfNotExists(Event event) {
        if (event.getTenantId() == null) {
            log.trace("Save system event with predefined id {}", systemTenantId);
            event.setTenantId(systemTenantId);
        }
        if (event.getId() == null) {
            event.setId(new EventId(UUIDs.timeBased()));
        }
        return save(event.getTenantId(), new EventEntity(event), true, eventsTtl);
    }

    @Override
    public Event findEvent(UUID tenantId, EntityId entityId, String eventType, String eventUid) {
        log.debug("Search event entity by [{}][{}][{}][{}]", tenantId, entityId, eventType, eventUid);
        Select.Where query = select().from(getColumnFamilyName()).where(
                eq(ModelConstants.EVENT_TENANT_ID_PROPERTY, tenantId))
                .and(eq(ModelConstants.EVENT_ENTITY_TYPE_PROPERTY, entityId.getEntityType()))
                .and(eq(ModelConstants.EVENT_ENTITY_ID_PROPERTY, entityId.getId()))
                .and(eq(ModelConstants.EVENT_TYPE_PROPERTY, eventType))
                .and(eq(ModelConstants.EVENT_UID_PROPERTY, eventUid));
        log.trace("Execute query [{}]", query);
        EventEntity entity = findOneByStatement(new TenantId(tenantId), query);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for event entity [{}]", entity != null, entity);
        } else {
            log.debug("Search result: [{}]", entity != null);
        }
        return DaoUtil.getData(entity);
    }

    @Override
    public List<Event> findEvents(UUID tenantId, EntityId entityId, TimePageLink pageLink) {
        log.trace("Try to find events by tenant [{}], entity [{}]and pageLink [{}]", tenantId, entityId, pageLink);
        List<EventEntity> entities = findPageWithTimeSearch(new TenantId(tenantId), EVENT_BY_ID_VIEW_NAME,
                Arrays.asList(eq(ModelConstants.EVENT_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.EVENT_ENTITY_TYPE_PROPERTY, entityId.getEntityType()),
                        eq(ModelConstants.EVENT_ENTITY_ID_PROPERTY, entityId.getId())),
                pageLink);
        log.trace("Found events by tenant [{}], entity [{}] and pageLink [{}]", tenantId, entityId, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<Event> findEvents(UUID tenantId, EntityId entityId, String eventType, TimePageLink pageLink) {
        log.trace("Try to find events by tenant [{}], entity [{}], type [{}] and pageLink [{}]", tenantId, entityId, eventType, pageLink);
        List<EventEntity> entities = findPageWithTimeSearch(new TenantId(tenantId), EVENT_BY_TYPE_AND_ID_VIEW_NAME,
                Arrays.asList(eq(ModelConstants.EVENT_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.EVENT_ENTITY_TYPE_PROPERTY, entityId.getEntityType()),
                        eq(ModelConstants.EVENT_ENTITY_ID_PROPERTY, entityId.getId()),
                        eq(ModelConstants.EVENT_TYPE_PROPERTY, eventType)),
                pageLink.isAscOrder() ? QueryBuilder.asc(ModelConstants.EVENT_TYPE_PROPERTY) :
                        QueryBuilder.desc(ModelConstants.EVENT_TYPE_PROPERTY),
                pageLink);
        log.trace("Found events by tenant [{}], entity [{}], type [{}] and pageLink [{}]", tenantId, entityId, eventType, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<Event> findLatestEvents(UUID tenantId, EntityId entityId, String eventType, int limit) {
        log.trace("Try to find telemetry events by tenant [{}], entity [{}], type [{}] and limit [{}]", tenantId, entityId, eventType, limit);
        Select select = select().from(EVENT_BY_TYPE_AND_ID_VIEW_NAME);
        Select.Where query = select.where();
        query.and(eq(ModelConstants.EVENT_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(ModelConstants.EVENT_ENTITY_TYPE_PROPERTY, entityId.getEntityType()));
        query.and(eq(ModelConstants.EVENT_ENTITY_ID_PROPERTY, entityId.getId()));
        query.and(eq(ModelConstants.EVENT_TYPE_PROPERTY, eventType));
        query.limit(limit);
        query.orderBy(QueryBuilder.desc(ModelConstants.EVENT_TYPE_PROPERTY), QueryBuilder.desc(ModelConstants.ID_PROPERTY));
        List<EventEntity> entities = findListByStatement(new TenantId(tenantId), query);
        return DaoUtil.convertDataList(entities);
    }

    private Optional<Event> save(TenantId tenantId, EventEntity entity, boolean ifNotExists, int ttl) {
        try {
            return saveAsync(tenantId, entity, ifNotExists, ttl).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Could not save EventEntity", e);
        }
    }

    private ListenableFuture<Optional<Event>> saveAsync(TenantId tenantId, EventEntity entity, boolean ifNotExists, int ttl) {
        if (entity.getId() == null) {
            entity.setId(UUIDs.timeBased());
        }
        Insert insert = QueryBuilder.insertInto(getColumnFamilyName())
                .value(ModelConstants.ID_PROPERTY, entity.getId())
                .value(ModelConstants.EVENT_TENANT_ID_PROPERTY, entity.getTenantId())
                .value(ModelConstants.EVENT_ENTITY_TYPE_PROPERTY, entity.getEntityType())
                .value(ModelConstants.EVENT_ENTITY_ID_PROPERTY, entity.getEntityId())
                .value(ModelConstants.EVENT_TYPE_PROPERTY, entity.getEventType())
                .value(ModelConstants.EVENT_UID_PROPERTY, entity.getEventUid())
                .value(ModelConstants.EVENT_BODY_PROPERTY, entity.getBody());

        if (ifNotExists) {
            insert = insert.ifNotExists();
        }

        int selectedTtl = (entity.getEventType().equals(DataConstants.DEBUG_RULE_NODE) ||
                entity.getEventType().equals(DataConstants.DEBUG_RULE_CHAIN)) ? debugEventsTtl : ttl;

        if (selectedTtl > 0) {
            insert.using(ttl(selectedTtl));
        }
        ResultSetFuture resultSetFuture = executeAsyncWrite(tenantId, insert);
        return Futures.transform(resultSetFuture, rs -> {
            if (rs.wasApplied()) {
                return Optional.of(DaoUtil.getData(entity));
            } else {
                return Optional.empty();
            }
        });
    }
}
