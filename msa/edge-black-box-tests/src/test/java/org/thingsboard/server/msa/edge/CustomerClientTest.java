/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CustomerClientTest extends AbstractContainerTest {

    @Test
    public void testCreateUpdateDeleteCustomer() throws Exception {
        testPublicCustomerCreatedOnEdge(edge.getTenantId());

        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        Customer savedSubSubCustomerA = saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());
        // create customer B
        Customer savedCustomerB = saveCustomer("Edge Customer B", null);

        // change owner to sub customer A
        cloudRestClient.changeOwnerToCustomer(savedSubCustomerA.getId(), edge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerA.getId()).isPresent());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedSubCustomerA.getId()).isPresent());

        cloudRestClient.syncEdge(edge.getId());
        testPublicCustomerCreatedOnEdge(savedSubCustomerA.getId());

        // update customer A
        savedCustomerA.setTitle("Edge Customer A Updated");
        cloudRestClient.saveCustomer(savedCustomerA);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Edge Customer A Updated".equals(edgeRestClient.getCustomerById(savedCustomerA.getId()).get().getTitle()));

        // update sub customer A
        savedSubCustomerA.setTitle("Edge Sub Customer A Updated");
        cloudRestClient.saveCustomer(savedSubCustomerA);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Edge Sub Customer A Updated".equals(edgeRestClient.getCustomerById(savedSubCustomerA.getId()).get().getTitle()));

        // make sure that all group permissions request are processed
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));

        // change owner to tenant
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerA.getId()).isEmpty());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedSubCustomerA.getId()).isEmpty());

        // delete customers
        cloudRestClient.deleteCustomer(savedCustomerA.getId());
        cloudRestClient.deleteCustomer(savedCustomerB.getId());
    }

    private void testPublicCustomerCreatedOnEdge(EntityId ownerId) {
        Customer publicCustomer = findPublicCustomer(ownerId);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(publicCustomer.getId()).isPresent());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupInfo> cloudPublicCustomerUserGroups = cloudRestClient.getEntityGroupsByOwnerAndType(ownerId, EntityType.USER);
                    List<EntityGroupInfo> edgePublicCustomerUserGroups = edgeRestClient.getEntityGroupsByOwnerAndType(ownerId, EntityType.USER);
                    return cloudPublicCustomerUserGroups.size() == edgePublicCustomerUserGroups.size();
                });
    }
}

