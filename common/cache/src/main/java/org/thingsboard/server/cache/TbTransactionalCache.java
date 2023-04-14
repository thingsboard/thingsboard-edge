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
package org.thingsboard.server.cache;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface TbTransactionalCache<K extends Serializable, V extends Serializable> {

    String getCacheName();

    TbCacheValueWrapper<V> get(K key);

    void put(K key, V value);

    void putIfAbsent(K key, V value);

    void evict(K key);

    void evict(Collection<K> keys);

    void evictOrPut(K key, V value);

    TbCacheTransaction<K, V> newTransactionForKey(K key);

    /**
     * Note that all keys should be in the same cache slot for redis. You may control the cache slot using '{}' bracers.
     * See CLUSTER KEYSLOT command for more details.
     * @param keys - list of keys to use
     * @return transaction object
     */
    TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys);

    default V getAndPutInTransaction(K key, Supplier<V> dbCall, boolean cacheNullValue) {
        TbCacheValueWrapper<V> cacheValueWrapper = get(key);
        if (cacheValueWrapper != null) {
            return cacheValueWrapper.get();
        }
        var cacheTransaction = newTransactionForKey(key);
        try {
            V dbValue = dbCall.get();
            if (dbValue != null || cacheNullValue) {
                cacheTransaction.putIfAbsent(key, dbValue);
                cacheTransaction.commit();
                return dbValue;
            } else {
                cacheTransaction.rollback();
                return null;
            }
        } catch (Throwable e) {
            cacheTransaction.rollback();
            throw e;
        }
    }

    default <R> R getAndPutInTransaction(K key, Supplier<R> dbCall, Function<V, R> cacheValueToResult, Function<R, V> dbValueToCacheValue, boolean cacheNullValue) {
        TbCacheValueWrapper<V> cacheValueWrapper = get(key);
        if (cacheValueWrapper != null) {
            var cacheValue = cacheValueWrapper.get();
            return cacheValue == null ? null : cacheValueToResult.apply(cacheValue);
        }
        var cacheTransaction = newTransactionForKey(key);
        try {
            R dbValue = dbCall.get();
            if (dbValue != null || cacheNullValue) {
                cacheTransaction.putIfAbsent(key, dbValueToCacheValue.apply(dbValue));
                cacheTransaction.commit();
                return dbValue;
            } else {
                cacheTransaction.rollback();
                return null;
            }
        } catch (Throwable e) {
            cacheTransaction.rollback();
            throw e;
        }
    }

}
