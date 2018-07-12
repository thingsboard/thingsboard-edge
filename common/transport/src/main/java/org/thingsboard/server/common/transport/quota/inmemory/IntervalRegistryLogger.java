/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.common.transport.quota.inmemory;

import com.google.common.collect.MinMaxPriorityQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
@Slf4j
public abstract class IntervalRegistryLogger {

    private final int topSize;
    private final KeyBasedIntervalRegistry intervalRegistry;
    private final long logIntervalMin;
    private ScheduledExecutorService executor;

    public IntervalRegistryLogger(int topSize, long logIntervalMin, KeyBasedIntervalRegistry intervalRegistry) {
        this.topSize = topSize;
        this.logIntervalMin = logIntervalMin;
        this.intervalRegistry = intervalRegistry;
    }

    public void schedule() {
        if (executor != null) {
            throw new IllegalStateException("Registry Cleaner already scheduled");
        }
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::logStatistic, logIntervalMin, logIntervalMin, TimeUnit.MINUTES);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void logStatistic() {
        Map<String, Long> registryContent = intervalRegistry.getContent();
        int uniqHosts = registryContent.size();
        long requestsCount = registryContent.values().stream().mapToLong(i -> i).sum();
        Map<String, Long> top = getTopElements(registryContent);
        log(top, uniqHosts, requestsCount);
    }

    protected Map<String, Long> getTopElements(Map<String, Long> countMap) {
        MinMaxPriorityQueue<Map.Entry<String, Long>> topQueue = MinMaxPriorityQueue
                .orderedBy(Comparator.comparing((Function<Map.Entry<String, Long>, Long>) Map.Entry::getValue).reversed())
                .maximumSize(topSize)
                .create(countMap.entrySet());

        return topQueue.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected abstract void log(Map<String, Long> top, int uniqHosts, long requestsCount);
}
