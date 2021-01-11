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
package org.thingsboard.server.service.scheduler;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import lombok.Data;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;

/**
 * Created by ashvayka on 25.06.18.
 */
@Data
class SchedulerEventMetaData {

    private final SchedulerEventInfo info;
    private final long startTime;
    private final String timezone;
    private final SchedulerRepeat repeat;
    private volatile ListenableScheduledFuture<?> nextTaskFuture;

    boolean passedAway(long ts) {
        return repeat == null ? startTime < ts : repeat.getEndsOn() < ts;
    }

    long getNextEventTime(long ts) {
        if (repeat != null && repeat.getEndsOn() > ts) {
            return repeat.getNext(startTime, ts, timezone);
        } else if (ts < startTime) {
            return startTime;
        } else {
            return 0L;
        }
    }
}
