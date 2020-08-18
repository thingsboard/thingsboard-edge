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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonNull;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.UserUpdateMsg;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class UserUpdateProcessor extends BaseUpdateProcessor {

    private final Lock userCreationLock = new ReentrantLock();

    @Autowired
    private UserService userService;

    public ListenableFuture<Void> onUserUpdate(TenantId tenantId, UserUpdateMsg userUpdateMsg) {
        UserId userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
        switch (userUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    userCreationLock.lock();
                    boolean created = false;
                    User user = userService.findUserById(tenantId, userId);
                    if (user == null) {
                        user = new User();
                        user.setTenantId(tenantId);
                        user.setId(userId);
                        created = true;
                    }
                    if (userUpdateMsg.getCustomerIdMSB() != 0 && userUpdateMsg.getCustomerIdLSB() != 0) {
                        CustomerId customerId = new CustomerId(new UUID(userUpdateMsg.getCustomerIdMSB(), userUpdateMsg.getCustomerIdLSB()));
                        user.setCustomerId(customerId);
                    }
                    user.setEmail(userUpdateMsg.getEmail());
                    user.setAuthority(Authority.valueOf(userUpdateMsg.getAuthority()));
                    user.setFirstName(userUpdateMsg.getFirstName());
                    user.setLastName(userUpdateMsg.getLastName());
                    user.setAdditionalInfo(JacksonUtil.toJsonNode(userUpdateMsg.getAdditionalInfo()));
                    User savedUser = userService.saveUser(user, created);

                    if (Authority.TENANT_ADMIN.equals(savedUser.getAuthority())) {
                        if (isFullAccess(savedUser.getAdditionalInfo())) {
                            EntityGroup edgeTenantAdmins = entityGroupService.findOrCreateEdgeTenantAdminsGroup(savedUser.getTenantId());
                            entityGroupService.addEntityToEntityGroup(savedUser.getTenantId(), edgeTenantAdmins.getId(), savedUser.getId());
                        } else {
                            EntityGroup edgeTenantUsers = entityGroupService.findOrCreateEdgeTenantUsersGroup(savedUser.getTenantId());
                            entityGroupService.addEntityToEntityGroup(savedUser.getTenantId(), edgeTenantUsers.getId(), savedUser.getId());
                        }
                    } else {
                        if (isFullAccess(savedUser.getAdditionalInfo())) {
                            EntityGroup edgeCustomerAdmins = entityGroupService.findOrCreateEdgeCustomerAdminsGroup(savedUser.getTenantId(), savedUser.getCustomerId());
                            entityGroupService.addEntityToEntityGroup(savedUser.getTenantId(), edgeCustomerAdmins.getId(), savedUser.getId());
                        } else {
                            EntityGroup edgeCustomerUsers = entityGroupService.findOrCreateEdgeCustomerUsersGroup(savedUser.getTenantId(), savedUser.getCustomerId());
                            entityGroupService.addEntityToEntityGroup(savedUser.getTenantId(), edgeCustomerUsers.getId(), savedUser.getId());
                        }
                    }

                    if (isNonEmptyGroupId(userUpdateMsg.getEntityGroupIdMSB(), userUpdateMsg.getEntityGroupIdLSB())) {
                        EntityGroupId entityGroupId = new EntityGroupId(new UUID(userUpdateMsg.getEntityGroupIdMSB(), userUpdateMsg.getEntityGroupIdLSB()));
                        addEntityToGroup(tenantId, entityGroupId, savedUser.getId());
                    }
                } finally {
                    userCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                if (isNonEmptyGroupId(userUpdateMsg.getEntityGroupIdMSB(), userUpdateMsg.getEntityGroupIdLSB())) {
                    EntityGroupId entityGroupId =
                            new EntityGroupId(new UUID(userUpdateMsg.getEntityGroupIdMSB(), userUpdateMsg.getEntityGroupIdLSB()));
                    entityGroupService.removeEntityFromEntityGroup(tenantId, entityGroupId, userId);
                } else {
                    User userToDelete = userService.findUserById(tenantId, userId);
                    if (userToDelete != null) {
                        userService.deleteUser(tenantId, userToDelete.getId());
                    }
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
                return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type" + userUpdateMsg.getMsgType()));
        }
        ListenableFuture<Void> aDRF = Futures.transform(requestForAdditionalData(tenantId, userUpdateMsg.getMsgType(), userId), future -> null, dbCallbackExecutor);

        ListenableFuture<ListenableFuture<Void>> t = Futures.transform(aDRF, aDR -> {
            ListenableFuture<CloudEvent> f = Futures.immediateFuture(null);
            if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(userUpdateMsg.getMsgType()) ||
                    UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(userUpdateMsg.getMsgType())) {
                f = saveCloudEvent(tenantId, CloudEventType.USER, ActionType.CREDENTIALS_REQUEST, userId, null);
            }
            return Futures.transform(f, tmp -> null, dbCallbackExecutor);
        }, dbCallbackExecutor);

        return Futures.transform(t, tt -> null, dbCallbackExecutor);
    }

    private boolean isFullAccess(JsonNode additionalInfo) {
        if (additionalInfo != null && additionalInfo.has("isFullAccess")) {
            return additionalInfo.get("isFullAccess").asBoolean();
        }
        return false;
    }

    public ListenableFuture<Void> onUserCredentialsUpdate(TenantId tenantId, UserCredentialsUpdateMsg userCredentialsUpdateMsg) {
        UserId userId = new UserId(new UUID(userCredentialsUpdateMsg.getUserIdMSB(), userCredentialsUpdateMsg.getUserIdLSB()));
        ListenableFuture<User> userFuture = userService.findUserByIdAsync(tenantId, userId);
        return Futures.transform(userFuture, user -> {
            if (user != null) {
                UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, user.getId());
                userCredentials.setEnabled(userCredentialsUpdateMsg.getEnabled());
                userCredentials.setPassword(userCredentialsUpdateMsg.getPassword());
                userCredentials.setActivateToken(null);
                userCredentials.setResetToken(null);
                userService.saveUserCredentials(tenantId, userCredentials);
            }
            return null;
        }, dbCallbackExecutor);
    }
}
