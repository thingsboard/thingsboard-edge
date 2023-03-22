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
package org.thingsboard.server.dao.user;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface UserService extends EntityDaoService {

	User findUserById(TenantId tenantId, UserId userId);

	ListenableFuture<User> findUserByIdAsync(TenantId tenantId, UserId userId);

    ListenableFuture<List<User>> findUsersByTenantIdAndIdsAsync(TenantId tenantId, List<UserId> userIds);

    User findUserByEmail(TenantId tenantId, String email);

    User findUserByTenantIdAndEmail(TenantId tenantId, String email);

    User changeOwner(User user, EntityId targetOwnerId);

	User saveUser(User user);

	UserCredentials findUserCredentialsByUserId(TenantId tenantId, UserId userId);

	UserCredentials findUserCredentialsByActivateToken(TenantId tenantId, String activateToken);

	UserCredentials findUserCredentialsByResetToken(TenantId tenantId, String resetToken);

	UserCredentials saveUserCredentials(TenantId tenantId, UserCredentials userCredentials);

	UserCredentials activateUserCredentials(TenantId tenantId, String activateToken, String password);

	UserCredentials requestPasswordReset(TenantId tenantId, String email);

    UserCredentials requestExpiredPasswordReset(TenantId tenantId, UserCredentialsId userCredentialsId);

    UserCredentials replaceUserCredentials(TenantId tenantId, UserCredentials userCredentials);

    void deleteUser(TenantId tenantId, UserId userId);

	PageData<User> findTenantAdmins(TenantId tenantId, PageLink pageLink);

    PageData<User> findUsersByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<User> findAllTenantAdmins(PageLink pageLink);

    PageData<User> findTenantAdminsByTenantsIds(List<TenantId> tenantsIds, PageLink pageLink);

    PageData<User> findTenantAdminsByTenantProfilesIds(List<TenantProfileId> tenantProfilesIds, PageLink pageLink);

    PageData<User> findAllUsers(PageLink pageLink);

	void deleteTenantAdmins(TenantId tenantId);

    PageData<User> findAllCustomerUsers(TenantId tenantId, PageLink pageLink);

    PageData<User> findCustomerUsers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    void deleteCustomerUsers(TenantId tenantId, CustomerId customerId);

    PageData<User> findUsersByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<User> findUsersByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    PageData<User> findUsersByTenantIdAndRoles(TenantId tenantId, List<RoleId> roles, PageLink pageLink);

	void setUserCredentialsEnabled(TenantId tenantId, UserId userId, boolean enabled);

    void resetFailedLoginAttempts(TenantId tenantId, UserId userId);

    int increaseFailedLoginAttempts(TenantId tenantId, UserId userId);

    void setLastLoginTs(TenantId tenantId, UserId userId);

    PageData<UserInfo> findUserInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<UserInfo> findTenantUserInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<UserInfo> findUserInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<UserInfo> findUserInfosByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

}
