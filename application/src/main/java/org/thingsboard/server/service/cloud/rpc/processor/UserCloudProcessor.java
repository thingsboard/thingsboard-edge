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
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
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

    public ListenableFuture<Void> processUserMsgFromCloud(TenantId tenantId, UserUpdateMsg userUpdateMsg) {
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
                        User savedUser = edgeCtx.getUserService().saveUser(tenantId, user, false);
                        if (created) {
                            createDefaultUserCredentials(savedUser.getTenantId(), savedUser.getId());
                        }
                    } finally {
                        userCreationLock.unlock();
                    }
                    return requestForAdditionalData(tenantId, userId);
                case ENTITY_DELETED_RPC_MESSAGE:
                    User userToDelete = edgeCtx.getUserService().findUserById(tenantId, userId);
                    if (userToDelete != null) {
                        edgeCtx.getUserService().deleteUser(tenantId, userToDelete);
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(userUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
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
