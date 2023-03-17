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
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

public interface GroupPermissionService extends EntityDaoService {

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
