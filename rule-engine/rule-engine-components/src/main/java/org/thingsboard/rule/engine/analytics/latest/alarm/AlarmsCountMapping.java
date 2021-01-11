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
package org.thingsboard.rule.engine.analytics.latest.alarm;

import lombok.Data;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;

import java.util.List;

@Data
public class AlarmsCountMapping {

    private String target;
    private List<String> typesList;
    private List<AlarmSeverity> severityList;
    private List<AlarmStatus> statusList;
    private long latestInterval;

    public AlarmFilter createAlarmFilter() {
        Long startTime = null;
        if (latestInterval > 0) {
            startTime = System.currentTimeMillis() - latestInterval;
        }
        return new AlarmFilter(nullIfEmpty(this.typesList), nullIfEmpty(this.severityList), nullIfEmpty(this.statusList), startTime);
    }

    private <T> List<T> nullIfEmpty(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list;
    }
}
