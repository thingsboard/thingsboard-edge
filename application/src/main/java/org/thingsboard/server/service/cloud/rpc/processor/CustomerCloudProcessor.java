/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Component
@Slf4j
public class CustomerCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private CustomerService customerService;

    public ListenableFuture<Void> processCustomerMsgFromCloud(TenantId tenantId, CustomerUpdateMsg customerUpdateMsg) {
        CustomerId customerId = new CustomerId(new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB()));
        switch (customerUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                customerCreationLock.lock();
                try {
                    Customer customer = customerService.findCustomerById(tenantId, customerId);
                    boolean created = false;
                    if (customer == null) {
                        customer = new Customer();
                        customer.setId(customerId);
                        customer.setCreatedTime(Uuids.unixTimestamp(customerId.getId()));
                        customer.setTenantId(tenantId);
                        created = true;
                    }
                    customer.setTitle(customerUpdateMsg.getTitle());
                    customer.setCountry(customerUpdateMsg.hasCountry() ? customerUpdateMsg.getCountry() : null);
                    customer.setState(customerUpdateMsg.hasState() ? customerUpdateMsg.getState() : null);
                    customer.setCity(customerUpdateMsg.hasCity() ? customerUpdateMsg.getCity() : null);
                    customer.setAddress(customerUpdateMsg.hasAddress() ? customerUpdateMsg.getAddress() : null);
                    customer.setAddress2(customerUpdateMsg.hasAddress2() ? customerUpdateMsg.getAddress2() : null);
                    customer.setZip(customerUpdateMsg.hasZip() ? customerUpdateMsg.getZip() : null);
                    customer.setPhone(customerUpdateMsg.hasPhone() ? customerUpdateMsg.getPhone() : null);
                    customer.setEmail(customerUpdateMsg.hasEmail() ? customerUpdateMsg.getEmail() : null);
                    customer.setAdditionalInfo(customerUpdateMsg.hasAdditionalInfo() ? JacksonUtil.toJsonNode(customerUpdateMsg.getAdditionalInfo()) : null);
                    EntityId ownerId = safeGetOwnerId(tenantId, customerUpdateMsg.getOwnerEntityType(),
                            customerUpdateMsg.getOwnerIdMSB(), customerUpdateMsg.getOwnerIdLSB());
                    customer.setOwnerId(ownerId);
                    Customer savedCustomer = customerService.saveCustomer(customer, false);

                    if (created) {
                        entityGroupService.addEntityToEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getOwnerId(), savedCustomer.getId());
                        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.CUSTOMER);
                        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.ASSET);
                        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.DEVICE);
                        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.ENTITY_VIEW);
                        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.EDGE);
                        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.DASHBOARD);
                        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.USER);
                    }
                } finally {
                    customerCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                Customer customerById = customerService.findCustomerById(tenantId, customerId);
                if (customerById != null) {
                    customerService.deleteCustomer(tenantId, customerId);
                }
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(customerUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }
}
