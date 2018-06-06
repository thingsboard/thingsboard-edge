/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.common.transport.quota.inmemory;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public abstract class KeyBasedIntervalRegistry {

    private final Map<String, IntervalCount> hostCounts = new ConcurrentHashMap<>();
    private final long intervalDurationMs;
    private final long ttlMs;
    private final Set<String> whiteList;
    private final Set<String> blackList;

    public KeyBasedIntervalRegistry(long intervalDurationMs, long ttlMs, String whiteList, String blackList, String name) {
        this.intervalDurationMs = intervalDurationMs;
        this.ttlMs = ttlMs;
        this.whiteList = Sets.newHashSet(StringUtils.split(whiteList, ','));
        this.blackList = Sets.newHashSet(StringUtils.split(blackList, ','));
        validate(name);
    }

    private void validate(String name) {
        if (ttlMs < intervalDurationMs) {
            log.warn("TTL for {} IntervalRegistry [{}] smaller than interval duration [{}]", name, ttlMs, intervalDurationMs);
        }
        log.info("Start {} KeyBasedIntervalRegistry with whitelist {}", name, whiteList);
        log.info("Start {} KeyBasedIntervalRegistry with blacklist {}", name, blackList);
    }

    public long tick(String clientHostId) {
        IntervalCount intervalCount = hostCounts.computeIfAbsent(clientHostId, s -> new IntervalCount(intervalDurationMs));
        long currentCount = intervalCount.resetIfExpiredAndTick();
        if (whiteList.contains(clientHostId)) {
            return 0;
        } else if (blackList.contains(clientHostId)) {
            return Long.MAX_VALUE;
        }
        return currentCount;
    }

    public void clean() {
        hostCounts.entrySet().removeIf(entry -> entry.getValue().silenceDuration() > ttlMs);
    }

    public Map<String, Long> getContent() {
        return hostCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        interval -> interval.getValue().getCount()));
    }
}
