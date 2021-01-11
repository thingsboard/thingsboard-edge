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
package org.thingsboard.rule.engine.analytics.latest.alarm;

import lombok.Data;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesGroup;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNodeConfiguration;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
public class TbAlarmsCountNodeConfiguration extends TbAbstractLatestNodeConfiguration {

    private boolean countAlarmsForChildEntities;
    private List<AlarmsCountMapping> alarmsCountMappings;

    @Override
    public TbAlarmsCountNodeConfiguration defaultConfiguration() {
        TbAlarmsCountNodeConfiguration configuration = new TbAlarmsCountNodeConfiguration();

        configuration.setParentEntitiesQuery(new ParentEntitiesGroup());

        configuration.setCountAlarmsForChildEntities(false);

        List<AlarmsCountMapping> alarmsCountMappings = new ArrayList<>();
        AlarmsCountMapping alarmsCountMapping = new AlarmsCountMapping();
        alarmsCountMapping.setTarget("alarmsCount");
        alarmsCountMappings.add(alarmsCountMapping);

        configuration.setAlarmsCountMappings(alarmsCountMappings);

        configuration.setPeriodTimeUnit(TimeUnit.MINUTES);
        configuration.setPeriodValue(5);

        return configuration;
    }
}
