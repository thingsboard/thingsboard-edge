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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
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

@Component
@Slf4j
public class UserCloudProcessor extends BaseEdgeProcessor {

    private final Lock userCreationLock = new ReentrantLock();

    @Autowired
    private UserService userService;

    public ListenableFuture<Void> processUserMsgFromCloud(TenantId tenantId,
                                                          UserUpdateMsg userUpdateMsg,
                                                          Long queueStartTs) throws ThingsboardException {
        UserId userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
        CustomerId customerId = safeGetCustomerId(userUpdateMsg.getCustomerIdMSB(), userUpdateMsg.getCustomerIdLSB());
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
                    } else {
                        changeOwnerIfRequired(tenantId, customerId, userId);
                    }
                    user.setEmail(userUpdateMsg.getEmail());
                    user.setAuthority(Authority.valueOf(userUpdateMsg.getAuthority()));
                    user.setFirstName(userUpdateMsg.hasFirstName() ? userUpdateMsg.getFirstName() : null);
                    user.setLastName(userUpdateMsg.hasLastName() ? userUpdateMsg.getLastName() : null);
                    user.setAdditionalInfo(userUpdateMsg.hasAdditionalInfo() ? JacksonUtil.toJsonNode(userUpdateMsg.getAdditionalInfo()) : null);
                    user.setCustomerId(customerId);
                    User savedUser = userService.saveUser(user, false);
                    if (created) {
                        UserCredentials userCredentials = new UserCredentials();
                        userCredentials.setEnabled(false);
                        userCredentials.setActivateToken(StringUtils.randomAlphanumeric(UserServiceImpl.DEFAULT_TOKEN_LENGTH));
                        userCredentials.setUserId(new UserId(savedUser.getUuidId()));
                        // TODO: @voba - save or update user password history?
                        userService.saveUserCredentials(user.getTenantId(), userCredentials);
                        if (!user.getTenantId().isNullUid()) {
                            entityGroupService.addEntityToEntityGroupAll(user.getTenantId(), savedUser.getOwnerId(), savedUser.getId());
                        }
                    }
                    safeAddEntityToGroup(tenantId, userUpdateMsg, savedUser);
                } finally {
                    userCreationLock.unlock();
                }
                return Futures.transformAsync(requestForAdditionalData(tenantId, userId, queueStartTs),
                        ignored -> cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.USER, EdgeEventActionType.CREDENTIALS_REQUEST,
                                userId, null, null, queueStartTs),
                        dbCallbackExecutorService);
            case ENTITY_DELETED_RPC_MESSAGE:
                if (userUpdateMsg.hasEntityGroupIdMSB() && userUpdateMsg.hasEntityGroupIdLSB()) {
                    UUID entityGroupUUID = safeGetUUID(userUpdateMsg.getEntityGroupIdMSB(),
                            userUpdateMsg.getEntityGroupIdLSB());
                    EntityGroupId entityGroupId = new EntityGroupId(entityGroupUUID);
                    entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, userId);
                    // TODO: @voba - check if entity has any more groups, except 'All' - in case only 'All' - remove entity from edge
                } else {
                    User userToDelete = userService.findUserById(tenantId, userId);
                    if (userToDelete != null) {
                        userService.deleteUser(tenantId, userToDelete.getId());
                    }
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
            default:
                return handleUnsupportedMsgType(userUpdateMsg.getMsgType());
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
        UserId userId = new UserId(new UUID(userCredentialsUpdateMsg.getUserIdMSB(), userCredentialsUpdateMsg.getUserIdLSB()));
        ListenableFuture<User> userFuture = userService.findUserByIdAsync(tenantId, userId);
        return Futures.transform(userFuture, user -> {
            if (user != null) {
                UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, user.getId());
                userCredentials.setEnabled(userCredentialsUpdateMsg.getEnabled());
                userCredentials.setPassword(userCredentialsUpdateMsg.getPassword());
                userCredentials.setActivateToken(null);
                userCredentials.setResetToken(null);
                userService.saveUserCredentials(tenantId, userCredentials, false);
            }
            return null;
        }, dbCallbackExecutorService);
    }
}
