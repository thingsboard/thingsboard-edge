/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.transport.limits;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.TenantProfileData;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@ConditionalOnExpression("('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true') || '${service.type:null}'=='tb-transport'")
@Slf4j
public class DefaultTransportRateLimitService implements TransportRateLimitService {

    private final ConcurrentMap<TenantId, TransportRateLimit[]> perTenantLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, TransportRateLimit[]> perDeviceLimits = new ConcurrentHashMap<>();

    private final TransportRateLimitFactory rateLimitFactory;
    private final TransportTenantProfileCache tenantProfileCache;

    public DefaultTransportRateLimitService(TransportRateLimitFactory rateLimitFactory, TransportTenantProfileCache tenantProfileCache) {
        this.rateLimitFactory = rateLimitFactory;
        this.tenantProfileCache = tenantProfileCache;
    }

    @Override
    public TransportRateLimit getRateLimit(TenantId tenantId, TransportRateLimitType limitType) {
        TransportRateLimit[] limits = perTenantLimits.get(tenantId);
        if (limits == null) {
            limits = fetchProfileAndInit(tenantId);
            perTenantLimits.put(tenantId, limits);
        }
        return limits[limitType.ordinal()];
    }

    @Override
    public TransportRateLimit getRateLimit(TenantId tenantId, DeviceId deviceId, TransportRateLimitType limitType) {
        TransportRateLimit[] limits = perDeviceLimits.get(deviceId);
        if (limits == null) {
            limits = fetchProfileAndInit(tenantId);
            perDeviceLimits.put(deviceId, limits);
        }
        return limits[limitType.ordinal()];
    }

    @Override
    public void update(TenantProfileUpdateResult update) {
        TransportRateLimit[] newLimits = createTransportRateLimits(update.getProfile());
        for (TenantId tenantId : update.getAffectedTenants()) {
            mergeLimits(tenantId, newLimits);
        }
    }

    @Override
    public void update(TenantId tenantId) {
        mergeLimits(tenantId, fetchProfileAndInit(tenantId));
    }

    public void mergeLimits(TenantId tenantId, TransportRateLimit[] newRateLimits) {
        TransportRateLimit[] oldRateLimits = perTenantLimits.get(tenantId);
        if (oldRateLimits == null) {
            perTenantLimits.put(tenantId, newRateLimits);
        } else {
            for (int i = 0; i < TransportRateLimitType.values().length; i++) {
                TransportRateLimit newLimit = newRateLimits[i];
                TransportRateLimit oldLimit = oldRateLimits[i];
                if (newLimit != null && (oldLimit == null || !oldLimit.getConfiguration().equals(newLimit.getConfiguration()))) {
                    oldRateLimits[i] = newLimit;
                }
            }
        }
    }

    @Override
    public void remove(TenantId tenantId) {
        perTenantLimits.remove(tenantId);
    }

    @Override
    public void remove(DeviceId deviceId) {
        perDeviceLimits.remove(deviceId);
    }

    private TransportRateLimit[] fetchProfileAndInit(TenantId tenantId) {
        return perTenantLimits.computeIfAbsent(tenantId, tmp -> createTransportRateLimits(tenantProfileCache.get(tenantId)));
    }

    private TransportRateLimit[] createTransportRateLimits(TenantProfile tenantProfile) {
        TenantProfileData profileData = tenantProfile.getProfileData();
        TransportRateLimit[] rateLimits = new TransportRateLimit[TransportRateLimitType.values().length];
        for (TransportRateLimitType type : TransportRateLimitType.values()) {
            rateLimits[type.ordinal()] = rateLimitFactory.create(type, profileData.getProperties().get(type.getConfigurationKey()));
        }
        return rateLimits;
    }
}
