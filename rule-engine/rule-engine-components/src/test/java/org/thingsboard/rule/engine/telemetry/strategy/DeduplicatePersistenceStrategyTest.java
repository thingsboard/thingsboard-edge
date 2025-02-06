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

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeduplicatePersistenceStrategyTest {

    final int deduplicationIntervalSecs = 10;

    DeduplicatePersistenceStrategy strategy;

    @BeforeEach
    void setup() {
        strategy = new DeduplicatePersistenceStrategy(deduplicationIntervalSecs);
    }

    @Test
    void shouldThrowWhenDeduplicationIntervalIsLessThanOneSecond() {
        assertThatThrownBy(() -> new DeduplicatePersistenceStrategy(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deduplication interval must be at least 1 second(s) and at most 86400 second(s), was 0 second(s)");
    }

    @Test
    void shouldThrowWhenDeduplicationIntervalIsMoreThan24Hours() {
        assertThatThrownBy(() -> new DeduplicatePersistenceStrategy(86401))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deduplication interval must be at least 1 second(s) and at most 86400 second(s), was 86401 second(s)");
    }

    @Test
    void shouldUseAtLeastTenMinutesForExpireAfterAccess() {
        // GIVEN
        int deduplicationIntervalSecs = 1; // min deduplication interval duration

        // WHEN
        strategy = new DeduplicatePersistenceStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().expireAfterAccess())
                .isPresent()
                .map(Policy.FixedExpiration::getExpiresAfter)
                .hasValue(Duration.ofMinutes(10L));
    }

    @Test
    void shouldCalculateExpireAfterAccessAsIntervalDurationMultipliedByTen() {
        // GIVEN
        int deduplicationIntervalSecs = (int) Duration.ofHours(1L).toSeconds(); // max deduplication interval duration

        // WHEN
        strategy = new DeduplicatePersistenceStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().expireAfterAccess())
                .isPresent()
                .map(Policy.FixedExpiration::getExpiresAfter)
                .hasValue(Duration.ofHours(10L));
    }

    @Test
    void shouldUseAtMostTwoDaysForExpireAfterAccess() {
        // GIVEN
        int deduplicationIntervalSecs = (int) Duration.ofDays(1L).toSeconds(); // max deduplication interval duration

        // WHEN
        strategy = new DeduplicatePersistenceStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().expireAfterAccess())
                .isPresent()
                .map(Policy.FixedExpiration::getExpiresAfter)
                .hasValue(Duration.ofDays(2L));
    }

    @Test
    void shouldNotAllowMoreThan100DeduplicationIntervals() {
        // GIVEN
        int deduplicationIntervalSecs = 1; // min deduplication interval duration

        // WHEN
        strategy = new DeduplicatePersistenceStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().eviction())
                .isPresent()
                .map(Policy.Eviction::getMaximum)
                .hasValue(100L);
    }

    @Test
    void shouldCalculateMaxIntervalsAsTwoDaysDividedByIntervalDuration() {
        // GIVEN
        int deduplicationIntervalSecs = (int) Duration.ofHours(1L).toSeconds();

        // WHEN
        strategy = new DeduplicatePersistenceStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().eviction())
                .isPresent()
                .map(Policy.Eviction::getMaximum)
                .hasValue(48L);
    }

    @Test
    void shouldKeepAtLeastTwoDeduplicationIntervals() {
        // GIVEN
        int deduplicationIntervalSecs = (int) Duration.ofDays(1L).toSeconds(); // max deduplication interval duration

        // WHEN
        strategy = new DeduplicatePersistenceStrategy(deduplicationIntervalSecs);

        // THEN
        var deduplicationCache = (LoadingCache<Long, Set<UUID>>) ReflectionTestUtils.getField(strategy, "deduplicationCache");

        assertThat(deduplicationCache.policy().eviction())
                .isPresent()
                .map(Policy.Eviction::getMaximum)
                .hasValue(2L);
    }

    @Test
    void shouldReturnTrueForFirstCallInInterval() {
        long ts = 1_000_000L;
        UUID originator = UUID.randomUUID();

        assertThat(strategy.shouldPersist(ts, originator)).isTrue();
    }

    @Test
    void shouldReturnFalseForSubsequentCallsInInterval() {
        long baseTs = 1_000_000L;
        UUID originator = UUID.randomUUID();

        // Initial call should return true
        assertThat(strategy.shouldPersist(baseTs, originator)).isTrue();

        // Subsequent call within the same interval should return false for the same originator
        long withinSameIntervalTs = baseTs + 1000L;
        assertThat(strategy.shouldPersist(withinSameIntervalTs, originator)).isFalse();
    }

    @Test
    void shouldHandleMultipleOriginatorsIndependently() {
        long baseTs = 1_000_000L;
        UUID originator1 = UUID.randomUUID();
        UUID originator2 = UUID.randomUUID();

        // First call for different originators in the same interval should return true independently
        assertThat(strategy.shouldPersist(baseTs, originator1)).isTrue();
        assertThat(strategy.shouldPersist(baseTs, originator2)).isTrue();

        // Subsequent calls for the same originators within the same interval should return false
        assertThat(strategy.shouldPersist(baseTs + 500L, originator1)).isFalse();
        assertThat(strategy.shouldPersist(baseTs + 500L, originator2)).isFalse();
    }

    @Test
    void shouldHandleEdgeCaseTimestamps() {
        long minTs = Long.MIN_VALUE;
        long maxTs = Long.MAX_VALUE;
        UUID originator = UUID.randomUUID();

        assertThat(strategy.shouldPersist(minTs, originator)).isTrue();
        assertThat(strategy.shouldPersist(minTs + 1L, originator)).isFalse();

        assertThat(strategy.shouldPersist(maxTs, originator)).isTrue();
        assertThat(strategy.shouldPersist(maxTs - 1L, originator)).isFalse();
    }

    @Test
    void shouldResetDeduplicationAtIntervalBoundaries() {
        UUID originator = UUID.randomUUID();

        // check 1st interval
        long firstIntervalStart = 0L;
        long firstIntervalEnd = firstIntervalStart + Duration.ofSeconds(deduplicationIntervalSecs).toMillis() - 1L;
        long firstIntervalMiddle = calculateMiddle(firstIntervalStart, firstIntervalEnd);

        assertThat(strategy.shouldPersist(firstIntervalStart, originator)).isTrue();
        assertThat(strategy.shouldPersist(firstIntervalStart + 1, originator)).isFalse();
        assertThat(strategy.shouldPersist(firstIntervalMiddle, originator)).isFalse();
        assertThat(strategy.shouldPersist(firstIntervalEnd - 1, originator)).isFalse();
        assertThat(strategy.shouldPersist(firstIntervalEnd, originator)).isFalse();

        // check 2nd interval
        long secondIntervalStart = firstIntervalEnd + 1L;
        long secondIntervalEnd = secondIntervalStart + Duration.ofSeconds(deduplicationIntervalSecs).toMillis() - 1L;
        long secondIntervalMiddle = calculateMiddle(secondIntervalStart, secondIntervalEnd);

        assertThat(strategy.shouldPersist(secondIntervalStart, originator)).isTrue();
        assertThat(strategy.shouldPersist(secondIntervalStart + 1, originator)).isFalse();
        assertThat(strategy.shouldPersist(secondIntervalMiddle, originator)).isFalse();
        assertThat(strategy.shouldPersist(secondIntervalEnd - 1, originator)).isFalse();
        assertThat(strategy.shouldPersist(secondIntervalEnd, originator)).isFalse();
    }

    @Test
    void shouldHandleMultipleOriginatorsOverMultipleIntervals() {
        UUID originator1 = UUID.randomUUID();
        UUID originator2 = UUID.randomUUID();
        long baseTs = 0L;

        // First interval for both originators
        assertThat(strategy.shouldPersist(baseTs, originator1)).isTrue();
        assertThat(strategy.shouldPersist(baseTs, originator2)).isTrue();

        // Move to the next interval
        long nextIntervalTs = baseTs + Duration.ofSeconds(10).toMillis();

        // Each originator should be allowed again in the new interval
        assertThat(strategy.shouldPersist(nextIntervalTs, originator1)).isTrue();
        assertThat(strategy.shouldPersist(nextIntervalTs, originator2)).isTrue();

        // Subsequent calls in the same new interval should return false
        assertThat(strategy.shouldPersist(nextIntervalTs + 500L, originator1)).isFalse();
        assertThat(strategy.shouldPersist(nextIntervalTs + 500L, originator2)).isFalse();
    }

    private static long calculateMiddle(long start, long end) {
        return start + (end - start) / 2;
    }

}
