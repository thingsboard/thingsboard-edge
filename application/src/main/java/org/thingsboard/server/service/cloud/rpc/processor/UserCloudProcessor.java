/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg.Builder;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.user.BaseUserProcessor;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@TbCoreComponent
public class UserCloudProcessor extends BaseUserProcessor {

    private final Lock userCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processUserMsgFromCloud(TenantId tenantId, UserUpdateMsg userUpdateMsg) {
        UserId userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (userUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    userCreationLock.lock();
                    try {
                        processUserMsgFromCloud(tenantId, userId, userUpdateMsg);
                    } finally {
                        userCreationLock.unlock();
                    }
                    return requestForAdditionalData(tenantId, userId);
                case ENTITY_DELETED_RPC_MESSAGE:
                    deleteUserAndPushEntityDeletedEventToRuleEngine(tenantId, userId);
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(userUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void processUserMsgFromCloud(TenantId tenantId, UserId userId, UserUpdateMsg userUpdateMsg) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateUser(tenantId, userId, userUpdateMsg);
        boolean isCreated = resultPair.getFirst();
        if (isCreated) {
            pushUserCreatedEventToRuleEngine(tenantId, userId);
        }

        boolean userEmailUpdated = resultPair.getSecond();
        if (userEmailUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.USER, EdgeEventActionType.UPDATED, userId, null);
        }
    }

    private void pushUserCreatedEventToRuleEngine(TenantId tenantId, UserId userId) {
        try {
            User user = edgeCtx.getUserService().findUserById(tenantId, userId);
            if (user != null) {
                String userAsString = JacksonUtil.toString(user);
                pushEntityEventToRuleEngine(tenantId, userId, user.getCustomerId(), TbMsgType.ENTITY_CREATED, userAsString, TbMsgMetaData.EMPTY);
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push user action to rule engine: {}", tenantId, userId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    public ListenableFuture<Void> processUserCredentialsMsgFromCloud(TenantId tenantId, UserCredentialsUpdateMsg userCredentialsUpdateMsg) {
        try {
            cloudSynchronizationManager.getSync().set(true);

            super.updateUserCredentials(tenantId, userCredentialsUpdateMsg);
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        UserId userId = new UserId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                User user = edgeCtx.getUserService().findUserById(cloudEvent.getTenantId(), userId);
                if (user != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(msgType, user));
                    UserCredentials userCredentials = edgeCtx.getUserService().findUserCredentialsByUserId(cloudEvent.getTenantId(), userId);
                    if (userCredentials != null) {
                        builder.addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentials));
                    }
                    return builder.build();
                } else {
                    log.info("Skipping event as user was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                Builder userUpdateMsg = UserUpdateMsg.newBuilder().setMsgType(msgType)
                        .setIdMSB(cloudEvent.getEntityId().getMostSignificantBits())
                        .setIdLSB(cloudEvent.getEntityId().getLeastSignificantBits());

                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addUserUpdateMsg(userUpdateMsg).build();
            }
            case CREDENTIALS_UPDATED -> {
                UserCredentials userCredentials = edgeCtx.getUserService().findUserCredentialsByUserId(cloudEvent.getTenantId(), userId);
                if (userCredentials != null) {
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentials))
                            .build();
                }
            }
        }
        return null;
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.USER;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, User user, UserUpdateMsg userUpdateMsg) {
        if (isCustomerNotExists(tenantId, user.getCustomerId())) {
            user.setCustomerId(null);
        }
    }

}
