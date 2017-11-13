/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseIntegrationControllerTest extends AbstractControllerTest {

    private IdComparator<Integration> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

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
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        Assert.assertNotNull(savedIntegration);
        Assert.assertNotNull(savedIntegration.getId());
        Assert.assertTrue(savedIntegration.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedIntegration.getTenantId());
        Assert.assertNotNull(savedIntegration.getDefaultConverterId());
        Assert.assertEquals(NULL_UUID, savedIntegration.getDefaultConverterId().getId());
        Assert.assertEquals(integration.getRoutingKey(), savedIntegration.getRoutingKey());

        savedIntegration.setRoutingKey("My new integration");
        doPost("/api/integration", savedIntegration, Integration.class);

        Integration foundIntegration = doGet("/api/integration/" + savedIntegration.getId().getId().toString(), Integration.class);
        Assert.assertEquals(foundIntegration.getRoutingKey(), savedIntegration.getRoutingKey());
    }

    @Test
    public void testFindIntegrationById() throws Exception {
        Integration integration = new Integration();
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);
        Integration foundIntegration = doGet("/api/integration/" + savedIntegration.getId().getId().toString(), Integration.class);
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);
    }

    @Test
    public void testFindIntegrationTypesByTenantId() throws Exception {
        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Integration integration = new Integration();
            integration.setRoutingKey("My integration A" + i);
            integration.setType(IntegrationType.OCEANCONNECT);
            integrations.add(doPost("/api/integration", integration, Integration.class));
        }
        for (int i = 0; i < 7; i++) {
            Integration integration = new Integration();
            integration.setRoutingKey("My integration B" + i);
            integration.setType(IntegrationType.SIGFOX);
            integrations.add(doPost("/api/integration", integration, Integration.class));
        }
        List<EntitySubtype> integrationTypes = doGetTyped("/api/integration/types",
                new TypeReference<List<EntitySubtype>>() {
                });

        Assert.assertNotNull(integrationTypes);
        Assert.assertEquals(2, integrationTypes.size());
        Assert.assertEquals(IntegrationType.OCEANCONNECT.toString(), integrationTypes.get(0).getType());
        Assert.assertEquals(IntegrationType.SIGFOX.toString(), integrationTypes.get(1).getType());
    }

    @Test
    public void testDeleteIntegration() throws Exception {
        Integration integration = new Integration();
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        doDelete("/api/integration/" + savedIntegration.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/integration/" + savedIntegration.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveIntegrationWithEmptyType() throws Exception {
        Integration integration = new Integration();
        integration.setRoutingKey("My integration");
        doPost("/api/integration", integration)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Integration type should be specified")));
    }

    @Test
    public void testSaveIntegrationWithEmptyRoutingKey() throws Exception {
        Integration integration = new Integration();
        integration.setType(IntegrationType.OCEANCONNECT);
        doPost("/api/integration", integration)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Integration routing key should be specified")));
    }

    @Test
    public void testAssignUnassignIntegrationToConverter() throws Exception {
        Integration integration = new Integration();
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        Converter converter = new Converter();
        converter.setName("My converter");
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        Integration assignedIntegration = doPost("/api/converter/" + savedConverter.getId().getId().toString()
                + "/integration/" + savedIntegration.getId().getId().toString(), Integration.class);
        Assert.assertEquals(savedConverter.getId(), assignedIntegration.getDefaultConverterId());

        Integration foundIntegration = doGet("/api/integration/" + savedIntegration.getId().getId().toString(), Integration.class);
        Assert.assertEquals(savedConverter.getId(), foundIntegration.getDefaultConverterId());

        Integration unassignedIntegration =
                doDelete("/api/converter/integration/" + savedIntegration.getId().getId().toString(), Integration.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedIntegration.getDefaultConverterId().getId());

        foundIntegration = doGet("/api/integration/" + savedIntegration.getId().getId().toString(), Integration.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundIntegration.getDefaultConverterId().getId());
    }

    @Test
    public void testAssignIntegrationToNonExistentConverter() throws Exception {
        Integration integration = new Integration();
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        doPost("/api/converter/" + UUIDs.timeBased().toString()
                + "/integration/" + savedIntegration.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAssignIntegrationToConverterFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");

        tenantAdmin2 = createUserAndLogin(tenantAdmin2, "testPassword1");

        Converter converter = new Converter();
        converter.setName("Different converter");
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        login(tenantAdmin.getEmail(), "testPassword1");

        Integration integration = new Integration();
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        doPost("/api/converter/" + savedConverter.getId().getId().toString()
                + "/integration/" + savedIntegration.getId().getId().toString())
                .andExpect(status().isForbidden());

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant2.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindTenantIntegrations() throws Exception {
        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Integration integration = new Integration();
            integration.setRoutingKey("Integration" + i);
            integration.setType(IntegrationType.OCEANCONNECT);
            integrations.add(doPost("/api/integration", integration, Integration.class));
        }
        List<Integration> loadedIntegrations = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<Integration> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/integrations?",
                    new TypeReference<TextPageData<Integration>>() {
                    }, pageLink);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations, idComparator);
        Collections.sort(loadedIntegrations, idComparator);

        Assert.assertEquals(integrations, loadedIntegrations);
    }

    @Test
    public void testFindTenantIntegrationsByRoutingKey() throws Exception {
        String title1 = "Integration title 1";
        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Integration integration = new Integration();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title1 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(IntegrationType.OCEANCONNECT);
            integrations.add(doPost("/api/integration", integration, Integration.class));
        }
        String title2 = "Integration title 2";
        List<Integration> integrations1 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Integration integration = new Integration();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title2 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(IntegrationType.OCEANCONNECT);
            integrations1.add(doPost("/api/integration", integration, Integration.class));
        }

        List<Integration> loadedIntegrations = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Integration> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/integrations?",
                    new TypeReference<TextPageData<Integration>>() {
                    }, pageLink);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations, idComparator);
        Collections.sort(loadedIntegrations, idComparator);

        Assert.assertEquals(integrations, loadedIntegrations);

        List<Integration> loadedIntegrations1 = new ArrayList<>();
        pageLink = new TextPageLink(4, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/integrations?",
                    new TypeReference<TextPageData<Integration>>() {
                    }, pageLink);
            loadedIntegrations1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations1, idComparator);
        Collections.sort(loadedIntegrations1, idComparator);

        Assert.assertEquals(integrations1, loadedIntegrations1);

        for (Integration integration : loadedIntegrations) {
            doDelete("/api/integration/" + integration.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/integrations?",
                new TypeReference<TextPageData<Integration>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Integration integration : loadedIntegrations1) {
            doDelete("/api/integration/" + integration.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/integrations?",
                new TypeReference<TextPageData<Integration>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindTenantIntegrationsByType() throws Exception {
        String title1 = "Integration title 1";
        IntegrationType type1 = IntegrationType.OCEANCONNECT;
        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Integration integration = new Integration();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title1 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(type1);
            integrations.add(doPost("/api/integration", integration, Integration.class));
        }
        String title2 = "Integration title 2";
        IntegrationType type2 = IntegrationType.SIGFOX;
        List<Integration> integrations1 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Integration integration = new Integration();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title2 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(type2);
            integrations1.add(doPost("/api/integration", integration, Integration.class));
        }

        List<Integration> loadedIntegrations = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15);
        TextPageData<Integration> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/integrations?type={type}&",
                    new TypeReference<TextPageData<Integration>>() {
                    }, pageLink, type1);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations, idComparator);
        Collections.sort(loadedIntegrations, idComparator);

        Assert.assertEquals(integrations, loadedIntegrations);

        List<Integration> loadedIntegrations1 = new ArrayList<>();
        pageLink = new TextPageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/integrations?type={type}&",
                    new TypeReference<TextPageData<Integration>>() {
                    }, pageLink, type2);
            loadedIntegrations1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations1, idComparator);
        Collections.sort(loadedIntegrations1, idComparator);

        Assert.assertEquals(integrations1, loadedIntegrations1);

        for (Integration integration : loadedIntegrations1) {
            doDelete("/api/integration/" + integration.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/integrations?type={type}&",
                new TypeReference<TextPageData<Integration>>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Integration integration : loadedIntegrations1) {
            doDelete("/api/integration/" + integration.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/integrations?type={type}&",
                new TypeReference<TextPageData<Integration>>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindConverterIntegrations() throws Exception {
        Converter converter = new Converter();
        converter.setName("Test converter");
        converter = doPost("/api/converter", converter, Converter.class);
        ConverterId converterId = converter.getId();

        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            Integration integration = new Integration();
            integration.setRoutingKey("Integration" + i);
            integration.setType(IntegrationType.OCEANCONNECT);
            integration = doPost("/api/integration", integration, Integration.class);
            integrations.add(doPost("/api/converter/" + converterId.getId().toString()
                    + "/integration/" + integration.getId().getId().toString(), Integration.class));
        }

        List<Integration> loadedIntegrations = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<Integration> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converter/" + converterId.getId().toString() + "/integrations?",
                    new TypeReference<TextPageData<Integration>>() {
                    }, pageLink);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations, idComparator);
        Collections.sort(loadedIntegrations, idComparator);

        Assert.assertEquals(integrations, loadedIntegrations);
    }

    @Test
    public void testFindConverterIntegrationsByRoutingKey() throws Exception {
        Converter converter = new Converter();
        converter.setName("Test converter");
        converter = doPost("/api/converter", converter, Converter.class);
        ConverterId converterId = converter.getId();

        String title1 = "Integration title 1";
        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            Integration integration = new Integration();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title1 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(IntegrationType.OCEANCONNECT);
            integration = doPost("/api/integration", integration, Integration.class);
            integrations.add(doPost("/api/converter/" + converterId.getId().toString()
                    + "/integration/" + integration.getId().getId().toString(), Integration.class));
        }
        String title2 = "Integration title 2";
        List<Integration> integrations1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Integration integration = new Integration();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title2 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(IntegrationType.OCEANCONNECT);
            integration = doPost("/api/integration", integration, Integration.class);
            integrations1.add(doPost("/api/converter/" + converterId.getId().toString()
                    + "/integration/" + integration.getId().getId().toString(), Integration.class));
        }

        List<Integration> loadedIntegrations = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Integration> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converter/" + converterId.getId().toString() + "/integrations?",
                    new TypeReference<TextPageData<Integration>>() {
                    }, pageLink);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations, idComparator);
        Collections.sort(loadedIntegrations, idComparator);

        Assert.assertEquals(integrations, loadedIntegrations);

        List<Integration> loadedIntegrations1 = new ArrayList<>();
        pageLink = new TextPageLink(4, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/converter/" + converterId.getId().toString() + "/integrations?",
                    new TypeReference<TextPageData<Integration>>() {
                    }, pageLink);
            loadedIntegrations1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations1, idComparator);
        Collections.sort(loadedIntegrations1, idComparator);

        Assert.assertEquals(integrations1, loadedIntegrations1);

        for (Integration integration : loadedIntegrations) {
            doDelete("/api/converter/integration/" + integration.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title1);
        pageData = doGetTypedWithPageLink("/api/converter/" + converterId.getId().toString() + "/integrations?",
                new TypeReference<TextPageData<Integration>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Integration integration : loadedIntegrations1) {
            doDelete("/api/converter/integration/" + integration.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title2);
        pageData = doGetTypedWithPageLink("/api/converter/" + converterId.getId().toString() + "/integrations?",
                new TypeReference<TextPageData<Integration>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindConverterIntegrationsByType() throws Exception {
        Converter converter = new Converter();
        converter.setName("Test converter");
        converter = doPost("/api/converter", converter, Converter.class);
        ConverterId converterId = converter.getId();

        String title1 = "Integration title 1";
        IntegrationType type1 = IntegrationType.OCEANCONNECT;
        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            Integration integration = new Integration();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title1 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(type1);
            integration = doPost("/api/integration", integration, Integration.class);
            integrations.add(doPost("/api/converter/" + converterId.getId().toString()
                    + "/integration/" + integration.getId().getId().toString(), Integration.class));
        }
        String title2 = "Integration title 2";
        IntegrationType type2 = IntegrationType.SIGFOX;
        List<Integration> integrations1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Integration integration = new Integration();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title2 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(type2);
            integration = doPost("/api/integration", integration, Integration.class);
            integrations1.add(doPost("/api/converter/" + converterId.getId().toString()
                    + "/integration/" + integration.getId().getId().toString(), Integration.class));
        }

        List<Integration> loadedIntegrations = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15);
        TextPageData<Integration> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/converter/" + converterId.getId().toString() + "/integrations?type={type}&",
                    new TypeReference<TextPageData<Integration>>() {
                    }, pageLink, type1);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations, idComparator);
        Collections.sort(loadedIntegrations, idComparator);

        Assert.assertEquals(integrations, loadedIntegrations);

        List<Integration> loadedIntegrations1 = new ArrayList<>();
        pageLink = new TextPageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/converter/" + converterId.getId().toString() + "/integrations?type={type}&",
                    new TypeReference<TextPageData<Integration>>() {
                    }, pageLink, type2);
            loadedIntegrations1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations1, idComparator);
        Collections.sort(loadedIntegrations1, idComparator);

        Assert.assertEquals(integrations1, loadedIntegrations1);

        for (Integration integration : loadedIntegrations) {
            doDelete("/api/converter/integration/" + integration.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4);
        pageData = doGetTypedWithPageLink("/api/converter/" + converterId.getId().toString() + "/integrations?type={type}&",
                new TypeReference<TextPageData<Integration>>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Integration integration : loadedIntegrations1) {
            doDelete("/api/converter/integration/" + integration.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4);
        pageData = doGetTypedWithPageLink("/api/converter/" + converterId.getId().toString() + "/integrations?type={type}&",
                new TypeReference<TextPageData<Integration>>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
}
