/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class UserClientTest extends AbstractContainerTest {

    @Test
    public void testCreateUpdateDeleteTenantUser() {
        // create user
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(edge.getTenantId());
        user.setEmail("edgeTenant@thingsboard.org");
        user.setFirstName("Joe");
        user.setLastName("Downs");
        User savedUser = cloudRestClient.saveUser(user, false);
        cloudRestClient.activateUser(savedUser.getId(), "tenant", false);
        loginIntoEdgeWithRetries("edgeTenant@thingsboard.org", "tenant");
        cloudRestClient.login("edgeTenant@thingsboard.org", "tenant");

        // update user
        savedUser.setFirstName("John");
        cloudRestClient.saveUser(savedUser, false);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "John".equals(edgeRestClient.getUserById(savedUser.getId()).get().getFirstName()));

        // update user credentials
        cloudRestClient.changePassword("tenant", "newTenant");
        loginIntoEdgeWithRetries("edgeTenant@thingsboard.org", "newTenant");

        // delete user
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        cloudRestClient.deleteUser(savedUser.getId());
        loginIntoEdgeWithRetries("tenant@thingsboard.org", "tenant");
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getUserById(savedUser.getId()).isEmpty());
    }

    @Test
    public void testCreateUpdateDeleteCustomerUser() {
        // create customer
        Customer customer = new Customer();
        customer.setTitle("User Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);

        // create user
        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(edge.getTenantId());
        user.setCustomerId(savedCustomer.getId());
        user.setEmail("edgeCustomer@thingsboard.org");
        user.setFirstName("Phil");
        user.setLastName("Trace");
        User savedUser = cloudRestClient.saveUser(user, false);
        cloudRestClient.activateUser(savedUser.getId(), "customer", false);
        loginIntoEdgeWithRetries("edgeCustomer@thingsboard.org", "customer");
        cloudRestClient.login("edgeCustomer@thingsboard.org", "customer");

        // update user
        savedUser.setFirstName("Phillip");
        cloudRestClient.saveUser(savedUser, false);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Phillip".equals(edgeRestClient.getUserById(savedUser.getId()).get().getFirstName()));

        // update user credentials
        cloudRestClient.changePassword("customer", "newCustomer");
        loginIntoEdgeWithRetries("edgeCustomer@thingsboard.org", "newCustomer");

        // delete user
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        cloudRestClient.deleteUser(savedUser.getId());
        cloudRestClient.deleteCustomer(savedCustomer.getId());
        loginIntoEdgeWithRetries("tenant@thingsboard.org", "tenant");
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getUserById(savedUser.getId()).isEmpty());
    }
}

