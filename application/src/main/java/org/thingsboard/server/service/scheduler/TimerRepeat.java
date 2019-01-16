package org.thingsboard.server.service.scheduler;

import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class TimerRepeat implements SchedulerRepeat {

    private long repeatInterval;
    private TimeUnit timeUnit;
    private long endsOn;


    @Override
    public long getEndsOn() {
        return endsOn;
    }

    @Override
    public SchedulerRepeatType getType() {
        return SchedulerRepeatType.TIMER;
    }

    @Override
    public long getNext(long startTime, long ts) {
        long interval = timeUnit.toMillis(repeatInterval);
        for (long tmp = startTime; tmp < endsOn; tmp += interval) {
            if (tmp > ts) {
                return tmp;
            }
        }
        return 0L;
    }
}
