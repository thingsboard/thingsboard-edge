/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.customer;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;

import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("CustomerDaoService")
@Slf4j
public class CustomerServiceImpl extends AbstractEntityService implements CustomerService {

    public static final String PUBLIC_CUSTOMER_TITLE = "Public";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private UserService userService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DashboardService dashboardService;

    @Lazy
    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Autowired
    private DataValidator<Customer> customerValidator;

    @Autowired
    private EntityCountService countService;

    @Override
    public Customer findCustomerById(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findCustomerById [{}]", customerId);
        Validator.validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        return customerDao.findById(tenantId, customerId.getId());
    }

    @Override
    public Optional<Customer> findCustomerByTenantIdAndTitle(TenantId tenantId, String title) {
        log.trace("Executing findCustomerByTenantIdAndTitle [{}] [{}]", tenantId, title);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return customerDao.findCustomersByTenantIdAndTitle(tenantId.getId(), title);
    }

    @Override
    public ListenableFuture<Customer> findCustomerByIdAsync(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findCustomerByIdAsync [{}]", customerId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        return customerDao.findByIdAsync(tenantId, customerId.getId());
    }

    @Override
    public Customer saveCustomer(Customer customer, boolean doValidate) {
        return doSaveCustomer(customer, doValidate);
    }

    @Override
    public Customer saveCustomer(Customer customer) {
        return doSaveCustomer(customer, true);
    }

    private Customer doSaveCustomer(Customer customer, boolean doValidate) {
        log.trace("Executing saveCustomer [{}]", customer);
        if (doValidate) {
            customerValidator.validate(customer, Customer::getTenantId);
        }
        try {
            Customer savedCustomer = customerDao.save(customer.getTenantId(), customer);
            dashboardService.updateCustomerDashboards(savedCustomer.getTenantId(), savedCustomer.getId());
            if (customer.getId() == null) {
                countService.publishCountEntityEvictEvent(savedCustomer.getTenantId(), EntityType.CUSTOMER);
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedCustomer.getTenantId())
                    .entityId(savedCustomer.getId()).created(customer.getId() == null).build());
            return savedCustomer;
        } catch (Exception e) {
            checkConstraintViolation(e, "customer_external_id_unq_key", "Customer with such external id already exists!");
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteCustomer(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteCustomer [{}]", customerId);
        Validator.validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Customer customer = findCustomerById(tenantId, customerId);
        if (customer == null) {
            throw new IncorrectParameterException("Unable to delete non-existent customer.");
        }
        dashboardService.unassignCustomerDashboards(tenantId, customerId);
        entityViewService.unassignCustomerEntityViews(customer.getTenantId(), customerId);
        assetService.unassignCustomerAssets(customer.getTenantId(), customerId);
        deviceService.unassignCustomerDevices(customer.getTenantId(), customerId);
        edgeService.unassignCustomerEdges(customer.getTenantId(), customerId);
        userService.deleteCustomerUsers(customer.getTenantId(), customerId);
        deleteEntityRelations(tenantId, customerId);
        apiUsageStateService.deleteApiUsageStateByEntityId(customerId);
        customerDao.removeById(tenantId, customerId.getId());
        countService.publishCountEntityEvictEvent(tenantId, EntityType.CUSTOMER);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(customerId).build());
    }

    @Override
    public Customer findOrCreatePublicCustomer(TenantId tenantId) {
        log.trace("Executing findOrCreatePublicCustomer, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_CUSTOMER_ID + tenantId);
        Optional<Customer> publicCustomerOpt = customerDao.findCustomersByTenantIdAndTitle(tenantId.getId(), PUBLIC_CUSTOMER_TITLE);
        if (publicCustomerOpt.isPresent()) {
            return publicCustomerOpt.get();
        } else {
            throw new RuntimeException("Unable to create public customer on edge - please create it on cloud and click 'Sync Edge' button.");
            // TODO: @voba - public customer should be created on the cloud
            // Customer publicCustomer = new Customer();
            // publicCustomer.setTenantId(tenantId);
            // publicCustomer.setTitle(PUBLIC_CUSTOMER_TITLE);
            // try {
            //     publicCustomer.setAdditionalInfo(JacksonUtil.toJsonNode("{ \"isPublic\": true }"));
            // } catch (IllegalArgumentException e) {
            //     throw new IncorrectParameterException("Unable to create public customer.", e);
            // }
            // Customer savedCustomer = customerDao.save(tenantId, publicCustomer);
            // countService.publishCountEntityEvictEvent(tenantId, EntityType.CUSTOMER);
            // return savedCustomer;
        }
    }

    @Override
    public PageData<Customer> findCustomersByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findCustomersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink);
        return customerDao.findCustomersByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteCustomersByTenantId(TenantId tenantId) {
        log.trace("Executing deleteCustomersByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        customersByTenantRemover.removeEntities(tenantId, tenantId);
    }

    private PaginatedRemover<TenantId, Customer> customersByTenantRemover =
            new PaginatedRemover<TenantId, Customer>() {

                @Override
                protected PageData<Customer> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return customerDao.findCustomersByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Customer entity) {
                    deleteCustomer(tenantId, new CustomerId(entity.getUuidId()));
                }
            };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findCustomerById(tenantId, new CustomerId(entityId.getId())));
    }

    @Transactional
    @Override
    public void deleteEntity(TenantId tenantId, EntityId id) {
        deleteCustomer(tenantId, (CustomerId) id);
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return customerDao.countByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

}
