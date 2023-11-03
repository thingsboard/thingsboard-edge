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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
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
                    deleteSystemResourceIfAlreadyExists(tbResourceId, ResourceType.valueOf(resourceUpdateMsg.getResourceType()), resourceUpdateMsg.getResourceKey());
                    super.saveOrUpdateTbResource(tenantId, tbResourceId, resourceUpdateMsg);
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

    public UplinkMsg convertResourceEventToUplink(CloudEvent cloudEvent) {
        TbResourceId tbResourceId = new TbResourceId(cloudEvent.getEntityId());
        UplinkMsg msg = null;
        switch (cloudEvent.getAction()) {
            case ADDED:
            case UPDATED:
                TbResource tbResource = resourceService.findResourceById(cloudEvent.getTenantId(), tbResourceId);
                if (tbResource != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    ResourceUpdateMsg resourceUpdateMsg =
                            resourceMsgConstructor.constructResourceUpdatedMsg(msgType, tbResource);
                    msg = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addResourceUpdateMsg(resourceUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
                ResourceUpdateMsg resourceUpdateMsg =
                        resourceMsgConstructor.constructResourceDeleteMsg(tbResourceId);
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addResourceUpdateMsg(resourceUpdateMsg)
                        .build();
                break;
        }
        return msg;
    }
}
