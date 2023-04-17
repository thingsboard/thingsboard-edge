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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class AssetProfileClientTest extends AbstractContainerTest {

    @Test
    public void testAssetProfiles() throws Exception {
        verifyAssetProfilesOnEdge(1);

        // create asset profile
        EntityGroup dashboardGroup = createEntityGroup(EntityType.DASHBOARD);
        DashboardId dashboardId = createDashboardAndAssignToEdge("Asset Profile Test Dashboard", dashboardGroup);
        RuleChainId savedRuleChainId = createRuleChainAndAssignToEdge("Asset Profile Test RuleChain");
        AssetProfile savedAssetProfile = createCustomAssetProfile(dashboardId, savedRuleChainId);

        verifyAssetProfilesOnEdge(2);

        // update asset profile
        savedAssetProfile.setName("Buildings Updated");
        cloudRestClient.saveAssetProfile(savedAssetProfile);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Buildings Updated".equals(edgeRestClient.getAssetProfileById(savedAssetProfile.getId()).get().getName()));
        // delete asset profile
        cloudRestClient.deleteAssetProfile(savedAssetProfile.getId());
        verifyAssetProfilesOnEdge(1);

        unAssignFromEdgeAndDeleteDashboard(dashboardId, dashboardGroup.getId());
        unAssignFromEdgeAndDeleteRuleChain(savedRuleChainId);
    }

    private AssetProfile createCustomAssetProfile(DashboardId defaultDashboardId, RuleChainId edgeRuleChainId) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName("Buildings");
        assetProfile.setImage("iVBORw0KGgoAAAANSUhEUgAAAQAAAAEABA");
        assetProfile.setDefault(false);
        assetProfile.setDescription("Asset profile description");
        assetProfile.setDefaultQueueName("Main");
        assetProfile.setDefaultDashboardId(defaultDashboardId);
        assetProfile.setDefaultEdgeRuleChainId(edgeRuleChainId);
        return cloudRestClient.saveAssetProfile(assetProfile);
    }

    private void verifyAssetProfilesOnEdge(int expectedAssetProfilesCnt) {
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() ->  edgeRestClient.getAssetProfiles(new PageLink(100)).getTotalElements() == expectedAssetProfilesCnt);

        PageData<AssetProfile> pageData = edgeRestClient.getAssetProfiles(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.ASSET_PROFILE);
    }

}

