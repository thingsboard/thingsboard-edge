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
package org.thingsboard.server.dao.sql.role;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Component
public class JpaRoleDao extends JpaAbstractSearchTextDao<RoleEntity, Role> implements RoleDao {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    protected Class<RoleEntity> getEntityClass() {
        return RoleEntity.class;
    }

    @Override
    protected CrudRepository<RoleEntity, UUID> getCrudRepository() {
        return roleRepository;
    }

    @Override
    public PageData<Role> findRolesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                roleRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        EntityId.NULL_UUID,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Role> findRolesByTenantIdAndType(UUID tenantId, RoleType type, PageLink pageLink) {
        return DaoUtil.toPageData(
                roleRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        EntityId.NULL_UUID,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<Role> findRoleByTenantIdAndName(UUID tenantId, String name) {
        return Optional.ofNullable(
                DaoUtil.getData(roleRepository.findByTenantIdAndCustomerIdAndName(tenantId, EntityId.NULL_UUID, name)));
    }

    @Override
    public Optional<Role> findRoleByByTenantIdAndCustomerIdAndName(UUID tenantId, UUID customerId, String name) {
        return Optional.ofNullable(
                DaoUtil.getData(roleRepository.findByTenantIdAndCustomerIdAndName(tenantId, customerId, name)));
    }

    @Override
    public PageData<Role> findRolesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                roleRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Role> findRolesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, RoleType type, PageLink pageLink) {
        return DaoUtil.toPageData(
                roleRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Role>> findRolesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> roleIds) {
        return service.submit(() -> DaoUtil.convertDataList(roleRepository.findRolesByTenantIdAndIdIn(tenantId, roleIds)));
    }

}
