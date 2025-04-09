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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.stats.EdqsStatsService;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsTimer;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.queue.edqs.EdqsComponent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@EdqsComponent
@Service
@Slf4j
@ConditionalOnProperty(name = "queue.edqs.stats.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultEdqsStatsService implements EdqsStatsService {

    private final StatsFactory statsFactory;

    @Value("${queue.edqs.stats.slow_query_threshold:3000}")
    private int slowQueryThreshold;

    private final ConcurrentHashMap<ObjectType, AtomicInteger> objectCounters = new ConcurrentHashMap<>();
    private final StatsTimer dataQueryTimer;
    private final StatsTimer countQueryTimer;

    private DefaultEdqsStatsService(StatsFactory statsFactory) {
        this.statsFactory = statsFactory;
        dataQueryTimer = statsFactory.createTimer(StatsType.EDQS, "entityDataQueryTimer");
        countQueryTimer = statsFactory.createTimer(StatsType.EDQS, "entityCountQueryTimer");
    }

    @Override
    public void reportAdded(ObjectType objectType) {
        getObjectCounter(objectType).incrementAndGet();
    }

    @Override
    public void reportRemoved(ObjectType objectType) {
        getObjectCounter(objectType).decrementAndGet();
    }

    @Override
    public void reportDataQuery(TenantId tenantId, EntityDataQuery query, long timingNanos) {
        double timingMs = timingNanos / 1000_000.0;
        if (timingMs < slowQueryThreshold) {
            log.debug("[{}] Executed data query in {} ms: {}", tenantId, timingMs, query);
        } else {
            log.warn("[{}] Executed slow data query in {} ms: {}", tenantId, timingMs, query);
        }
        dataQueryTimer.record(timingNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void reportCountQuery(TenantId tenantId, EntityCountQuery query, long timingNanos) {
        double timingMs = timingNanos / 1000_000.0;
        if (timingMs < slowQueryThreshold) {
            log.debug("[{}] Executed count query in {} ms: {}", tenantId, timingMs, query);
        } else {
            log.warn("[{}] Executed slow count query in {} ms: {}", tenantId, timingMs, query);
        }
        countQueryTimer.record(timingNanos, TimeUnit.NANOSECONDS);
    }

    private AtomicInteger getObjectCounter(ObjectType objectType) {
        return objectCounters.computeIfAbsent(objectType, type ->
                statsFactory.createGauge("edqsObjectsCount", new AtomicInteger(), "objectType", type.name()));
    }

}
