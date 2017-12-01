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
package org.thingsboard.server.dao.service;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseIntegrationServiceTest extends AbstractBeforeTest {

    private IdComparator<Integration> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void beforeRun() {
        tenantId = before();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveIntegration() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        Integration savedIntegration = integrationService.saveIntegration(integration);

        Assert.assertNotNull(savedIntegration);
        Assert.assertNotNull(savedIntegration.getId());
        Assert.assertTrue(savedIntegration.getCreatedTime() > 0);
        Assert.assertEquals(integration.getTenantId(), savedIntegration.getTenantId());
        Assert.assertNotNull(savedIntegration.getDefaultConverterId());
        Assert.assertEquals(NULL_UUID, savedIntegration.getDefaultConverterId().getId());
        Assert.assertEquals(integration.getRoutingKey(), savedIntegration.getRoutingKey());

        savedIntegration.setRoutingKey("My new integration");

        integrationService.saveIntegration(savedIntegration);
        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getId());
        Assert.assertEquals(foundIntegration.getRoutingKey(), savedIntegration.getRoutingKey());

        integrationService.deleteIntegration(savedIntegration.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveIntegrationWithEmptyRoutingKey() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setType(IntegrationType.OCEANCONNECT);
        integrationService.saveIntegration(integration);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveIntegrationWithEmptyTenant() {
        Integration integration = new Integration();
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        integrationService.saveIntegration(integration);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveIntegrationWithInvalidTenant() {
        Integration integration = new Integration();
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setTenantId(new TenantId(UUIDs.timeBased()));
        integrationService.saveIntegration(integration);
    }

    @Test(expected = DataValidationException.class)
    public void testAssignIntegrationToNonExistentConverter() {
        Integration integration = new Integration();
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setTenantId(tenantId);
        integration = integrationService.saveIntegration(integration);
        try {
            integrationService.assignIntegrationToConverter(integration.getId(), new ConverterId(UUIDs.timeBased()));
        } finally {
            integrationService.deleteIntegration(integration.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testAssignIntegrationToConverterFromDifferentTenant() {
        Integration integration = new Integration();
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setTenantId(tenantId);
        integration = integrationService.saveIntegration(integration);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant");
        tenant = tenantService.saveTenant(tenant);
        Converter converter = new Converter();
        converter.setTenantId(tenant.getId());
        converter.setType(ConverterType.GENERIC);
        converter.setName("Test different converter");
        converter = converterService.saveConverter(converter);
        try {
            integrationService.assignIntegrationToConverter(integration.getId(), converter.getId());
        } finally {
            integrationService.deleteIntegration(integration.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testFindIntegrationById() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        Integration savedIntegration = integrationService.saveIntegration(integration);
        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getId());
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);
        integrationService.deleteIntegration(savedIntegration.getId());
    }

    @Test
    public void testFindIntegrationTypesByTenantId() throws Exception {
        List<Integration> integrations = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Integration integration = new Integration();
                integration.setTenantId(tenantId);
                integration.setRoutingKey("My integration A" + i);
                integration.setType(IntegrationType.OCEANCONNECT);
                integrations.add(integrationService.saveIntegration(integration));
            }
            for (int i = 0; i < 7; i++) {
                Integration integration = new Integration();
                integration.setTenantId(tenantId);
                integration.setRoutingKey("My integration B" + i);
                integration.setType(IntegrationType.SIGFOX);
                integrations.add(integrationService.saveIntegration(integration));
            }
            List<EntitySubtype> integrationTypes = integrationService.findIntegrationTypesByTenantId(tenantId).get();
            Assert.assertNotNull(integrationTypes);
            Assert.assertEquals(2, integrationTypes.size());
            Assert.assertEquals(IntegrationType.OCEANCONNECT.toString(), integrationTypes.get(0).getType());
            Assert.assertEquals(IntegrationType.SIGFOX.toString(), integrationTypes.get(1).getType());
        } finally {
            integrations.forEach((integration) -> {
                integrationService.deleteIntegration(integration.getId());
            });
        }
    }

    @Test
    public void testDeleteIntegration() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setRoutingKey("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        Integration savedIntegration = integrationService.saveIntegration(integration);
        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getId());
        Assert.assertNotNull(foundIntegration);
        integrationService.deleteIntegration(savedIntegration.getId());
        foundIntegration = integrationService.findIntegrationById(savedIntegration.getId());
        Assert.assertNull(foundIntegration);
    }

    @Test
    public void testFindIntegrationsByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            integration.setRoutingKey("Integration" + i);
            integration.setType(IntegrationType.OCEANCONNECT);
            integrations.add(integrationService.saveIntegration(integration));
        }

        List<Integration> loadedIntegrations = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<Integration> pageData = null;
        do {
            pageData = integrationService.findIntegrationsByTenantId(tenantId, pageLink);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        integrations.sort(idComparator);
        loadedIntegrations.sort(idComparator);

        Assert.assertEquals(integrations, loadedIntegrations);

        integrationService.deleteIntegrationsByTenantId(tenantId);

        pageLink = new TextPageLink(33);
        pageData = integrationService.findIntegrationsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    public List<Integration> createIntegrationsList(int maxInt, String title, IntegrationType type) {
        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < maxInt; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(type);
            integrations.add(integrationService.saveIntegration(integration));
        }
        return integrations;
    }

    public List<Integration> getIntegrationsList(int limit, String title, IntegrationType type) {
        List<Integration> loadedIntegrations = new ArrayList<>();
        TextPageData<Integration> pageData;
        TextPageLink pageLink = new TextPageLink(limit, title);
        if (type == null) {
            do {
                pageData = integrationService.findIntegrationsByTenantId(tenantId, pageLink);
                loadedIntegrations.addAll(pageData.getData());
                if (pageData.hasNext()) {
                    pageLink = pageData.getNextPageLink();
                }
            } while (pageData.hasNext());
        } else {
            do {
                pageData = integrationService.findIntegrationsByTenantIdAndType(tenantId, type, pageLink);
                loadedIntegrations.addAll(pageData.getData());
                if (pageData.hasNext()) {
                    pageLink = pageData.getNextPageLink();
                }
            } while (pageData.hasNext());
        }
        return loadedIntegrations;
    }

    @Test
    public void testFindIntegrationsByTenantIdAndRoutingKey() {
        String title1 = "Integration title 1";
        List<Integration> integrationsTitle1;
        integrationsTitle1 = createIntegrationsList(143, title1, IntegrationType.OCEANCONNECT);

        String title2 = "Integration title 2";
        List<Integration> integrationsTitle2;
        integrationsTitle2 = createIntegrationsList(175, title2, IntegrationType.OCEANCONNECT);

        List<Integration> loadedIntegrationsTitle1;
        loadedIntegrationsTitle1 = getIntegrationsList(15, title1, null);
        Collections.sort(integrationsTitle1, idComparator);
        Collections.sort(loadedIntegrationsTitle1, idComparator);
        Assert.assertEquals(integrationsTitle1, loadedIntegrationsTitle1);

        List<Integration> loadedIntegrationsTitle2;
        loadedIntegrationsTitle2 = getIntegrationsList(4, title2, null);
        Collections.sort(integrationsTitle2, idComparator);
        Collections.sort(loadedIntegrationsTitle2, idComparator);
        Assert.assertEquals(integrationsTitle2, loadedIntegrationsTitle2);

        for (Integration integration : loadedIntegrationsTitle1) {
            integrationService.deleteIntegration(integration.getId());
        }

        TextPageLink pageLink = new TextPageLink(4, title1);
        TextPageData<Integration> pageData = integrationService.findIntegrationsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Integration integration : loadedIntegrationsTitle2) {
            integrationService.deleteIntegration(integration.getId());
        }

        pageLink = new TextPageLink(4, title2);
        pageData = integrationService.findIntegrationsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindIntegrationsByTenantIdAndType() {
        String title1 = "Integration title 1";
        IntegrationType type1 = IntegrationType.OCEANCONNECT;
        List<Integration> integrationsType1;
        integrationsType1 = createIntegrationsList(143, title1, type1);

        String title2 = "Integration title 2";
        IntegrationType type2 = IntegrationType.SIGFOX;
        List<Integration> integrationsType2;
        integrationsType2 = createIntegrationsList(175, title2, type2);

        List<Integration> loadedIntegrationsType1;
        loadedIntegrationsType1 = getIntegrationsList(15, null, type1);
        Collections.sort(integrationsType1, idComparator);
        Collections.sort(loadedIntegrationsType1, idComparator);
        Assert.assertEquals(integrationsType1, loadedIntegrationsType1);

        List<Integration> loadedIntegrationsType2;
        loadedIntegrationsType2 = getIntegrationsList(4, null, type2);
        Collections.sort(integrationsType2, idComparator);
        Collections.sort(loadedIntegrationsType2, idComparator);
        Assert.assertEquals(integrationsType2, loadedIntegrationsType2);

        for (Integration integration : loadedIntegrationsType1) {
            integrationService.deleteIntegration(integration.getId());
        }

        TextPageLink pageLink = new TextPageLink(4);
        TextPageData<Integration> pageData = integrationService.findIntegrationsByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Integration integration : loadedIntegrationsType2) {
            integrationService.deleteIntegration(integration.getId());
        }

        pageLink = new TextPageLink(4);
        pageData = integrationService.findIntegrationsByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindIntegrationsByTenantIdAndConverterId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Converter converter = new Converter();
        converter.setName("Test converter");
        converter.setTenantId(tenantId);
        converter.setType(ConverterType.GENERIC);
        converter = converterService.saveConverter(converter);
        ConverterId converterId = converter.getId();

        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 278; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            integration.setRoutingKey("Integration" + i);
            integration.setType(IntegrationType.OCEANCONNECT);
            integration = integrationService.saveIntegration(integration);
            integrations.add(integrationService.assignIntegrationToConverter(integration.getId(), converterId));
        }

        List<Integration> loadedIntegrations = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<Integration> pageData;
        do {
            pageData = integrationService.findIntegrationsByTenantIdAndConverterId(tenantId, converterId, pageLink);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrations, idComparator);
        Collections.sort(loadedIntegrations, idComparator);

        Assert.assertEquals(integrations, loadedIntegrations);

        integrationService.unassignConverterIntegrations(tenantId, converterId);

        pageLink = new TextPageLink(33);
        pageData = integrationService.findIntegrationsByTenantIdAndConverterId(tenantId, converterId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindIntegrationsByTenantIdConverterIdAndType() {
        Converter converter = new Converter();
        converter.setName("Test converter");
        converter.setTenantId(tenantId);
        converter.setType(ConverterType.GENERIC);
        converter = converterService.saveConverter(converter);
        ConverterId converterId = converter.getId();

        String title1 = "Integration title 1";
        IntegrationType type1 = IntegrationType.OCEANCONNECT;
        List<Integration> integrationsType1 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title1 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(type1);
            integration = integrationService.saveIntegration(integration);
            integrationsType1.add(integrationService.assignIntegrationToConverter(integration.getId(), converterId));
        }
        String title2 = "Integration title 2";
        IntegrationType type2 = IntegrationType.SIGFOX;
        List<Integration> integrationsType2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title2 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(type2);
            integration = integrationService.saveIntegration(integration);
            integrationsType2.add(integrationService.assignIntegrationToConverter(integration.getId(), converterId));
        }

        List<Integration> loadedIntegrationsType1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15);
        TextPageData<Integration> pageData;
        do {
            pageData = integrationService.findIntegrationsByTenantIdAndConverterIdAndType(tenantId, converterId, type1, pageLink);
            loadedIntegrationsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrationsType1, idComparator);
        Collections.sort(loadedIntegrationsType1, idComparator);

        Assert.assertEquals(integrationsType1, loadedIntegrationsType1);

        List<Integration> loadedIntegrationsType2 = new ArrayList<>();
        pageLink = new TextPageLink(4);
        do {
            pageData = integrationService.findIntegrationsByTenantIdAndConverterIdAndType(tenantId, converterId, type2, pageLink);
            loadedIntegrationsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(integrationsType2, idComparator);
        Collections.sort(loadedIntegrationsType2, idComparator);

        Assert.assertEquals(integrationsType2, loadedIntegrationsType2);

        for (Integration integration : loadedIntegrationsType1) {
            integrationService.deleteIntegration(integration.getId());
        }

        pageLink = new TextPageLink(4);
        pageData = integrationService.findIntegrationsByTenantIdAndConverterIdAndType(tenantId, converterId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Integration integration : loadedIntegrationsType2) {
            integrationService.deleteIntegration(integration.getId());
        }

        pageLink = new TextPageLink(4);
        pageData = integrationService.findIntegrationsByTenantIdAndConverterIdAndType(tenantId, converterId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        converterService.deleteConverter(converterId);
    }

    @Test
    public void testFindIntegrationsByTenantIdConverterIdAndRoutingKey() {
        Converter converter = new Converter();
        converter.setName("Test converter");
        converter.setType(ConverterType.GENERIC);
        converter.setTenantId(tenantId);
        converter = converterService.saveConverter(converter);
        ConverterId converterId = converter.getId();

        String title1 = "Integration title 1";
        List<Integration> integrationList1 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title1 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(IntegrationType.OCEANCONNECT);
            integration = integrationService.saveIntegration(integration);
            integrationList1.add(integrationService.assignIntegrationToConverter(integration.getId(), converterId));
        }
        String title2 = "Integration title 2";
        List<Integration> integrationList2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String routingKey = title2 + suffix;
            routingKey = i % 2 == 0 ? routingKey.toLowerCase() : routingKey.toUpperCase();
            integration.setRoutingKey(routingKey);
            integration.setType(IntegrationType.OCEANCONNECT);
            integration = integrationService.saveIntegration(integration);
            integrationList2.add(integrationService.assignIntegrationToConverter(integration.getId(), converterId));
        }

        List<Integration> loadedIntegrations1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Integration> pageData = null;
        do {
            pageData = integrationService.findIntegrationsByTenantIdAndConverterId(tenantId, converterId, pageLink);
            loadedIntegrations1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        integrationList1.sort(idComparator);
        loadedIntegrations1.sort(idComparator);

        Assert.assertEquals(integrationList1, loadedIntegrations1);

        List<Integration> loadedIntegrations2 = new ArrayList<>();
        pageLink = new TextPageLink(4, title2);
        do {
            pageData = integrationService.findIntegrationsByTenantIdAndConverterId(tenantId, converterId, pageLink);
            loadedIntegrations2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        integrationList2.sort(idComparator);
        loadedIntegrations2.sort(idComparator);

        Assert.assertEquals(integrationList2, loadedIntegrations2);

        for (Integration integration : loadedIntegrations1) {
            integrationService.deleteIntegration(integration.getId());
        }

        pageLink = new TextPageLink(4, title1);
        pageData = integrationService.findIntegrationsByTenantIdAndConverterId(tenantId, converterId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Integration integration : loadedIntegrations2) {
            integrationService.deleteIntegration(integration.getId());
        }

        pageLink = new TextPageLink(4, title2);
        pageData = integrationService.findIntegrationsByTenantIdAndConverterId(tenantId, converterId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        converterService.deleteConverter(converterId);
    }
}
