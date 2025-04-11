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
package org.thingsboard.server.edqs.stats;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.TbBytePool;
import org.thingsboard.common.util.TbStringPool;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.stats.EdqsStatsService;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsTimer;
import org.thingsboard.server.edqs.repo.DefaultEdqsRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.edqs.api.supported:true}' == 'true' && '${queue.edqs.stats.enabled:true}' == 'true'")
public class DefaultEdqsStatsService implements EdqsStatsService {

    private final StatsFactory statsFactory;

    @Value("${queue.edqs.stats.slow_query_threshold}")
    private int slowQueryThreshold;

    private final ConcurrentMap<ObjectType, AtomicInteger> objectCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StatsTimer> timers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StatsCounter> counters = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        statsFactory.createGauge("edqsMapGauges", "stringPoolSize", TbStringPool.getPool(), Map::size);
        statsFactory.createGauge("edqsMapGauges", "bytePoolSize", TbBytePool.getPool(), Map::size);
        statsFactory.createGauge("edqsMapGauges", "tenantReposSize", DefaultEdqsRepository.getRepos(), Map::size);
    }

    @Override
    public void reportAdded(ObjectType objectType) {
        getObjectGauge(objectType).incrementAndGet();
    }

    @Override
    public void reportRemoved(ObjectType objectType) {
        getObjectGauge(objectType).decrementAndGet();
    }

    @Override
    public void reportEntityDataQuery(TenantId tenantId, EntityDataQuery query, long timingNanos) {
        checkTiming(tenantId, query, timingNanos);
        getTimer("entityDataQueryTimer").record(timingNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void reportEntityCountQuery(TenantId tenantId, EntityCountQuery query, long timingNanos) {
        checkTiming(tenantId, query, timingNanos);
        getTimer("entityCountQueryTimer").record(timingNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void reportEdqsDataQuery(TenantId tenantId, EntityDataQuery query, long timingNanos) {
        checkTiming(tenantId, query, timingNanos);
        getTimer("edqsDataQueryTimer").record(timingNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void reportEdqsCountQuery(TenantId tenantId, EntityCountQuery query, long timingNanos) {
        checkTiming(tenantId, query, timingNanos);
        getTimer("edqsCountQueryTimer").record(timingNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void reportStringCompressed() {
        getCounter("stringsCompressed").increment();
    }

    @Override
    public void reportStringUncompressed() {
        getCounter("stringsUncompressed").increment();
    }

    private void checkTiming(TenantId tenantId, EntityCountQuery query, long timingNanos) {
        double timingMs = timingNanos / 1000_000.0;
        String queryType = query instanceof EntityDataQuery ? "data" : "count";
        if (timingMs < slowQueryThreshold) {
            log.debug("[{}] Executed " + queryType + " query in {} ms: {}", tenantId, timingMs, query);
        } else {
            log.warn("[{}] Executed slow " + queryType + " query in {} ms: {}", tenantId, timingMs, query);
        }
    }

    private StatsTimer getTimer(String name) {
        return timers.computeIfAbsent(name, __ -> statsFactory.createStatsTimer("edqsTimers", name));
    }

    private StatsCounter getCounter(String name) {
        return counters.computeIfAbsent(name, __ -> statsFactory.createStatsCounter("edqsCounters", name));
    }

    private AtomicInteger getObjectGauge(ObjectType objectType) {
        return objectCounters.computeIfAbsent(objectType, type ->
                statsFactory.createGauge("edqsGauges", "objectsCount", new AtomicInteger(), "objectType", type.name()));
    }

}
