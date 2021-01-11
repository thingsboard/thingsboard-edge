/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.analytics.incoming;

import lombok.Data;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesGroup;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNodeConfiguration;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.analytics.incoming.state.StatePersistPolicy;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Data
public class TbSimpleAggMsgNodeConfiguration extends TbAbstractLatestNodeConfiguration {

    private String mathFunction;

    private AggIntervalType aggIntervalType;
    private String timeZoneId;
    //For Static Intervals
    private String aggIntervalTimeUnit;
    private int aggIntervalValue;

    private boolean autoCreateIntervals;

    private String intervalPersistencePolicy;
    private String intervalCheckTimeUnit;
    private int intervalCheckValue;

    private String intervalTtlTimeUnit;
    private int intervalTtlValue;

    private String inputValueKey;
    private String outputValueKey;

    private String statePersistencePolicy;
    private String statePersistenceTimeUnit;
    private int statePersistenceValue;

    @Override
    public TbSimpleAggMsgNodeConfiguration defaultConfiguration() {
        TbSimpleAggMsgNodeConfiguration configuration = new TbSimpleAggMsgNodeConfiguration();

        configuration.setMathFunction(MathFunction.AVG.name());
        configuration.setAggIntervalType(AggIntervalType.HOUR);
        configuration.setAggIntervalTimeUnit(TimeUnit.HOURS.name());
        configuration.setAggIntervalValue(1);

        configuration.setAutoCreateIntervals(false);

        configuration.setParentEntitiesQuery(new ParentEntitiesGroup());

        configuration.setPeriodTimeUnit(TimeUnit.MINUTES);
        configuration.setPeriodValue(5);

        configuration.setIntervalPersistencePolicy(IntervalPersistPolicy.ON_EACH_CHECK_AFTER_INTERVAL_END.name());
        configuration.setIntervalCheckTimeUnit(TimeUnit.MINUTES.name());
        configuration.setIntervalCheckValue(1);

        configuration.setIntervalTtlTimeUnit(TimeUnit.DAYS.name());
        configuration.setIntervalTtlValue(1);

        configuration.setInputValueKey("temperature");
        configuration.setOutputValueKey("avgHourlyTemperature");

        configuration.setStatePersistencePolicy(StatePersistPolicy.ON_EACH_CHANGE.name());
        configuration.setStatePersistenceTimeUnit(TimeUnit.MINUTES.name());
        configuration.setStatePersistenceValue(1);

        return configuration;
    }

}
