/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;

import java.util.UUID;

@RestController
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
    @RequestMapping(value = "/groupPermission", method = RequestMethod.POST)
    @ResponseBody
    public GroupPermission saveGroupPermission(@RequestBody GroupPermission groupPermission) throws ThingsboardException {
        try {
            groupPermission.setTenantId(getCurrentUser().getTenantId());

            Operation operation = groupPermission.getId() == null ? Operation.CREATE : Operation.WRITE;

            accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, operation,
                    groupPermission.getId(), groupPermission);

            GroupPermission savedGroupPermission = checkNotNull(groupPermissionService.saveGroupPermission(getTenantId(), groupPermission));
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
            groupPermissionService.deleteGroupPermission(getTenantId(), groupPermissionId);
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
    @RequestMapping(value = "/groupPermissions/{userGroupId}", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<GroupPermission> getGroupPermissions(
            @RequestParam int limit,
            @PathVariable("userGroupId") String strUserGroupId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            EntityGroupId userGroupId = new EntityGroupId(UUID.fromString(strUserGroupId));
            checkEntityGroupId(userGroupId, Operation.READ);
            return checkNotNull(groupPermissionService.findGroupPermissionByTenantIdAndUserGroupId(tenantId, userGroupId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
