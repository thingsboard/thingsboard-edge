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
package org.thingsboard.server.dao.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Getter
public class BufferedRateExecutorStats {
    private static final String TENANT_ID_TAG = "tenantId";


    private static final String TOTAL_ADDED = "totalAdded";
    private static final String TOTAL_LAUNCHED = "totalLaunched";
    private static final String TOTAL_RELEASED = "totalReleased";
    private static final String TOTAL_FAILED = "totalFailed";
    private static final String TOTAL_EXPIRED = "totalExpired";
    private static final String TOTAL_REJECTED = "totalRejected";
    private static final String TOTAL_RATE_LIMITED = "totalRateLimited";

    private final StatsFactory statsFactory;

    private final ConcurrentMap<TenantId, DefaultCounter> rateLimitedTenants = new ConcurrentHashMap<>();

    private final List<StatsCounter> statsCounters = new ArrayList<>();

    private final StatsCounter totalAdded;
    private final StatsCounter totalLaunched;
    private final StatsCounter totalReleased;
    private final StatsCounter totalFailed;
    private final StatsCounter totalExpired;
    private final StatsCounter totalRejected;
    private final StatsCounter totalRateLimited;

    public BufferedRateExecutorStats(StatsFactory statsFactory) {
        this.statsFactory = statsFactory;

        String key = StatsType.RATE_EXECUTOR.getName();

        this.totalAdded = statsFactory.createStatsCounter(key, TOTAL_ADDED);
        this.totalLaunched = statsFactory.createStatsCounter(key, TOTAL_LAUNCHED);
        this.totalReleased = statsFactory.createStatsCounter(key, TOTAL_RELEASED);
        this.totalFailed = statsFactory.createStatsCounter(key, TOTAL_FAILED);
        this.totalExpired = statsFactory.createStatsCounter(key, TOTAL_EXPIRED);
        this.totalRejected = statsFactory.createStatsCounter(key, TOTAL_REJECTED);
        this.totalRateLimited = statsFactory.createStatsCounter(key, TOTAL_RATE_LIMITED);

        this.statsCounters.add(totalAdded);
        this.statsCounters.add(totalLaunched);
        this.statsCounters.add(totalReleased);
        this.statsCounters.add(totalFailed);
        this.statsCounters.add(totalExpired);
        this.statsCounters.add(totalRejected);
        this.statsCounters.add(totalRateLimited);
    }

    public void incrementRateLimitedTenant(TenantId tenantId){
        rateLimitedTenants.computeIfAbsent(tenantId,
                tId -> {
                    String key = StatsType.RATE_EXECUTOR.getName() + ".tenant";
                    return statsFactory.createDefaultCounter(key, TENANT_ID_TAG, tId.toString());
                }
        )
                .increment();
    }
}
