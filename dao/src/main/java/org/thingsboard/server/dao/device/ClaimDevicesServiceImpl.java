/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
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
package org.thingsboard.server.dao.device;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.claim.ClaimData;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.common.data.CacheConstants.CLAIM_DEVICES_CACHE;
import static org.thingsboard.server.common.data.CacheConstants.ENTITY_OWNERS_CACHE;

@Service
@Slf4j
public class ClaimDevicesServiceImpl implements ClaimDevicesService {

    private static final String CLAIM_ATTRIBUTE_NAME = "claimingAllowed";

    @Autowired
    private DeviceService deviceService;
    @Autowired
    private AttributesService attributesService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private EntityGroupService entityGroupService;

    @Value("${security.claim.allowClaimingByDefault}")
    private boolean isAllowedClaimingByDefault;

    @Value("${security.claim.duration}")
    private long systemDurationMs;

    @Override
    public ListenableFuture<Void> registerClaimingInfo(TenantId tenantId, DeviceId deviceId, String secretKey, long durationMs) {
        ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(tenantId, deviceId);
        return Futures.transformAsync(deviceFuture, device -> {
            Cache cache = cacheManager.getCache(CLAIM_DEVICES_CACHE);
            List<Object> key = constructCacheKey(device.getId());

            if (isAllowedClaimingByDefault) {
                if (device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                    persistInCache(secretKey, durationMs, cache, key);
                    return Futures.immediateFuture(null);
                }
                log.warn("The device [{}] has been already claimed!", device.getName());
                throw new IllegalArgumentException();
            } else {
                ListenableFuture<List<AttributeKvEntry>> claimingAllowedFuture = attributesService.find(tenantId, device.getId(),
                        DataConstants.SERVER_SCOPE, Collections.singletonList(CLAIM_ATTRIBUTE_NAME));
                return Futures.transform(claimingAllowedFuture, list -> {
                    if (list != null && !list.isEmpty()) {
                        Optional<Boolean> claimingAllowedOptional = list.get(0).getBooleanValue();
                        if (claimingAllowedOptional.isPresent() && claimingAllowedOptional.get()
                                && device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                            persistInCache(secretKey, durationMs, cache, key);
                            return null;
                        }
                    }
                    log.warn("Failed to find claimingAllowed attribute for device or it is already claimed![{}]", device.getName());
                    throw new IllegalArgumentException();
                });
            }
        });
    }

    @Override
    public ListenableFuture<ClaimResult> claimDevice(Device device, CustomerId customerId, String secretKey) {
        List<Object> key = constructCacheKey(device.getId());
        Cache cache = cacheManager.getCache(CLAIM_DEVICES_CACHE);
        ClaimData claimData = cache.get(key, ClaimData.class);
        if (claimData != null) {
            long currTs = System.currentTimeMillis();
            if (currTs > claimData.getExpirationTime() || !secretKey.equals(claimData.getSecretKey())) {
                log.warn("The claiming timeout occurred or wrong 'secretKey' provided for the device [{}]", device.getName());
                cache.evict(key);
                return Futures.immediateFuture(new ClaimResult(null, ClaimResponse.FAILURE));
            } else {
                if (device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                    ListenableFuture<Void> future = Futures.transform(entityGroupService.findEntityGroupsForEntity(device.getTenantId(), device.getId()), entityGroupList -> {
                        for (EntityGroupId entityGroupId : entityGroupList) {
                            entityGroupService.removeEntityFromEntityGroup(device.getTenantId(), entityGroupId, device.getId());
                        }
                        entityGroupService.addEntityToEntityGroupAll(device.getTenantId(), customerId, device.getId());

                        device.setCustomerId(customerId);
                        device.setOwnerId(customerId);
                        deviceService.saveDevice(device);

                        Cache ownersCache = cacheManager.getCache(ENTITY_OWNERS_CACHE);
                        ownersCache.evict(getOwnersCacheKey(device.getId()));
                        ownersCache.evict(getOwnerCacheKey(device.getId()));
                        return null;
                    });
                    return Futures.transformAsync(future, input -> Futures.transform(removeClaimingSavedData(cache, key, device), result -> new ClaimResult(device, ClaimResponse.SUCCESS)));
                }
                return Futures.transform(removeClaimingSavedData(cache, key, device), result -> new ClaimResult(null, ClaimResponse.CLAIMED));
            }
        } else {
            log.warn("Failed to find the device's claiming message![{}]", device.getName());
            if (device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                return Futures.immediateFuture(new ClaimResult(null, ClaimResponse.FAILURE));
            } else {
                return Futures.immediateFuture(new ClaimResult(null, ClaimResponse.CLAIMED));
            }
        }
    }

    @Override
    public ListenableFuture<List<Void>> reClaimDevice(TenantId tenantId, Device device) {
        if (!device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            cacheEviction(device.getId());

            device.setCustomerId(null);
            deviceService.saveDevice(device);
            if (isAllowedClaimingByDefault) {
                return Futures.immediateFuture(Collections.emptyList());
            }
            return attributesService.save(tenantId, device.getId(), DataConstants.SERVER_SCOPE, Collections.singletonList(
                    new BaseAttributeKvEntry(new BooleanDataEntry(CLAIM_ATTRIBUTE_NAME, true),
                            System.currentTimeMillis())));
        }
        cacheEviction(device.getId());
        return Futures.immediateFuture(Collections.emptyList());
    }

    private List<Object> constructCacheKey(DeviceId deviceId) {
        return Collections.singletonList(deviceId);
    }

    private void persistInCache(String secretKey, long durationMs, Cache cache, List<Object> key) {
        ClaimData claimData = new ClaimData(secretKey,
                System.currentTimeMillis() + validateDurationMs(durationMs));
        cache.putIfAbsent(key, claimData);
    }

    private long validateDurationMs(long durationMs) {
        if (durationMs > 0L) {
            return durationMs;
        }
        return systemDurationMs;
    }

    private ListenableFuture<List<Void>> removeClaimingSavedData(Cache cache, List<Object> key, Device device) {
        cache.evict(key);
        if (isAllowedClaimingByDefault) {
            return Futures.immediateFuture(null);
        }
        return attributesService.removeAll(device.getTenantId(),
                device.getId(), DataConstants.SERVER_SCOPE, Collections.singletonList(CLAIM_ATTRIBUTE_NAME));
    }

    private void cacheEviction(DeviceId deviceId) {
        Cache cache = cacheManager.getCache(CLAIM_DEVICES_CACHE);
        cache.evict(constructCacheKey(deviceId));
    }

    private String getOwnersCacheKey(EntityId entityId) {
        return ENTITY_OWNERS_CACHE + "_" + entityId.getId().toString();
    }

    private String getOwnerCacheKey(EntityId entityId) {
        return ENTITY_OWNERS_CACHE + "_owner_" + entityId.getId().toString();
    }
}
