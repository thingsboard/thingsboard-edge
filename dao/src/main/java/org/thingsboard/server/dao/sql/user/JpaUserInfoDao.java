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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UserInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.model.sql.UserInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.user.UserInfoDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaUserInfoDao extends JpaAbstractSearchTextDao<UserInfoEntity, UserInfo> implements UserInfoDao {

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Override
    protected Class<UserInfoEntity> getEntityClass() {
        return UserInfoEntity.class;
    }

    @Override
    protected JpaRepository<UserInfoEntity, UUID> getRepository() {
        return userInfoRepository;
    }

    @Override
    public PageData<UserInfo> findUsersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(userInfoRepository
                .findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<UserInfo> findTenantUsersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(userInfoRepository
                .findTenantUsersByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<UserInfo> findUsersByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(userInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<UserInfo> findUsersByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(userInfoRepository
                .findByTenantIdAndCustomerIdIncludingSubCustomers(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }
}
