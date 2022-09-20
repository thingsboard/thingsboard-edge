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

import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultIntegrationStatistics implements IntegrationStatisticsService {

    private static final String STATS_KEY_CNT = StatsType.INTEGRATION.getName() + "_stats_cnt";
    private static final String STATS_KEY_PROCESS = StatsType.INTEGRATION.getName() + "_stats_process";
    private static final String NAME = "name";
    private static final String START = "start";
    private static final String MSGS_UPLINK = "msgUplink";
    private static final String MSGS_DOWNLINK = "msgDownlink";
    private static final String INTEGRATION_TYPE = "type";
    private static final String PROCESS_STATE = "state";
    private static final String PROCESS_STATE_SUCCESS = "success";
    private static final String PROCESS_STATE_FAILED = "failed";

    private final Map<String, DefaultCounter> counters = new HashMap<>();
    private final Map<String, AtomicLong> gauges = new HashMap<>();

    private final StatsFactory statsFactory;

    private boolean integrationStatisticsEnabled = true;

    @Override
    public void onIntegrationStartCounterAddSuccess(String integrationTypeName, int cntIntegration) {
        if (integrationStatisticsEnabled) {
            try {
                logMessagesCounterAdd(cntIntegration, NAME, START, PROCESS_STATE, PROCESS_STATE_SUCCESS, INTEGRATION_TYPE, integrationTypeName);
            } catch (Exception e) {
                log.error("onIntegrationStartCounterSuccess type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationStartCounterAddFailed(String integrationTypeName, int cntIntegration) {
        if (integrationStatisticsEnabled) {
            try {
                logMessagesCounterAdd(cntIntegration, NAME, START, PROCESS_STATE, PROCESS_STATE_FAILED, INTEGRATION_TYPE, integrationTypeName);
            } catch (Exception e) {
                log.error("onIntegrationStartCounterFailed type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationStartGaugeSuccess(String integrationTypeName, int cntIntegration) {
        if (integrationStatisticsEnabled) {
            try {
                logMessagesGauge(cntIntegration, NAME, START, PROCESS_STATE, PROCESS_STATE_SUCCESS, INTEGRATION_TYPE, integrationTypeName);
            } catch (Exception e) {
                log.error("onIntegrationStartGaugeSuccess type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationStartGaugeFailed(String integrationTypeName, int cntIntegration) {
        if (integrationStatisticsEnabled) {
            try {
                logMessagesGauge(cntIntegration, NAME, START, PROCESS_STATE, PROCESS_STATE_FAILED, INTEGRATION_TYPE, integrationTypeName);
            } catch (Exception e) {
                log.error("onIntegrationStartGaugeFailed type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationMsgsUplinkSuccess(String integrationTypeName, int cntIntegration) {
        if (integrationStatisticsEnabled) {
            try {
                logMessagesCounterAdd(cntIntegration, NAME, MSGS_UPLINK, PROCESS_STATE, PROCESS_STATE_SUCCESS, INTEGRATION_TYPE, integrationTypeName);
            } catch (Exception e) {
                log.error("onIntegrationMsgsUplink type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationMsgsUplinkFailed(String integrationTypeName, int cntIntegration) {
        if (integrationStatisticsEnabled) {
            try {
                logMessagesCounterAdd(cntIntegration, NAME, MSGS_UPLINK, PROCESS_STATE, PROCESS_STATE_FAILED, INTEGRATION_TYPE, integrationTypeName);
            } catch (Exception e) {
                log.error("onIntegrationMsgsUplink type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationMsgsDownlinkSuccess(String integrationTypeName, int cntIntegration) {
        if (integrationStatisticsEnabled) {
            try {
                logMessagesCounterAdd(cntIntegration, NAME, MSGS_DOWNLINK, PROCESS_STATE, PROCESS_STATE_SUCCESS, INTEGRATION_TYPE, integrationTypeName);
            } catch (Exception e) {
                log.error("onIntegrationMsgsDownlink type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationMsgsDownlinkFailed(String integrationTypeName, int cntIntegration) {
        if (integrationStatisticsEnabled) {
            try {
                logMessagesCounterAdd(cntIntegration, NAME, MSGS_DOWNLINK, PROCESS_STATE, PROCESS_STATE_FAILED, INTEGRATION_TYPE, integrationTypeName);
            } catch (Exception e) {
                log.error("onIntegrationMsgsDownlink type:  [{}], error: [{}]", integrationTypeName, e.getMessage());
            }
        }
    }

    @Override
    public void printStats() {
        if (counters.size() > 0) {
            StringBuilder stats = new StringBuilder();
            counters.entrySet().stream().forEach(c -> stats.append(c.getKey()).append(" = [").append(c.getValue().get()).append("] "));
            log.info("Core Stats: {}", stats);
        }
    }

    @Override
    public void reset() {
        counters.values().forEach(DefaultCounter::clear);
    }

    public void removeCounter(DefaultCounter counter) {
        counters.values().remove(counter);
    }

    private void logMessagesCounterWithClear(int cntIntegration, String... tags) throws Exception {
        DefaultCounter counter = getOrCreateStatsCounter(cntIntegration, tags);
        if (counter != null) {
            counter.clear();
            counter.add(cntIntegration);
        }
    }

    private void logMessagesCounterAdd(int cntIntegration, String... tags) throws Exception {
        DefaultCounter counter = getOrCreateStatsCounter(cntIntegration, tags);
        if (counter != null) {
            while (cntIntegration >0) {
                counter.increment();
                cntIntegration--;
            }
        }
    }

    private void logMessagesGauge(int cntValue, String... tags) throws Exception {
        AtomicLong gauge = getOrCreateStatsGauge(tags);
        gauge.set(cntValue);
    }

    private DefaultCounter getOrCreateStatsCounter(int cntIntegration, String... tags) throws Exception {
        String statsName = getValidateStatsName(tags);
        Optional<Map.Entry<String, DefaultCounter>> statsCounterOpt = counters.entrySet().stream()
                .filter(c -> c.getKey().equals(statsName)).findFirst();
        if (statsCounterOpt.isPresent()) {
            return statsCounterOpt.get().getValue();
        } else if (cntIntegration > 0) {
            return createAndRegisterCounter(statsName, tags);
        } else {
            return null;
        }
    }

    private AtomicLong getOrCreateStatsGauge(String... tags) throws Exception {
        String statsName = getValidateStatsName(tags);
        Optional<Map.Entry<String, AtomicLong>> statsGaugeOpt = gauges.entrySet().stream()
                .filter(c -> c.getKey().equals(statsName)).findFirst();
        AtomicLong statsGauge = statsGaugeOpt.isPresent() ? statsGaugeOpt.get().getValue() :
                createAndRegisterGauge(statsName, tags);
        return statsGauge;
    }

    private DefaultCounter createAndRegisterCounter(String statsName, String... tags) {
        DefaultCounter counter = statsFactory.createDefaultCounter(STATS_KEY_CNT, tags);
        counters.putIfAbsent(statsName, counter);
        return counter;
    }

    private AtomicLong createAndRegisterGauge(String statsName, String... tags) {
        AtomicLong gaugeValue = new AtomicLong(0);
        statsFactory.createGauge(STATS_KEY_PROCESS, gaugeValue, tags);
        gauges.putIfAbsent(statsName, gaugeValue);
        return gaugeValue;
    }

    private String getValidateStatsName(String... keyValues) throws Exception {
        Tags.of(keyValues);
        if (keyValues.length != 6) {
            throw new IllegalArgumentException("Length eyValues  must be equals 6!");
        }
        return String.join("_", keyValues);
    }
}
