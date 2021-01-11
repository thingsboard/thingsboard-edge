/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.integration.downlink;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.thingsboard.server.common.data.CacheConstants.DOWNLINK_CACHE;

/**
 * Created by ashvayka on 22.02.18.
 */
@Service
@Slf4j
public class DefaultDownlinkService implements DownlinkService {

    @Autowired
    private CacheManager cacheManager;

    @Cacheable(cacheNames = DOWNLINK_CACHE, key = "{#integrationId, #entityId}")
    @Override
    public DownLinkMsg get(IntegrationId integrationId, EntityId entityId) {
        return null;
    }

    @Override
    public DownLinkMsg put(IntegrationDownlinkMsg msg) {
        return getAndMerge(msg, DownLinkMsg::from, DownLinkMsg::merge);
    }

    @CacheEvict(cacheNames = DOWNLINK_CACHE, key = "{#integrationId, #entityId}")
    @Override
    public void remove(IntegrationId integrationId, EntityId entityId) {

    }

    private <T extends IntegrationDownlinkMsg> DownLinkMsg getAndMerge(T msg, Function<T, DownLinkMsg> from, BiFunction<DownLinkMsg, T, DownLinkMsg> merge) {
        Cache cache = cacheManager.getCache(DOWNLINK_CACHE);
        List<Object> key = new ArrayList<>();
        key.add(msg.getIntegrationId());
        key.add(msg.getEntityId());

        DownLinkMsg result = cache.get(key, DownLinkMsg.class);

        if (result == null) {
            result = from.apply(msg);
        } else {
            result = merge.apply(result, msg);
        }

        cache.put(key, result);
        return result;
    }
}
