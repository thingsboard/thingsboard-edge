package org.thingsboard.server.service.scheduler;

import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.TimeZone;

public class SchedulerUtils {

    public static Calendar getCalendarWithTimeZone(String timezone) {
        TimeZone tz;
        if (StringUtils.isEmpty(timezone)) {
            tz = TimeZone.getTimeZone("UTC");
        } else {
            tz = TimeZone.getTimeZone(timezone);
        }
        return Calendar.getInstance(tz);
    }
}
