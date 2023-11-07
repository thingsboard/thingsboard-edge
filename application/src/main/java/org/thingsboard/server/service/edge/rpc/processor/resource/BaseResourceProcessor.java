/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.resource;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseResourceProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateTbResource(TenantId tenantId, TbResourceId tbResourceId, ResourceUpdateMsg resourceUpdateMsg, EdgeId edgeId) {
        boolean resourceKeyUpdated = false;
        try {
            boolean created = false;
            TbResource resource = resourceService.findResourceById(tenantId, tbResourceId);
            if (resource == null) {
                resource = new TbResource();
                if (resourceUpdateMsg.getIsSystem()) {
                    resource.setTenantId(TenantId.SYS_TENANT_ID);
                } else {
                    resource.setTenantId(tenantId);
                }
                resource.setCreatedTime(Uuids.unixTimestamp(tbResourceId.getId()));
                created = true;
            }
            String resourceKey = resourceUpdateMsg.getResourceKey();
            ResourceType resourceType = ResourceType.valueOf(resourceUpdateMsg.getResourceType());
            PageDataIterable<TbResource> resourcesIterable = new PageDataIterable<>(
                    link -> resourceService.findTenantResourcesByResourceTypeAndPageLink(tenantId, resourceType, link), 1024);
            for (TbResource tbResource : resourcesIterable) {
                if (tbResource.getResourceKey().equals(resourceUpdateMsg.getResourceKey()) && !tbResourceId.equals(tbResource.getId())) {
                    resourceKey = StringUtils.randomAlphabetic(15) + "_" + resourceKey;
                    log.warn("[{}] Resource with resource type {} and key {} already exists. Renaming resource key to {}",
                            tenantId, resourceType, resourceUpdateMsg.getResourceKey(), resourceKey);
                    resourceKeyUpdated = true;
                }
            }
            resource.setTitle(resourceUpdateMsg.getTitle());
            resource.setResourceKey(resourceKey);
            resource.setResourceType(resourceType);
            resource.setFileName(resourceUpdateMsg.getFileName());
            resource.setData(resourceUpdateMsg.hasData() ? resourceUpdateMsg.getData() : null);
            resource.setEtag(resourceUpdateMsg.hasEtag() ? resourceUpdateMsg.getEtag() : null);
            resourceValidator.validate(resource, TbResourceInfo::getTenantId);
            if (created) {
                resource.setId(tbResourceId);
            }
            resourceService.saveResource(resource, edgeId);
        } catch (Exception e) {
            log.error("[{}] Failed to process resource update msg [{}]", tenantId, resourceUpdateMsg, e);
            throw e;
        }
        return resourceKeyUpdated;
    }
}
