/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.dao.grouppermission;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;

import java.util.List;
import java.util.Optional;

public interface GroupPermissionService {

    GroupPermission saveGroupPermission(TenantId tenantId, GroupPermission groupPermission);

    GroupPermission findGroupPermissionById(TenantId tenantId, GroupPermissionId groupPermissionId);

    ListenableFuture<GroupPermissionInfo> findGroupPermissionInfoByIdAsync(TenantId tenantId, GroupPermissionId groupPermissionId, boolean isUserGroup);

    PageData<GroupPermission> findGroupPermissionByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionByTenantIdAndUserGroupIdAndRoleId(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(TenantId tenantId, EntityGroupId entityGroupId, EntityGroupId userGroupId, RoleId roleId, PageLink pageLink);

    ListenableFuture<List<GroupPermissionInfo>> findGroupPermissionInfoListByTenantIdAndUserGroupIdAsync(TenantId tenantId, EntityGroupId userGroupId);

    ListenableFuture<List<GroupPermissionInfo>> loadUserGroupPermissionInfoListAsync(TenantId tenantId, List<GroupPermission> permissions);

    List<GroupPermission> findGroupPermissionListByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId entityGroupId);

    PageData<GroupPermission> findGroupPermissionByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink);

    Optional<GroupPermission> findPublicGroupPermissionByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId);

    ListenableFuture<List<GroupPermissionInfo>> findGroupPermissionInfoListByTenantIdAndEntityGroupIdAsync(TenantId tenantId, EntityGroupId entityGroupId);

    PageData<GroupPermission> findGroupPermissionByTenantIdAndRoleId(TenantId tenantId, RoleId roleId, PageLink pageLink);

    ListenableFuture<GroupPermission> findGroupPermissionByIdAsync(TenantId tenantId, GroupPermissionId groupPermissionId);

    void deleteGroupPermission(TenantId tenantId, GroupPermissionId groupPermissionId);

    void deleteGroupPermissionsByTenantId(TenantId tenantId);

    void deleteGroupPermissionsByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId);

    void deleteGroupPermissionsByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId);

    void deleteGroupPermissionsByTenantIdAndRoleId(TenantId tenantId, RoleId roleId);

}
