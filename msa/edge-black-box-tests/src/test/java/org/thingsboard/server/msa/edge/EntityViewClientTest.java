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
package org.thingsboard.server.msa.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EntityViewClientTest extends AbstractContainerTest {

    @Test
    public void testEntityViews() throws Exception {
        verifyEntityGroups(EntityType.ENTITY_VIEW, 1);

        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));

        EntityGroup entityViewEntityGroup = new EntityGroup();
        entityViewEntityGroup.setType(EntityType.ENTITY_VIEW);
        entityViewEntityGroup.setName("EntityViewGroup");
        EntityGroupInfo savedEntityViewEntityGroup = cloudRestClient.saveEntityGroup(entityViewEntityGroup);
        EntityView savedEntityViewOnCloud = saveEntityViewOnCloud("Edge Entity View 1", "Default", device.getId(), savedEntityViewEntityGroup.getId());

        cloudRestClient.assignEntityGroupToEdge(edge.getId(), savedEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);

        verifyEntityGroups(EntityType.ENTITY_VIEW, 2);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isPresent());

        JsonNode entityViewAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"entityViewKey\":\"entityViewValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedEntityViewOnCloud.getId(), DataConstants.SERVER_SCOPE, entityViewAttributes);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> verifyAttributeOnEdge(savedEntityViewOnCloud.getId(),
                        DataConstants.SERVER_SCOPE, "entityViewKey", "entityViewValue"));

        cloudRestClient.unassignEntityGroupFromEdge(edge.getId(), savedEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);

        verifyEntityGroups(EntityType.ENTITY_VIEW, 1);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isEmpty());

        cloudRestClient.deleteEntityView(savedEntityViewOnCloud.getId());

        // TODO: @voba - compare
//
//        // create entity view and assign to edge
//        Device device = saveAndAssignDeviceToEdge();
//        EntityView savedEntityViewOnCloud = saveEntityViewOnCloud("Edge Entity View 1", "Default", device.getId());
//        cloudRestClient.assignEntityViewToEdge(edge.getId(), savedEntityViewOnCloud.getId());
//        Awaitility.await()
//                .atMost(30, TimeUnit.SECONDS)
//                .until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isPresent());
//
//        // update entity view
//        savedEntityViewOnCloud.setName("Updated Edge Entity View 1");
//        cloudRestClient.saveEntityView(savedEntityViewOnCloud);
//        Awaitility.await()
//                .atMost(30, TimeUnit.SECONDS)
//                .until(() -> "Updated Edge Entity View 1".equals(edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).get().getName()));
//
//        // assign entity view to customer
//        Customer customer = new Customer();
//        customer.setTitle("Entity View Test Customer");
//        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
//        cloudRestClient.assignEntityViewToCustomer(savedCustomer.getId(), savedEntityViewOnCloud.getId());
//        Awaitility.await()
//                .atMost(30, TimeUnit.SECONDS)
//                .until(() -> savedCustomer.getId().equals(edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).get().getCustomerId()));
//
//        // unassign entity view from customer
//        cloudRestClient.unassignEntityViewFromCustomer(savedEntityViewOnCloud.getId());
//        Awaitility.await()
//                .atMost(30, TimeUnit.SECONDS)
//                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).get().getCustomerId().getId()));
//        cloudRestClient.deleteCustomer(savedCustomer.getId());
//
//        // unassign entity view from edge
//        cloudRestClient.unassignEntityViewFromEdge(edge.getId(), savedEntityViewOnCloud.getId());
//        Awaitility.await()
//                .atMost(30, TimeUnit.SECONDS)
//                .until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isEmpty());
//        cloudRestClient.deleteEntityView(savedEntityViewOnCloud.getId());
    }

    private EntityView saveEntityViewOnCloud(String entityViewName, String type, DeviceId deviceId, EntityGroupId entityGroupId) {
        EntityView entityView = new EntityView();
        entityView.setName(entityViewName);
        entityView.setType(type);
        entityView.setEntityId(deviceId);
        EntityView savedEntityView = cloudRestClient.saveEntityView(entityView);
        if (entityGroupId != null) {
            cloudRestClient.addEntitiesToEntityGroup(entityGroupId, Arrays.asList(savedEntityView.getId()));
        }
        return savedEntityView;
    }

}

