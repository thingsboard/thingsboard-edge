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
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.FstStatsService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class FstStatsServiceImpl implements FstStatsService {
    private final ConcurrentHashMap<String, StatsCounter> encodeCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StatsCounter> decodeCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> encodeTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> decodeTimer = new ConcurrentHashMap<>();

    @Autowired
    private StatsFactory statsFactory;

    @Override
    public void incrementEncode(Class<?> clazz) {
        encodeCounters.computeIfAbsent(clazz.getSimpleName(), key -> statsFactory.createStatsCounter("fst_encode", key)).increment();
    }

    @Override
    public void incrementDecode(Class<?> clazz) {
        decodeCounters.computeIfAbsent(clazz.getSimpleName(), key -> statsFactory.createStatsCounter("fst_decode", key)).increment();
    }

    @Override
    public void recordEncodeTime(Class<?> clazz, long startTime) {
        encodeTimers.computeIfAbsent(clazz.getSimpleName(),
                key -> statsFactory.createTimer("fst_encode_time", "statsName", key)).record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordDecodeTime(Class<?> clazz, long startTime) {
        decodeTimer.computeIfAbsent(clazz.getSimpleName(),
                key -> statsFactory.createTimer("fst_decode_time", "statsName", key)).record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }

}
