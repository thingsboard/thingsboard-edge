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
package org.thingsboard.server.dao.sql.grouppermission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.grouppermission.GroupPermissionDao;
import org.thingsboard.server.dao.model.sql.GroupPermissionEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.UUID;


@Component
public class JpaGroupPermissionDao extends JpaAbstractDao<GroupPermissionEntity, GroupPermission> implements GroupPermissionDao {

    @Autowired
    private GroupPermissionRepository groupPermissionRepository;

    @Override
    protected Class<GroupPermissionEntity> getEntityClass() {
        return GroupPermissionEntity.class;
    }

    @Override
    protected CrudRepository<GroupPermissionEntity, UUID> getCrudRepository() {
        return groupPermissionRepository;
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantId(
                        tenantId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupId(UUID tenantId, UUID userGroupId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantIdAndUserGroupId(
                        tenantId,
                        userGroupId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupIdAndRoleId(UUID tenantId, UUID userGroupId, UUID roleId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantIdAndUserGroupIdAndRoleId(
                        tenantId,
                        userGroupId,
                        roleId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(UUID tenantId, UUID entityGroupId, UUID userGroupId, UUID roleId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(
                        tenantId,
                        entityGroupId,
                        userGroupId,
                        roleId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupId(UUID tenantId, UUID entityGroupId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantIdAndEntityGroupId(
                        tenantId,
                        entityGroupId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndRoleId(UUID tenantId, UUID roleId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantIdAndRoleId(
                        tenantId,
                        roleId,
                        DaoUtil.toPageable(pageLink)));
    }
}
