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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
public class RoleDataValidator extends DataValidator<Role> {

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    @Lazy
    private RoleService roleService;

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
    protected Role validateUpdate(TenantId tenantId, Role role) {
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

        Role before = roleService.findRoleById(tenantId, role.getId());
        if (role.getType() != before.getType()) {
            throw new DataValidationException("Role type cannot be changed after role creation");
        }
        return before;
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
            if (!tenantService.tenantExists(role.getTenantId())) {
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
}
