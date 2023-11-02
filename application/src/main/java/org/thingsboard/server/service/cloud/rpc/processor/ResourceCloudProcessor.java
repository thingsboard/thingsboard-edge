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
