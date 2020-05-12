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
package org.thingsboard.server.dao.attributes;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.nosql.CassandraAbstractAsyncDao;
import org.thingsboard.server.dao.timeseries.CassandraBaseTimeseriesDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.ATTRIBUTES_KV_CF;
import static org.thingsboard.server.dao.model.ModelConstants.ATTRIBUTE_KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ATTRIBUTE_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.LAST_UPDATE_TS_COLUMN;

/**
 * @author Andrew Shvayka
 */
@Component
@Slf4j
@NoSqlDao
public class CassandraBaseAttributesDao extends CassandraAbstractAsyncDao implements AttributesDao {

    private PreparedStatement saveStmt;

    @PostConstruct
    public void init() {
        super.startExecutor();
    }

    @PreDestroy
    public void stop() {
        super.stopExecutor();
    }

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String attributeType, String attributeKey) {
        Select.Where select = select().from(ATTRIBUTES_KV_CF)
                .where(eq(ENTITY_TYPE_COLUMN, entityId.getEntityType()))
                .and(eq(ENTITY_ID_COLUMN, entityId.getId()))
                .and(eq(ATTRIBUTE_TYPE_COLUMN, attributeType))
                .and(eq(ATTRIBUTE_KEY_COLUMN, attributeKey));
        log.trace("Generated query [{}] for entityId {} and key {}", select, entityId, attributeKey);
        return Futures.transform(executeAsyncRead(tenantId, select), (Function<? super ResultSet, ? extends Optional<AttributeKvEntry>>) input ->
                        Optional.ofNullable(convertResultToAttributesKvEntry(attributeKey, input.one()))
                , readResultsProcessingExecutor);
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String attributeType, Collection<String> attributeKeys) {
        List<ListenableFuture<Optional<AttributeKvEntry>>> entries = new ArrayList<>();
        attributeKeys.forEach(attributeKey -> entries.add(find(tenantId, entityId, attributeType, attributeKey)));
        return Futures.transform(Futures.allAsList(entries), (Function<List<Optional<AttributeKvEntry>>, ? extends List<AttributeKvEntry>>) input -> {
            List<AttributeKvEntry> result = new ArrayList<>();
            input.stream().filter(opt -> opt.isPresent()).forEach(opt -> result.add(opt.get()));
            return result;
        }, readResultsProcessingExecutor);
    }


    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, String attributeType) {
        Select.Where select = select().from(ATTRIBUTES_KV_CF)
                .where(eq(ENTITY_TYPE_COLUMN, entityId.getEntityType()))
                .and(eq(ENTITY_ID_COLUMN, entityId.getId()))
                .and(eq(ATTRIBUTE_TYPE_COLUMN, attributeType));
        log.trace("Generated query [{}] for entityId {} and attributeType {}", select, entityId, attributeType);
        return Futures.transform(executeAsyncRead(tenantId, select), (Function<? super ResultSet, ? extends List<AttributeKvEntry>>) input ->
                        convertResultToAttributesKvEntryList(input)
                , readResultsProcessingExecutor);
    }

    @Override
    public ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, String attributeType, AttributeKvEntry attribute) {
        BoundStatement stmt = getSaveStmt().bind()
                .setString(0, entityId.getEntityType().name())
                .setUUID(1, entityId.getId())
                .setString(2, attributeType)
                .setString(3, attribute.getKey())
                .setLong(4, attribute.getLastUpdateTs())
                .set(5, attribute.getStrValue().orElse(null), String.class)
                .set(6, attribute.getBooleanValue().orElse(null), Boolean.class)
                .set(7, attribute.getLongValue().orElse(null), Long.class)
                .set(8, attribute.getDoubleValue().orElse(null), Double.class)
                .set(9, attribute.getJsonValue().orElse(null), String.class);

        log.trace("Generated save stmt [{}] for entityId {} and attributeType {} and attribute", stmt, entityId, attributeType, attribute);
        return getFuture(executeAsyncWrite(tenantId, stmt), rs -> null);
    }

    @Override
    public ListenableFuture<List<Void>> removeAll(TenantId tenantId, EntityId entityId, String attributeType, List<String> keys) {
        List<ListenableFuture<Void>> futures = keys
                .stream()
                .map(key -> delete(tenantId, entityId, attributeType, key))
                .collect(Collectors.toList());
        return Futures.allAsList(futures);
    }

    private ListenableFuture<Void> delete(TenantId tenantId, EntityId entityId, String attributeType, String key) {
        Statement delete = QueryBuilder.delete().all().from(ModelConstants.ATTRIBUTES_KV_CF)
                .where(eq(ENTITY_TYPE_COLUMN, entityId.getEntityType()))
                .and(eq(ENTITY_ID_COLUMN, entityId.getId()))
                .and(eq(ATTRIBUTE_TYPE_COLUMN, attributeType))
                .and(eq(ATTRIBUTE_KEY_COLUMN, key));
        log.debug("Remove request: {}", delete.toString());
        return getFuture(executeAsyncWrite(tenantId, delete), rs -> null);
    }

    private PreparedStatement getSaveStmt() {
        if (saveStmt == null) {
            saveStmt = prepare("INSERT INTO " + ModelConstants.ATTRIBUTES_KV_CF +
                    "(" + ENTITY_TYPE_COLUMN +
                    "," + ENTITY_ID_COLUMN +
                    "," + ATTRIBUTE_TYPE_COLUMN +
                    "," + ATTRIBUTE_KEY_COLUMN +
                    "," + LAST_UPDATE_TS_COLUMN +
                    "," + ModelConstants.STRING_VALUE_COLUMN +
                    "," + ModelConstants.BOOLEAN_VALUE_COLUMN +
                    "," + ModelConstants.LONG_VALUE_COLUMN +
                    "," + ModelConstants.DOUBLE_VALUE_COLUMN +
                    "," + ModelConstants.JSON_VALUE_COLUMN +
                    ")" +
                    " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        }
        return saveStmt;
    }

    private AttributeKvEntry convertResultToAttributesKvEntry(String key, Row row) {
        AttributeKvEntry attributeEntry = null;
        if (row != null) {
            long lastUpdateTs = row.get(LAST_UPDATE_TS_COLUMN, Long.class);
            attributeEntry = new BaseAttributeKvEntry(CassandraBaseTimeseriesDao.toKvEntry(row, key), lastUpdateTs);
        }
        return attributeEntry;
    }

    private List<AttributeKvEntry> convertResultToAttributesKvEntryList(ResultSet resultSet) {
        List<Row> rows = resultSet.all();
        List<AttributeKvEntry> entries = new ArrayList<>(rows.size());
        if (!rows.isEmpty()) {
            rows.forEach(row -> {
                String key = row.getString(ModelConstants.ATTRIBUTE_KEY_COLUMN);
                AttributeKvEntry kvEntry = convertResultToAttributesKvEntry(key, row);
                if (kvEntry != null) {
                    entries.add(kvEntry);
                }
            });
        }
        return entries;
    }
}
