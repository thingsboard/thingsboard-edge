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
package org.thingsboard.server.service.security.permission;

import org.codehaus.jackson.type.TypeReference;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_CACHE;
import static org.thingsboard.server.common.data.CacheConstants.USER_PERMISSIONS_CACHE;

@Service
public class DefaultUserPermissionsService implements UserPermissionsService {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Cacheable(cacheNames = USER_PERMISSIONS_CACHE, key = "{#customerId, #userId}")
    @Override
    public MergedUserPermissions getMergedPermissions(TenantId tenantId, CustomerId customerId, UserId userId) throws Exception {
        ListenableFuture<List<EntityGroupId>> groups = entityGroupService.findEntityGroupsForEntity(TenantId.SYS_TENANT_ID, userId);
        ListenableFuture<List<GroupPermission>> permissions = Futures.transformAsync(groups, toGroupPermissionsList(tenantId), dbCallbackExecutorService);
        return Futures.transform(permissions, groupPermissions -> toMergedUserPermissions(tenantId, groupPermissions), dbCallbackExecutorService).get();
    }

    private AsyncFunction<List<EntityGroupId>, List<GroupPermission>> toGroupPermissionsList(TenantId tenantId) {
        return groupIds -> {
            List<GroupPermission> result = new ArrayList<>(groupIds.size());
            for (EntityGroupId userGroupId : groupIds) {
                result.addAll(groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(tenantId, userGroupId));
            }
            return Futures.immediateFuture(result);
        };
    }

    private MergedUserPermissions toMergedUserPermissions(TenantId tenantId, List<GroupPermission> groupPermissions) {
        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        Map<EntityGroupId, Set<Operation>> groupSpecificPermissions = new HashMap<>();
        for (GroupPermission groupPermission : groupPermissions) {
            if (groupPermission.getEntityGroupId() == null) {
                addGenericRolePermissions(tenantId, genericPermissions, groupPermission);
            } else {
                addGroupSpecificRolePermissions(tenantId, groupSpecificPermissions, groupPermission);
            }
        }
        return new MergedUserPermissions(genericPermissions, groupSpecificPermissions);
    }

    private void addGenericRolePermissions(TenantId tenantId, Map<Resource, Set<Operation>> target, GroupPermission groupPermission) {
        Role role = roleService.findRoleById(tenantId, groupPermission.getRoleId());
        Map<Resource, List<Operation>> rolePermissions = mapper.convertValue(role.getPermissions(), new TypeReference<Map<Resource, List<Operation>>>() {
        });
        rolePermissions.forEach(((resource, operations) -> target.computeIfAbsent(resource, r -> new HashSet<>()).addAll(operations)));
    }

    private void addGroupSpecificRolePermissions(TenantId tenantId, Map<EntityGroupId, Set<Operation>> target, GroupPermission groupPermission) {
        Role role = roleService.findRoleById(tenantId, groupPermission.getRoleId());
        List<Operation> roleOperations = mapper.convertValue(role.getPermissions(), new TypeReference<List<Operation>>() {
        });
        target.computeIfAbsent(groupPermission.getEntityGroupId(), id -> new HashSet<>()).addAll(roleOperations);
    }

}
