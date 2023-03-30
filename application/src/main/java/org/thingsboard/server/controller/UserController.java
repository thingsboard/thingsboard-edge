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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserEmailInfo;
import org.thingsboard.server.common.data.UserInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.UserSettings;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.user.TbUserService;
import org.thingsboard.server.service.query.EntityQueryService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.query.EntityKeyType.ENTITY_FIELD;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_DASHBOARD;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD;
import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD_HIDE_TOOLBAR;
import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD_ID;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.USER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.USER_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.USER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class UserController extends BaseController {

    public static final String USER_ID = "userId";
    public static final String PATHS = "paths";
    public static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";
    public static final String ACTIVATE_URL_PATTERN = "%s/api/noauth/activate?activateToken=%s";

    @Value("${security.user_token_access_enabled}")
    private boolean userTokenAccessEnabled;

    private final MailService mailService;
    private final UserPermissionsService userPermissionsService;
    private final JwtTokenFactory tokenFactory;
    private final SystemSecurityService systemSecurityService;
    private final ApplicationEventPublisher eventPublisher;
    private final TbUserService tbUserService;

    @Autowired
    private EntityQueryService entityQueryService;

    @ApiOperation(value = "Get User (getUserById)",
            notes = "Fetch the User object based on the provided User Id. " +
                    "If the user has the authority of 'SYS_ADMIN', the server does not perform additional checks. " +
                    "If the user has the authority of 'TENANT_ADMIN', the server checks that the requested user is owned by the same tenant. " +
                    "If the user has the authority of 'CUSTOMER_USER', the server checks that the requested user is owned by the same customer.\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public User getUserById(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.READ);
            if (user.getAdditionalInfo().isObject()) {
                ObjectNode additionalInfo = (ObjectNode) user.getAdditionalInfo();
                processDashboardIdFromAdditionalInfo(additionalInfo, DEFAULT_DASHBOARD);
                processDashboardIdFromAdditionalInfo(additionalInfo, HOME_DASHBOARD);
                UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
                if (userCredentials.isEnabled() && !additionalInfo.has("userCredentialsEnabled")) {
                    additionalInfo.put("userCredentialsEnabled", true);
                }
            }
            return user;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get User info (getUserInfoById)",
            notes = "Fetch the User info object based on the provided User Id. " +
                    "If the user has the authority of 'SYS_ADMIN', the server does not perform additional checks. " +
                    "If the user has the authority of 'TENANT_ADMIN', the server checks that the requested user is owned by the same tenant. " +
                    "If the user has the authority of 'CUSTOMER_USER', the server checks that the requested user is owned by the same customer.\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/info/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public UserInfo getUserInfoById(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            UserInfo user = checkUserInfoId(userId, Operation.READ);
            if (user.getAdditionalInfo().isObject()) {
                ObjectNode additionalInfo = (ObjectNode) user.getAdditionalInfo();
                processDashboardIdFromAdditionalInfo(additionalInfo, DEFAULT_DASHBOARD);
                processDashboardIdFromAdditionalInfo(additionalInfo, HOME_DASHBOARD);
                UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
                if (userCredentials.isEnabled() && !additionalInfo.has("userCredentialsEnabled")) {
                    additionalInfo.put("userCredentialsEnabled", true);
                }
            }
            return user;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check Token Access Enabled (isUserTokenAccessEnabled)",
            notes = "Checks that the system is configured to allow administrators to impersonate themself as other users. " +
                    "If the user who performs the request has the authority of 'SYS_ADMIN', it is possible to login as any tenant administrator. " +
                    "If the user who performs the request has the authority of 'TENANT_ADMIN', it is possible to login as any customer user.\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/tokenAccessEnabled", method = RequestMethod.GET)
    @ResponseBody
    public boolean isUserTokenAccessEnabled() {
        return userTokenAccessEnabled;
    }

    @ApiOperation(value = "Get User Token (getUserToken)",
            notes = "Returns the token of the User based on the provided User Id. " +
                    "If the user who performs the request has the authority of 'SYS_ADMIN', it is possible to get the token of any tenant administrator. " +
                    "If the user who performs the request has the authority of 'TENANT_ADMIN', it is possible to get the token of any customer user that belongs to the same tenant. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}/token", method = RequestMethod.GET)
    @ResponseBody
    public JwtPair getUserToken(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId) throws ThingsboardException {
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
            return tokenFactory.createTokenPair(securityUser);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Save Or update User (saveUser)",
            notes = "Create or update the User. When creating user, platform generates User Id as " + UUID_WIKI_LINK +
                    "The newly created User Id will be present in the response. " +
                    "Specify existing User Id to update the device. " +
                    "Referencing non-existing User Id will cause 'Not Found' error." +
                    "\n\nDevice email is unique for entire platform setup.\n\n" +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new User entity." +
                    RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    @ResponseBody
    public User saveUser(
            @ApiParam(value = "A JSON value representing the User.", required = true)
            @RequestBody User user,
            @ApiParam(value = "Send activation email (or use activation link)", defaultValue = "true")
            @RequestParam(required = false, defaultValue = "true") boolean sendActivationMail,
            @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
            @RequestParam(name = "entityGroupIds", required = false) String[] strEntityGroupIds,
            HttpServletRequest request) throws ThingsboardException {

        if (!Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
            user.setTenantId(getCurrentUser().getTenantId());
        }

        List<EntityGroupId> entityGroupIds = new ArrayList<>();
        List<EntityGroup> entityGroups = new ArrayList<>();
        String[] groupIds = null;
        if (!StringUtils.isEmpty(strEntityGroupId)) {
            groupIds = new String[]{strEntityGroupId};
        } else if (strEntityGroupIds != null && strEntityGroupIds.length > 0) {
            groupIds = strEntityGroupIds;
        }
        if (groupIds != null) {
            for (String id : groupIds) {
                EntityGroupId entityGroupId = new EntityGroupId(toUUID(id));
                EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
                entityGroupIds.add(entityGroupId);
                entityGroups.add(entityGroup);
            }
        }

        if (user.getId() == null && getCurrentUser().getAuthority() != Authority.SYS_ADMIN &&
                (user.getCustomerId() == null || user.getCustomerId().isNullUid())) {
            if (!entityGroups.isEmpty() && entityGroups.get(0).getOwnerId().getEntityType() == EntityType.CUSTOMER) {
                user.setOwnerId(new CustomerId(entityGroups.get(0).getOwnerId().getId()));
            } else if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                user.setOwnerId(getCurrentUser().getCustomerId());
            }
        }

        if (getCurrentUser().getId().equals(user.getId())) {
            accessControlService.checkPermission(getCurrentUser(), Resource.PROFILE, Operation.WRITE);
        } else {
            checkEntityWithGroupIds(user.getId(), user, Resource.USER, entityGroupIds);
        }

        if (!accessControlService.hasPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE)) {
            String prevHomeDashboardId = null;
            boolean prevHideDashboardToolbar = true;
            if (user.getId() != null) {
                User prevUser = userService.findUserById(getTenantId(), user.getId());
                JsonNode additionalInfo = prevUser.getAdditionalInfo();
                if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID)) {
                    prevHomeDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                    if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                        prevHideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                    }
                }
            }
            JsonNode additionalInfo = user.getAdditionalInfo();
            if (additionalInfo == null) {
                additionalInfo = JacksonUtil.newObjectNode();
                user.setAdditionalInfo(additionalInfo);
            }
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_ID, prevHomeDashboardId);
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_HIDE_TOOLBAR, prevHideDashboardToolbar);
        }

        return tbUserService.save(getTenantId(), getCurrentUser().getCustomerId(), getCurrentUser().getAuthority(),
                user, sendActivationMail, request, entityGroups, getCurrentUser());
    }

    @ApiOperation(value = "Send or re-send the activation email",
            notes = "Force send the activation email to the user. Useful to resend the email if user has accidentally deleted it.\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/sendActivationMail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void sendActivationEmail(
            @ApiParam(value = "Email of the user", required = true)
            @RequestParam(value = "email") String email,
            HttpServletRequest request) throws ThingsboardException {
        try {
            User user = checkNotNull(userService.findUserByEmail(getCurrentUser().getTenantId(), email));

            accessControlService.checkPermission(getCurrentUser(), Resource.USER, Operation.READ,
                    user.getId(), user);

            UserCredentials userCredentials = userService.findUserCredentialsByUserId(getCurrentUser().getTenantId(), user.getId());
            if (!userCredentials.isEnabled() && userCredentials.getActivateToken() != null) {
                String baseUrl = systemSecurityService.getBaseUrl(getCurrentUser().getAuthority(), getTenantId(), getCurrentUser().getCustomerId(), request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                mailService.sendActivationEmail(getTenantId(), activateUrl, email);
            } else {
                throw new ThingsboardException("User is already activated!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get the activation link (getActivationLink)",
            notes = "Get the activation link for the user. " +
                    "The base url for activation link is configurable in the general settings of system administrator. \n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}/activationLink", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public String getActivationLink(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId,
            HttpServletRequest request) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.READ);
            SecurityUser authUser = getCurrentUser();
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(authUser.getTenantId(), user.getId());
            if (!userCredentials.isEnabled() && userCredentials.getActivateToken() != null) {
                String baseUrl = systemSecurityService.getBaseUrl(authUser.getAuthority(), getTenantId(), authUser.getCustomerId(), request);
                return String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
            } else {
                throw new ThingsboardException("User is already activated!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete User (deleteUser)",
            notes = "Deletes the User, it's credentials and all the relations (from and to the User). " +
                    "Referencing non-existing User Id will cause an error. \n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteUser(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        UserId userId = new UserId(toUUID(strUserId));
        User user = checkUserId(userId, Operation.DELETE);
        if (user.getAuthority() == Authority.SYS_ADMIN && getCurrentUser().getId().equals(userId)) {
            throw new ThingsboardException("Sysadmin is not allowed to delete himself", ThingsboardErrorCode.PERMISSION_DENIED);
        }

        userPermissionsService.onUserUpdatedOrRemoved(user);

        tbUserService.delete(getTenantId(), getCurrentUser().getCustomerId(), user, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Users (getTenantAdmins)",
            notes = "Returns a page of users owned by tenant. " + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getTenantAdmins(
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(TENANT_ID) String strTenantId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
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

    @ApiOperation(value = "Get Customer Users (getCustomerUsers)",
            notes = "Returns a page of users owned by customer. "
                    + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getCustomerUsers(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
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

    @ApiOperation(value = "Find users by query (findUsersByQuery)",
            notes = "Returns page of user data objects. Search is been executed by email, firstName and " +
                    "lastName fields. " + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/users/info", method = RequestMethod.GET)
    @ResponseBody
    public PageData<UserEmailInfo> findUsersByQuery(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();

        EntityTypeFilter entityFilter = new EntityTypeFilter();
        entityFilter.setEntityType(EntityType.USER);
        EntityDataPageLink pageLink = new EntityDataPageLink(pageSize, page, textSearch, createEntityDataSortOrder(sortProperty, sortOrder));
        List<EntityKey> entityFields = Arrays.asList(new EntityKey(ENTITY_FIELD, "firstName"),
                new EntityKey(ENTITY_FIELD, "lastName"),
                new EntityKey(ENTITY_FIELD, "email"));

        EntityDataQuery query = new EntityDataQuery(entityFilter, pageLink, entityFields, null, null);

        return entityQueryService.findEntityDataByQuery(securityUser, query).mapData(entityData ->
        {
            Map<String, TsValue> fieldValues = entityData.getLatest().get(ENTITY_FIELD);
            return new UserEmailInfo(UserId.fromString(entityData.getEntityId().getId().toString()),
                    fieldValues.get("email").getValue(),
                    fieldValues.get("firstName").getValue(),
                    fieldValues.get("lastName").getValue());
        });
    }

    @ApiOperation(value = "Get Customer Users (getCustomerUsers)",
            notes = "Returns a page of users for the current tenant with authority 'CUSTOMER_USER'. "
                    + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getAllCustomerUsers(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
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

    @ApiOperation(value = "Get Users (getUsers)",
            notes = "Returns a page of user objects available for the current user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getUserUsers(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            SecurityUser currentUser = getCurrentUser();
            MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
            return entityService.findUserEntities(currentUser.getTenantId(), currentUser.getCustomerId(), mergedUserPermissions, EntityType.USER,
                    Operation.READ, null, pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get All User Infos for current user (getAllUserInfos)",
            notes = "Returns a page of user info objects owned by the tenant or the customer of a current user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/userInfos/all", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<UserInfo> getAllUserInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.USER, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                if (includeCustomers != null && includeCustomers) {
                    return checkNotNull(userService.findUserInfosByTenantId(tenantId, pageLink));
                } else {
                    return checkNotNull(userService.findTenantUserInfosByTenantId(tenantId, pageLink));
                }
            } else {
                CustomerId customerId = getCurrentUser().getCustomerId();
                if (includeCustomers != null && includeCustomers) {
                    return checkNotNull(userService.findUserInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
                } else {
                    return checkNotNull(userService.findUserInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer user Infos (getCustomerUserInfos)",
            notes = "Returns a page of user info objects owned by the specified customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/userInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<UserInfo> getCustomerUserInfos(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.USER, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (includeCustomers != null && includeCustomers) {
                return checkNotNull(userService.findUserInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
            } else {
                return checkNotNull(userService.findUserInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Users By Ids (getUsersByIds)",
            notes = "Requested users must be owned by tenant or assigned to customer which user is performing the request. "
                    + "\n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/users", params = {"userIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<User> getUsersByIds(
            @ApiParam(value = "A list of user ids, separated by comma ','", required = true)
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

    @ApiOperation(value = "Enable/Disable User credentials (setUserCredentialsEnabled)",
            notes = "Enables or Disables user credentials. Useful when you would like to block user account without deleting it. "
                    + PAGE_DATA_PARAMETERS + SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}/userCredentialsEnabled", method = RequestMethod.POST)
    @ResponseBody
    public void setUserCredentialsEnabled(
            @ApiParam(value = USER_ID_PARAM_DESCRIPTION)
            @PathVariable(USER_ID) String strUserId,
            @ApiParam(value = "Disable (\"true\") or enable (\"false\") the credentials.", defaultValue = "true")
            @RequestParam(required = false, defaultValue = "true") boolean userCredentialsEnabled) throws
            ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            checkUserId(userId, Operation.WRITE);
            TenantId tenantId = getCurrentUser().getTenantId();
            userService.setUserCredentialsEnabled(tenantId, userId, userCredentialsEnabled);

            if (!userCredentialsEnabled) {
                eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(userId));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get users by Entity Group Id (getUsersByEntityGroupId)",
            notes = "Returns a page of user objects that belongs to specified Entity Group Id. " +
                    PAGE_DATA_PARAMETERS + "\n\n" + RBAC_GROUP_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/users", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<User> getUsersByEntityGroupId(
            @ApiParam(value = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = USER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = USER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        checkEntityGroupType(EntityType.USER, entityGroup.getType());
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

    @ApiOperation(value = "Save user settings (saveUserSettings)",
            notes = "Save user settings represented in json format for authorized user. " )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/user/settings")
    public JsonNode saveUserSettings(@RequestBody JsonNode settings) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();

        UserSettings userSettings = new UserSettings();
        userSettings.setSettings(settings);
        userSettings.setUserId(currentUser.getId());
        return userSettingsService.saveUserSettings(currentUser.getTenantId(), userSettings).getSettings();
    }

    @ApiOperation(value = "Update user settings (saveUserSettings)",
            notes = "Update user settings for authorized user. Only specified json elements will be updated." +
                    "Example: you have such settings: {A:5, B:{C:10, D:20}}. Updating it with {B:{C:10, D:30}} will result in" +
                    "{A:5, B:{C:10, D:30}}. The same could be achieved by putting {B.D:30}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = "/user/settings")
    public void putUserSettings(@RequestBody JsonNode settings) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        userSettingsService.updateUserSettings(currentUser.getTenantId(), currentUser.getId(), settings);
    }

    @ApiOperation(value = "Get user settings (getUserSettings)",
            notes = "Fetch the User settings based on authorized user. " )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/user/settings")
    public JsonNode getUserSettings() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();

        UserSettings userSettings = userSettingsService.findUserSettings(currentUser.getTenantId(), currentUser.getId());
        return userSettings == null ? JacksonUtil.newObjectNode(): userSettings.getSettings();
    }

    @ApiOperation(value = "Delete user settings (deleteUserSettings)",
            notes = "Delete user settings by specifying list of json element xpaths. \n " +
                    "Example: to delete B and C element in { \"A\": {\"B\": 5}, \"C\": 15} send A.B,C in jsonPaths request parameter" )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/settings/{paths}", method = RequestMethod.DELETE)
    public void deleteUserSettings(@ApiParam(value = PATHS)
                                        @PathVariable(PATHS) String paths) throws ThingsboardException {
        checkParameter(USER_ID, paths);

        SecurityUser currentUser = getCurrentUser();
        userSettingsService.deleteUserSettings(currentUser.getTenantId(), currentUser.getId(), Arrays.asList(paths.split(",")));
    }

}
