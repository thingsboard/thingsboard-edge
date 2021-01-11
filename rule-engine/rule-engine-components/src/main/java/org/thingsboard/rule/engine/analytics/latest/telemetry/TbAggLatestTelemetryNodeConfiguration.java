/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesGroup;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNodeConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
public class TbAggLatestTelemetryNodeConfiguration extends TbAbstractLatestNodeConfiguration {

    private List<AggLatestMapping> aggMappings;

    @Override
    public TbAggLatestTelemetryNodeConfiguration defaultConfiguration() {
        TbAggLatestTelemetryNodeConfiguration configuration = new TbAggLatestTelemetryNodeConfiguration();

        configuration.setParentEntitiesQuery(new ParentEntitiesGroup());

        List<AggLatestMapping> aggMappings = new ArrayList<>();
        AggLatestMapping aggMapping = new AggLatestMapping();
        aggMapping.setSource("temperature");
        aggMapping.setSourceScope("LATEST_TELEMETRY");
        aggMapping.setAggFunction(MathFunction.AVG);
        aggMapping.setDefaultValue(0);
        aggMapping.setTarget("latestAvgTemperature");
        aggMappings.add(aggMapping);

        configuration.setAggMappings(aggMappings);

        configuration.setPeriodTimeUnit(TimeUnit.MINUTES);
        configuration.setPeriodValue(5);

        return configuration;
    }
}
