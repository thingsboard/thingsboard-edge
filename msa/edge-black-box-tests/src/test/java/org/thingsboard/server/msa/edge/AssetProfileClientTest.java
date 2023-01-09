/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.IdBased;
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
        Dashboard dashboard = createDashboardAndAssignToEdge("Asset Profile Test Dashboard");
        AssetProfile savedAssetProfile = createCustomAssetProfile(dashboard.getId());

        verifyAssetProfilesOnEdge(2);

        // update asset profile
        savedAssetProfile.setName("Buildings Updated");
        cloudRestClient.saveAssetProfile(savedAssetProfile);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Buildings Updated".equals(edgeRestClient.getAssetProfileById(savedAssetProfile.getId()).get().getName()));
        // delete asset profile
        cloudRestClient.deleteAssetProfile(savedAssetProfile.getId());
        verifyAssetProfilesOnEdge(1);

        cloudRestClient.unassignDashboardFromEdge(edge.getId(), dashboard.getId());
        cloudRestClient.deleteDashboard(dashboard.getId());
    }

    private AssetProfile createCustomAssetProfile(DashboardId defaultDashboardId) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName("Buildings");
        assetProfile.setImage("iVBORw0KGgoAAAANSUhEUgAAAQAAAAEABA");
        assetProfile.setDefault(false);
        assetProfile.setDescription("Asset profile description");
        assetProfile.setDefaultQueueName("Main");
        assetProfile.setDefaultDashboardId(defaultDashboardId);
        // TODO: @voba
        // assetProfile.setDefaultRuleChainId();
        return cloudRestClient.saveAssetProfile(assetProfile);
    }

    private void verifyAssetProfilesOnEdge(int expectedAssetProfilesCnt) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() ->  edgeRestClient.getAssetProfiles(new PageLink(100)).getTotalElements() == expectedAssetProfilesCnt);

        PageData<AssetProfile> pageData = edgeRestClient.getAssetProfiles(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.ASSET_PROFILE);
    }

}

