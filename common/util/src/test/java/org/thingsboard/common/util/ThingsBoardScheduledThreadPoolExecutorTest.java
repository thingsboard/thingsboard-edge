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
package org.thingsboard.common.util;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ThingsBoardScheduledThreadPoolExecutorTest {

    ThingsBoardScheduledThreadPoolExecutor scheduler;

    @BeforeEach
    void setup() {
        scheduler = new ThingsBoardScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory());
    }

    @AfterEach
    void cleanup() {
        scheduler.shutdownNow();
    }

    @Test
    @DisplayName("scheduleAtFixedRate() should continue periodic execution even if command throws exception")
    void scheduleAtFixedRateShouldNotStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        AtomicInteger executionCounter = new AtomicInteger(0);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new RuntimeException("Unexpected exception");
            } finally {
                executionCounter.incrementAndGet();
            }
        };

        // WHEN
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(exceptionThrowingCommand, 0, 100, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Wait until command is executed at least twice")
                .atMost(10, TimeUnit.SECONDS)
                .failFast("Future should not be done or cancelled; task should continue running", () -> future.isDone() || future.isCancelled())
                .untilAsserted(() -> assertThat(executionCounter.get())
                        .as("Task should be executed at least twice")
                        .isGreaterThan(2));
    }

    @Test
    @DisplayName("scheduleAtFixedRate() should stop periodic execution if command throws an error")
    void scheduleAtFixedRateShouldStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        AtomicInteger executionCounter = new AtomicInteger(0);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new Error("Unexpected error");
            } finally {
                executionCounter.incrementAndGet();
            }
        };

        // WHEN
        scheduler.scheduleAtFixedRate(exceptionThrowingCommand, 0, 100, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Command that throws an error should execute exactly once")
                .pollDelay(5, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .failFast("Command that throws an error should not execute more than once", () -> executionCounter.get() > 1)
                .until(() -> executionCounter.get() == 1);
    }

    @Test
    @DisplayName("scheduleWithFixedDelay() should continue periodic execution even if command throws exception")
    void scheduleWithFixedDelayShouldNotStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        AtomicInteger executionCounter = new AtomicInteger(0);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new RuntimeException("Unexpected exception");
            } finally {
                executionCounter.incrementAndGet();
            }
        };

        // WHEN
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(exceptionThrowingCommand, 0, 100, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Wait until command is executed at least twice")
                .atMost(10, TimeUnit.SECONDS)
                .failFast("Future should not be done or cancelled; task should continue running", () -> future.isDone() || future.isCancelled())
                .untilAsserted(() -> assertThat(executionCounter.get())
                        .as("Task should be executed at least twice")
                        .isGreaterThan(2));
    }

    @Test
    @DisplayName("scheduleWithFixedDelay() should stop periodic execution if command throws an error")
    void scheduleWithFixedDelayShouldStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        AtomicInteger executionCounter = new AtomicInteger(0);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new Error("Unexpected error");
            } finally {
                executionCounter.incrementAndGet();
            }
        };

        // WHEN
        scheduler.scheduleWithFixedDelay(exceptionThrowingCommand, 0, 100, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Command that throws an error should execute exactly once")
                .pollDelay(5, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .failFast("Command that throws an error should not execute more than once", () -> executionCounter.get() > 1)
                .until(() -> executionCounter.get() == 1);
    }

}
