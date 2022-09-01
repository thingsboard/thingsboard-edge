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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@TbCoreOrIntegrationExecutorComponent
@Service
@RequiredArgsConstructor
public class DefaultIntegrationStatistics implements IntegrationStatisticsService {

    private static final String STATS_KEY = StatsType.INTEGRATION.getName() + "_stats";
    private static final String TOTAL_NAME = "name";
    private static final String TOTAL_START = "totalStart";
    private static final String TOTAL_MSGS_UPLINK = "totalUplinkMsg";
    private static final String TOTAL_MSGS_DOWNLINK = "totalDownlinkMsg";
    private static final String INTEGRATION_TYPE = "type";
    private static final String PROCESS_STATE = "state";
    private static final String PROCESS_STATE_SUCCESS = "success";
    private static final String PROCESS_STATE_FAILED = "failed";

    private final Map<String, DefaultCounter> counters = new HashMap<>();

    private final StatsFactory statsFactory;

    @Value("${integrations.statistics.enabled}")
    private boolean integrationStatisticsEnabled;

    @Override
    public void onIntegrationStart(IntegrationType type) {
        if (integrationStatisticsEnabled) {
            try {
                logMessages(TOTAL_NAME, TOTAL_START, PROCESS_STATE, PROCESS_STATE_SUCCESS, INTEGRATION_TYPE, type.name());
            } catch (Exception e) {
                log.error("OnIntegrationStartStatistic type:  [{}], error: [{}]", type.name(), e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationStartFailed(IntegrationType type) {
        if (integrationStatisticsEnabled) {
            try {
                logMessages(TOTAL_NAME, TOTAL_START, PROCESS_STATE, PROCESS_STATE_FAILED, INTEGRATION_TYPE, type.name());
            } catch (Exception e) {
                log.error("OnIntegrationStartFailedStatistic type:  [{}], error: [{}]", type.name(), e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationMsgsUplink(IntegrationType type) {
        if (integrationStatisticsEnabled) {
            try {
                logMessages(TOTAL_NAME, TOTAL_MSGS_UPLINK, PROCESS_STATE, PROCESS_STATE_SUCCESS, INTEGRATION_TYPE, type.name());
            } catch (Exception e) {
                log.error("onIntegrationMsgsUplink type:  [{}], error: [{}]", type.name(), e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationMsgsUplinkFailed(IntegrationType type) {
        if (integrationStatisticsEnabled) {
            try {
                logMessages(TOTAL_NAME, TOTAL_MSGS_UPLINK, PROCESS_STATE, PROCESS_STATE_FAILED, INTEGRATION_TYPE, type.name());
            } catch (Exception e) {
                log.error("onIntegrationMsgsUplink type:  [{}], error: [{}]", type.name(), e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationMsgsDownlink(IntegrationType type) {
        if (integrationStatisticsEnabled) {
            try {
                logMessages(TOTAL_NAME, TOTAL_MSGS_DOWNLINK, PROCESS_STATE, PROCESS_STATE_SUCCESS, INTEGRATION_TYPE, type.name());
            } catch (Exception e) {
                log.error("onIntegrationMsgsDownlink type:  [{}], error: [{}]", type.name(), e.getMessage());
            }
        }
    }

    @Override
    public void onIntegrationMsgsDownlinkFailed(IntegrationType type) {
        if (integrationStatisticsEnabled) {
            try {
                logMessages(TOTAL_NAME, TOTAL_MSGS_DOWNLINK, PROCESS_STATE_FAILED, PROCESS_STATE_SUCCESS, INTEGRATION_TYPE, type.name());
            } catch (Exception e) {
                log.error("onIntegrationMsgsDownlink type:  [{}], error: [{}]", type.name(), e.getMessage());
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

    private void logMessages(String... tags) throws Exception {
        DefaultCounter counter = getOrCreateStatsCounter(tags);
        counter.increment();
    }

    private DefaultCounter getOrCreateStatsCounter(String... tags) throws Exception {
        String statsName = getValidateStatsName(tags);
        Optional<Map.Entry<String, DefaultCounter>> statsCounterOpt = counters.entrySet().stream()
                .filter(c -> c.getValue().equals(statsName)).findFirst();
        DefaultCounter statsCounter = statsCounterOpt.isPresent() ? statsCounterOpt.get().getValue() :
                register(statsName, tags);
        return statsCounter;
    }

    private DefaultCounter register(String statsName, String... tags) {
        DefaultCounter counter = statsFactory.createDefaultCounter(STATS_KEY, tags);
        counters.putIfAbsent(statsName, counter);
        return counter;
    }

    private String getValidateStatsName(String... keyValues) throws Exception {
        Tags.of(keyValues);
        if (keyValues.length != 6) {
            throw new IllegalArgumentException("Length eyValues  must be equals 6!");
        }
        return String.join("_", keyValues);
    }
}
