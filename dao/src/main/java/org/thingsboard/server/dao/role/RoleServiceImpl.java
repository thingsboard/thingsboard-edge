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
package org.thingsboard.server.dao.role;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.common.data.CacheConstants.ROLE_CACHE;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.*;

@Service
@Slf4j
public class RoleServiceImpl extends AbstractEntityService implements RoleService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_ROLE_ID = "Incorrect roleId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_ROLE_NAME = "Incorrect role name ";

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @CacheEvict(cacheNames = ROLE_CACHE, key = "{#role.id}")
    @Override
    public Role saveRole(TenantId tenantId, Role role) {
        log.trace("Executing save role [{}]", role);
        roleValidator.validate(role, Role::getTenantId);
        Role savedRole = roleDao.save(tenantId, role);
        return savedRole;
    }

    @Cacheable(cacheNames = ROLE_CACHE, key = "{#roleId}")
    @Override
    public Role findRoleById(TenantId tenantId, RoleId roleId) {
        log.trace("Executing findRoleById [{}]", roleId);
        validateId(roleId, INCORRECT_ROLE_ID + roleId);
        return roleDao.findById(tenantId, roleId.getId());
    }

    @Override
    public ListenableFuture<List<Role>> findRolesByIdsAsync(TenantId tenantId, List<RoleId> roleIds) {
        log.trace("Executing findRolesByIdsAsync, tenantId [{}], roleIds [{}]", tenantId, roleIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(roleIds, "Incorrect roleIds " + roleIds);
        return roleDao.findRolesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(roleIds));
    }

    @Override
    public Optional<Role> findRoleByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findRoleByTenantIdAndName, tenantId [{}], name [{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(name, INCORRECT_ROLE_NAME + name);
        return roleDao.findRoleByTenantIdAndName(tenantId.getId(), name);
    }

    @Override
    public Optional<Role> findRoleByByTenantIdAndCustomerIdAndName(TenantId tenantId, CustomerId customerId, String name) {
        log.trace("Executing findRoleByByTenantIdAndCustomerIdAndName, tenantId [{}], customerId [{}], name [{}]", tenantId, customerId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(name, INCORRECT_ROLE_NAME + name);
        return roleDao.findRoleByByTenantIdAndCustomerIdAndName(tenantId.getId(), customerId.getId(), name);
    }

    @Override
    public TextPageData<Role> findRolesByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findRolesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Role> roles = roleDao.findRolesByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(roles, pageLink);
    }

    @Override
    public TextPageData<Role> findRolesByTenantIdAndType(TenantId tenantId, TextPageLink pageLink, RoleType type) {
        log.trace("Executing findRolesByTenantIdAndType, tenantId [{}], pageLink [{}], type [{}]", tenantId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Role> roles = roleDao.findRolesByTenantIdAndType(tenantId.getId(), type, pageLink);
        return new TextPageData<>(roles, pageLink);
    }

    @Override
    public ListenableFuture<Role> findRoleByIdAsync(TenantId tenantId, RoleId roleId) {
        log.trace("Executing findRoleByIdAsync [{}]", roleId);
        validateId(roleId, INCORRECT_ROLE_ID + roleId);
        return roleDao.findByIdAsync(tenantId, roleId.getId());
    }

    @CacheEvict(cacheNames = ROLE_CACHE, key = "{#roleId}")
    @Override
    public void deleteRole(TenantId tenantId, RoleId roleId) {
        log.trace("Executing deleteRole [{}]", roleId);
        validateId(roleId, INCORRECT_ROLE_ID + roleId);
        groupPermissionService.deleteGroupPermissionsByTenantIdAndRoleId(tenantId, roleId);
        deleteEntityRelations(tenantId, roleId);
        roleDao.removeById(tenantId, roleId.getId());
    }

    @Override
    public void deleteRolesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteRolesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantRoleRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteRolesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteRolesByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerRoleRemover.removeEntities(tenantId, customerId);
    }

    @Override
    public TextPageData<Role> findRolesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
        log.trace("Executing findRolesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Role> roles = roleDao.findRolesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        return new TextPageData<>(roles, pageLink);
    }

    @Override
    public TextPageData<Role> findRolesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, RoleType type, TextPageLink pageLink) {
        log.trace("Executing findRolesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Role> roles = roleDao.findRolesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
        return new TextPageData<>(roles, pageLink);
    }

    private DataValidator<Role> roleValidator =
            new DataValidator<Role>() {

                @Override
                protected void validateCreate(TenantId tenantId, Role role) {
                    if (role.getCustomerId() == null || role.getCustomerId().isNullUid()) {
                        roleDao.findRoleByTenantIdAndName(role.getTenantId().getId(), role.getName())
                                .ifPresent(e -> {
                                    throw new DataValidationException("Role with such name already exists!");
                                });
                    } else {
                        roleDao.findRoleByByTenantIdAndCustomerIdAndName(role.getTenantId().getId(), role.getCustomerId().getId(), role.getName())
                                .ifPresent(e -> {
                                    throw new DataValidationException("Role with such name already exists!");
                                });
                    }
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Role role) {
                    if (role.getCustomerId() == null || role.getCustomerId().isNullUid()) {
                        roleDao.findRoleByTenantIdAndName(role.getTenantId().getId(), role.getName())
                                .ifPresent(e -> {
                                    if (!e.getUuidId().equals(role.getUuidId())) {
                                        throw new DataValidationException("Role with such name already exists!");
                                    }
                                });
                    } else {
                        roleDao.findRoleByByTenantIdAndCustomerIdAndName(role.getTenantId().getId(), role.getCustomerId().getId(), role.getName())
                                .ifPresent(e -> {
                                    if (!e.getUuidId().equals(role.getUuidId())) {
                                        throw new DataValidationException("Role with such name already exists!");
                                    }
                                });
                    }
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Role role) {
                    if (role.getType() == null) {
                        throw new DataValidationException("Role type should be specified!");
                    }
                    if (StringUtils.isEmpty(role.getName())) {
                        throw new DataValidationException("Role name should be specified!");
                    }
                    if (role.getTenantId() == null) {
                        role.setTenantId(new TenantId(NULL_UUID));
                    } else if (!role.getTenantId().isNullUid()) { // not Sys admin level
                        Tenant tenant = tenantDao.findById(tenantId, role.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Role is referencing to non-existent tenant!");
                        }
                    }
                    if (role.getCustomerId() == null) {
                        role.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!role.getCustomerId().isNullUid()) {
                        Customer customer = customerDao.findById(tenantId, role.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign role to non-existent customer!");
                        }
                        if (!customer.getTenantId().equals(role.getTenantId())) {
                            throw new DataValidationException("Can't assign role to customer from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Role> tenantRoleRemover =
            new PaginatedRemover<TenantId, Role>() {

                @Override
                protected List<Role> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
                    return roleDao.findRolesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Role entity) {
                    deleteRole(tenantId, new RoleId(entity.getUuidId()));
                }
            };

    private PaginatedRemover<CustomerId, Role> customerRoleRemover =
            new PaginatedRemover<CustomerId, Role>() {

                @Override
                protected List<Role> findEntities(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
                    return roleDao.findRolesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Role entity) {
                    deleteRole(tenantId, new RoleId(entity.getUuidId()));
                }
            };
}
