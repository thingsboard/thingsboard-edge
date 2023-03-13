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
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.GROUP_PERMISSION_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class GroupPermissionController extends BaseController {

    public static final String GROUP_PERMISSION_ID = "groupPermissionId";
    public static final String GROUP_PERMISSION_DESCRIPTION = "Group permission entity represents list of allowed operations for certain User Group to perform against certain Entity Group. " +
            "Basically, this entity wires three other entities: \n\n" +
            " * Role that defines set of allowed operations;\n" +
            " * User Group that defines set of users who may perform the operations; \n" +
            " * Entity Group that defines set of entities which will be accessible to users;\n\n";

    public static final String GROUP_PERMISSION_INFO_DESCRIPTION = GROUP_PERMISSION_DESCRIPTION + " Group Permission Info object extends the Group Permissions with the full information about Role and User and/or Entity Groups. ";

    @ApiOperation(value = "Get Group Permission (getGroupPermissionById)",
            notes = "Fetch the Group Permission object based on the provided Group Permission Id. " +
                    GROUP_PERMISSION_DESCRIPTION + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/groupPermission/{groupPermissionId}", method = RequestMethod.GET)
    @ResponseBody
    public GroupPermission getGroupPermissionById(
            @ApiParam(value = GROUP_PERMISSION_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        return checkGroupPermissionId(new GroupPermissionId(toUUID(strGroupPermissionId)), Operation.READ);
    }

    @ApiOperation(value = "Get Group Permission Info (getGroupPermissionInfoById)",
            notes = "Fetch the Group Permission Info object based on the provided Group Permission Id and the flag that controls what additional information to load: User or Entity Group. " +
                    GROUP_PERMISSION_INFO_DESCRIPTION + RBAC_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/groupPermission/info/{groupPermissionId}", method = RequestMethod.GET)
    @ResponseBody
    public GroupPermissionInfo getGroupPermissionInfoById(
            @ApiParam(value = GROUP_PERMISSION_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId,
            @ApiParam(value = "Load additional information about User('true') or Entity Group('false).", required = true)
            @RequestParam boolean isUserGroup) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        return checkGroupPermissionInfoId(new GroupPermissionId(toUUID(strGroupPermissionId)), Operation.READ, isUserGroup);
    }

    @ApiOperation(value = "Create Or Update Group Permission (saveGroupPermission)",
            notes = "Creates or Updates the Group Permission. When creating group permission, platform generates Group Permission Id as " + UUID_WIKI_LINK +
                    "The newly created Group Permission id will be present in the response. " +
                    "Specify existing Group Permission id to update the permission. " +
                    "Referencing non-existing Group Permission Id will cause 'Not Found' error." +
                    "\n\n" + GROUP_PERMISSION_DESCRIPTION + ControllerConstants.RBAC_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/groupPermission", method = RequestMethod.POST)
    @ResponseBody
    public GroupPermission saveGroupPermission(
            @ApiParam(value = "A JSON value representing the group permission.", required = true)
            @RequestBody GroupPermission groupPermission) throws ThingsboardException {
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

            notificationEntityService.logEntityAction(getTenantId(), savedGroupPermission.getId(), savedGroupPermission,
                    groupPermission.getId() == null ? ActionType.ADDED : ActionType.UPDATED, getCurrentUser());

            sendEntityNotificationMsg(getTenantId(), savedGroupPermission.getId(),
                    groupPermission.getId() == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);
            return savedGroupPermission;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.GROUP_PERMISSION), groupPermission,
                    groupPermission.getId() == null ? ActionType.ADDED : ActionType.UPDATED, getCurrentUser(), e);
            throw e;
        }
    }

    @ApiOperation(value = "Delete group permission (deleteGroupPermission)",
            notes = "Deletes the group permission. Referencing non-existing group permission Id will cause an error." + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/groupPermission/{groupPermissionId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteGroupPermission(
            @ApiParam(value = GROUP_PERMISSION_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId) throws ThingsboardException {
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

            notificationEntityService.logEntityAction(getTenantId(), groupPermissionId, groupPermission,
                    ActionType.DELETED, getCurrentUser(), strGroupPermissionId);

            sendEntityNotificationMsg(getTenantId(), groupPermissionId, EdgeEventActionType.DELETED);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(EntityType.GROUP_PERMISSION),
                    ActionType.DELETED, getCurrentUser(), e, strGroupPermissionId);
            throw e;
        }
    }

    @ApiOperation(value = "Get group permissions by User Group Id (getUserGroupPermissions)",
            notes = "Returns a list of group permission objects that belongs to specified User Group Id. " +
                    GROUP_PERMISSION_INFO_DESCRIPTION + "\n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/userGroup/{userGroupId}/groupPermissions", method = RequestMethod.GET)
    @ResponseBody
    public List<GroupPermissionInfo> getUserGroupPermissions(
            @ApiParam(value = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("userGroupId") String strUserGroupId) throws ThingsboardException, ExecutionException, InterruptedException {
        TenantId tenantId = getCurrentUser().getTenantId();
        EntityGroupId userGroupId = new EntityGroupId(UUID.fromString(strUserGroupId));
        checkEntityGroupId(userGroupId, Operation.READ);
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
        List<GroupPermissionInfo> groupPermissions = groupPermissionService.findGroupPermissionInfoListByTenantIdAndUserGroupIdAsync(tenantId, userGroupId).get();
        return applyPermissionInfo(groupPermissions);
    }

    @ApiOperation(value = "Load User Group Permissions (loadUserGroupPermissionInfos)",
            notes = "Enrich a list of group permission objects with the information about Role, User and Entity Groups. " +
                    GROUP_PERMISSION_INFO_DESCRIPTION + "\n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/userGroup/groupPermissions/info", method = RequestMethod.POST)
    @ResponseBody
    public List<GroupPermissionInfo> loadUserGroupPermissionInfos(
            @ApiParam(value = "JSON array of group permission objects", required = true)
            @RequestBody List<GroupPermission> permissions) throws ThingsboardException, ExecutionException, InterruptedException {
        TenantId tenantId = getCurrentUser().getTenantId();
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
        List<GroupPermissionInfo> permissionInfoList = groupPermissionService.loadUserGroupPermissionInfoListAsync(tenantId, permissions).get();
        return applyPermissionInfo(permissionInfoList);
    }

    @ApiOperation(value = "Get group permissions by Entity Group Id (getEntityGroupPermissions)",
            notes = "Returns a list of group permission objects that is assigned for the specified Entity Group Id. " +
                    GROUP_PERMISSION_INFO_DESCRIPTION + "\n\n" + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/groupPermissions", method = RequestMethod.GET)
    @ResponseBody
    public List<GroupPermissionInfo> getEntityGroupPermissions(
            @ApiParam(value = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityGroupId") String strEntityGroupId) throws ThingsboardException, ExecutionException, InterruptedException {
        TenantId tenantId = getCurrentUser().getTenantId();
        EntityGroupId entityGroupId = new EntityGroupId(UUID.fromString(strEntityGroupId));
        checkEntityGroupId(entityGroupId, Operation.READ);
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
        List<GroupPermissionInfo> groupPermissions = groupPermissionService.findGroupPermissionInfoListByTenantIdAndEntityGroupIdAsync(tenantId, entityGroupId).get();
        return applyPermissionInfo(groupPermissions);
    }

    private List<GroupPermissionInfo> applyPermissionInfo(List<GroupPermissionInfo> groupPermissions) throws ThingsboardException {
        groupPermissions = groupPermissions.stream().filter(gp -> gp != null && gp.getRole() != null).collect(Collectors.toList());
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
