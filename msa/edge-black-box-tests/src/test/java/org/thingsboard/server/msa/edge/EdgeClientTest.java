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
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class EdgeClientTest extends AbstractContainerTest {
    @Test
    public void testChangeOwner_fromTenantToCustomer_andFromCustomerToTenant() {
        // create customer
        Customer savedCustomer = saveCustomer("Edge Customer", null);

        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        // validate tenant groups on edge
        List<EntityGroupId> tenantEntityGroupIds = createEntitiesGroupAndAssignToEdge(edge.getTenantId());

        // change owner to customer
        cloudRestClient.changeOwnerToCustomer(savedCustomer.getId(), edge.getId());
        // validate that customer was created on edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isPresent());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // create device, asset, entity view, dashboard, user entity groups on customer level and assign to edge
        // validate customer groups on edge
        List<EntityGroupId> customerEntityGroupIds = createEntitiesGroupAndAssignToEdge(savedCustomer.getId());

        // change owner to tenant
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // validate that customer entity group were deleted from edge
        validateEntityGroupsAreRemovedFromEdge(customerEntityGroupIds);

        // validate that customer was deleted from edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isEmpty());

        // remove tenant entity groups
        for (EntityGroupId tenantEntityGroupId : tenantEntityGroupIds) {
            cloudRestClient.deleteEntityGroup(tenantEntityGroupId);
        }

        // validate no tenant groups on edge
        validateEntityGroupsAreRemovedFromEdge(tenantEntityGroupIds);

        // remove customer
        cloudRestClient.deleteCustomer(savedCustomer.getId());
    }

    @Test
    public void testChangeOwner_fromTenantToSubCustomer_andFromSubCustomerToTenant() {
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());

        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        // validate tenant groups on edge
        List<EntityGroupId> tenantEntityGroupIds = createEntitiesGroupAndAssignToEdge(edge.getTenantId());

        // change owner from tenant to child customer
        cloudRestClient.changeOwnerToCustomer(savedSubCustomerA.getId(), edge.getId());

        // validate that customer and sub customer were created on edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerA.getId()).isPresent());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedSubCustomerA.getId()).isPresent());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // create device, asset, entity view, dashboard, user entity groups on customer level and assign to edge
        // validate customer groups on edge
        List<EntityGroupId> customerAEntityGroupIds = createEntitiesGroupAndAssignToEdge(savedCustomerA.getId());

        // create device, asset, entity view, dashboard, user entity groups on sub customer level and assign to edge
        // validate sub customer groups on edge
        List<EntityGroupId> subCustomerAEntityGroupIds = createEntitiesGroupAndAssignToEdge(savedSubCustomerA.getId());

        // change owner to tenant
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());

        // validate that customer and sub customer entity groups were deleted from edge
        validateEntityGroupsAreRemovedFromEdge(customerAEntityGroupIds);
        validateEntityGroupsAreRemovedFromEdge(subCustomerAEntityGroupIds);

        // validate that customer and sub customer were unassigned from edge
        validateEntityGroupsAreUnassignedFromEdge(customerAEntityGroupIds);
        validateEntityGroupsAreUnassignedFromEdge(subCustomerAEntityGroupIds);

        // remove tenant entity groups
        for (EntityGroupId tenantEntityGroupId : tenantEntityGroupIds) {
            cloudRestClient.deleteEntityGroup(tenantEntityGroupId);
        }
        // validate no tenant groups on edge
        validateEntityGroupsAreRemovedFromEdge(tenantEntityGroupIds);

        // remove sub customer
        // remove customer
        cloudRestClient.deleteCustomer(savedCustomerA.getId());
    }

    @Test
    public void testChangeOwner_fromCustomerToSubCustomer_andFromSubCustomerToCustomer() {
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());

        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        List<EntityGroupId> tenantEntityGroupIds = createEntitiesGroupAndAssignToEdge(edge.getTenantId());

        // change owner from tenant to parent customer
        cloudRestClient.changeOwnerToCustomer(savedCustomerA.getId(), edge.getId());

        // validate that customer was created on edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerA.getId()).isPresent());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // create device, asset, entity view, dashboard, user entity groups on customer level and assign to edge
        // validate customer groups on edge
        List<EntityGroupId> customerAEntityGroupIds = createEntitiesGroupAndAssignToEdge(savedCustomerA.getId());

        // change owner to child customer
        cloudRestClient.changeOwnerToCustomer(savedSubCustomerA.getId(), edge.getId());

        // validate that sub customer was created on edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedSubCustomerA.getId()).isPresent());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // validate that customer groups are still on edge
        validateEntityGroupsAreStillOnEdge(customerAEntityGroupIds);

        // create device, asset, entity view, dashboard, user entity groups on sub customer level and assign to edge
        // validate sub customer groups on edge
        List<EntityGroupId> subCustomerAEntityGroupIds = createEntitiesGroupAndAssignToEdge(savedSubCustomerA.getId());

        // change owner to parent customer
        cloudRestClient.changeOwnerToCustomer(savedCustomerA.getId(), edge.getId());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // validate that customer groups are still on edge
        validateEntityGroupsAreStillOnEdge(customerAEntityGroupIds);

        // validate that sub customer was deleted from edge
        validateEntityGroupsAreRemovedFromEdge(subCustomerAEntityGroupIds);

        // validate that sub customer entity groups were unassigned from edge
        validateEntityGroupsAreUnassignedFromEdge(subCustomerAEntityGroupIds);

        // validate that sub customer was removed from edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedSubCustomerA.getId()).isEmpty());

        // change owner to tenant
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());

        // validate that customer was deleted from edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerA.getId()).isEmpty());

        // validate that customer entity group were deleted from edge
        validateEntityGroupsAreRemovedFromEdge(customerAEntityGroupIds);

        // validate that customer entity groups were unassigned from edge
        validateEntityGroupsAreUnassignedFromEdge(customerAEntityGroupIds);

        // remove tenant entity groups
        for (EntityGroupId tenantEntityGroupId : tenantEntityGroupIds) {
            cloudRestClient.deleteEntityGroup(tenantEntityGroupId);
        }

        // validate no tenant groups on edge
        validateEntityGroupsAreRemovedFromEdge(tenantEntityGroupIds);

        // remove sub sub customer
        // remove sub customer
        // remove customer
        cloudRestClient.deleteCustomer(savedCustomerA.getId());
    }

    @Test
    public void testChangeOwner_fromSubCustomerToSubSubCustomer_andFromSubSubCustomerToSubCustomer() {
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);
        // create sub customer A
        Customer savedSubCustomerA = saveCustomer("Edge Sub Customer A", savedCustomerA.getId());
        // create sub sub customer A
        Customer savedSubSubCustomerA = saveCustomer("Edge Sub Sub Customer A", savedSubCustomerA.getId());

        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        List<EntityGroupId> tenantEntityGroupIds = createEntitiesGroupAndAssignToEdge(edge.getTenantId());

        // change owner from tenant to parent customer
        cloudRestClient.changeOwnerToCustomer(savedSubCustomerA.getId(), edge.getId());

        // validate that customer was created on edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerA.getId()).isPresent());

        // validate that sub customer was created on edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedSubCustomerA.getId()).isPresent());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // create device, asset, entity view, dashboard, user entity groups on customer level and assign to edge
        // validate customer groups on edge
        List<EntityGroupId> customerAEntityGroupIds = createEntitiesGroupAndAssignToEdge(savedCustomerA.getId());

        // create device, asset, entity view, dashboard, user entity groups on sub customer level and assign to edge
        // validate sub customer groups on edge
        List<EntityGroupId> subCustomerAEntityGroupIds = createEntitiesGroupAndAssignToEdge(savedSubCustomerA.getId());

        // change owner to child customer
        cloudRestClient.changeOwnerToCustomer(savedSubSubCustomerA.getId(), edge.getId());

        // validate that sub customer was created on edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedSubSubCustomerA.getId()).isPresent());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // validate that customer groups are still on edge
        validateEntityGroupsAreStillOnEdge(customerAEntityGroupIds);

        // validate that sub customer groups are still on edge
        validateEntityGroupsAreStillOnEdge(subCustomerAEntityGroupIds);

        // create device, asset, entity view, dashboard, user entity groups on sub customer level and assign to edge
        // validate sub customer groups on edge
        List<EntityGroupId> subSubCustomerAEntityGroupIds = createEntitiesGroupAndAssignToEdge(savedSubSubCustomerA.getId());

        // change owner to parent customer
        cloudRestClient.changeOwnerToCustomer(savedSubCustomerA.getId(), edge.getId());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // validate that customer groups are still on edge
        validateEntityGroupsAreStillOnEdge(customerAEntityGroupIds);

        // validate that sub customer groups are still on edge
        validateEntityGroupsAreStillOnEdge(subCustomerAEntityGroupIds);

        // validate that sub sub customer was deleted from edge
        validateEntityGroupsAreRemovedFromEdge(subSubCustomerAEntityGroupIds);

        // validate that sub sub customer entity groups were unassigned from edge
        validateEntityGroupsAreUnassignedFromEdge(subSubCustomerAEntityGroupIds);

        // validate that sub sub customer was removed from edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedSubSubCustomerA.getId()).isEmpty());

        // change owner to tenant
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());

        // validate that customer was deleted from edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerA.getId()).isEmpty());

        // validate that sub customer was deleted from edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedSubCustomerA.getId()).isEmpty());

        // validate that customer entity group were deleted from edge
        validateEntityGroupsAreRemovedFromEdge(customerAEntityGroupIds);

        // validate that sub customer entity group were deleted from edge
        validateEntityGroupsAreRemovedFromEdge(subCustomerAEntityGroupIds);

        // validate that customer entity groups were unassigned from edge
        validateEntityGroupsAreUnassignedFromEdge(customerAEntityGroupIds);

        // validate that sub customer entity groups were unassigned from edge
        validateEntityGroupsAreUnassignedFromEdge(subCustomerAEntityGroupIds);

        // remove tenant entity groups
        for (EntityGroupId tenantEntityGroupId : tenantEntityGroupIds) {
            cloudRestClient.deleteEntityGroup(tenantEntityGroupId);
        }

        // validate no tenant groups on edge
        validateEntityGroupsAreRemovedFromEdge(tenantEntityGroupIds);

        // remove sub sub customer
        // remove sub customer
        // remove customer
        cloudRestClient.deleteCustomer(savedCustomerA.getId());
    }

    @Test
    public void testChangeOwner_fromSubCustomerAToCustomerB_andFromCustomerBToCustomerA() {
        // create customer A
        Customer savedCustomerA = saveCustomer("Edge Customer A", null);

        // create customer B
        Customer savedCustomerB = saveCustomer("Edge Customer B", null);

        // create device, asset, entity view, dashboard, user entity groups on tenant level and assign to edge
        // validate tenant groups on edge
        List<EntityGroupId> tenantEntityGroupIds = createEntitiesGroupAndAssignToEdge(edge.getTenantId());

        // change owner to customer A
        cloudRestClient.changeOwnerToCustomer(savedCustomerA.getId(), edge.getId());

        // validate that customer A was created on edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerA.getId()).isPresent());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // create device, asset, entity view, dashboard, user entity groups on customer A level and assign to edge
        // validate customer A groups on edge
        List<EntityGroupId> customerAEntityGroupIds = createEntitiesGroupAndAssignToEdge(savedCustomerA.getId());

        // change owner to customer B
        cloudRestClient.changeOwnerToCustomer(savedCustomerB.getId(), edge.getId());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // validate that customer A entity group were deleted from edge
        validateEntityGroupsAreRemovedFromEdge(customerAEntityGroupIds);

        // validate that customer A entity groups were unassigned from edge
        validateEntityGroupsAreUnassignedFromEdge(customerAEntityGroupIds);

        // validate that customer A was deleted from edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerA.getId()).isEmpty());

        // validate that customer B was created on edge
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerB.getId()).isPresent());

        // create device, asset, entity view, dashboard, user entity groups on customer B level and assign to edge
        // validate customer B groups on edge
        List<EntityGroupId> customerBEntityGroupIds = createEntitiesGroupAndAssignToEdge(savedCustomerB.getId());

        // change owner to tenant
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());

        // validate that tenant groups are still on edge
        validateEntityGroupsAreStillOnEdge(tenantEntityGroupIds);

        // validate that customer B was deleted from edge
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomerB.getId()).isEmpty());

        // validate that customer B entity group were deleted from edge
        validateEntityGroupsAreRemovedFromEdge(customerBEntityGroupIds);

        // validate that customer B entity groups were unassigned from edge
        validateEntityGroupsAreUnassignedFromEdge(customerBEntityGroupIds);

        // remove tenant entity groups
        for (EntityGroupId tenantEntityGroupId : tenantEntityGroupIds) {
            cloudRestClient.deleteEntityGroup(tenantEntityGroupId);
        }

        // validate no tenant groups on edge
        validateEntityGroupsAreRemovedFromEdge(tenantEntityGroupIds);

        // remove customer A
        cloudRestClient.deleteCustomer(savedCustomerA.getId());

        // remove customer B
        cloudRestClient.deleteCustomer(savedCustomerB.getId());
    }

    private List<EntityGroupId> createEntitiesGroupAndAssignToEdge(EntityId ownerId) {
        List<EntityGroupId> result = new ArrayList<>();
        EntityGroup deviceEntityGroup = createEntityGroup(EntityType.DEVICE, ownerId);
        assignEntityGroupToEdge(deviceEntityGroup);
        EntityGroup assetEntityGroup = createEntityGroup(EntityType.ASSET, ownerId);
        assignEntityGroupToEdge(assetEntityGroup);
        EntityGroup entityViewEntityGroup = createEntityGroup(EntityType.ENTITY_VIEW, ownerId);
        assignEntityGroupToEdge(entityViewEntityGroup);
        EntityGroup dashboardEntityGroup = createEntityGroup(EntityType.DASHBOARD, ownerId);
        assignEntityGroupToEdge(dashboardEntityGroup);
        EntityGroup userEntityGroup = createEntityGroup(EntityType.USER, ownerId);
        assignEntityGroupToEdge(userEntityGroup);

        result.add(deviceEntityGroup.getId());
        result.add(assetEntityGroup.getId());
        result.add(entityViewEntityGroup.getId());
        result.add(dashboardEntityGroup.getId());
        result.add(userEntityGroup.getId());

        return result;
    }

    private void validateEntityGroupsAreStillOnEdge(List<EntityGroupId> entityGroupIds) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    boolean result = true;
                    for (EntityGroupId entityGroupId : entityGroupIds) {
                        result &= edgeRestClient.getEntityGroupById(entityGroupId).isPresent();
                    }
                    return result;
                });
    }

    private void validateEntityGroupsAreRemovedFromEdge(List<EntityGroupId> entityGroupIds) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    boolean result = true;
                    for (EntityGroupId entityGroupId : entityGroupIds) {
                        result &= edgeRestClient.getEntityGroupById(entityGroupId).isEmpty();
                    }
                    return result;
                });
    }

    private void validateEntityGroupsAreUnassignedFromEdge(List<EntityGroupId> entityGroupIds) {
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> edgeEntityGroupsId = new ArrayList<>();
                    List<EntityGroupInfo> deviceEdgeGroups = cloudRestClient.getAllEdgeEntityGroups(edge.getId(), EntityType.DEVICE);
                    edgeEntityGroupsId.addAll(deviceEdgeGroups.stream().map(EntityGroup::getId).collect(Collectors.toList()));
                    List<EntityGroupInfo> assetEdgeGroups = cloudRestClient.getAllEdgeEntityGroups(edge.getId(), EntityType.ASSET);
                    edgeEntityGroupsId.addAll(assetEdgeGroups.stream().map(EntityGroup::getId).collect(Collectors.toList()));
                    List<EntityGroupInfo> entityViewEdgeGroups = cloudRestClient.getAllEdgeEntityGroups(edge.getId(), EntityType.ENTITY_VIEW);
                    edgeEntityGroupsId.addAll(entityViewEdgeGroups.stream().map(EntityGroup::getId).collect(Collectors.toList()));
                    List<EntityGroupInfo> dashboardEdgeGroups = cloudRestClient.getAllEdgeEntityGroups(edge.getId(), EntityType.DASHBOARD);
                    edgeEntityGroupsId.addAll(dashboardEdgeGroups.stream().map(EntityGroup::getId).collect(Collectors.toList()));
                    List<EntityGroupInfo> userEdgeGroups = cloudRestClient.getAllEdgeEntityGroups(edge.getId(), EntityType.USER);
                    edgeEntityGroupsId.addAll(userEdgeGroups.stream().map(EntityGroup::getId).collect(Collectors.toList()));
                    boolean result = true;
                    for (EntityGroupId entityGroupId : entityGroupIds) {
                        result &= !edgeEntityGroupsId.contains(entityGroupId);
                    }
                    return result;
                });
    }

}

