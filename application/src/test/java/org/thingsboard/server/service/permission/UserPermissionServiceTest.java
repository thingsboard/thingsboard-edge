/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.permission;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.SimpleTbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.user.UserPermissionCacheKey;
import org.thingsboard.server.service.security.permission.DefaultUserPermissionsService;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class UserPermissionServiceTest {

    @Mock
    private TbTransactionalCache<UserPermissionCacheKey, MergedUserPermissions> cache;
    @Mock
    private EntityGroupService entityGroupService;
    @Mock
    private GroupPermissionService groupPermissionService;

    @InjectMocks
    private DefaultUserPermissionsService userPermissionsService;

    private User testUser;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        tenantId = new TenantId(UUID.randomUUID());
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        testUser = new User();
        testUser.setId(userId);
        testUser.setTenantId(tenantId);
        testUser.setCustomerId(customerId);
        testUser.setAuthority(Authority.TENANT_ADMIN);
    }

    @Test
    void testGetMergedPermissions_sysAdmin() throws Exception {
        testUser.setAuthority(Authority.SYS_ADMIN);
        MergedUserPermissions permissions = userPermissionsService.getMergedPermissions(testUser, false);
        assertNotNull(permissions);
        assertEquals(getSysAdminPermissions(), permissions);
    }

    @Test
    void testGetMergedPermissions_tenantAdmin() throws ThingsboardException {
        MergedUserPermissions cachedPermissions = mock(MergedUserPermissions.class);
        SimpleTbCacheValueWrapper<MergedUserPermissions> cacheValueWrapper = SimpleTbCacheValueWrapper.wrap(cachedPermissions);
        when(cache.get(any())).thenReturn(cacheValueWrapper);

        MergedUserPermissions permissions = userPermissionsService.getMergedPermissions(testUser, false);
        assertEquals(cachedPermissions, permissions);
        verify(cache).get(any());
    }

    @ParameterizedTest
    @CsvSource({"DEVICE", "ASSET"})
    void testOnRoleUpdated(EntityType entityType) throws ThingsboardException {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setId(new RoleId(UUID.randomUUID()));
        role.setPermissions(JacksonUtil.newObjectNode());

        List<GroupPermission> groupPermissions = List.of(createGroupPermission(tenantId, new EntityGroupId(UUID.randomUUID()), role.getId(), new EntityGroupId(UUID.randomUUID()), entityType));

        PageData<GroupPermission> pageData = new PageData<>(groupPermissions, 0, groupPermissions.size(), false);
        when(groupPermissionService.findGroupPermissionByTenantIdAndRoleId(any(), any(), any())).thenReturn(pageData);

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setOwnerId(new CustomerId(UUID.randomUUID()));
        when(entityGroupService.findEntityGroupById(any(), any())).thenReturn(entityGroup);

        List<EntityId> entityIds = List.of(new UserId(UUID.randomUUID()));
        when(entityGroupService.findAllEntityIdsAsync(any(), any(), any())).thenReturn(Futures.immediateFuture(entityIds));

        userPermissionsService.onRoleUpdated(role);

        verify(groupPermissionService).findGroupPermissionByTenantIdAndRoleId(any(), any(), any());
        verify(entityGroupService, times(1)).findEntityGroupById(any(), any());
        verify(entityGroupService, times(1)).findAllEntityIdsAsync(any(), any(), any());
        verify(cache, times(1)).evict(any(UserPermissionCacheKey.class));
    }

    @Test
    void testOnGroupPermissionUpdated() throws ThingsboardException {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setTenantId(tenantId);
        groupPermission.setUserGroupId(new EntityGroupId(UUID.randomUUID()));
        groupPermission.setPublic(false);

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setOwnerId(new CustomerId(UUID.randomUUID()));
        when(entityGroupService.findEntityGroupById(any(), any())).thenReturn(entityGroup);

        List<EntityId> entityIds = List.of(new UserId(UUID.randomUUID()));
        when(entityGroupService.findAllEntityIdsAsync(any(), any(), any())).thenReturn(Futures.immediateFuture(entityIds));

        userPermissionsService.onGroupPermissionUpdated(groupPermission);

        verify(entityGroupService).findEntityGroupById(any(), any());
        verify(entityGroupService).findAllEntityIdsAsync(any(), any(), any());
        verify(cache).evict(any(UserPermissionCacheKey.class));
    }

    @Test
    void testOnGroupPermissionDeleted() throws ThingsboardException {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setTenantId(tenantId);
        groupPermission.setUserGroupId(new EntityGroupId(UUID.randomUUID()));
        groupPermission.setPublic(false);

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setOwnerId(new CustomerId(UUID.randomUUID()));
        when(entityGroupService.findEntityGroupById(any(), any())).thenReturn(entityGroup);

        List<EntityId> entityIds = List.of(new UserId(UUID.randomUUID()));
        when(entityGroupService.findAllEntityIdsAsync(any(), any(), any())).thenReturn(Futures.immediateFuture(entityIds));

        userPermissionsService.onGroupPermissionDeleted(groupPermission);

        verify(entityGroupService).findEntityGroupById(any(), any());
        verify(entityGroupService).findAllEntityIdsAsync(any(), any(), any());
        verify(cache).evict(any(UserPermissionCacheKey.class));
    }

    @Test
    void testOnUserUpdatedOrRemoved() {
        userPermissionsService.onUserUpdatedOrRemoved(testUser);
        verify(cache).evict((UserPermissionCacheKey) any());
    }

    private MergedUserPermissions getSysAdminPermissions() throws Exception {
        Field field = DefaultUserPermissionsService.class.getDeclaredField("sysAdminPermissions");
        field.setAccessible(true);
        return (MergedUserPermissions) field.get(userPermissionsService);
    }

    private GroupPermission createGroupPermission(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId, EntityGroupId entityGroupId, EntityType entityGroupType) {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setTenantId(tenantId);
        groupPermission.setUserGroupId(userGroupId);
        groupPermission.setRoleId(roleId);
        groupPermission.setEntityGroupId(entityGroupId);
        groupPermission.setEntityGroupType(entityGroupType);
        return groupPermission;
    }

}
