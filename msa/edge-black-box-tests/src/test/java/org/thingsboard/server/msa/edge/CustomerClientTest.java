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
package org.thingsboard.server.msa.edge;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class CustomerClientTest extends AbstractContainerTest {

    @Test
    public void testCreateUpdateDeleteCustomer() {
        // create customer
        Customer customer = new Customer();
        customer.setTitle("Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isPresent());

        // update customer
        savedCustomer.setTitle("Updated Customer Name");
        cloudRestClient.saveCustomer(savedCustomer);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Updated Customer Name".equals(edgeRestClient.getCustomerById(savedCustomer.getId()).get().getTitle()));

        // delete customer
        cloudRestClient.deleteCustomer(savedCustomer.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isEmpty());
    }

    @Test
    public void testPublicCustomerCreatedOnEdge() {
        Customer publicCustomer = findPublicCustomer();
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(publicCustomer.getId()).isPresent());
    }
}

