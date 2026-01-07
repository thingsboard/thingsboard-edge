/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class CustomerCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processCustomerMsgFromCloud(TenantId tenantId, CustomerUpdateMsg customerUpdateMsg) {
        CustomerId customerId = new CustomerId(new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (customerUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    customerCreationLock.lock();
                    try {
                        Customer customer = JacksonUtil.fromString(customerUpdateMsg.getEntity(), Customer.class, true);
                        if (customer == null) {
                            throw new RuntimeException("[{" + tenantId + "}] customerUpdateMsg {" + customerUpdateMsg + "} cannot be converted to customer");
                        }
                        CustomerService customerService = edgeCtx.getCustomerService();

                        Optional<Customer> edgeCustomer = customerService.findCustomerByTenantIdAndTitle(customer.getTenantId(), customer.getTitle());
                        edgeCustomer.filter(oldCustomer -> !oldCustomer.getId().equals(customer.getId()))
                                .ifPresent(value -> customerService.deleteCustomer(value.getTenantId(), value.getId()));

                        customerService.saveCustomer(customer, false);
                    } finally {
                        customerCreationLock.unlock();
                    }
                    return requestForAdditionalData(tenantId, customerId);
                case ENTITY_DELETED_RPC_MESSAGE:
                    Customer customerById = edgeCtx.getCustomerService().findCustomerById(tenantId, customerId);
                    if (customerById != null) {
                        edgeCtx.getCustomerService().deleteCustomer(tenantId, customerId);
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
        Customer customer = edgeCtx.getCustomerService().findCustomerById(tenantId, customerId);
        if (customer == null && customerId != null && !customerId.isNullUid()) {
            customerCreationLock.lock();
            try {
                customer = new Customer();
                customer.setId(customerId);
                customer.setCreatedTime(Uuids.unixTimestamp(customerId.getId()));
                customer.setTenantId(tenantId);
                customer.setTitle("TMP_NAME_" + StringUtils.randomAlphanumeric(10));
                edgeCtx.getCustomerService().saveCustomer(customer, false);
            } finally {
                customerCreationLock.unlock();
            }
        }
    }

}
