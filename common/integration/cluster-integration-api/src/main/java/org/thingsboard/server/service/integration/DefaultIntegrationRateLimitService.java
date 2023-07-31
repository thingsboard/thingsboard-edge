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
package org.thingsboard.server.service.integration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType.SOFT;

@Service
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-integration-executor'")
@Slf4j
public class DefaultIntegrationRateLimitService implements IntegrationRateLimitService {
    @Value("${integrations.rate_limits.enabled}")
    private boolean rateLimitEnabled;

    @Value("${integrations.rate_limits.tenant}")
    private String perTenantLimitsConf;

    @Value("${integrations.rate_limits.device}")
    private String perDevicesLimitsConf;

    @Value("${cache.rateLimits.timeToLiveInMinutes:60}")
    private int rateLimitsTtl;
    @Value("${cache.rateLimits.maxSize:100000}")
    private int rateLimitsCacheMaxSize;

    @Value("#{${cache.rateLimits.timeToLiveInMinutes} * 60 * 1000}")
    private long deduplicationDuration;

    private Cache<RateLimitKey, TbRateLimits> rateLimits;

    private ConcurrentMap<DeduplicationKey, Long> deduplicationCache;

    @PostConstruct
    private void init() {
        rateLimits = Caffeine.newBuilder()
                .expireAfterAccess(rateLimitsTtl, TimeUnit.MINUTES)
                .maximumSize(rateLimitsCacheMaxSize)
                .build();

        deduplicationCache = new ConcurrentReferenceHashMap<>(16, SOFT);
    }

    @Override
    public void checkLimit(TenantId tenantId, Supplier<String> msg) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", tenantId, msg.get());
        }
        if (!rateLimitEnabled) {
            return;
        }
        RateLimitKey key = new RateLimitKey(tenantId, null);
        TbRateLimits rateLimit = rateLimits.get(key, k -> new TbRateLimits(perTenantLimitsConf));
        if (!rateLimit.tryConsume()) {
            if (log.isTraceEnabled()) {
                log.trace("[{}] Tenant level rate limit detected: {}", tenantId, msg.get());
            }
            throw new TbRateLimitsException(EntityType.TENANT);
        }
    }

    @Override
    public void checkLimit(TenantId tenantId, String deviceName, Supplier<String> msg) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", deviceName, msg.get());
        }
        if (!rateLimitEnabled) {
            return;
        }

        RateLimitKey key = RateLimitKey.of(tenantId, deviceName);
        TbRateLimits rateLimit = rateLimits.get(key, k -> new TbRateLimits(perDevicesLimitsConf));
        if (!rateLimit.tryConsume()) {
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Device level rate limit detected: {}", tenantId, deviceName, msg.get());
            }
            throw new TbRateLimitsException(EntityType.DEVICE);
        }
    }

    public boolean alreadyProcessed(IntegrationId integrationId, EntityType entityType) {
        DeduplicationKey deduplicationKey = DeduplicationKey.of(integrationId, entityType);
        AtomicBoolean alreadyProcessed = new AtomicBoolean(false);

        deduplicationCache.compute(deduplicationKey, (key, lastProcessedTs) -> {
            if (lastProcessedTs != null) {
                long passed = System.currentTimeMillis() - lastProcessedTs;
                if (passed <= deduplicationDuration) {
                    alreadyProcessed.set(true);
                    return lastProcessedTs;
                }
            }

            return System.currentTimeMillis();
        });

        return alreadyProcessed.get();
    }

    @Data(staticConstructor = "of")
    private static class DeduplicationKey {
        private final IntegrationId integrationId;
        private final EntityType entityType;
    }

    @Data(staticConstructor = "of")
    private static class RateLimitKey {
        private final TenantId tenantId;
        private final String deviceName;
    }
}
