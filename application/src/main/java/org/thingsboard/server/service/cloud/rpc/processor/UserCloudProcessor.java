// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.v1.ApiKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.user.BaseUserProcessor;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class UserCloudProcessor extends BaseUserProcessor {

    public ListenableFuture<Void> processUserMsgFromCloud(TenantId tenantId, UserUpdateMsg userUpdateMsg) throws ThingsboardException {
        UserId userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            return switch (userUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateUserFromCloud(tenantId, userId, userUpdateMsg);
                    yield requestForAdditionalData(tenantId, userId);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    if (userUpdateMsg.hasEntityGroupIdMSB() && userUpdateMsg.hasEntityGroupIdLSB()) {
                        UUID entityGroupUUID = safeGetUUID(userUpdateMsg.getEntityGroupIdMSB(),
                                userUpdateMsg.getEntityGroupIdLSB());
                        EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                        edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, userId);
                        yield removeEntityIfInSingleAllGroup(tenantId, userId, () -> edgeCtx.getUserService().deleteUser(tenantId, userId));
                    }
                    deleteUserAndPushEntityDeletedEventToRuleEngine(tenantId, userId);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(userUpdateMsg.getMsgType());
            };
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void saveOrUpdateUserFromCloud(TenantId tenantId, UserId userId, UserUpdateMsg userUpdateMsg) throws ThingsboardException {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateUser(tenantId, userId, userUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            pushUserCreatedEventToRuleEngine(tenantId, userId);
        }
        Boolean userEmailUpdated = resultPair.getSecond();
        if (userEmailUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.USER, EdgeEventActionType.UPDATED, userId, null, null);
        }
    }

    private void pushUserCreatedEventToRuleEngine(TenantId tenantId, UserId userId) {
        User user = edgeCtx.getUserService().findUserById(tenantId, userId);
        pushUserEventToRuleEngine(tenantId, user, TbMsgType.ENTITY_CREATED);
    }

    private void pushUserEventToRuleEngine(TenantId tenantId, User user, TbMsgType msgType) {
        try {
            String deviceAsString = JacksonUtil.toString(user);
            pushEntityEventToRuleEngine(tenantId, user.getId(), user.getCustomerId(), msgType, deviceAsString, new TbMsgMetaData());
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push user action to rule engine: {}", tenantId, user.getId(), msgType.name(), e);
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
        EntityGroupId entityGroupId = cloudEvent.getEntityGroupId() != null ? new EntityGroupId(cloudEvent.getEntityGroupId()) : null;
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED, ADDED_TO_ENTITY_GROUP -> {
                User user = edgeCtx.getUserService().findUserById(cloudEvent.getTenantId(), userId);
                if (user != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserUpdateMsg(EdgeMsgConstructorUtils.constructUserUpdatedMsg(msgType, user, entityGroupId));
                    UserCredentials userCredentials = edgeCtx.getUserService().findUserCredentialsByUserId(cloudEvent.getTenantId(), userId);
                    if (userCredentials != null) {
                        builder.addUserCredentialsUpdateMsg(EdgeMsgConstructorUtils.constructUserCredentialsUpdatedMsg(userCredentials));
                    }
                    List<ApiKey> apiKeys = edgeCtx.getApiKeyService().findApiKeysByUserId(cloudEvent.getTenantId(), userId);
                    for (ApiKey apiKey : apiKeys) {
                        builder.addApiKeyUpdateMsg(EdgeMsgConstructorUtils.constructApiKeyUpdatedMsg(msgType, apiKey));
                    }
                    return builder.build();
                } else {
                    log.info("Skipping event as user was not found [{}]", cloudEvent);
                }
            }
            case DELETED, REMOVED_FROM_ENTITY_GROUP -> {
                UserUpdateMsg userDeleteMsg = EdgeMsgConstructorUtils.constructUserDeleteMsg(userId, entityGroupId);

                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addUserUpdateMsg(userDeleteMsg).build();
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
