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
package org.thingsboard.server.service.notification.rule.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.notification.NotificationRuleService;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationRulesCache implements NotificationRulesCache {

    private final NotificationRuleService notificationRuleService;

    @Value("${cache.notificationRules.maxSize:1000}")
    private int cacheMaxSize;
    @Value("${cache.notificationRules.timeToLiveInMinutes:30}")
    private int cacheValueTtl;
    private Cache<String, List<NotificationRule>> cache;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @PostConstruct
    private void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterAccess(cacheValueTtl, TimeUnit.MINUTES)
                .build();
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onComponentLifecycleEvent(ComponentLifecycleMsg event) {
        switch (event.getEntityId().getEntityType()) {
            case NOTIFICATION_RULE:
                evict(event.getTenantId()); // TODO: evict by trigger type of the rule
                break;
            case TENANT:
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    lock.writeLock().lock(); // locking in case rules for tenant are fetched while evicting
                    try {
                        evict(event.getTenantId());
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
                break;
        }
    }

    @Override
    public List<NotificationRule> getEnabled(TenantId tenantId, NotificationRuleTriggerType triggerType) {
        lock.readLock().lock();
        try {
            log.trace("Retrieving notification rules of type {} for tenant {} from cache", triggerType, tenantId);
            return cache.get(key(tenantId, triggerType), k -> {
                List<NotificationRule> rules = notificationRuleService.findEnabledNotificationRulesByTenantIdAndTriggerType(tenantId, triggerType);
                log.trace("Fetched notification rules of type {} for tenant {} (count: {})", triggerType, tenantId, rules.size());
                return !rules.isEmpty() ? rules : Collections.emptyList();
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    public void evict(TenantId tenantId) {
        cache.invalidateAll(Arrays.stream(NotificationRuleTriggerType.values())
                .map(triggerType -> key(tenantId, triggerType))
                .collect(Collectors.toList()));
        log.trace("Evicted all notification rules for tenant {} from cache", tenantId);
    }

    private static String key(TenantId tenantId, NotificationRuleTriggerType triggerType) {
        return tenantId + "_" + triggerType;
    }

}
