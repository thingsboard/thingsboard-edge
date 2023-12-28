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
package org.thingsboard.server.common.transport.limits;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@TbTransportComponent
@Slf4j
public class DefaultEntityLimitsCache implements EntityLimitsCache {

    private static final int DEVIATION = 10;
    private final Cache<EntityLimitKey, Boolean> cache;

    public DefaultEntityLimitsCache(@Value("${cache.entityLimits.timeToLiveInMinutes:5}") int ttl,
                                    @Value("${cache.entityLimits.maxSize:100000}") int maxSize) {
        // We use the 'random' expiration time to avoid peak loads.
        long mainPart = (TimeUnit.MINUTES.toNanos(ttl) / 100) * (100 - DEVIATION);
        long randomPart = (TimeUnit.MINUTES.toNanos(ttl) / 100) * DEVIATION;
        cache = Caffeine.newBuilder()
                .expireAfter(new Expiry<EntityLimitKey, Boolean>() {
                    @Override
                    public long expireAfterCreate(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime) {
                        return mainPart + (long) (randomPart * ThreadLocalRandom.current().nextDouble());
                    }

                    @Override
                    public long expireAfterUpdate(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .maximumSize(maxSize)
                .build();
    }

    @Override
    public boolean get(EntityLimitKey key) {
        var result = cache.getIfPresent(key);
        return result != null ? result : false;
    }

    @Override
    public void put(EntityLimitKey key, boolean value) {
        cache.put(key, value);
    }
}
