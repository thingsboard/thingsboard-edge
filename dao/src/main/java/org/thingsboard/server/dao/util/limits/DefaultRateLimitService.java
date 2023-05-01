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
package org.thingsboard.server.dao.util.limits;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.TenantProfileNotFoundException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DefaultRateLimitService implements RateLimitService {

    private final TbTenantProfileCache tenantProfileCache;

    public DefaultRateLimitService(TbTenantProfileCache tenantProfileCache,
                                   @Value("${cache.rateLimits.timeToLiveInMinutes:60}") int rateLimitsTtl,
                                   @Value("${cache.rateLimits.maxSize:100000}") int rateLimitsCacheMaxSize) {
        this.tenantProfileCache = tenantProfileCache;
        this.rateLimits = Caffeine.newBuilder()
                .expireAfterAccess(rateLimitsTtl, TimeUnit.MINUTES)
                .maximumSize(rateLimitsCacheMaxSize)
                .build();
    }

    private final Cache<RateLimitKey, TbRateLimits> rateLimits;

    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId) {
        return checkRateLimit(api, tenantId, tenantId);
    }

    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId, Object level) {
        if (tenantId.isSysTenantId()) {
            return true;
        }
        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        if (tenantProfile == null) {
            throw new TenantProfileNotFoundException(tenantId);
        }

        String rateLimitConfig = tenantProfile.getProfileConfiguration()
                .map(profileConfiguration -> api.getLimitConfig(profileConfiguration, level))
                .orElse(null);
        return checkRateLimit(api, level, rateLimitConfig);
    }

    @Override
    public boolean checkRateLimit(LimitedApi api, Object level, String rateLimitConfig) {
        RateLimitKey key = new RateLimitKey(api, level);
        if (StringUtils.isEmpty(rateLimitConfig)) {
            rateLimits.invalidate(key);
            return true;
        }
        log.trace("[{}] Checking rate limit for {} ({})", level, api, rateLimitConfig);

        TbRateLimits rateLimit = rateLimits.asMap().compute(key, (k, limit) -> {
            if (limit == null || !limit.getConfiguration().equals(rateLimitConfig)) {
                limit = new TbRateLimits(rateLimitConfig, api.isRefillRateLimitIntervally());
                log.trace("[{}] Created new rate limit bucket for {} ({})", level, api, rateLimitConfig);
            }
            return limit;
        });
        boolean success = rateLimit.tryConsume();
        if (!success) {
            log.debug("[{}] Rate limit exceeded for {} ({})", level, api, rateLimitConfig);
        }
        return success;
    }

    @Override
    public void cleanUp(LimitedApi api, Object level) {
        RateLimitKey key = new RateLimitKey(api, level);
        rateLimits.invalidate(key);
    }

    @Data(staticConstructor = "of")
    private static class RateLimitKey {
        private final LimitedApi api;
        private final Object level;
    }

}
