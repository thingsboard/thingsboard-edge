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
package org.thingsboard.server.dao.sqlts.timescale;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sqlts.timescale.TimescaleTsKvCompositeKey;
import org.thingsboard.server.dao.model.sqlts.timescale.TimescaleTsKvEntity;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueue;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sqlts.AbstractSqlTimeseriesDao;
import org.thingsboard.server.dao.sqlts.EntityContainer;
import org.thingsboard.server.dao.sqlts.InsertTsRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@TimescaleDBTsDao
public class TimescaleTimeseriesDao extends AbstractSqlTimeseriesDao implements TimeseriesDao {

    @Autowired
    private TsKvTimescaleRepository tsKvRepository;

    @Autowired
    private AggregationRepository aggregationRepository;

    @Autowired
    protected InsertTsRepository<TimescaleTsKvEntity> insertRepository;

    protected TbSqlBlockingQueue<EntityContainer<TimescaleTsKvEntity>> tsQueue;

    @PostConstruct
    protected void init() {
        super.init();
        TbSqlBlockingQueueParams tsParams = TbSqlBlockingQueueParams.builder()
                .logName("TS Timescale")
                .batchSize(tsBatchSize)
                .maxDelay(tsMaxDelay)
                .statsPrintIntervalMs(tsStatsPrintIntervalMs)
                .build();
        tsQueue = new TbSqlBlockingQueue<>(tsParams);
        tsQueue.init(logExecutor, v -> insertRepository.saveOrUpdate(v));
    }

    @PreDestroy
    protected void destroy() {
        super.destroy();
        if (tsQueue != null) {
            tsQueue.destroy();
        }
    }

    @Override
    protected ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return findAllAsyncWithLimit(tenantId, entityId, query);
        } else {
            long startTs = query.getStartTs();
            long endTs = query.getEndTs();
            long timeBucket = query.getInterval();
            ListenableFuture<List<Optional<TsKvEntry>>> future = findAllAndAggregateAsync(tenantId, entityId, query.getKey(), startTs, endTs, timeBucket, query.getAggregation());
            return getTskvEntriesFuture(future);
        }
    }

    @Override
    protected ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        String strKey = query.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        List<TimescaleTsKvEntity> timescaleTsKvEntities = tsKvRepository.findAllWithLimit(
                tenantId.getId(),
                entityId.getId(),
                keyId,
                query.getStartTs(),
                query.getEndTs(),
                new PageRequest(0, query.getLimit(),
                        new Sort(Sort.Direction.fromString(
                                query.getOrderBy()), "ts")));
        timescaleTsKvEntities.forEach(tsKvEntity -> tsKvEntity.setStrKey(strKey));
        return Futures.immediateFuture(DaoUtil.convertDataList(timescaleTsKvEntities));
    }

    private ListenableFuture<List<Optional<TsKvEntry>>> findAllAndAggregateAsync(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, long timeBucket, Aggregation aggregation) {
        CompletableFuture<List<TimescaleTsKvEntity>> listCompletableFuture = switchAggregation(key, startTs, endTs, timeBucket, aggregation, entityId.getId(), tenantId.getId());
        SettableFuture<List<TimescaleTsKvEntity>> listenableFuture = SettableFuture.create();
        listCompletableFuture.whenComplete((timescaleTsKvEntities, throwable) -> {
            if (throwable != null) {
                listenableFuture.setException(throwable);
            } else {
                listenableFuture.set(timescaleTsKvEntities);
            }
        });
        return Futures.transform(listenableFuture, timescaleTsKvEntities -> {
            if (!CollectionUtils.isEmpty(timescaleTsKvEntities)) {
                List<Optional<TsKvEntry>> result = new ArrayList<>();
                timescaleTsKvEntities.forEach(entity -> {
                    if (entity != null && entity.isNotEmpty()) {
                        entity.setEntityId(entityId.getId());
                        entity.setTenantId(tenantId.getId());
                        entity.setStrKey(key);
                        result.add(Optional.of(DaoUtil.getData(entity)));
                    } else {
                        result.add(Optional.empty());
                    }
                });
                return result;
            } else {
                return Collections.emptyList();
            }
        });
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return processFindAllAsync(tenantId, entityId, queries);
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
    public ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        String strKey = tsKvEntry.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        TimescaleTsKvEntity entity = new TimescaleTsKvEntity();
        entity.setTenantId(tenantId.getId());
        entity.setEntityId(entityId.getId());
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(keyId);
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        entity.setJsonValue(tsKvEntry.getJsonValue().orElse(null));

        log.trace("Saving entity to timescale db: {}", entity);
        return tsQueue.add(new EntityContainer(entity, null));
    }

    @Override
    public ListenableFuture<Void> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        return getSaveLatestFuture(entityId, tsKvEntry);
    }

    @Override
    public ListenableFuture<TsKvEntry> findOneAsync(TenantId tenantId, EntityId entityId, long ts, String key) {
        return Futures.immediateFuture(DaoUtil.getData(tsKvRepository.findById(new TimescaleTsKvCompositeKey(tenantId.getId(), entityId.getId(), getOrSaveKeyId(key), ts))));
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        String strKey = query.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        return service.submit(() -> {
            tsKvRepository.delete(
                    tenantId.getId(),
                    entityId.getId(),
                    keyId,
                    query.getStartTs(),
                    query.getEndTs());
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return getRemoveLatestFuture(tenantId, entityId, query);
    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> null);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> switchAggregation(String key, long startTs, long endTs, long timeBucket, Aggregation aggregation, UUID entityId, UUID tenantId) {
        switch (aggregation) {
            case AVG:
                return findAvg(key, startTs, endTs, timeBucket, entityId, tenantId);
            case MAX:
                return findMax(key, startTs, endTs, timeBucket, entityId, tenantId);
            case MIN:
                return findMin(key, startTs, endTs, timeBucket, entityId, tenantId);
            case SUM:
                return findSum(key, startTs, endTs, timeBucket, entityId, tenantId);
            case COUNT:
                return findCount(key, startTs, endTs, timeBucket, entityId, tenantId);
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findCount(String key, long startTs, long endTs, long timeBucket, UUID entityId, UUID tenantId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findCount(
                tenantId,
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findSum(String key, long startTs, long endTs, long timeBucket, UUID entityId, UUID tenantId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findSum(
                tenantId,
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findMin(String key, long startTs, long endTs, long timeBucket, UUID entityId, UUID tenantId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findMin(
                tenantId,
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findMax(String key, long startTs, long endTs, long timeBucket, UUID entityId, UUID tenantId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findMax(
                tenantId,
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findAvg(String key, long startTs, long endTs, long timeBucket, UUID entityId, UUID tenantId) {
        Integer keyId = getOrSaveKeyId(key);
        return aggregationRepository.findAvg(
                tenantId,
                entityId,
                keyId,
                timeBucket,
                startTs,
                endTs);
    }
}