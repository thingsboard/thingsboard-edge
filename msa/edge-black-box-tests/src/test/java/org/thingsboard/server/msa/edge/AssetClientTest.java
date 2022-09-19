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
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class AssetClientTest extends AbstractContainerTest {

    @Test
    public void testAssets() throws Exception {
        Asset savedAsset = saveAndAssignAssetToEdge();

        cloudRestClient.assignAssetToEdge(edge.getId(), savedAsset.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getAssetById(savedAsset.getId()).isPresent());

        cloudRestClient.unassignAssetFromEdge(edge.getId(), savedAsset.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getAssetById(savedAsset.getId()).isEmpty());

        cloudRestClient.deleteAsset(savedAsset.getId());
    }

}

