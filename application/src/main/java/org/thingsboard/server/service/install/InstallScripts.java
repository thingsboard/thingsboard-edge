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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.exception.DataValidationException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.thingsboard.server.service.install.DatabaseHelper.objectMapper;
import static org.thingsboard.server.utils.LwM2mObjectModelUtils.toLwm2mResource;

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
    public static final String DEVICE_PROFILE_DIR = "device_profile";
    public static final String DEMO_DIR = "demo";
    public static final String RULE_CHAINS_DIR = "rule_chains";
    public static final String ROOT_RULE_CHAIN_DIR = "root_rule_chain";
    public static final String ROOT_RULE_CHAIN_JSON = "root_rule_chain.json";
    public static final String WIDGET_BUNDLES_DIR = "widget_bundles";
    public static final String OAUTH2_CONFIG_TEMPLATES_DIR = "oauth2_config_templates";
    public static final String DASHBOARDS_DIR = "dashboards";
    public static final String MAIL_TEMPLATES_DIR = "mail_templates";
    public static final String MAIL_TEMPLATES_JSON = "mail_templates.json";
    public static final String CREDENTIALS_DIR = "credentials";
    public static final String EDGE_MANAGEMENT = "edge_management";
    public static final String MODELS_LWM2M_DIR = "lwm2m-registry";

    public static final String JSON_EXT = ".json";
    public static final String XML_EXT = ".xml";

    private static final Pattern velocityVarPattern = Pattern.compile("\\$([a-zA-Z]+)");
    private static final String velocityVarToFreeMakerReplacementPattern = "\\${$1}";

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

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private OAuth2ConfigTemplateService oAuth2TemplateService;

    @Autowired
    private ResourceService resourceService;

    private Path getTenantRuleChainsDir() {
        return Paths.get(getDataDir(), JSON_DIR, TENANT_DIR, RULE_CHAINS_DIR);
    }

    public Path getRootTenantRuleChainFile() {
        return Paths.get(getDataDir(), JSON_DIR, TENANT_DIR, ROOT_RULE_CHAIN_DIR, ROOT_RULE_CHAIN_JSON);
    }

    private Path getDeviceProfileDefaultRuleChainTemplateFilePath() {
        return Paths.get(getDataDir(), JSON_DIR, TENANT_DIR, DEVICE_PROFILE_DIR, "rule_chain_template.json");
    }

    private Path getEdgeRuleChainsDir() {
        return Paths.get(getDataDir(), JSON_DIR, TENANT_DIR, EDGE_MANAGEMENT, RULE_CHAINS_DIR);
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
        Map<String, RuleChainId> ruleChainIdMap = loadAdditionalTenantRuleChains(tenantId, getTenantRuleChainsDir());
        Path rootRuleChainFile = getRootTenantRuleChainFile();
        loadRootRuleChain(tenantId, ruleChainIdMap, rootRuleChainFile);
   }

    private RuleChain loadRuleChain(Path path, JsonNode ruleChainJson, TenantId tenantId, String newRuleChainName) {
        try {
            RuleChain ruleChain = objectMapper.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
            RuleChainMetaData ruleChainMetaData = objectMapper.treeToValue(ruleChainJson.get("metadata"), RuleChainMetaData.class);

            ruleChain.setTenantId(tenantId);
            if (!StringUtils.isEmpty(newRuleChainName)) {
                ruleChain.setName(newRuleChainName);
            }
            ruleChain = ruleChainService.saveRuleChain(ruleChain);

            ruleChainMetaData.setRuleChainId(ruleChain.getId());
            ruleChainService.saveRuleChainMetaData(TenantId.SYS_TENANT_ID, ruleChainMetaData);
            return ruleChain;
        } catch (Exception e) {
            log.error("Unable to load rule chain from json: [{}]", path.toString());
            throw new RuntimeException("Unable to load rule chain from json", e);
        }
    }

    public RuleChain createDefaultRuleChain(TenantId tenantId, String ruleChainName) throws IOException {
        return createRuleChainFromFile(tenantId, getDeviceProfileDefaultRuleChainTemplateFilePath(), ruleChainName);
    }

    public RuleChain createRuleChainFromFile(TenantId tenantId, Path templateFilePath, String newRuleChainName) throws IOException {
        JsonNode ruleChainJson = objectMapper.readTree(templateFilePath.toFile());
        return this.loadRuleChain(templateFilePath, ruleChainJson, tenantId, newRuleChainName);
    }

    public void createDefaultEdgeRuleChains(TenantId tenantId) throws IOException {
        Path edgeChainsDir = getEdgeRuleChainsDir();
        loadAdditionalTenantRuleChains(tenantId, edgeChainsDir);
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
                                            WidgetTypeDetails widgetTypeDetails = objectMapper.treeToValue(widgetTypeJson, WidgetTypeDetails.class);
                                            widgetTypeDetails.setBundleAlias(savedWidgetsBundle.getAlias());
                                            widgetTypeService.saveWidgetType(widgetTypeDetails);
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
                                EntityGroup dashboardGroup = entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(tenantId, customerId, EntityType.DASHBOARD);
                                entityGroupService.addEntityToEntityGroup(tenantId, dashboardGroup.getId(), savedDashboard.getId());
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
        JsonNode mailTemplatesJson = readMailTemplates();
        mailTemplateSettings.setJsonValue(mailTemplatesJson);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, mailTemplateSettings);
    }

    public void updateMailTemplates(AdminSettingsId adminSettingsId, JsonNode oldTemplates) throws Exception {
        AdminSettings mailTemplateSettings = new AdminSettings();
        mailTemplateSettings.setId(adminSettingsId);
        mailTemplateSettings.setKey("mailTemplates");
        mailTemplateSettings.setJsonValue(updateMailTemplates(oldTemplates));
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, mailTemplateSettings);
    }

    public ObjectNode updateMailTemplates(JsonNode oldTemplates) throws IOException {
        JsonNode newTemplates = readMailTemplates();

        ObjectNode result = objectMapper.createObjectNode();
        Iterator<String> fieldsIterator = newTemplates.fieldNames();
        while (fieldsIterator.hasNext()) {
            String field = fieldsIterator.next();
            if (oldTemplates.has(field)) {
                result.set(field, oldTemplates.get(field));
            } else {
                result.set(field, newTemplates.get(field));
            }
        }
        Optional<String> updated = updateMailTemplatesFromVelocityToFreeMarker(objectMapper.writeValueAsString(result));
        if (updated.isPresent()) {
            result = (ObjectNode) objectMapper.readTree(updated.get());
        }
        return result;
    }

    public Optional<String> updateMailTemplatesFromVelocityToFreeMarker(String mailTemplatesJsonString) {
        Matcher matcher = velocityVarPattern.matcher(mailTemplatesJsonString);
        if (matcher.find()) {
            mailTemplatesJsonString = matcher.replaceAll(velocityVarToFreeMakerReplacementPattern);
            return Optional.of(mailTemplatesJsonString);
        } else {
            return Optional.empty();
        }
    }

    private JsonNode readMailTemplates() throws IOException {
        Path mailTemplatesFile = Paths.get(getDataDir(), JSON_DIR, SYSTEM_DIR, MAIL_TEMPLATES_DIR, MAIL_TEMPLATES_JSON);
        return objectMapper.readTree(mailTemplatesFile.toFile());
    }

    public void loadDemoRuleChains(TenantId tenantId) {
        try {
            createDefaultRuleChains(tenantId);
            createDefaultRuleChain(tenantId, "Thermostat");
            createDefaultEdgeRuleChains(tenantId);
        } catch (Exception e) {
            log.error("Unable to load rule chain from json", e);
            throw new RuntimeException("Unable to load rule chain from json", e);
        }
    }

    private void loadRootRuleChain(TenantId tenantId, Map<String, RuleChainId> ruleChainIdMap, Path rootRuleChainFile) throws IOException {
        String rootRuleChainContent = FileUtils.readFileToString(rootRuleChainFile.toFile(), "UTF-8");
        for (Map.Entry<String, RuleChainId> entry : ruleChainIdMap.entrySet()) {
            String key = "${" + entry.getKey() + "}";
            rootRuleChainContent = rootRuleChainContent.replace(key, entry.getValue().toString());
        }
        JsonNode rootRuleChainJson = objectMapper.readTree(rootRuleChainContent);
        loadRuleChain(rootRuleChainFile, rootRuleChainJson, tenantId, null);
    }

    private Map<String, RuleChainId> loadAdditionalTenantRuleChains(TenantId tenantId, Path chainsDir) throws IOException {
        Map<String, RuleChainId> ruleChainIdMap = new HashMap<>();
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(chainsDir, path -> path.toString().endsWith(InstallScripts.JSON_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            JsonNode ruleChainJson = objectMapper.readTree(path.toFile());

                            RuleChain ruleChain = loadRuleChain(path, ruleChainJson, tenantId, null);
                            ruleChainIdMap.put(ruleChain.getName(), ruleChain.getId());

                        } catch (Exception e) {
                            log.error("Unable to load rule chain from json: [{}]", path.toString());
                            throw new RuntimeException("Unable to load rule chain from json", e);
                        }
                    }
            );
        }
        return ruleChainIdMap;
    }

    public void createOAuth2Templates() throws Exception {
        Path oauth2ConfigTemplatesDir = Paths.get(getDataDir(), JSON_DIR, SYSTEM_DIR, OAUTH2_CONFIG_TEMPLATES_DIR);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(oauth2ConfigTemplatesDir, path -> path.toString().endsWith(JSON_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            JsonNode oauth2ConfigTemplateJson = objectMapper.readTree(path.toFile());
                            OAuth2ClientRegistrationTemplate clientRegistrationTemplate = objectMapper.treeToValue(oauth2ConfigTemplateJson, OAuth2ClientRegistrationTemplate.class);
                            Optional<OAuth2ClientRegistrationTemplate> existingClientRegistrationTemplate =
                                    oAuth2TemplateService.findClientRegistrationTemplateByProviderId(clientRegistrationTemplate.getProviderId());
                            if (existingClientRegistrationTemplate.isPresent()) {
                                clientRegistrationTemplate.setId(existingClientRegistrationTemplate.get().getId());
                            }
                            oAuth2TemplateService.saveClientRegistrationTemplate(clientRegistrationTemplate);
                        } catch (Exception e) {
                            log.error("Unable to load oauth2 config templates from json: [{}]", path.toString());
                            throw new RuntimeException("Unable to load oauth2 config templates from json", e);
                        }
                    }
            );
        }
    }

    public void loadSystemLwm2mResources() {
        Path resourceLwm2mPath = Paths.get(dataDir, MODELS_LWM2M_DIR);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(resourceLwm2mPath, path -> path.toString().endsWith(InstallScripts.XML_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            String data = Base64.getEncoder().encodeToString(Files.readAllBytes(path));
                            TbResource tbResource = new TbResource();
                            tbResource.setTenantId(TenantId.SYS_TENANT_ID);
                            tbResource.setData(data);
                            tbResource.setResourceType(ResourceType.LWM2M_MODEL);
                            tbResource.setFileName(path.toFile().getName());
                            doSaveLwm2mResource(tbResource);
                        } catch (Exception e) {
                            log.error("Unable to load resource lwm2m object model from file: [{}]", path.toString());
                            throw new RuntimeException("resource lwm2m object model from file", e);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Unable to load resources lwm2m object model from file: [{}]", resourceLwm2mPath.toString());
            throw new RuntimeException("resource lwm2m object model from file", e);
        }
    }

    private void doSaveLwm2mResource(TbResource resource) throws ThingsboardException {
        try {
            log.trace("Executing saveResource [{}]", resource);
            if (StringUtils.isEmpty(resource.getData())) {
                throw new DataValidationException("Resource data should be specified!");
            }
            toLwm2mResource(resource);
            resourceService.saveResource(resource);
        } catch (DataValidationException e) {
            log.error("[{}] {}", resource.getFileName(), e.getMessage());
        } catch (Exception ex) {
            throw ex;
        }
    }
}

