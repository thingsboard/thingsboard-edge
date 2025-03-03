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

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.kv.IntervalType;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilsTest {

    @Test
    void testWeekEnd() {
        long ts = 1704899727000L; // Wednesday, January 10 15:15:27 GMT
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK, ZoneId.of("Europe/Kyiv"))).isEqualTo(1705183200000L); // Sunday, January 14, 2024 0:00:00 GMT+02:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK_ISO, ZoneId.of("Europe/Kyiv"))).isEqualTo(1705269600000L); // Monday, January 15, 2024 0:00:00 GMT+02:00

        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK, ZoneId.of("Europe/Amsterdam"))).isEqualTo(1705186800000L); // Sunday, January 14, 2024 0:00:00 GMT+01:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK_ISO, ZoneId.of("Europe/Amsterdam"))).isEqualTo(1705273200000L); // Monday, January 15, 2024 0:00:00 GMT+01:00

        ts = 1704621600000L; // Sunday, January 7, 2024 12:00:00 GMT+02:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK, ZoneId.of("Europe/Kyiv"))).isEqualTo(1705183200000L); // Sunday, January 14, 2024 0:00:00 GMT+02:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK_ISO, ZoneId.of("Europe/Kyiv"))).isEqualTo(1704664800000L); // Monday, January 8, 2024 0:00:00 GMT+02:00
    }


    @Test
    void testMonthEnd() {
        long ts = 1704899727000L; // Wednesday, January 10 15:15:27 GMT
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.MONTH, ZoneId.of("Europe/Kyiv"))).isEqualTo(1706738400000L); // Thursday, February 1, 2024 0:00:00 GMT+02:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.MONTH, ZoneId.of("Europe/Amsterdam"))).isEqualTo(1706742000000L); // Monday, January 15, 2024 0:00:00 GMT+02:00
    }

    @Test
    void testQuarterEnd() {
        long ts = 1704899727000L; // Wednesday, January 10 15:15:27 GMT
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.QUARTER, ZoneId.of("Europe/Kyiv"))).isEqualTo(1711918800000L); // Monday, April 1, 2024 0:00:00 GMT+03:00 DST
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.QUARTER, ZoneId.of("Europe/Amsterdam"))).isEqualTo(1711922400000L); // Monday, April 1, 2024 1:00:00 GMT+03:00 DST

        ts = 1711929600000L; // Monday, April 1, 2024 3:00:00 GMT+03:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.QUARTER, ZoneId.of("Europe/Kyiv"))).isEqualTo(1719781200000L); // Monday, July 1, 2024 0:00:00 GMT+03:00 DST
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.QUARTER, ZoneId.of("America/New_York"))).isEqualTo(1711944000000L); // Monday, April 1, 2024 7:00:00 GMT+03:00 DST
    }

}
