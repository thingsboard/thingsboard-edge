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
package org.thingsboard.server.service.security.auth.oauth2;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class AbstractOAuth2ClientMapper {

    @Autowired
    private UserService userService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserPermissionsService userPermissionsService;

    @Autowired
    private EntityGroupService entityGroupService;

    private final Lock userCreationLock = new ReentrantLock();

    protected SecurityUser getOrCreateSecurityUserFromOAuth2User(OAuth2User oauth2User, boolean allowUserCreation) {
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, oauth2User.getEmail());

        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, oauth2User.getEmail());

        if (user == null && !allowUserCreation) {
            throw new UsernameNotFoundException("User not found: " + oauth2User.getEmail());
        }

        if (user == null) {
            userCreationLock.lock();
            try {
                user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, oauth2User.getEmail());
                if (user == null) {
                    user = new User();
                    if (oauth2User.getCustomerId() == null && StringUtils.isEmpty(oauth2User.getCustomerName())) {
                        user.setAuthority(Authority.TENANT_ADMIN);
                    } else {
                        user.setAuthority(Authority.CUSTOMER_USER);
                    }
                    TenantId tenantId = oauth2User.getTenantId() != null ?
                            oauth2User.getTenantId() : getTenantId(oauth2User.getTenantName());
                    user.setTenantId(tenantId);
                    CustomerId parentCustomerId = oauth2User.getParentCustomerId() != null ?
                            oauth2User.getParentCustomerId() : getCustomerId(user.getTenantId(), oauth2User.getParentCustomerName(), null);
                    CustomerId customerId = oauth2User.getCustomerId() != null ?
                            oauth2User.getCustomerId() : getCustomerId(user.getTenantId(), oauth2User.getCustomerName(), parentCustomerId);
                    user.setCustomerId(customerId);
                    user.setEmail(oauth2User.getEmail());
                    user.setFirstName(oauth2User.getFirstName());
                    user.setLastName(oauth2User.getLastName());
                    user = userService.saveUser(user);
                }
            } finally {
                userCreationLock.unlock();
            }
        }

        try {
            ListenableFuture<Void> future = addUserToUserGroups(oauth2User, user);
            future.get();
        } catch (Exception e) {
            log.error("Error while adding user to entity groups", e);
            throw new RuntimeException("Error while adding user to entity groups", e);
        }

        try {
            SecurityUser securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user));
            return (SecurityUser) new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()).getPrincipal();
        } catch (Exception e) {
            log.error("Can't get or create security user from oauth2 user", e);
            throw new RuntimeException("Can't get or create security user from oauth2 user", e);
        }
    }

    private ListenableFuture<Void> addUserToUserGroups(OAuth2User oauth2User, User user) {
        List<EntityGroupId> addedEntityGroups = new ArrayList<>();
        try {
            EntityGroup allUserGroup = entityGroupService.findOrCreateUserGroup(user.getTenantId(), user.getOwnerId(), EntityGroup.GROUP_ALL_NAME, "");
            addedEntityGroups.add(allUserGroup.getId());
            if (oauth2User.getUserGroups() != null && !oauth2User.getUserGroups().isEmpty()) {
                for (String group : oauth2User.getUserGroups()) {
                    EntityGroup userGroup = entityGroupService.findOrCreateUserGroup(user.getTenantId(), user.getOwnerId(), group, "");
                    if (userGroup != null) {
                        addedEntityGroups.add(userGroup.getId());
                        entityGroupService.addEntityToEntityGroup(user.getTenantId(), userGroup.getId(), user.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Can't add user [{}] to user groups", user.getEmail(), e);
            throw new RuntimeException("Can't add user to user groups", e);
        }

        ListenableFuture<List<EntityGroupId>> future = entityGroupService.findEntityGroupsForEntity(user.getTenantId(), user.getId());

        return Futures.transformAsync(future, currentEntityGroups -> {
            if (currentEntityGroups != null && !currentEntityGroups.isEmpty()) {
                for (EntityGroupId currentEntityGroupId : currentEntityGroups) {
                    if (!addedEntityGroups.contains(currentEntityGroupId)) {
                        entityGroupService.removeEntityFromEntityGroup(user.getTenantId(), currentEntityGroupId, user.getId());
                    }
                }
            }
            return Futures.immediateFuture(null);
        }, MoreExecutors.directExecutor());
    }

    private MergedUserPermissions getMergedUserPermissions(User user) {
        try {
            return userPermissionsService.getMergedPermissions(user, false);
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to get user permissions", e);
        }
    }

    private TenantId getTenantId(String tenantName) {
        List<Tenant> tenants = tenantService.findTenants(new TextPageLink(1, tenantName)).getData();
        Tenant tenant;
        if (tenants == null || tenants.isEmpty()) {
            tenant = new Tenant();
            tenant.setTitle(tenantName);
            tenant = tenantService.saveTenant(tenant);
        } else {
            tenant = tenants.get(0);
        }
        return tenant.getTenantId();
    }

    private CustomerId getCustomerId(TenantId tenantId, String customerName, CustomerId parentCustomerId) {
        if (StringUtils.isEmpty(customerName)) {
            return null;
        }
        Optional<Customer> customerOpt = customerService.findCustomerByTenantIdAndTitle(tenantId, customerName);
        if (customerOpt.isPresent()) {
            return customerOpt.get().getId();
        } else {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setTitle(customerName);
            customer.setParentCustomerId(parentCustomerId);
            return customerService.saveCustomer(customer).getId();
        }
    }
}
