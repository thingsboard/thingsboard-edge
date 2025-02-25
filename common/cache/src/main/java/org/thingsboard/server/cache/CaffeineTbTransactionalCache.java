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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
public abstract class CaffeineTbTransactionalCache<K extends Serializable, V extends Serializable> implements TbTransactionalCache<K, V> {

    @Getter
    protected final String cacheName;
    protected final CaffeineCache cache;
    protected final Lock lock = new ReentrantLock();
    private final Map<K, Set<UUID>> objectTransactions = new HashMap<>();
    private final Map<UUID, CaffeineTbCacheTransaction<K, V>> transactions = new HashMap<>();

    public CaffeineTbTransactionalCache(CacheManager cacheManager, String cacheName) {
        this.cacheName = cacheName;
        this.cache = (CaffeineCache) Optional.ofNullable(cacheManager.getCache(cacheName))
                .orElseThrow(() -> new IllegalArgumentException("Cache '" + cacheName + "' is not configured"));;
    }

    @Override
    public TbCacheValueWrapper<V> get(K key) {
        return SimpleTbCacheValueWrapper.wrap(cache.get(key));
    }

    @Override
    public void put(K key, V value) {
        lock.lock();
        try {
            failAllTransactionsByKey(key);
            cache.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void putIfAbsent(K key, V value) {
        lock.lock();
        try {
            failAllTransactionsByKey(key);
            doPutIfAbsent(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void evict(K key) {
        lock.lock();
        try {
            failAllTransactionsByKey(key);
            doEvict(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void evict(Collection<K> keys) {
        lock.lock();
        try {
            keys.forEach(key -> {
                failAllTransactionsByKey(key);
                doEvict(key);
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void evictOrPut(K key, V value) {
        //No need to put the value in case of Caffeine, because evict will cancel concurrent transaction used to "get" the missing value from cache.
        evict(key);
    }

    @Override
    public void evictByPrefix(String prefix) {
        lock.lock();
        try {
            Set<K> keysToEvict = getKeysByPrefix(prefix);
            evict(keysToEvict);
        } finally {
            lock.unlock();
        }
    }

    private Set<K> getKeysByPrefix(String prefix) {
        Set<K> keysToEvict = new HashSet<>();
        cache.getNativeCache().asMap().forEach((key, value) -> {
            if (key.toString().startsWith(prefix)) {
                keysToEvict.add((K) key);
            }
        });
        return keysToEvict;
    }

    @Override
    public TbCacheTransaction<K, V> newTransactionForKey(K key) {
        return newTransaction(Collections.singletonList(key));
    }

    @Override
    public TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys) {
        return newTransaction(keys);
    }

    void doPutIfAbsent(K key, V value) {
        cache.putIfAbsent(key, value);
    }

    void doEvict(K key) {
        cache.evict(key);
    }

    TbCacheTransaction<K, V> newTransaction(List<K> keys) {
        lock.lock();
        try {
            var transaction = new CaffeineTbCacheTransaction<>(this, keys);
            var transactionId = transaction.getId();
            for (K key : keys) {
                objectTransactions.computeIfAbsent(key, k -> new HashSet<>()).add(transactionId);
            }
            transactions.put(transactionId, transaction);
            return transaction;
        } finally {
            lock.unlock();
        }
    }

    public boolean commit(UUID trId, Map<K, V> pendingPuts) {
        lock.lock();
        try {
            var tr = transactions.get(trId);
            var success = !tr.isFailed();
            if (success) {
                for (K key : tr.getKeys()) {
                    Set<UUID> otherTransactions = objectTransactions.get(key);
                    if (otherTransactions != null) {
                        for (UUID otherTrId : otherTransactions) {
                            if (trId == null || !trId.equals(otherTrId)) {
                                transactions.get(otherTrId).setFailed(true);
                            }
                        }
                    }
                }
                pendingPuts.forEach(this::doPutIfAbsent);
            }
            removeTransaction(trId);
            return success;
        } finally {
            lock.unlock();
        }
    }

    void rollback(UUID id) {
        lock.lock();
        try {
            removeTransaction(id);
        } finally {
            lock.unlock();
        }
    }

    private void removeTransaction(UUID id) {
        CaffeineTbCacheTransaction<K, V> transaction = transactions.remove(id);
        if (transaction != null) {
            for (var key : transaction.getKeys()) {
                Set<UUID> transactions = objectTransactions.get(key);
                if (transactions != null) {
                    transactions.remove(id);
                    if (transactions.isEmpty()) {
                        objectTransactions.remove(key);
                    }
                }
            }
        }
    }

    protected void failAllTransactionsByKey(K key) {
        Set<UUID> transactionsIds = objectTransactions.get(key);
        if (transactionsIds != null) {
            for (UUID otherTrId : transactionsIds) {
                transactions.get(otherTrId).setFailed(true);
            }
        }
    }

}
