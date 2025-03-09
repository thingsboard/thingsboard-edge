/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sqlts;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.VersionedTbCache;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.cache.CacheExecutorService;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;
import org.thingsboard.server.dao.timeseries.TsLatestCacheKey;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDaoCachedRedis;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@SqlTsLatestAnyDaoCachedRedis
@RequiredArgsConstructor
@Primary
public class CachedRedisSqlTimeseriesLatestDao extends BaseAbstractSqlTimeseriesDao implements TimeseriesLatestDao {
    public static final String STATS_NAME = "ts_latest.cache";
    final CacheExecutorService cacheExecutorService;
    final SqlTimeseriesLatestDao sqlDao;
    final StatsFactory statsFactory;
    final VersionedTbCache<TsLatestCacheKey, TsKvEntry> cache;
    DefaultCounter hitCounter;
    DefaultCounter missCounter;

    @PostConstruct
    public void init() {
        log.info("Init Redis cache-aside SQL Timeseries Latest DAO");
        this.hitCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "hit");
        this.missCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "miss");
    }

    @Override
    public ListenableFuture<Long> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        ListenableFuture<Long> future = sqlDao.saveLatest(tenantId, entityId, tsKvEntry);
        future = Futures.transform(future, version -> {
                    cache.put(new TsLatestCacheKey(entityId, tsKvEntry.getKey()), new BasicTsKvEntry(tsKvEntry.getTs(), ((BasicTsKvEntry) tsKvEntry).getKv(), version));
                    return version;
                },
                cacheExecutorService);
        if (log.isTraceEnabled()) {
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(Long result) {
                    log.trace("saveLatest onSuccess [{}][{}][{}]", entityId, tsKvEntry.getKey(), tsKvEntry);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.info("saveLatest onFailure [{}][{}][{}]", entityId, tsKvEntry.getKey(), tsKvEntry, t);
                }
            }, MoreExecutors.directExecutor());
        }
        return future;
    }

    @Override
    public ListenableFuture<TsKvLatestRemovingResult> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        ListenableFuture<TsKvLatestRemovingResult> future = sqlDao.removeLatest(tenantId, entityId, query);
        future = Futures.transform(future, x -> {
                    if (x.isRemoved()) {
                        TsLatestCacheKey key = new TsLatestCacheKey(entityId, query.getKey());
                        Long version = x.getVersion();
                        TsKvEntry newTsKvEntry = x.getData();
                        if (newTsKvEntry != null) {
                            cache.put(key, new BasicTsKvEntry(newTsKvEntry.getTs(), ((BasicTsKvEntry) newTsKvEntry).getKv(), version));
                        } else {
                            cache.evict(key, version);
                        }
                    }
                    return x;
                },
                cacheExecutorService);
        if (log.isTraceEnabled()) {
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(TsKvLatestRemovingResult result) {
                    log.trace("removeLatest onSuccess [{}][{}][{}]", entityId, query.getKey(), query);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.info("removeLatest onFailure [{}][{}][{}]", entityId, query.getKey(), query, t);
                }
            }, MoreExecutors.directExecutor());
        }
        return future;
    }

    @Override
    public ListenableFuture<Optional<TsKvEntry>> findLatestOpt(TenantId tenantId, EntityId entityId, String key) {
        log.trace("findLatestOpt");
        return doFindLatest(tenantId, entityId, key);
    }

    @Override
    public ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key) {
        return Futures.transform(doFindLatest(tenantId, entityId, key), x -> sqlDao.wrapNullTsKvEntry(key, x.orElse(null)), MoreExecutors.directExecutor());
    }

    public ListenableFuture<Optional<TsKvEntry>> doFindLatest(TenantId tenantId, EntityId entityId, String key) {
        final TsLatestCacheKey cacheKey = new TsLatestCacheKey(entityId, key);
        ListenableFuture<TbCacheValueWrapper<TsKvEntry>> cacheFuture = cacheExecutorService.submit(() -> cache.get(cacheKey));

        return Futures.transformAsync(cacheFuture, (cacheValueWrap) -> {
            if (cacheValueWrap != null) {
                final TsKvEntry tsKvEntry = cacheValueWrap.get();
                log.debug("findLatest cache hit [{}][{}][{}]", entityId, key, tsKvEntry);
                return Futures.immediateFuture(Optional.ofNullable(tsKvEntry));
            }
            log.debug("findLatest cache miss [{}][{}]", entityId, key);
            ListenableFuture<Optional<TsKvEntry>> daoFuture = sqlDao.findLatestOpt(tenantId, entityId, key);

            return Futures.transform(daoFuture, daoValue -> {
                cache.put(cacheKey, daoValue.orElse(null));
                return daoValue;
            }, MoreExecutors.directExecutor());
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        return sqlDao.findAllLatest(tenantId, entityId);
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return sqlDao.findAllKeysByDeviceProfileId(tenantId, deviceProfileId);
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        return sqlDao.findAllKeysByEntityIds(tenantId, entityIds);
    }


}
