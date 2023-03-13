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

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

public class GeneratorTools {

    private static final Random random = new Random();

    public static long randomLong(double min, double max) {
        return (long) randomDouble(min, max);
    }

    public static double randomDouble(double min, double max) {
        return min + random.nextDouble() * Math.abs(max - min);
    }

    public static double getMultiplier(long ts, double holidayMultiplier, double workHoursMultiplier, double nightHoursMultiplier) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        double multiplier = 1.0;
        if (dayOfWeek == 1 || dayOfWeek == 7) {
            multiplier *= holidayMultiplier;
        }

        if (hour > 8 && hour < 18) {
            multiplier *= workHoursMultiplier;
        } else if (hour < 6 || hour > 22) {
            multiplier *= nightHoursMultiplier;
        }
        return multiplier;
    }

    public static int getHour(TimeZone tz, long ts) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTimeZone(tz);
        c.setTime(date);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    public static int getMinute(TimeZone tz, long ts) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTimeZone(tz);
        c.setTime(date);
        return c.get(Calendar.MINUTE);
    }

    public static boolean isHoliday(long ts) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == 1 || dayOfWeek == 7;
    }

    public static boolean isWorkHour(long ts) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int hour = c.get(Calendar.HOUR_OF_DAY);

        return hour > 8 && hour < 18;
    }

    public static boolean isNightHour(long ts) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int hour = c.get(Calendar.HOUR_OF_DAY);

        return hour < 6 || hour >= 22;
    }

}
