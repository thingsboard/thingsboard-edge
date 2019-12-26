/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sqlts.timescale.TimescaleTsKvCompositeKey;
import org.thingsboard.server.dao.model.sqlts.timescale.TimescaleTsKvEntity;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueue;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sqlts.AbstractSqlTimeseriesDao;
import org.thingsboard.server.dao.sqlts.AbstractTimeseriesInsertRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;


@Component
@Slf4j
@TimescaleDBTsDao
public class TimescaleTimeseriesDao extends AbstractSqlTimeseriesDao implements TimeseriesDao {

    private static final String TS = "ts";

    @Autowired
    private TsKvTimescaleRepository tsKvRepository;

    @Autowired
    private AggregationRepository aggregationRepository;

    @Autowired
    private AbstractTimeseriesInsertRepository insertRepository;

    @Autowired
    ScheduledLogExecutorComponent logExecutor;

    @Value("${sql.ts_timescale.batch_size:1000}")
    private int batchSize;

    @Value("${sql.ts_timescale.batch_max_delay:100}")
    private long maxDelay;

    @Value("${sql.ts_timescale.stats_print_interval_ms:1000}")
    private long statsPrintIntervalMs;

    private TbSqlBlockingQueue<TimescaleTsKvEntity> queue;

    @PostConstruct
    private void init() {
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("TS Timescale")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .build();
        queue = new TbSqlBlockingQueue<>(params);
        queue.init(logExecutor, v -> insertRepository.saveOrUpdate(v));
    }

    @PreDestroy
    private void destroy() {
        if (queue != null) {
            queue.destroy();
        }
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        return processFindAllAsync(tenantId, entityId, queries);
    }

    protected ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        if (query.getAggregation() == Aggregation.NONE) {
            return findAllAsyncWithLimit(tenantId, entityId, query);
        } else {
            long startTs = query.getStartTs();
            long endTs = query.getEndTs();
            long timeBucket = query.getInterval();
            ListenableFuture<List<Optional<TsKvEntry>>> future = findAndAggregateAsync(tenantId, entityId, query.getKey(), startTs, endTs, timeBucket, query.getAggregation());
            return getTskvEntriesFuture(future);
        }
    }

    private ListenableFuture<List<TsKvEntry>> findAllAsyncWithLimit(TenantId tenantId, EntityId entityId, ReadTsKvQuery query) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(
                        tsKvRepository.findAllWithLimit(
                                fromTimeUUID(tenantId.getId()),
                                fromTimeUUID(entityId.getId()),
                                query.getKey(),
                                query.getStartTs(),
                                query.getEndTs(),
                                new PageRequest(0, query.getLimit(),
                                        new Sort(Sort.Direction.fromString(
                                                query.getOrderBy()), "ts")))));
    }


    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        ListenableFuture<List<TimescaleTsKvEntity>> future = getLatest(tenantId, entityId, key, 0L, System.currentTimeMillis());
        return Futures.transform(future, latest -> {
            if (!CollectionUtils.isEmpty(latest)) {
                return DaoUtil.getData(latest.get(0));
            } else {
                return new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
            }
        }, service);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return Futures.immediateFuture(DaoUtil.convertDataList(Lists.newArrayList(tsKvRepository.findAllLatestValues(fromTimeUUID(tenantId.getId()), fromTimeUUID(entityId.getId())))));
    }

    @Override
    public ListenableFuture<Void> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        TimescaleTsKvEntity entity = new TimescaleTsKvEntity();
        entity.setTenantId(fromTimeUUID(tenantId.getId()));
        entity.setEntityId(fromTimeUUID(entityId.getId()));
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(tsKvEntry.getKey());
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        return queue.add(entity);
    }

    @Override
    public ListenableFuture<Void> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key, long ttl) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<TsKvEntry> findOneAsync(TenantId tenantId, EntityId entityId, long ts, String key) {
        return Futures.immediateFuture(DaoUtil.getData(tsKvRepository.findById(new TimescaleTsKvCompositeKey(tenantId.getId().toString(), entityId.getId().toString(), key, ts))));
    }

    @Override
    public ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> {
            tsKvRepository.delete(
                    fromTimeUUID(tenantId.getId()),
                    fromTimeUUID(entityId.getId()),
                    query.getKey(),
                    query.getStartTs(),
                    query.getEndTs());
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> removePartition(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        return service.submit(() -> null);
    }

    private ListenableFuture<Void> getNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<List<TsKvEntry>> future = findNewLatestEntryFuture(tenantId, entityId, query);
        return Futures.transformAsync(future, entryList -> {
            if (entryList.size() == 1) {
                return save(tenantId, entityId, entryList.get(0), 0L);
            } else {
                log.trace("Could not find new latest value for [{}], key - {}", entityId, query.getKey());
            }
            return Futures.immediateFuture(null);
        }, service);
    }

    private ListenableFuture<List<TimescaleTsKvEntity>> findLatestByQuery(TenantId tenantId, EntityId entityId, TsKvQuery query) {
        return getLatest(tenantId, entityId, query.getKey(), query.getStartTs(), query.getEndTs());
    }

    private ListenableFuture<List<TimescaleTsKvEntity>> getLatest(TenantId tenantId, EntityId entityId, String key, long start, long end) {
        return Futures.immediateFuture(tsKvRepository.findAllWithLimit(
                fromTimeUUID(tenantId.getId()),
                fromTimeUUID(entityId.getId()),
                key,
                start,
                end,
                new PageRequest(0, 1,
                        new Sort(Sort.Direction.DESC, TS))));
    }

    private ListenableFuture<List<Optional<TsKvEntry>>> findAndAggregateAsync(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, long timeBucket, Aggregation aggregation) {
        String entityIdStr = fromTimeUUID(entityId.getId());
        String tenantIdStr = fromTimeUUID(tenantId.getId());
        CompletableFuture<List<TimescaleTsKvEntity>> listCompletableFuture = switchAgregation(key, startTs, endTs, timeBucket, aggregation, entityIdStr, tenantIdStr);
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
                        entity.setEntityId(entityIdStr);
                        entity.setTenantId(tenantIdStr);
                        entity.setKey(key);
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

    private CompletableFuture<List<TimescaleTsKvEntity>> switchAgregation(String key, long startTs, long endTs, long timeBucket, Aggregation aggregation, String entityIdStr, String tenantIdStr) {
        switch (aggregation) {
            case AVG:
                return findAvg(key, startTs, endTs, timeBucket, entityIdStr, tenantIdStr);
            case MAX:
                return findMax(key, startTs, endTs, timeBucket, entityIdStr, tenantIdStr);
            case MIN:
                return findMin(key, startTs, endTs, timeBucket, entityIdStr, tenantIdStr);
            case SUM:
                return findSum(key, startTs, endTs, timeBucket, entityIdStr, tenantIdStr);
            case COUNT:
                return findCount(key, startTs, endTs, timeBucket, entityIdStr, tenantIdStr);
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findAvg(String key, long startTs, long endTs, long timeBucket, String entityIdStr, String tenantIdStr) {
        return aggregationRepository.findAvg(
                tenantIdStr,
                entityIdStr,
                key,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findMax(String key, long startTs, long endTs, long timeBucket, String entityIdStr, String tenantIdStr) {
        return aggregationRepository.findMax(
                tenantIdStr,
                entityIdStr,
                key,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findMin(String key, long startTs, long endTs, long timeBucket, String entityIdStr, String tenantIdStr) {
        return aggregationRepository.findMin(
                tenantIdStr,
                entityIdStr,
                key,
                timeBucket,
                startTs,
                endTs);

    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findSum(String key, long startTs, long endTs, long timeBucket, String entityIdStr, String tenantIdStr) {
        return aggregationRepository.findSum(
                tenantIdStr,
                entityIdStr,
                key,
                timeBucket,
                startTs,
                endTs);
    }

    private CompletableFuture<List<TimescaleTsKvEntity>> findCount(String key, long startTs, long endTs, long timeBucket, String entityIdStr, String tenantIdStr) {
        return aggregationRepository.findCount(
                tenantIdStr,
                entityIdStr,
                key,
                timeBucket,
                startTs,
                endTs);
    }
}