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
package org.thingsboard.server.service.queue;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Data
public class TbTenantRuleEngineStats {

    private final UUID tenantId;

    private final AtomicInteger totalMsgCounter = new AtomicInteger(0);
    private final AtomicInteger successMsgCounter = new AtomicInteger(0);
    private final AtomicInteger tmpTimeoutMsgCounter = new AtomicInteger(0);
    private final AtomicInteger tmpFailedMsgCounter = new AtomicInteger(0);

    private final AtomicInteger timeoutMsgCounter = new AtomicInteger(0);
    private final AtomicInteger failedMsgCounter = new AtomicInteger(0);

    private final Map<String, AtomicInteger> counters = new HashMap<>();

    public TbTenantRuleEngineStats(UUID tenantId) {
        this.tenantId = tenantId;
        counters.put(TbRuleEngineConsumerStats.TOTAL_MSGS, totalMsgCounter);
        counters.put(TbRuleEngineConsumerStats.SUCCESSFUL_MSGS, successMsgCounter);
        counters.put(TbRuleEngineConsumerStats.TIMEOUT_MSGS, timeoutMsgCounter);
        counters.put(TbRuleEngineConsumerStats.FAILED_MSGS, failedMsgCounter);

        counters.put(TbRuleEngineConsumerStats.TMP_TIMEOUT, tmpTimeoutMsgCounter);
        counters.put(TbRuleEngineConsumerStats.TMP_FAILED, tmpFailedMsgCounter);
    }

    public void logSuccess() {
        totalMsgCounter.incrementAndGet();
        successMsgCounter.incrementAndGet();
    }

    public void logFailed() {
        totalMsgCounter.incrementAndGet();
        failedMsgCounter.incrementAndGet();
    }

    public void logTimeout() {
        totalMsgCounter.incrementAndGet();
        timeoutMsgCounter.incrementAndGet();
    }

    public void logTmpFailed() {
        totalMsgCounter.incrementAndGet();
        tmpFailedMsgCounter.incrementAndGet();
    }

    public void logTmpTimeout() {
        totalMsgCounter.incrementAndGet();
        tmpTimeoutMsgCounter.incrementAndGet();
    }

    public void printStats() {
        int total = totalMsgCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach((label, value) -> {
                stats.append(label).append(" = [").append(value.get()).append("]");
            });
            log.info("[{}] Stats: {}", tenantId, stats);
        }
    }

    public void reset() {
        counters.values().forEach(counter -> counter.set(0));
    }
}
