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
package org.thingsboard.server.dao.sql.owner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.OwnerInfoEntity;
import org.thingsboard.server.dao.owner.OwnerInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@SqlDao
public class JpaOwnerInfoDao extends JpaAbstractDao<OwnerInfoEntity, EntityInfo> implements OwnerInfoDao {

    @Autowired
    private OwnerInfoRepository ownerInfoRepository;

    @Override
    protected Class<OwnerInfoEntity> getEntityClass() {
        return OwnerInfoEntity.class;
    }

    @Override
    protected JpaRepository<OwnerInfoEntity, UUID> getRepository() {
        return ownerInfoRepository;
    }

    @Override
    public PageData<EntityInfo> findTenantOwnerByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(ownerInfoRepository
                .findTenantOwnerByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findCustomerOwnersByTenantIdIncludingTenant(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(ownerInfoRepository
                .findCustomerOwnersByTenantIdIncludingTenant(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findCustomerOwnersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(ownerInfoRepository
                .findCustomerOwnersByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findCustomerOwnersByIdsAndTenantId(UUID tenantId, List<UUID> ownerIds, PageLink pageLink) {
        return DaoUtil.toPageData(ownerInfoRepository
                .findCustomerOwnersByIdsAndTenantId(
                        tenantId,
                        ownerIds,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }
}
