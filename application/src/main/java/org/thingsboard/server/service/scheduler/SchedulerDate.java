package org.thingsboard.server.service.scheduler;

import java.util.Calendar;

abstract class SchedulerDate {

    protected long getNext(long startTime, long ts, String timezone, long endsOn, int calendarField) {
        Calendar calendar = SchedulerUtils.getCalendarWithTimeZone(timezone);

        long tmp = startTime;
        int repeatIteration = 0;
        while (tmp < endsOn) {
            calendar.setTimeInMillis(startTime);
            calendar.add(calendarField, repeatIteration);
            tmp = calendar.getTimeInMillis();
            if (tmp > ts) {
                return tmp;
            }
            repeatIteration++;
        }
        return 0L;
    }
}
