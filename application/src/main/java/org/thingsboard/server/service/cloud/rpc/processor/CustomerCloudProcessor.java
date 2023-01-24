/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
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

    public ListenableFuture<Void> processCustomerMsgFromCloud(TenantId tenantId, CustomerUpdateMsg customerUpdateMsg) {
        CustomerId customerId = new CustomerId(new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB()));
        switch (customerUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                customerCreationLock.lock();
                try {
                    Customer customer = customerService.findCustomerById(tenantId, customerId);
                    if (customer == null) {
                        customer = new Customer();
                        customer.setId(customerId);
                        customer.setCreatedTime(Uuids.unixTimestamp(customerId.getId()));
                        customer.setTenantId(tenantId);
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
                    customerService.saveCustomer(customer, false);
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

    public void createCustomerIfNotExists(TenantId tenantId, EdgeConfiguration edgeConfiguration) {
        CustomerId customerId = safeGetCustomerId(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB());
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer == null) {
            customerCreationLock.lock();
            try {
                customer = new Customer();
                customer.setId(customerId);
                customer.setCreatedTime(Uuids.unixTimestamp(customerId.getId()));
                customer.setTenantId(tenantId);
                customer.setTitle("TMP_NAME_" + StringUtils.randomAlphanumeric(10));
                customerService.saveCustomer(customer, false);
            } finally {
                customerCreationLock.unlock();
            }
        }
    }
}
