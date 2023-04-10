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
package org.thingsboard.server.dao.sql.tenant;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.BaseTenantProfileServiceTest;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantProfileDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaTenantDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private TenantProfileDao tenantProfileDao;

    List<Tenant> createdTenants = new ArrayList<>();
    TenantProfile tenantProfile;

    @Before
    public void setUp() throws Exception {
        tenantProfile = tenantProfileDao.save(TenantId.SYS_TENANT_ID, BaseTenantProfileServiceTest.createTenantProfile("default tenant profile"));
        assertThat(tenantProfile).as("tenant profile").isNotNull();
    }

    @After
    public void tearDown() throws Exception {
        createdTenants.forEach((tenant)-> tenantDao.removeById(TenantId.SYS_TENANT_ID, tenant.getUuidId()));
        tenantProfileDao.removeById(TenantId.SYS_TENANT_ID, tenantProfile.getUuidId());
    }

    @Test
    //@DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testFindTenants() {
        createTenants();
        assertEquals(30, tenantDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());

        PageLink pageLink = new PageLink(20, 0, "title");
        PageData<Tenant> tenants1 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID, pageLink);
        assertEquals(20, tenants1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Tenant> tenants2 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID,
                pageLink);
        assertEquals(10, tenants2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Tenant> tenants3 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID,
                pageLink);
        assertEquals(0, tenants3.getData().size());
    }

    private void createTenants() {
        for (int i = 0; i < 30; i++) {
            createTenant("TITLE", i);
        }
    }

    void createTenant(String title, int index) {
        Tenant tenant = new Tenant();
        tenant.setId(TenantId.fromUUID(Uuids.timeBased()));
        tenant.setTitle(title + "_" + index);
        tenant.setTenantProfileId(tenantProfile.getId());
        createdTenants.add(tenantDao.save(TenantId.SYS_TENANT_ID, tenant));
    }

    @Test
    //@DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testIsExistsTenantById() {
        final UUID uuid = Uuids.timeBased();
        final TenantId tenantId = new TenantId(uuid);
        assertThat(tenantDao.existsById(tenantId, uuid)).as("Is tenant exists before save").isFalse();

        final Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTitle("Tenant " + uuid);
        tenant.setTenantProfileId(tenantProfile.getId());

        createdTenants.add(tenantDao.save(TenantId.SYS_TENANT_ID, tenant));

        assertThat(tenantDao.existsById(tenantId, uuid)).as("Is tenant exists after save").isTrue();

    }

}
