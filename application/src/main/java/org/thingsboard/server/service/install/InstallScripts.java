/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.server.service.install.DatabaseHelper.objectMapper;

/**
 * Created by ashvayka on 18.04.18.
 */
@Component
@Slf4j
public class InstallScripts {

    public static final String APP_DIR = "application";
    public static final String SRC_DIR = "src";
    public static final String MAIN_DIR = "main";
    public static final String DATA_DIR = "data";
    public static final String JSON_DIR = "json";
    public static final String SYSTEM_DIR = "system";
    public static final String TENANT_DIR = "tenant";
    public static final String DEMO_DIR = "demo";
    public static final String RULE_CHAINS_DIR = "rule_chains";
    public static final String ROOT_RULE_CHAIN_DIR = "root_rule_chain";
    public static final String ROOT_RULE_CHAIN_JSON = "root_rule_chain.json";
    public static final String WIDGET_BUNDLES_DIR = "widget_bundles";
    public static final String DASHBOARDS_DIR = "dashboards";
    public static final String MAIL_TEMPLATES_DIR = "mail_templates";
    public static final String MAIL_TEMPLATES_JSON = "mail_templates.json";

    public static final String JSON_EXT = ".json";

    @Value("${install.data_dir:}")
    private String dataDir;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    public Path getTenantRuleChainsDir() {
        return Paths.get(getDataDir(), JSON_DIR, TENANT_DIR, RULE_CHAINS_DIR);
    }

    public Path getRootTenantRuleChainFile() {
        return Paths.get(getDataDir(), JSON_DIR, TENANT_DIR, ROOT_RULE_CHAIN_DIR, ROOT_RULE_CHAIN_JSON);
    }


    public String getDataDir() {
        if (!StringUtils.isEmpty(dataDir)) {
            if (!Paths.get(this.dataDir).toFile().isDirectory()) {
                throw new RuntimeException("'install.data_dir' property value is not a valid directory!");
            }
            return dataDir;
        } else {
            String workDir = System.getProperty("user.dir");
            if (workDir.endsWith("application")) {
                return Paths.get(workDir, SRC_DIR, MAIN_DIR, DATA_DIR).toString();
            } else {
                Path dataDirPath = Paths.get(workDir, APP_DIR, SRC_DIR, MAIN_DIR, DATA_DIR);
                if (Files.exists(dataDirPath)) {
                    return dataDirPath.toString();
                } else {
                    throw new RuntimeException("Not valid working directory: " + workDir + ". Please use either root project directory, application module directory or specify valid \"install.data_dir\" ENV variable to avoid automatic data directory lookup!");
                }
            }
        }
    }

    public void createDefaultRuleChains(TenantId tenantId) throws IOException {
        Path tenantChainsDir = getTenantRuleChainsDir();
        Map<String, RuleChainId> ruleChainIdMap = new HashMap<>();
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(tenantChainsDir, path -> path.toString().endsWith(InstallScripts.JSON_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            JsonNode ruleChainJson = objectMapper.readTree(path.toFile());
                            RuleChain ruleChain = loadRuleChain(path, ruleChainJson, tenantId);
                            ruleChainIdMap.put(ruleChain.getName(), ruleChain.getId());
                        } catch (Exception e) {
                            log.error("Unable to load rule chain from json: [{}]", path.toString());
                            throw new RuntimeException("Unable to load rule chain from json", e);
                        }
                    }
            );
        }
        Path rootRuleChainFile = getRootTenantRuleChainFile();
        String rootRuleChainContent = FileUtils.readFileToString(rootRuleChainFile.toFile(), "UTF-8");
        for (Map.Entry<String, RuleChainId> entry : ruleChainIdMap.entrySet()) {
            String key = "${" + entry.getKey() + "}";
            rootRuleChainContent = rootRuleChainContent.replace(key, entry.getValue().toString());
        }
        JsonNode rootRuleChainJson = objectMapper.readTree(rootRuleChainContent);
        loadRuleChain(rootRuleChainFile, rootRuleChainJson, tenantId);
    }

    private RuleChain loadRuleChain(Path path, JsonNode ruleChainJson, TenantId tenantId) {
        try {
            RuleChain ruleChain = objectMapper.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
            RuleChainMetaData ruleChainMetaData = objectMapper.treeToValue(ruleChainJson.get("metadata"), RuleChainMetaData.class);

            ruleChain.setTenantId(tenantId);
            ruleChain = ruleChainService.saveRuleChain(ruleChain);

            ruleChainMetaData.setRuleChainId(ruleChain.getId());
            ruleChainService.saveRuleChainMetaData(ruleChainMetaData);
            return ruleChain;
        } catch (Exception e) {
            log.error("Unable to load rule chain from json: [{}]", path.toString());
            throw new RuntimeException("Unable to load rule chain from json", e);
        }
    }

    public void loadSystemWidgets() throws Exception {
        Path widgetBundlesDir = Paths.get(getDataDir(), JSON_DIR, SYSTEM_DIR, WIDGET_BUNDLES_DIR);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(widgetBundlesDir, path -> path.toString().endsWith(JSON_EXT))) {
            dirStream.forEach(
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
    }

    public void loadDashboards(TenantId tenantId, CustomerId customerId) throws Exception {
        Path dashboardsDir = Paths.get(getDataDir(), JSON_DIR, DEMO_DIR, DASHBOARDS_DIR);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dashboardsDir, path -> path.toString().endsWith(JSON_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            JsonNode dashboardJson = objectMapper.readTree(path.toFile());
                            Dashboard dashboard = objectMapper.treeToValue(dashboardJson, Dashboard.class);
                            dashboard.setTenantId(tenantId);
                            Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
                            if (customerId != null && !customerId.isNullUid()) {
                                dashboardService.assignDashboardToCustomer(savedDashboard.getId(), customerId);
                            }
                        } catch (Exception e) {
                            log.error("Unable to load dashboard from json: [{}]", path.toString());
                            throw new RuntimeException("Unable to load dashboard from json", e);
                        }
                    }
            );
        }
    }

    public void loadMailTemplates() throws Exception {
        AdminSettings mailTemplateSettings = new AdminSettings();
        mailTemplateSettings.setKey("mailTemplates");
        Path mailTemplatesFile = Paths.get(getDataDir(), JSON_DIR, SYSTEM_DIR, MAIL_TEMPLATES_DIR, MAIL_TEMPLATES_JSON);
        JsonNode mailTemplatesJson = objectMapper.readTree(mailTemplatesFile.toFile());
        mailTemplateSettings.setJsonValue(mailTemplatesJson);
        adminSettingsService.saveAdminSettings(mailTemplateSettings);
    }

}
