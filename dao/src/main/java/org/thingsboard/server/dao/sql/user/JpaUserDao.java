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
package org.thingsboard.server.dao.sql.user;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * @author Valerii Sosliuk
 */
@Component
@Slf4j
@SqlDao
public class JpaUserDao extends JpaAbstractSearchTextDao<UserEntity, User> implements UserDao {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected Class<UserEntity> getEntityClass() {
        return UserEntity.class;
    }

    @Override
    protected JpaRepository<UserEntity, UUID> getRepository() {
        return userRepository;
    }

    @Override
    public User findByEmail(TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByEmail(email));
    }

    @Override
    public User findByTenantIdAndEmail(TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByTenantIdAndEmail(tenantId.getId(), email));
    }

    @Override
    public PageData<User> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findByTenantId(
                                tenantId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<User> findTenantAdmins(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findUsersByAuthority(
                                tenantId,
                                NULL_UUID,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                Authority.TENANT_ADMIN,
                                DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }


    @Override
    public PageData<User> findCustomerUsers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
            userRepository
                    .findUsersByAuthority(
                            tenantId,
                            customerId,
                            Objects.toString(pageLink.getTextSearch(), ""),
                            Authority.CUSTOMER_USER,
                            DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));

    }

    @Override
    public PageData<User> findAllCustomerUsers(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findAllTenantUsersByAuthority(
                                tenantId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                Authority.CUSTOMER_USER,
                                DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public ListenableFuture<List<User>> findUsersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> userIds) {
        return service.submit(() -> DaoUtil.convertDataList(userRepository.findUsersByTenantIdAndIdIn(tenantId, userIds)));
    }

    @Override
    public PageData<User> findUsersByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository
                .findByEntityGroupId(
                        groupId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<User> findUsersByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository
                .findByEntityGroupIds(
                        groupIds,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<User> findUsersByTenantIdAndRolesIds(TenantId tenantId, List<RoleId> rolesIds, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findByTenantIdAndRolesIds(tenantId.getId(), DaoUtil.toUUIDs(rolesIds),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<User> findAll(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findAll(DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return userRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}
