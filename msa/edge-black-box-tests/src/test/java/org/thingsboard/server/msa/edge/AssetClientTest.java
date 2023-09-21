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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AssetClientTest extends AbstractContainerTest {

    @Test
    public void testAssets() throws Exception {
        // create asset #1, add to group #1 and assign group #1 to edge
        EntityGroup savedAssetEntityGroup1 = createEntityGroup(EntityType.ASSET);
        Asset savedAsset1 = saveAssetAndAssignEntityGroupToEdge("Building", savedAssetEntityGroup1);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedAssetEntityGroup1.getId()).isPresent());

        // update asset #1
        String updatedAssetName = savedAsset1.getName() + "Updated";
        savedAsset1.setName(updatedAssetName);
        cloudRestClient.saveAsset(savedAsset1);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> updatedAssetName.equals(cloudRestClient.getAssetById(savedAsset1.getId()).get().getName())
                        && updatedAssetName.equals(edgeRestClient.getAssetById(savedAsset1.getId()).get().getName()));

        // save asset #1 attribute
        JsonNode assetAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"assetKey\":\"assetValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedAsset1.getId(), DataConstants.SERVER_SCOPE, assetAttributes);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnEdge(savedAsset1.getId(),
                        DataConstants.SERVER_SCOPE, "assetKey", "assetValue"));

        // create asset #2 inside group #1
        Asset savedAsset2 = saveAssetOnCloud(StringUtils.randomAlphanumeric(15), "Building", savedAssetEntityGroup1.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetById(savedAsset2.getId()).isPresent());

        // add group #2 and assign to edge
        EntityGroup savedAssetEntityGroup2 = createEntityGroup(EntityType.ASSET);
        assignEntityGroupToEdge(savedAssetEntityGroup2);

        // add asset #2 to group #2
        cloudRestClient.addEntitiesToEntityGroup(savedAssetEntityGroup2.getId(), Collections.singletonList(savedAsset2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> asset2Groups = edgeRestClient.getEntityGroupsForEntity(savedAsset2.getId());
                    return asset2Groups.contains(savedAssetEntityGroup2.getId());
                });

        // add asset #2 to group #1
        cloudRestClient.addEntitiesToEntityGroup(savedAssetEntityGroup1.getId(), Collections.singletonList(savedAsset2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> asset2Groups = edgeRestClient.getEntityGroupsForEntity(savedAsset2.getId());
                    return asset2Groups.contains(savedAssetEntityGroup1.getId());
                });

        // remove asset #2 from group #1
        cloudRestClient.removeEntitiesFromEntityGroup(savedAssetEntityGroup1.getId(), Collections.singletonList(savedAsset2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> asset2Groups = edgeRestClient.getEntityGroupsForEntity(savedAsset2.getId());
                    return !asset2Groups.contains(savedAssetEntityGroup1.getId()) && asset2Groups.contains(savedAssetEntityGroup2.getId());
                });

        // remove asset #2 from group #2
        cloudRestClient.removeEntitiesFromEntityGroup(savedAssetEntityGroup2.getId(), Collections.singletonList(savedAsset2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<Asset> assets = edgeRestClient.getAssetsByEntityGroupId(savedAssetEntityGroup2.getId(), new PageLink(1000)).getData();
                    return !assets.contains(savedAsset2);
                });

        // delete asset #2
        cloudRestClient.deleteAsset(savedAsset2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetById(savedAsset2.getId()).isEmpty());

        // unassign group #1 from edge
        cloudRestClient.unassignEntityGroupFromEdge(edge.getId(), savedAssetEntityGroup1.getId(), EntityType.ASSET);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedAssetEntityGroup1.getId()).isEmpty());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetById(savedAsset1.getId()).isEmpty());

        // clean up
        cloudRestClient.deleteAsset(savedAsset1.getId());
        cloudRestClient.deleteEntityGroup(savedAssetEntityGroup1.getId());
        cloudRestClient.deleteEntityGroup(savedAssetEntityGroup2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedAssetEntityGroup2.getId()).isEmpty());

        // delete "building" asset profile
        cloudRestClient.deleteAssetProfile(savedAsset1.getAssetProfileId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetProfileById(savedAsset1.getAssetProfileId()).isEmpty());
    }

    @Test
    public void testSendAssetToCloud() {
        // create asset on edge
        EntityGroup savedAssetEntityGroup = createEntityGroup(EntityType.ASSET);
        assignEntityGroupToEdge(savedAssetEntityGroup);

        String assetName = "Edge Asset_" + StringUtils.randomAlphanumeric(15);
        Asset savedAssetOnEdge = saveAssetOnEdge(assetName, edgeRestClient.getDefaultAssetProfileInfo().getName(), savedAssetEntityGroup.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getAssetById(savedAssetOnEdge.getId()).isPresent());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getEntityGroupsForEntity(savedAssetOnEdge.getId()).size() == 3);

        // update asset
        String assetNameUpdated = assetName + "Updated";
        savedAssetOnEdge.setName(assetNameUpdated);
        edgeRestClient.saveAsset(savedAssetOnEdge);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> assetNameUpdated.equals(cloudRestClient.getAssetById(savedAssetOnEdge.getId()).get().getName()));

        // delete asset
        edgeRestClient.deleteAsset(savedAssetOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getEntityGroupsForEntity(savedAssetOnEdge.getId()).size() == 2);

        cloudRestClient.deleteAsset(savedAssetOnEdge.getId());
        cloudRestClient.deleteEntityGroup(savedAssetEntityGroup.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedAssetEntityGroup.getId()).isEmpty());
    }

    @Test
    public void testSendAssetToCloudWithNameThatAlreadyExistsOnCloud() {
        // create asset on cloud and edge with the same name
        EntityGroup savedAssetEntityGroup = createEntityGroup(EntityType.ASSET);
        assignEntityGroupToEdge(savedAssetEntityGroup);

        String assetName = "Edge Asset_" + StringUtils.randomAlphanumeric(15);
        Asset savedAssetOnCloud = saveAssetOnCloud(assetName, "Building", null);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetProfileById(savedAssetOnCloud.getAssetProfileId()).isPresent());

        Asset savedAssetOnEdge = saveAssetOnEdge(assetName, "Building", savedAssetEntityGroup.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<Asset> assetOptional = cloudRestClient.getAssetById(savedAssetOnEdge.getId());
                    return assetOptional.isPresent() && !assetOptional.get().getName().equals(savedAssetOnCloud.getName());
                });

        // delete asset
        edgeRestClient.deleteAsset(savedAssetOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getEntityGroupsForEntity(savedAssetOnEdge.getId()).size() == 2);

        cloudRestClient.deleteAsset(savedAssetOnEdge.getId());
        cloudRestClient.deleteAsset(savedAssetOnCloud.getId());

        cloudRestClient.deleteEntityGroup(savedAssetEntityGroup.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetById(savedAssetOnEdge.getId()).isEmpty() &&
                                edgeRestClient.getAssetById(savedAssetOnCloud.getId()).isEmpty() &&
                        edgeRestClient.getEntityGroupById(savedAssetEntityGroup.getId()).isEmpty());


        // delete "Building" asset profile
        cloudRestClient.deleteAssetProfile(savedAssetOnCloud.getAssetProfileId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetProfileById(savedAssetOnCloud.getAssetProfileId()).isEmpty());
    }

}

