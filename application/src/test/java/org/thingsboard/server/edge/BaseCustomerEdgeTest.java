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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class BaseCustomerEdgeTest extends AbstractEdgeTest {

    @Test
    public void testCreateUpdateDeleteCustomer() throws Exception {
        edgeImitator.expectMessageAmount(1);
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        Customer savedSubSubCustomerA = saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());
        // create customer B
        Customer savedCustomerB = saveCustomer("Edge Customer B", null);

        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to sub customer A
        changeEdgeOwnerFromTenantToSubCustomer(savedCustomerA, savedSubCustomerA);

        // update customer and validate changes on edge
        updateCustomerAndValidateChangesOnEdge(savedCustomerA, "Edge Customer A Updated");

        // update sub customer and validate changes on edge
        updateCustomerAndValidateChangesOnEdge(savedSubCustomerA, "Edge Sub Customer A Updated");

        // update sub sub customer and validate NO changes on edge
        updateCustomerAndValidate_NO_ChangesOnEdge(savedSubSubCustomerA, "Edge Sub Sub Customer A Updated");

        // update customer B and validate NO changes on edge
        updateCustomerAndValidate_NO_ChangesOnEdge(savedCustomerB, "Edge Customer B Updated");

        // change edge owner from sub customer A to tenant
        changeEdgeOwnerFromSubCustomerToTenant(savedCustomerA, savedSubCustomerA);

        // delete customers
        doDelete("/api/customer/" + savedCustomerA.getUuidId())
                .andExpect(status().isOk());
        doDelete("/api/customer/" + savedCustomerB.getUuidId())
                .andExpect(status().isOk());
    }

    private void updateCustomerAndValidateChangesOnEdge(Customer customer, String updatedTitle) throws InterruptedException {
        edgeImitator.expectMessageAmount(1);
        customer.setTitle(updatedTitle);
        doPost("/api/customer", customer, Customer.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CustomerUpdateMsg);
        CustomerUpdateMsg customerUpdateMsg = (CustomerUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(customer.getUuidId().getMostSignificantBits(), customerUpdateMsg.getIdMSB());
        Assert.assertEquals(customer.getUuidId().getLeastSignificantBits(), customerUpdateMsg.getIdLSB());
        Assert.assertEquals(updatedTitle, customerUpdateMsg.getTitle());
    }

    private void updateCustomerAndValidate_NO_ChangesOnEdge(Customer customer, String updatedTitle) throws InterruptedException {
        edgeImitator.expectMessageAmount(1);
        customer.setTitle(updatedTitle);
        doPost("/api/customer", customer, Customer.class);
        Assert.assertFalse(edgeImitator.waitForMessages(1));
    }

    @Test
    public void testChangeOwnerOfCustomer_validateChangesToEdgeEntityGroups() {

    }

    @Test
    public void testChangeOwnerOfCustomerFromTenantToCustomer() {

    }

}
