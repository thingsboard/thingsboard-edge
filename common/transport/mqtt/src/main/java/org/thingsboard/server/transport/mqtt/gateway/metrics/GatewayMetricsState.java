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
package org.thingsboard.server.transport.mqtt.gateway.metrics;

import lombok.Getter;
import org.thingsboard.server.common.msg.gateway.metrics.GatewayMetadata;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GatewayMetricsState {

    private final Map<String, ConnectorMetricsState> connectors;
    private final Lock updateLock;

    @Getter
    private volatile TransportProtos.SessionInfoProto sessionInfo;

    public GatewayMetricsState(TransportProtos.SessionInfoProto sessionInfo) {
        this.connectors = new HashMap<>();
        this.updateLock = new ReentrantLock();
        this.sessionInfo = sessionInfo;
    }

    public void updateSessionInfo(TransportProtos.SessionInfoProto sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    public void update(List<GatewayMetadata> metricsData, long serverReceiveTs) {
        updateLock.lock();
        try {
            metricsData.forEach(data -> {
                connectors.computeIfAbsent(data.connector(), k -> new ConnectorMetricsState()).update(data, serverReceiveTs);
            });
        } finally {
            updateLock.unlock();
        }
    }

    public Map<String, ConnectorMetricsResult> getStateResult() {
        Map<String, ConnectorMetricsResult> result = new HashMap<>();
        updateLock.lock();
        try {
            connectors.forEach((name, state) -> result.put(name, state.getResult()));
            connectors.clear();
        } finally {
            updateLock.unlock();
        }

        return result;
    }

    public boolean isEmpty() {
        return connectors.isEmpty();
    }

    private static class ConnectorMetricsState {
        private final AtomicInteger count;
        private final AtomicLong gwLatencySum;
        private final AtomicLong transportLatencySum;
        private volatile long minGwLatency;
        private volatile long maxGwLatency;
        private volatile long minTransportLatency;
        private volatile long maxTransportLatency;

        private ConnectorMetricsState() {
            this.count = new AtomicInteger(0);
            this.gwLatencySum = new AtomicLong(0);
            this.transportLatencySum = new AtomicLong(0);
        }

        private void update(GatewayMetadata metricsData, long serverReceiveTs) {
            long gwLatency = metricsData.publishedTs() - metricsData.receivedTs();
            long transportLatency = serverReceiveTs - metricsData.publishedTs();
            count.incrementAndGet();
            gwLatencySum.addAndGet(gwLatency);
            transportLatencySum.addAndGet(transportLatency);
            if (minGwLatency == 0 || minGwLatency > gwLatency) {
                minGwLatency = gwLatency;
            }
            if (maxGwLatency < gwLatency) {
                maxGwLatency = gwLatency;
            }
            if (minTransportLatency == 0 || minTransportLatency > transportLatency) {
                minTransportLatency = transportLatency;
            }
            if (maxTransportLatency < transportLatency) {
                maxTransportLatency = transportLatency;
            }
        }

        private ConnectorMetricsResult getResult() {
            long count = this.count.get();
            long avgGwLatency = gwLatencySum.get() / count;
            long avgTransportLatency = transportLatencySum.get() / count;
            return new ConnectorMetricsResult(avgGwLatency, minGwLatency, maxGwLatency, avgTransportLatency, minTransportLatency, maxTransportLatency);
        }
    }

    public record ConnectorMetricsResult(long avgGwLatency, long minGwLatency, long maxGwLatency,
                                         long avgTransportLatency, long minTransportLatency, long maxTransportLatency) {
    }

}
