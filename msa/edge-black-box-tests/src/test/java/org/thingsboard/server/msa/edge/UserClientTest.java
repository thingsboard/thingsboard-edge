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
package org.thingsboard.server.msa.edge;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class UserClientTest extends AbstractContainerTest {

    @Test
    public void testUsers() {
        verifyEntityGroups(EntityType.USER, 3);
    }

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

