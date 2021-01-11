/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.integration;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.integration.IntegrationDao;

import java.util.Optional;
import java.util.UUID;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JpaIntegrationDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private IntegrationDao integrationDao;

    @Test
    public void testFindIntegrationsByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID converterId1 = Uuids.timeBased();
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
        String routingKey = RandomStringUtils.randomAlphanumeric(15);
        String routingKey2 = RandomStringUtils.randomAlphanumeric(15);
        saveIntegration(integrationId1, tenantId1, converterId1, "TEST_INTEGRATION", routingKey, IntegrationType.OCEANCONNECT);
        saveIntegration(integrationId2, tenantId2, converterId2, "TEST_INTEGRATION", routingKey2, IntegrationType.OCEANCONNECT);

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
        UUID converterId2 = Uuids.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID integrationId = Uuids.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            UUID converterId = i % 2 == 0 ? converterId1 : converterId2;
            saveIntegration(integrationId, tenantId, converterId, "INTEGRATION_" + i, RandomStringUtils.randomAlphanumeric(15),
                    IntegrationType.OCEANCONNECT);
        }
    }

    private void saveIntegration(UUID id, UUID tenantId, UUID converterId, String name, String routingKey, IntegrationType type) {
        Integration integration = new Integration();
        integration.setId(new IntegrationId(id));
        integration.setTenantId(new TenantId(tenantId));
        integration.setDefaultConverterId(new ConverterId(converterId));
        integration.setName(name);
        integration.setRoutingKey(routingKey);
        integration.setType(type);
        integrationDao.save(new TenantId(tenantId), integration);
    }
}
