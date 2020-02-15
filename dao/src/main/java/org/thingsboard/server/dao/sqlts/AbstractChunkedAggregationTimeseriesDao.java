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
package org.thingsboard.server.dao.sqlts;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueue;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractChunkedAggregationTimeseriesDao<T extends AbstractTsKvEntity> extends AbstractSqlTimeseriesDao {

    @Autowired
    protected InsertTsRepository<T> insertRepository;

    protected TbSqlBlockingQueue<EntityContainer<T>> tsQueue;

    @PostConstruct
    protected void init() {
        super.init();
        TbSqlBlockingQueueParams tsParams = TbSqlBlockingQueueParams.builder()
                .logName("TS")
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

    protected abstract ListenableFuture<Optional<TsKvEntry>> findAndAggregateAsync(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, long ts, Aggregation aggregation);

    protected void switchAggregation(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, Aggregation aggregation, List<CompletableFuture<T>> entitiesFutures) {
        switch (aggregation) {
            case AVG:
                findAvg(tenantId, entityId, key, startTs, endTs, entitiesFutures);
                break;
            case MAX:
                findMax(tenantId, entityId, key, startTs, endTs, entitiesFutures);
                break;
            case MIN:
                findMin(tenantId, entityId, key, startTs, endTs, entitiesFutures);
                break;
            case SUM:
                findSum(tenantId, entityId, key, startTs, endTs, entitiesFutures);
                break;
            case COUNT:
                findCount(tenantId, entityId, key, startTs, endTs, entitiesFutures);
                break;
            default:
                throw new IllegalArgumentException("Not supported aggregation type: " + aggregation);
        }
    }

    protected abstract void findCount(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures);

    protected abstract void findSum(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures);

    protected abstract void findMin(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures);

    protected abstract void findMax(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures);

    protected abstract void findAvg(TenantId tenantId, EntityId entityId, String key, long startTs, long endTs, List<CompletableFuture<T>> entitiesFutures);

    protected SettableFuture<T> setFutures(List<CompletableFuture<T>> entitiesFutures) {
        SettableFuture<T> listenableFuture = SettableFuture.create();
        CompletableFuture<List<T>> entities =
                CompletableFuture.allOf(entitiesFutures.toArray(new CompletableFuture[entitiesFutures.size()]))
                        .thenApply(v -> entitiesFutures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()));

        entities.whenComplete((tsKvEntities, throwable) -> {
            if (throwable != null) {
                listenableFuture.setException(throwable);
            } else {
                T result = null;
                for (T entity : tsKvEntities) {
                    if (entity.isNotEmpty()) {
                        result = entity;
                        break;
                    }
                }
                listenableFuture.set(result);
            }
        });
        return listenableFuture;
    }
}

