/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.customer.CustomerCacheEvictEvent;
import org.thingsboard.server.cache.customer.CustomerCacheKey;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("CustomerDaoService")
@Slf4j
public class CustomerServiceImpl extends AbstractCachedEntityService<CustomerCacheKey, Customer, CustomerCacheEvictEvent> implements CustomerService {

    public static final String PUBLIC_CUSTOMER_SUFFIX = "Public";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_OWNER_ID = "Incorrect ownerId ";
    public static final String PUBLIC_CUSTOMER_ADDITIONAL_INFO_STR = "{ \"isPublic\": true }";
    public static final JsonNode PUBLIC_CUSTOMER_ADDITIONAL_INFO_JSON = JacksonUtil.toJsonNode(PUBLIC_CUSTOMER_ADDITIONAL_INFO_STR);

    private final ConcurrentMap<TbPair<TenantId, EntityId>, Object> publicCustomerCreationLocks = new ConcurrentReferenceHashMap<>();

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private CustomerInfoDao customerInfoDao;

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
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private BlobEntityService blobEntityService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Autowired
    private DataValidator<Customer> customerValidator;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private EntityCountService countService;

    @Autowired
    private EntityService entityService;

    @TransactionalEventListener(classes = CustomerCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(CustomerCacheEvictEvent event) {
        List<CustomerCacheKey> keys = new ArrayList<>(2);
        keys.add(new CustomerCacheKey(event.getTenantId(), event.getNewTitle()));
        if (StringUtils.isNotEmpty(event.getOldTitle()) && !event.getOldTitle().equals(event.getNewTitle())) {
            keys.add(new CustomerCacheKey(event.getTenantId(), event.getOldTitle()));
        }
        cache.evict(keys);
    }

    @Override
    public Customer findCustomerById(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findCustomerById [{}]", customerId);
        Validator.validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        return customerDao.findById(tenantId, customerId.getId());
    }

    @Override
    public CustomerInfo findCustomerInfoById(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findCustomerInfoById [{}]", customerId);
        Validator.validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        return customerInfoDao.findById(tenantId, customerId.getId());
    }

    @Override
    public Optional<Customer> findCustomerByTenantIdAndTitle(TenantId tenantId, String title) {
        log.trace("Executing findCustomerByTenantIdAndTitle [{}] [{}]", tenantId, title);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return Optional.ofNullable(cache.getAndPutInTransaction(new CustomerCacheKey(tenantId, title),
                () -> customerDao.findCustomerByTenantIdAndTitle(tenantId.getId(), title)
                        .orElse(null), true));
    }

    @Override
    public ListenableFuture<Customer> findCustomerByIdAsync(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findCustomerByIdAsync [{}]", customerId);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        return customerDao.findByIdAsync(tenantId, customerId.getId());
    }

    @Override
    public ListenableFuture<List<Customer>> findCustomersByTenantIdAndIdsAsync(TenantId tenantId, List<CustomerId> customerIds) {
        log.trace("Executing findCustomersByTenantIdAndIdsAsync, tenantId [{}], customerIds [{}]", tenantId, customerIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(customerIds, ids -> "Incorrect customerIds " + ids);
        return customerDao.findCustomersByTenantIdAndIdsAsync(tenantId.getId(), customerIds.stream().map(CustomerId::getId).collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Customer saveCustomer(Customer customer) {
        return saveCustomer(customer, true);
    }

    private Customer saveCustomer(Customer customer, boolean doValidate) {
        log.trace("Executing saveCustomer [{}]", customer);
        Customer oldCustomer = null;
        String oldCustomerTitle = null;
        if (doValidate) {
            oldCustomer = customerValidator.validate(customer, Customer::getTenantId);
            if (oldCustomer != null) {
                oldCustomerTitle = oldCustomer.getTitle();
            }
        }
        var evictEvent = new CustomerCacheEvictEvent(customer.getTenantId(), customer.getTitle(), oldCustomerTitle);
        try {
            Customer savedCustomer = customerDao.saveAndFlush(customer.getTenantId(), customer);
            if (customer.getId() == null) {
                entityGroupService.addEntityToEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getOwnerId(), savedCustomer.getId());
                entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.CUSTOMER);
                entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.ASSET);
                entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.DEVICE);
                entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.ENTITY_VIEW);
                entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.EDGE);
                entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.DASHBOARD);
                entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.USER);

                if (!customer.isPublic()) {
                    entityGroupService.findOrCreateCustomerUsersGroup(savedCustomer.getTenantId(), savedCustomer.getId(), savedCustomer.getParentCustomerId());
                    entityGroupService.findOrCreateCustomerAdminsGroup(savedCustomer.getTenantId(), savedCustomer.getId(), savedCustomer.getParentCustomerId());
                } else {
                    entityGroupService.findOrCreatePublicUsersGroup(savedCustomer.getTenantId(), savedCustomer.getId());
                }
                countService.publishCountEntityEvictEvent(savedCustomer.getTenantId(), EntityType.CUSTOMER);
            } else {
                if (oldCustomer != null && !savedCustomer.getName().equals(oldCustomer.getName())) {
                    List<EdgeId> edgeIds = edgeService.findAllRelatedEdgeIds(savedCustomer.getTenantId(), savedCustomer.getId());
                    if (edgeIds != null) {
                        for (EdgeId edgeId : edgeIds) {
                            Edge edge = edgeService.findEdgeById(savedCustomer.getTenantId(), edgeId);
                            edgeService.renameEdgeAllGroups(savedCustomer.getTenantId(), edge, edge.getName(), oldCustomer.getName(), savedCustomer.getName());
                        }
                    }
                    Optional<Customer> publicCustomerOpt = customerDao
                            .findPublicCustomerByTenantIdAndOwnerId(savedCustomer.getTenantId().getId(), savedCustomer.getId().getId());
                    if (publicCustomerOpt.isPresent()) {
                        Customer publicCustomer = publicCustomerOpt.get();
                        publicCustomer.setTitle(toPublicSubCustomerTitle(savedCustomer.getTitle()));
                        saveCustomer(publicCustomer);
                    }
                }
            }
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedCustomer.getTenantId())
                    .entityId(savedCustomer.getId()).created(customer.getId() == null).build());
            return savedCustomer;
        } catch (Exception e) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(e,
                    "customer_title_unq_key", "Customer with such title already exists!",
                    "customer_external_id_unq_key", "Customer with such external id already exists!");
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteCustomer(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteCustomer [{}]", customerId);
        Validator.validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        deleteCustomer(tenantId, customerId, true);
    }

    private void deleteCustomer(TenantId tenantId, CustomerId customerId, boolean deleteSubcustomers) {
        Customer customer = findCustomerById(tenantId, customerId);
        if (customer == null) {
            throw new IncorrectParameterException("Unable to delete non-existent customer.");
        }
        if (deleteSubcustomers) {
            try {
                List<CustomerId> customerIds = fetchSubcustomers(tenantId, customerId);
                for (CustomerId subCustomerId : customerIds) {
                    deleteCustomer(tenantId, subCustomerId, true);
                }
            } catch (Exception e) {
                log.error("Failed to delete subcustomers", e);
                throw new RuntimeException(e);
            }
        }
        whiteLabelingService.deleteDomainWhiteLabelingByEntityId(tenantId, customerId);
        dashboardService.deleteDashboardsByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        entityViewService.deleteEntityViewsByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        assetService.deleteAssetsByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        deviceService.deleteDevicesByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        edgeService.deleteEdgesByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        userService.deleteCustomerUsers(customer.getTenantId(), customerId);
        schedulerEventService.deleteSchedulerEventsByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        blobEntityService.deleteBlobEntitiesByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        deleteEntityGroups(tenantId, customerId);
        deleteEntityRelations(tenantId, customerId);
        roleService.deleteRolesByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        apiUsageStateService.deleteApiUsageStateByEntityId(customerId);
        customerDao.removeById(tenantId, customerId.getId());
        countService.publishCountEntityEvictEvent(tenantId, EntityType.CUSTOMER);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(customerId).build());
        publishEvictEvent(new CustomerCacheEvictEvent(customer.getTenantId(), customer.getTitle(), null));
    }

    private List<CustomerId> fetchSubcustomers(TenantId tenantId, CustomerId customerId) throws Exception {
        List<CustomerId> customerIds = new ArrayList<>();
        Optional<EntityGroup> entityGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, customerId, EntityType.CUSTOMER, EntityGroup.GROUP_ALL_NAME);
        if (entityGroup.isPresent()) {
            List<EntityId> childCustomerIds = entityGroupService.findAllEntityIdsAsync(tenantId, entityGroup.get().getId(), new PageLink(Integer.MAX_VALUE)).get();
            childCustomerIds.forEach(entityId -> customerIds.add(new CustomerId(entityId.getId())));
        }
        return customerIds;
    }

    @Override
    @Transactional
    public Customer findOrCreatePublicCustomer(TenantId tenantId, EntityId ownerId) {
        log.trace("Executing findOrCreatePublicCustomer, tenantId [{}], ownerId [{}]", tenantId, ownerId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateEntityId(ownerId, id -> INCORRECT_OWNER_ID + id);
        var publicCustomerOpt = customerDao.findPublicCustomerByTenantIdAndOwnerId(tenantId.getId(), ownerId.getId());
        if (publicCustomerOpt.isPresent()) {
            return publicCustomerOpt.get();
        }
        synchronized (publicCustomerCreationLocks.computeIfAbsent(new TbPair<>(tenantId, ownerId), k -> new Object())) {
            publicCustomerOpt = customerDao.findPublicCustomerByTenantIdAndOwnerId(tenantId.getId(), ownerId.getId());
            if (publicCustomerOpt.isPresent()) {
                return publicCustomerOpt.get();
            }
            var publicCustomer = new Customer();
            publicCustomer.setTenantId(tenantId);

            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                publicCustomer.setTitle(PUBLIC_CUSTOMER_SUFFIX);
            } else {
                Optional<String> ownerNameOpt = entityService.fetchEntityName(tenantId, ownerId);
                String ownerName = ownerNameOpt.orElseThrow(
                        () -> new RuntimeException("Failed to fetch owner name for ownerId: " + ownerId.getId().toString()));
                publicCustomer.setTitle(toPublicSubCustomerTitle(ownerName));
            }
            if (ownerId.getEntityType() == EntityType.CUSTOMER) {
                publicCustomer.setParentCustomerId(new CustomerId(ownerId.getId()));
            }
            try {
                publicCustomer.setAdditionalInfo(PUBLIC_CUSTOMER_ADDITIONAL_INFO_JSON);
            } catch (IllegalArgumentException e) {
                throw new IncorrectParameterException("Unable to create public customer", e);
            }
            return saveCustomer(publicCustomer, false);
        }
    }

    @Override
    @Transactional
    public EntityGroup findOrCreatePublicUserGroup(TenantId tenantId, EntityId ownerId) {
        log.trace("Executing findOrCreatePublicUserGroup, tenantId [{}], ownerId [{}]", tenantId, ownerId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateEntityId(ownerId, id -> INCORRECT_OWNER_ID + id);
        Customer publicCustomer = findOrCreatePublicCustomer(tenantId, ownerId);
        return entityGroupService.findOrCreatePublicUsersGroup(publicCustomer.getTenantId(), publicCustomer.getId());
    }

    @Override
    @Transactional
    public Role findOrCreatePublicUserEntityGroupRole(TenantId tenantId, EntityId ownerId) {
        log.trace("Executing findOrCreatePublicUserRole, tenantId [{}], ownerId [{}]", tenantId, ownerId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateEntityId(ownerId, id -> INCORRECT_OWNER_ID + id);
        Customer publicCustomer = findOrCreatePublicCustomer(tenantId, ownerId);
        return roleService.findOrCreatePublicUsersEntityGroupRole(publicCustomer.getTenantId(), publicCustomer.getId());
    }

    @Override
    public PageData<Customer> findCustomersByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findCustomersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> "Incorrect tenantId " + id);
        Validator.validatePageLink(pageLink);
        return customerDao.findCustomersByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteCustomersByTenantId(TenantId tenantId) {
        log.trace("Executing deleteCustomersByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, id -> "Incorrect tenantId " + id);
        customersByTenantRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public PageData<Customer> findCustomersByEntityGroupId(EntityGroupId groupId, PageLink pageLink) {
        log.trace("Executing findCustomersByEntityGroupId, groupId [{}], pageLink [{}]", groupId, pageLink);
        validateId(groupId, id -> "Incorrect entityGroupId " + id);
        validatePageLink(pageLink);
        return customerDao.findCustomersByEntityGroupId(groupId.getId(), pageLink);
    }

    @Override
    public PageData<Customer> findCustomersByEntityGroupIds(List<EntityGroupId> groupIds, List<CustomerId> additionalCustomerIds, PageLink pageLink) {
        log.trace("Executing findCustomersByEntityGroupId, groupIds [{}], additionalCustomerIds [{}], pageLink [{}]", groupIds, additionalCustomerIds, pageLink);
        validateIds(groupIds, ids -> "Incorrect groupIds " + ids);
        validatePageLink(pageLink);
        return customerDao.findCustomersByEntityGroupIds(toUUIDs(groupIds), toUUIDs(additionalCustomerIds), pageLink);
    }

    @Override
    public PageData<CustomerInfo> findCustomerInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findCustomerInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return customerInfoDao.findCustomersByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<CustomerInfo> findTenantCustomerInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantCustomerInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return customerInfoDao.findTenantCustomersByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<CustomerInfo> findCustomerInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findCustomerInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return customerInfoDao.findCustomersByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<CustomerInfo> findCustomerInfosByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findCustomerInfosByTenantIdAndCustomerIdIncludingSubCustomers, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return customerInfoDao.findCustomersByTenantIdAndCustomerIdIncludingSubCustomers(tenantId.getId(), customerId.getId(), pageLink);
    }

    private final PaginatedRemover<TenantId, Customer> customersByTenantRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Customer> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return customerDao.findCustomersByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Customer entity) {
                    deleteCustomer(tenantId, new CustomerId(entity.getUuidId()), false);
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

    public static String toPublicSubCustomerTitle(String ownerTitle) {
        return "[" + ownerTitle + "] " + PUBLIC_CUSTOMER_SUFFIX;
    }

}
