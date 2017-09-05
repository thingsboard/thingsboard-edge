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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.extensions.core.plugin.telemetry.TelemetryStoragePlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BasePluginControllerTest extends AbstractControllerTest {

    private IdComparator<PluginMetaData> idComparator = new IdComparator<>();

    private final ObjectMapper mapper = new ObjectMapper();
    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSavePlugin() throws Exception {
        PluginMetaData plugin = new PluginMetaData();
        doPost("/api/plugin", plugin).andExpect(status().isBadRequest());
        plugin.setName("My plugin");
        doPost("/api/plugin", plugin).andExpect(status().isBadRequest());
        plugin.setApiToken("myplugin");
        doPost("/api/plugin", plugin).andExpect(status().isBadRequest());
        plugin.setConfiguration(mapper.readTree("{}"));
        doPost("/api/plugin", plugin).andExpect(status().isBadRequest());
        plugin.setClazz(TelemetryStoragePlugin.class.getName());
        PluginMetaData savedPlugin = doPost("/api/plugin", plugin, PluginMetaData.class);

        Assert.assertNotNull(savedPlugin);
        Assert.assertNotNull(savedPlugin.getId());
        Assert.assertTrue(savedPlugin.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedPlugin.getTenantId());
    }

    @Test
    public void testFindPluginById() throws Exception {
        PluginMetaData plugin = new PluginMetaData();
        plugin.setName("My plugin");
        plugin.setApiToken("myplugin");
        plugin.setConfiguration(mapper.readTree("{}"));
        plugin.setClazz(TelemetryStoragePlugin.class.getName());

        PluginMetaData savedPlugin = doPost("/api/plugin", plugin, PluginMetaData.class);
        PluginMetaData foundPlugin = doGet("/api/plugin/" + savedPlugin.getId().getId().toString(), PluginMetaData.class);
        Assert.assertNotNull(foundPlugin);
        Assert.assertEquals(savedPlugin, foundPlugin);
    }

    @Test
    public void testActivatePlugin() throws Exception {
        PluginMetaData plugin = new PluginMetaData();
        plugin.setName("My plugin");
        plugin.setApiToken("myplugin");
        plugin.setConfiguration(mapper.readTree("{}"));
        plugin.setClazz(TelemetryStoragePlugin.class.getName());

        PluginMetaData savedPlugin = doPost("/api/plugin", plugin, PluginMetaData.class);

        doPost("/api/plugin/" + savedPlugin.getId().getId().toString() + "/activate").andExpect(status().isOk());
    }

    @Test
    public void testSuspendPlugin() throws Exception {
        PluginMetaData plugin = new PluginMetaData();
        plugin.setName("My plugin");
        plugin.setApiToken("myplugin");
        plugin.setConfiguration(mapper.readTree("{}"));
        plugin.setClazz(TelemetryStoragePlugin.class.getName());

        PluginMetaData savedPlugin = doPost("/api/plugin", plugin, PluginMetaData.class);

        doPost("/api/plugin/" + savedPlugin.getId().getId().toString() + "/activate").andExpect(status().isOk());

        RuleMetaData rule = BaseRuleControllerTest.createRuleMetaData(savedPlugin);
        RuleMetaData savedRule = doPost("/api/rule", rule, RuleMetaData.class);
        doPost("/api/rule/" + savedRule.getId().getId().toString() + "/activate").andExpect(status().isOk());

        doPost("/api/plugin/" + savedPlugin.getId().getId().toString() + "/suspend").andExpect(status().isBadRequest());

        doPost("/api/rule/" + savedRule.getId().getId().toString() + "/suspend").andExpect(status().isOk());

        doPost("/api/plugin/" + savedPlugin.getId().getId().toString() + "/suspend").andExpect(status().isOk());
    }

    @Test
    public void testDeletePluginById() throws Exception {
        PluginMetaData plugin = new PluginMetaData();
        plugin.setName("My plugin");
        plugin.setApiToken("myplugin");
        plugin.setConfiguration(mapper.readTree("{}"));
        plugin.setClazz(TelemetryStoragePlugin.class.getName());

        PluginMetaData savedPlugin = doPost("/api/plugin", plugin, PluginMetaData.class);

        RuleMetaData rule = BaseRuleControllerTest.createRuleMetaData(savedPlugin);
        RuleMetaData savedRule = doPost("/api/rule", rule, RuleMetaData.class);

        doDelete("/api/plugin/" + savedPlugin.getId().getId()).andExpect(status().isBadRequest());

        doDelete("/api/rule/" + savedRule.getId().getId()).andExpect(status().isOk());

        doDelete("/api/plugin/" + savedPlugin.getId().getId()).andExpect(status().isOk());
        doGet("/api/plugin/" + savedPlugin.getId().getId().toString()).andExpect(status().isNotFound());
    }

    @Test
    public void testFindPluginByToken() throws Exception {
        PluginMetaData plugin = new PluginMetaData();
        plugin.setName("My plugin");
        plugin.setApiToken("myplugin");
        plugin.setConfiguration(mapper.readTree("{}"));
        plugin.setClazz(TelemetryStoragePlugin.class.getName());

        PluginMetaData savedPlugin = doPost("/api/plugin", plugin, PluginMetaData.class);
        PluginMetaData foundPlugin = doGet("/api/plugin/token/" + "myplugin", PluginMetaData.class);
        Assert.assertNotNull(foundPlugin);
        Assert.assertEquals(savedPlugin, foundPlugin);
    }

    @Test
    public void testFindCurrentTenantPlugins() throws Exception {
        List<PluginMetaData> plugins = testPluginsCreation("/api/plugin");
        for (PluginMetaData plugin : plugins) {
            doDelete("/api/plugin/" + plugin.getId().getId()).andExpect(status().isOk());
        }
    }

    @Test
    public void testFindSystemPlugins() throws Exception {
        loginSysAdmin();
        List<PluginMetaData> plugins = testPluginsCreation("/api/plugin/system");
        for (PluginMetaData plugin : plugins) {
            doDelete("/api/plugin/" + plugin.getId().getId()).andExpect(status().isOk());
        }
    }

    private List<PluginMetaData> testPluginsCreation(String url) throws Exception {
        List<PluginMetaData> plugins = new ArrayList<>();
        for (int i = 0; i < 111; i++) {
            PluginMetaData plugin = new PluginMetaData();
            plugin.setName("My plugin");
            plugin.setApiToken("myplugin" + i);
            plugin.setConfiguration(mapper.readTree("{}"));
            plugin.setClazz(TelemetryStoragePlugin.class.getName());
            plugins.add(doPost("/api/plugin", plugin, PluginMetaData.class));
        }

        List<PluginMetaData> loadedPlugins = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<PluginMetaData> pageData;
        do {
            pageData = doGetTypedWithPageLink(url + "?",
                    new TypeReference<TextPageData<PluginMetaData>>() {
                    }, pageLink);
            loadedPlugins.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        loadedPlugins = loadedPlugins.stream()
                .filter(p -> !p.getName().equals("System Telemetry Plugin"))
                .filter(p -> !p.getName().equals("Mail Sender Plugin"))
                .filter(p -> !p.getName().equals("System RPC Plugin"))
                .collect(Collectors.toList());

        Collections.sort(plugins, idComparator);
        Collections.sort(loadedPlugins, idComparator);

        Assert.assertEquals(plugins, loadedPlugins);
        return loadedPlugins;
    }
}
