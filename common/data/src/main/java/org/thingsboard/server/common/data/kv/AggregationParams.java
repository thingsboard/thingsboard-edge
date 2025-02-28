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
package org.thingsboard.server.common.data.kv;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@EqualsAndHashCode
@Slf4j
public class AggregationParams {
    private static final Map<String, String> TZ_LINKS = Map.of("EST", "America/New_York", "GMT+0", "GMT", "GMT-0", "GMT", "HST", "US/Hawaii", "MST", "America/Phoenix", "ROC", "Asia/Taipei");
    @Getter
    private final Aggregation aggregation;
    @Getter
    private final IntervalType intervalType;
    @Getter
    private final ZoneId tzId;

    private final long interval;

    public static AggregationParams none() {
        return new AggregationParams(Aggregation.NONE, null, null, 0L);
    }

    public static AggregationParams milliseconds(Aggregation aggregationType, long aggregationIntervalMs) {
        return new AggregationParams(aggregationType, IntervalType.MILLISECONDS, null, aggregationIntervalMs);
    }

    public static AggregationParams calendar(Aggregation aggregationType, IntervalType intervalType, String tzIdStr) {
        return calendar(aggregationType, intervalType, getZoneId(tzIdStr));
    }

    public static AggregationParams calendar(Aggregation aggregationType, IntervalType intervalType, ZoneId tzId) {
        return new AggregationParams(aggregationType, intervalType, tzId, 0L);
    }

    public static AggregationParams of(Aggregation aggregation, IntervalType intervalType, ZoneId tzId, long interval) {
        return new AggregationParams(aggregation, intervalType, tzId, interval);
    }

    public long getInterval() {
        if (intervalType == null) {
            return 0L;
        } else {
            switch (intervalType) {
                case WEEK:
                case WEEK_ISO:
                    return TimeUnit.DAYS.toMillis(7);
                case MONTH:
                    return TimeUnit.DAYS.toMillis(30);
                case QUARTER:
                    return TimeUnit.DAYS.toMillis(90);
                default:
                    return interval;
            }
        }
    }

    private static ZoneId getZoneId(String tzIdStr) {
        if (StringUtils.isEmpty(tzIdStr)) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(tzIdStr, TZ_LINKS);
        } catch (DateTimeException e) {
            log.warn("[{}] Failed to convert the time zone. Fallback to default.", tzIdStr);
            return ZoneId.systemDefault();
        }
    }
}
