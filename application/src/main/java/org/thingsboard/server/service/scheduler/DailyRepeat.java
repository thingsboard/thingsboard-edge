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

import lombok.Data;

/**
 * Created by ashvayka on 28.11.17.
 */
@Data
public class DailyRepeat implements SchedulerRepeat {

    public static final long _1DAY = 1000 * 60 * 60 * 24;
    private long endsOn;

    @Override
    public SchedulerRepeatType getType() {
        return SchedulerRepeatType.DAILY;
    }


    @Override
    public long getNext(long startTime, long ts, String timezone) {
        for (long tmp = startTime; tmp < endsOn; tmp += _1DAY) {
            if (tmp > ts) {
                return tmp;
            }
        }
        return 0L;
    }
}
