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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class ReconnectStrategyExponential implements ReconnectStrategy {

    public static final int DEFAULT_RECONNECT_INTERVAL_SEC = 10;
    public static final int MAX_RECONNECT_INTERVAL_SEC = 60;
    public static final int EXP_MAX = 8;
    public static final long JITTER_MAX = 1;
    private final long reconnectIntervalMinSeconds;
    private final long reconnectIntervalMaxSeconds;
    private long lastDisconnectNanoTime = 0; //isotonic time
    private long retryCount = 0;

    public ReconnectStrategyExponential(long reconnectIntervalMinSeconds) {
        this.reconnectIntervalMaxSeconds = calculateIntervalMax(reconnectIntervalMinSeconds);
        this.reconnectIntervalMinSeconds = calculateIntervalMin(reconnectIntervalMinSeconds);
    }

    long calculateIntervalMax(long reconnectIntervalMinSeconds) {
        return reconnectIntervalMinSeconds > MAX_RECONNECT_INTERVAL_SEC ? reconnectIntervalMinSeconds : MAX_RECONNECT_INTERVAL_SEC;
    }

    long calculateIntervalMin(long reconnectIntervalMinSeconds) {
        return Math.min((reconnectIntervalMinSeconds > 0 ? reconnectIntervalMinSeconds : DEFAULT_RECONNECT_INTERVAL_SEC), this.reconnectIntervalMaxSeconds);
    }

    @Override
    synchronized public long getNextReconnectDelay() {
        final long currentNanoTime = getNanoTime();
        final long coolDownSpentNanos = currentNanoTime - lastDisconnectNanoTime;
        lastDisconnectNanoTime = currentNanoTime;
        if (isCooledDown(coolDownSpentNanos)) {
            retryCount = 0;
            return reconnectIntervalMinSeconds;
        }
        return calculateNextReconnectDelay() + calculateJitter();
    }

    long calculateJitter() {
        return ThreadLocalRandom.current().nextInt() >= 0 ? JITTER_MAX : 0;
    }

    long calculateNextReconnectDelay() {
        return Math.min(reconnectIntervalMaxSeconds, reconnectIntervalMinSeconds + calculateExp(retryCount++));
    }

    long calculateExp(long e) {
        return 1L << Math.min(e, EXP_MAX);
    }

    boolean isCooledDown(long coolDownSpentNanos) {
        return TimeUnit.NANOSECONDS.toSeconds(coolDownSpentNanos) > reconnectIntervalMaxSeconds + reconnectIntervalMinSeconds;
    }

    long getNanoTime() {
        return System.nanoTime();
    }

}
