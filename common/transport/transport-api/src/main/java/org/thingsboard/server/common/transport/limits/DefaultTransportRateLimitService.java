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
package org.thingsboard.server.common.transport.limits;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
@TbTransportComponent
@Slf4j
public class DefaultTransportRateLimitService implements TransportRateLimitService {

    private final static DummyTransportRateLimit ALLOW = new DummyTransportRateLimit();
    private final ConcurrentMap<TenantId, Boolean> tenantAllowed = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, Set<DeviceId>> tenantDevices = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, EntityTransportRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, EntityTransportRateLimits> perDeviceLimits = new ConcurrentHashMap<>();
    private final Map<InetAddress, InetAddressRateLimitStats> ipMap = new ConcurrentHashMap<>();

    private final TransportTenantProfileCache tenantProfileCache;

    @Value("${transport.rate_limits.ip_limits_enabled:false}")
    private boolean ipRateLimitsEnabled;
    @Value("${transport.rate_limits.max_wrong_credentials_per_ip:10}")
    private int maxWrongCredentialsPerIp;
    @Value("${transport.rate_limits.ip_block_timeout:60000}")
    private long ipBlockTimeout;

    public DefaultTransportRateLimitService(TransportTenantProfileCache tenantProfileCache) {
        this.tenantProfileCache = tenantProfileCache;
    }

    @Override
    public EntityType checkLimits(TenantId tenantId, DeviceId deviceId, int dataPoints) {
        if (!tenantAllowed.getOrDefault(tenantId, Boolean.TRUE)) {
            return EntityType.API_USAGE_STATE;
        }
        if (!checkEntityRateLimit(dataPoints, getTenantRateLimits(tenantId))) {
            return EntityType.TENANT;
        }
        if (!checkEntityRateLimit(dataPoints, getDeviceRateLimits(tenantId, deviceId))) {
            return EntityType.DEVICE;
        }
        return null;
    }

    private boolean checkEntityRateLimit(int dataPoints, EntityTransportRateLimits tenantLimits) {
        if (dataPoints > 0) {
            return tenantLimits.getTelemetryMsgRateLimit().tryConsume() && tenantLimits.getTelemetryDataPointsRateLimit().tryConsume(dataPoints);
        } else {
            return tenantLimits.getRegularMsgRateLimit().tryConsume();
        }
    }

    @Override
    public void update(TenantProfileUpdateResult update) {
        log.info("Received tenant profile update: {}", update.getProfile());
        EntityTransportRateLimits tenantRateLimitPrototype = createRateLimits(update.getProfile(), true);
        EntityTransportRateLimits deviceRateLimitPrototype = createRateLimits(update.getProfile(), false);
        for (TenantId tenantId : update.getAffectedTenants()) {
            mergeLimits(tenantId, tenantRateLimitPrototype, perTenantLimits::get, perTenantLimits::put);
            tenantDevices.get(tenantId).forEach(deviceId -> {
                mergeLimits(deviceId, deviceRateLimitPrototype, perDeviceLimits::get, perDeviceLimits::put);
            });
        }
    }

    @Override
    public void update(TenantId tenantId) {
        EntityTransportRateLimits tenantRateLimitPrototype = createRateLimits(tenantProfileCache.get(tenantId), true);
        EntityTransportRateLimits deviceRateLimitPrototype = createRateLimits(tenantProfileCache.get(tenantId), false);
        mergeLimits(tenantId, tenantRateLimitPrototype, perTenantLimits::get, perTenantLimits::put);
        tenantDevices.get(tenantId).forEach(deviceId -> {
            mergeLimits(deviceId, deviceRateLimitPrototype, perDeviceLimits::get, perDeviceLimits::put);
        });
    }

    @Override
    public void remove(TenantId tenantId) {
        perTenantLimits.remove(tenantId);
        tenantDevices.remove(tenantId);
    }

    @Override
    public void remove(DeviceId deviceId) {
        perDeviceLimits.remove(deviceId);
        tenantDevices.values().forEach(set -> set.remove(deviceId));
    }

    @Override
    public void update(TenantId tenantId, boolean allowed) {
        tenantAllowed.put(tenantId, allowed);
    }

    @Override
    public boolean checkAddress(InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return true;
        }
        var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        return !stats.isBlocked() || (stats.getLastActivityTs() + ipBlockTimeout < System.currentTimeMillis());
    }

    @Override
    public void onAuthSuccess(InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return;
        }

        var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        stats.getLock().lock();
        try {
            stats.setLastActivityTs(System.currentTimeMillis());
            stats.setFailureCount(0);
            if (stats.isBlocked()) {
                stats.setBlocked(false);
                log.info("[{}] IP address un-blocked due to correct credentials.", address.getAddress());
            }
        } finally {
            stats.getLock().unlock();
        }
    }

    @Override
    public void onAuthFailure(InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return;
        }

        var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        stats.getLock().lock();
        try {
            stats.setLastActivityTs(System.currentTimeMillis());
            int failureCount = stats.getFailureCount() + 1;
            stats.setFailureCount(failureCount);
            if (failureCount >= maxWrongCredentialsPerIp) {
                log.info("[{}] IP address blocked due to constantly wrong credentials.", address.getAddress());
                stats.setBlocked(true);
            }
        } finally {
            stats.getLock().unlock();
        }
    }

    @Override
    public void invalidateRateLimitsIpTable(long sessionInactivityTimeout) {
        if (!ipRateLimitsEnabled) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        long expTime = currentTime - Math.max(sessionInactivityTimeout, ipBlockTimeout);
        for (var entry : ipMap.entrySet()) {
            var stats = entry.getValue();
            if (stats.getLastActivityTs() < expTime) {
                log.debug("[{}] IP address removed due to session inactivity timeout.", entry.getKey());
                ipMap.remove(entry.getKey());
            } else if (stats.isBlocked() && (stats.getLastActivityTs() + ipBlockTimeout < currentTime)) {
                log.info("[{}] IP address unblocked due ip block timeout.", entry.getKey());
                stats.setBlocked(false);
            }
        }
    }

    private <T extends EntityId> void mergeLimits(T entityId, EntityTransportRateLimits newRateLimits,
                                                  Function<T, EntityTransportRateLimits> getFunction,
                                                  BiConsumer<T, EntityTransportRateLimits> putFunction) {
        EntityTransportRateLimits oldRateLimits = getFunction.apply(entityId);
        if (oldRateLimits == null) {
            if (EntityType.TENANT.equals(entityId.getEntityType())) {
                log.info("[{}] New rate limits: {}", entityId, newRateLimits);
            } else {
                log.debug("[{}] New rate limits: {}", entityId, newRateLimits);
            }
            putFunction.accept(entityId, newRateLimits);
        } else {
            EntityTransportRateLimits updated = merge(oldRateLimits, newRateLimits);
            if (updated != null) {
                if (EntityType.TENANT.equals(entityId.getEntityType())) {
                    log.info("[{}] Updated rate limits: {}", entityId, updated);
                } else {
                    log.debug("[{}] Updated rate limits: {}", entityId, updated);
                }
                putFunction.accept(entityId, updated);
            }
        }
    }

    private EntityTransportRateLimits merge(EntityTransportRateLimits oldRateLimits, EntityTransportRateLimits newRateLimits) {
        boolean regularUpdate = !oldRateLimits.getRegularMsgRateLimit().getConfiguration().equals(newRateLimits.getRegularMsgRateLimit().getConfiguration());
        boolean telemetryMsgRateUpdate = !oldRateLimits.getTelemetryMsgRateLimit().getConfiguration().equals(newRateLimits.getTelemetryMsgRateLimit().getConfiguration());
        boolean telemetryDataPointUpdate = !oldRateLimits.getTelemetryDataPointsRateLimit().getConfiguration().equals(newRateLimits.getTelemetryDataPointsRateLimit().getConfiguration());
        if (regularUpdate || telemetryMsgRateUpdate || telemetryDataPointUpdate) {
            return new EntityTransportRateLimits(
                    regularUpdate ? newLimit(newRateLimits.getRegularMsgRateLimit().getConfiguration()) : oldRateLimits.getRegularMsgRateLimit(),
                    telemetryMsgRateUpdate ? newLimit(newRateLimits.getTelemetryMsgRateLimit().getConfiguration()) : oldRateLimits.getTelemetryMsgRateLimit(),
                    telemetryDataPointUpdate ? newLimit(newRateLimits.getTelemetryDataPointsRateLimit().getConfiguration()) : oldRateLimits.getTelemetryDataPointsRateLimit());
        } else {
            return null;
        }
    }

    private EntityTransportRateLimits createRateLimits(TenantProfile tenantProfile, boolean tenant) {
        TenantProfileData profileData = tenantProfile.getProfileData();
        DefaultTenantProfileConfiguration profile = (DefaultTenantProfileConfiguration) profileData.getConfiguration();
        if (profile == null) {
            return new EntityTransportRateLimits(ALLOW, ALLOW, ALLOW);
        } else {
            TransportRateLimit regularMsgRateLimit = newLimit(tenant ? profile.getTransportTenantMsgRateLimit() : profile.getTransportDeviceMsgRateLimit());
            TransportRateLimit telemetryMsgRateLimit = newLimit(tenant ? profile.getTransportTenantTelemetryMsgRateLimit() : profile.getTransportDeviceTelemetryMsgRateLimit());
            TransportRateLimit telemetryDpRateLimit = newLimit(tenant ? profile.getTransportTenantTelemetryDataPointsRateLimit() : profile.getTransportTenantTelemetryDataPointsRateLimit());
            return new EntityTransportRateLimits(regularMsgRateLimit, telemetryMsgRateLimit, telemetryDpRateLimit);
        }
    }

    private static TransportRateLimit newLimit(String config) {
        return StringUtils.isEmpty(config) ? ALLOW : new SimpleTransportRateLimit(config);
    }

    private EntityTransportRateLimits getTenantRateLimits(TenantId tenantId) {
        EntityTransportRateLimits limits = perTenantLimits.get(tenantId);
        if (limits == null) {
            limits = createRateLimits(tenantProfileCache.get(tenantId), true);
            perTenantLimits.put(tenantId, limits);
        }
        return limits;
    }

    private EntityTransportRateLimits getDeviceRateLimits(TenantId tenantId, DeviceId deviceId) {
        EntityTransportRateLimits limits = perDeviceLimits.get(deviceId);
        if (limits == null) {
            limits = createRateLimits(tenantProfileCache.get(tenantId), false);
            perDeviceLimits.put(deviceId, limits);
            tenantDevices.computeIfAbsent(tenantId, id -> ConcurrentHashMap.newKeySet()).add(deviceId);
        }
        return limits;
    }
}
