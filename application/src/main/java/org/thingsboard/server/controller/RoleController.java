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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.ROLE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ROLE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ROLE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class RoleController extends AutoCommitController {

    public static final String ROLE_ID = "roleId";
    public static final String ROLE_SHORT_DESCRIPTION = "Role Contains a set of permissions. Role has two types. " +
            "Generic Role may be assigned to the user group and will provide permissions for all entities of a certain type. " +
            "Group Role may be assigned to both user and entity group and will provides permissions only for the entities that belong to specified entity group. " +
            "The assignment of the Role to the User Group is done using [Group Permission Controller](/swagger-ui.html#/group-permission-controller).";

    public static final String ROLE_PERMISSIONS_DESCRIPTION = "Example of Generic Role with read-only permissions for any resource and all permissions for the 'DEVICE' and 'PROFILE' resources is listed below: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"name\": \"Read-Only User\",\n" +
            "  \"type\": \"GENERIC\",\n" +
            "  \"permissions\": {\n" +
            "    \"ALL\": [\n" +
            "      \"READ\",\n" +
            "      \"RPC_CALL\",\n" +
            "      \"READ_CREDENTIALS\",\n" +
            "      \"READ_ATTRIBUTES\",\n" +
            "      \"READ_TELEMETRY\"\n" +
            "    ],\n" +
            "    \"DEVICE\": [\n" +
            "      \"ALL\"\n" +
            "    ]\n" +
            "    \"PROFILE\": [\n" +
            "      \"ALL\"\n" +
            "    ]\n" +
            "  },\n" +
            "  \"additionalInfo\": {\n" +
            "    \"description\": \"Read-only permissions for everything, Write permissions for devices and own profile.\"\n" +
            "  }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\nExample of Group Role with read-only permissions. Note that the group role has no association with the resources. The type of the resource is taken from the entity group that this role is assigned to: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"name\": \"Entity Group Read-only User\",\n" +
            "  \"type\": \"GROUP\",\n" +
            "  \"permissions\": [\n" +
            "    \"READ\",\n" +
            "    \"RPC_CALL\",\n" +
            "    \"READ_CREDENTIALS\",\n" +
            "    \"READ_ATTRIBUTES\",\n" +
            "    \"READ_TELEMETRY\"\n" +
            "  ],\n" +
            "  \"additionalInfo\": {\n" +
            "    \"description\": \"Read-only permissions.\"\n" +
            "  }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    @ApiOperation(value = "Get Role by Id (getRoleById)",
            notes = "Fetch the Role object based on the provided Role Id. " +
                    ROLE_SHORT_DESCRIPTION + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/role/{roleId}", method = RequestMethod.GET)
    @ResponseBody
    public Role getRoleById(
            @ApiParam(value = ROLE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ROLE_ID) String strRoleId) throws ThingsboardException {
        checkParameter(ROLE_ID, strRoleId);
        return checkRoleId(new RoleId(toUUID(strRoleId)), Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Role (saveRole)",
            notes = "Creates or Updates the Role. When creating Role, platform generates Role Id as " + UUID_WIKI_LINK +
                    "The newly created Role id will be present in the response. " +
                    "Specify existing Role id to update the permission. " +
                    "Referencing non-existing Group Permission Id will cause 'Not Found' error." +
                    "\n\n" + ROLE_SHORT_DESCRIPTION + "\n\n" + ROLE_PERMISSIONS_DESCRIPTION +
                    ControllerConstants.RBAC_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/role", method = RequestMethod.POST)
    @ResponseBody
    public Role saveRole(
            @ApiParam(value = "A JSON value representing the role.", required = true)
            @RequestBody Role role) throws Exception {
        SecurityUser currentUser = getCurrentUser();
        try {
            role.setTenantId(currentUser.getTenantId());
            if (Authority.CUSTOMER_USER.equals(currentUser.getAuthority())) {
                role.setCustomerId(currentUser.getCustomerId());
            }
            checkEntity(role.getId(), role, Resource.ROLE, null);

            Role savedRole = checkNotNull(roleService.saveRole(getTenantId(), role));

            autoCommit(currentUser, savedRole.getId());

            userPermissionsService.onRoleUpdated(savedRole);

            notificationEntityService.logEntityAction(getTenantId(), savedRole.getId(), savedRole,
                    role.getId() == null ? ActionType.ADDED : ActionType.UPDATED, currentUser);

            sendEntityNotificationMsg(getTenantId(), savedRole.getId(),
                    role.getId() == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);

            return savedRole;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.ROLE), role,
                    role.getId() == null ? ActionType.ADDED : ActionType.UPDATED, currentUser, e);
            throw e;
        }
    }

    @ApiOperation(value = "Delete role (deleteRole)",
            notes = "Deletes the role. Referencing non-existing role Id will cause an error." + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/role/{roleId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRole(
            @ApiParam(value = ROLE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ROLE_ID) String strRoleId) throws Exception {
        checkParameter(ROLE_ID, strRoleId);
        try {
            RoleId roleId = new RoleId(toUUID(strRoleId));
            Role role = checkRoleId(roleId, Operation.DELETE);

            if (isUsed(role.getId(), getTenantId())) {
                throw new ThingsboardException("Role can't be deleted because it used by user group permissions!", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            roleService.deleteRole(getTenantId(), roleId);
            notificationEntityService.logEntityAction(getTenantId(), roleId, role, ActionType.DELETED, getCurrentUser(), strRoleId);

            sendEntityNotificationMsg(getTenantId(), roleId, EdgeEventActionType.DELETED);

        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.ROLE), ActionType.DELETED, getCurrentUser(), e, strRoleId);
            throw e;
        }
    }

    private boolean isUsed(RoleId roleId, TenantId tenantId) {
        return groupPermissionService.findGroupPermissionByTenantIdAndRoleId(tenantId, roleId, new PageLink(1)).getTotalElements() > 0;
    }

    @ApiOperation(value = "Get Roles (getRoles)",
            notes = "Returns a page of roles that are available for the current user. " + ROLE_SHORT_DESCRIPTION +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/roles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Role> getRoles(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = "Type of the role", allowableValues = "GENERIC, GROUP")
            @RequestParam(required = false) String type,
            @ApiParam(value = ROLE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ROLE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ROLE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);

        if (type != null && type.trim().length() > 0) {
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                return checkNotNull(roleService.findRolesByTenantIdAndType(tenantId, pageLink, RoleType.valueOf(type)));
            } else {
                return checkNotNull(roleService.findRolesByTenantIdAndCustomerIdAndType(tenantId, getCurrentUser().getCustomerId(), checkStrRoleType("type", type), pageLink));
            }
        } else {
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                return checkNotNull(roleService.findRolesByTenantId(tenantId, pageLink));
            } else {
                return checkNotNull(roleService.findRolesByTenantIdAndCustomerId(tenantId, getCurrentUser().getCustomerId(), pageLink));
            }
        }
    }

    @ApiOperation(value = "Get Roles By Ids (getRolesByIds)",
            notes = "Returns the list of rows based on their ids. " + "\n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/roles", params = {"roleIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Role> getRolesByIds(
            @ApiParam(value = "A list of role ids, separated by comma ','")
            @RequestParam("roleIds") String[] strRoleIds) throws Exception {
        checkArrayParameter("roleIds", strRoleIds);
        if (!accessControlService.hasPermission(getCurrentUser(), Resource.ROLE, Operation.READ)) {
            return Collections.emptyList();
        }
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<RoleId> roleIds = new ArrayList<>();
        for (String strRoleId : strRoleIds) {
            roleIds.add(new RoleId(toUUID(strRoleId)));
        }
        List<Role> roles = checkNotNull(roleService.findRolesByIdsAsync(tenantId, roleIds).get());
        return filterRolesByReadPermission(roles);
    }

    private List<Role> filterRolesByReadPermission(List<Role> roles) {
        return roles.stream().filter(role -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.ROLE, Operation.READ, role.getId(), role);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

}
