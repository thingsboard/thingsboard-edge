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
package org.thingsboard.server.dao.aspect;

import lombok.Data;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Data
public class DbCallStats {

    private final TenantId tenantId;
    private final ConcurrentMap<String, MethodCallStats> methodStats = new ConcurrentHashMap<>();
    private final AtomicInteger successCalls = new AtomicInteger();
    private final AtomicInteger failureCalls = new AtomicInteger();

    public void onMethodCall(String methodName, boolean success, long executionTime) {
        var methodCallStats = methodStats.computeIfAbsent(methodName, m -> new MethodCallStats());
        methodCallStats.getExecutions().incrementAndGet();
        methodCallStats.getTiming().addAndGet(executionTime);
        if (success) {
            successCalls.incrementAndGet();
        } else {
            failureCalls.incrementAndGet();
            methodCallStats.getFailures().incrementAndGet();
        }
    }

    public DbCallStatsSnapshot snapshot() {
        return DbCallStatsSnapshot.builder()
                .tenantId(tenantId)
                .totalSuccess(successCalls.get())
                .totalFailure(failureCalls.get())
                .methodStats(methodStats.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().snapshot())))
                .totalTiming(methodStats.values().stream().map(MethodCallStats::getTiming).map(AtomicLong::get).reduce(0L, Long::sum))
                .build();
    }

}
