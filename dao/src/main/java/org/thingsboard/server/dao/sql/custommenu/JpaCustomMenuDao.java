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
package org.thingsboard.server.dao.sql.custommenu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuFilter;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.menu.CustomMenuDao;
import org.thingsboard.server.dao.model.sql.CustomMenuEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;


@Component
@Slf4j
@SqlDao
public class JpaCustomMenuDao extends JpaAbstractDao<CustomMenuEntity, CustomMenu> implements CustomMenuDao {

    @Autowired
    private CustomMenuRepository customMenuRepository;

    @Autowired
    private CustomMenuInfoRepository customMenuInfoRepository;

    @Override
    public CustomMenuInfo findInfoById(CustomMenuId customMenuId) {
        return DaoUtil.getData(customMenuInfoRepository.findById(customMenuId.getId()));
    }

    @Override
    public PageData<CustomMenuInfo> findInfosByFilter(TenantId tenantId, CustomMenuFilter customMenuFilter, PageLink pageLink) {
        return DaoUtil.toPageData(customMenuInfoRepository.findByTenantIdAndCustomerIdAndScopeAndAssigneeType(customMenuFilter.getTenantId().getId(),
                customMenuFilter.getCustomerId() == null ? EntityId.NULL_UUID : customMenuFilter.getCustomerId().getId(),
                customMenuFilter.getScope(), customMenuFilter.getAssigneeType(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public CustomMenu findDefaultMenuByScope(TenantId tenantId, CustomerId customerId, CMScope scope) {
        return DaoUtil.getData(customMenuRepository.findDefaultByTenantIdAndCustomerIdAndScope(tenantId.getId(),
                customerId == null ? EntityId.NULL_UUID : customerId.getId(), scope));
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        customMenuRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public PageData<CustomMenu> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(customMenuRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    protected Class<CustomMenuEntity> getEntityClass() {
        return CustomMenuEntity.class;
    }

    @Override
    protected JpaRepository<CustomMenuEntity, UUID> getRepository() {
        return customMenuRepository;
    }

}
