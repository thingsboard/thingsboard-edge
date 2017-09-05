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
package org.thingsboard.server.dao.sql.plugin;

import com.datastax.driver.core.utils.UUIDs;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.plugin.PluginDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 5/1/2017.
 */
public class JpaBasePluginDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private PluginDao pluginDao;

    @Test
    @DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testFindByTenantIdAndPageLink() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        createPluginsTwoTenants(tenantId1, tenantId2, "plugin_");
        List<PluginMetaData> rules1 = pluginDao.findByTenantIdAndPageLink(
                new TenantId(tenantId1), new TextPageLink(20, "plugin_"));
        assertEquals(20, rules1.size());

        List<PluginMetaData> rules2 = pluginDao.findByTenantIdAndPageLink(new TenantId(tenantId1),
                new TextPageLink(20, "plugin_", rules1.get(19).getId().getId(), null));
        assertEquals(10, rules2.size());

        List<PluginMetaData> rules3 = pluginDao.findByTenantIdAndPageLink(new TenantId(tenantId1),
                new TextPageLink(20, "plugin_", rules2.get(9).getId().getId(), null));
        assertEquals(0, rules3.size());
    }

    @Test
    @DatabaseSetup(value = "classpath:dbunit/empty_dataset.xml")
    public void testFindAllTenantRulesByTenantId() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        createTenantsAndSystemPlugins(tenantId1, tenantId2, "name_");
        List<PluginMetaData> rules1 = pluginDao.findAllTenantPluginsByTenantId(
                tenantId1, new TextPageLink(40, "name_"));
        assertEquals(40, rules1.size());

        List<PluginMetaData> rules2 = pluginDao.findAllTenantPluginsByTenantId(tenantId1,
                new TextPageLink(40, "name_", rules1.get(19).getId().getId(), null));
        assertEquals(20, rules2.size());

        List<PluginMetaData> rules3 = pluginDao.findAllTenantPluginsByTenantId(tenantId1,
                new TextPageLink(40, "name_", rules2.get(19).getId().getId(), null));
        assertEquals(0, rules3.size());
    }

    private void createTenantsAndSystemPlugins(UUID tenantId1, UUID tenantId2, String namePrefix) {
        for (int i = 0; i < 40; i++) {
            createPlugin(tenantId1, namePrefix, i);
            createPlugin(tenantId2, namePrefix, i);
            createPlugin(null, namePrefix, i);
        }
    }

    private void createPluginsTwoTenants(UUID tenantId1, UUID tenantId2, String namePrefix) {
        for (int i = 0; i < 30; i++) {
            createPlugin(tenantId1, namePrefix, i);
            createPlugin(tenantId2, namePrefix, i);
        }
    }

    private void createPlugin(UUID tenantId, String namePrefix, int i) {
        PluginMetaData plugin = new PluginMetaData();
        plugin.setId(new PluginId(UUIDs.timeBased()));
        plugin.setTenantId(new TenantId(tenantId));
        plugin.setName(namePrefix + i);
        pluginDao.save(plugin);
    }
}
