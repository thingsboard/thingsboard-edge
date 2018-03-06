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
package org.thingsboard.server.dao.service.rule;

import com.datastax.driver.core.utils.UUIDs;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class BaseRuleServiceTest extends AbstractServiceTest {

    @Test
    public void saveRule() throws Exception {
        PluginMetaData plugin = generatePlugin(null, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(plugin);
        RuleMetaData ruleMetaData = ruleService.saveRule(generateRule(plugin.getTenantId(), null, plugin.getApiToken()));
        Assert.assertNotNull(ruleMetaData.getId());
        Assert.assertNotNull(ruleMetaData.getAdditionalInfo());
        ruleMetaData.setAdditionalInfo(mapper.readTree("{\"description\":\"test\"}"));
        RuleMetaData newRuleMetaData = ruleService.saveRule(ruleMetaData);
        Assert.assertEquals(ruleMetaData.getAdditionalInfo(), newRuleMetaData.getAdditionalInfo());
    }

    @Test
    public void findRuleById() throws Exception {
        PluginMetaData plugin = generatePlugin(null, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(plugin);

        RuleMetaData expected = ruleService.saveRule(generateRule(plugin.getTenantId(), null, plugin.getApiToken()));
        Assert.assertNotNull(expected.getId());
        RuleMetaData found = ruleService.findRuleById(expected.getId());
        Assert.assertEquals(expected, found);
    }

    @Test
    public void findPluginRules() throws Exception {
        TenantId tenantIdA = new TenantId(UUIDs.timeBased());
        TenantId tenantIdB = new TenantId(UUIDs.timeBased());

        PluginMetaData pluginA = generatePlugin(tenantIdA, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        PluginMetaData pluginB = generatePlugin(tenantIdB, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(pluginA);
        pluginService.savePlugin(pluginB);

        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));

        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));

        List<RuleMetaData> foundA = ruleService.findPluginRules(pluginA.getApiToken());
        Assert.assertEquals(3, foundA.size());

        List<RuleMetaData> foundB = ruleService.findPluginRules(pluginB.getApiToken());
        Assert.assertEquals(2, foundB.size());
    }

    @Test
    public void findSystemRules() throws Exception {
        TenantId systemTenant = new TenantId(ModelConstants.NULL_UUID); // system tenant id

        PluginMetaData plugin = generatePlugin(systemTenant, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(plugin);
        ruleService.saveRule(generateRule(systemTenant, null, plugin.getApiToken()));
        ruleService.saveRule(generateRule(systemTenant, null, plugin.getApiToken()));
        ruleService.saveRule(generateRule(systemTenant, null, plugin.getApiToken()));
        TextPageData<RuleMetaData> found = ruleService.findSystemRules(new TextPageLink(100));
        Assert.assertEquals(3, found.getData().size());
    }

    @Test
    public void findTenantRules() throws Exception {
        TenantId tenantIdA = new TenantId(UUIDs.timeBased());
        TenantId tenantIdB = new TenantId(UUIDs.timeBased());

        PluginMetaData pluginA = generatePlugin(tenantIdA, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        PluginMetaData pluginB = generatePlugin(tenantIdB, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(pluginA);
        pluginService.savePlugin(pluginB);

        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));

        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));

        TextPageData<RuleMetaData> foundA = ruleService.findTenantRules(tenantIdA, new TextPageLink(100));
        Assert.assertEquals(3, foundA.getData().size());

        TextPageData<RuleMetaData> foundB = ruleService.findTenantRules(tenantIdB, new TextPageLink(100));
        Assert.assertEquals(2, foundB.getData().size());
    }

    @Test
    public void deleteRuleById() throws Exception {
        PluginMetaData plugin = generatePlugin(null, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(plugin);

        RuleMetaData expected = ruleService.saveRule(generateRule(plugin.getTenantId(), null, plugin.getApiToken()));
        Assert.assertNotNull(expected.getId());
        RuleMetaData found = ruleService.findRuleById(expected.getId());
        Assert.assertEquals(expected, found);
        ruleService.deleteRuleById(expected.getId());
        found = ruleService.findRuleById(expected.getId());
        Assert.assertNull(found);
    }

    @Test
    public void deleteRulesByTenantId() throws Exception {
        TenantId tenantIdA = new TenantId(UUIDs.timeBased());
        TenantId tenantIdB = new TenantId(UUIDs.timeBased());

        PluginMetaData pluginA = generatePlugin(tenantIdA, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        PluginMetaData pluginB = generatePlugin(tenantIdB, "testPluginToken" + ThreadLocalRandom.current().nextInt());
        pluginService.savePlugin(pluginA);
        pluginService.savePlugin(pluginB);

        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdA, null, pluginA.getApiToken()));

        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));
        ruleService.saveRule(generateRule(tenantIdB, null, pluginB.getApiToken()));

        TextPageData<RuleMetaData> foundA = ruleService.findTenantRules(tenantIdA, new TextPageLink(100));
        Assert.assertEquals(3, foundA.getData().size());

        TextPageData<RuleMetaData> foundB = ruleService.findTenantRules(tenantIdB, new TextPageLink(100));
        Assert.assertEquals(2, foundB.getData().size());

        ruleService.deleteRulesByTenantId(tenantIdA);

        foundA = ruleService.findTenantRules(tenantIdA, new TextPageLink(100));
        Assert.assertEquals(0, foundA.getData().size());

        foundB = ruleService.findTenantRules(tenantIdB, new TextPageLink(100));
        Assert.assertEquals(2, foundB.getData().size());
    }
}