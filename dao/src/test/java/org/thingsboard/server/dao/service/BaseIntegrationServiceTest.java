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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.integration.IntegrationDao;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public abstract class BaseIntegrationServiceTest extends AbstractBeforeTest {

    private IdComparator<Integration> idComparator = new IdComparator<>();

    private TenantId tenantId;
    private ConverterId converterId;

    private final static String INTEGRATION_BASE_NAME = "INTEGRATION_";

    private static final JsonNode CUSTOM_CONVERTER_CONFIGURATION = new ObjectMapper()
            .createObjectNode().put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");

    private static final ObjectNode INTEGRATION_CONFIGURATION = new ObjectMapper()
            .createObjectNode();
    static {
        INTEGRATION_CONFIGURATION.putObject("metadata").put("key1", "val1");
    }

    @SpyBean
    IntegrationDao integrationDao;
    private List<Integration> savedIntegrations = new LinkedList<>();

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
        clearSavedIntegrations();
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveIntegration() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
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
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        integrationService.saveIntegration(integration);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveIntegrationWithInvalidTenant() {
        Integration integration = new Integration();
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
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
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setTenantId(tenantId);
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        integrationService.saveIntegration(integration);
    }

    @Test(expected = DataValidationException.class)
    public void testUpdateIntegrationType() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);
        savedIntegration.setType(IntegrationType.HTTP);
        integrationService.saveIntegration(savedIntegration);
    }

    @Test
    public void testFindIntegrationById() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
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
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
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
            integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
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

    @Test
    public void testFindIntegrationUseCache() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);

        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        verify(integrationDao, times(1)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);

        for (int i = 0; i < 10; i++) {
            foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
            verify(integrationDao, times(1)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
            Assert.assertNotNull(foundIntegration);
            Assert.assertEquals(savedIntegration, foundIntegration);
        }

        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());
    }

    @Test
    public void testSaveIntegrationEvictCache() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);

        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        verify(integrationDao, times(1)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);


        savedIntegration.setName("New name");
        integrationService.saveIntegration(savedIntegration);

        foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        verify(integrationDao, times(2)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);

        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());
    }

    @Test
    public void testDeleteIntegrationEvictCache() {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName("My integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setType(IntegrationType.OCEANCONNECT);
        integration.setConfiguration(INTEGRATION_CONFIGURATION);
        Integration savedIntegration = integrationService.saveIntegration(integration);

        Integration foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        verify(integrationDao, times(1)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
        Assert.assertNotNull(foundIntegration);
        Assert.assertEquals(savedIntegration, foundIntegration);

        integrationService.deleteIntegration(savedIntegration.getTenantId(), savedIntegration.getId());

        foundIntegration = integrationService.findIntegrationById(savedIntegration.getTenantId(), savedIntegration.getId());
        Assert.assertNull(foundIntegration);
        verify(integrationDao, times(2)).findById(eq(tenantId), eq(savedIntegration.getUuidId()));
    }

    @Test
    public void testDeleteIntegrationsByTenantIdEvictAllEntries() {
        final int integrationNumber = 10;
        List<Integration> savedIntegrations = new LinkedList<>();
        for (int i = 0; i < integrationNumber; i++) {
            Integration integration = new Integration();
            integration.setTenantId(tenantId);
            integration.setDefaultConverterId(converterId);
            integration.setName("My integration" + i);
            integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
            integration.setType(IntegrationType.OCEANCONNECT);
            integration.setConfiguration(INTEGRATION_CONFIGURATION);
            savedIntegrations.add(
                    integrationService.saveIntegration(integration)
            );
        }

        for (int i = 0; i < integrationNumber; i++) {
            Integration integration = savedIntegrations.get(i);
            Integration foundIntegration = integrationService.findIntegrationById(integration.getTenantId(), integration.getId());
            verify(integrationDao, times(1)).findById(eq(tenantId), eq(integration.getUuidId()));
            Assert.assertNotNull(foundIntegration);
            Assert.assertEquals(integration, foundIntegration);
        }

        integrationService.deleteIntegrationsByTenantId(tenantId);

        for (int i = 0; i < integrationNumber; i++) {
            Integration integration = savedIntegrations.get(i);
            Integration foundIntegration = integrationService.findIntegrationById(integration.getTenantId(), integration.getId());
            verify(integrationDao, times(2)).findById(eq(tenantId), eq(integration.getUuidId()));
            Assert.assertNull(foundIntegration);
        }
    }

    @Test
    public void testFindAllIntegrationInfos() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<IntegrationInfo> integrationInfos = integrationService
                .findAllCoreIntegrationInfos(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertNotEquals("List with integration infos expected, but list is empty!", 0, integrationInfos.size());
        Assert.assertEquals("List of found integration infos doesn't correspond the size of previously saved integrations!",
                numIntegrations, integrationInfos.size()
        );

        boolean allMatch = savedIntegrations.stream()
                .allMatch(integration ->
                        integrationInfos.stream()
                                .anyMatch(integrationInfo -> integrationInfo.getId().equals(integration.getId())
                                )
                );
        Assert.assertTrue("Found integration infos don't correspond the created integrations!", allMatch);
    }

    @Test
    public void testFindAllIntegrationInfosAfterRemovingIntegrationCase() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<IntegrationInfo> integrationInfos = integrationService
                .findAllCoreIntegrationInfos(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertNotEquals("List with integration infos expected, but list is empty!", 0, integrationInfos.size());
        Assert.assertEquals("List of found integration infos doesn't correspond the size of previously saved integrations!",
                numIntegrations, integrationInfos.size()
        );

        boolean allMatch = savedIntegrations.stream()
                .allMatch(integration ->
                        integrationInfos.stream()
                                .anyMatch(integrationInfo -> integrationInfo.getId().equals(integration.getId())
                                )
                );
        Assert.assertTrue("Found integration infos don't correspond the created integrations!", allMatch);
        clearSavedIntegrations();


        List<IntegrationInfo> emptyIntegrationInfos = integrationService
                .findAllCoreIntegrationInfos(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integration infos is null!", emptyIntegrationInfos);
        Assert.assertEquals("List with integration infos expected to be empty, but it's not!", 0, emptyIntegrationInfos.size());

    }

    @Test
    public void testFindAllIntegrationInfosTypeIsNullCase() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }


        List<IntegrationInfo> integrationInfos = integrationService
                .findAllCoreIntegrationInfos(null, false, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertEquals("List with integration infos expected to be empty, but it's not!", 0, integrationInfos.size());
    }

    @Test
    public void testFindAllIntegrationInfosDifferentTypesCase() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    i % 2 == 0 ? IntegrationType.OCEANCONNECT : IntegrationType.MQTT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<IntegrationInfo> integrationInfos = integrationService
                .findAllCoreIntegrationInfos(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertNotEquals("List with integration infos expected, but list is empty!", 0, integrationInfos.size());
        Assert.assertEquals("List of found integration infos doesn't correspond the size of previously saved integrations!",
                numIntegrations / 2, integrationInfos.size()
        );
        boolean allMatch = true;
        for (int i = 0; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allMatch &= integrationInfos.stream().anyMatch(integrationInfo -> integrationInfo.getId().equals(integration.getId()));
        }
        Assert.assertTrue("Found integration infos don't correspond the created integrations!", allMatch);
        boolean allTypeMatch = true;
        for (int i = 0; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allTypeMatch &= integration.getType().equals(IntegrationType.OCEANCONNECT);
        }
        Assert.assertTrue("Found integration infos have different type!", allTypeMatch);


        integrationInfos = integrationService
                .findAllCoreIntegrationInfos(IntegrationType.MQTT, false, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertNotEquals("List with integration infos expected, but list is empty!", 0, integrationInfos.size());
        Assert.assertEquals("List of found integration infos doesn't correspond the size of previously saved integrations!",
                numIntegrations / 2, integrationInfos.size()
        );
        for (int i = 1; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allMatch &= integrationInfos.stream().anyMatch(integrationInfo -> integrationInfo.getId().equals(integration.getId()));
        }
        Assert.assertTrue("Found integration infos don't correspond the created integrations!", allMatch);
        for (int i = 1; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allTypeMatch &= integration.getType().equals(IntegrationType.MQTT);
        }
        Assert.assertTrue("Found integration infos have different type!", allTypeMatch);


        integrationInfos = integrationService
                .findAllCoreIntegrationInfos(IntegrationType.HTTP, false, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertEquals("List with integration infos expected to be empty, but it's not!", 0, integrationInfos.size());
    }

    @Test
    public void testFindAllIntegrationInfosRemoteEnabledCase() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            boolean isOdd = i % 2 == 0;

            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    isOdd,
                    !isOdd
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<IntegrationInfo> integrationInfosRemote = integrationService
                .findAllCoreIntegrationInfos(IntegrationType.OCEANCONNECT, true, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfosRemote);
        Assert.assertNotEquals("List with integration infos expected, but list is empty!", 0, integrationInfosRemote.size());
        Assert.assertEquals("List of found integration infos doesn't correspond the size of previously saved integrations!",
                numIntegrations / 2, integrationInfosRemote.size()
        );
        boolean allMatch = true;
        for (int i = 0; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allMatch &= integrationInfosRemote.stream().anyMatch(integrationInfo -> integrationInfo.getId().equals(integration.getId()));
        }
        Assert.assertTrue("Found integration infos don't correspond the created integrations!", allMatch);
        boolean allRemote = integrationInfosRemote.stream()
                .allMatch(IntegrationInfo::isRemote);
        Assert.assertTrue("Found integration infos expected to be remote, but they aren't!", allRemote);


        List<IntegrationInfo> integrationInfosEnabled = integrationService
                .findAllCoreIntegrationInfos(IntegrationType.OCEANCONNECT, false, true);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfosRemote);
        Assert.assertNotEquals("List with integration infos expected, but list is empty!", 0, integrationInfosRemote.size());
        Assert.assertEquals("List of found integration infos doesn't correspond the size of previously saved integrations!",
                numIntegrations / 2, integrationInfosRemote.size()
        );
        for (int i = 1; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allMatch &= integrationInfosEnabled.stream().anyMatch(integrationInfo -> integrationInfo.getId().equals(integration.getId()));
        }
        Assert.assertTrue("Found integration infos don't correspond the created integrations!", allMatch);
        boolean allEnabled = integrationInfosEnabled.stream()
                .allMatch(IntegrationInfo::isEnabled);
        Assert.assertTrue("Found integration infos expected to be enabled, but they aren't!", allEnabled);


        List<IntegrationInfo> integrationInfos = integrationService
                .findAllCoreIntegrationInfos(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertEquals("List with integration infos expected to be empty, but it's not!", 0, integrationInfos.size());
    }

    @Test
    public void testFindAllIntegrationInfosDifferentTypeRemoteEnabledCase() {
        final int numIntegrations = 60;

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            boolean isOdd = i % 2 == 0;

            Integration integration = saveIntegration(
                    id, tenantId, converterId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    isOdd ? IntegrationType.OCEANCONNECT : IntegrationType.MQTT,
                    isOdd,
                    !isOdd
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<IntegrationInfo> integrationInfos = integrationService
                .findAllCoreIntegrationInfos(IntegrationType.OCEANCONNECT, true, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertNotEquals("List with integration infos expected, but list is empty!", 0, integrationInfos.size());
        Assert.assertEquals("List of found integration infos doesn't correspond the size of previously saved integrations!",
                numIntegrations / 2, integrationInfos.size()
        );
        boolean allMatch = true;
        for (int i = 0; i < savedIntegrations.size(); i += 2) {
            var integration = savedIntegrations.get(i);
            allMatch &= integrationInfos.stream().anyMatch(integrationInfo -> integrationInfo.getId().equals(integration.getId()));
        }
        Assert.assertTrue("Found integration infos don't correspond the created integrations!", allMatch);
        boolean allRemote = integrationInfos.stream()
                .allMatch(IntegrationInfo::isRemote);
        Assert.assertTrue("Found integration infos expected to be remote, but they aren't!", allRemote);
    }

    private Integration saveIntegration(UUID id, TenantId tenantId, ConverterId converterId, String name, String routingKey, IntegrationType type, boolean isRemote, boolean isEnabled) {
        Integration integration = new Integration();
        integration.setId(new IntegrationId(id));
        integration.setTenantId(tenantId);
        integration.setDefaultConverterId(converterId);
        integration.setName(name);
        integration.setRoutingKey(routingKey);
        integration.setType(type);
        integration.setRemote(isRemote);
        integration.setEnabled(isEnabled);
        return integrationService.saveIntegration(integration);
    }

    private void clearSavedIntegrations() {
        if (!savedIntegrations.isEmpty()) {
            savedIntegrations.forEach(integration ->
                    integrationService.deleteIntegration(
                            integration.getTenantId(),
                            integration.getId()
                    )
            );
            savedIntegrations.clear();
        }
    }

}
