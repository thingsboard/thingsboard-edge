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
package org.thingsboard.server.dao.sql.integration;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.integration.IntegrationDao;
import org.thingsboard.server.dao.integration.IntegrationInfoDao;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class JpaIntegrationInfoDaoTest extends AbstractJpaDaoTest {

    private final static String INTEGRATION_BASE_NAME = "INTEGRATION_";

    List<Integration> savedIntegrations = new LinkedList<>();

    List<Converter> savedConverters = new ArrayList<>();

    @Autowired
    private IntegrationInfoDao integrationInfoDao;

    @Autowired
    private IntegrationDao integrationDao;

    @Autowired
    private ConverterDao converterDao;

    @After
    public void tearDown() {
        clearSavedIntegrations();
        clearSavedConverters();
    }

    @Test
    public void testFindAllIntegrationInfos() {
        final int numIntegrations = 60;
        UUID tenantId = Uuids.timeBased();
        UUID convId = Uuids.timeBased();
        saveConverter(convId, tenantId);

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, convId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<IntegrationInfo> integrationInfos = integrationInfoDao
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
        UUID tenantId = Uuids.timeBased();
        UUID convId = Uuids.timeBased();
        saveConverter(convId, tenantId);

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, convId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<IntegrationInfo> integrationInfos = integrationInfoDao
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


        List<IntegrationInfo> emptyIntegrationInfos = integrationInfoDao
                .findAllCoreIntegrationInfos(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integration infos is null!", emptyIntegrationInfos);
        Assert.assertEquals("List with integration infos expected to be empty, but it's not!", 0, emptyIntegrationInfos.size());

    }

    @Test
    public void testFindAllIntegrationInfosTypeIsNullCase() {
        final int numIntegrations = 60;
        UUID tenantId = Uuids.timeBased();
        UUID convId = Uuids.timeBased();
        saveConverter(convId, tenantId);

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, convId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }


        List<IntegrationInfo> integrationInfos = integrationInfoDao
                .findAllCoreIntegrationInfos(null, false, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertEquals("List with integration infos expected to be empty, but it's not!", 0, integrationInfos.size());
    }

    @Test
    public void testFindAllIntegrationInfosDifferentTypesCase() {
        final int numIntegrations = 60;
        UUID tenantId = Uuids.timeBased();
        UUID convId = Uuids.timeBased();
        saveConverter(convId, tenantId);

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            Integration integration = saveIntegration(
                    id, tenantId, convId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    i % 2 == 0 ? IntegrationType.OCEANCONNECT : IntegrationType.MQTT,
                    false,
                    false
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<IntegrationInfo> integrationInfos = integrationInfoDao
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


        integrationInfos = integrationInfoDao
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


        integrationInfos = integrationInfoDao
                .findAllCoreIntegrationInfos(IntegrationType.HTTP, false, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertEquals("List with integration infos expected to be empty, but it's not!", 0, integrationInfos.size());
    }

    @Test
    public void testFindAllIntegrationInfosRemoteEnabledCase() {
        final int numIntegrations = 60;
        UUID tenantId = Uuids.timeBased();
        UUID convId = Uuids.timeBased();
        saveConverter(convId, tenantId);

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            boolean isOdd = i % 2 == 0;

            Integration integration = saveIntegration(
                    id, tenantId, convId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT,
                    isOdd,
                    !isOdd
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<IntegrationInfo> integrationInfosRemote = integrationInfoDao
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


        List<IntegrationInfo> integrationInfosEnabled = integrationInfoDao
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


        List<IntegrationInfo> integrationInfos = integrationInfoDao
                .findAllCoreIntegrationInfos(IntegrationType.OCEANCONNECT, false, false);
        Assert.assertNotNull("List of found integration infos is null!", integrationInfos);
        Assert.assertEquals("List with integration infos expected to be empty, but it's not!", 0, integrationInfos.size());
    }

    @Test
    public void testFindAllIntegrationInfosDifferentTypeRemoteEnabledCase() {
        final int numIntegrations = 60;
        UUID tenantId = Uuids.timeBased();
        UUID convId = Uuids.timeBased();
        saveConverter(convId, tenantId);

        for (int i = 0; i < numIntegrations; i++) {
            UUID id = Uuids.timeBased();
            boolean isOdd = i % 2 == 0;

            Integration integration = saveIntegration(
                    id, tenantId, convId,
                    INTEGRATION_BASE_NAME + i,
                    StringUtils.randomAlphanumeric(15),
                    isOdd ? IntegrationType.OCEANCONNECT : IntegrationType.MQTT,
                    isOdd,
                    !isOdd
            );
            Assert.assertNotNull("Saved integration is null!", integration);
            savedIntegrations.add(integration);
        }

        List<IntegrationInfo> integrationInfos = integrationInfoDao
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

    private Integration saveIntegration(UUID id, UUID tenantId, UUID converterId, String name, String routingKey, IntegrationType type, boolean isRemote, boolean isEnabled) {
        Integration integration = new Integration();
        integration.setId(new IntegrationId(id));
        integration.setTenantId(new TenantId(tenantId));
        integration.setDefaultConverterId(new ConverterId(converterId));
        integration.setName(name);
        integration.setRoutingKey(routingKey);
        integration.setType(type);
        integration.setRemote(isRemote);
        integration.setEnabled(isEnabled);
        return integrationDao.save(new TenantId(tenantId), integration);
    }

    private void saveConverter(UUID id, UUID tenantId) {
        Converter converter = new Converter();
        converter.setId(new ConverterId(id));
        converter.setTenantId(new TenantId(tenantId));
        converter.setName("CONVERTER");
        converter.setType(ConverterType.UPLINK);
        savedConverters.add(converterDao.save(new TenantId(tenantId), converter));
    }

    private void clearSavedIntegrations() {
        if (!savedIntegrations.isEmpty()) {
            savedIntegrations.forEach(integration ->
                    integrationDao.removeById(
                            integration.getTenantId(),
                            integration.getUuidId()
                    )
            );
            savedIntegrations.clear();
        }
    }

    private void clearSavedConverters() {
        savedConverters.forEach(converter -> converterDao.removeById(converter.getTenantId(), converter.getUuidId()));
        savedIntegrations.clear();
    }
}
