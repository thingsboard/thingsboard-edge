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
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class CustomerEdgeEventFetcher extends BasePageableEdgeEventFetcher<Customer> {

    private final CustomerService customerService;
    private final CustomerId ownerId;

    @Override
    PageData<Customer> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        List<Customer> customersHierarchy = getCustomersHierarchy(tenantId, ownerId);
        return new PageData<>(customersHierarchy, 1, customersHierarchy.size(), false);
    }

    List<Customer> getCustomersHierarchy(TenantId tenantId, CustomerId customerId) {
        List<Customer> result = new ArrayList<>();
        Customer customerById = customerService.findCustomerById(tenantId, customerId);
        result.add(customerById);
        if (customerById != null && customerById.getParentCustomerId() != null && !customerById.getParentCustomerId().isNullUid()) {
            result.addAll(getCustomersHierarchy(tenantId, customerById.getParentCustomerId()));
        }
        return result;
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Customer customer) {
        return EdgeUtils.constructEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER,
                EdgeEventActionType.ADDED, customer.getId(), null);
    }
}
