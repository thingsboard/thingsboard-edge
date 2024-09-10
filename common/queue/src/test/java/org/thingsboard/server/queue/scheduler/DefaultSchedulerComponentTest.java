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
package org.thingsboard.server.queue.scheduler;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSchedulerComponentTest {

    DefaultSchedulerComponent schedulerComponent;

    @BeforeEach
    void setup() {
        schedulerComponent = new DefaultSchedulerComponent();
        schedulerComponent.init();
    }

    @AfterEach
    void cleanup() {
        schedulerComponent.destroy();
    }

    @Test
    @DisplayName("scheduleAtFixedRate() should continue periodic execution even if command throws exception")
    void scheduleAtFixedRateShouldNotStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        var wasExecutedAtLeastOnce = new AtomicBoolean(false);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new RuntimeException("Unexpected exception");
            } finally {
                wasExecutedAtLeastOnce.set(true);
            }
        };

        // WHEN
        ScheduledFuture<?> future = schedulerComponent.scheduleAtFixedRate(exceptionThrowingCommand, 0, 200, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Wait until command is executed at least once")
                .atMost(5, TimeUnit.SECONDS)
                .until(wasExecutedAtLeastOnce::get);

        assertThat(future.isDone()).as("Periodic execution should not stop after unhandled exception is thrown by the command").isFalse();
    }

    @Test
    @DisplayName("scheduleWithFixedDelay() should continue periodic execution even if command throws exception")
    void scheduleWithFixedDelayShouldNotStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        var wasExecutedAtLeastOnce = new AtomicBoolean(false);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new RuntimeException("Unexpected exception");
            } finally {
                wasExecutedAtLeastOnce.set(true);
            }
        };

        // WHEN
        ScheduledFuture<?> future = schedulerComponent.scheduleWithFixedDelay(exceptionThrowingCommand, 0, 200, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Wait until command is executed at least once")
                .atMost(5, TimeUnit.SECONDS)
                .until(wasExecutedAtLeastOnce::get);

        assertThat(future.isDone()).as("Periodic execution should not stop after unhandled exception is thrown by the command").isFalse();
    }

}
