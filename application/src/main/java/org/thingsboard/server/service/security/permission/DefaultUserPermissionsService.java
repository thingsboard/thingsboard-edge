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
package org.thingsboard.server.service.security.permission;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.MergedGroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.USER_PERMISSIONS_CACHE;

@Slf4j
@Service
public class DefaultUserPermissionsService implements UserPermissionsService {

    @Autowired
    private CacheManager cacheManager;

    private static MergedUserPermissions sysAdminPermissions;

    static {
        Map<Resource, Set<Operation>> sysAdminGenericPermissions = new HashMap<>();
        sysAdminGenericPermissions.put(Resource.PROFILE, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.ADMIN_SETTINGS, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.DASHBOARD, new HashSet<>(Arrays.asList(Operation.READ)));
        sysAdminGenericPermissions.put(Resource.ALARM, new HashSet<>(Arrays.asList(Operation.READ)));
        sysAdminGenericPermissions.put(Resource.TENANT, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.TENANT_PROFILE, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.RULE_CHAIN, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.USER, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.WIDGETS_BUNDLE, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.WIDGET_TYPE, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.WHITE_LABELING, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminGenericPermissions.put(Resource.TB_RESOURCE, new HashSet<>(Arrays.asList(Operation.ALL)));
        sysAdminPermissions = new MergedUserPermissions(sysAdminGenericPermissions, new HashMap<>());
    }

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Override
    public MergedUserPermissions getMergedPermissions(User user, boolean isPublic) throws ThingsboardException {
        if (Authority.SYS_ADMIN.equals(user.getAuthority())) {
            return sysAdminPermissions;
        }
        MergedUserPermissions result = getMergedPermissionsFromCache(user.getTenantId(), user.getCustomerId(), user.getId());
        if (result == null) {
            ListenableFuture<List<EntityGroupId>> groups;
            if (isPublic) {
                ListenableFuture<Optional<EntityGroup>> publicUserGroup = entityGroupService.findPublicUserGroup(user.getTenantId(), user.getCustomerId());
                groups = Futures.transform(publicUserGroup, groupOptional -> {
                    if (groupOptional.isPresent()) {
                        return Arrays.asList(groupOptional.get().getId());
                    } else {
                        return Collections.emptyList();
                    }
                }, MoreExecutors.directExecutor());
            } else {
                groups = entityGroupService.findEntityGroupsForEntity(user.getTenantId(), user.getId());
            }
            ListenableFuture<List<GroupPermission>> permissions = Futures.transformAsync(groups, toGroupPermissionsList(user.getTenantId()), dbCallbackExecutorService);
            try {
                result = Futures.transform(permissions, groupPermissions -> toMergedUserPermissions(user.getTenantId(), groupPermissions), dbCallbackExecutorService).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
            }
            putMergedPermissionsToCache(user.getTenantId(), user.getCustomerId(), user.getId(), result);
        }
        return result;
    }

    @Override
    public void onRoleUpdated(Role role) throws ThingsboardException {
        PageData<GroupPermission> groupPermissions =
                groupPermissionService.findGroupPermissionByTenantIdAndRoleId(role.getTenantId(), role.getId(), new PageLink(Integer.MAX_VALUE));
        Set<EntityGroupId> uniqueUserGroups = new HashSet<>();
        for (GroupPermission gpe : groupPermissions.getData()) {
            uniqueUserGroups.add(gpe.getUserGroupId());
        }
        evictCacheForUserGroups(role.getTenantId(), uniqueUserGroups, false);
    }

    @Override
    public void onGroupPermissionUpdated(GroupPermission groupPermission) throws ThingsboardException {
        evictCacheForUserGroups(groupPermission.getTenantId(), Collections.singleton(groupPermission.getUserGroupId()), groupPermission.isPublic());
    }

    @Override
    public void onGroupPermissionDeleted(GroupPermission groupPermission) throws ThingsboardException {
        evictCacheForUserGroups(groupPermission.getTenantId(), Collections.singleton(groupPermission.getUserGroupId()), groupPermission.isPublic());
    }

    @Override
    public void onUserUpdatedOrRemoved(User user) {
        evictMergedPermissionsToCache(user.getTenantId(), user.getCustomerId(), user.getId());
    }

    private void evictCacheForUserGroups(TenantId tenantId, Set<EntityGroupId> uniqueUserGroups, boolean isPublic) throws ThingsboardException {
        Map<EntityId, Set<EntityId>> usersByOwnerMap = new HashMap<>();
        for (EntityGroupId userGroupId : uniqueUserGroups) {
            EntityGroup userGroup = entityGroupService.findEntityGroupById(tenantId, userGroupId);
            try {
                List<EntityId> entityIds;
                if (isPublic) {
                    entityIds = Collections.singletonList(new UserId(EntityId.NULL_UUID));
                } else {
                    entityIds = entityGroupService.findAllEntityIds(tenantId, userGroupId, new PageLink(Integer.MAX_VALUE)).get();
                }
                usersByOwnerMap.computeIfAbsent(userGroup.getOwnerId(), ownerId -> new HashSet<>()).addAll(entityIds);
            } catch (InterruptedException | ExecutionException e) {
                throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
            }
        }
        usersByOwnerMap.forEach((ownerId, userIds) ->
                userIds.forEach(userId -> evictMergedPermissionsToCache(tenantId,
                                EntityType.CUSTOMER.equals(ownerId.getEntityType()) ? ownerId : new CustomerId(CustomerId.NULL_UUID), userId)));
    }

    private MergedUserPermissions getMergedPermissionsFromCache(TenantId tenantId, CustomerId customerId, UserId userId) {
        Cache cache = cacheManager.getCache(USER_PERMISSIONS_CACHE);
        String cacheKey = toKey(tenantId, customerId, userId);
        byte[] data = cache.get(cacheKey, byte[].class);
        MergedUserPermissions result = null;
        if (data != null && data.length > 0) {
            try {
                result = fromBytes(data);
            } catch (InvalidProtocolBufferException e) {
                log.warn("[{}][{}][{}] Failed to decode merged user permissions from cache: {}", tenantId, customerId, userId, Arrays.toString(data));
            }
        } else {
            log.debug("[{}][{}][{}] Not user permissions in cache", tenantId, customerId, userId);
        }
        return result;
    }

    private void putMergedPermissionsToCache(TenantId tenantId, CustomerId customerId, UserId userId, MergedUserPermissions permissions) {
        log.debug("[{}][{}][{}] Pushing user permissions to cache: {}", tenantId, customerId, userId, permissions);
        Cache cache = cacheManager.getCache(USER_PERMISSIONS_CACHE);
        cache.put(toKey(tenantId, customerId, userId), toBytes(permissions));
    }

    private void evictMergedPermissionsToCache(EntityId tenantId, EntityId customerId, EntityId userId) {
        log.debug("[{}][{}][{}] Evict user permissions to cache", tenantId, customerId, userId);
        Cache cache = cacheManager.getCache(USER_PERMISSIONS_CACHE);
        cache.evict(toKey(tenantId, customerId, userId));
    }

    private String toKey(EntityId tenantId, EntityId customerId, EntityId userId) {
        return (tenantId != null ? tenantId.getId().toString() : "null") + "-" +
                (customerId != null ? customerId.getId().toString() : "null") + "-" +
                (userId != null ? userId.getId().toString() : "null") + "-";
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
            Role role = roleService.findRoleById(tenantId, groupPermission.getRoleId());
            if (role.getType() == RoleType.GENERIC) {
                addGenericRolePermissions(role, genericPermissions, groupPermission);
            } else {
                addGroupSpecificRolePermissions(role, groupSpecificPermissions, groupPermission);
            }
        }
        return new MergedUserPermissions(genericPermissions, groupSpecificPermissions);
    }

    private void addGenericRolePermissions(Role role, Map<Resource, Set<Operation>> target, GroupPermission groupPermission) {
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

    private void addGroupSpecificRolePermissions(Role role, Map<EntityGroupId, MergedGroupPermissionInfo> target, GroupPermission groupPermission) {
        List<Operation> roleOperations = new ArrayList<>();
        role.getPermissions().forEach(node -> roleOperations.add(Operation.valueOf(node.asText())));
        target.computeIfAbsent(groupPermission.getEntityGroupId(), id -> new MergedGroupPermissionInfo(groupPermission.getEntityGroupType(), new HashSet<>())).getOperations().addAll(roleOperations);
    }

    private byte[] toBytes(MergedUserPermissions result) {
        TransportProtos.MergedUserPermissionsProto.Builder builder = TransportProtos.MergedUserPermissionsProto.newBuilder();
        result.getGenericPermissions().forEach(((resource, operations) -> {
            builder.addGeneric(TransportProtos.GenericUserPermissionsProto.newBuilder()
                    .setResource(resource.name())
                    .addAllOperation(operations.stream().map(Operation::name).collect(Collectors.toList())));
        }));
        result.getGroupPermissions().forEach((entityGroupId, mergedGroupPermissionInfo) -> {
            builder.addGroup(TransportProtos.GroupUserPermissionsProto.newBuilder()
                    .setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits())
                    .setEntityType(mergedGroupPermissionInfo.getEntityType().name())
                    .addAllOperation(mergedGroupPermissionInfo.getOperations().stream().map(Operation::name).collect(Collectors.toList()))
            );
        });

        return builder.build().toByteArray();
    }

    private MergedUserPermissions fromBytes(byte[] data) throws InvalidProtocolBufferException {
        TransportProtos.MergedUserPermissionsProto proto = TransportProtos.MergedUserPermissionsProto.parseFrom(data);
        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        Map<EntityGroupId, MergedGroupPermissionInfo> groupSpecificPermissions = new HashMap<>();

        for (TransportProtos.GenericUserPermissionsProto genericPermissionsProto : proto.getGenericList()) {
            HashSet<Operation> operations = new HashSet<>();
            genericPermissionsProto.getOperationList().forEach(o -> operations.add(Operation.valueOf(o)));
            genericPermissions.put(Resource.valueOf(genericPermissionsProto.getResource()), operations);
        }
        for (TransportProtos.GroupUserPermissionsProto groupPermissionsProto : proto.getGroupList()) {
            HashSet<Operation> operations = new HashSet<>();
            groupPermissionsProto.getOperationList().forEach(o -> operations.add(Operation.valueOf(o)));
            groupSpecificPermissions.put(new EntityGroupId(new UUID(groupPermissionsProto.getEntityGroupIdMSB(), groupPermissionsProto.getEntityGroupIdLSB())),
                    new MergedGroupPermissionInfo(EntityType.valueOf(groupPermissionsProto.getEntityType()), operations));
        }
        return new MergedUserPermissions(genericPermissions, groupSpecificPermissions);
    }

}
