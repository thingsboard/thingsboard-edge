/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import static org.thingsboard.server.common.data.CacheConstants.ATTRIBUTES_CACHE;

@Service
@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "true")
@Primary
@Slf4j
public class AttributesCacheWrapper {
    private final Cache attributesCache;

    public AttributesCacheWrapper(CacheManager cacheManager) {
        this.attributesCache = cacheManager.getCache(ATTRIBUTES_CACHE);
    }

    public Cache.ValueWrapper get(AttributeCacheKey attributeCacheKey) {
        try {
            return attributesCache.get(attributeCacheKey);
        } catch (Exception e) {
            log.debug("Failed to retrieve element from cache for key {}. Reason - {}.", attributeCacheKey, e.getMessage());
            return null;
        }
    }

    public void put(AttributeCacheKey attributeCacheKey, AttributeKvEntry attributeKvEntry) {
        try {
            attributesCache.put(attributeCacheKey, attributeKvEntry);
        } catch (Exception e) {
            log.debug("Failed to put element from cache for key {}. Reason - {}.", attributeCacheKey, e.getMessage());
        }
    }

    public void evict(AttributeCacheKey attributeCacheKey) {
        try {
            attributesCache.evict(attributeCacheKey);
        } catch (Exception e) {
            log.debug("Failed to evict element from cache for key {}. Reason - {}.", attributeCacheKey, e.getMessage());
        }
    }
}
