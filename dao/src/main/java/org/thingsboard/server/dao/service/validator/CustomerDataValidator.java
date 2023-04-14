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
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.customer.CustomerServiceImpl;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Optional;

@Component
public class CustomerDataValidator extends DataValidator<Customer> {

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private TenantService tenantService;

    @Override
    protected void validateCreate(TenantId tenantId, Customer customer) {
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.CUSTOMER);
        customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle()).ifPresent(
                c -> {
                    throw new DataValidationException("Customer with such title already exists!");
                }
        );
    }

    @Override
    protected Customer validateUpdate(TenantId tenantId, Customer customer) {
        Optional<Customer> customerOpt = customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle());
        customerOpt.ifPresent(
                c -> {
                    if (!c.getId().equals(customer.getId())) {
                        throw new DataValidationException("Customer with such title already exists!");
                    }
                }
        );
        return customerOpt.orElse(null);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Customer customer) {
        if (StringUtils.isEmpty(customer.getTitle())) {
            throw new DataValidationException("Customer title should be specified!");
        }
        if (customer.getTitle().equals(CustomerServiceImpl.PUBLIC_CUSTOMER_TITLE)) {
            throw new DataValidationException("'Public' title for customer is system reserved!");
        }
        if (!StringUtils.isEmpty(customer.getEmail())) {
            validateEmail(customer.getEmail());
        }
        if (customer.getTenantId() == null) {
            throw new DataValidationException("Customer should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(customer.getTenantId())) {
                throw new DataValidationException("Customer is referencing to non-existent tenant!");
            }
        }
    }
}
