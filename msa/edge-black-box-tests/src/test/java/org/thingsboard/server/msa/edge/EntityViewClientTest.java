/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Optional;
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
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView1.getId()).isPresent());

        // update entity view #1
        savedEntityView1.setName("Updated Edge Entity View 1");
        cloudRestClient.saveEntityView(savedEntityView1);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Updated Edge Entity View 1".equals(edgeRestClient.getEntityViewById(savedEntityView1.getId()).get().getName()));

        // unassign entity #1 view from edge
        cloudRestClient.unassignEntityViewFromEdge(edge.getId(), savedEntityView1.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView1.getId()).isEmpty());
        cloudRestClient.deleteEntityView(savedEntityView1.getId());

        // create entity view #2 and assign to edge
        EntityView savedEntityView2 = saveEntityViewOnCloud("Edge Entity View 2", "Default", device.getId());
        cloudRestClient.assignEntityViewToEdge(edge.getId(), savedEntityView2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView2.getId()).isPresent());

        // assign entity view #2 to customer
        Customer customer = new Customer();
        customer.setTitle("Entity View Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);
        cloudRestClient.assignEntityViewToCustomer(savedCustomer.getId(), savedEntityView2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> savedCustomer.getId().equals(edgeRestClient.getEntityViewById(savedEntityView2.getId()).get().getCustomerId()));

        // unassign entity view #2 from customer
        cloudRestClient.unassignEntityViewFromCustomer(savedEntityView2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getEntityViewById(savedEntityView2.getId()).get().getCustomerId().getId()));
        cloudRestClient.deleteCustomer(savedCustomer.getId());

        // delete entity view #2
        cloudRestClient.deleteEntityView(savedEntityView2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView2.getId()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
    }

    @Test
    public void testSendEntityViewToCloud() {
        // create asset on edge
        Asset savedAssetOnEdge = saveAssetOnEdge("Edge Asset For Entity View", edgeRestClient.getDefaultAssetProfileInfo().getName());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getAssetById(savedAssetOnEdge.getId()).isPresent());

        // create entity view on edge
        EntityView savedEntityViewOnEdge = saveEntityViewOnEdge("Edge Entity View 3", "Default", savedAssetOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).isPresent());

        // update entity view
        savedEntityViewOnEdge.setName("Edge Entity View 3 Updated");
        edgeRestClient.saveEntityView(savedEntityViewOnEdge);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Edge Entity View 3 Updated".equals(cloudRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).get().getName()));

        // assign entity view to customer
        Customer customer = new Customer();
        customer.setTitle("Edge Entity View 3 Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);
        edgeRestClient.assignEntityViewToCustomer(savedCustomer.getId(), savedEntityViewOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> savedCustomer.getId().equals(cloudRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).get().getCustomerId()));

        // unassign entity view from customer
        edgeRestClient.unassignEntityViewFromCustomer(savedEntityViewOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(cloudRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).get().getCustomerId().getId()));
        cloudRestClient.deleteCustomer(savedCustomer.getId());

        // delete entity view
        edgeRestClient.deleteEntityView(savedEntityViewOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EntityView> edgeEntityViews = cloudRestClient.getEdgeEntityViews(edge.getId(), new PageLink(1000));
                    long count = edgeEntityViews.getData().stream().filter(d -> savedEntityViewOnEdge.getId().equals(d.getId())).count();
                    return count == 0;
                });

        cloudRestClient.deleteEntityView(savedEntityViewOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).isEmpty());

        // cleanup
        cloudRestClient.deleteAsset(savedAssetOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getAssetById(savedAssetOnEdge.getId()).isEmpty());
    }

    @Test
    public void testSendEntityViewToCloudWithNameThatAlreadyExistsOnCloud() {
        // create entity view on cloud and edge with the same name
        Device device = saveAndAssignDeviceToEdge();
        EntityView savedEntityViewOnCloud = saveEntityViewOnCloud("Edge Entity View Exists", "Default", device.getId());
        EntityView savedEntityViewOnEdge = saveEntityViewOnEdge("Edge Entity View Exists", "Default", device.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<EntityView> entityViewOptional = cloudRestClient.getEntityViewById(savedEntityViewOnEdge.getId());
                    return entityViewOptional.isPresent() && !entityViewOptional.get().getName().equals(savedEntityViewOnCloud.getName());
                });

        // delete entity view
        cloudRestClient.deleteEntityView(savedEntityViewOnEdge.getId());
        cloudRestClient.deleteEntityView(savedEntityViewOnCloud.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).isEmpty() &&
                        cloudRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).isEmpty() &&
                        cloudRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceById(device.getId()).isEmpty());
    }

    private EntityView saveEntityViewOnEdge(String entityViewName, String type, EntityId entityId) {
        return saveEntityView(entityViewName, type, entityId, edgeRestClient);
    }

    private EntityView saveEntityViewOnCloud(String entityViewName, String type, EntityId entityId) {
        return saveEntityView(entityViewName, type, entityId, cloudRestClient);
    }

    private EntityView saveEntityView(String entityViewName, String type, EntityId entityId, RestClient restClient) {
        EntityView entityView = new EntityView();
        entityView.setName(entityViewName);
        entityView.setType(type);
        entityView.setEntityId(entityId);
        return restClient.saveEntityView(entityView);
    }

}
