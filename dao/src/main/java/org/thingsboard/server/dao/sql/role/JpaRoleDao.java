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
package org.thingsboard.server.dao.sql.role;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Component
@SqlDao
public class JpaRoleDao extends JpaAbstractDao<RoleEntity, Role> implements RoleDao {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    protected Class<RoleEntity> getEntityClass() {
        return RoleEntity.class;
    }

    @Override
    protected JpaRepository<RoleEntity, UUID> getRepository() {
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

    @Override
    public Role findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(roleRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<Role> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(roleRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public RoleId getExternalIdByInternal(RoleId internalId) {
        return Optional.ofNullable(roleRepository.getExternalIdById(internalId.getId()))
                .map(RoleId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }

}
