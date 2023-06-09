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
package org.thingsboard.server.service.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
@Slf4j
public class DefaultTbAssetProfileCache implements TbAssetProfileCache {

    private final Lock assetProfileFetchLock = new ReentrantLock();
    private final AssetProfileService assetProfileService;
    private final AssetService assetService;

    private final ConcurrentMap<AssetProfileId, AssetProfile> assetProfilesMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<AssetId, AssetProfileId> assetsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, Consumer<AssetProfile>>> profileListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, BiConsumer<AssetId, AssetProfile>>> assetProfileListeners = new ConcurrentHashMap<>();

    public DefaultTbAssetProfileCache(AssetProfileService assetProfileService, AssetService assetService) {
        this.assetProfileService = assetProfileService;
        this.assetService = assetService;
    }

    @Override
    public AssetProfile get(TenantId tenantId, AssetProfileId assetProfileId) {
        AssetProfile profile = assetProfilesMap.get(assetProfileId);
        if (profile == null) {
            assetProfileFetchLock.lock();
            try {
                profile = assetProfilesMap.get(assetProfileId);
                if (profile == null) {
                    profile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
                    if (profile != null) {
                        assetProfilesMap.put(assetProfileId, profile);
                        log.debug("[{}] Fetch asset profile into cache: {}", profile.getId(), profile);
                    }
                }
            } finally {
                assetProfileFetchLock.unlock();
            }
        }
        log.trace("[{}] Found asset profile in cache: {}", assetProfileId, profile);
        return profile;
    }

    @Override
    public AssetProfile get(TenantId tenantId, AssetId assetId) {
        AssetProfileId profileId = assetsMap.get(assetId);
        if (profileId == null) {
            Asset asset = assetService.findAssetById(tenantId, assetId);
            if (asset != null) {
                profileId = asset.getAssetProfileId();
                assetsMap.put(assetId, profileId);
            } else {
                return null;
            }
        }
        return get(tenantId, profileId);
    }

    @Override
    public void evict(TenantId tenantId, AssetProfileId profileId) {
        AssetProfile oldProfile = assetProfilesMap.remove(profileId);
        log.debug("[{}] evict asset profile from cache: {}", profileId, oldProfile);
        AssetProfile newProfile = get(tenantId, profileId);
        if (newProfile != null) {
            notifyProfileListeners(newProfile);
        }
    }

    @Override
    public void evict(TenantId tenantId, AssetId assetId) {
        AssetProfileId old = assetsMap.remove(assetId);
        if (old != null) {
            AssetProfile newProfile = get(tenantId, assetId);
            if (newProfile == null || !old.equals(newProfile.getId())) {
                notifyAssetListeners(tenantId, assetId, newProfile);
            }
        }
    }

    @Override
    public void addListener(TenantId tenantId, EntityId listenerId,
                            Consumer<AssetProfile> profileListener,
                            BiConsumer<AssetId, AssetProfile> assetListener) {
        if (profileListener != null) {
            profileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, profileListener);
        }
        if (assetListener != null) {
            assetProfileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, assetListener);
        }
    }

    @Override
    public AssetProfile find(AssetProfileId assetProfileId) {
        return assetProfileService.findAssetProfileById(TenantId.SYS_TENANT_ID, assetProfileId);
    }

    @Override
    public AssetProfile findOrCreateAssetProfile(TenantId tenantId, String profileName) {
        return assetProfileService.findOrCreateAssetProfile(tenantId, profileName);
    }

    @Override
    public void removeListener(TenantId tenantId, EntityId listenerId) {
        ConcurrentMap<EntityId, Consumer<AssetProfile>> tenantListeners = profileListeners.get(tenantId);
        if (tenantListeners != null) {
            tenantListeners.remove(listenerId);
        }
        ConcurrentMap<EntityId, BiConsumer<AssetId, AssetProfile>> assetListeners = assetProfileListeners.get(tenantId);
        if (assetListeners != null) {
            assetListeners.remove(listenerId);
        }
    }

    private void notifyProfileListeners(AssetProfile profile) {
        ConcurrentMap<EntityId, Consumer<AssetProfile>> tenantListeners = profileListeners.get(profile.getTenantId());
        if (tenantListeners != null) {
            tenantListeners.forEach((id, listener) -> listener.accept(profile));
        }
    }

    private void notifyAssetListeners(TenantId tenantId, AssetId assetId, AssetProfile profile) {
        if (profile != null) {
            ConcurrentMap<EntityId, BiConsumer<AssetId, AssetProfile>> tenantListeners = assetProfileListeners.get(tenantId);
            if (tenantListeners != null) {
                tenantListeners.forEach((id, listener) -> listener.accept(assetId, profile));
            }
        }
    }

}
