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
package org.thingsboard.server.common.data.util;

import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionsUtil {
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * Returns new set with elements that are present in set B(new) but absent in set A(old).
     */
    public static <T> Set<T> diffSets(Set<T> a, Set<T> b) {
        return b.stream().filter(p -> !a.contains(p)).collect(Collectors.toSet());
    }

    public static <T> boolean contains(Collection<T> collection, T element) {
        return isNotEmpty(collection) && collection.contains(element);
    }

    public static <T> int countNonNull(T[] array) {
        int count = 0;
        for (T t : array) {
            if (t != null) count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> mapOf(Object... kvs) {
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            K key = (K) kvs[i];
            V value = (V) kvs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    public static  <K, V> Map<K, V> unmodifiableMapOf(Object... kvs) {
        return Collections.unmodifiableMap(mapOf(kvs));
    }

}
