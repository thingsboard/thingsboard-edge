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
package org.thingsboard.server.dao.sqlts.hsql;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sqlts.hsql.TsKvCompositeKey;
import org.thingsboard.server.dao.model.sqlts.hsql.TsKvEntity;
import org.thingsboard.server.dao.sqlts.AbstractSimpleSqlTimeseriesDao;
import org.thingsboard.server.dao.sqlts.EntityContainer;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlTsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;


@Component
@Slf4j
@SqlTsDao
@HsqlDao
public class JpaHsqlTimeseriesDao extends AbstractSimpleSqlTimeseriesDao<TsKvEntity> implements TimeseriesDao {

    @Autowired
    private TsKvHsqlRepository tsKvRepository;

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return processFindAllAsync(tenantId, entityId, queries);
    }

    @Override
    public ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        TsKvEntity entity = new TsKvEntity();
        entity.setEntityType(entityId.getEntityType());
        entity.setEntityId(fromTimeUUID(entityId.getId()));
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(tsKvEntry.getKey());
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        log.trace("Saving entity: {}", entity);
        return tsQueue.add(new EntityContainer(entity, null));
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> {
            tsKvRepository.delete(
                    fromTimeUUID(entityId.getId()),
                    entityId.getEntityType(),
                    query.getKey(),
                    query.getStartTs(),
                    query.getEndTs());
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        return getSaveLatestFuture(entityId, tsKvEntry);
    }

    @Override
    public ListenableFuture<Void> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return getRemoveLatestFuture(tenantId, entityId, query);
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        return getFindLatestFuture(entityId, key);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return getFindAllLatestFuture(entityId);
    }

    @Override
    public ListenableFuture<Void> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<TsKvEntry> findOneAsync(TenantId tenantId, EntityId entityId, long ts, String key) {
        return Futures.immediateFuture(DaoUtil.getData(tsKvRepository.findById(new TsKvCompositeKey(entityId.getEntityType(), entityId.getId().toString(), key, ts))));
    }


    protected ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation) {
        List<CompletableFuture<TsKvEntity>> entitiesFutures = new ArrayList<>();
        switchAgregation(entityId, key, startTs, endTs, aggregation, entitiesFutures);
        return Futures.transform(setFutures(entitiesFutures), entity -> {
            if (entity != null && entity.isNotEmpty()) {
                entity.setEntityId(fromTimeUUID(entityId.getId()));
                entity.setEntityType(entityId.getEntityType());
                entity.setKey(key);
                entity.setTs(ts);
                return Optional.of(DaoUtil.getData(entity));
            } else {
                return Optional.empty();
            }
        });
    }

    protected ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(EntityId entityId, ReadTsKvQuery query) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(
                        tsKvRepository.findAllWithLimit(
                                fromTimeUUID(entityId.getId()),
                                entityId.getEntityType(),
                                query.getKey(),
                                query.getStartTs(),
                                query.getEndTs(),
                                new PageRequest(0, query.getLimit(),
                                        new Sort(Sort.Direction.fromString(
                                                query.getOrderBy()), "ts")))));
    }

    protected void findCount(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TsKvEntity>> entitiesFutures) {
        entitiesFutures.add(tsKvRepository.findCount(
                fromTimeUUID(entityId.getId()),
                entityId.getEntityType(),
                key,
                startTs,
                endTs));
    }

    protected void findSum(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TsKvEntity>> entitiesFutures) {
        entitiesFutures.add(tsKvRepository.findSum(
                fromTimeUUID(entityId.getId()),
                entityId.getEntityType(),
                key,
                startTs,
                endTs));
    }

    protected void findMin(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TsKvEntity>> entitiesFutures) {
        entitiesFutures.add(tsKvRepository.findStringMin(
                fromTimeUUID(entityId.getId()),
                entityId.getEntityType(),
                key,
                startTs,
                endTs));
        entitiesFutures.add(tsKvRepository.findNumericMin(
                fromTimeUUID(entityId.getId()),
                entityId.getEntityType(),
                key,
                startTs,
                endTs));
    }

    protected void findMax(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TsKvEntity>> entitiesFutures) {
        entitiesFutures.add(tsKvRepository.findStringMax(
                fromTimeUUID(entityId.getId()),
                entityId.getEntityType(),
                key,
                startTs,
                endTs));
        entitiesFutures.add(tsKvRepository.findNumericMax(
                fromTimeUUID(entityId.getId()),
                entityId.getEntityType(),
                key,
                startTs,
                endTs));
    }

    protected void findAvg(EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<TsKvEntity>> entitiesFutures) {
        entitiesFutures.add(tsKvRepository.findAvg(
                fromTimeUUID(entityId.getId()),
                entityId.getEntityType(),
                key,
                startTs,
                endTs));
    }

}