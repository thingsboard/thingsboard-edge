/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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

import com.datastax.driver.core.utils.UUIDs;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.integration.IntegrationDao;

import java.util.List;
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
        UUID tenantId1 = UUIDs.timeBased();
        UUID converterId1 = UUIDs.timeBased();
        saveTernary(tenantId1, converterId1);
        assertEquals(60, integrationDao.find().size());

        TextPageLink pageLink1 = new TextPageLink(20, "INTEGRATION_");
        List<Integration> integrations1 = integrationDao.findByTenantIdAndPageLink(tenantId1, pageLink1);
        assertEquals(20, integrations1.size());

        TextPageLink pageLink2 = new TextPageLink(20, "INTEGRATION_", integrations1.get(19).getId().getId(), null);
        List<Integration> integrations2 = integrationDao.findByTenantIdAndPageLink(tenantId1, pageLink2);
        assertEquals(10, integrations2.size());

        TextPageLink pageLink3 = new TextPageLink(20, "INTEGRATION_", integrations2.get(9).getId().getId(), null);
        List<Integration> integrations3 = integrationDao.findByTenantIdAndPageLink(tenantId1, pageLink3);
        assertEquals(0, integrations3.size());
    }

    @Test
    public void testFindIntegrationByRoutingKey() {
        UUID integrationId1 = UUIDs.timeBased();
        UUID integrationId2 = UUIDs.timeBased();
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        UUID converterId1 = UUIDs.timeBased();
        UUID converterId2 = UUIDs.timeBased();
        String routingKey = RandomStringUtils.randomAlphanumeric(15);
        String routingKey2 = RandomStringUtils.randomAlphanumeric(15);
        saveIntegration(integrationId1, tenantId1, converterId1, "TEST_INTEGRATION", routingKey, IntegrationType.OCEANCONNECT);
        saveIntegration(integrationId2, tenantId2, converterId2, "TEST_INTEGRATION", routingKey2, IntegrationType.OCEANCONNECT);

        Optional<Integration> integrationOpt1 = integrationDao.findByRoutingKey(routingKey);
        assertTrue("Optional expected to be non-empty", integrationOpt1.isPresent());
        assertEquals(integrationId1, integrationOpt1.get().getId().getId());

        integrationOpt1 = integrationDao.findByRoutingKey(routingKey2);
        assertTrue("Optional expected to be non-empty", integrationOpt1.isPresent());
        assertEquals(integrationId2, integrationOpt1.get().getId().getId());

        Optional<Integration> integrationOpt2 = integrationDao.findByRoutingKey("NON_EXISTENT_ROUTING_KEY");
        assertFalse("Optional expected to be empty", integrationOpt2.isPresent());
    }

    private void saveTernary(UUID tenantId1, UUID converterId1) {
        UUID tenantId2 = UUIDs.timeBased();
        UUID converterId2 = UUIDs.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID integrationId = UUIDs.timeBased();
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
        integrationDao.save(integration);
    }
}
