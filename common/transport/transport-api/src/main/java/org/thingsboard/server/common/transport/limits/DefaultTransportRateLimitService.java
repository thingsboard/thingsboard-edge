/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.HashSet;
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

    private final TransportTenantProfileCache tenantProfileCache;

    public DefaultTransportRateLimitService(TransportTenantProfileCache tenantProfileCache) {
        this.tenantProfileCache = tenantProfileCache;
    }

    @Override
    public EntityType checkLimits(TenantId tenantId, DeviceId deviceId, int dataPoints) {
        if (!tenantAllowed.getOrDefault(tenantId, Boolean.TRUE)) {
            return EntityType.TENANT;
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
