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

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
@AllArgsConstructor
public class EdgeDataValidator extends DataValidator<Edge> {

    private final EdgeDao edgeDao;
    private final TenantService tenantService;
    private final CustomerDao customerDao;

    @Override
    protected void validateCreate(TenantId tenantId, Edge edge) {
    }

    @Override
    protected Edge validateUpdate(TenantId tenantId, Edge edge) {
        return edgeDao.findById(edge.getTenantId(), edge.getId().getId());
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Edge edge) {
        if (StringUtils.isEmpty(edge.getType())) {
            throw new DataValidationException("Edge type should be specified!");
        }
        if (StringUtils.isEmpty(edge.getName())) {
            throw new DataValidationException("Edge name should be specified!");
        }
        if (StringUtils.isEmpty(edge.getSecret())) {
            throw new DataValidationException("Edge secret should be specified!");
        }
        if (StringUtils.isEmpty(edge.getRoutingKey())) {
            throw new DataValidationException("Edge routing key should be specified!");
        }
        if (StringUtils.isEmpty(edge.getEdgeLicenseKey())) {
            throw new DataValidationException("Edge license key should be specified!");
        }
        if (StringUtils.isEmpty(edge.getCloudEndpoint())) {
            throw new DataValidationException("Cloud endpoint should be specified!");
        }
        if (edge.getTenantId() == null) {
            throw new DataValidationException("Edge should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(edge.getTenantId())) {
                throw new DataValidationException("Edge is referencing to non-existent tenant!");
            }
        }
        if (edge.getCustomerId() == null) {
            edge.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!edge.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(edge.getTenantId(), edge.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign edge to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(edge.getTenantId().getId())) {
                throw new DataValidationException("Can't assign edge to customer from different tenant!");
            }
        }
    }
}
