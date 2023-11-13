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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Component
@Slf4j
public class CustomerCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private CustomerService customerService;

    public ListenableFuture<Void> processCustomerMsgFromCloud(TenantId tenantId, CustomerUpdateMsg customerUpdateMsg,
                                                              Long queueStartTs) throws ThingsboardException {
        CustomerId customerId = new CustomerId(new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (customerUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    customerCreationLock.lock();
                    try {
                        Customer customer = JacksonUtil.fromStringIgnoreUnknownProperties(customerUpdateMsg.getEntity(), Customer.class);
                        if (customer == null) {
                            throw new RuntimeException("[{" + tenantId + "}] customerUpdateMsg {" + customerUpdateMsg + "} cannot be converted to customer");
                        }
                        EntityId ownerId = customer.getOwnerId();
                        if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                            createCustomerIfNotExists(tenantId, new CustomerId(ownerId.getId()));
                        }
                        boolean created = false;
                        Customer customerById = customerService.findCustomerById(tenantId, customerId);
                        if (customerById == null) {
                            created = true;
                        } else {
                            CustomerId tmpCustomerOwnerId = new CustomerId(EntityId.NULL_UUID);
                            if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                                tmpCustomerOwnerId = new CustomerId(ownerId.getId());
                            }
                            changeOwnerIfRequired(tenantId, tmpCustomerOwnerId, customerId);
                        }
                        Customer savedCustomer = customerService.saveCustomer(customer, false);
                        if (created) {
                            postCreateSteps(savedCustomer);
                        }
                    } finally {
                        customerCreationLock.unlock();
                    }
                    return requestForAdditionalData(tenantId, customerId, queueStartTs);
                case ENTITY_DELETED_RPC_MESSAGE:
                    Customer customerById = customerService.findCustomerById(tenantId, customerId);
                    if (customerById != null) {
                        customerService.deleteCustomer(tenantId, customerId);
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(customerUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    public void createCustomerIfNotExists(TenantId tenantId, EdgeConfiguration edgeConfiguration) {
        CustomerId customerId = safeGetCustomerId(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB());
        createCustomerIfNotExists(tenantId, customerId);
    }

    private void createCustomerIfNotExists(TenantId tenantId, CustomerId customerId) {
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer == null && customerId != null && !customerId.isNullUid()) {
            customerCreationLock.lock();
            try {
                customer = new Customer();
                customer.setId(customerId);
                customer.setCreatedTime(Uuids.unixTimestamp(customerId.getId()));
                customer.setTenantId(tenantId);
                customer.setTitle("TMP_NAME_" + StringUtils.randomAlphanumeric(10));
                Customer savedCustomer = customerService.saveCustomer(customer, false);
                postCreateSteps(savedCustomer);
            } finally {
                customerCreationLock.unlock();
            }
        }
    }

    private void postCreateSteps(Customer savedCustomer) {
        entityGroupService.addEntityToEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getOwnerId(), savedCustomer.getId());
        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.CUSTOMER);
        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.ASSET);
        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.DEVICE);
        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.ENTITY_VIEW);
        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.EDGE);
        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.DASHBOARD);
        entityGroupService.createEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getId(), EntityType.USER);
    }
}
