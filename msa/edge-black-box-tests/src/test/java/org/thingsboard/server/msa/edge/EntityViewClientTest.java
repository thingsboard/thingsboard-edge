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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class EntityViewClientTest extends AbstractContainerTest {

    @Test
    public void testEntityViews() {
        // create entity view #1 and assign to edge
        Device device = saveAndAssignDeviceToEdge();
        EntityView savedEntityView1 = saveEntityViewOnCloud("Edge Entity View 1", "Default", device.getId());
        cloudRestClient.assignEntityViewToEdge(edge.getId(), savedEntityView1.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView1.getId()).isPresent());

        // update entity view #1
        savedEntityView1.setName("Updated Edge Entity View 1");
        cloudRestClient.saveEntityView(savedEntityView1);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Updated Edge Entity View 1".equals(edgeRestClient.getEntityViewById(savedEntityView1.getId()).get().getName()));

        // unassign entity #1 view from edge
        cloudRestClient.unassignEntityViewFromEdge(edge.getId(), savedEntityView1.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView1.getId()).isEmpty());
        cloudRestClient.deleteEntityView(savedEntityView1.getId());

        // create entity view #2 and assign to edge
        EntityView savedEntityView2 = saveEntityViewOnCloud("Edge Entity View 2", "Default", device.getId());
        cloudRestClient.assignEntityViewToEdge(edge.getId(), savedEntityView2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView2.getId()).isPresent());

        // assign entity view #2 to customer
        Customer customer = new Customer();
        customer.setTitle("Entity View Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);
        cloudRestClient.assignEntityViewToCustomer(savedCustomer.getId(), savedEntityView2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> savedCustomer.getId().equals(edgeRestClient.getEntityViewById(savedEntityView2.getId()).get().getCustomerId()));

        // unassign entity view #2 from customer
        cloudRestClient.unassignEntityViewFromCustomer(savedEntityView2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getEntityViewById(savedEntityView2.getId()).get().getCustomerId().getId()));
        cloudRestClient.deleteCustomer(savedCustomer.getId());

        // delete entity view #2
        cloudRestClient.deleteEntityView(savedEntityView2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView2.getId()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
    }

    private EntityView saveEntityViewOnCloud(String entityViewName, String type, DeviceId deviceId) {
        EntityView entityView = new EntityView();
        entityView.setName(entityViewName);
        entityView.setType(type);
        entityView.setEntityId(deviceId);
        return cloudRestClient.saveEntityView(entityView);
    }

}

