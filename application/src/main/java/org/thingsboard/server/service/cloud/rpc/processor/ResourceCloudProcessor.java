/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.resource.BaseResourceProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
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
                        edgeCtx.getResourceService().deleteResource(tenantId, resultPair.getSecond(), true);
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    TbResource tbResourceToDelete = edgeCtx.getResourceService().findResourceById(tenantId, tbResourceId);
                    if (tbResourceToDelete != null) {
                        edgeCtx.getResourceService().deleteResource(tenantId, tbResourceId, true);
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
        PageDataIterable<TbResource> resourcesIterable = new PageDataIterable<>(link -> edgeCtx.getResourceService().findTenantResourcesByResourceTypeAndPageLink(tenantId, resourceType, link), 1024);
        for (TbResource tbResource : resourcesIterable) {
            if (tbResource.getResourceKey().equals(resourceKey) && !tbResourceId.equals(tbResource.getId())) {
                tbResource.setResourceKey(StringUtils.randomAlphanumeric(15) + resourceKey);
                edgeCtx.getResourceService().saveResource(tbResource, false);
                return Pair.of(true, tbResource.getId());
            }
        }
        return Pair.of(false, new TbResourceId(UUID.randomUUID()));
    }

    private void deleteSystemResourceIfAlreadyExists(TbResourceId tbResourceId, ResourceType resourceType, String resourceKey) {
        PageDataIterable<TbResource> entityIdsIterator = new PageDataIterable<>(link -> edgeCtx.getResourceService().findAllTenantResources(TenantId.SYS_TENANT_ID, link), 1024);
        for (TbResource resource : entityIdsIterator) {
            if (resource.getResourceType().equals(resourceType) && resource.getResourceKey().equals(resourceKey) && !resource.getId().equals(tbResourceId)) {
                edgeCtx.getResourceService().deleteResource(TenantId.SYS_TENANT_ID, resource.getId(), true);
                break;
            }
        }
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        TbResourceId tbResourceId = new TbResourceId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                TbResource tbResource = edgeCtx.getResourceService().findResourceById(cloudEvent.getTenantId(), tbResourceId);
                if (tbResource != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    ResourceUpdateMsg resourceUpdateMsg = EdgeMsgConstructorUtils.constructResourceUpdatedMsg(msgType, tbResource);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addResourceUpdateMsg(resourceUpdateMsg)
                            .build();
                }
            }
        }
        return null;
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.TB_RESOURCE;
    }

}
