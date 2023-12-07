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
        TbResource resource = saveResourceOnEdge(title, "ResourceCloud.jks", cloudRestClient);
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
        TbResource resource = saveResourceOnEdge(title, "ResourceEdge.jks", edgeRestClient);
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
        tbResource.setResourceType(ResourceType.JKS);
        tbResource.setFileName(resourceKey);
        tbResource.setData("Data".getBytes());
        return restClient.saveResource(tbResource);
    }
}
