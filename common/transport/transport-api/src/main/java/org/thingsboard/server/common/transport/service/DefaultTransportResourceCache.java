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
package org.thingsboard.server.common.transport.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportResourceCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@TbTransportComponent
public class DefaultTransportResourceCache implements TransportResourceCache {

    private final Lock resourceFetchLock = new ReentrantLock();
    private final ConcurrentMap<ResourceCompositeKey, TbResource> resources = new ConcurrentHashMap<>();
    private final Set<ResourceCompositeKey> keys = ConcurrentHashMap.newKeySet();
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final TransportService transportService;

    public DefaultTransportResourceCache(DataDecodingEncodingService dataDecodingEncodingService, @Lazy TransportService transportService) {
        this.dataDecodingEncodingService = dataDecodingEncodingService;
        this.transportService = transportService;
    }

    @Override
    public Optional<TbResource> get(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        ResourceCompositeKey compositeKey = new ResourceCompositeKey(tenantId, resourceType, resourceKey);
        TbResource resource;

        if (keys.contains(compositeKey)) {
            resource = resources.get(compositeKey);
            if (resource == null) {
                resource = resources.get(compositeKey.getSystemKey());
            }
        } else {
            resourceFetchLock.lock();
            try {
                if (keys.contains(compositeKey)) {
                    resource = resources.get(compositeKey);
                    if (resource == null) {
                        resource = resources.get(compositeKey.getSystemKey());
                    }
                } else {
                    resource = fetchResource(compositeKey);
                    keys.add(compositeKey);
                }
            } finally {
                resourceFetchLock.unlock();
            }
        }

        return Optional.ofNullable(resource);
    }

    private TbResource fetchResource(ResourceCompositeKey compositeKey) {
        UUID tenantId = compositeKey.getTenantId().getId();
        TransportProtos.GetResourceRequestMsg.Builder builder = TransportProtos.GetResourceRequestMsg.newBuilder();
        builder
                .setTenantIdLSB(tenantId.getLeastSignificantBits())
                .setTenantIdMSB(tenantId.getMostSignificantBits())
                .setResourceType(compositeKey.resourceType.name())
                .setResourceKey(compositeKey.resourceKey);
        TransportProtos.GetResourceResponseMsg responseMsg = transportService.getResource(builder.build());

        Optional<TbResource> optionalResource = dataDecodingEncodingService.decode(responseMsg.getResource().toByteArray());
        if (optionalResource.isPresent()) {
            TbResource resource = optionalResource.get();
            resources.put(new ResourceCompositeKey(resource.getTenantId(), resource.getResourceType(), resource.getResourceKey()), resource);
            return resource;
        }

        return null;
    }

    @Override
    public void update(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        ResourceCompositeKey compositeKey = new ResourceCompositeKey(tenantId, resourceType, resourceKey);
        if (keys.contains(compositeKey) || resources.containsKey(compositeKey)) {
            fetchResource(compositeKey);
        }
    }

    @Override
    public void evict(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        ResourceCompositeKey compositeKey = new ResourceCompositeKey(tenantId, resourceType, resourceKey);
        keys.remove(compositeKey);
        resources.remove(compositeKey);
    }

    @Data
    private static class ResourceCompositeKey {
        private final TenantId tenantId;
        private final ResourceType resourceType;
        private final String resourceKey;

        public ResourceCompositeKey getSystemKey() {
            return new ResourceCompositeKey(TenantId.SYS_TENANT_ID, resourceType, resourceKey);
        }
    }
}
