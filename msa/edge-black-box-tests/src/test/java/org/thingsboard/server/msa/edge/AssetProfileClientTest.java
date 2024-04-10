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
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.AssetProfile;
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
    public void testAssetProfiles() {
        verifyAssetProfilesOnEdge(1);

        // create asset profile
        DashboardId dashboardId = createDashboardAndAssignToEdge("Asset Profile Test Dashboard");
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

        unAssignFromEdgeAndDeleteDashboard(dashboardId);
        unAssignFromEdgeAndDeleteRuleChain(savedRuleChainId);
    }

    @Test
    public void testAssetProfileToCloud() {
        // create asset profile on edge
        AssetProfile saveAssetProfileOnEdge = saveAssetProfileOnEdge("Asset Profile On Edge");
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getAssetProfileById(saveAssetProfileOnEdge.getId()).isPresent());

        // update asset profile
        saveAssetProfileOnEdge.setName("Asset Profile On Edge Updated");
        edgeRestClient.saveAssetProfile(saveAssetProfileOnEdge);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Asset Profile On Edge Updated".equals(cloudRestClient.getAssetProfileById(saveAssetProfileOnEdge.getId()).get().getName()));

        // cleanup - we can delete asset profile only on Cloud
        cloudRestClient.deleteAssetProfile(saveAssetProfileOnEdge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAssetProfileById(saveAssetProfileOnEdge.getId()).isEmpty());
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
                .until(() ->  {
                    PageData<AssetProfile> assetProfiles = edgeRestClient.getAssetProfiles(new PageLink(100));
                    for (AssetProfile assetProfile : assetProfiles.getData()) {
                        System.out.println("<<<< " + assetProfile);
                    }
                    return assetProfiles.getTotalElements() == expectedAssetProfilesCnt;
                });

        PageData<AssetProfile> pageData = edgeRestClient.getAssetProfiles(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.ASSET_PROFILE);
    }

    private AssetProfile saveAssetProfileOnEdge(String assetProfileName) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(assetProfileName);
        return edgeRestClient.saveAssetProfile(assetProfile);
    }

}
