/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.processor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class EntityGroupUpdateProcessor extends BaseUpdateProcessor {

    private final Lock entityGroupCreationLock = new ReentrantLock();

    public void onEntityGroupUpdate(TenantId tenantId, EntityGroupUpdateMsg entityGroupUpdateMsg) {
        log.info("onEntityGroupUpdate {}", entityGroupUpdateMsg);
        EntityGroupId entityGroupId = new EntityGroupId(new UUID(entityGroupUpdateMsg.getIdMSB(), entityGroupUpdateMsg.getIdLSB()));
        switch (entityGroupUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    entityGroupCreationLock.lock();
                    EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, entityGroupId);
                    boolean created = false;
                    if (entityGroup == null) {
                        entityGroup = new EntityGroup();
                        entityGroup.setId(entityGroupId);
                        created = true;
                    }
                    entityGroup.setName(entityGroupUpdateMsg.getName());
                    entityGroup.setType(EntityType.valueOf(entityGroupUpdateMsg.getType()));
                    entityGroup.setConfiguration(JacksonUtil.toJsonNode(entityGroupUpdateMsg.getConfiguration()));
                    entityGroup.setAdditionalInfo(JacksonUtil.toJsonNode(entityGroupUpdateMsg.getAdditionalInfo()));

                    // TODO: voba - parent ID is hardcoded. Should be updated in next releases
                    entityGroup.setOwnerId(tenantId);
                    entityGroupService.saveEntityGroup(tenantId, tenantId, entityGroup, created);

                } finally {
                    entityGroupCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                ListenableFuture<EntityGroup> entityGroupByIdAsyncFuture = entityGroupService.findEntityGroupByIdAsync(tenantId, entityGroupId);
                Futures.addCallback(entityGroupByIdAsyncFuture, new FutureCallback<EntityGroup>() {
                    @Override
                    public void onSuccess(@Nullable EntityGroup entityGroup) {
                        if (entityGroup != null) {
                            entityGroupService.deleteEntityGroup(tenantId, entityGroup.getId());
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Can't delete entity group by id, entityGroupUpdateMsg [{}]", entityGroupUpdateMsg, t);
                    }
                }, dbCallbackExecutor);
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
        }

        if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(entityGroupUpdateMsg.getMsgType()) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(entityGroupUpdateMsg.getMsgType())) {
            ObjectNode body = mapper.createObjectNode();
            body.put("type", entityGroupUpdateMsg.getType());
            saveCloudEvent(tenantId, CloudEventType.ENTITY_GROUP, ActionType.GROUP_ENTITIES_REQUEST, entityGroupId, body);
        }
    }
}
