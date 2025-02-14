/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.queue.edqs.EdqsComponent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@EdqsComponent
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "queue.edqs.stats.enabled", havingValue = "true", matchIfMissing = true)
public class EdqsStatsService {

    private final ConcurrentHashMap<TenantId, EdqsStats> statsMap = new ConcurrentHashMap<>();
    private final StatsFactory statsFactory;

    @Scheduled(initialDelayString = "${queue.edqs.stats.print-interval-ms:60000}",
            fixedDelayString = "${queue.edqs.stats.print-interval-ms:60000}")
    private void reportStats() {
        String values = statsMap.entrySet().stream()
                .map(kv -> "TenantId [" + kv.getKey() + "] stats [" + kv.getValue() + "]")
                .collect(Collectors.joining(System.lineSeparator()));
        log.info("EDQS Stats: {}", values);
    }

    public void reportEvent(TenantId tenantId, ObjectType objectType, EdqsEventType eventType) {
        statsMap.computeIfAbsent(tenantId, id -> new EdqsStats(tenantId, statsFactory))
                .reportEvent(objectType, eventType);
    }

    @Getter
    @AllArgsConstructor
    static class EdqsStats {

        private final TenantId tenantId;
        private final ConcurrentHashMap<ObjectType, AtomicInteger> entityCounters = new ConcurrentHashMap<>();
        private final StatsFactory statsFactory;

        private AtomicInteger getOrCreateObjectCounter(ObjectType objectType) {
            return entityCounters.computeIfAbsent(objectType,
                    type -> statsFactory.createGauge(StatsType.EDQS.getName() + "_object_count", new AtomicInteger(),
                            "tenantId", tenantId.toString(), "objectType", type.name()));
        }

        @Override
        public String toString() {
            return entityCounters.entrySet().stream()
                    .map(counters -> counters.getKey().name()+ " total = [" + counters.getValue() + "]")
                    .collect(Collectors.joining(", "));
        }

        public void reportEvent(ObjectType objectType, EdqsEventType eventType) {
            AtomicInteger objectCounter = getOrCreateObjectCounter(objectType);
            if (eventType == EdqsEventType.UPDATED){
                objectCounter.incrementAndGet();
            } else if (eventType == EdqsEventType.DELETED) {
                objectCounter.decrementAndGet();
            }
        }
    }

}
