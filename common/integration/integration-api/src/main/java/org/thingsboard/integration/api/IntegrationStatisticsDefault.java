/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.thingsboard.integration.api.IntegrationStatisticsKeyTags.STATS_KEY_COUNTER;
import static org.thingsboard.integration.api.IntegrationStatisticsKeyTags.STATS_KEY_GAUGE;
import static org.thingsboard.integration.api.IntegrationStatisticsKeyTags.START;
import static org.thingsboard.integration.api.IntegrationStatisticsKeyTags.MSGS_DOWNLINK;
import static org.thingsboard.integration.api.IntegrationStatisticsKeyTags.MSGS_UPLINK;
import static org.thingsboard.integration.api.IntegrationStatisticsKeyTags.PROCESS_STATE_FAILED;
import static org.thingsboard.integration.api.IntegrationStatisticsKeyTags.PROCESS_STATE_SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("('${metrics.enabled:false}'=='true') && ('${service.type:null}'=='tb-integration' " +
        "|| '${service.type:null}'=='tb-integration-executor' || '${service.type:null}'=='monolith')")
public class IntegrationStatisticsDefault implements IntegrationStatisticsService {

    private final Map<IntegrationStatisticsKey, DefaultCounter> counters = new ConcurrentHashMap<>();
    private final Map<IntegrationStatisticsKey, AtomicLong> gauges = new ConcurrentHashMap<>();

    private final StatsFactory statsFactory;

    @Override
    public void onIntegrationMsgsStateSuccessCounterAdd(String integrationTypeName) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(START.getName(), PROCESS_STATE_SUCCESS.getName(), integrationTypeName));
        } catch (Exception e) {
            log.error("onIntegrationStartCounterSuccess type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
        }
    }

    @Override
    public void onIntegrationMsgsStateFailedCounterAdd(String integrationTypeName) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(START.getName(), PROCESS_STATE_FAILED.getName(), integrationTypeName));
        } catch (Exception e) {
            log.error("onIntegrationStartCounterFailed type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
        }
    }

    @Override
    public void onIntegrationStateSuccessGauge(String integrationTypeName, int cntIntegration) {
        try {
            logMessagesGauge(cntIntegration, new IntegrationStatisticsKey(START.getName(), PROCESS_STATE_SUCCESS.getName(), integrationTypeName));
        } catch (Exception e) {
            log.error("onIntegrationStartGaugeSuccess type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
        }
    }

    @Override
    public void onIntegrationStateFailedGauge(String integrationTypeName, int cntIntegration) {
        try {
            logMessagesGauge(cntIntegration, new IntegrationStatisticsKey(START.getName(), PROCESS_STATE_FAILED.getName(), integrationTypeName));
        } catch (Exception e) {
            log.error("onIntegrationStartGaugeFailed type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
        }
    }

    @Override
    public void onIntegrationMsgsUplinkSuccess(String integrationTypeName) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(MSGS_UPLINK.getName(), PROCESS_STATE_SUCCESS.getName(), integrationTypeName));
        } catch (Exception e) {
            log.error("onIntegrationMsgsUplink type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
        }
    }

    @Override
    public void onIntegrationMsgsUplinkFailed(String integrationTypeName) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(MSGS_UPLINK.getName(), PROCESS_STATE_FAILED.getName(), integrationTypeName));
        } catch (Exception e) {
            log.error("onIntegrationMsgsUplink type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
        }
    }

    @Override
    public void onIntegrationMsgsDownlinkSuccess(String integrationTypeName) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(MSGS_DOWNLINK.getName(), PROCESS_STATE_SUCCESS.getName(), integrationTypeName));
        } catch (Exception e) {
            log.error("Type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
        }
    }

    @Override
    public void onIntegrationMsgsDownlinkFailed(String integrationTypeName) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(MSGS_DOWNLINK.getName(), PROCESS_STATE_FAILED.getName(), integrationTypeName));
        } catch (Exception e) {
            log.error("Type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
        }
    }

    @Override
    public Map<String, Long> getGaugesSuccess() {
        return gauges.entrySet().stream().filter(m -> m.getKey().getProcessState().equals(PROCESS_STATE_SUCCESS.getName())).collect(
                Collectors.toMap(m -> m.getKey().getIntegrationType(), m -> m.getValue().get()));
    }

    @Override
    public Map<String, Long> getGaugesFailed() {
        return gauges.entrySet().stream().filter(m -> m.getKey().getProcessState().equals(PROCESS_STATE_FAILED.getName())).collect(
                Collectors.toMap(m -> m.getKey().getIntegrationType(), m -> m.getValue().get()));
    }

    @Override
    public void printStats() {
        if (counters.size() > 0) {
            StringBuilder stats = new StringBuilder();
            counters.entrySet().stream().forEach(c -> stats.append(c.getKey()).append(" = [").append(c.getValue().get()).append("] "));
            log.info("Integration Stats: {}", stats);
        }
    }

    @Override
    public void reset() {
        counters.values().forEach(DefaultCounter::clear);
    }

    private void logMessagesCounterAdd(IntegrationStatisticsKey tags) throws Exception {
        DefaultCounter counter = getOrCreateStatsCounter(tags);
        counter.increment();
    }

    private void logMessagesGauge(int cntValue, IntegrationStatisticsKey tags) throws Exception {
        AtomicLong gauge = getOrCreateStatsGauge(tags);
        gauge.set(cntValue);
    }

    private DefaultCounter getOrCreateStatsCounter(IntegrationStatisticsKey tags) throws Exception {
        return counters.computeIfAbsent(tags, s ->
                statsFactory.createDefaultCounter(STATS_KEY_COUNTER.getName(), tags.getKey()));
    }

    private AtomicLong getOrCreateStatsGauge(IntegrationStatisticsKey tags) throws Exception {
        return gauges.computeIfAbsent(tags, s ->
                statsFactory.createGauge(STATS_KEY_GAUGE.getName(), new AtomicLong(0),  tags.getKey()));
    }

}
