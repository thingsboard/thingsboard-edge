/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.tenant;

import com.datastax.driver.core.utils.UUIDs;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaTenantDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private TenantDao tenantDao;

    @Test
    @DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testFindTenantsByRegion() {
        createTenants();
        assertEquals(60, tenantDao.find().size());
        List<Tenant> tenants1 = tenantDao.findTenantsByRegion("REGION_1", new TextPageLink(20,"title"));
        assertEquals(20, tenants1.size());
        List<Tenant> tenants2 = tenantDao.findTenantsByRegion("REGION_1",
                new TextPageLink(20,"title", tenants1.get(19).getId().getId(), null));
        assertEquals(10, tenants2.size());
        List<Tenant> tenants3 = tenantDao.findTenantsByRegion("REGION_1",
                new TextPageLink(20,"title", tenants2.get(9).getId().getId(), null));
        assertEquals(0, tenants3.size());
    }

    private void createTenants() {
        for (int i = 0; i < 30; i++) {
            createTenant("REGION_1", "TITLE", i);
            createTenant("REGION_2", "TITLE", i);
        }
    }

    private void createTenant(String region, String title, int index) {
        Tenant tenant = new Tenant();
        tenant.setId(new TenantId(UUIDs.timeBased()));
        tenant.setRegion(region);
        tenant.setTitle(title + "_" + index);
        tenantDao.save(tenant);
    }

}
