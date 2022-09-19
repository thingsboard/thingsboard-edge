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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class EntityViewClientTest extends AbstractContainerTest {

    @Test
    public void testEntityViews() throws Exception {
        Device device = saveAndAssignDeviceToEdge();

        EntityView savedEntityViewOnCloud = saveEntityViewOnCloud("Edge Entity View 1", "Default", device.getId());

        cloudRestClient.assignEntityViewToEdge(edge.getId(), savedEntityViewOnCloud.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isPresent());

        cloudRestClient.unassignEntityViewFromEdge(edge.getId(), savedEntityViewOnCloud.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isEmpty());

        cloudRestClient.deleteEntityView(savedEntityViewOnCloud.getId());
    }

    private EntityView saveEntityViewOnCloud(String entityViewName, String type, DeviceId deviceId) {
        EntityView entityView = new EntityView();
        entityView.setName(entityViewName);
        entityView.setType(type);
        entityView.setEntityId(deviceId);
        return cloudRestClient.saveEntityView(entityView);
    }

}

