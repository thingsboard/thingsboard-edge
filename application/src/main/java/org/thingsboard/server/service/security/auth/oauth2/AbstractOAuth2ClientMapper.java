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
package org.thingsboard.server.service.security.auth.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.entitiy.user.TbUserService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class AbstractOAuth2ClientMapper {
    private static final int DASHBOARDS_REQUEST_LIMIT = 10;

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserPermissionsService userPermissionsService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private OwnersCacheService ownersCacheService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private TbUserService tbUserService;

    @Autowired
    protected TbTenantProfileCache tenantProfileCache;

    @Autowired
    protected TbClusterService tbClusterService;

    @Value("${edges.enabled}")
    @Getter
    private boolean edgesEnabled;

    private final Lock userCreationLock = new ReentrantLock();

    protected SecurityUser getOrCreateSecurityUserFromOAuth2User(OAuth2User oauth2User, OAuth2Registration registration) {

        OAuth2MapperConfig config = registration.getMapperConfig();

        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, oauth2User.getEmail());

        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, oauth2User.getEmail());

        if (user == null && !config.isAllowUserCreation()) {
            throw new UsernameNotFoundException("User not found: " + oauth2User.getEmail());
        }

        boolean isNewUser = false;

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

                    ObjectNode additionalInfo = mapper.createObjectNode();

                    if (registration.getAdditionalInfo() != null &&
                            registration.getAdditionalInfo().has("providerName")) {
                        additionalInfo.put("authProviderName", registration.getAdditionalInfo().get("providerName").asText());
                    }

                    user.setAdditionalInfo(additionalInfo);

                    user = tbUserService.save(tenantId, customerId, null, user, false, null, (EntityGroup) null, null);
                    if (config.isActivateUser()) {
                        UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
                        userService.activateUserCredentials(user.getTenantId(), userCredentials.getActivateToken(), passwordEncoder.encode(""));
                    }
                    isNewUser = true;
                }
            } catch (Exception e) {
                log.error("Can't get or create security user from oauth2 user", e);
                throw new RuntimeException("Can't get or create security user from oauth2 user", e);
            } finally {
                userCreationLock.unlock();
            }

            try {
                ListenableFuture<Void> future = addUserToUserGroups(oauth2User, user);
                future.get();
            } catch (Exception e) {
                log.error("Error while adding user to entity groups", e);
                throw new RuntimeException("Error while adding user to entity groups", e);
            }
        }

        SecurityUser securityUser;
        try {
            securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user));
        } catch (Exception e) {
            log.error("Can't get or create security user from oauth2 user", e);
            throw new RuntimeException("Can't get or create security user from oauth2 user", e);
        }

        if (isNewUser && !StringUtils.isEmpty(oauth2User.getDefaultDashboardName())) {
            TenantId tenantId = user.getTenantId();
            try {
                Optional<DashboardId> dashboardIdOpt = findDefaultDashboard(oauth2User, securityUser, tenantId);
                if (dashboardIdOpt.isPresent()) {
                    user = userService.findUserById(user.getTenantId(), user.getId());
                    JsonNode additionalInfo = user.getAdditionalInfo();
                    if (additionalInfo == null || additionalInfo instanceof NullNode) {
                        additionalInfo = mapper.createObjectNode();
                    }
                    ((ObjectNode) additionalInfo).put("defaultDashboardFullscreen", oauth2User.isAlwaysFullScreen());
                    ((ObjectNode) additionalInfo).put("defaultDashboardId", dashboardIdOpt.get().getId().toString());
                    user.setAdditionalInfo(additionalInfo);
                    user = userService.saveUser(user);
                    securityUser = new SecurityUser(user, true, principal, getMergedUserPermissions(user));
                }
            } catch (Exception e) {
                log.error("Error while setting default dashboard for user", e);
            }
        }

        try {
            return (SecurityUser) new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()).getPrincipal();
        } catch (Exception e) {
            log.error("Can't create authentication token from security user", e);
            throw new RuntimeException("Can't create authentication token from security user", e);
        }
    }

    private Optional<DashboardId> findDefaultDashboard(OAuth2User oauth2User, SecurityUser securityUser, TenantId tenantId) throws Exception {
        PageLink pageLink = new PageLink(1, 0, oauth2User.getDefaultDashboardName());
        return ownersCacheService.getGroupEntities(tenantId, securityUser,
                EntityType.DASHBOARD, Operation.READ,
                pageLink,
                (groupIds) -> dashboardService.findDashboardsByEntityGroupIds(groupIds, pageLink))
                .getData()
                .stream()
                .findAny()
                .map(IdBased::getId);
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

        ListenableFuture<List<EntityGroupId>> future = entityGroupService.findEntityGroupsForEntityAsync(user.getTenantId(), user.getId());

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

    private TenantId getTenantId(String tenantName) throws IOException {
        List<Tenant> tenants = tenantService.findTenants(new PageLink(1, 0, tenantName)).getData();
        Tenant tenant;
        if (tenants == null || tenants.isEmpty()) {
            tenant = new Tenant();
            tenant.setTitle(tenantName);
            tenant = tenantService.saveTenant(tenant);
            installScripts.createDefaultRuleChains(tenant.getId());
            installScripts.createDefaultEdgeRuleChains(tenant.getId());
            tenantProfileCache.evict(tenant.getId());
            tbClusterService.onTenantChange(tenant, null);
            tbClusterService.broadcastEntityStateChangeEvent(tenant.getId(), tenant.getId(),
                    ComponentLifecycleEvent.CREATED);
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
