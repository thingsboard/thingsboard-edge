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
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.ATTRIBUTES_CACHE;
import static org.thingsboard.server.dao.attributes.AttributeUtils.validate;

@Service
@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "true")
@Primary
@Slf4j
public class CachedAttributesService implements AttributesService {
    private static final String STATS_NAME = "attributes.cache";

    private final AttributesDao attributesDao;
    private final AttributesCacheWrapper cacheWrapper;
    private final DefaultCounter hitCounter;
    private final DefaultCounter missCounter;

    public CachedAttributesService(AttributesDao attributesDao,
                                   AttributesCacheWrapper cacheWrapper,
                                   StatsFactory statsFactory) {
        this.attributesDao = attributesDao;
        this.cacheWrapper = cacheWrapper;

        this.hitCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "hit");
        this.missCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "miss");
    }

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey);

        AttributeCacheKey attributeCacheKey = new AttributeCacheKey(scope, entityId, attributeKey);
        Cache.ValueWrapper cachedAttributeValue = cacheWrapper.get(attributeCacheKey);
        if (cachedAttributeValue != null) {
            hitCounter.increment();
            AttributeKvEntry cachedAttributeKvEntry = (AttributeKvEntry) cachedAttributeValue.get();
            return Futures.immediateFuture(Optional.ofNullable(cachedAttributeKvEntry));
        } else {
            missCounter.increment();
            ListenableFuture<Optional<AttributeKvEntry>> result = attributesDao.find(tenantId, entityId, scope, attributeKey);
            return Futures.transform(result, foundAttrKvEntry -> {
                // TODO: think if it's a good idea to store 'empty' attributes
                cacheWrapper.put(attributeCacheKey, foundAttrKvEntry.orElse(null));
                return foundAttrKvEntry;
            }, MoreExecutors.directExecutor());
        }
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, Collection<String> attributeKeys) {
        validate(entityId, scope);
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey));

        Map<String, Cache.ValueWrapper> wrappedCachedAttributes = findCachedAttributes(entityId, scope, attributeKeys);

        List<AttributeKvEntry> cachedAttributes = wrappedCachedAttributes.values().stream()
                .map(wrappedCachedAttribute -> (AttributeKvEntry) wrappedCachedAttribute.get())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (wrappedCachedAttributes.size() == attributeKeys.size()) {
            return Futures.immediateFuture(cachedAttributes);
        }

        Set<String> notFoundAttributeKeys = new HashSet<>(attributeKeys);
        notFoundAttributeKeys.removeAll(wrappedCachedAttributes.keySet());

        ListenableFuture<List<AttributeKvEntry>> result = attributesDao.find(tenantId, entityId, scope, notFoundAttributeKeys);
        return Futures.transform(result, foundInDbAttributes -> mergeDbAndCacheAttributes(entityId, scope, cachedAttributes, notFoundAttributeKeys, foundInDbAttributes), MoreExecutors.directExecutor());

    }

    private Map<String, Cache.ValueWrapper> findCachedAttributes(EntityId entityId, String scope, Collection<String> attributeKeys) {
        Map<String, Cache.ValueWrapper> cachedAttributes = new HashMap<>();
        for (String attributeKey : attributeKeys) {
            Cache.ValueWrapper cachedAttributeValue = cacheWrapper.get(new AttributeCacheKey(scope, entityId, attributeKey));
            if (cachedAttributeValue != null) {
                hitCounter.increment();
                cachedAttributes.put(attributeKey, cachedAttributeValue);
            } else {
                missCounter.increment();
            }
        }
        return cachedAttributes;
    }

    private List<AttributeKvEntry> mergeDbAndCacheAttributes(EntityId entityId, String scope, List<AttributeKvEntry> cachedAttributes, Set<String> notFoundAttributeKeys, List<AttributeKvEntry> foundInDbAttributes) {
        for (AttributeKvEntry foundInDbAttribute : foundInDbAttributes) {
            AttributeCacheKey attributeCacheKey = new AttributeCacheKey(scope, entityId, foundInDbAttribute.getKey());
            cacheWrapper.put(attributeCacheKey, foundInDbAttribute);
            notFoundAttributeKeys.remove(foundInDbAttribute.getKey());
        }
        for (String key : notFoundAttributeKeys){
            cacheWrapper.put(new AttributeCacheKey(scope, entityId, key), null);
        }
        List<AttributeKvEntry> mergedAttributes = new ArrayList<>(cachedAttributes);
        mergedAttributes.addAll(foundInDbAttributes);
        return mergedAttributes;
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, String scope) {
        validate(entityId, scope);
        return attributesDao.findAll(tenantId, entityId, scope);
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return attributesDao.findAllKeysByDeviceProfileId(tenantId, deviceProfileId);
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds) {
        return attributesDao.findAllKeysByEntityIds(tenantId, entityType, entityIds);
    }

    @Override
    public ListenableFuture<List<Void>> save(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        validate(entityId, scope);
        attributes.forEach(AttributeUtils::validate);

        List<ListenableFuture<Void>> saveFutures = attributes.stream().map(attribute -> attributesDao.save(tenantId, entityId, scope, attribute)).collect(Collectors.toList());
        ListenableFuture<List<Void>> future = Futures.allAsList(saveFutures);

        // TODO: can do if (attributesCache.get() != null) attributesCache.put() instead, but will be more twice more requests to cache
        List<String> attributeKeys = attributes.stream().map(KvEntry::getKey).collect(Collectors.toList());
        future.addListener(() -> evictAttributesFromCache(tenantId, entityId, scope, attributeKeys), MoreExecutors.directExecutor());
        return future;
    }

    @Override
    public ListenableFuture<List<Void>> removeAll(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        validate(entityId, scope);
        ListenableFuture<List<Void>> future = attributesDao.removeAll(tenantId, entityId, scope, attributeKeys);
        future.addListener(() -> evictAttributesFromCache(tenantId, entityId, scope, attributeKeys), MoreExecutors.directExecutor());
        return future;
    }

    private void evictAttributesFromCache(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        try {
            for (String attributeKey : attributeKeys) {
                cacheWrapper.evict(new AttributeCacheKey(scope, entityId, attributeKey));
            }
        } catch (Exception e) {
            log.error("[{}][{}] Failed to remove values from cache.", tenantId, entityId, e);
        }
    }
}
