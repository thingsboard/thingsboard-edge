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
package org.thingsboard.server.common.data.scheduler;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Collections;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;

public class WeeklyRepeatTest {

    public static final String TIMEZONE = "EET";

    @Test
    public void getNextTest() throws ParseException {
        long startTime = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-09-10T01:22:00+03:00Z").getTime();
        long ts = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-11-24T01:00:00+02:00Z").getTime();
        long nextTs = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-04-03T01:00:00+03:00Z").getTime();
        long endTs = ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-11-24T01:00:00+02:00Z").getTime();

        WeeklyRepeat weeklyRepeat = new WeeklyRepeat();
        weeklyRepeat.setRepeatOn(Collections.singletonList(4));
        weeklyRepeat.setEndsOn(endTs);

        assertThat(weeklyRepeat.getNext(startTime, ts, TIMEZONE)).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-11-24T01:22:00+02:00Z").getTime());

        assertThat(weeklyRepeat.getNext(startTime, nextTs, TIMEZONE)).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-04-06T01:22:00+03:00Z").getTime());
    }
}