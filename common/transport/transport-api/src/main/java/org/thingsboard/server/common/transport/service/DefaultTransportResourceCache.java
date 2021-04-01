/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.Resource;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportResourceCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
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
    private final ConcurrentMap<ResourceKey, Resource> resources = new ConcurrentHashMap<>();
    private final Set<ResourceKey> keys = ConcurrentHashMap.newKeySet();
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final TransportService transportService;

    public DefaultTransportResourceCache(DataDecodingEncodingService dataDecodingEncodingService, @Lazy TransportService transportService) {
        this.dataDecodingEncodingService = dataDecodingEncodingService;
        this.transportService = transportService;
    }

    @Override
    public Resource get(TenantId tenantId, ResourceType resourceType, String resourceId) {
        ResourceKey resourceKey = new ResourceKey(tenantId, resourceType, resourceId);
        Resource resource;

        if (keys.contains(resourceKey)) {
            resource = resources.get(resourceKey);
            if (resource == null) {
                resource = resources.get(resourceKey.getSystemKey());
            }
        } else {
            resourceFetchLock.lock();
            try {
                if (keys.contains(resourceKey)) {
                    resource = resources.get(resourceKey);
                    if (resource == null) {
                        resource = resources.get(resourceKey.getSystemKey());
                    }
                } else {
                    resource = fetchResource(resourceKey);
                    keys.add(resourceKey);
                }
            } finally {
                resourceFetchLock.unlock();
            }
        }

        return resource;
    }

    private Resource fetchResource(ResourceKey resourceKey) {
        UUID tenantId = resourceKey.getTenantId().getId();
        TransportProtos.GetResourceRequestMsg.Builder builder = TransportProtos.GetResourceRequestMsg.newBuilder();
        builder
                .setTenantIdLSB(tenantId.getLeastSignificantBits())
                .setTenantIdMSB(tenantId.getMostSignificantBits())
                .setResourceType(resourceKey.resourceType.name())
                .setResourceId(resourceKey.resourceId);
        TransportProtos.GetResourceResponseMsg responseMsg = transportService.getResource(builder.build());

        Optional<Resource> optionalResource = dataDecodingEncodingService.decode(responseMsg.getResource().toByteArray());
        if (optionalResource.isPresent()) {
            Resource resource = optionalResource.get();
            resources.put(new ResourceKey(resource.getTenantId(), resource.getResourceType(), resource.getResourceId()), resource);
            return resource;
        }

        return null;
    }

    @Override
    public void update(TenantId tenantId, ResourceType resourceType, String resourceId) {
        ResourceKey resourceKey = new ResourceKey(tenantId, resourceType, resourceId);
        if (keys.contains(resourceKey) || resources.containsKey(resourceKey)) {
            fetchResource(resourceKey);
        }
    }

    @Override
    public void evict(TenantId tenantId, ResourceType resourceType, String resourceId) {
        ResourceKey resourceKey = new ResourceKey(tenantId, resourceType, resourceId);
        keys.remove(resourceKey);
        resources.remove(resourceKey);
    }

    @Data
    private static class ResourceKey {
        private final TenantId tenantId;
        private final ResourceType resourceType;
        private final String resourceId;

        public ResourceKey getSystemKey() {
            return new ResourceKey(TenantId.SYS_TENANT_ID, resourceType, resourceId);
        }
    }
}
