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
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.msa.AbstractContainerTest;

@Slf4j
public class EdgeClientTest extends AbstractContainerTest {

    @Test
    public void testEdge_assignToCustomer_unassignFromCustomer() {
        performTestOnEachEdge(this::_testEdge_assignToCustomer_unassignFromCustomer);
    }

    private void _testEdge_assignToCustomer_unassignFromCustomer() {
        // assign edge to customer
        Customer customer = new Customer();
        customer.setTitle("Edge Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);

        // assign edge to customer
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);

        // unassign edge from customer
        unassignEdgeFromCustomerAndValidateUnassignmentOnCloud();

        // cleanup
        cloudRestClient.deleteCustomer(savedCustomer.getId());
    }

}
