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

import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

public class ResourceClientTest extends AbstractContainerTest {

    @Test
    public void testSendResourceToEdge() {
        // create resource on cloud
        String title = "Resource on Cloud";
        TbResource resource = saveResourceOnEdge(title, "ResourceCloud.js", cloudRestClient);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getResourceById(resource.getId()).isPresent());

        // update resource on cloud
        String updateTitle = title + " Updated";
        resource.setTitle(updateTitle);
        cloudRestClient.saveResource(resource);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getResourceById(resource.getId()).get().getTitle().equals(updateTitle));

        // delete resource on cloud
        cloudRestClient.deleteResource(resource.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<TbResourceInfo> resources = edgeRestClient.getResources(new PageLink(1000));
                    long count = resources.getData().stream().filter(d -> resource.getId().equals(d.getId())).count();
                    return count == 0;
                });
    }

    @Test
    public void testSendResourceToCloud() {
        // create resource on edge
        String title = "Resource on Edge";
        TbResource resource = saveResourceOnEdge(title, "ResourceEdge.js", edgeRestClient);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getResourceById(resource.getId()).isPresent());

        // update resource on edge
        String updateTitle = title + " Updated";
        resource.setTitle(updateTitle);
        edgeRestClient.saveResource(resource);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getResourceById(resource.getId()).get().getTitle().equals(updateTitle));

        // cleanup - we can delete resources only on Cloud
        cloudRestClient.deleteResource(resource.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<TbResourceInfo> resources = edgeRestClient.getResources(new PageLink(1000));
                    long count = resources.getData().stream().filter(d -> resource.getId().equals(d.getId())).count();
                    return count == 0;
                });
    }

    private TbResource saveResourceOnEdge(String title, String resourceKey, RestClient restClient) {
        TbResource tbResource = new TbResource();
        tbResource.setTitle(title);
        tbResource.setResourceKey(resourceKey);
        tbResource.setResourceType(ResourceType.JS_MODULE);
        tbResource.setFileName(resourceKey);
        tbResource.setData("Data".getBytes());
        return restClient.saveResource(tbResource);
    }

}
