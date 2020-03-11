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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRepository;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.EntityGroupController.ENTITY_GROUP_ID;

@RestController
@RequestMapping("/api")
public class UserController extends BaseController {

    public static final String USER_ID = "userId";
    public static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";
    public static final String ACTIVATE_URL_PATTERN = "%s/api/noauth/activate?activateToken=%s";

    @Value("${security.user_token_access_enabled}")
    @Getter
    private boolean userTokenAccessEnabled;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserPermissionsService userPermissionsService;

    @Autowired
    private JwtTokenFactory tokenFactory;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;


    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public User getUserById(@PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            return checkUserId(userId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/tokenAccessEnabled", method = RequestMethod.GET)
    @ResponseBody
    public boolean isUserTokenAccessEnabled() {
        return userTokenAccessEnabled;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}/token", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getUserToken(@PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            if (!userTokenAccessEnabled) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            UserId userId = new UserId(toUUID(strUserId));
            SecurityUser authUser = getCurrentUser();
            User user = checkUserId(userId, Operation.IMPERSONATE);
            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            UserCredentials credentials = userService.findUserCredentialsByUserId(authUser.getTenantId(), userId);
            MergedUserPermissions userPermissions = userPermissionsService.getMergedPermissions(authUser, false);
            SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal, userPermissions);
            JwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
            JwtToken refreshToken = refreshTokenRepository.requestRefreshToken(securityUser);
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode tokenObject = objectMapper.createObjectNode();
            tokenObject.put("token", accessToken.getToken());
            tokenObject.put("refreshToken", refreshToken.getToken());
            return tokenObject;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    @ResponseBody
    public User saveUser(@RequestBody User user,
                         @RequestParam(required = false, defaultValue = "true") boolean sendActivationMail,
                         @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
                         HttpServletRequest request) throws ThingsboardException {
        try {

            if (getCurrentUser().getAuthority() != Authority.SYS_ADMIN) {
                user.setTenantId(getCurrentUser().getTenantId());
            }

            Operation operation = user.getId() == null ? Operation.CREATE : Operation.WRITE;

            if (operation == Operation.CREATE
                    && getCurrentUser().getAuthority() == Authority.CUSTOMER_USER &&
                    (user.getCustomerId() == null || user.getCustomerId().isNullUid())) {
                user.setCustomerId(getCurrentUser().getCustomerId());
            }

            EntityGroupId entityGroupId = null;
            if (!StringUtils.isEmpty(strEntityGroupId)) {
                entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            }

            if (operation == Operation.CREATE || !getCurrentUser().getId().equals(user.getId())) {
                accessControlService.checkPermission(getCurrentUser(), Resource.USER, operation,
                        user.getId(), user, entityGroupId);
            } else if (getCurrentUser().getId().equals(user.getId())) {
                accessControlService.checkPermission(getCurrentUser(), Resource.PROFILE, Operation.WRITE);
            }

            boolean sendEmail = user.getId() == null && sendActivationMail;
            User savedUser = checkNotNull(userService.saveUser(user));

            // Add Tenant Admins to 'Tenant Administrators' user group if created by Sys Admin
            if (operation == Operation.CREATE && getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
                EntityGroup admins = entityGroupService.findOrCreateTenantAdminsGroup(savedUser.getTenantId());
                entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, admins.getId(), savedUser.getId());
            } else if (entityGroupId != null && operation == Operation.CREATE) {
                entityGroupService.addEntityToEntityGroup(getTenantId(), entityGroupId, savedUser.getId());
            }

            if (sendEmail) {
                SecurityUser authUser = getCurrentUser();
                UserCredentials userCredentials = userService.findUserCredentialsByUserId(authUser.getTenantId(), savedUser.getId());
                String baseUrl = constructBaseUrl(request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                String email = savedUser.getEmail();
                try {
                    mailService.sendActivationEmail(getTenantId(), activateUrl, email);
                } catch (ThingsboardException e) {
                    userService.deleteUser(authUser.getTenantId(), savedUser.getId());
                    throw e;
                }
            }

            userPermissionsService.onUserUpdatedOrRemoved(savedUser);

            logEntityAction(savedUser.getId(), savedUser,
                    savedUser.getCustomerId(),
                    user.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedUser;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.USER), user,
                    null, user.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/sendActivationMail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void sendActivationEmail(
            @RequestParam(value = "email") String email,
            HttpServletRequest request) throws ThingsboardException {
        try {
            User user = checkNotNull(userService.findUserByEmail(getCurrentUser().getTenantId(), email));

            accessControlService.checkPermission(getCurrentUser(), Resource.USER, Operation.READ,
                    user.getId(), user);

            UserCredentials userCredentials = userService.findUserCredentialsByUserId(getCurrentUser().getTenantId(), user.getId());
            if (!userCredentials.isEnabled()) {
                String baseUrl = constructBaseUrl(request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                mailService.sendActivationEmail(getTenantId(), activateUrl, email);
            } else {
                throw new ThingsboardException("User is already active!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}/activationLink", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public String getActivationLink(
            @PathVariable(USER_ID) String strUserId,
            HttpServletRequest request) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.READ);
            SecurityUser authUser = getCurrentUser();
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(authUser.getTenantId(), user.getId());
            if (!userCredentials.isEnabled()) {
                String baseUrl = constructBaseUrl(request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                return activateUrl;
            } else {
                throw new ThingsboardException("User is already active!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteUser(@PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.DELETE);
            userService.deleteUser(getCurrentUser().getTenantId(), userId);

            userPermissionsService.onUserUpdatedOrRemoved(user);

            logEntityAction(userId, user,
                    user.getCustomerId(),
                    ActionType.DELETED, null, strUserId);

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.USER),
                    null,
                    null,
                    ActionType.DELETED, e, strUserId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getTenantAdmins(
            @PathVariable("tenantId") String strTenantId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(userService.findTenantAdmins(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getCustomerUsers(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            accessControlService.checkPermission(getCurrentUser(), Resource.USER, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(userService.findCustomerUsers(tenantId, customerId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getAllCustomerUsers(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.USER, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(userService.findAllCustomerUsers(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getUserUsers(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return getGroupEntities(getCurrentUser(), EntityType.USER, Operation.READ,
                    (groupIds) -> userService.findUsersByEntityGroupIds(groupIds, pageLink)
            );
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/users", params = {"userIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<User> getUsersByIds(
            @RequestParam("userIds") String[] strUserIds) throws ThingsboardException {
        checkArrayParameter("userIds", strUserIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<UserId> userIds = new ArrayList<>();
            for (String strUserId : strUserIds) {
                userIds.add(new UserId(toUUID(strUserId)));
            }
            List<User> users = checkNotNull(userService.findUsersByTenantIdAndIdsAsync(tenantId, userIds).get());
            return filterUsersByReadPermission(users);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}/userCredentialsEnabled", method = RequestMethod.POST)
    @ResponseBody
    public void setUserCredentialsEnabled(@PathVariable(USER_ID) String strUserId,
                                          @RequestParam(required = false, defaultValue = "true") boolean userCredentialsEnabled) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            checkUserId(userId, Operation.WRITE);
            TenantId tenantId = getCurrentUser().getTenantId();
            userService.setUserCredentialsEnabled(tenantId, userId, userCredentialsEnabled);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getUsersByEntityGroupId(
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = "Page size", required = true, allowableValues = "range[1, infinity]") @RequestParam int pageSize,
            @ApiParam(value = "Page", required = true, allowableValues = "range[0, infinity]") @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        EntityType entityType = entityGroup.getType();
        checkEntityGroupType(entityType);
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(userService.findUsersByEntityGroupId(entityGroupId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<User> filterUsersByReadPermission(List<User> users) {
        return users.stream().filter(user -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.USER, Operation.READ, user.getId(), user);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }
}
