/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.user.UserServiceImpl;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@TbCoreComponent
public class UserCloudProcessor extends BaseEdgeProcessor {

    private final Lock userCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processUserMsgFromCloud(TenantId tenantId, UserUpdateMsg userUpdateMsg) throws ThingsboardException {
        UserId userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (userUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    userCreationLock.lock();
                    try {
                        User user = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
                        if (user == null) {
                            throw new RuntimeException("[{" + tenantId + "}] userUpdateMsg {" + userUpdateMsg + "} cannot be converted to user");
                        }
                        User userById = edgeCtx.getUserService().findUserById(tenantId, userId);
                        boolean created = userById == null;
                        if (!created) {
                            changeOwnerIfRequired(tenantId, user.getCustomerId(), userId);
                        }
                        if (isCustomerNotExists(tenantId, user.getCustomerId())) {
                            user.setCustomerId(null);
                        }
                        User savedUser = edgeCtx.getUserService().saveUser(tenantId, user, false);
                        if (created) {
                            createDefaultUserCredentials(savedUser.getTenantId(), savedUser.getId());
                            if (!user.getTenantId().isNullUid()) {
                                edgeCtx.getEntityGroupService().addEntityToEntityGroupAll(user.getTenantId(), savedUser.getOwnerId(), savedUser.getId());
                            }
                        }
                        safeAddEntityToGroup(tenantId, userUpdateMsg, savedUser);
                    } finally {
                        userCreationLock.unlock();
                    }
                    return requestForAdditionalData(tenantId, userId);
                case ENTITY_DELETED_RPC_MESSAGE:
                    userCreationLock.lock();
                    try {
                        if (userUpdateMsg.hasEntityGroupIdMSB() && userUpdateMsg.hasEntityGroupIdLSB()) {
                            UUID entityGroupUUID = safeGetUUID(userUpdateMsg.getEntityGroupIdMSB(),
                                    userUpdateMsg.getEntityGroupIdLSB());
                            EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                            edgeCtx.getEntityGroupService().removeEntityFromEntityGroup(tenantId, entityGroupId, userId);
                            return removeEntityIfInSingleAllGroup(tenantId, userId, () -> edgeCtx.getUserService().deleteUser(tenantId, userId));
                        } else {
                            User userToDelete = edgeCtx.getUserService().findUserById(tenantId, userId);
                            if (userToDelete != null) {
                                edgeCtx.getUserService().deleteUser(tenantId, userToDelete);
                            }
                        }
                        return Futures.immediateFuture(null);
                    } finally {
                        userCreationLock.unlock();
                    }
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(userUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void safeAddEntityToGroup(TenantId tenantId, UserUpdateMsg userUpdateMsg, User savedUser) {
        if (userUpdateMsg.hasEntityGroupIdMSB() && userUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(userUpdateMsg.getEntityGroupIdMSB(),
                    userUpdateMsg.getEntityGroupIdLSB());
            EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
            safeAddEntityToGroup(tenantId, entityGroupId, savedUser.getId());
        }
    }

    public ListenableFuture<Void> processUserCredentialsMsgFromCloud(TenantId tenantId, UserCredentialsUpdateMsg userCredentialsUpdateMsg) {
        try {
            cloudSynchronizationManager.getSync().set(true);
            UserCredentials userCredentialsMsg = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
            if (userCredentialsMsg == null) {
                throw new RuntimeException("[{" + tenantId + "}] userCredentialsUpdateMsg {" + userCredentialsUpdateMsg + "} cannot be converted to user credentials");
            }
            User user = edgeCtx.getUserService().findUserById(tenantId, userCredentialsMsg.getUserId());
            if (user != null) {
                UserCredentials userCredentialsByUserId = edgeCtx.getUserService().findUserCredentialsByUserId(tenantId, user.getId());
                if (userCredentialsByUserId == null) {
                    userCredentialsByUserId = createDefaultUserCredentials(tenantId, userCredentialsMsg.getUserId());
                }
                userCredentialsByUserId.setEnabled(userCredentialsMsg.isEnabled());
                userCredentialsByUserId.setPassword(userCredentialsMsg.getPassword());
                userCredentialsByUserId.setActivateToken(userCredentialsMsg.getActivateToken());
                userCredentialsByUserId.setResetToken(userCredentialsMsg.getResetToken());
                userCredentialsByUserId.setAdditionalInfo(userCredentialsMsg.getAdditionalInfo());
                edgeCtx.getUserService().saveUserCredentials(tenantId, userCredentialsByUserId);
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    private UserCredentials createDefaultUserCredentials(TenantId tenantId, UserId userId) {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(false);
        userCredentials.setActivateToken(StringUtils.randomAlphanumeric(UserServiceImpl.DEFAULT_TOKEN_LENGTH));
        userCredentials.setUserId(userId);
        userCredentials.setAdditionalInfo(JacksonUtil.newObjectNode());
        // TODO: Edge-only:  save or update user password history?
        return edgeCtx.getUserService().saveUserCredentials(tenantId, userCredentials, false);
    }

}
