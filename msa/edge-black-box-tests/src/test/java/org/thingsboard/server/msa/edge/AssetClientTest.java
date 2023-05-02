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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class AssetClientTest extends AbstractContainerTest {

    @Test
    public void testAssets() throws Exception {
        // create asset #1 and assign to edge
        Asset savedAsset1 = saveAndAssignAssetToEdge("Building");
        cloudRestClient.assignAssetToEdge(edge.getId(), savedAsset1.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetById(savedAsset1.getId()).isPresent());

        // update asset #1
        savedAsset1.setName("Updated Asset Name");
        cloudRestClient.saveAsset(savedAsset1);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Updated Asset Name".equals(edgeRestClient.getAssetById(savedAsset1.getId()).get().getName()));

        // save asset attribute
        JsonNode assetAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"assetKey\":\"assetValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedAsset1.getId(), DataConstants.SERVER_SCOPE, assetAttributes);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnEdge(savedAsset1.getId(),
                        DataConstants.SERVER_SCOPE, "assetKey", "assetValue"));

        // unassign asset #1 from edge
        cloudRestClient.unassignAssetFromEdge(edge.getId(), savedAsset1.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetById(savedAsset1.getId()).isEmpty());
        cloudRestClient.deleteAsset(savedAsset1.getId());

        // create asset #2 and assign to edge
        Asset savedAsset2 = saveAndAssignAssetToEdge("Building");
        cloudRestClient.assignAssetToEdge(edge.getId(), savedAsset2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetById(savedAsset2.getId()).isPresent());

        // assign asset #2 to customer
        Customer customer = new Customer();
        customer.setTitle("Asset Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);
        cloudRestClient.assignAssetToCustomer(savedCustomer.getId(), savedAsset2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> savedCustomer.getId().equals(edgeRestClient.getAssetById(savedAsset2.getId()).get().getCustomerId()));

        // unassign asset #2 from customer
        cloudRestClient.unassignAssetFromCustomer(savedAsset2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getAssetById(savedAsset2.getId()).get().getCustomerId().getId()));
        cloudRestClient.deleteCustomer(savedCustomer.getId());

        // delete asset #2
        cloudRestClient.deleteAsset(savedAsset2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetById(savedAsset2.getId()).isEmpty());

        // delete "Building" asset profile
        cloudRestClient.deleteAssetProfile(savedAsset1.getAssetProfileId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetProfileById(savedAsset1.getAssetProfileId()).isEmpty());
    }

}

