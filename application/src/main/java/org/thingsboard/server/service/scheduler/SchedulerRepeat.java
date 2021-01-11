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
package org.thingsboard.server.service.scheduler;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by ashvayka on 28.11.17.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DailyRepeat.class, name = "DAILY"),
        @JsonSubTypes.Type(value = WeeklyRepeat.class, name = "WEEKLY"),
        @JsonSubTypes.Type(value = MonthlyRepeat.class, name = "MONTHLY"),
        @JsonSubTypes.Type(value = YearlyRepeat.class, name = "YEARLY"),
        @JsonSubTypes.Type(value = TimerRepeat.class, name = "TIMER")
})
public interface SchedulerRepeat {

    long getEndsOn();

    SchedulerRepeatType getType();

    long getNext(long startTime, long ts, String timezone);
}
