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

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.MergedGroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefaultUserPermissionsService implements UserPermissionsService {


    private static MergedUserPermissions sysAdminPermissions;
    static {
        Map<Resource, Set<Operation>> sysAdminGenericPermissions = new HashMap<>();
        sysAdminGenericPermissions.put(Resource.ADMIN_SETTINGS, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.DASHBOARD, new HashSet<>(Arrays.asList(Operation.READ)));
        sysAdminGenericPermissions.put(Resource.ALARM, new HashSet<>(Arrays.asList(Operation.READ)));
        sysAdminGenericPermissions.put(Resource.TENANT, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.RULE_CHAIN, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.USER, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.WIDGETS_BUNDLE, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.WIDGET_TYPE, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.WHITE_LABELING, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminPermissions = new MergedUserPermissions(sysAdminGenericPermissions, new HashMap<>());
    }

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired
    private UserPermissionsCacheService userPermissionsCacheService;

    @Override
    public MergedUserPermissions getMergedPermissions(User user) throws Exception {
        if (user.getAuthority() == Authority.SYS_ADMIN) {
            return sysAdminPermissions;
        }
        byte[] data = userPermissionsCacheService.getMergedPermissions(user.getTenantId(), user.getCustomerId(), user.getId());
        MergedUserPermissions result = null;
        if (data.length != 0) {
            try {
                result = fromBytes(data);
            } catch (InvalidProtocolBufferException e) {
                log.warn("[{}][{}][{}] Failed to decode merged user permissions from cache: {}", user.getTenantId(), user.getCustomerId(), user.getId(), Arrays.toString(data));
            }
        }
        if (result == null) {
            ListenableFuture<List<EntityGroupId>> groups = entityGroupService.findEntityGroupsForEntity(user.getTenantId(), user.getId());
            ListenableFuture<List<GroupPermission>> permissions = Futures.transformAsync(groups, toGroupPermissionsList(user.getTenantId()), dbCallbackExecutorService);
            result = Futures.transform(permissions, groupPermissions -> toMergedUserPermissions(user.getTenantId(), groupPermissions), dbCallbackExecutorService).get();
            userPermissionsCacheService.putMergedPermissions(user.getTenantId(), user.getCustomerId(), user.getId(), toBytes(result));
        }
        return result;
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
        Map<EntityGroupId, MergedGroupPermissionInfo> groupSpecificPermissions = new HashMap<>();
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
        Map<Resource, List<Operation>> rolePermissions = new HashMap<>();
        for (Resource resource : Resource.values()) {
            if (role.getPermissions().has(resource.name())) {
                List<Operation> operations = new ArrayList<>();
                rolePermissions.put(resource, operations);
                role.getPermissions().get(resource.name()).forEach(node -> operations.add(Operation.valueOf(node.asText())));
            }
        }
        rolePermissions.forEach(((resource, operations) -> target.computeIfAbsent(resource, r -> new HashSet<>()).addAll(operations)));
    }

    private void addGroupSpecificRolePermissions(TenantId tenantId, Map<EntityGroupId, MergedGroupPermissionInfo> target, GroupPermission groupPermission) {
        Role role = roleService.findRoleById(tenantId, groupPermission.getRoleId());
        List<Operation> roleOperations = new ArrayList<>();
        role.getPermissions().forEach(node -> roleOperations.add(Operation.valueOf(node.asText())));
        target.computeIfAbsent(groupPermission.getEntityGroupId(), id -> new MergedGroupPermissionInfo(groupPermission.getEntityGroupType(), new HashSet<>())).getOperations().addAll(roleOperations);
    }

    private byte[] toBytes(MergedUserPermissions result) {
        ClusterAPIProtos.MergedUserPermissionsProto.Builder builder = ClusterAPIProtos.MergedUserPermissionsProto.newBuilder();
        result.getGenericPermissions().forEach(((resource, operations) -> {
            builder.addGeneric(ClusterAPIProtos.GenericUserPermissionsProto.newBuilder()
                    .setResource(resource.name())
                    .addAllOperation(operations.stream().map(Operation::name).collect(Collectors.toList())));
        }));
        result.getGroupPermissions().forEach((entityGroupId, mergedGroupPermissionInfo) -> {
            builder.addGroup(ClusterAPIProtos.GroupUserPermissionsProto.newBuilder()
                    .setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits())
                    .setEntityType(mergedGroupPermissionInfo.getEntityType().name())
                    .addAllOperation(mergedGroupPermissionInfo.getOperations().stream().map(Operation::name).collect(Collectors.toList()))
            );
        });

        return builder.build().toByteArray();
    }

    private MergedUserPermissions fromBytes(byte[] data) throws InvalidProtocolBufferException {
        ClusterAPIProtos.MergedUserPermissionsProto proto = ClusterAPIProtos.MergedUserPermissionsProto.parseFrom(data);
        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        Map<EntityGroupId, MergedGroupPermissionInfo> groupSpecificPermissions = new HashMap<>();

        for (ClusterAPIProtos.GenericUserPermissionsProto genericPermissionsProto : proto.getGenericList()) {
            HashSet<Operation> operations = new HashSet<>();
            genericPermissionsProto.getOperationList().forEach(o -> operations.add(Operation.valueOf(o)));
            genericPermissions.put(Resource.valueOf(genericPermissionsProto.getResource()), operations);
        }
        for (ClusterAPIProtos.GroupUserPermissionsProto groupPermissionsProto : proto.getGroupList()) {
            HashSet<Operation> operations = new HashSet<>();
            groupPermissionsProto.getOperationList().forEach(o -> operations.add(Operation.valueOf(o)));
            groupSpecificPermissions.put(new EntityGroupId(new UUID(groupPermissionsProto.getEntityGroupIdMSB(), groupPermissionsProto.getEntityGroupIdLSB())),
                    new MergedGroupPermissionInfo(EntityType.valueOf(groupPermissionsProto.getEntityType()), operations));
        }
        return new MergedUserPermissions(genericPermissions, groupSpecificPermissions);
    }

}
