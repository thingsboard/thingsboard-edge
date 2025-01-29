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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class WidgetBundleAndTypeClientTest extends AbstractContainerTest {

    @Test
    public void testWidgetsBundles_verifyInitialSetup() {
        performTestOnEachEdge(this::_testWidgetsBundles_verifyInitialSetup);
    }

    private void _testWidgetsBundles_verifyInitialSetup() {
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getWidgetsBundles(new PageLink(100)).getTotalElements() == 30);

        PageData<WidgetsBundle> pageData = edgeRestClient.getWidgetsBundles(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.WIDGETS_BUNDLE);

        for (WidgetsBundleId widgetsBundleId : pageData.getData().stream().map(WidgetsBundle::getId).collect(Collectors.toList())) {
            Awaitility.await()
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .atMost(30, TimeUnit.SECONDS)
                    .until(() -> {
                        List<WidgetType> edgeBundleWidgetTypes = edgeRestClient.getBundleWidgetTypes(widgetsBundleId);
                        List<WidgetType> cloudBundleWidgetTypes = cloudRestClient.getBundleWidgetTypes(widgetsBundleId);
                        return cloudBundleWidgetTypes != null && edgeBundleWidgetTypes != null
                                && edgeBundleWidgetTypes.size() == cloudBundleWidgetTypes.size();
                    });
            List<WidgetType> edgeBundleWidgetTypes = edgeRestClient.getBundleWidgetTypes(widgetsBundleId);
            List<WidgetType> cloudBundleWidgetTypes = cloudRestClient.getBundleWidgetTypes(widgetsBundleId);
            Assert.assertNotNull("edgeBundleWidgetTypes can't be null", edgeBundleWidgetTypes);
            Assert.assertNotNull("cloudBundleWidgetTypes can't be null", cloudBundleWidgetTypes);
            assertEntitiesByIdsAndType(edgeBundleWidgetTypes.stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.WIDGET_TYPE);
        }
    }

    @Test
    public void testWidgetsBundleAndWidgetType() {
        performTestOnEachEdge(this::_testWidgetsBundleAndWidgetType);
    }

    private void _testWidgetsBundleAndWidgetType() {
        // create widget bundle
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("Test Widget Bundle");
        WidgetsBundle savedWidgetsBundle = cloudRestClient.saveWidgetsBundle(widgetsBundle);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getWidgetsBundleById(savedWidgetsBundle.getId()).isPresent());

        // create widget type
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Test Widget Type");
        ObjectNode descriptor = JacksonUtil.newObjectNode();
        descriptor.put("key", "value");
        widgetType.setDescriptor(descriptor);
        WidgetTypeDetails savedWidgetType = cloudRestClient.saveWidgetType(widgetType);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getWidgetTypeById(savedWidgetType.getId()).isPresent());

        // update widget bundle
        savedWidgetsBundle.setTitle("Test Widget Bundle Updated");
        cloudRestClient.saveWidgetsBundle(savedWidgetsBundle);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Test Widget Bundle Updated".equals(edgeRestClient.getWidgetsBundleById(savedWidgetsBundle.getId()).get().getName()));

        // update widget type
        savedWidgetType.setName("Test Widget Type Updated");
        cloudRestClient.saveWidgetType(savedWidgetType);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Test Widget Type Updated".equals(edgeRestClient.getWidgetTypeById(savedWidgetType.getId()).get().getName()));

        // delete widget type
        cloudRestClient.deleteWidgetType(savedWidgetType.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getWidgetTypeById(savedWidgetType.getId()).isEmpty());

        // delete widget bundle
        cloudRestClient.deleteWidgetsBundle(savedWidgetsBundle.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getWidgetsBundleById(savedWidgetsBundle.getId()).isEmpty());
    }

}
