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
package org.thingsboard.server.dao.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisSslCredentials;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationCacheKey;
import org.thingsboard.server.dao.relation.RelationRedisCache;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RelationRedisCache.class, CacheSpecsMap.class, TBRedisCacheConfiguration.class})
@TestPropertySource(properties = {
        "cache.type=redis",
        "cache.specs.relations.timeToLiveInMinutes=1440",
        "cache.specs.relations.maxSize=0",
})
@Slf4j
public class RedisTbTransactionalCacheTest {

    @MockBean
    private RelationRedisCache relationRedisCache;
    @MockBean
    private RedisConnectionFactory connectionFactory;
    @MockBean
    private RedisConnection redisConnection;
    @MockBean
    private RedisSslCredentials redisSslCredentials;

    @Test
    public void testNoOpWhenCacheDisabled() {
        when(connectionFactory.getConnection()).thenReturn(redisConnection);

        relationRedisCache.put(createRelationCacheKey(), null);
        relationRedisCache.putIfAbsent(createRelationCacheKey(), null);
        relationRedisCache.evict(createRelationCacheKey());
        relationRedisCache.evict(List.of(createRelationCacheKey()));
        relationRedisCache.getAndPutInTransaction(createRelationCacheKey(), null, false);
        relationRedisCache.getAndPutInTransaction(createRelationCacheKey(), null, null, null, false);
        relationRedisCache.getOrFetchFromDB(createRelationCacheKey(), null, false, false);

        verify(connectionFactory, never()).getConnection();
        verifyNoInteractions(redisConnection);
    }

    private RelationCacheKey createRelationCacheKey() {
        return new RelationCacheKey(new DeviceId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()), null, RelationTypeGroup.COMMON);
    }

}
