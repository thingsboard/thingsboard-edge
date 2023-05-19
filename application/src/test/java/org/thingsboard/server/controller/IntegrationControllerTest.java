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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.integration.IntegrationManagerService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL",
})
@DaoSqlTest
public class IntegrationControllerTest extends AbstractControllerTest {

    @Autowired
    IntegrationManagerService integrationManagerService;

    private IdComparator<Integration> idComparator = new IdComparator<>();
    private IdComparator<IntegrationInfo> infosIdComparator = new IdComparator<>();

    private Tenant savedTenant;
    private Converter savedConverter;
    private User tenantAdmin;

    private static final JsonNode CUSTOM_CONVERTER_CONFIGURATION = JacksonUtil.newObjectNode()
            .put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");

    private static final ObjectNode INTEGRATION_CONFIGURATION = JacksonUtil.newObjectNode();

    static {
        INTEGRATION_CONFIGURATION.putObject("metadata").put("key1", "val1");
    }

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        savedConverter = doPost("/api/converter", converter, Converter.class);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveIntegration() throws Exception {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(savedConverter.getId());
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        Assert.assertNotNull(savedIntegration);
        Assert.assertNotNull(savedIntegration.getId());
        Assert.assertTrue(savedIntegration.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedIntegration.getTenantId());
        Assert.assertEquals(savedConverter.getId(), savedIntegration.getDefaultConverterId());
        Assert.assertEquals(integration.getRoutingKey(), savedIntegration.getRoutingKey());

        savedIntegration.setName("My new integration");
        doPost("/api/integration", savedIntegration, Integration.class);

        Integration foundIntegration = doGet("/api/integration/" + savedIntegration.getId().getId().toString(), Integration.class);
        Assert.assertEquals(foundIntegration.getName(), savedIntegration.getName());
    }

    @Test
    public void testFindIntegrationById() throws Exception {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(savedConverter.getId());
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);
        Integration foundIntegration = doGet("/api/integration/" + savedIntegration.getId().getId().toString(), Integration.class);
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);
    }

    @Test
    public void testDeleteIntegration() throws Exception {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(savedConverter.getId());
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        doDelete("/api/integration/" + savedIntegration.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/integration/" + savedIntegration.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveIntegrationWithEmptyType() throws Exception {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(savedConverter.getId());
        doPost("/api/integration", integration)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Integration type should be specified")));
    }

    @Test
    public void testSaveIntegrationWithEmptyRoutingKey() throws Exception {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        integration.setDefaultConverterId(savedConverter.getId());
        doPost("/api/integration", integration)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Integration routing key should be specified")));
    }

    @Test
    public void testSaveIntegrationWithEmptyConverterId() throws Exception {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        doPost("/api/integration", integration)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Integration default converter should be specified")));
    }

    @Test
    public void testFindTenantIntegrations() throws Exception {
        List<Integration> integrationList = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Integration integration = new Integration();
            integration.setName("Integration" + i);
            integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
            integration.setType(IntegrationType.OCEANCONNECT);
            integration.setConfiguration(INTEGRATION_CONFIGURATION);
            integration.setDefaultConverterId(savedConverter.getId());
            integrationList.add(doPost("/api/integration", integration, Integration.class));
        }
        List<Integration> loadedIntegrations = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Integration> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/integrations?",
                    new TypeReference<PageData<Integration>>() {
                    }, pageLink);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrationList, idComparator);
        Collections.sort(loadedIntegrations, idComparator);

        Assert.assertEquals(integrationList, loadedIntegrations);
    }

    @Test
    public void testFindIntegrationInfos() throws Exception {
        List<IntegrationInfo> integrationList = new ArrayList<>();
        for (int i = 0; i < 33; i++) {
            Integration integration = new Integration();
            integration.setName("Integration" + i);
            integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
            integration.setType(IntegrationType.OCEANCONNECT);
            integration.setConfiguration(INTEGRATION_CONFIGURATION);
            integration.setDefaultConverterId(savedConverter.getId());
            integrationList.add(toInfo(doPost("/api/integration", integration, Integration.class)));
        }

        Converter edgeConverter = new Converter();
        edgeConverter.setName("My edge converter");
        edgeConverter.setType(ConverterType.UPLINK);
        edgeConverter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        edgeConverter.setEdgeTemplate(true);
        edgeConverter = doPost("/api/converter", edgeConverter, Converter.class);

        Integration edgeIntegration = new Integration();
        edgeIntegration.setName("edge integration");
        edgeIntegration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        edgeIntegration.setType(IntegrationType.OCEANCONNECT);
        edgeIntegration.setConfiguration(INTEGRATION_CONFIGURATION);
        edgeIntegration.setDefaultConverterId(edgeConverter.getId());
        edgeIntegration.setEdgeTemplate(true);
        toInfo(doPost("/api/integration", edgeIntegration, Integration.class));

        List<IntegrationInfo> loadedIntegrations = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<IntegrationInfo> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/integrationInfos?",
                    new TypeReference<PageData<IntegrationInfo>>() {
                    }, pageLink);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrationList, infosIdComparator);
        Collections.sort(loadedIntegrations, infosIdComparator);

        Assert.assertEquals(integrationList, loadedIntegrations);
    }

    @Test
    public void testFindTenantIntegrationsBySearchText() throws Exception {
        String title1 = "Integration title 1";
        List<Integration> integrations1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Integration integration = new Integration();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            integration.setName(name);
            integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
            integration.setType(IntegrationType.OCEANCONNECT);
            integration.setConfiguration(INTEGRATION_CONFIGURATION);
            integration.setDefaultConverterId(savedConverter.getId());
            integrations1.add(doPost("/api/integration", integration, Integration.class));
        }
        String title2 = "Integration title 2";
        List<Integration> integrations2 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Integration integration = new Integration();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            integration.setName(name);
            integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
            integration.setType(IntegrationType.OCEANCONNECT);
            integration.setConfiguration(INTEGRATION_CONFIGURATION);
            integration.setDefaultConverterId(savedConverter.getId());
            integrations2.add(doPost("/api/integration", integration, Integration.class));
        }

        List<Integration> loadedIntegrations1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Integration> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/integrations?",
                    new TypeReference<PageData<Integration>>() {
                    }, pageLink);
            loadedIntegrations1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations1, idComparator);
        Collections.sort(loadedIntegrations1, idComparator);

        Assert.assertEquals(integrations1, loadedIntegrations1);

        List<Integration> loadedIntegrations2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/integrations?",
                    new TypeReference<PageData<Integration>>() {
                    }, pageLink);
            loadedIntegrations2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations2, idComparator);
        Collections.sort(loadedIntegrations2, idComparator);

        Assert.assertEquals(integrations2, loadedIntegrations2);

        for (Integration integration : loadedIntegrations1) {
            doDelete("/api/integration/" + integration.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/integrations?",
                new TypeReference<PageData<Integration>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Integration integration : loadedIntegrations2) {
            doDelete("/api/integration/" + integration.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/integrations?",
                new TypeReference<PageData<Integration>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testUpdateIntegrationFromLocalToRemoteIsCorrect() throws Exception {
        Integration integration = new Integration();
        integration.setName("Local integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(savedConverter.getId());
        integration.setType(IntegrationType.HTTP);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        integration.setEnabled(true);
        Integration foundIntegration = doPost("/api/integration", integration, Integration.class);

        Awaitility.await("[PlatformIntegrationService]: try to get local integration by routing key")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        integrationManagerService.getIntegrationByRoutingKey(foundIntegration.getRoutingKey()).get();
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });

        Assert.assertNotNull(
                integrationManagerService.getIntegrationByRoutingKey(foundIntegration.getRoutingKey()).get()
        );

        foundIntegration.setRemote(true);
        doPost("/api/integration", foundIntegration, Integration.class);


        Awaitility.await("[PlatformIntegrationService]: try to get remote integration by routing key")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        integrationManagerService.getIntegrationByRoutingKey(foundIntegration.getRoutingKey()).get();
                        return false;
                    } catch (Exception e) {
                        return true;
                    }
                });

        try {
            integrationManagerService.getIntegrationByRoutingKey(foundIntegration.getRoutingKey()).get();
            Assert.fail("Remote integration wasn't deleted from PlatformIntegrationService");
        } catch (ExecutionException e) {
            if (!e.getMessage().contains("The integration is executed remotely!")) {
                Assert.fail("Exception during PlatformIntegrationService execution! " + e.getMessage());
            }
        }

    }

    @Test
    public void testUpdateIntegrationFromRemoteToLocalIsCorrect() throws Exception {
        Integration integration = new Integration();
        integration.setName("Local integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(savedConverter.getId());
        integration.setType(IntegrationType.HTTP);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        integration.setEnabled(true);
        integration.setRemote(true);
        Integration foundIntegration = doPost("/api/integration", integration, Integration.class);

        Awaitility.await("[PlatformIntegrationService]: try to get local integration by routing key")
                .atLeast(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        integrationManagerService.getIntegrationByRoutingKey(foundIntegration.getRoutingKey()).get();
                        return false;
                    } catch (Exception e) {
                        return true;
                    }
                });

        try {
            integrationManagerService.getIntegrationByRoutingKey(foundIntegration.getRoutingKey()).get();
            Assert.fail("Expected that remote integration is not present in PlatformIntegrationService but it is not!");
        } catch (ExecutionException e) {
            if (!e.getMessage().contains("The integration is executed remotely!")) {
                Assert.fail("Exception during PlatformIntegrationService execution! " + e.getMessage());
            }
        }

        foundIntegration.setRemote(false);
        doPost("/api/integration", foundIntegration, Integration.class);

        Awaitility.await("[PlatformIntegrationService]: try to get remote integration by routing key")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        integrationManagerService.getIntegrationByRoutingKey(foundIntegration.getRoutingKey()).get();
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });

        Assert.assertNotNull(
                "Expected that local integration is present in PlatformIntegrationService but it is not!",
                integrationManagerService.getIntegrationByRoutingKey(foundIntegration.getRoutingKey()).get()
        );
    }

    @Test
    public void testFindEdgeIntegrationsByEdgeId() throws Exception {
        Converter edgeConverter = new Converter();
        edgeConverter.setName("My edge converter");
        edgeConverter.setType(ConverterType.UPLINK);
        edgeConverter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        edgeConverter.setEdgeTemplate(true);
        edgeConverter = doPost("/api/converter", edgeConverter, Converter.class);

        Edge edge = constructEdge("Edge with integration", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        List<Integration> edgeIntegrations = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            Integration integration = new Integration();
            integration.setName("Edge integration " + i);
            integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
            integration.setDefaultConverterId(edgeConverter.getId());
            integration.setType(IntegrationType.HTTP);
            integration.setConfiguration(INTEGRATION_CONFIGURATION);
            integration.setEdgeTemplate(true);
            Integration savedIntegration = doPost("/api/integration", integration, Integration.class);
            doPost("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/integration/" + savedIntegration.getId().getId().toString(), Integration.class);
            edgeIntegrations.add(savedIntegration);
        }

        List<Integration> loadedEdgeIntegrations = new ArrayList<>();
        PageLink pageLink = new PageLink(13);
        PageData<Integration> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/integrations?",
                    new TypeReference<>() {}, pageLink);
            loadedEdgeIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgeIntegrations, idComparator);
        Collections.sort(loadedEdgeIntegrations, idComparator);

        Assert.assertEquals(edgeIntegrations, loadedEdgeIntegrations);

        for (Integration integration : loadedEdgeIntegrations) {
            doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/integration/" + integration.getId().getId().toString(), Integration.class);
        }

        pageLink = new PageLink(13);
        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/integrations?",
                new TypeReference<>() {}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getTotalElements());
    }

    @Test
    public void testFindMissingEdgeAttributesInIntegrationConfig() throws Exception {
        Converter edgeConverter = new Converter();
        edgeConverter.setName("My edge converter");
        edgeConverter.setType(ConverterType.UPLINK);
        ObjectNode converterConfiguration =
                JacksonUtil.OBJECT_MAPPER.createObjectNode().put("decoder", "return {deviceName: 'Device Name', deviceType: metadata['deviceType']};");
        edgeConverter.setConfiguration(converterConfiguration);
        edgeConverter.setEdgeTemplate(true);
        edgeConverter = doPost("/api/converter", edgeConverter, Converter.class);

        Integration integration = new Integration();
        integration.setName("Edge integration #1");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(edgeConverter.getId());
        integration.setType(IntegrationType.HTTP);
        ObjectNode integrationConfiguration = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        integrationConfiguration.putObject("metadata").put("deviceType", "${{DEVICE_TYPE}}").put("deviceFirmware", "${{DEVICE_FW}}");
        integrationConfiguration.put("baseUrl", "${{HTTP_URL}}/api");
        integration.setConfiguration(integrationConfiguration);
        integration.setEdgeTemplate(true);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        Edge edge1 = constructEdge("Edge #1 with integration", "default");
        Edge savedEdge1 = doPost("/api/edge", edge1, Edge.class);

        JsonNode attributesData1 = JacksonUtil.toJsonNode("{\"HTTP_URL\":\"localhost:18080\"}");
        doPost("/api/plugins/telemetry/EDGE/" + savedEdge1.getUuidId() + "/attributes/SERVER_SCOPE", attributesData1);

        Edge edge2 = constructEdge("Edge #2 with integration", "default");
        Edge savedEdge2 = doPost("/api/edge", edge2, Edge.class);

        JsonNode attributesData2 = JacksonUtil.toJsonNode("{\"DEVICE_TYPE\":\"thermostat\"}");
        doPost("/api/plugins/telemetry/EDGE/" + savedEdge2.getUuidId() + "/attributes/SERVER_SCOPE", attributesData2);

        doPost("/api/edge/" + savedEdge1.getUuidId()
                + "/integration/" + savedIntegration.getUuidId(), Integration.class);

        doPost("/api/edge/" + savedEdge2.getUuidId()
                + "/integration/" + savedIntegration.getUuidId(), Integration.class);

        String missingAttributesForAllRelatedEdges =
                doGet("/api/edge/integration/" + savedIntegration.getUuidId() + "/allMissingAttributes", String.class);

        Assert.assertTrue(missingAttributesForAllRelatedEdges.contains("Edge #1 with integration"));
        Assert.assertTrue(missingAttributesForAllRelatedEdges.contains("Edge #2 with integration"));
        Assert.assertTrue(missingAttributesForAllRelatedEdges.contains("DEVICE_TYPE"));
        Assert.assertTrue(missingAttributesForAllRelatedEdges.contains("HTTP_URL"));
        Assert.assertTrue(missingAttributesForAllRelatedEdges.contains("DEVICE_FW"));

        Integration integration2 = new Integration();
        integration2.setName("Edge integration #2");
        integration2.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration2.setDefaultConverterId(edgeConverter.getId());
        integration2.setType(IntegrationType.HTTP);
        ObjectNode integrationConfiguration2 = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        integrationConfiguration2.put("baseUrl", "${{HTTPS_URL}}/api");
        integration2.setConfiguration(integrationConfiguration2);
        integration2.setEdgeTemplate(true);
        Integration savedIntegration2 = doPost("/api/integration", integration2, Integration.class);

        doPost("/api/edge/" + savedEdge1.getUuidId()
                + "/integration/" + savedIntegration2.getUuidId(), Integration.class);

        String integrationIds = savedIntegration.getUuidId() + "," + savedIntegration2.getUuidId();
        String missingAttributesForEdge1 =
                doGet("/api/edge/integration/" + savedEdge1.getUuidId() + "/missingAttributes?integrationIds=" + integrationIds, String.class);

        Assert.assertTrue(missingAttributesForEdge1.contains("Edge integration #1"));
        Assert.assertTrue(missingAttributesForEdge1.contains("DEVICE_TYPE"));
        Assert.assertTrue(missingAttributesForEdge1.contains("DEVICE_FW"));
        Assert.assertTrue(missingAttributesForEdge1.contains("Edge integration #2"));
        Assert.assertTrue(missingAttributesForEdge1.contains("HTTPS_URL"));
        Assert.assertFalse(missingAttributesForEdge1.contains("HTTP_URL"));
    }

    private IntegrationInfo toInfo(Integration integration) {
        IntegrationInfo integrationInfo = new IntegrationInfo(integration.getId());
        integrationInfo.setCreatedTime(integration.getCreatedTime());
        integrationInfo.setTenantId(integration.getTenantId());
        integrationInfo.setName(integration.getName());
        integrationInfo.setType(integration.getType());
        integrationInfo.setEnabled(integration.isEnabled());
        integrationInfo.setEdgeTemplate(integration.isEdgeTemplate());
        integrationInfo.setRemote(integration.isRemote());
        integrationInfo.setAllowCreateDevicesOrAssets(integration.isAllowCreateDevicesOrAssets());

        return integrationInfo;
    }

}
