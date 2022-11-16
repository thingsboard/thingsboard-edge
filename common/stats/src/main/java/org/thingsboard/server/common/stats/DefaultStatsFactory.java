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
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DefaultStatsFactory implements StatsFactory {
    private static final String TOTAL_MSGS = "totalMsgs";
    private static final String SUCCESSFUL_MSGS = "successfulMsgs";
    private static final String FAILED_MSGS = "failedMsgs";

    private static final String STATS_NAME_TAG = "statsName";

    private static final Counter STUB_COUNTER = new StubCounter();

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${metrics.enabled:false}")
    private Boolean metricsEnabled;

    @Value("${metrics.timer.percentiles:0.5}")
    private String timerPercentilesStr;

    private double[] timerPercentiles;

    @PostConstruct
    public void init() {
        if (!StringUtils.isEmpty(timerPercentilesStr)) {
            String[] split = timerPercentilesStr.split(",");
            timerPercentiles = new double[split.length];
            for (int i = 0; i < split.length; i++) {
                timerPercentiles[i] = Double.parseDouble(split[i]);
            }
        }
    }


    @Override
    public StatsCounter createStatsCounter(String key, String statsName) {
        return new StatsCounter(
                new AtomicInteger(0),
                metricsEnabled ?
                        meterRegistry.counter(key, STATS_NAME_TAG, statsName)
                        : STUB_COUNTER,
                statsName
        );
    }

    @Override
    public DefaultCounter createDefaultCounter(String key, String... tags) {
        return new DefaultCounter(
                new AtomicInteger(0),
                metricsEnabled ?
                        meterRegistry.counter(key, tags)
                        : STUB_COUNTER
        );
    }

    @Override
    public <T extends Number> T createGauge(String key, T number, String... tags) {
        return metricsEnabled ? meterRegistry.gauge(key, Tags.of(tags), number) : number;
    }

    @Override
    public MessagesStats createMessagesStats(String key) {
        StatsCounter totalCounter = createStatsCounter(key, TOTAL_MSGS);
        StatsCounter successfulCounter = createStatsCounter(key, SUCCESSFUL_MSGS);
        StatsCounter failedCounter = createStatsCounter(key, FAILED_MSGS);
        return new DefaultMessagesStats(totalCounter, successfulCounter, failedCounter);
    }

    @Override
    public Timer createTimer(String key, String... tags) {
        Timer.Builder timerBuilder = Timer.builder(key)
                .tags(tags)
                .publishPercentiles();
        if (timerPercentiles != null && timerPercentiles.length > 0) {
            timerBuilder.publishPercentiles(timerPercentiles);
        }
        return timerBuilder.register(meterRegistry);
    }

    private static class StubCounter implements Counter {
        @Override
        public void increment(double amount) {
        }

        @Override
        public double count() {
            return 0;
        }

        @Override
        public Id getId() {
            return null;
        }
    }

    public Meter remove(Counter counter) {
        return meterRegistry.remove(counter.getId());
    }

}
