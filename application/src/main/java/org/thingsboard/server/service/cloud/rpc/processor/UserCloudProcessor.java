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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.user.UserServiceImpl;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.common.data.StringUtils.generateSafeToken;

@Component
@Slf4j
public class UserCloudProcessor extends BaseEdgeProcessor {

    private final Lock userCreationLock = new ReentrantLock();

    @Autowired
    private UserService userService;

    public ListenableFuture<Void> processUserMsgFromCloud(TenantId tenantId,
                                                          CustomerId edgeCustomerId,
                                                          UserUpdateMsg userUpdateMsg,
                                                          Long queueStartTs) {
        UserId userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
        CustomerId customerId = safeGetCustomerId(userUpdateMsg.getCustomerIdMSB(), userUpdateMsg.getCustomerIdLSB(), tenantId, edgeCustomerId);
        switch (userUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                userCreationLock.lock();
                try {
                    boolean created = false;
                    User user = userService.findUserById(tenantId, userId);
                    if (user == null) {
                        user = new User();
                        user.setTenantId(tenantId);
                        user.setId(userId);
                        user.setCreatedTime(Uuids.unixTimestamp(userId.getId()));
                        created = true;
                    }
                    user.setEmail(userUpdateMsg.getEmail());
                    user.setAuthority(Authority.valueOf(userUpdateMsg.getAuthority()));
                    user.setFirstName(userUpdateMsg.hasFirstName() ? userUpdateMsg.getFirstName() : null);
                    user.setLastName(userUpdateMsg.hasLastName() ? userUpdateMsg.getLastName() : null);
                    user.setAdditionalInfo(userUpdateMsg.hasAdditionalInfo() ? JacksonUtil.toJsonNode(userUpdateMsg.getAdditionalInfo()) : null);
                    user.setCustomerId(customerId);
                    User savedUser = userService.saveUser(user, false);
                    if (created) {
                        createDefaultUserCredentials(savedUser.getTenantId(), savedUser.getId());
                    }
                } finally {
                    userCreationLock.unlock();
                }
                return Futures.transformAsync(requestForAdditionalData(tenantId, userId, queueStartTs),
                        ignored -> cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.USER, EdgeEventActionType.CREDENTIALS_REQUEST,
                                userId, null, queueStartTs),
                        dbCallbackExecutorService);
            case ENTITY_DELETED_RPC_MESSAGE:
                User userToDelete = userService.findUserById(tenantId, userId);
                if (userToDelete != null) {
                    userService.deleteUser(tenantId, userToDelete.getId());
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
            default:
                return handleUnsupportedMsgType(userUpdateMsg.getMsgType());
        }
    }

    public ListenableFuture<Void> processUserCredentialsMsgFromCloud(TenantId tenantId, UserCredentialsUpdateMsg userCredentialsUpdateMsg) {
        UserId userId = new UserId(new UUID(userCredentialsUpdateMsg.getUserIdMSB(), userCredentialsUpdateMsg.getUserIdLSB()));
        ListenableFuture<User> userFuture = userService.findUserByIdAsync(tenantId, userId);
        return Futures.transform(userFuture, user -> {
            if (user != null) {
                UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, user.getId());
                if (userCredentials == null) {
                    userCredentials = createDefaultUserCredentials(tenantId, userId);
                }
                userCredentials.setEnabled(userCredentialsUpdateMsg.getEnabled());
                userCredentials.setPassword(userCredentialsUpdateMsg.getPassword());
                userCredentials.setActivateToken(null);
                userCredentials.setResetToken(null);
                userService.saveUserCredentials(tenantId, userCredentials);
            }
            return null;
        }, dbCallbackExecutorService);
    }

    private UserCredentials createDefaultUserCredentials(TenantId tenantId, UserId userId) {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(false);
        userCredentials.setActivateToken(StringUtils.randomAlphanumeric(UserServiceImpl.DEFAULT_TOKEN_LENGTH));
        userCredentials.setUserId(userId);
        userCredentials.setAdditionalInfo(JacksonUtil.newObjectNode());
        // TODO: @voba - save or update user password history?
        return userService.saveUserCredentials(tenantId, userCredentials, false);
    }
}
