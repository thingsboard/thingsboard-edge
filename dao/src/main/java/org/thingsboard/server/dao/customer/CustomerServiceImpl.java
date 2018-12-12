/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.customer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.thingsboard.server.dao.service.Validator.validateEntityId;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class CustomerServiceImpl extends AbstractEntityService implements CustomerService {

    private static final String PUBLIC_CUSTOMER_TITLE = "Public";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private GroupPermissionService groupPermissionService;

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
    public Customer saveCustomer(Customer customer) {
        log.trace("Executing saveCustomer [{}]", customer);
        customerValidator.validate(customer, Customer::getTenantId);
        Customer savedCustomer = customerDao.save(customer.getTenantId(), customer);
        if (customer.getId() == null) {
            entityGroupService.addEntityToEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getOwnerId(), savedCustomer.getId());
            entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.CUSTOMER);
            entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.ASSET);
            entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.DEVICE);
            entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.ENTITY_VIEW);
            entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.DASHBOARD);

            // User Group 'All' -> 'User' role -> Read only permissions
            EntityGroup users = entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.USER);
            Role userRole = getOrCreateGenericRole(savedCustomer, Role.ROLE_USER_NAME, GroupPermission.READ_ONLY_PERMISSIONS);
            GroupPermission usersGroupPermission = new GroupPermission();
            usersGroupPermission.setTenantId(savedCustomer.getTenantId());
            usersGroupPermission.setUserGroupId(users.getId());
            usersGroupPermission.setRoleId(userRole.getId());
            groupPermissionService.saveGroupPermission(savedCustomer.getTenantId(), usersGroupPermission);

            // User Group 'Admins' -> 'Admin' role -> All permissions
            EntityGroup admins = entityGroupService.getOrCreateAdminsUserGroup(savedCustomer.getTenantId(), savedCustomer.getId());
            Role adminRole = getOrCreateGenericRole(savedCustomer, Role.ROLE_ADMIN_NAME, GroupPermission.ALL_PERMISSIONS);
            GroupPermission adminsGroupPermission = new GroupPermission();
            adminsGroupPermission.setTenantId(savedCustomer.getTenantId());
            adminsGroupPermission.setUserGroupId(admins.getId());
            adminsGroupPermission.setRoleId(adminRole.getId());
            groupPermissionService.saveGroupPermission(savedCustomer.getTenantId(), adminsGroupPermission);

        }
        dashboardService.updateCustomerDashboards(savedCustomer.getTenantId(), savedCustomer.getId());
        return savedCustomer;
    }

    private Role getOrCreateGenericRole(Customer customer, String name, Map<Resource, List<Operation>> permissions) {
        Optional<Role> roleOptional;
        if (customer.isSubCustomer()) {
            roleOptional = roleService.findRoleByByTenantIdAndCustomerIdAndName(customer.getTenantId(), customer.getParentCustomerId(), name);
        } else {
            roleOptional = roleService.findRoleByTenantIdAndName(customer.getTenantId(), name);
        }
        if (!roleOptional.isPresent()) {
            Role role = new Role();
            role.setTenantId(customer.getTenantId());
            if (customer.isSubCustomer()) {
                role.setCustomerId(customer.getParentCustomerId());
            } else {
                role.setCustomerId(new CustomerId(EntityId.NULL_UUID));
            }
            role.setName(name);
            role.setType(RoleType.GENERIC);
            role.setPermissions(mapper.valueToTree(permissions));
            return roleService.saveRole(customer.getTenantId(), role);
        } else {
            return roleOptional.get();
        }
    }

    @Override
    public void deleteCustomer(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteCustomer [{}]", customerId);
        Validator.validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Customer customer = findCustomerById(tenantId, customerId);
        if (customer == null) {
            throw new IncorrectParameterException("Unable to delete non-existent customer.");
        }
        //TODO (Security): // recursively delete subcustomers
        whiteLabelingService.deleteDomainWhiteLabelingByEntityId(tenantId, customerId);
        dashboardService.unassignCustomerDashboards(tenantId, customerId);
        entityViewService.unassignCustomerEntityViews(customer.getTenantId(), customerId);
        assetService.unassignCustomerAssets(customer.getTenantId(), customerId);
        deviceService.unassignCustomerDevices(customer.getTenantId(), customerId);
        userService.deleteCustomerUsers(customer.getTenantId(), customerId);
        deleteEntityGroups(tenantId, customerId);
        deleteEntityRelations(tenantId, customerId);
        roleService.deleteRolesByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        customerDao.removeById(tenantId, customerId.getId());
    }

    @Override
    public Customer findOrCreatePublicCustomer(TenantId tenantId) {
        log.trace("Executing findOrCreatePublicCustomer, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_CUSTOMER_ID + tenantId);
        Optional<Customer> publicCustomerOpt = customerDao.findCustomersByTenantIdAndTitle(tenantId.getId(), PUBLIC_CUSTOMER_TITLE);
        if (publicCustomerOpt.isPresent()) {
            return publicCustomerOpt.get();
        } else {
            Customer publicCustomer = new Customer();
            publicCustomer.setTenantId(tenantId);
            publicCustomer.setTitle(PUBLIC_CUSTOMER_TITLE);
            try {
                publicCustomer.setAdditionalInfo(new ObjectMapper().readValue("{ \"isPublic\": true }", JsonNode.class));
            } catch (IOException e) {
                throw new IncorrectParameterException("Unable to create public customer.", e);
            }
            return customerDao.save(tenantId, publicCustomer);
        }
    }

    @Override
    public TextPageData<Customer> findCustomersByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findCustomersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<Customer> customers = customerDao.findCustomersByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(customers, pageLink);
    }

    @Override
    public void deleteCustomersByTenantId(TenantId tenantId) {
        log.trace("Executing deleteCustomersByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        customersByTenantRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public ShortEntityView findGroupCustomer(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing findGroupCustomer, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        validateEntityId(entityId, "Incorrect entityId " + entityId);
        return entityGroupService.findGroupEntity(tenantId, entityGroupId, entityId, new CustomerViewFunction(tenantId));
    }

    @Override
    public ListenableFuture<TimePageData<ShortEntityView>> findCustomersByEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId, TimePageLink pageLink) {
        log.trace("Executing findCustomersByEntityGroupId, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        return entityGroupService.findEntities(tenantId, entityGroupId, pageLink, new CustomerViewFunction(tenantId));
    }

    class CustomerViewFunction implements BiFunction<ShortEntityView, List<EntityField>, ShortEntityView> {

        private final TenantId tenantId;

        CustomerViewFunction(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public ShortEntityView apply(ShortEntityView entityView, List<EntityField> entityFields) {
            Customer customer = findCustomerById(tenantId, new CustomerId(entityView.getId().getId()));
            entityView.put(EntityField.NAME.name().toLowerCase(), customer.getName());
            for (EntityField field : entityFields) {
                String key = field.name().toLowerCase();
                switch (field) {
                    case TITLE:
                        entityView.put(key, customer.getTitle());
                        break;
                    case EMAIL:
                        entityView.put(key, customer.getEmail());
                        break;
                    case COUNTRY:
                        entityView.put(key, customer.getCountry());
                        break;
                    case STATE:
                        entityView.put(key, customer.getState());
                        break;
                    case CITY:
                        entityView.put(key, customer.getCity());
                        break;
                    case ADDRESS:
                        entityView.put(key, customer.getAddress());
                        break;
                    case ADDRESS2:
                        entityView.put(key, customer.getAddress2());
                        break;
                    case ZIP:
                        entityView.put(key, customer.getZip());
                        break;
                    case PHONE:
                        entityView.put(key, customer.getPhone());
                        break;
                }
            }
            return entityView;
        }
    }

    private DataValidator<Customer> customerValidator =
            new DataValidator<Customer>() {

                @Override
                protected void validateCreate(TenantId tenantId, Customer customer) {
                    customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle()).ifPresent(
                            c -> {
                                throw new DataValidationException("Customer with such title already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Customer customer) {
                    customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle()).ifPresent(
                            c -> {
                                if (!c.getId().equals(customer.getId())) {
                                    throw new DataValidationException("Customer with such title already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Customer customer) {
                    if (StringUtils.isEmpty(customer.getTitle())) {
                        throw new DataValidationException("Customer title should be specified!");
                    }
                    if (customer.getTitle().equals(PUBLIC_CUSTOMER_TITLE)) {
                        throw new DataValidationException("'Public' title for customer is system reserved!");
                    }
                    if (!StringUtils.isEmpty(customer.getEmail())) {
                        validateEmail(customer.getEmail());
                    }
                    if (customer.getTenantId() == null) {
                        throw new DataValidationException("Customer should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, customer.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Customer is referencing to non-existent tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Customer> customersByTenantRemover =
            new PaginatedRemover<TenantId, Customer>() {

                @Override
                protected List<Customer> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
                    return customerDao.findCustomersByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Customer entity) {
                    deleteCustomer(tenantId, new CustomerId(entity.getUuidId()));
                }
            };
}
