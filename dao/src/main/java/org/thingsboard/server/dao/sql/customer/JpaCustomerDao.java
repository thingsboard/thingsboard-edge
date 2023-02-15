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

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.model.sql.CustomerEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaCustomerDao extends JpaAbstractSearchTextDao<CustomerEntity, Customer> implements CustomerDao {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    protected Class<CustomerEntity> getEntityClass() {
        return CustomerEntity.class;
    }

    @Override
    protected JpaRepository<CustomerEntity, UUID> getRepository() {
        return customerRepository;
    }

    @Override
    public PageData<Customer> findCustomersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(customerRepository.findByTenantId(
                tenantId,
                Objects.toString(pageLink.getTextSearch(), ""),
                DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public Optional<Customer> findCustomersByTenantIdAndTitle(UUID tenantId, String title) {
        Customer customer = DaoUtil.getData(customerRepository.findByTenantIdAndTitle(tenantId, title));
        return Optional.ofNullable(customer);
    }

    @Override
    public ListenableFuture<List<Customer>> findCustomersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> customerIds) {
        return DaoUtil.getEntitiesByTenantIdAndIdIn(customerIds, ids ->
                customerRepository.findCustomersByTenantIdAndIdIn(tenantId, ids), service);
    }

    @Override
    public PageData<Customer> findCustomersByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(customerRepository
                .findByEntityGroupId(
                        groupId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    @Override
    public PageData<Customer> findCustomersByEntityGroupIds(List<UUID> groupIds, List<UUID> additionalCustomerIds, PageLink pageLink) {
        return DaoUtil.toPageData(customerRepository
                .findByEntityGroupIds(
                        groupIds,
                        additionalCustomerIds != null && !additionalCustomerIds.isEmpty() ? additionalCustomerIds : null,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, CustomerEntity.customerColumnMap)));
    }

    public Long countByTenantId(TenantId tenantId) {
        return customerRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public Customer findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(customerRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public Customer findByTenantIdAndName(UUID tenantId, String name) {
        return findCustomersByTenantIdAndTitle(tenantId, name).orElse(null);
    }

    @Override
    public PageData<Customer> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findCustomersByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<CustomerId> findIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        Page<UUID> page;
        if(customerId == null){
            page = customerRepository.findIdsByTenantIdAndNullCustomerId(tenantId, DaoUtil.toPageable(pageLink));
        } else {
            page = customerRepository.findIdsByTenantIdAndCustomerId(tenantId, customerId, DaoUtil.toPageable(pageLink));
        }
        return DaoUtil.pageToPageData(page, CustomerId::new);
    }

    @Override
    public CustomerId getExternalIdByInternal(CustomerId internalId) {
        return Optional.ofNullable(customerRepository.getExternalIdById(internalId.getId()))
                .map(CustomerId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

}
