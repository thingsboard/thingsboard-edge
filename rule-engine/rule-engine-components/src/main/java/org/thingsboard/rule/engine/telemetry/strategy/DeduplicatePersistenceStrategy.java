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
package org.thingsboard.rule.engine.telemetry.strategy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

final class DeduplicatePersistenceStrategy implements PersistenceStrategy {

    private static final int MIN_DEDUPLICATION_INTERVAL_SECS = 1;
    private static final int MAX_DEDUPLICATION_INTERVAL_SECS = (int) Duration.ofDays(1L).toSeconds();

    private static final long MIN_INTERVAL_EXPIRY_MILLIS = Duration.ofMinutes(10L).toMillis();
    private static final int INTERVAL_EXPIRY_FACTOR = 10;
    private static final long MAX_INTERVAL_EXPIRY_MILLIS = Duration.ofDays(2L).toMillis();

    private static final int MAX_TOTAL_INTERVALS_DURATION_SECS = (int) Duration.ofDays(2L).toSeconds();
    private static final int MAX_NUMBER_OF_INTERVALS = 100;

    private final long deduplicationIntervalMillis;
    private final LoadingCache<Long, Set<UUID>> deduplicationCache;

    @JsonCreator
    public DeduplicatePersistenceStrategy(@JsonProperty("deduplicationIntervalSecs") int deduplicationIntervalSecs) {
        if (deduplicationIntervalSecs < MIN_DEDUPLICATION_INTERVAL_SECS || deduplicationIntervalSecs > MAX_DEDUPLICATION_INTERVAL_SECS) {
            throw new IllegalArgumentException("Deduplication interval must be at least " + MIN_DEDUPLICATION_INTERVAL_SECS + " second(s) " +
                    "and at most " + MAX_DEDUPLICATION_INTERVAL_SECS + " second(s), was " + deduplicationIntervalSecs + " second(s)");
        }
        deduplicationIntervalMillis = Duration.ofSeconds(deduplicationIntervalSecs).toMillis();
        deduplicationCache = Caffeine.newBuilder()
                .softValues()
                .expireAfterAccess(calculateExpireAfterAccess(deduplicationIntervalSecs))
                .maximumSize(calculateMaxNumberOfDeduplicationIntervals(deduplicationIntervalSecs))
                .build(__ -> Sets.newConcurrentHashSet());
    }

    /**
     * Calculates the expire-after-access duration. By default, we keep each deduplication interval
     * alive for 10 “iterations” (interval duration × 10). However, we never let this drop below
     * 10 minutes to ensure adequate retention for small intervals, nor exceed 48 hours to prevent
     * storing stale data in memory.
     */
    private static Duration calculateExpireAfterAccess(int deduplicationIntervalSecs) {
        long desiredExpiryMillis = Duration.ofSeconds(deduplicationIntervalSecs).toMillis() * INTERVAL_EXPIRY_FACTOR;
        return Duration.ofMillis(Longs.constrainToRange(desiredExpiryMillis, MIN_INTERVAL_EXPIRY_MILLIS, MAX_INTERVAL_EXPIRY_MILLIS));
    }

    /**
     * Calculates the maximum number of deduplication intervals we will store in the cache.
     * We limit retention to two days to avoid stale data and cap it at 100 intervals to manage memory usage.
     */
    private static long calculateMaxNumberOfDeduplicationIntervals(int deduplicationIntervalSecs) {
        int numberOfDeduplicationIntervals = MAX_TOTAL_INTERVALS_DURATION_SECS / deduplicationIntervalSecs;
        return Math.min(numberOfDeduplicationIntervals, MAX_NUMBER_OF_INTERVALS);
    }

    @JsonProperty("deduplicationIntervalSecs")
    public long getDeduplicationIntervalSecs() {
        return Duration.ofMillis(deduplicationIntervalMillis).toSeconds();
    }

    @Override
    public boolean shouldPersist(long ts, UUID originatorUuid) {
        long intervalNumber = ts / deduplicationIntervalMillis;
        return deduplicationCache.get(intervalNumber).add(originatorUuid);
    }

}
