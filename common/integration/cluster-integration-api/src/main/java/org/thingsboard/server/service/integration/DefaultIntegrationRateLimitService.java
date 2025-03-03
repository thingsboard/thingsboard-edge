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
package org.thingsboard.server.service.integration;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DeduplicationUtil;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.queue.util.TbCoreOrIntegrationExecutorComponent;

import java.util.function.Supplier;

@Service
@TbCoreOrIntegrationExecutorComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultIntegrationRateLimitService implements IntegrationRateLimitService {

    @Value("${event.debug.rate_limits.enabled}")
    private boolean eventRateLimitsEnabled;

    @Value("${event.debug.rate_limits.integration}")
    private String integrationEventsRateLimitsConf;

    @Value("${event.debug.rate_limits.converter}")
    private String converterEventsRateLimitsConf;

    @Value("#{${cache.rateLimits.timeToLiveInMinutes} * 60 * 1000}")
    private long deduplicationDuration;

    private final RateLimitService rateLimitService;

    @Override
    public void checkLimit(TenantId tenantId, Supplier<String> msg) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", tenantId, msg.get());
        }

        if (!rateLimitService.checkRateLimit(LimitedApi.INTEGRATION_MSGS_PER_TENANT, tenantId)) {
            if (log.isTraceEnabled()) {
                log.trace("[{}] Tenant level rate limit detected: {}", tenantId, msg.get());
            }
            throw new TbRateLimitsException(EntityType.TENANT);
        }
    }

    @Override
    public void checkLimitPerDevice(TenantId tenantId, String deviceName, Supplier<String> msg) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", deviceName, msg.get());
        }

        if (!rateLimitService.checkRateLimit(LimitedApi.INTEGRATION_MSGS_PER_DEVICE, tenantId, Pair.of(tenantId, deviceName))) {
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Device level rate limit detected: {}", tenantId, deviceName, msg.get());
            }
            throw new TbRateLimitsException(EntityType.DEVICE);
        }
    }

    @Override
    public void checkLimitPerAsset(TenantId tenantId, String assetName, Supplier<String> msg) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", assetName, msg.get());
        }

        if (!rateLimitService.checkRateLimit(LimitedApi.INTEGRATION_MSGS_PER_ASSET, tenantId, Pair.of(tenantId, assetName))) {
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Asset level rate limit detected: {}", tenantId, assetName, msg.get());
            }
            throw new TbRateLimitsException(EntityType.ASSET);
        }
    }

    @Override
    public boolean checkLimit(TenantId tenantId, IntegrationId integrationId, boolean throwException) {
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Processing integration debug event msg.", tenantId, integrationId);
        }
        if (!eventRateLimitsEnabled) {
            return true;
        }

        if (!rateLimitService.checkRateLimit(LimitedApi.INTEGRATION_EVENTS, (Object) tenantId, integrationEventsRateLimitsConf)) {
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Integration level debug rate limit detected.", tenantId, integrationId);
            }
            if (throwException) {
                throw new TbRateLimitsException(EntityType.INTEGRATION, "Integration debug rate limits reached!");
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean checkLimit(TenantId tenantId, ConverterId converterId, boolean throwException) {
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Processing converter debug event msg.", tenantId, converterId);
        }
        if (!eventRateLimitsEnabled) {
            return true;
        }

        if (!rateLimitService.checkRateLimit(LimitedApi.CONVERTER_EVENTS, (Object) tenantId, converterEventsRateLimitsConf)) {
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Converter level debug rate limit detected.", tenantId, converterId);
            }
            if (throwException) {
                throw new TbRateLimitsException(EntityType.CONVERTER, "Converter debug rate limits reached!");
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean alreadyProcessed(EntityId entityId, EntityType entityType) {
        var deduplicationKey = DeduplicationKey.of(entityId, entityType);
        return DeduplicationUtil.alreadyProcessed(deduplicationKey, deduplicationDuration);
    }

    @Data(staticConstructor = "of")
    private static class DeduplicationKey {
        private final EntityId entityId;
        private final EntityType entityType;
    }

}
