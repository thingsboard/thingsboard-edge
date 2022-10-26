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
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("('${metrics.enabled:false}'=='true') && ('${service.type:null}'=='tb-integration' " +
        "|| '${service.type:null}'=='tb-integration-executor' || '${service.type:null}'=='monolith')")
public class IntegrationStatisticsDefault implements IntegrationStatisticsService {

    private final Map<IntegrationStatisticsKey, DefaultCounter> counters = new ConcurrentHashMap<>();
    private final Map<IntegrationStatisticsKey, AtomicLong> gauges = new ConcurrentHashMap<>();

    private static final String STATS_KEY_COUNTER = StatsType.INTEGRATION.getName() + "_stats_counter";
    private static final String STATS_KEY_GAUGE = StatsType.INTEGRATION.getName() + "_stats_gauge";

    private final StatsFactory statsFactory;

    @Override
    public void onIntegrationMsgsStateSuccessCounterAdd(IntegrationType integrationType) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, true, integrationType));
        } catch (Exception e) {
            log.error("onIntegrationStartCounterSuccess type:  [{}], error: [{}]", integrationType.name(), e.getMessage());
        }
    }

    @Override
    public void onIntegrationMsgsStateFailedCounterAdd(IntegrationType integrationType) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, false, integrationType));
        } catch (Exception e) {
            log.error("onIntegrationStartCounterFailed type:  [{}], error: [{}]", integrationType.name(), e.getMessage());
        }
    }

    @Override
    public void onIntegrationStateSuccessGauge(IntegrationType integrationType, int cntIntegration) {
        try {
            logMessagesGauge(cntIntegration, new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, true, integrationType));
        } catch (Exception e) {
            log.error("onIntegrationStartGaugeSuccess type:  [{}], error: [{}]", integrationType.name(), e.getMessage());
        }
    }

    @Override
    public void onIntegrationStateFailedGauge(IntegrationType integrationType, int cntIntegration) {
        try {
            logMessagesGauge(cntIntegration, new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, false, integrationType));
        } catch (Exception e) {
            log.error("onIntegrationStartGaugeFailed type:  [{}], error: [{}]", integrationType.name(), e.getMessage());
        }
    }

    @Override
    public void onIntegrationMsgsUplinkSuccess(IntegrationType integrationType) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.MSGS_UPLINK, true, integrationType));
        } catch (Exception e) {
            log.error("onIntegrationMsgsUplink type:  [{}], error: [{}]", integrationType.name(), e.getMessage());
        }
    }

    @Override
    public void onIntegrationMsgsUplinkFailed(IntegrationType integrationType) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.MSGS_UPLINK, false, integrationType));
        } catch (Exception e) {
            log.error("onIntegrationMsgsUplink type:  [{}], error: [{}]", integrationType.name(), e.getMessage());
        }
    }

    @Override
    public void onIntegrationMsgsDownlinkSuccess(IntegrationType integrationType) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.MSGS_DOWNLINK, true, integrationType));
        } catch (Exception e) {
            log.error("Type:  [{}], error: [{}]", integrationType.name(), e.getMessage());
        }
    }

    @Override
    public void onIntegrationMsgsDownlinkFailed(IntegrationType integrationType) {
        try {
            logMessagesCounterAdd(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.MSGS_DOWNLINK, false, integrationType));
        } catch (Exception e) {
            log.error("Type:  [{}], error: [{}]", integrationType.name(), e.getMessage());
        }
    }

    @Override
    public Map<IntegrationType, Long> getGaugesSuccess() {
        return gauges.entrySet().stream().filter(m -> m.getKey().isProcessState() &&
                m.getKey().getIntegrationStatisticsMetricName().equals(IntegrationStatisticsMetricName.START)).collect(
                Collectors.toMap(m -> m.getKey().getIntegrationType(), m -> m.getValue().get()));
    }

    @Override
    public Map<IntegrationType, Long> getGaugesFailed() {
        return gauges.entrySet().stream().filter(m -> !m.getKey().isProcessState()
                && m.getKey().getIntegrationStatisticsMetricName().equals(IntegrationStatisticsMetricName.START))
                .collect(Collectors.toMap(m -> m.getKey().getIntegrationType(), m -> m.getValue().get()));
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
                statsFactory.createDefaultCounter(STATS_KEY_COUNTER, tags.getTags()));
    }

    private AtomicLong getOrCreateStatsGauge(IntegrationStatisticsKey tags) throws Exception {
        return gauges.computeIfAbsent(tags, s ->
                statsFactory.createGauge(STATS_KEY_GAUGE, new AtomicLong(0),  tags.getTags()));
    }

}
