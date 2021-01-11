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
package org.thingsboard.server.service.integration.rpc;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.IntegrationId;

import static org.thingsboard.server.common.data.CacheConstants.REMOTE_INTEGRATIONS_CACHE;

@Service
public class DefaultRemoteIntegrationSessionService implements RemoteIntegrationSessionService {

    @Cacheable(cacheNames = REMOTE_INTEGRATIONS_CACHE, key = "{#integrationId}")
    @Override
    public IntegrationSession findIntegrationSession(IntegrationId integrationId) {
        return null;
    }

    @CachePut(cacheNames = REMOTE_INTEGRATIONS_CACHE, key = "{#integrationId}")
    @Override
    public IntegrationSession putIntegrationSession(IntegrationId integrationId, IntegrationSession session) {
        return session;
    }

    @CacheEvict(cacheNames = REMOTE_INTEGRATIONS_CACHE, key = "{#integrationId}")
    @Override
    public void removeIntegrationSession(IntegrationId integrationId) {

    }
}
