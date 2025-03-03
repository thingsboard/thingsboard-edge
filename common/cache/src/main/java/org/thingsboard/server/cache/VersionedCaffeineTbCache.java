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
package org.thingsboard.server.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.util.TbPair;

import java.io.Serializable;

public abstract class VersionedCaffeineTbCache<K extends VersionedCacheKey, V extends Serializable & HasVersion> extends CaffeineTbTransactionalCache<K, V> implements VersionedTbCache<K, V> {

    public VersionedCaffeineTbCache(CacheManager cacheManager, String cacheName) {
        super(cacheManager, cacheName);
    }

    @Override
    public TbCacheValueWrapper<V> get(K key) {
        TbPair<Long, V> versionValuePair = doGet(key);
        if (versionValuePair != null) {
            return SimpleTbCacheValueWrapper.wrap(versionValuePair.getSecond());
        }
        return null;
    }

    @Override
    public void put(K key, V value) {
        Long version = getVersion(value);
        if (version == null) {
            return;
        }
        doPut(key, value, version);
    }

    private void doPut(K key, V value, Long version) {
        lock.lock();
        try {
            TbPair<Long, V> versionValuePair = doGet(key);
            if (versionValuePair == null || version > versionValuePair.getFirst()) {
                failAllTransactionsByKey(key);
                cache.put(key, wrapValue(value, version));
            }
        } finally {
            lock.unlock();
        }
    }

    private TbPair<Long, V> doGet(K key) {
        Cache.ValueWrapper source = cache.get(key);
        return source == null ? null : (TbPair<Long, V>) source.get();
    }

    @Override
    public void evict(K key) {
        lock.lock();
        try {
            failAllTransactionsByKey(key);
            cache.evict(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void evict(K key, Long version) {
        if (version == null) {
            return;
        }
        doPut(key, null, version);
    }

    @Override
    void doPutIfAbsent(K key, V value) {
        cache.putIfAbsent(key, wrapValue(value, getVersion(value)));
    }

    private TbPair<Long, V> wrapValue(V value, Long version) {
        return TbPair.of(version, value);
    }

}
