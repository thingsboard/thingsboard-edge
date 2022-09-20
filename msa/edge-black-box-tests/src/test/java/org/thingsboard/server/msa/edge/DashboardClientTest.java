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
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class DashboardClientTest extends AbstractContainerTest {

    @Test
    public void testDashboards() {
        // create dashboard and assign to edge
        Dashboard savedDashboardOnCloud = saveDashboardOnCloud("Edge Dashboard 1");
        cloudRestClient.assignDashboardToEdge(edge.getId(), savedDashboardOnCloud.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).isPresent());

        // update dashboard
        savedDashboardOnCloud.setTitle("Updated Dashboard Title");
        cloudRestClient.saveDashboard(savedDashboardOnCloud);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Updated Dashboard Title".equals(edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).get().getTitle()));

        // assign dashboard to customer
        Customer customer = new Customer();
        customer.setTitle("Dashboard Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        cloudRestClient.assignDashboardToCustomer(savedCustomer.getId(), savedDashboardOnCloud.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Dashboard dashboard = edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).get();
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

        // unassign dashboard from customer
        cloudRestClient.unassignDashboardFromCustomer(savedCustomer.getId(), savedDashboardOnCloud.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Dashboard dashboard = edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).get();
                    return dashboard.getAssignedCustomers().isEmpty();
                });
        cloudRestClient.deleteCustomer(savedCustomer.getId());

        // unassign dashboard from edge
        cloudRestClient.unassignDashboardFromEdge(edge.getId(), savedDashboardOnCloud.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).isEmpty());
        cloudRestClient.deleteDashboard(savedDashboardOnCloud.getId());
    }

    private Dashboard saveDashboardOnCloud(String dashboardTitle) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(dashboardTitle);
        return cloudRestClient.saveDashboard(dashboard);
    }
}

