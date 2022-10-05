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
package org.thingsboard.server.edge;

import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract public class BaseEdgeTest extends AbstractEdgeTest {

    @Test
    public void testChangeOwner_fromTenantToCustomer_andFromCustomerToTenant() throws Exception {
        // create customer
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer
        Customer savedSubSubCustomerA = saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());

        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        EntityGroup deviceEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "EdgeDeviceGroup");
        EntityGroup assetEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ASSET, "EdgeAssetGroup");
        EntityGroup entityViewEntityGroup = createEntityGroupAndAssignToEdge(EntityType.ENTITY_VIEW, "EdgeEntityViewGroup");
        EntityGroup dashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "EdgeDashboardGroup");
        EntityGroup userEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "EdgeUserGroup");

        // change edge owner from tenant to customer A
        changeEdgeOwnerFromTenantToCustomer(savedCustomerA);

        // create device, asset, entity view, dashboard, user entity groups on customer level and assign to edge
        // validate customer groups on edge

        // change owner to tenant
        changeEdgeOwnerFromCustomerToTenant(savedCustomerA);

        // remove tenant entity groups
        // validate no tenant groups on edge

        // delete customers
        doDelete("/api/customer/" + savedCustomerA.getUuidId())
                .andExpect(status().isOk());
    }

    @Test
    public void testChangeOwner_fromTenantToSubCustomer_andFromSubCustomerToTenant() {
        // create customer
        // create sub customer
        // create sub sub customer
        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        // validate tenant groups on edge
        // change owner from tenant to child customer
        // validate that customer and sub customer were created on edge
        // validate that tenant groups are still on edge
        // create device, asset, entity view, dashboard, user entity groups on customer level and assign to edge
        // create device, asset, entity view, dashboard, user entity groups on sub customer level and assign to edge
        // validate customer groups on edge
        // validate sub customer groups on edge
        // change owner to tenant
        // validate that customer and sub customer entity groups were deleted from edge
        // validate that customer and sub customer were deleted from edge
        // remove tenant entity groups
        // validate no tenant groups on edge
        // remove sub sub customer
        // remove sub customer
        // remove customer
    }

    @Test
    public void testChangeOwner_fromCustomerToSubSubCustomer_andFromSubSubCustomerToCustomer() {
        // create customer
        // create sub customer
        // create sub sub customer
        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        // change owner from tenant to parent customer
        // validate that customer was created on edge
        // validate that tenant groups are still on edge
        // create device, asset, entity view, dashboard, user entity groups on customer level and assign to edge
        // validate customer groups on edge
        // change owner to child customer
        // validate that sub customer was created on edge
        // validate that tenant groups are still on edge
        // validate that customer groups are still on edge
        // create device, asset, entity view, dashboard, user entity groups on sub customer level and assign to edge
        // validate sub customer groups on edge
        // change owner to parent customer
        // validate that tenant groups are still on edge
        // validate that customer groups are still on edge
        // validate that sub customer was deleted from edge
        // validate that sub customer entity groups were deleted from edge
        // change owner to tenant
        // validate that customer was deleted from edge
        // validate that customer entity group were deleted from edge
        // remove tenant entity groups
        // validate no tenant groups on edge
        // remove sub sub customer
        // remove sub customer
        // remove customer
    }

    @Test
    public void testChangeOwner_fromSubCustomerAToCustomerB() {
    }


}
