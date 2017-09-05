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
package org.thingsboard.server.dao.service;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.component.ComponentDescriptorService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.io.IOException;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AbstractServiceTest.class, loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan("org.thingsboard.server")
public abstract class AbstractServiceTest {

    protected ObjectMapper mapper = new ObjectMapper();

    @Autowired
    protected UserService userService;

    @Autowired
    protected AdminSettingsService adminSettingsService;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected TimeseriesService tsService;

    @Autowired
    protected PluginService pluginService;

    @Autowired
    protected RuleService ruleService;

    @Autowired
    protected EventService eventService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected AlarmService alarmService;

    @Autowired
    private ComponentDescriptorService componentDescriptorService;

    class IdComparator<D extends BaseData<? extends UUIDBased>> implements Comparator<D> {
        @Override
        public int compare(D o1, D o2) {
            return o1.getId().getId().compareTo(o2.getId().getId());
        }
    }


    protected Event generateEvent(TenantId tenantId, EntityId entityId, String eventType, String eventUid) throws IOException {
        if (tenantId == null) {
            tenantId = new TenantId(UUIDs.timeBased());
        }
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(eventType);
        event.setUid(eventUid);
        event.setBody(readFromResource("TestJsonData.json"));
        return event;
    }

    protected PluginMetaData generatePlugin(TenantId tenantId, String token) throws IOException {
        return generatePlugin(tenantId, token, "org.thingsboard.component.PluginTest", "org.thingsboard.component.ActionTest", "TestJsonDescriptor.json", "TestJsonData.json");
    }

    protected PluginMetaData generatePlugin(TenantId tenantId, String token, String clazz, String actions, String configurationDescriptorResource, String dataResource) throws IOException {
        if (tenantId == null) {
            tenantId = new TenantId(UUIDs.timeBased());
        }
        if (token == null) {
            token = UUID.randomUUID().toString();
        }
        getOrCreateDescriptor(ComponentScope.TENANT, ComponentType.PLUGIN, clazz, configurationDescriptorResource, actions);
        PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setName("Testing");
        pluginMetaData.setClazz(clazz);
        pluginMetaData.setTenantId(tenantId);
        pluginMetaData.setApiToken(token);
        pluginMetaData.setAdditionalInfo(mapper.readTree("{\"test\":\"test\"}"));
        try {
            pluginMetaData.setConfiguration(readFromResource(dataResource));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return pluginMetaData;
    }

    private ComponentDescriptor getOrCreateDescriptor(ComponentScope scope, ComponentType type, String clazz, String configurationDescriptorResource) throws IOException {
        return getOrCreateDescriptor(scope, type, clazz, configurationDescriptorResource, null);
    }

    private ComponentDescriptor getOrCreateDescriptor(ComponentScope scope, ComponentType type, String clazz, String configurationDescriptorResource, String actions) throws IOException {
        ComponentDescriptor descriptor = componentDescriptorService.findByClazz(clazz);
        if (descriptor == null) {
            descriptor = new ComponentDescriptor();
            descriptor.setName("test");
            descriptor.setClazz(clazz);
            descriptor.setScope(scope);
            descriptor.setType(type);
            descriptor.setActions(actions);
            descriptor.setConfigurationDescriptor(readFromResource(configurationDescriptorResource));
            componentDescriptorService.saveComponent(descriptor);
        }
        return descriptor;
    }

    public JsonNode readFromResource(String resourceName) throws IOException {
        return mapper.readTree(this.getClass().getClassLoader().getResourceAsStream(resourceName));
    }

    protected RuleMetaData generateRule(TenantId tenantId, Integer weight, String pluginToken) throws IOException {
        if (tenantId == null) {
            tenantId = new TenantId(UUIDs.timeBased());
        }
        if (weight == null) {
            weight = ThreadLocalRandom.current().nextInt();
        }

        RuleMetaData ruleMetaData = new RuleMetaData();
        ruleMetaData.setName("Testing");
        ruleMetaData.setTenantId(tenantId);
        ruleMetaData.setWeight(weight);
        ruleMetaData.setPluginToken(pluginToken);

        ruleMetaData.setAction(createNode(ComponentScope.TENANT, ComponentType.ACTION,
                "org.thingsboard.component.ActionTest", "TestJsonDescriptor.json", "TestJsonData.json"));
        ruleMetaData.setProcessor(createNode(ComponentScope.TENANT, ComponentType.PROCESSOR,
                "org.thingsboard.component.ProcessorTest", "TestJsonDescriptor.json", "TestJsonData.json"));
        ruleMetaData.setFilters(mapper.createArrayNode().add(
                createNode(ComponentScope.TENANT, ComponentType.FILTER,
                        "org.thingsboard.component.FilterTest", "TestJsonDescriptor.json", "TestJsonData.json")
        ));

        ruleMetaData.setAdditionalInfo(mapper.readTree("{}"));
        return ruleMetaData;
    }

    protected JsonNode createNode(ComponentScope scope, ComponentType type, String clazz, String configurationDescriptor, String configuration) throws IOException {
        getOrCreateDescriptor(scope, type, clazz, configurationDescriptor);
        ObjectNode oNode = mapper.createObjectNode();
        oNode.set("name", new TextNode("test action"));
        oNode.set("clazz", new TextNode(clazz));
        oNode.set("configuration", readFromResource(configuration));
        return oNode;
    }
}
