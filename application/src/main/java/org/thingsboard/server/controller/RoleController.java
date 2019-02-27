/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
public class RoleController extends BaseController {

    public static final String ROLE_ID = "roleId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/role/{roleId}", method = RequestMethod.GET)
    @ResponseBody
    public Role getRoleById(@PathVariable(ROLE_ID) String strRoleId) throws ThingsboardException {
        checkParameter(ROLE_ID, strRoleId);
        try {
            return checkRoleId(new RoleId(toUUID(strRoleId)), Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/role", method = RequestMethod.POST)
    @ResponseBody
    public Role saveRole(@RequestBody Role role) throws ThingsboardException {
        try {
            role.setTenantId(getCurrentUser().getTenantId());
            if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                role.setCustomerId(getCurrentUser().getCustomerId());
            }
            Operation operation = role.getId() == null ? Operation.CREATE : Operation.WRITE;

            if (operation == Operation.CREATE && getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                role.setCustomerId(getCurrentUser().getCustomerId());
            }

            accessControlService.checkPermission(getCurrentUser(), Resource.ROLE, operation,
                    role.getId(), role);

            Role savedRole = checkNotNull(roleService.saveRole(getTenantId(), role));

            userPermissionsService.onRoleUpdated(savedRole);

            logEntityAction(savedRole.getId(), savedRole, null,
                    role.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);
            return savedRole;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ROLE), role, null,
                    role.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/role/{roleId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRole(@PathVariable(ROLE_ID) String strRoleId) throws ThingsboardException {
        checkParameter(ROLE_ID, strRoleId);
        try {
            RoleId roleId = new RoleId(toUUID(strRoleId));
            Role role = checkRoleId(roleId, Operation.DELETE);

            TimePageData<GroupPermission> groupPermissions =
                    groupPermissionService.findGroupPermissionByTenantIdAndRoleId(getTenantId(), role.getId(), new TimePageLink(1));
            if (!groupPermissions.getData().isEmpty()) {
                throw new ThingsboardException("Role can't be deleted because it used by user group permissions!", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            roleService.deleteRole(getTenantId(), roleId);
            logEntityAction(roleId, role, null, ActionType.DELETED, null, strRoleId);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ROLE),
                    null,
                    null,
                    ActionType.DELETED, e, strRoleId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/roles", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Role> getRoles(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ROLE, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);

            if (type != null && type.trim().length() > 0) {
                if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
                    return checkNotNull(roleService.findRolesByTenantIdAndType(tenantId, pageLink, RoleType.valueOf(type)));
                } else {
                    return checkNotNull(roleService.findRolesByTenantIdAndCustomerIdAndType(tenantId, getCurrentUser().getCustomerId(), checkStrRoleType("type", type), pageLink));
                }
            } else {
                if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
                    return checkNotNull(roleService.findRolesByTenantId(tenantId, pageLink));
                } else {
                    return checkNotNull(roleService.findRolesByTenantIdAndCustomerId(tenantId, getCurrentUser().getCustomerId(), pageLink));
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/roles", params = {"roleIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Role> getRolesByIds(
            @RequestParam("roleIds") String[] strRoleIds) throws ThingsboardException {
        checkArrayParameter("roleIds", strRoleIds);
        try {
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
        } catch (Exception e) {
            throw handleException(e);
        }
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
