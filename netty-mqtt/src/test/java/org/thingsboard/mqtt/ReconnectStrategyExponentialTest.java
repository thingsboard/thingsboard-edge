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
package org.thingsboard.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willAnswer;
import static org.thingsboard.mqtt.ReconnectStrategyExponential.EXP_MAX;
import static org.thingsboard.mqtt.ReconnectStrategyExponential.JITTER_MAX;

@Slf4j
class ReconnectStrategyExponentialTest {

    @Execution(ExecutionMode.SAME_THREAD) // just for convenient log reading
    @ParameterizedTest
    @ValueSource(ints = {1, 0, 60})
    public void exponentialReconnectDelayTest(final int reconnectIntervalMinSeconds) {
        final ReconnectStrategyExponential strategy = Mockito.spy(new ReconnectStrategyExponential(reconnectIntervalMinSeconds));
        log.info("=== Reconnect delay test for ReconnectStrategyExponential({}) : calculated min [{}] max [{}] ===", reconnectIntervalMinSeconds, strategy.getReconnectIntervalMinSeconds(), strategy.getReconnectIntervalMaxSeconds());
        final AtomicLong nanoTime = new AtomicLong(System.nanoTime());
        willAnswer((x) -> nanoTime.get()).given(strategy).getNanoTime();
        final LinkedBlockingDeque<Long> jittersCaptured = new LinkedBlockingDeque<>();
        final LinkedBlockingDeque<Long> expCaptured = new LinkedBlockingDeque<>();

        willAnswer(captureResult(jittersCaptured)).given(strategy).calculateJitter();
        willAnswer(captureResult(expCaptured)).given(strategy).calculateExp(anyLong());

        for (int phase = 0; phase < 3; phase++) {
            log.info("== Phase {} ==", phase);
            long previousDelay = 0;
            for (int i = 0; i < EXP_MAX + 4; i++) {
                final long nextReconnectDelay = strategy.getNextReconnectDelay();
                nanoTime.addAndGet(TimeUnit.SECONDS.toNanos(nextReconnectDelay));
                log.info("Retry [{}] Delay [{}] : min [{}] exp [{}] jitter [{}]", strategy.getRetryCount(), nextReconnectDelay, strategy.getReconnectIntervalMinSeconds(), expCaptured.peekLast(), jittersCaptured.peekLast());
                assertThat(previousDelay).satisfiesAnyOf(
                        v -> assertThat(v).isLessThanOrEqualTo(nextReconnectDelay),
                        v -> assertThat(v).isCloseTo(nextReconnectDelay, offset(JITTER_MAX)) // Adjust tolerance as needed
                );
                previousDelay = nextReconnectDelay;
            }
            log.info("Jitters captured: {}", drainAll(jittersCaptured));
            log.info("Exponents captured: {}", drainAll(expCaptured));
            assertThat(previousDelay).isCloseTo(strategy.getReconnectIntervalMaxSeconds(), offset(JITTER_MAX));

            final long coolDownPeriodSec = strategy.getReconnectIntervalMinSeconds() + strategy.getReconnectIntervalMaxSeconds() + 1;
            log.info("Cooling down for [{}] seconds ...", coolDownPeriodSec);
            nanoTime.addAndGet(TimeUnit.SECONDS.toNanos(coolDownPeriodSec));
            assertThat(strategy.isCooledDown(TimeUnit.SECONDS.toNanos(coolDownPeriodSec))).as("cooled down").isTrue();
        }
    }

    private Answer<Long> captureResult(Collection<Long> collection) {
        return invocation -> {
            long result = (long) invocation.callRealMethod();
            collection.add(result);
            return result;
        };
    }

    private Collection<Long> drainAll(BlockingQueue<Long> jittersCaptured) {
        Collection<Long> elements = new ArrayList<>();
        jittersCaptured.drainTo(elements);
        return elements;
    }

}
