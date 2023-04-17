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
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class DashboardClientTest extends AbstractContainerTest {

    @Test
    public void testDashboards() {
        // create dashboard #1 and assign to edge
        Dashboard savedDashboard1 = saveDashboardOnCloud("Edge Dashboard 1");
        cloudRestClient.assignDashboardToEdge(edge.getId(), savedDashboard1.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboard1.getId()).isPresent());

        // update dashboard #1
        savedDashboard1.setTitle("Updated Dashboard Title");
        cloudRestClient.saveDashboard(savedDashboard1);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Updated Dashboard Title".equals(edgeRestClient.getDashboardById(savedDashboard1.getId()).get().getTitle()));

        // unassign dashboard #1 from edge
        cloudRestClient.unassignDashboardFromEdge(edge.getId(), savedDashboard1.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboard1.getId()).isEmpty());
        cloudRestClient.deleteDashboard(savedDashboard1.getId());

        // create dashboard #2 and assign to edge
        Dashboard savedDashboard2 = saveDashboardOnCloud("Edge Dashboard 2");
        cloudRestClient.assignDashboardToEdge(edge.getId(), savedDashboard2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboard2.getId()).isPresent());

        // assign dashboard #2 to customer
        Customer customer = new Customer();
        customer.setTitle("Dashboard Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);
        cloudRestClient.assignDashboardToCustomer(savedCustomer.getId(), savedDashboard2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Dashboard dashboard = edgeRestClient.getDashboardById(savedDashboard2.getId()).get();
                    if (dashboard.getAssignedCustomers() == null
                            || dashboard.getAssignedCustomers().isEmpty()) {
                        return false;
                    }
                    if (dashboard.getAssignedCustomers().size() != 1)  {
                        return false;
                    }
                    ShortCustomerInfo assignedCustomer = dashboard.getAssignedCustomers().iterator().next();
                    return savedCustomer.getId().equals(assignedCustomer.getCustomerId());
                });

        // unassign dashboard #2 from customer
        cloudRestClient.unassignDashboardFromCustomer(savedCustomer.getId(), savedDashboard2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Dashboard dashboard = edgeRestClient.getDashboardById(savedDashboard2.getId()).get();
                    return dashboard.getAssignedCustomers().isEmpty();
                });
        cloudRestClient.deleteCustomer(savedCustomer.getId());

        // delete dashboard #2
        cloudRestClient.deleteDashboard(savedDashboard2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboard2.getId()).isEmpty());
    }
}

