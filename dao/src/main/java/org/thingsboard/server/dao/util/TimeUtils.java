/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.util;

import org.thingsboard.server.common.data.kv.IntervalType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;

public class TimeUtils {

    public static long calculateIntervalEnd(long startTs, IntervalType intervalType, ZoneId tzId) {
        var startTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTs), tzId);
        switch (intervalType) {
            case WEEK:
                return startTime.truncatedTo(ChronoUnit.DAYS).with(WeekFields.SUNDAY_START.dayOfWeek(), 1).plusDays(7).toInstant().toEpochMilli();
            case WEEK_ISO:
                return startTime.truncatedTo(ChronoUnit.DAYS).with(WeekFields.ISO.dayOfWeek(), 1).plusDays(7).toInstant().toEpochMilli();
            case MONTH:
                return startTime.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).plusMonths(1).toInstant().toEpochMilli();
            case QUARTER:
                return startTime.truncatedTo(ChronoUnit.DAYS).with(IsoFields.DAY_OF_QUARTER, 1).plusMonths(3).toInstant().toEpochMilli();
            default:
                throw new RuntimeException("Not supported!");
        }
    }

}
