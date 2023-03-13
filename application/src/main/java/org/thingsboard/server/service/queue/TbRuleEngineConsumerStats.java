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

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class TbRuleEngineConsumerStats {

    public static final String TOTAL_MSGS = "totalMsgs";
    public static final String SUCCESSFUL_MSGS = "successfulMsgs";
    public static final String TMP_TIMEOUT = "tmpTimeout";
    public static final String TMP_FAILED = "tmpFailed";
    public static final String TIMEOUT_MSGS = "timeoutMsgs";
    public static final String FAILED_MSGS = "failedMsgs";
    public static final String SUCCESSFUL_ITERATIONS = "successfulIterations";
    public static final String FAILED_ITERATIONS = "failedIterations";

    private final StatsFactory statsFactory;

    private final StatsCounter totalMsgCounter;
    private final StatsCounter successMsgCounter;
    private final StatsCounter tmpTimeoutMsgCounter;
    private final StatsCounter tmpFailedMsgCounter;

    private final StatsCounter timeoutMsgCounter;
    private final StatsCounter failedMsgCounter;

    private final StatsCounter successIterationsCounter;
    private final StatsCounter failedIterationsCounter;

    private final List<StatsCounter> counters = new ArrayList<>();
    private final ConcurrentMap<UUID, TbTenantRuleEngineStats> tenantStats = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, Timer> tenantMsgProcessTimers = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, RuleEngineException> tenantExceptions = new ConcurrentHashMap<>();

    private final String queueName;

    public TbRuleEngineConsumerStats(String queueName, StatsFactory statsFactory) {
        this.queueName = queueName;
        this.statsFactory = statsFactory;

        String statsKey = StatsType.RULE_ENGINE.getName() + "." + queueName;
        this.totalMsgCounter = statsFactory.createStatsCounter(statsKey, TOTAL_MSGS);
        this.successMsgCounter = statsFactory.createStatsCounter(statsKey, SUCCESSFUL_MSGS);
        this.timeoutMsgCounter = statsFactory.createStatsCounter(statsKey, TIMEOUT_MSGS);
        this.failedMsgCounter = statsFactory.createStatsCounter(statsKey, FAILED_MSGS);
        this.tmpTimeoutMsgCounter = statsFactory.createStatsCounter(statsKey, TMP_TIMEOUT);
        this.tmpFailedMsgCounter = statsFactory.createStatsCounter(statsKey, TMP_FAILED);
        this.successIterationsCounter = statsFactory.createStatsCounter(statsKey, SUCCESSFUL_ITERATIONS);
        this.failedIterationsCounter = statsFactory.createStatsCounter(statsKey, FAILED_ITERATIONS);

        counters.add(totalMsgCounter);
        counters.add(successMsgCounter);
        counters.add(timeoutMsgCounter);
        counters.add(failedMsgCounter);

        counters.add(tmpTimeoutMsgCounter);
        counters.add(tmpFailedMsgCounter);
        counters.add(successIterationsCounter);
        counters.add(failedIterationsCounter);
    }

    public Timer getTimer(TenantId tenantId, String status){
        return tenantMsgProcessTimers.computeIfAbsent(tenantId,
                id -> statsFactory.createTimer(StatsType.RULE_ENGINE.getName() + "." + queueName,
                        "tenantId", tenantId.getId().toString(),
                        "status", status
                ));
    }

    public void log(TbRuleEngineProcessingResult msg, boolean finalIterationForPack) {
        int success = msg.getSuccessMap().size();
        int pending = msg.getPendingMap().size();
        int failed = msg.getFailedMap().size();
        totalMsgCounter.add(success + pending + failed);
        successMsgCounter.add(success);
        msg.getSuccessMap().values().forEach(m -> getTenantStats(m).logSuccess());
        if (finalIterationForPack) {
            if (pending > 0 || failed > 0) {
                timeoutMsgCounter.add(pending);
                failedMsgCounter.add(failed);
                if (pending > 0) {
                    msg.getPendingMap().values().forEach(m -> getTenantStats(m).logTimeout());
                }
                if (failed > 0) {
                    msg.getFailedMap().values().forEach(m -> getTenantStats(m).logFailed());
                }
                failedIterationsCounter.increment();
            } else {
                successIterationsCounter.increment();
            }
        } else {
            failedIterationsCounter.increment();
            tmpTimeoutMsgCounter.add(pending);
            tmpFailedMsgCounter.add(failed);
            if (pending > 0) {
                msg.getPendingMap().values().forEach(m -> getTenantStats(m).logTmpTimeout());
            }
            if (failed > 0) {
                msg.getFailedMap().values().forEach(m -> getTenantStats(m).logTmpFailed());
            }
        }
        msg.getExceptionsMap().forEach(tenantExceptions::putIfAbsent);
    }

    private TbTenantRuleEngineStats getTenantStats(TbProtoQueueMsg<ToRuleEngineMsg> m) {
        ToRuleEngineMsg reMsg = m.getValue();
        return tenantStats.computeIfAbsent(new UUID(reMsg.getTenantIdMSB(), reMsg.getTenantIdLSB()), TbTenantRuleEngineStats::new);
    }

    public ConcurrentMap<UUID, TbTenantRuleEngineStats> getTenantStats() {
        return tenantStats;
    }

    public String getQueueName() {
        return queueName;
    }

    public ConcurrentMap<TenantId, RuleEngineException> getTenantExceptions() {
        return tenantExceptions;
    }

    public void printStats() {
        int total = totalMsgCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach(counter -> {
                stats.append(counter.getName()).append(" = [").append(counter.get()).append("] ");
            });
            log.info("[{}] Stats: {}", queueName, stats);
        }
    }

    public void reset() {
        counters.forEach(StatsCounter::clear);
        tenantStats.clear();
        tenantExceptions.clear();
    }
}
