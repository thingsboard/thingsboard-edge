/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
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

@RestController
@TbCoreComponent
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
            if (Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())) {
                role.setCustomerId(getCurrentUser().getCustomerId());
            }
            checkEntity(role.getId(), role, Resource.ROLE, null);

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

            PageData<GroupPermission> groupPermissions =
                    groupPermissionService.findGroupPermissionByTenantIdAndRoleId(getTenantId(), role.getId(), new PageLink(1));
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
    @RequestMapping(value = "/roles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Role> getRoles(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
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
