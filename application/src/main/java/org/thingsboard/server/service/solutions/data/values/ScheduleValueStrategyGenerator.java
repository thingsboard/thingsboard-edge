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
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

public class ScheduleValueStrategyGenerator extends TelemetryGenerator {

    private TelemetryGenerator defaultGenerator;
    private TimeZone timeZone;
    private Map<ValueStrategySchedule, TelemetryGenerator> scheduleGenerators;
    private TelemetryGenerator prevGenerator;

    public ScheduleValueStrategyGenerator(TelemetryProfile tp) {
        super(tp);
        var def = (ScheduleValueStrategyDefinition) tp.getValueStrategy();
        timeZone = TimeZone.getTimeZone(def.getTimeZone());
        defaultGenerator = TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), def.getDefaultDefinition()));
        scheduleGenerators = new LinkedHashMap<>();
        for (ValueStrategySchedule scheduleItem : def.getSchedule()) {
            scheduleGenerators.put(scheduleItem, TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), scheduleItem.getDefinition())));
        }
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        int hour = GeneratorTools.getHour(timeZone, ts);
        int minute = GeneratorTools.getMinute(timeZone, ts);
        TelemetryGenerator generator = scheduleGenerators.entrySet().stream().filter(pair -> {
            var schedule = pair.getKey();
            if (hour == schedule.getStartHour() && hour == schedule.getEndHour()) {
                return schedule.getStartMinute() <= minute && minute <= schedule.getEndMinute();
            } else if (hour == schedule.getStartHour() && hour < schedule.getEndHour()) {
                return schedule.getStartMinute() <= minute;
            } else if (hour > schedule.getStartHour() && hour < schedule.getEndHour()) {
                return true;
            } else if (hour > schedule.getStartHour() && hour == schedule.getEndHour()) {
                return minute <= schedule.getEndHour();
            } else {
                return false;
            }
        }).map(Map.Entry::getValue).findFirst().orElse(defaultGenerator);

        if (prevGenerator != null && !prevGenerator.equals(generator)) {
            generator.setValue(prevGenerator.getValue());
        }

        generator.addValue(ts, values);

        prevGenerator = generator;
    }
}
