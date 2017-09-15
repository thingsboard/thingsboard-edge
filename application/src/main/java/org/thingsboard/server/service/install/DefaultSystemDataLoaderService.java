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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.dao.rule.RuleService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.service.mail.MailTemplates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Profile("install")
@Slf4j
public class DefaultSystemDataLoaderService implements SystemDataLoaderService {

    private static final String JSON_DIR = "json";
    private static final String SYSTEM_DIR = "system";
    private static final String DEMO_DIR = "demo";
    private static final String WIDGET_BUNDLES_DIR = "widget_bundles";
    private static final String PLUGINS_DIR = "plugins";
    private static final String RULES_DIR = "rules";
    private static final String DASHBOARDS_DIR = "dashboards";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${install.data_dir}")
    private String dataDir;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private PluginService pluginService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private DashboardService dashboardService;

    @Bean
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void createSysAdmin() {
        createUser(Authority.SYS_ADMIN, null, null, "sysadmin@thingsboard.org", "sysadmin");
    }

    @Override
    public void createAdminSettings() throws Exception {
        AdminSettings generalSettings = new AdminSettings();
        generalSettings.setKey("general");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("baseUrl", "http://localhost:8080");
        generalSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(generalSettings);

        AdminSettings mailSettings = new AdminSettings();
        mailSettings.setKey("mail");
        node = objectMapper.createObjectNode();
        node.put("mailFrom", "ThingsBoard <sysadmin@localhost.localdomain>");
        node.put("smtpProtocol", "smtp");
        node.put("smtpHost", "localhost");
        node.put("smtpPort", "25");
        node.put("timeout", "10000");
        node.put("enableTls", "false");
        node.put("username", "");
        node.put("password", "");
        mailSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(mailSettings);

        AdminSettings mailTemplateSettings = new AdminSettings();
        mailTemplateSettings.setKey("mailTemplates");
        node = MailTemplates.defaultMailTemplates;
        mailTemplateSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(mailTemplateSettings);
    }

    @Override
    public void loadSystemWidgets() throws Exception {
        Path widgetBundlesDir = Paths.get(dataDir, JSON_DIR, SYSTEM_DIR, WIDGET_BUNDLES_DIR);
        Files.newDirectoryStream(widgetBundlesDir, path -> path.toString().endsWith(".json"))
                .forEach(
                        path -> {
                            try {
                                JsonNode widgetsBundleDescriptorJson = objectMapper.readTree(path.toFile());
                                JsonNode widgetsBundleJson = widgetsBundleDescriptorJson.get("widgetsBundle");
                                WidgetsBundle widgetsBundle = objectMapper.treeToValue(widgetsBundleJson, WidgetsBundle.class);
                                WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
                                JsonNode widgetTypesArrayJson = widgetsBundleDescriptorJson.get("widgetTypes");
                                widgetTypesArrayJson.forEach(
                                        widgetTypeJson -> {
                                            try {
                                                WidgetType widgetType = objectMapper.treeToValue(widgetTypeJson, WidgetType.class);
                                                widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
                                                widgetTypeService.saveWidgetType(widgetType);
                                            } catch (Exception e) {
                                                log.error("Unable to load widget type from json: [{}]", path.toString());
                                                throw new RuntimeException("Unable to load widget type from json", e);
                                            }
                                        }
                                );
                            } catch (Exception e) {
                                log.error("Unable to load widgets bundle from json: [{}]", path.toString());
                                throw new RuntimeException("Unable to load widgets bundle from json", e);
                            }
                        }
                );
    }

    @Override
    public void loadSystemPlugins() throws Exception {
        loadPlugins(Paths.get(dataDir, JSON_DIR, SYSTEM_DIR, PLUGINS_DIR), null);
    }


    @Override
    public void loadSystemRules() throws Exception {
        loadRules(Paths.get(dataDir, JSON_DIR, SYSTEM_DIR, RULES_DIR), null);
    }

    @Override
    public void loadDemoData() throws Exception {
        Tenant demoTenant = new Tenant();
        demoTenant.setRegion("Global");
        demoTenant.setTitle("Tenant");
        demoTenant = tenantService.saveTenant(demoTenant);
        createUser(Authority.TENANT_ADMIN, demoTenant.getId(), null, "tenant@thingsboard.org", "tenant");

        Customer customerA = new Customer();
        customerA.setTenantId(demoTenant.getId());
        customerA.setTitle("Customer A");
        customerA = customerService.saveCustomer(customerA);
        Customer customerB = new Customer();
        customerB.setTenantId(demoTenant.getId());
        customerB.setTitle("Customer B");
        customerB = customerService.saveCustomer(customerB);
        Customer customerC = new Customer();
        customerC.setTenantId(demoTenant.getId());
        customerC.setTitle("Customer C");
        customerC = customerService.saveCustomer(customerC);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerA.getId(), "customer@thingsboard.org", "customer");
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerA.getId(), "customerA@thingsboard.org", "customer");
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerB.getId(), "customerB@thingsboard.org", "customer");
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerC.getId(), "customerC@thingsboard.org", "customer");

        createDevice(demoTenant.getId(), customerA.getId(), "default", "Test Device A1", "A1_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerA.getId(), "default", "Test Device A2", "A2_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerA.getId(), "default", "Test Device A3", "A3_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerB.getId(), "default", "Test Device B1", "B1_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerC.getId(), "default", "Test Device C1", "C1_TEST_TOKEN", null);

        createDevice(demoTenant.getId(), null, "default", "DHT11 Demo Device", "DHT11_DEMO_TOKEN", "Demo device that is used in sample " +
                "applications that upload data from DHT11 temperature and humidity sensor");

        createDevice(demoTenant.getId(), null, "default", "Raspberry Pi Demo Device", "RASPBERRY_PI_DEMO_TOKEN", "Demo device that is used in " +
                "Raspberry Pi GPIO control sample application");

        loadPlugins(Paths.get(dataDir, JSON_DIR, DEMO_DIR, PLUGINS_DIR), demoTenant.getId());
        loadRules(Paths.get(dataDir, JSON_DIR, DEMO_DIR, RULES_DIR), demoTenant.getId());
        loadDashboards(Paths.get(dataDir, JSON_DIR, DEMO_DIR, DASHBOARDS_DIR), demoTenant.getId(), null);
    }

    @Override
    public void deleteSystemWidgetBundle(String bundleAlias) throws Exception {
        WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(new TenantId(ModelConstants.NULL_UUID), bundleAlias);
        if (widgetsBundle != null) {
            widgetsBundleService.deleteWidgetsBundle(widgetsBundle.getId());
        }
    }

    private User createUser(Authority authority,
                            TenantId tenantId,
                            CustomerId customerId,
                            String email,
                            String password) {
        User user = new User();
        user.setAuthority(authority);
        user.setEmail(email);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user = userService.saveUser(user);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getId());
        userCredentials.setPassword(passwordEncoder.encode(password));
        userCredentials.setEnabled(true);
        userCredentials.setActivateToken(null);
        userService.saveUserCredentials(userCredentials);
        return user;
    }

    private Device createDevice(TenantId tenantId,
                                CustomerId customerId,
                                String type,
                                String name,
                                String accessToken,
                                String description) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setType(type);
        device.setName(name);
        if (description != null) {
            ObjectNode additionalInfo = objectMapper.createObjectNode();
            additionalInfo.put("description", description);
            device.setAdditionalInfo(additionalInfo);
        }
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getId());
        deviceCredentials.setCredentialsId(accessToken);
        deviceCredentialsService.updateDeviceCredentials(deviceCredentials);
        return device;
    }

    private void loadPlugins(Path pluginsDir, TenantId tenantId) throws Exception{
        Files.newDirectoryStream(pluginsDir, path -> path.toString().endsWith(".json"))
                .forEach(
                        path -> {
                            try {
                                JsonNode pluginJson = objectMapper.readTree(path.toFile());
                                PluginMetaData plugin = objectMapper.treeToValue(pluginJson, PluginMetaData.class);
                                plugin.setTenantId(tenantId);
                                if (plugin.getState() == ComponentLifecycleState.ACTIVE) {
                                    plugin.setState(ComponentLifecycleState.SUSPENDED);
                                    PluginMetaData savedPlugin = pluginService.savePlugin(plugin);
                                    pluginService.activatePluginById(savedPlugin.getId());
                                } else {
                                    pluginService.savePlugin(plugin);
                                }
                            } catch (Exception e) {
                                log.error("Unable to load plugin from json: [{}]", path.toString());
                                throw new RuntimeException("Unable to load plugin from json", e);
                            }
                        }
                );

    }

    private void loadRules(Path rulesDir, TenantId tenantId) throws Exception {
        Files.newDirectoryStream(rulesDir, path -> path.toString().endsWith(".json"))
                .forEach(
                        path -> {
                            try {
                                JsonNode ruleJson = objectMapper.readTree(path.toFile());
                                RuleMetaData rule = objectMapper.treeToValue(ruleJson, RuleMetaData.class);
                                rule.setTenantId(tenantId);
                                if (rule.getState() == ComponentLifecycleState.ACTIVE) {
                                    rule.setState(ComponentLifecycleState.SUSPENDED);
                                    RuleMetaData savedRule = ruleService.saveRule(rule);
                                    ruleService.activateRuleById(savedRule.getId());
                                } else {
                                    ruleService.saveRule(rule);
                                }
                            } catch (Exception e) {
                                log.error("Unable to load rule from json: [{}]", path.toString());
                                throw new RuntimeException("Unable to load rule from json", e);
                            }
                        }
                );
    }

    private void loadDashboards(Path dashboardsDir, TenantId tenantId, CustomerId customerId) throws Exception {
        Files.newDirectoryStream(dashboardsDir, path -> path.toString().endsWith(".json"))
                .forEach(
                        path -> {
                            try {
                                JsonNode dashboardJson = objectMapper.readTree(path.toFile());
                                Dashboard dashboard = objectMapper.treeToValue(dashboardJson, Dashboard.class);
                                dashboard.setTenantId(tenantId);
                                dashboard.setCustomerId(customerId);
                                dashboardService.saveDashboard(dashboard);
                            } catch (Exception e) {
                                log.error("Unable to load dashboard from json: [{}]", path.toString());
                                throw new RuntimeException("Unable to load dashboard from json", e);
                            }
                        }
                );
    }
}
