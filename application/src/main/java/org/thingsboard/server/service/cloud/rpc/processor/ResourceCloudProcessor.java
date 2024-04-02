/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.constructor.resource.ResourceMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.resource.BaseResourceProcessor;

import java.util.UUID;

@Component
@Slf4j
public class ResourceCloudProcessor extends BaseResourceProcessor {

    public ListenableFuture<Void> processResourceMsgFromCloud(TenantId tenantId, ResourceUpdateMsg resourceUpdateMsg) {
        TbResourceId tbResourceId = new TbResourceId(new UUID(resourceUpdateMsg.getIdMSB(), resourceUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (resourceUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    TbResource tbResource = JacksonUtil.fromString(resourceUpdateMsg.getEntity(), TbResource.class, true);
                    if (tbResource == null) {
                        throw new RuntimeException("[{" + tenantId + "}] resourceUpdateMsg {" + resourceUpdateMsg + "} cannot be converted to tb resource");
                    }
                    deleteSystemResourceIfAlreadyExists(tbResourceId, tbResource.getResourceType(), tbResource.getResourceKey());
                    Pair<Boolean, TbResourceId> resultPair = renamePreviousResource(tenantId, tbResourceId, tbResource.getResourceType(), tbResource.getResourceKey());
                    super.saveOrUpdateTbResource(tenantId, tbResourceId, resourceUpdateMsg);
                    if (resultPair.getFirst()) {
                        resourceService.deleteResource(tenantId, resultPair.getSecond());
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    TbResource tbResourceToDelete = resourceService.findResourceById(tenantId, tbResourceId);
                    if (tbResourceToDelete != null) {
                        resourceService.deleteResource(tenantId, tbResourceId);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(resourceUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    private Pair<Boolean, TbResourceId> renamePreviousResource(TenantId tenantId, TbResourceId tbResourceId, ResourceType resourceType, String resourceKey) {
        PageDataIterable<TbResource> resourcesIterable = new PageDataIterable<>(
                link -> resourceService.findTenantResourcesByResourceTypeAndPageLink(tenantId, resourceType, link), 1024);
        for (TbResource tbResource : resourcesIterable) {
            if (tbResource.getResourceKey().equals(resourceKey) && !tbResourceId.equals(tbResource.getId())) {
                tbResource.setResourceKey(StringUtils.randomAlphanumeric(15) + resourceKey);
                resourceService.saveResource(tbResource, false);
                return Pair.of(true, tbResource.getId());
            }
        }
        return Pair.of(false, new TbResourceId(UUID.randomUUID()));
    }

    private void deleteSystemResourceIfAlreadyExists(TbResourceId tbResourceId, ResourceType resourceType, String resourceKey) {
        PageDataIterable<TbResource> entityIdsIterator = new PageDataIterable<>(
                link -> resourceService.findAllTenantResources(TenantId.SYS_TENANT_ID, link), 1024);
        for (TbResource resource : entityIdsIterator) {
            if (resource.getResourceType().equals(resourceType)
                    && resource.getResourceKey().equals(resourceKey)
                    && !resource.getId().equals(tbResourceId)) {
                resourceService.deleteResource(TenantId.SYS_TENANT_ID, resource.getId());
                break;
            }
        }
    }

    public UplinkMsg convertResourceEventToUplink(CloudEvent cloudEvent, EdgeVersion edgeVersion) {
        TbResourceId tbResourceId = new TbResourceId(cloudEvent.getEntityId());
        UplinkMsg msg = null;
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                TbResource tbResource = resourceService.findResourceById(cloudEvent.getTenantId(), tbResourceId);
                if (tbResource != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    ResourceUpdateMsg resourceUpdateMsg = ((ResourceMsgConstructor)
                            resourceMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructResourceUpdatedMsg(msgType, tbResource);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addResourceUpdateMsg(resourceUpdateMsg)
                            .build();
                }
            }
        }
        return msg;
    }

    @Override
    protected TbResource constructResourceFromUpdateMsg(TenantId tenantId, TbResourceId tbResourceId, ResourceUpdateMsg resourceUpdateMsg) {
        return JacksonUtil.fromString(resourceUpdateMsg.getEntity(), TbResource.class, true);
    }

}
