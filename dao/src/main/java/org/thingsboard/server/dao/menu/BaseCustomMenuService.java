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
package org.thingsboard.server.dao.menu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.CustomMenuAssignResult;
import org.thingsboard.server.common.data.CustomMenuDeleteResult;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuAssigneeInfo;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.menu.MenuItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.checkNotNull;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseCustomMenuService extends AbstractCachedEntityService<CustomMenuId, CustomMenu, CustomMenuCacheEvictEvent> implements CustomMenuService {

    private static final String INCORRECT_CUSTOM_MENU_ID = "Incorrect customMenuId ";
    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    private static final String INCORRECT_USER_ID = "Incorrect userId ";

    private final CustomerService customerService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomMenuDao customMenuDao;
    private final DataValidator<CustomMenuInfo> customMenuInfoValidator;

    @Override
    public CustomMenu createCustomMenu(CustomMenuInfo customMenuInfo, List<EntityId> assignToList) {
        log.trace("Executing saveCustomMenu [{}]", customMenuInfo);
        customMenuInfoValidator.validate(customMenuInfo, CustomMenuInfo::getTenantId);
        return saveCustomMenu(new CustomMenu(customMenuInfo), assignToList);
    }

    @Override
    public boolean updateCustomMenuName(CustomMenuId customMenuId, String name) {
        log.trace("Executing updateCustomMenuName [{}] [{}] ", customMenuId, name);
        return customMenuDao.updateCustomMenuName(customMenuId, name);
    }

    @Override
    public CustomMenu updateCustomMenu(CustomMenu customMenu) {
        log.trace("Executing updateCustomMenu [{}] ", customMenu);
        customMenuInfoValidator.validate(customMenu, CustomMenuInfo::getTenantId);
        return saveCustomMenu(customMenu, null);
    }

    @Override
    public CustomMenuAssignResult assignCustomMenu(TenantId tenantId, CustomMenuId customMenuId, CMAssigneeType newAssigneeType, List<EntityId> newAssignToList, boolean force) {
        CustomMenuAssigneeInfo oldCMInfo = findCustomMenuAssigneeInfoById(tenantId, customMenuId);
        List<EntityId> existingEntityIds = oldCMInfo.getAssigneeList()
                .stream()
                .map(EntityInfo::getId)
                .toList();

        List<EntityId> toRemoveEntityIds = existingEntityIds.stream()
                .filter(entityId -> newAssignToList.stream().noneMatch(id -> id.equals(entityId)))
                .toList();
        List<EntityId> toAddEntityIds = newAssignToList.stream()
                .filter(entityId -> !existingEntityIds.contains(entityId))
                .toList();

        CustomMenuAssignResult.CustomMenuAssignResultBuilder result = CustomMenuAssignResult.builder();

        boolean success = true;
        CMAssigneeType oldAssigneeType = oldCMInfo.getAssigneeType();
        if (!force && (oldAssigneeType != newAssigneeType)) {
            success = false;
            result.oldAssigneeType(oldAssigneeType);
            result.oldAsigneeList(oldCMInfo.getAssigneeList());
        }
        if (success) {
            if (oldAssigneeType != newAssigneeType) {
                CustomMenu customMenu = findCustomMenuById(oldCMInfo.getTenantId(), oldCMInfo.getId());
                customMenu.setAssigneeType(newAssigneeType);
                updateCustomMenu(customMenu);
            }
            assignCustomMenu(oldCMInfo.getId(), newAssigneeType, toAddEntityIds);
            unassignCustomMenu(oldAssigneeType, toRemoveEntityIds);
        }
        return result.success(success).build();
    }

    private void assignCustomMenu(CustomMenuId customMenuId, CMAssigneeType assigneeType, List<EntityId> assignToList) {
        switch (assigneeType) {
            case CUSTOMERS:
                List<CustomerId> customerIds = assignToList.stream().map(CustomerId.class::cast).collect(Collectors.toList());
                customerService.updateCustomersCustomMenuId(customerIds, customMenuId.getId());
                break;
            case USERS:
                List<UserId> userIds = assignToList.stream().map(UserId.class::cast).collect(Collectors.toList());
                userService.updateUsersCustomMenuId(userIds, customMenuId.getId());
                break;
            case NO_ASSIGN:
            case ALL:
                break;
            default:
                throw new IncorrectParameterException("Assignee list is applicable to 'CUSTOMERS' or 'USERS' assignee type only!");
        }
    }

    private void unassignCustomMenu(CMAssigneeType assigneeType, List<EntityId> toRemoveEntityIds) {
        switch (assigneeType) {
            case CUSTOMERS:
                if (!CollectionUtils.isEmpty(toRemoveEntityIds)) {
                    List<CustomerId> toRemoveCustomerIds = toRemoveEntityIds.stream().map(CustomerId.class::cast).collect(Collectors.toList());
                    customerService.updateCustomersCustomMenuId(toRemoveCustomerIds, null);
                }
                break;
            case USERS:
                if (!CollectionUtils.isEmpty(toRemoveEntityIds)) {
                    List<UserId> toRemoveUserIds = toRemoveEntityIds.stream().map(UserId.class::cast).collect(Collectors.toList());
                    userService.updateUsersCustomMenuId(toRemoveUserIds, null);
                }
                break;
            default:
                throw new IncorrectParameterException("List of assigners can be applied only to CUSTOMERS or USERS assignee type!");
        }
    }

    @Override
    public CustomMenuInfo findCustomMenuInfoById(TenantId tenantId, CustomMenuId customMenuId) {
        log.trace("Executing findCustomMenuInfoById [{}]", customMenuId);
        validateId(customMenuId, id -> INCORRECT_CUSTOM_MENU_ID + id);
        return new CustomMenuInfo(cache.getAndPutInTransaction(customMenuId, () -> customMenuDao.findById(tenantId, customMenuId.getId()), true));
    }

    @Override
    public CustomMenu findCustomMenuById(TenantId tenantId, CustomMenuId customMenuId) {
        log.trace("Executing findCustomMenuById [{}]", customMenuId);
        validateId(customMenuId, id -> INCORRECT_CUSTOM_MENU_ID + id);
        return cache.getAndPutInTransaction(customMenuId, () -> customMenuDao.findById(tenantId, customMenuId.getId()), true);
    }

    @Override
    public PageData<CustomMenuInfo> getCustomMenuInfos(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing getCustomerUserCustomMenus [{}] [{}]", tenantId, customerId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(tenantId, id -> INCORRECT_CUSTOMER_ID + id);
        return customMenuDao.findByTenantIdAndCustomerId(tenantId, customerId, pageLink);
    }

    @Override
    public CustomMenuConfig getSystemAdminCustomMenuConfig() {
        log.trace("Executing getSystemAdminCustomMenu");
        CustomMenu customMenu = findDefaultCustomMenuByScope(TenantId.SYS_TENANT_ID, null, CMScope.SYSTEM);
        return getVisibleMenuItems(customMenu);
    }

    @Override
    public CustomMenuConfig getTenantUserCustomMenuConfig(TenantId tenantId, UserId userId) {
        log.trace("Executing getTenantUserCustomMenu [{}] ", userId);
        Validator.validateId(userId, id -> INCORRECT_USER_ID + id);
        CustomMenu result = findCustomMenuByUserId(tenantId, userId);
        if (result == null) {
            result = findDefaultCustomMenuByScope(tenantId, null, CMScope.TENANT);
            if (result == null) {
                result = findDefaultCustomMenuByScope(TenantId.SYS_TENANT_ID, null, CMScope.TENANT);
            }
        }
        return getVisibleMenuItems(result);
    }

    @Override
    public CustomMenuConfig getCustomerUserCustomMenuConfig(TenantId tenantId, CustomerId customerId, UserId userId) {
        log.trace("Executing getCustomerUserCustomMenu [{}] ", userId);
        Validator.validateId(userId, id -> INCORRECT_USER_ID + id);
        CustomMenu result = findCustomMenuByUserId(tenantId, userId);
        if (result == null) {
            result = findCustomerHierarchyCustomMenu(tenantId, customerId);
            if (result == null) {
                result = findDefaultCustomMenuByScope(tenantId, null, CMScope.CUSTOMER);
            }
            if (result == null) {
                result = findDefaultCustomMenuByScope(TenantId.SYS_TENANT_ID, null, CMScope.CUSTOMER);
            }
        }
        return getVisibleMenuItems(result);
    }

    @Override
    public CustomMenuDeleteResult deleteCustomMenu(TenantId tenantId, CustomMenuId customMenuId, boolean force) {
        log.trace("Executing deleteCustomMenu [{}]", customMenuId);
        CustomMenuAssigneeInfo customMenuAssigneeInfo = findCustomMenuAssigneeInfoById(tenantId, customMenuId);
        CustomMenuDeleteResult.CustomMenuDeleteResultBuilder result = CustomMenuDeleteResult.builder()
                .assigneeType(customMenuAssigneeInfo.getAssigneeType());
        boolean success = true;
        if (!force && !customMenuAssigneeInfo.getAssigneeList().isEmpty()) {
            success = false;
            result.assigneeList(customMenuAssigneeInfo.getAssigneeList());
        }
        if (success) {
            //delete assignee list
            if (!customMenuAssigneeInfo.getAssigneeList().isEmpty() && force) {
                List<EntityId> entityIds = customMenuAssigneeInfo.getAssigneeList().stream().map(EntityInfo::getId).toList();
                unassignCustomMenu(customMenuAssigneeInfo.getAssigneeType(), entityIds);
            }
            deleteCustomMenu(tenantId, customMenuAssigneeInfo);
        }
        return result.success(success).build();
    }

    private void deleteCustomMenu(TenantId tenantId, CustomMenuInfo customMenuInfo) {
        try {
            customMenuDao.removeById(tenantId, customMenuInfo.getId().getId());
//            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entity(customMenuInfo)
//                    .edgeEventType(EdgeEventType.CUSTOM_MENU).actionType(ActionType.DELETED).build());
        } catch (Exception t) {
            ConstraintViolationException e = DaoUtil.extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_user_custom_menu")) {
                throw new DataValidationException("The custom menu referenced by the user cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public CustomMenuAssigneeInfo findCustomMenuAssigneeInfoById(TenantId tenantId, CustomMenuId customMenuId) {
        log.trace("Executing findCustomMenuById [{}]", customMenuId);
        validateId(customMenuId, id -> INCORRECT_CUSTOM_MENU_ID + id);
        CustomMenu customMenu = cache.getAndPutInTransaction(customMenuId, () -> customMenuDao.findById(tenantId, customMenuId.getId()), true);
        return customMenu == null ? null : new CustomMenuAssigneeInfo(customMenu, getMenuAssigners(customMenu));
    }

    @Override
    public CustomMenu findDefaultCustomMenuByScope(TenantId tenantId, CustomerId customerId, CMScope scope) {
        log.trace("Executing findDefaultCustomMenuByScope [{}] [{}] [{}]", tenantId, customerId, scope);
        checkNotNull(scope, "Scope could not be null");
        return customMenuDao.findDefaultMenuByScope(tenantId, customerId, scope);
    }

    private CustomMenu saveCustomMenu(CustomMenu customMenu, List<EntityId> assignToList) {
        try {
            CustomMenu savedCustomMenu = customMenuDao.saveAndFlush(customMenu.getTenantId(), customMenu);
            if (CollectionUtils.isNotEmpty(assignToList)) {
                assignCustomMenu(savedCustomMenu.getId(), customMenu.getAssigneeType(), assignToList);
            }
            publishEvictEvent(new CustomMenuCacheEvictEvent(savedCustomMenu.getId()));
            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(savedCustomMenu.getTenantId()).entityId(getEntityIdForEvent(customMenu.getTenantId(), customMenu.getCustomerId()))
                    .edgeEventType(EdgeEventType.CUSTOM_MENU).actionType(ActionType.UPDATED).build());
            return savedCustomMenu;
        } catch (Exception t) {
            throw t;
        }
    }

    private static CustomMenuConfig getVisibleMenuItems(CustomMenu customMenu) {
        if (customMenu == null || customMenu.getConfig() == null) {
            return null;
        }
        return new CustomMenuConfig(customMenu.getConfig().getItems().stream()
                .filter(MenuItem::isVisible)
                .map(MenuItem.class::cast)
                .collect(Collectors.toList()));
    }

    @Override
    public List<EntityInfo> getMenuAssigners(CustomMenuInfo customMenuInfo) {
        List<EntityInfo> assigners = new ArrayList<>();
        switch (customMenuInfo.getAssigneeType()) {
            case NO_ASSIGN:
            case ALL:
                break;
            case CUSTOMERS:
                List<Customer> customers = customerService.findCustomersByCustomMenuId(customMenuInfo.getId());
                for (Customer customer : customers) {
                    assigners.add(new EntityInfo(customer.getId(), customer.getName()));
                }
                break;
            case USERS:
                List<User> users = userService.findUsersByCustomMenuId(customMenuInfo.getId());
                for (User user : users) {
                    assigners.add(new EntityInfo(user.getId(), user.getName()));
                }
                break;
            default:
                throw new RuntimeException("Invalid custom menu assignee type '" + customMenuInfo.getAssigneeType() + "' specified for custom menu!");
        }
        return assigners;
    }

    private CustomMenu findCustomMenuByUserId(TenantId tenantId, UserId userId) {
        CustomMenuId customMenuId = userService.findUserById(tenantId, userId).getCustomMenuId();
        return customMenuId == null ? null : findCustomMenuById(tenantId, customMenuId);
    }

    private CustomMenu findCustomerHierarchyCustomMenu(TenantId tenantId, CustomerId customerId) {
        CustomMenu result = findDefaultCustomMenuByScope(tenantId, customerId, CMScope.CUSTOMER);
        if (result == null) {
            Customer customer = customerService.findCustomerById(tenantId, customerId);
            CustomMenuId customerCustomMenuId = customer.getCustomMenuId();
            if (customerCustomMenuId != null) {
                result = findCustomMenuById(tenantId, customerCustomMenuId);
            }
            if (result == null) {
                if (customer.isSubCustomer()) {
                    result = findCustomerHierarchyCustomMenu(tenantId, customer.getParentCustomerId());
                }
            }
        }
        return result;
    }

    private static EntityId getEntityIdForEvent(TenantId tenantId, CustomerId customerId) {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @TransactionalEventListener(classes = CustomMenuCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(CustomMenuCacheEvictEvent event) {
        cache.evict(event.getCustomMenuId());
    }
}
