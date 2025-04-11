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
package org.thingsboard.server.service.housekeeper.stats;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsTimer;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnProperty(name = "queue.core.housekeeper.stats.enabled", havingValue = "true", matchIfMissing = true)
public class HousekeeperStatsService {

    private final Map<HousekeeperTaskType, HousekeeperStats> stats = new EnumMap<>(HousekeeperTaskType.class);

    public HousekeeperStatsService(StatsFactory statsFactory) {
        for (HousekeeperTaskType taskType : HousekeeperTaskType.values()) {
            stats.put(taskType, new HousekeeperStats(taskType, statsFactory));
        }
    }

    @Scheduled(initialDelayString = "${queue.core.housekeeper.stats.print-interval-ms:60000}",
            fixedDelayString = "${queue.core.housekeeper.stats.print-interval-ms:60000}")
    private void reportStats() {
        String statsStr = stats.values().stream().map(stats -> {
            String countersStr = stats.getCounters().stream()
                    .filter(counter -> counter.get() > 0)
                    .map(counter -> counter.getName() + " = [" + counter.get() + "]")
                    .collect(Collectors.joining(" "));
            if (countersStr.isEmpty()) {
                return null;
            } else {
                return stats.getTaskType() + " " + countersStr + " avgProcessingTime [" + stats.getProcessingTimer().getAvg() + " ms]";
            }
        }).filter(Objects::nonNull).collect(Collectors.joining(", "));

        if (!statsStr.isEmpty()) {
            stats.values().forEach(HousekeeperStats::reset);
            log.info("Housekeeper stats: {}", statsStr);
        }
    }

    public void reportProcessed(HousekeeperTaskType taskType, ToHousekeeperServiceMsg msg, long timing) {
        HousekeeperStats stats = this.stats.get(taskType);
        if (msg.getTask().getErrorsCount() == 0) {
            stats.getProcessedCounter().increment();
        } else {
            stats.getReprocessedCounter().increment();
        }
        stats.getProcessingTimer().record(timing);
    }

    public void reportFailure(HousekeeperTaskType taskType, ToHousekeeperServiceMsg msg) {
        HousekeeperStats stats = this.stats.get(taskType);
        if (msg.getTask().getErrorsCount() == 0) {
            stats.getFailedProcessingCounter().increment();
        } else {
            stats.getFailedReprocessingCounter().increment();
        }
    }

    @Getter
    static class HousekeeperStats {
        private final HousekeeperTaskType taskType;
        private final List<StatsCounter> counters = new ArrayList<>();

        private final StatsCounter processedCounter;
        private final StatsCounter failedProcessingCounter;
        private final StatsCounter reprocessedCounter;
        private final StatsCounter failedReprocessingCounter;

        private final StatsTimer processingTimer;

        public HousekeeperStats(HousekeeperTaskType taskType, StatsFactory statsFactory) {
            this.taskType = taskType;
            this.processedCounter = register("processed", statsFactory);
            this.failedProcessingCounter = register("failedProcessing", statsFactory);
            this.reprocessedCounter = register("reprocessed", statsFactory);
            this.failedReprocessingCounter = register("failedReprocessing", statsFactory);
            this.processingTimer = statsFactory.createStatsTimer(StatsType.HOUSEKEEPER.getName(), "processingTime", "taskType", taskType.name());
        }

        private StatsCounter register(String statsName, StatsFactory statsFactory) {
            StatsCounter counter = statsFactory.createStatsCounter(StatsType.HOUSEKEEPER.getName(), statsName, "taskType", taskType.name());
            counters.add(counter);
            return counter;
        }

        public void reset() {
            counters.forEach(DefaultCounter::clear);
            processingTimer.reset();
        }

    }

}
