/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.CloudType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class CustomerCloudProcessor extends BaseCloudProcessor {

    private final Lock customerCreationLock = new ReentrantLock();

    @Autowired
    private CustomerService customerService;

    public ListenableFuture<Void> processCustomerMsgFromCloud(TenantId tenantId, CustomerUpdateMsg customerUpdateMsg, CloudType cloudType) {
        CustomerId customerId = new CustomerId(new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB()));
        switch (customerUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    customerCreationLock.lock();
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
                    customer.setCountry(customerUpdateMsg.getCountry());
                    customer.setState(customerUpdateMsg.getState());
                    customer.setCity(customerUpdateMsg.getCity());
                    customer.setAddress(customerUpdateMsg.getAddress());
                    customer.setAddress2(customerUpdateMsg.getAddress2());
                    customer.setZip(customerUpdateMsg.getZip());
                    customer.setPhone(customerUpdateMsg.getPhone());
                    customer.setEmail(customerUpdateMsg.getEmail());
                    customer.setAdditionalInfo(JacksonUtil.toJsonNode(customerUpdateMsg.getAdditionalInfo()));
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

                    if (created && CloudType.CE.equals(cloudType)) {
                        createCustomerEntityGroupsOnTenantLevel(tenantId, savedCustomer.getId());
                    }

                } finally {
                    customerCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                Customer customerById = customerService.findCustomerById(tenantId, customerId);
                if (customerById != null) {
                    deleteCustomerEntityGroupsOnTenantLevel(tenantId, customerById.getId());
                    customerService.deleteCustomer(tenantId, customerId);
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
                return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type " + customerUpdateMsg.getMsgType()));
        }
        return Futures.immediateFuture(null);
    }

    private void deleteCustomerEntityGroupsOnTenantLevel(TenantId tenantId, CustomerId customerId) {
        deleteCustomerEntityGroupOnTenantLevel(tenantId, customerId, EntityType.DEVICE);
        deleteCustomerEntityGroupOnTenantLevel(tenantId, customerId, EntityType.ASSET);
        deleteCustomerEntityGroupOnTenantLevel(tenantId, customerId, EntityType.ENTITY_VIEW);
        deleteCustomerEntityGroupOnTenantLevel(tenantId, customerId, EntityType.DASHBOARD);
    }

    private void deleteCustomerEntityGroupOnTenantLevel(TenantId tenantId, CustomerId customerId, EntityType entityType) {
        EntityGroup entityGroup = entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(tenantId, customerId, entityType);
        if (entityGroup != null && entityGroup.getId() != null) {
            entityGroupService.deleteEntityGroup(tenantId, entityGroup.getId());
        }
    }

    private void createCustomerEntityGroupsOnTenantLevel(TenantId tenantId, CustomerId customerId) {
        createCustomerEntityGroupOnTenantLevel(tenantId, customerId, EntityType.DEVICE);
        createCustomerEntityGroupOnTenantLevel(tenantId, customerId, EntityType.ASSET);
        createCustomerEntityGroupOnTenantLevel(tenantId, customerId, EntityType.ENTITY_VIEW);
        createCustomerEntityGroupOnTenantLevel(tenantId, customerId, EntityType.DASHBOARD);
    }

    private void createCustomerEntityGroupOnTenantLevel(TenantId tenantId, CustomerId customerId, EntityType entityType) {
        EntityGroup entityGroup = entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(tenantId, customerId, entityType);
        Role readOnlyGroupRole = roleService.findOrCreateReadOnlyEntityGroupRole(tenantId, null);

        EntityGroup edgeCECustomerUsers =
                entityGroupService.findOrCreateEdgeCECustomerUsersGroup(tenantId, customerId);

        entityGroupService.findOrCreateEntityGroupPermission(tenantId,
                entityGroup.getId(),
                entityGroup.getType(),
                edgeCECustomerUsers.getId(),
                readOnlyGroupRole.getId());
    }
}
