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
package org.thingsboard.server.queue.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.queue.util.PropertyUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType.SOFT;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationDeduplicationService implements NotificationDeduplicationService {

    private Map<NotificationRuleTriggerType, Long> deduplicationDurations;

    @Autowired(required = false)
    private CacheManager cacheManager;
    private final ConcurrentMap<String, Long> localCache = new ConcurrentReferenceHashMap<>(16, SOFT);

    @Override
    public boolean alreadyProcessed(NotificationRuleTrigger trigger) {
        String deduplicationKey = trigger.getDeduplicationKey();
        return alreadyProcessed(trigger, deduplicationKey, true);
    }

    @Override
    public boolean alreadyProcessed(NotificationRuleTrigger trigger, NotificationRule rule) {
        String deduplicationKey = getDeduplicationKey(trigger, rule);
        return alreadyProcessed(trigger, deduplicationKey, false);
    }

    private boolean alreadyProcessed(NotificationRuleTrigger trigger, String deduplicationKey, boolean onlyLocalCache) {
        Long lastProcessedTs = localCache.get(deduplicationKey);
        if (lastProcessedTs == null && !onlyLocalCache) {
            Cache externalCache = getExternalCache();
            if (externalCache != null) {
                lastProcessedTs = externalCache.get(deduplicationKey, Long.class);
            } else {
                log.warn("Sent notifications cache is not set up");
            }
        }

        boolean alreadyProcessed = false;
        long deduplicationDuration = getDeduplicationDuration(trigger);
        if (lastProcessedTs != null) {
            long passed = System.currentTimeMillis() - lastProcessedTs;
            log.trace("Deduplicating trigger {} by key '{}'. Deduplication duration: {} ms, passed: {} ms",
                    trigger.getType(), deduplicationKey, deduplicationDuration, passed);
            if (deduplicationDuration == 0 || passed <= deduplicationDuration) {
                alreadyProcessed = true;
            }
        }

        if (!alreadyProcessed) {
            lastProcessedTs = System.currentTimeMillis();
        }
        localCache.put(deduplicationKey, lastProcessedTs);
        if (!onlyLocalCache) {
            if (!alreadyProcessed || deduplicationDuration == 0) {
                // if lastProcessedTs is changed or if deduplicating infinitely (so that cache value not removed by ttl)
                Cache externalCache = getExternalCache();
                if (externalCache != null) {
                    externalCache.put(deduplicationKey, lastProcessedTs);
                }
            }
        }
        return alreadyProcessed;
    }

    public static String getDeduplicationKey(NotificationRuleTrigger trigger, NotificationRule rule) {
        return String.join("_", trigger.getDeduplicationKey(), rule.getDeduplicationKey());
    }

    private long getDeduplicationDuration(NotificationRuleTrigger trigger) {
        return deduplicationDurations.computeIfAbsent(trigger.getType(), triggerType -> {
            return trigger.getDefaultDeduplicationDuration();
        });
    }

    private Cache getExternalCache() {
        return Optional.ofNullable(cacheManager)
                .map(cacheManager -> cacheManager.getCache(CacheConstants.SENT_NOTIFICATIONS_CACHE))
                .orElse(null);
    }

    @Autowired
    public void setDeduplicationDurations(@Value("${notification_system.rules.deduplication_durations:}")
                                          String deduplicationDurationsStr) {
        this.deduplicationDurations = new HashMap<>();
        PropertyUtils.getProps(deduplicationDurationsStr).forEach((triggerType, duration) -> {
            this.deduplicationDurations.put(NotificationRuleTriggerType.valueOf(triggerType), Long.parseLong(duration));
        });
    }

}
