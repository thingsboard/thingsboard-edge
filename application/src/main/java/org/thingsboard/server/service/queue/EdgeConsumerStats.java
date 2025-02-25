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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeNotificationMsg;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EdgeConsumerStats {

    public static final String TOTAL_MSGS = "totalMsgs";
    public static final String EDGE_NOTIFICATIONS = "edgeNfs";
    public static final String TO_CORE_NF_EDGE_EVENT = "coreNfEdgeHPUpd";
    public static final String TO_CORE_NF_EDGE_EVENT_UPDATE = "coreNfEdgeUpd";
    public static final String TO_CORE_NF_EDGE_SYNC_REQUEST = "coreNfEdgeSyncReq";
    public static final String TO_CORE_NF_EDGE_SYNC_RESPONSE = "coreNfEdgeSyncResp";
    public static final String TO_CORE_NF_EDGE_COMPONENT_LIFECYCLE = "coreNfEdgeCompLfcl";

    private final StatsCounter totalCounter;
    private final StatsCounter edgeNotificationsCounter;
    private final StatsCounter edgeHighPriorityCounter;
    private final StatsCounter edgeEventUpdateCounter;
    private final StatsCounter edgeSyncRequestCounter;
    private final StatsCounter edgeSyncResponseCounter;
    private final StatsCounter edgeComponentLifecycle;

    private final List<StatsCounter> counters = new ArrayList<>(7);

    public EdgeConsumerStats(StatsFactory statsFactory) {
        String statsKey = StatsType.EDGE.getName();

        this.totalCounter = register(statsFactory.createStatsCounter(statsKey, TOTAL_MSGS));
        this.edgeNotificationsCounter = register(statsFactory.createStatsCounter(statsKey, EDGE_NOTIFICATIONS));
        this.edgeHighPriorityCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_EVENT));
        this.edgeEventUpdateCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_EVENT_UPDATE));
        this.edgeSyncRequestCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_SYNC_REQUEST));
        this.edgeSyncResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_SYNC_RESPONSE));
        this.edgeComponentLifecycle = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_COMPONENT_LIFECYCLE));
    }

    private StatsCounter register(StatsCounter counter) {
        counters.add(counter);
        return counter;
    }

    public void log(ToEdgeNotificationMsg msg) {
        totalCounter.increment();
        if (msg.hasEdgeHighPriority()) {
            edgeHighPriorityCounter.increment();
        } else if (msg.hasEdgeEventUpdate()) {
            edgeEventUpdateCounter.increment();
        } else if (msg.hasToEdgeSyncRequest()) {
            edgeSyncRequestCounter.increment();
        } else if (msg.hasFromEdgeSyncResponse()) {
            edgeSyncResponseCounter.increment();
        } else if (msg.hasComponentLifecycle()) {
            edgeComponentLifecycle.increment();
        }
    }

    public void log(ToEdgeMsg msg) {
        totalCounter.increment();
        edgeNotificationsCounter.increment();
    }

    public void printStats() {
        int total = totalCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach(counter -> stats.append(counter.getName()).append(" = [").append(counter.get()).append("] "));
            log.info("Edge Stats: {}", stats);
        }
    }

    public void reset() {
        counters.forEach(StatsCounter::clear);
    }

}
