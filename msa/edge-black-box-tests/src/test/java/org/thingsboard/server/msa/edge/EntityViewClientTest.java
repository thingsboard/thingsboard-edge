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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EntityViewClientTest extends AbstractContainerTest {

    @Test
    public void testEntityViews() throws Exception {
        Device device = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));

        // create entity view #1, add to group #1 and assign group #1 to edge
        EntityGroup savedEntityViewEntityGroup1 = createEntityGroup(EntityType.ENTITY_VIEW);
        EntityView savedEntityView1 = saveEntityViewOnCloud("Edge Entity View 1", "Default", device.getId(), savedEntityViewEntityGroup1.getId());

        assignEntityGroupToEdge(savedEntityViewEntityGroup1);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedEntityViewEntityGroup1.getId()).isPresent());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView1.getId()).isPresent());

        // update entity view #1
        String updatedEntityViewName = savedEntityView1.getName() + "Updated";
        savedEntityView1.setName(updatedEntityViewName);
        cloudRestClient.saveEntityView(savedEntityView1);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> updatedEntityViewName.equals(cloudRestClient.getEntityViewById(savedEntityView1.getId()).get().getName())
                        && updatedEntityViewName.equals(edgeRestClient.getEntityViewById(savedEntityView1.getId()).get().getName()));

        // save entity view #1 attribute
        JsonNode entityViewAttributes = JacksonUtil.toJsonNode("{\"entityViewKey\":\"entityViewValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedEntityView1.getId(), DataConstants.SERVER_SCOPE, entityViewAttributes);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS).
                until(() -> verifyAttributeOnEdge(savedEntityView1.getId(),
                        DataConstants.SERVER_SCOPE, "entityViewKey", "entityViewValue"));

        // create entity view #2 inside group #1
        EntityView savedEntityView2 = saveEntityViewOnCloud("Edge Entity View 2", "Default", device.getId(), savedEntityViewEntityGroup1.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView2.getId()).isPresent());

        // add group #2 and assign to edge
        EntityGroup savedEntityViewEntityGroup2 = createEntityGroup(EntityType.ENTITY_VIEW);
        assignEntityGroupToEdge(savedEntityViewEntityGroup2);

        // add entity view #2 to group #2
        cloudRestClient.addEntitiesToEntityGroup(savedEntityViewEntityGroup2.getId(), Collections.singletonList(savedEntityView2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> entityView2Groups = edgeRestClient.getEntityGroupsForEntity(savedEntityView2.getId());
                    return entityView2Groups.contains(savedEntityViewEntityGroup2.getId());
                });

        // add entity view #2 to group #1
        cloudRestClient.addEntitiesToEntityGroup(savedEntityViewEntityGroup1.getId(), Collections.singletonList(savedEntityView2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> entityView2Groups = edgeRestClient.getEntityGroupsForEntity(savedEntityView2.getId());
                    return entityView2Groups.contains(savedEntityViewEntityGroup1.getId());
                });

        // remove entity view #2 from group #1
        cloudRestClient.removeEntitiesFromEntityGroup(savedEntityViewEntityGroup1.getId(), Collections.singletonList(savedEntityView2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> entityView2Groups = edgeRestClient.getEntityGroupsForEntity(savedEntityView2.getId());
                    return !entityView2Groups.contains(savedEntityViewEntityGroup1.getId()) && entityView2Groups.contains(savedEntityViewEntityGroup2.getId());
                });

        // remove entity view #2 from group #2
        cloudRestClient.removeEntitiesFromEntityGroup(savedEntityViewEntityGroup2.getId(), Collections.singletonList(savedEntityView2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityView> entityViews = edgeRestClient.getEntityViewsByEntityGroupId(savedEntityViewEntityGroup2.getId(), new PageLink(1000)).getData();
                    return !entityViews.contains(savedEntityView2);
                });

        // delete entity view #2
        cloudRestClient.deleteEntityView(savedEntityView2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView2.getId()).isEmpty());

        // unassign group #1 from edge
        cloudRestClient.unassignEntityGroupFromEdge(edge.getId(), savedEntityViewEntityGroup1.getId(), EntityType.ENTITY_VIEW);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedEntityViewEntityGroup1.getId()).isEmpty());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView1.getId()).isEmpty());

        // clean up
        cloudRestClient.deleteEntityView(savedEntityView1.getId());
        cloudRestClient.deleteEntityGroup(savedEntityViewEntityGroup1.getId());
        cloudRestClient.deleteEntityGroup(savedEntityViewEntityGroup2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityView1.getId()).isEmpty() &&
                        edgeRestClient.getEntityGroupById(savedEntityViewEntityGroup1.getId()).isEmpty() &&
                        edgeRestClient.getEntityGroupById(savedEntityViewEntityGroup2.getId()).isEmpty());
        // cleanup
        cloudRestClient.deleteDevice(device.getId());
    }

    private EntityView saveEntityViewOnCloud(String entityViewName, String type, DeviceId deviceId, EntityGroupId entityGroupId) {
        EntityView entityView = new EntityView();
        entityView.setName(entityViewName);
        entityView.setType(type);
        entityView.setEntityId(deviceId);
        return cloudRestClient.saveEntityView(entityView, entityGroupId);
    }

    @Test
    public void testSendEntityViewToCloud() {
        // create asset on edge
        EntityGroup savedAssetEntityGroup = createEntityGroup(EntityType.ASSET);
        assignEntityGroupToEdge(savedAssetEntityGroup);
        Asset savedAssetOnEdge = saveAssetOnEdge("Edge Asset For Entity View", edgeRestClient.getDefaultAssetProfileInfo().getName(), savedAssetEntityGroup.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getAssetById(savedAssetOnEdge.getId()).isPresent());

        // create entity view on edge
        EntityGroup savedEntityViewEntityGroup = createEntityGroup(EntityType.ASSET);
        assignEntityGroupToEdge(savedEntityViewEntityGroup);
        EntityView savedEntityViewOnEdge = saveEntityViewOnEdge("Edge Entity View 3", "Default", savedAssetOnEdge.getId(), savedEntityViewEntityGroup.getId());
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


        // delete entity view
        edgeRestClient.deleteEntityView(savedEntityViewOnEdge.getId());
        cloudRestClient.deleteEntityView(savedEntityViewOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).isEmpty() &&
                        cloudRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).isEmpty());
        cloudRestClient.deleteEntityGroup(savedEntityViewEntityGroup.getId());

        // cleanup
        cloudRestClient.deleteAsset(savedAssetOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getAssetById(savedAssetOnEdge.getId()).isEmpty());
        cloudRestClient.deleteEntityGroup(savedAssetEntityGroup.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedAssetEntityGroup.getId()).isEmpty()
                        && cloudRestClient.getEntityGroupById(savedAssetEntityGroup.getId()).isEmpty());
    }

    @Test
    public void testSendEntityViewToCloudWithNameThatAlreadyExistsOnCloud() {
        // create entity view on cloud and edge with the same name
        Device device = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));

        EntityGroup savedEntityViewEntityGroup = createEntityGroup(EntityType.ENTITY_VIEW);
        assignEntityGroupToEdge(savedEntityViewEntityGroup);
        EntityView savedEntityViewOnCloud = saveEntityViewOnCloud("Edge Entity View Exists", "Default", device.getId(), null);
        EntityView savedEntityViewOnEdge = saveEntityViewOnEdge("Edge Entity View Exists", "Default", device.getId(), savedEntityViewEntityGroup.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<EntityView> entityViewOptional = cloudRestClient.getEntityViewById(savedEntityViewOnEdge.getId());
                    return entityViewOptional.isPresent() && !entityViewOptional.get().getName().equals(savedEntityViewOnCloud.getName());
                });

        // delete entity view
        edgeRestClient.deleteEntityView(savedEntityViewOnEdge.getId());
        cloudRestClient.deleteEntityView(savedEntityViewOnEdge.getId());
        cloudRestClient.deleteEntityView(savedEntityViewOnCloud.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).isEmpty() &&
                        cloudRestClient.getEntityViewById(savedEntityViewOnEdge.getId()).isEmpty() &&
                        cloudRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isEmpty());
        cloudRestClient.deleteEntityGroup(savedEntityViewEntityGroup.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedEntityViewEntityGroup.getId()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceById(device.getId()).isEmpty());
    }

    private EntityView saveEntityViewOnEdge(String entityViewName, String type, EntityId entityId, EntityGroupId entityGroupId) {
        EntityView entityView = new EntityView();
        entityView.setName(entityViewName);
        entityView.setType(type);
        entityView.setEntityId(entityId);
        return edgeRestClient.saveEntityView(entityView, entityGroupId);
    }
}
