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
package org.thingsboard.server.dao.sql.integration;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.integration.IntegrationDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JpaIntegrationDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private IntegrationDao integrationDao;

    @Test
    public void testFindIntegrationsByTenantId() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID converterId1 = UUIDs.timeBased();
        saveTernary(tenantId1, converterId1);
        assertEquals(60, integrationDao.find().size());

        TextPageLink pageLink1 = new TextPageLink(20, "INTEGRATION_");
        List<Integration> integrations1 = integrationDao.findIntegrationsByTenantId(tenantId1, pageLink1);
        assertEquals(20, integrations1.size());

        TextPageLink pageLink2 = new TextPageLink(20, "INTEGRATION_", integrations1.get(19).getId().getId(), null);
        List<Integration> integrations2 = integrationDao.findIntegrationsByTenantId(tenantId1, pageLink2);
        assertEquals(10, integrations2.size());

        TextPageLink pageLink3 = new TextPageLink(20, "INTEGRATION_", integrations2.get(9).getId().getId(), null);
        List<Integration> integrations3 = integrationDao.findIntegrationsByTenantId(tenantId1, pageLink3);
        assertEquals(0, integrations3.size());
    }

    @Test
    public void testFindIntegrationsByTenantIdAndConverterId() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID converterId1 = UUIDs.timeBased();
        saveTernary(tenantId1, converterId1);

        TextPageLink pageLink1 = new TextPageLink(20, "INTEGRATION_");
        List<Integration> integrations1 = integrationDao.findIntegrationsByTenantIdAndConverterId(tenantId1, converterId1, pageLink1);
        assertEquals(20, integrations1.size());

        TextPageLink pageLink2 = new TextPageLink(20, "INTEGRATION_", integrations1.get(19).getId().getId(), null);
        List<Integration> integrations2 = integrationDao.findIntegrationsByTenantIdAndConverterId(tenantId1, converterId1, pageLink2);
        assertEquals(10, integrations2.size());

        TextPageLink pageLink3 = new TextPageLink(20, "INTEGRATION_", integrations2.get(9).getId().getId(), null);
        List<Integration> integrations3 = integrationDao.findIntegrationsByTenantIdAndConverterId(tenantId1, converterId1, pageLink3);
        assertEquals(0, integrations3.size());
    }

    @Test
    public void testFindIntegrationsByTenantIdAndIdsAsync() throws ExecutionException, InterruptedException {
        UUID tenantId = UUIDs.timeBased();
        UUID converterId = UUIDs.timeBased();
        List<UUID> searchIds = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            UUID integrationId = UUIDs.timeBased();
            saveIntegration(integrationId, tenantId, converterId, "INTEGRATION_" + i, IntegrationType.OCEANCONNECT);
            if (i % 3 == 0) {
                searchIds.add(integrationId);
            }
        }

        ListenableFuture<List<Integration>> integrationsFuture = integrationDao
                .findIntegrationsByTenantIdAndIdsAsync(tenantId, searchIds);
        List<Integration> integrations = integrationsFuture.get();
        assertNotNull(integrations);
        assertEquals(10, integrations.size());
    }

    @Test
    public void testFindIntegrationsByTenantIdConverterIdAndIdsAsync() throws ExecutionException, InterruptedException {
        UUID tenantId = UUIDs.timeBased();
        UUID converterId1 = UUIDs.timeBased();
        UUID converterId2 = UUIDs.timeBased();
        List<UUID> searchIds = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            UUID integrationId = UUIDs.timeBased();
            UUID converterId = i % 2 == 0 ? converterId1 : converterId2;
            saveIntegration(integrationId, tenantId, converterId, "INTEGRATION_" + i, IntegrationType.OCEANCONNECT);
            if (i % 3 == 0) {
                searchIds.add(integrationId);
            }
        }

        ListenableFuture<List<Integration>> integrationsFuture = integrationDao
                .findIntegrationsByTenantIdAndConverterIdAndIdsAsync(tenantId, converterId1, searchIds);
        List<Integration> integrations = integrationsFuture.get();
        assertNotNull(integrations);
        assertEquals(5, integrations.size());
    }

    @Test
    public void testFindIntegrationsByTenantIdAndRoutingKey() {
        UUID integrationId1 = UUIDs.timeBased();
        UUID integrationId2 = UUIDs.timeBased();
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        UUID converterId1 = UUIDs.timeBased();
        UUID converterId2 = UUIDs.timeBased();
        String routingKey = "TEST_INTEGRATION";
        saveIntegration(integrationId1, tenantId1, converterId1, routingKey, IntegrationType.OCEANCONNECT);
        saveIntegration(integrationId2, tenantId2, converterId2, routingKey, IntegrationType.OCEANCONNECT);

        Optional<Integration> integrationOpt1 = integrationDao.findIntegrationsByTenantIdAndRoutingKey(tenantId2, routingKey);
        assertTrue("Optional expected to be non-empty", integrationOpt1.isPresent());
        assertEquals(integrationId2, integrationOpt1.get().getId().getId());

        Optional<Integration> integrationOpt2 = integrationDao.findIntegrationsByTenantIdAndRoutingKey(tenantId2, "NON_EXISTENT_ROUTING_KEY");
        assertFalse("Optional expected to be empty", integrationOpt2.isPresent());
    }

    @Test
    public void testFindTenantIntegrationTypesAsync() throws ExecutionException, InterruptedException {
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        UUID converterId1 = UUIDs.timeBased();
        UUID converterId2 = UUIDs.timeBased();
        saveIntegration(UUIDs.timeBased(), tenantId1, converterId1, "TEST_INTEGRATION_1", IntegrationType.OCEANCONNECT);
        saveIntegration(UUIDs.timeBased(), tenantId1, converterId1, "TEST_INTEGRATION_2", IntegrationType.OCEANCONNECT);
        saveIntegration(UUIDs.timeBased(), tenantId1, converterId1, "TEST_INTEGRATION_3", IntegrationType.SIGFOX);
        saveIntegration(UUIDs.timeBased(), tenantId1, converterId1, "TEST_INTEGRATION_4", IntegrationType.SIGFOX);
        saveIntegration(UUIDs.timeBased(), tenantId1, converterId1, "TEST_INTEGRATION_5", IntegrationType.SIGFOX);

        saveIntegration(UUIDs.timeBased(), tenantId2, converterId2, "TEST_INTEGRATION_6", IntegrationType.OCEANCONNECT);
        saveIntegration(UUIDs.timeBased(), tenantId2, converterId2, "TEST_INTEGRATION_7", IntegrationType.OCEANCONNECT);

        List<EntitySubtype> tenant1Types = integrationDao.findTenantIntegrationTypesAsync(tenantId1).get();
        assertNotNull(tenant1Types);
        List<EntitySubtype> tenant2Types = integrationDao.findTenantIntegrationTypesAsync(tenantId2).get();
        assertNotNull(tenant2Types);

        assertEquals(2, tenant1Types.size());
        assertTrue(tenant1Types.stream().anyMatch(t -> t.getType().equals(IntegrationType.OCEANCONNECT.toString())));
        assertTrue(tenant1Types.stream().anyMatch(t -> t.getType().equals(IntegrationType.SIGFOX.toString())));
        assertFalse(tenant1Types.stream().anyMatch(t -> t.getType().equals("TYPE_1")));

        assertEquals(1, tenant2Types.size());
        assertTrue(tenant2Types.stream().anyMatch(t -> t.getType().equals(IntegrationType.OCEANCONNECT.toString())));
        assertFalse(tenant2Types.stream().anyMatch(t -> t.getType().equals("TYPE_2")));
    }

    private void saveTernary(UUID tenantId1, UUID converterId1) {
        UUID tenantId2 = UUIDs.timeBased();
        UUID converterId2 = UUIDs.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID integrationId = UUIDs.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            UUID converterId = i % 2 == 0 ? converterId1 : converterId2;
            saveIntegration(integrationId, tenantId, converterId, "INTEGRATION_" + i, IntegrationType.OCEANCONNECT);
        }
    }

    private void saveIntegration(UUID id, UUID tenantId, UUID converterId, String routingKey, IntegrationType type) {
        Integration integration = new Integration();
        integration.setId(new IntegrationId(id));
        integration.setTenantId(new TenantId(tenantId));
        integration.setDefaultConverterId(new ConverterId(converterId));
        integration.setRoutingKey(routingKey);
        integration.setType(type);
        integrationDao.save(integration);
    }
}
