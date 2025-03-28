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

import org.thingsboard.server.common.data.HasVersion;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

public interface VersionedTbCache<K extends VersionedCacheKey, V extends Serializable & HasVersion> extends TbTransactionalCache<K, V> {

    TbCacheValueWrapper<V> get(K key);

    default V get(K key, Supplier<V> supplier) {
        return get(key, supplier, true);
    }

    default V get(K key, Supplier<V> supplier, boolean putToCache) {
        return Optional.ofNullable(get(key))
                .map(TbCacheValueWrapper::get)
                .orElseGet(() -> {
                    V value = supplier.get();
                    if (putToCache) {
                        put(key, value);
                    }
                    return value;
                });
    }

    void put(K key, V value);

    void evict(K key);

    void evict(Collection<K> keys);

    void evict(K key, Long version);

    default Long getVersion(V value) {
        return null;
        /* version on edge is static to update cache correctly
        if (value == null) {
            return 0L;
        } else if (value.getVersion() != null) {
            return value.getVersion();
        } else {
            return null;
        }
         */
    }

}
