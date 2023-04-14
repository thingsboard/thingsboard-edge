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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.integration.IntegrationDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JpaIntegrationDaoTest extends AbstractJpaDaoTest {

    List<Integration> savedIntegrations = new ArrayList<>();
    List<Converter> savedConverters = new ArrayList<>();

    @Autowired
    private IntegrationDao integrationDao;

    @Autowired
    private ConverterDao converterDao;

    @After
    public void tearDown() {
        savedIntegrations.forEach(integration -> integrationDao.removeById(integration.getTenantId(), integration.getUuidId()));
        savedIntegrations.clear();
        savedConverters.forEach(converter -> converterDao.removeById(converter.getTenantId(), converter.getUuidId()));
        savedConverters.clear();
    }

    @Test
    public void testFindIntegrationsByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID converterId1 = Uuids.timeBased();
        saveConverter(converterId1, tenantId1, "TEST_CONVERTER", ConverterType.UPLINK).getUuidId();
        saveTernary(tenantId1, converterId1);
        assertEquals(60, integrationDao.find(TenantId.SYS_TENANT_ID).size());

        PageLink pageLink = new PageLink(20, 0, "INTEGRATION_");
        PageData<Integration> integrations1 = integrationDao.findByTenantId(tenantId1, pageLink);
        assertEquals(20, integrations1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Integration> integrations2 = integrationDao.findByTenantId(tenantId1, pageLink);
        assertEquals(10, integrations2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Integration> integrations3 = integrationDao.findByTenantId(tenantId1, pageLink);
        assertEquals(0, integrations3.getData().size());
    }

    @Test
    public void testFindIntegrationByRoutingKey() {
        UUID integrationId1 = Uuids.timeBased();
        UUID integrationId2 = Uuids.timeBased();
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();
        UUID converterId1 = Uuids.timeBased();
        UUID converterId2 = Uuids.timeBased();
        saveConverter(converterId1, tenantId1, "TEST_CONVERTER_1", ConverterType.UPLINK).getUuidId();
        saveConverter(converterId2, tenantId1, "TEST_CONVERTER_2", ConverterType.UPLINK).getUuidId();

        String routingKey = StringUtils.randomAlphanumeric(15);
        String routingKey2 = StringUtils.randomAlphanumeric(15);
        savedIntegrations.add(saveIntegration(integrationId1, tenantId1, converterId1, "TEST_INTEGRATION", routingKey, IntegrationType.OCEANCONNECT));
        savedIntegrations.add(saveIntegration(integrationId2, tenantId2, converterId2, "TEST_INTEGRATION", routingKey2, IntegrationType.OCEANCONNECT));

        Optional<Integration> integrationOpt1 = integrationDao.findByRoutingKey(tenantId1, routingKey);
        assertTrue("Optional expected to be non-empty", integrationOpt1.isPresent());
        assertEquals(integrationId1, integrationOpt1.get().getId().getId());

        integrationOpt1 = integrationDao.findByRoutingKey(tenantId2, routingKey2);
        assertTrue("Optional expected to be non-empty", integrationOpt1.isPresent());
        assertEquals(integrationId2, integrationOpt1.get().getId().getId());

        Optional<Integration> integrationOpt2 = integrationDao.findByRoutingKey(tenantId1, "NON_EXISTENT_ROUTING_KEY");
        assertFalse("Optional expected to be empty", integrationOpt2.isPresent());
    }

    private void saveTernary(UUID tenantId1, UUID converterId1) {
        UUID tenantId2 = Uuids.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID integrationId = Uuids.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            UUID converterId = i % 2 == 0 ? converterId1 : saveConverter(Uuids.timeBased(), tenantId1, "TEST_CONVERTER_" + i, ConverterType.UPLINK).getUuidId();
            savedIntegrations.add(saveIntegration(integrationId, tenantId, converterId, "INTEGRATION_" + i, StringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT));
        }
    }

    private Integration saveIntegration(UUID id, UUID tenantId, UUID converterId, String name, String routingKey, IntegrationType type) {
        Integration integration = new Integration();
        integration.setId(new IntegrationId(id));
        integration.setTenantId(new TenantId(tenantId));
        integration.setDefaultConverterId(new ConverterId(converterId));
        integration.setName(name);
        integration.setRoutingKey(routingKey);
        integration.setType(type);
        return integrationDao.save(new TenantId(tenantId), integration);
    }

    private Converter saveConverter(UUID id, UUID tenantId, String name, ConverterType type) {
        Converter converter = new Converter();
        converter.setId(new ConverterId(id));
        converter.setTenantId(new TenantId(tenantId));
        converter.setName(name);
        converter.setType(type);
        Converter savedConverter = converterDao.save(new TenantId(tenantId), converter);
        savedConverters.add(savedConverter);
        return savedConverter;
    }
}
