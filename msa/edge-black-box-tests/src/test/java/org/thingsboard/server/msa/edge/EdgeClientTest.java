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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class EdgeClientTest extends AbstractContainerTest {

    @Test
    public void testEdge_assignToCustomer_unassignFromCustomer() throws Exception {
        // assign edge to customer
        Customer customer = new Customer();
        customer.setTitle("Edge Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        cloudRestClient.assignEdgeToCustomer(savedCustomer.getId(), edge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> savedCustomer.getId().equals(edgeRestClient.getEdgeById(edge.getId()).get().getCustomerId()));

        // sleep 10 to make sure that sync process is completed
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        // unassign edge from customer
        cloudRestClient.unassignEdgeFromCustomer(edge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getEdgeById(edge.getId()).get().getCustomerId().getId()));
    }

}

