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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class GroupPermissionController extends BaseController {

    public static final String GROUP_PERMISSION_ID = "groupPermissionId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/groupPermission/{groupPermissionId}", method = RequestMethod.GET)
    @ResponseBody
    public GroupPermission getGroupPermissionById(@PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        try {
            return checkGroupPermissionId(new GroupPermissionId(toUUID(strGroupPermissionId)), Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/groupPermission/info/{groupPermissionId}", method = RequestMethod.GET)
    @ResponseBody
    public GroupPermissionInfo getGroupPermissionInfoById(
            @PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId,
            @RequestParam boolean isUserGroup) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        try {
            return checkGroupPermissionInfoId(new GroupPermissionId(toUUID(strGroupPermissionId)), Operation.READ, isUserGroup);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/groupPermission", method = RequestMethod.POST)
    @ResponseBody
    public GroupPermission saveGroupPermission(@RequestBody GroupPermission groupPermission) throws ThingsboardException {
        try {
            groupPermission.setTenantId(getCurrentUser().getTenantId());

            checkEntity(groupPermission.getId(), groupPermission, Resource.GROUP_PERMISSION, null);

            if (groupPermission.isPublic()) {
                throw permissionDenied();
            }

            Role role = checkRoleId(groupPermission.getRoleId(), Operation.READ);
            if (groupPermission.getUserGroupId() != null && !groupPermission.getUserGroupId().isNullUid()) {
                checkEntityGroupId(groupPermission.getUserGroupId(), Operation.WRITE);
            }
            if (groupPermission.getEntityGroupId() != null && !groupPermission.getEntityGroupId().isNullUid()) {
                if (role.getType() == RoleType.GENERIC) {
                    throw new IllegalArgumentException("Can't assign Generic Role to entity group!");
                }
                checkEntityGroupId(groupPermission.getEntityGroupId(), Operation.WRITE);
            }

            GroupPermission savedGroupPermission = checkNotNull(groupPermissionService.saveGroupPermission(getTenantId(), groupPermission));

            userPermissionsService.onGroupPermissionUpdated(savedGroupPermission);

            logEntityAction(savedGroupPermission.getId(), savedGroupPermission, null,
                    groupPermission.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);
            return savedGroupPermission;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.GROUP_PERMISSION), groupPermission, null,
                    groupPermission.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/groupPermission/{groupPermissionId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteGroupPermission(@PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        try {
            GroupPermissionId groupPermissionId = new GroupPermissionId(toUUID(strGroupPermissionId));
            GroupPermission groupPermission = checkGroupPermissionId(groupPermissionId, Operation.DELETE);
            if (groupPermission.isPublic()) {
                throw permissionDenied();
            }

            checkRoleId(groupPermission.getRoleId(), Operation.READ);
            if (groupPermission.getUserGroupId() != null && !groupPermission.getUserGroupId().isNullUid()) {
                checkEntityGroupId(groupPermission.getUserGroupId(), Operation.WRITE);
            }
            if (groupPermission.getEntityGroupId() != null && !groupPermission.getEntityGroupId().isNullUid()) {
                checkEntityGroupId(groupPermission.getEntityGroupId(), Operation.WRITE);
            }

            groupPermissionService.deleteGroupPermission(getTenantId(), groupPermissionId);
            userPermissionsService.onGroupPermissionDeleted(groupPermission);

            logEntityAction(groupPermissionId, groupPermission, null, ActionType.DELETED, null, strGroupPermissionId);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.GROUP_PERMISSION),
                    null,
                    null,
                    ActionType.DELETED, e, strGroupPermissionId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/userGroup/{userGroupId}/groupPermissions", method = RequestMethod.GET)
    @ResponseBody
    public List<GroupPermissionInfo> getUserGroupPermissions(
            @PathVariable("userGroupId") String strUserGroupId) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            EntityGroupId userGroupId = new EntityGroupId(UUID.fromString(strUserGroupId));
            checkEntityGroupId(userGroupId, Operation.READ);
            accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
            List<GroupPermissionInfo> groupPermissions = groupPermissionService.findGroupPermissionInfoListByTenantIdAndUserGroupIdAsync(tenantId, userGroupId).get();
            return applyPermissionInfo(groupPermissions);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/userGroup/groupPermissions/info", method = RequestMethod.POST)
    @ResponseBody
    public List<GroupPermissionInfo> loadUserGroupPermissionInfos(
           @RequestBody List<GroupPermission> permissions) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
            List<GroupPermissionInfo> permissionInfoList = groupPermissionService.loadUserGroupPermissionInfoListAsync(tenantId, permissions).get();
            return applyPermissionInfo(permissionInfoList);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/groupPermissions", method = RequestMethod.GET)
    @ResponseBody
    public List<GroupPermissionInfo> getEntityGroupPermissions(
            @PathVariable("entityGroupId") String strEntityGroupId) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            EntityGroupId entityGroupId = new EntityGroupId(UUID.fromString(strEntityGroupId));
            checkEntityGroupId(entityGroupId, Operation.READ);
            accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
            List<GroupPermissionInfo> groupPermissions = groupPermissionService.findGroupPermissionInfoListByTenantIdAndEntityGroupIdAsync(tenantId, entityGroupId).get();
            return applyPermissionInfo(groupPermissions);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<GroupPermissionInfo> applyPermissionInfo(List<GroupPermissionInfo> groupPermissions) throws ThingsboardException {
        for (GroupPermissionInfo groupPermission : groupPermissions) {
            Role role = groupPermission.getRole();
            groupPermission.setReadOnly(!accessControlService.hasPermission(getCurrentUser(), Resource.ROLE, Operation.READ, role.getId(), role));
            if (groupPermission.isPublic()) {
                groupPermission.setReadOnly(true);
            }
        }
        return groupPermissions;
    }

}
