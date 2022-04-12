/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseIntegrationServiceTest extends AbstractBeforeTest {

    private IdComparator<Integration> idComparator = new IdComparator<>();

    private TenantId tenantId;
    private ConverterId converterId;

    private static final JsonNode CUSTOM_CONVERTER_CONFIGURATION = new ObjectMapper()
            .createObjectNode().put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");

    private static final ObjectNode INTEGRATION_CONFIGURATION = new ObjectMapper()
            .createObjectNode();
    static {
        INTEGRATION_CONFIGURATION.putObject("metadata").put("key1", "val1");
    }

    @Before
    public void beforeRun() {
        tenantId = before();
        Converter savedConverter = createConverter(tenantId);
        converterId = savedConverter.getId();
    }

    private Converter createConverter(TenantId tenantId) {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(CUSTOM_CONVERTER_CONFIGURATION);
        return converterService.saveConverter(converter);
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveIntegration() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(RandomStringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);

        Assert.assertNotNull(savedIntegration);
        Assert.assertNotNull(savedIntegration.getId());
        Assert.assertTrue(savedIntegration.getCreatedTime() > 0);
        Assert.assertEquals(integration.getTenantId(), savedIntegration.getTenantId());
        Assert.assertEquals(integration.getDefaultConverterId(), savedIntegration.getDefaultConverterId());
        Assert.assertEquals(integration.getRoutingKey(), savedIntegration.getRoutingKey());

        savedIntegration.setName("My new integration");

        integrationService.saveIntegration(savedIntegration);
        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        Assert.assertEquals(foundIntegration.getName(), savedIntegration.getName());

        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveIntegrationWithEmptyRoutingKey() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        integrationService.saveIntegration(integration);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveIntegrationWithEmptyTenant() {
        Integration integration = new Integration();
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(RandomStringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        integrationService.saveIntegration(integration);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveIntegrationWithInvalidTenant() {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(RandomStringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(converterId);
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        integration.setTenantId(new TenantId(Uuids.timeBased()));
        integrationService.saveIntegration(integration);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveIntegrationWithEmptyConverterId() {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(RandomStringUtils.randomAlphanumeric(15));
        integration.setTenantId(tenantId);
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        integrationService.saveIntegration(integration);
    }

    @Test
    public void testFindIntegrationById() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(RandomStringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);
        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);
        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());
    }

    @Test
    public void testDeleteIntegration() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(RandomStringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);
        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        Assert.assertNotNull(foundIntegration);
        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());
        foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        Assert.assertNull(foundIntegration);
    }

    @Test
    public void testFindTenantIntegrations() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();
        Converter converter = createConverter(tenantId);
        ConverterId converterId = converter.getId();

        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            integration.setDefaultConverterId(converterId);
            integration.setName("Integration" + i);
            integration.setRoutingKey(RandomStringUtils.randomAlphanumeric(15));
            integration.setType(IntegrationType.OCEANCONNECT);
            integration.setConfiguration(INTEGRATION_CONFIGURATION);
            integrations.add(integrationService.saveIntegration(integration));
        }

        List<Integration> loadedIntegrations = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Integration> pageData = null;
        do {
            pageData = integrationService.findTenantIntegrations(tenantId, pageLink);
            loadedIntegrations.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        integrations.sort(idComparator);
        loadedIntegrations.sort(idComparator);

        Assert.assertEquals(integrations, loadedIntegrations);

        integrationService.deleteIntegrationsByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = integrationService.findTenantIntegrations(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

}
