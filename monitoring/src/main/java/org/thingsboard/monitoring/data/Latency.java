/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.monitoring.data;

import com.google.common.util.concurrent.AtomicDouble;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class Latency {

    private final String key;
    private final AtomicDouble latencySum = new AtomicDouble();
    private final AtomicInteger counter = new AtomicInteger();

    public synchronized void report(double latencyInMs) {
        latencySum.addAndGet(latencyInMs);
        counter.incrementAndGet();
    }

    public synchronized double getAvg() {
        return latencySum.get() / counter.get();
    }

    public boolean isNotEmpty() {
        return counter.get() > 0;
    }

    public synchronized void reset() {
        latencySum.set(0.0);
        counter.set(0);
    }

    public String getKey() {
        return key;
    }

    public synchronized Latency snapshot() {
        Latency snapshot = new Latency(key);
        snapshot.latencySum.set(latencySum.get());
        snapshot.counter.set(counter.get());
        return snapshot;
    }

    @Override
    public String toString() {
        return "Latency{" +
                "key='" + key + '\'' +
                ", avgLatency=" + getAvg() +
                '}';
    }

}
