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
package org.thingsboard.server.dao.sql.customer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerInfoDao;
import org.thingsboard.server.dao.model.sql.CustomerEntity;
import org.thingsboard.server.dao.model.sql.CustomerInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaCustomerInfoDao extends JpaAbstractSearchTextDao<CustomerInfoEntity, CustomerInfo> implements CustomerInfoDao {

    @Autowired
    private CustomerInfoRepository customerInfoRepository;

    @Override
    protected Class<CustomerInfoEntity> getEntityClass() {
        return CustomerInfoEntity.class;
    }

    @Override
    protected JpaRepository<CustomerInfoEntity, UUID> getRepository() {
        return customerInfoRepository;
    }

    @Override
    public PageData<CustomerInfo> findCustomersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(customerInfoRepository
                .findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public PageData<CustomerInfo> findTenantCustomersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(customerInfoRepository
                .findTenantCustomersByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public PageData<CustomerInfo> findCustomersByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(customerInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public PageData<CustomerInfo> findCustomersByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(customerInfoRepository
                .findByTenantIdAndCustomerIdIncludingSubCustomers(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

}
