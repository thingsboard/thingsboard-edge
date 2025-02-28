/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.sync.vc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNode;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNodeConfiguration;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.sync.vc.EntityTypeLoadResult;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositoryAuthMethod;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.request.create.ComplexVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.EntityTypeVersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.SyncStrategy;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadConfig;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadRequest;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "service.integrations.supported=ALL",
})
public class VersionControlTest extends AbstractControllerTest {

    @Autowired
    private EntitiesVersionControlService versionControlService;
    @Autowired
    private OtaPackageService otaPackageService;

    private TenantId tenantId1;
    protected User tenantAdmin1;

    private TenantId tenantId2;
    protected User tenantAdmin2;

    private String repoKey;
    private String branch;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();
        Tenant tenant1 = new Tenant();
        tenant1.setTitle("Tenant 1");
        tenant1.setEmail("tenant1@thingsboard.org");
        tenant1 = saveTenant(tenant1);
        this.tenantId1 = tenant1.getId();
        User tenantAdmin1 = new User();
        tenantAdmin1.setTenantId(tenantId1);
        tenantAdmin1.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin1.setEmail("tenant1-admin@thingsboard.org");
        this.tenantAdmin1 = createUser(tenantAdmin1, tenantAdmin1.getEmail());

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Tenant 2");
        tenant2.setEmail("tenant2@thingsboard.org");
        tenant2 = saveTenant(tenant2);
        this.tenantId2 = tenant2.getId();
        User tenantAdmin2 = new User();
        tenantAdmin2.setTenantId(tenantId2);
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setEmail("tenant2-admin@thingsboard.org");
        this.tenantAdmin2 = createUser(tenantAdmin2, tenantAdmin2.getEmail());

        this.repoKey = UUID.randomUUID().toString();
        this.branch = "test_" + repoKey;
        configureRepository(tenantId1);
        configureRepository(tenantId2);

        loginTenant1();
    }

    @Test
    public void testAssetVc_withProfile_betweenTenants() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "Asset profile of tenant 1");
        Asset asset = createAsset(null, assetProfile.getId(), "Asset of tenant 1");
        String versionId = createVersion("assets and profiles", EntityType.ASSET, EntityType.ASSET_PROFILE);
        assertThat(listVersions()).extracting(EntityVersion::getName).containsExactly("assets and profiles");

        loginTenant2();
        Map<EntityType, EntityTypeLoadResult> result = loadVersion(versionId, EntityType.ASSET, EntityType.ASSET_PROFILE);
        assertThat(result.get(EntityType.ASSET).getCreated()).isEqualTo(1);
        assertThat(result.get(EntityType.ASSET_PROFILE).getCreated()).isEqualTo(1);

        Asset importedAsset = findAsset(asset.getName());
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);
        checkImportedAssetData(asset, importedAsset);

        AssetProfile importedAssetProfile = findAssetProfile(assetProfile.getName());
        checkImportedEntity(tenantId1, assetProfile, tenantId2, importedAssetProfile);
        checkImportedAssetProfileData(assetProfile, importedAssetProfile);

        assertThat(importedAsset.getAssetProfileId()).isEqualTo(importedAssetProfile.getId());
    }

    @Test
    public void testAssetVc_sameTenant() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "Asset profile v1.0");
        Asset asset = createAsset(null, assetProfile.getId(), "Asset v1.0");
        String versionId = createVersion("assets", EntityType.ASSET);

        loadVersion(versionId, EntityType.ASSET);
        Asset importedAsset = findAsset(asset.getName());
        checkImportedEntity(tenantId1, asset, tenantId1, importedAsset);
        checkImportedAssetData(asset, importedAsset);
    }

    @Test
    public void testAssetVc_sameTenant_withCustomer() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "Asset profile v1.0");
        Customer customer = createCustomer("My customer");
        Asset asset = createAsset(customer.getId(), assetProfile.getId(), "My asset");
        String versionId = createVersion("assets", EntityType.ASSET);

        loadVersion(versionId, EntityType.ASSET);
        Asset importedAsset = findAsset(asset.getName());
        assertThat(importedAsset.getCustomerId()).isEqualTo(asset.getCustomerId());
    }

    @Test
    public void testCustomerVc_sameTenant() throws Exception {
        Customer customer = createCustomer("Customer v1.0");
        String versionId = createVersion("customers", EntityType.CUSTOMER);

        loadVersion(versionId, EntityType.CUSTOMER);
        Customer importedCustomer = findCustomer(customer.getName());
        checkImportedEntity(tenantId1, customer, tenantId1, importedCustomer);
        checkImportedCustomerData(customer, importedCustomer);
    }

    @Test
    public void testCustomerAndUsersVc_betweenTenants() throws Exception {
        Customer customer = createCustomer("Customer v1.0");
        String versionId = createVersion("customers", EntityType.ROLE, EntityType.CUSTOMER, EntityType.USER);

        loginTenant2();
        loadVersion(versionId, EntityType.ROLE, EntityType.CUSTOMER, EntityType.USER);
        Customer importedCustomer = findCustomer(customer.getName());
        checkImportedEntity(tenantId1, customer, tenantId2, importedCustomer);
        checkImportedCustomerData(customer, importedCustomer);
    }

    @Test
    public void testCustomerVc_betweenTenants() throws Exception {
        Customer customer = createCustomer("Customer of tenant 1");
        String versionId = createVersion("customers", EntityType.CUSTOMER);

        loginTenant2();
        loadVersion(versionId, EntityType.CUSTOMER);
        Customer importedCustomer = findCustomer(customer.getName());
        checkImportedEntity(tenantId1, customer, tenantId2, importedCustomer);
        checkImportedCustomerData(customer, importedCustomer);
    }

    @Test
    public void testDeviceVc_sameTenant() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);
        Device device = createDevice(null, deviceProfile.getId(), "Device v1.0", "test1", newDevice -> {
            newDevice.setFirmwareId(firmware.getId());
            newDevice.setSoftwareId(software.getId());
        });
        DeviceCredentials deviceCredentials = findDeviceCredentials(device.getId());
        String versionId = createVersion("devices", EntityType.DEVICE);

        loadVersion(versionId, EntityType.DEVICE);
        Device importedDevice = findDevice(device.getName());

        checkImportedEntity(tenantId1, device, tenantId1, importedDevice);
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(device.getDeviceProfileId());
        assertThat(findDeviceCredentials(device.getId())).isEqualToIgnoringGivenFields(deviceCredentials, "version");
        assertThat(importedDevice.getFirmwareId()).isEqualTo(firmware.getId());
        assertThat(importedDevice.getSoftwareId()).isEqualTo(software.getId());
    }

    @Test
    public void testDeviceVc_withProfile_betweenTenants() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile of tenant 1");
        createVersion("profiles", EntityType.DEVICE_PROFILE);
        Device device = createDevice(null, deviceProfile.getId(), "Device of tenant 1", "test1");
        String versionId = createVersion("devices", EntityType.DEVICE);
        DeviceCredentials deviceCredentials = findDeviceCredentials(device.getId());
        DeviceCredentials newCredentials = new DeviceCredentials(deviceCredentials);
        newCredentials.setCredentialsId("new access token"); // updating access token to avoid constraint errors on import
        doPost("/api/device/credentials", newCredentials, DeviceCredentials.class);
        assertThat(listVersions()).extracting(EntityVersion::getName).containsExactly("devices", "profiles");

        loginTenant2();
        Map<EntityType, EntityTypeLoadResult> result = loadVersion(versionId, EntityType.DEVICE, EntityType.DEVICE_PROFILE);
        assertThat(result.get(EntityType.DEVICE).getCreated()).isEqualTo(1);
        assertThat(result.get(EntityType.DEVICE_PROFILE).getCreated()).isEqualTo(1);

        Device importedDevice = findDevice(device.getName());
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        checkImportedDeviceData(device, importedDevice);

        DeviceProfile importedDeviceProfile = findDeviceProfile(deviceProfile.getName());
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, importedDeviceProfile);
        checkImportedDeviceProfileData(deviceProfile, importedDeviceProfile);

        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(importedDeviceProfile.getId());

        DeviceCredentials importedCredentials = findDeviceCredentials(importedDevice.getId());
        assertThat(importedCredentials.getId()).isNotEqualTo(deviceCredentials.getId());
        assertThat(importedCredentials.getCredentialsId()).isEqualTo(deviceCredentials.getCredentialsId());
        assertThat(importedCredentials.getCredentialsValue()).isEqualTo(deviceCredentials.getCredentialsValue());
        assertThat(importedCredentials.getCredentialsType()).isEqualTo(deviceCredentials.getCredentialsType());
    }

    @Test
    public void testDashboardVc_betweenTenants() throws Exception {
        Dashboard dashboard = createDashboard(null, "Dashboard of tenant 1");
        String versionId = createVersion("dashboards", EntityType.DASHBOARD);

        loginTenant2();
        loadVersion(versionId, EntityType.DASHBOARD);
        Dashboard importedDashboard = findDashboard(dashboard.getName());
        checkImportedEntity(tenantId1, dashboard, tenantId2, importedDashboard);
        checkImportedDashboardData(dashboard, importedDashboard);
    }

    @Test
    public void testDashboardVc_sameTenant() throws Exception {
        Dashboard dashboard = createDashboard(null, "Dashboard v1.0");
        String versionId = createVersion("dashboards", EntityType.DASHBOARD);

        loadVersion(versionId, EntityType.DASHBOARD);
        Dashboard importedDashboard = findDashboard(dashboard.getName());
        checkImportedEntity(tenantId1, dashboard, tenantId1, importedDashboard);
        checkImportedDashboardData(dashboard, importedDashboard);
    }

    @Test
    public void testDashboardVc_betweenTenants_withEntityAliases() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "A");
        Asset asset1 = createAsset(null, assetProfile.getId(), "Asset 1");
        Asset asset2 = createAsset(null, assetProfile.getId(), "Asset 2");
        Dashboard dashboard = createDashboard(null, "Dashboard 1");
        Dashboard otherDashboard = createDashboard(null, "Dashboard 2");
        loginTenant2();
        DeviceProfile existingDeviceProfile = createDeviceProfile(null, null, "Existing");

        loginTenant1();
        String aliasId = "23c4185d-1497-9457-30b2-6d91e69a5b2c";
        String unknownUuid = "ea0dc8b0-3d85-11ed-9200-77fc04fa14fa";
        String entityAliases = "{\n" +
                "\"" + aliasId + "\": {\n" +
                "\"alias\": \"assets\",\n" +
                "\"filter\": {\n" +
                "   \"entityList\": [\n" +
                "   \"" + asset1.getId() + "\",\n" +
                "   \"" + asset2.getId() + "\",\n" +
                "   \"" + tenantId1.getId() + "\",\n" +
                "   \"" + existingDeviceProfile.getId() + "\",\n" +
                "   \"" + unknownUuid + "\"\n" +
                "   ],\n" +
                "   \"id\":\"" + asset1.getId() + "\",\n" +
                "   \"resolveMultiple\": true\n" +
                "},\n" +
                "\"id\": \"" + aliasId + "\"\n" +
                "}\n" +
                "}";
        String widgetId = "ea8f34a0-264a-f11f-cde3-05201bb4ff4b";
        String actionId = "4a8e6efa-3e68-fa59-7feb-d83366130cae";
        String widgets = "{\n" +
                "  \"" + widgetId + "\": {\n" +
                "    \"config\": {\n" +
                "      \"actions\": {\n" +
                "        \"rowClick\": [\n" +
                "          {\n" +
                "            \"name\": \"go to dashboard\",\n" +
                "            \"targetDashboardId\": \"" + otherDashboard.getId() + "\",\n" +
                "            \"id\": \"" + actionId + "\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    \"row\": 0,\n" +
                "    \"col\": 0,\n" +
                "    \"id\": \"" + widgetId + "\"\n" +
                "  }\n" +
                "}";

        ObjectNode dashboardConfiguration = JacksonUtil.newObjectNode();
        dashboardConfiguration.set("entityAliases", JacksonUtil.toJsonNode(entityAliases));
        dashboardConfiguration.set("widgets", JacksonUtil.toJsonNode(widgets));
        dashboardConfiguration.set("description", new TextNode("hallo"));
        dashboard.setConfiguration(dashboardConfiguration);
        dashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        String versionId = createVersion("dashboard with related", EntityType.ASSET, EntityType.ASSET_PROFILE, EntityType.DASHBOARD);

        loginTenant2();
        loadVersion(versionId, EntityType.ASSET, EntityType.ASSET_PROFILE, EntityType.DASHBOARD);

        AssetProfile importedProfile = findAssetProfile(assetProfile.getName());
        Asset importedAsset1 = findAsset(asset1.getName());
        Asset importedAsset2 = findAsset(asset2.getName());
        Dashboard importedOtherDashboard = findDashboard(otherDashboard.getName());
        Dashboard importedDashboard = findDashboard(dashboard.getName());

        Map.Entry<String, JsonNode> entityAlias = importedDashboard.getConfiguration().get("entityAliases").fields().next();
        assertThat(entityAlias.getKey()).isEqualTo(aliasId);
        assertThat(entityAlias.getValue().get("id").asText()).isEqualTo(aliasId);

        List<String> aliasEntitiesIds = Streams.stream(entityAlias.getValue().get("filter").get("entityList").elements())
                .map(JsonNode::asText).collect(Collectors.toList());
        assertThat(aliasEntitiesIds).size().isEqualTo(5);
        assertThat(aliasEntitiesIds).element(0).as("external asset 1 was replaced with imported one")
                .isEqualTo(importedAsset1.getId().toString());
        assertThat(aliasEntitiesIds).element(1).as("external asset 2 was replaced with imported one")
                .isEqualTo(importedAsset2.getId().toString());
        assertThat(aliasEntitiesIds).element(2).as("external tenant id was replaced with new tenant id")
                .isEqualTo(tenantId2.toString());
        assertThat(aliasEntitiesIds).element(3).as("existing device profile id was left as is")
                .isEqualTo(existingDeviceProfile.getId().toString());
        assertThat(aliasEntitiesIds).element(4).as("unresolved uuid was replaced with tenant id")
                .isEqualTo(tenantId2.toString());
        assertThat(entityAlias.getValue().get("filter").get("id").asText()).as("external asset 1 was replaced with imported one")
                .isEqualTo(importedAsset1.getId().toString());

        ObjectNode widgetConfig = importedDashboard.getWidgetsConfig().get(0);
        assertThat(widgetConfig.get("id").asText()).as("widget id is not replaced")
                .isEqualTo(widgetId);
        JsonNode actionConfig = widgetConfig.get("config").get("actions").get("rowClick").get(0);
        assertThat(actionConfig.get("id").asText()).as("action id is not replaced")
                .isEqualTo(actionId);
        assertThat(actionConfig.get("targetDashboardId").asText()).as("dashboard id is replaced with imported one")
                .isEqualTo(importedOtherDashboard.getId().toString());
    }

    @Test
    public void testRuleChainVc_betweenTenants() throws Exception {
        RuleChain ruleChain = createRuleChain("Rule chain of tenant 1");
        RuleChainMetaData metaData = findRuleChainMetaData(ruleChain.getId());
        String versionId = createVersion("rule chains", EntityType.RULE_CHAIN);

        loginTenant2();
        loadVersion(versionId, EntityType.RULE_CHAIN);
        RuleChain importedRuleChain = findRuleChain(ruleChain.getName());
        RuleChainMetaData importedMetaData = findRuleChainMetaData(importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId2, importedRuleChain);
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    @Test
    public void testRuleChainVc_sameTenant() throws Exception {
        RuleChain ruleChain = createRuleChain("Rule chain v1.0");
        RuleChainMetaData metaData = findRuleChainMetaData(ruleChain.getId());
        String versionId = createVersion("rule chains", EntityType.RULE_CHAIN);

        loadVersion(versionId, EntityType.RULE_CHAIN);
        RuleChain importedRuleChain = findRuleChain(ruleChain.getName());
        RuleChainMetaData importedMetaData = findRuleChainMetaData(importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId1, importedRuleChain);
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    @Test
    public void testRuleChainVc_ruleNodesConfigs() throws Exception {
        Customer customer = createCustomer("Customer 1");
        RuleChain ruleChain = createRuleChain("Rule chain 1");
        RuleChainMetaData metaData = findRuleChainMetaData(ruleChain.getId());

        List<RuleNode> nodes = new ArrayList<>(metaData.getNodes());
        RuleNode generatorNode = new RuleNode();
        generatorNode.setName("Generator");
        generatorNode.setType(TbMsgGeneratorNode.class.getName());
        TbMsgGeneratorNodeConfiguration generatorNodeConfig = new TbMsgGeneratorNodeConfiguration();
        generatorNodeConfig.setOriginatorType(EntityType.ASSET_PROFILE);
        generatorNodeConfig.setOriginatorId(customer.getId().toString());
        generatorNodeConfig.setPeriodInSeconds(5);
        generatorNodeConfig.setMsgCount(1);
        generatorNodeConfig.setScriptLang(ScriptLanguage.JS);
        UUID someUuid = UUID.randomUUID();
        generatorNodeConfig.setJsScript("var msg = { temp: 42, humidity: 77 };\n" +
                "var metadata = { data: 40 };\n" +
                "var msgType = \"POST_TELEMETRY_REQUEST\";\n" +
                "var someUuid = \"" + someUuid + "\";\n" +
                "return { msg: msg, metadata: metadata, msgType: msgType };");
        generatorNode.setConfiguration(JacksonUtil.valueToTree(generatorNodeConfig));
        nodes.add(generatorNode);
        metaData.setNodes(nodes);
        doPost("/api/ruleChain/metadata", metaData, RuleChainMetaData.class);

        String versionId = createVersion("rule chains with customers", EntityType.RULE_CHAIN, EntityType.CUSTOMER);

        loginTenant2();
        loadVersion(versionId, EntityType.RULE_CHAIN, EntityType.CUSTOMER);
        Customer importedCustomer = findCustomer(customer.getName());
        RuleChain importedRuleChain = findRuleChain(ruleChain.getName());
        RuleChainMetaData importedMetaData = findRuleChainMetaData(importedRuleChain.getId());

        TbMsgGeneratorNodeConfiguration importedGeneratorNodeConfig = JacksonUtil.treeToValue(importedMetaData.getNodes().stream()
                .filter(node -> node.getName().equals(generatorNode.getName()))
                .findFirst().get().getConfiguration(), TbMsgGeneratorNodeConfiguration.class);
        assertThat(importedGeneratorNodeConfig.getOriginatorId()).isEqualTo(importedCustomer.getId().toString());
        assertThat(importedGeneratorNodeConfig.getJsScript()).contains("var someUuid = \"" + someUuid + "\";");
    }

    @Test
    public void testVcWithRelations_betweenTenants() throws Exception {
        Asset asset = createAsset(null, null, "Asset 1");
        Device device = createDevice(null, null, "Device 1", "test1");
        EntityRelation relation = createRelation(asset.getId(), device.getId());
        String versionId = createVersion("assets and devices", EntityType.ASSET, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE);

        loginTenant2();
        loadVersion(versionId, config -> {
            config.setLoadCredentials(false);
        }, EntityType.ASSET, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE);

        Asset importedAsset = findAsset(asset.getName());
        Device importedDevice = findDevice(device.getName());
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);

        List<EntityRelation> importedRelations = findRelationsByTo(importedDevice.getId());
        assertThat(importedRelations).size().isOne();
        assertThat(importedRelations.get(0)).satisfies(importedRelation -> {
            assertThat(importedRelation.getFrom()).isEqualTo(importedAsset.getId());
            assertThat(importedRelation.getType()).isEqualTo(relation.getType());
            assertThat(importedRelation.getAdditionalInfo()).isEqualTo(relation.getAdditionalInfo());
        });
    }

    @Test
    public void testVcWithRelations_sameTenant() throws Exception {
        Asset asset = createAsset(null, null, "Asset 1");
        Device device1 = createDevice(null, null, "Device 1", "test1");
        EntityRelation relation1 = createRelation(device1.getId(), asset.getId());
        String versionId = createVersion("assets", EntityType.ASSET);

        Device device2 = createDevice(null, null, "Device 2", "test2");
        EntityRelation relation2 = createRelation(device2.getId(), asset.getId());
        List<EntityRelation> relations = findRelationsByTo(asset.getId());
        assertThat(relations).contains(relation1, relation2);

        loadVersion(versionId, EntityType.ASSET);

        relations = findRelationsByTo(asset.getId());
        assertThat(relations).contains(relation1);
        assertThat(relations).doesNotContain(relation2);
    }

    @Test
    public void testDefaultDeviceProfileVc_betweenTenants_findExisting() throws Exception {
        DeviceProfile defaultDeviceProfile = findDeviceProfile("default");
        defaultDeviceProfile.setName("non-default-name");
        doPost("/api/deviceProfile", defaultDeviceProfile, DeviceProfile.class);
        String versionId = createVersion("device profiles", EntityType.DEVICE_PROFILE);

        loginTenant2();
        loadVersion(versionId, config -> {
            config.setFindExistingEntityByName(false);
        }, EntityType.DEVICE_PROFILE);

        DeviceProfile importedDeviceProfile = findDeviceProfile(defaultDeviceProfile.getName());
        assertThat(importedDeviceProfile.isDefault()).isTrue();
        assertThat(importedDeviceProfile.getName()).isEqualTo(defaultDeviceProfile.getName());
        checkImportedEntity(tenantId1, defaultDeviceProfile, tenantId2, importedDeviceProfile);
    }

    @Test
    public void testIntegrationVcWithConverter_betweenTenants() throws Exception {
        Converter converter = createConverter(ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(converter.getId(), IntegrationType.HTTP, "Integration 1");
        String versionId = createVersion("converters and integrations", EntityType.CONVERTER, EntityType.INTEGRATION);

        loginTenant2();
        loadVersion(versionId, config -> {
            config.setAutoGenerateIntegrationKey(true);
        }, EntityType.CONVERTER, EntityType.INTEGRATION);

        Converter importedConverter = findConverter(converter.getName());
        checkImportedEntity(tenantId1, converter, tenantId2, importedConverter);
        checkImportedConverterData(converter, importedConverter);

        Integration importedIntegration = findIntegration(integration.getName());
        checkImportedEntity(tenantId1, integration, tenantId2, importedIntegration);
        checkImportedIntegrationData(integration, importedIntegration);
    }

    @Test
    public void testIntegrationVcWithConverter_sameTenant() throws Exception {
        Converter converter = createConverter(ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(converter.getId(), IntegrationType.HTTP, "Integration 1");
        String versionId = createVersion("converters and integrations", EntityType.CONVERTER, EntityType.INTEGRATION);

        loadVersion(versionId, EntityType.CONVERTER, EntityType.INTEGRATION);

        Converter importedConverter = findConverter(converter.getName());
        checkImportedEntity(tenantId1, converter, tenantId1, importedConverter);
        checkImportedConverterData(converter, importedConverter);

        Integration importedIntegration = findIntegration(integration.getName());
        checkImportedEntity(tenantId1, integration, tenantId1, importedIntegration);
        checkImportedIntegrationData(integration, importedIntegration);
    }

    @Test
    public void testEntityGroupVc_betweenTenants() throws Exception {
        List<EntityGroup> entityGroups = new ArrayList<>();
        for (EntityType groupType : EntityGroup.groupTypes) {
            if (groupType == EntityType.EDGE) {
                continue;
            }
            EntityGroup entityGroup = createEntityGroup(tenantId1, groupType, groupType + " group");
            entityGroups.add(entityGroup);
        }
        String versionId = createVersion("entity groups", EntityGroup.groupTypes);

        loginTenant2();
        loadVersion(versionId, EntityGroup.groupTypes);

        for (EntityGroup entityGroup : entityGroups) {
            EntityGroup importedEntityGroup = findEntityGroup(entityGroup.getName(), entityGroup.getType());
            checkImportedEntity(tenantId1, tenantId1, entityGroup, tenantId2, tenantId2, importedEntityGroup);
            checkImportedEntityGroupData(entityGroup, importedEntityGroup);
        }
    }

    @Test
    public void testEntityGroupVc_sameTenant() throws Exception {
        List<EntityGroup> entityGroups = new ArrayList<>();
        for (EntityType groupType : EntityGroup.groupTypes) {
            if (groupType == EntityType.EDGE) {
                continue;
            }
            EntityGroup entityGroup = createEntityGroup(tenantId1, groupType, groupType + " group");
            entityGroups.add(entityGroup);
        }
        String versionId = createVersion("entity groups", EntityGroup.groupTypes);

        loadVersion(versionId, EntityGroup.groupTypes);

        for (EntityGroup entityGroup : entityGroups) {
            EntityGroup importedEntityGroup = findEntityGroup(entityGroup.getName(), entityGroup.getType());
            checkImportedEntity(tenantId1, tenantId1, entityGroup, tenantId1, tenantId1, importedEntityGroup);
            checkImportedEntityGroupData(entityGroup, importedEntityGroup);
        }
    }

    @Test
    public void testEntityGroupVcWithPermissions_betweenTenants() throws Exception {
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(null, "Role for User group 1", List.of(Operation.READ));
        createGroupPermission(userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);
        String versionId = createVersion("groups", EntityType.USER, EntityType.DEVICE, EntityType.ROLE);

        loginTenant2();
        loadVersion(versionId, EntityType.USER, EntityType.DEVICE, EntityType.ROLE);

        Role importedRole = findRole(role.getName());
        checkImportedEntity(tenantId1, role, tenantId2, importedRole);
        assertThat(importedRole.getName()).isEqualTo(role.getName());
        assertThat(importedRole.getPermissions()).isEqualTo(role.getPermissions());

        EntityGroup importedDeviceGroup = findEntityGroup(deviceGroup.getName(), EntityType.DEVICE);
        checkImportedEntity(tenantId1, tenantId1, deviceGroup, tenantId2, tenantId2, importedDeviceGroup);

        EntityGroup importedUserGroup = findEntityGroup(userGroup.getName(), EntityType.USER);
        checkImportedEntity(tenantId1, tenantId1, userGroup, tenantId2, tenantId2, importedUserGroup);

        List<GroupPermissionInfo> importedGroupPermissions = findGroupPermissions(importedUserGroup.getId());
        assertThat(importedGroupPermissions).singleElement().satisfies(importedGroupPermission -> {
            assertThat(importedGroupPermission.getRoleId()).isEqualTo(importedRole.getId());
            assertThat(importedGroupPermission.getEntityGroupId()).isEqualTo(importedDeviceGroup.getId());
            assertThat(importedGroupPermission.getEntityGroupType()).isEqualTo(EntityType.DEVICE);
        });
    }

    @Test
    public void testEntityGroupVcWithPermissions_sameTenant() throws Exception {
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(null, "Role for User group 1", List.of(Operation.READ));
        GroupPermission groupPermission = createGroupPermission(userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);
        String versionId = createVersion("user groups", EntityType.USER);

        loadVersion(versionId, EntityType.USER);

        EntityGroup importedUserGroup = findEntityGroup(userGroup.getName(), EntityType.USER);
        checkImportedEntity(tenantId1, tenantId1, userGroup, tenantId1, tenantId1, importedUserGroup);

        List<GroupPermissionInfo> importedGroupPermissions = findGroupPermissions(importedUserGroup.getId());
        assertThat(importedGroupPermissions).singleElement().satisfies(permission -> {
            assertThat(new GroupPermission(permission)).isEqualTo(groupPermission);
        });
    }

    @Test
    public void testEntityGroupVcWithPermissions_betweenTenants_permissionsUpdated() throws Exception {
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(null, "Role for User group 1", List.of(Operation.READ));
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        GroupPermission groupPermission = createGroupPermission(userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);
        String versionId = createVersion("groups", EntityType.USER, EntityType.DEVICE, EntityType.ROLE);

        loginTenant2();
        loadVersion(versionId, EntityType.USER, EntityType.DEVICE, EntityType.ROLE);

        Role importedRole = findRole(role.getName());
        EntityGroup importedDeviceGroup = findEntityGroup(deviceGroup.getName(), EntityType.DEVICE);
        EntityGroup importedUserGroup = findEntityGroup(userGroup.getName(), EntityType.USER);

        List<GroupPermissionInfo> importedGroupPermissions = findGroupPermissions(importedUserGroup.getId());
        assertThat(importedGroupPermissions).singleElement().satisfies(importedGroupPermission -> {
            assertThat(importedGroupPermission.getRoleId()).isEqualTo(importedRole.getId());
            assertThat(importedGroupPermission.getEntityGroupId()).isEqualTo(importedDeviceGroup.getId());
            assertThat(importedGroupPermission.getEntityGroupType()).isEqualTo(EntityType.DEVICE);
        });

        loginTenant1();
        doDelete("/api/groupPermission/" + groupPermission.getId()).andExpect(status().isOk());
        assertThat(findGroupPermissions(userGroup.getId())).isEmpty();
        role = createGenericRole(null, "Read devices", Map.of(
                Resource.DEVICE, List.of(Operation.READ),
                Resource.DEVICE_GROUP, List.of(Operation.READ)
        ));
        groupPermission = createGroupPermission(userGroup.getId(), role.getId());
        versionId = createVersion("groups 2", EntityType.ROLE, EntityType.USER);

        loginTenant2();
        loadVersion(versionId, EntityType.ROLE, EntityType.USER);

        Role newImportedRole = findRole(role.getName());
        List<GroupPermissionInfo> updatedGroupPermissions = findGroupPermissions(importedUserGroup.getId());
        assertThat(updatedGroupPermissions).singleElement().satisfies(newGroupPermission -> {
            assertThat(newGroupPermission.getEntityGroupId()).matches(entityGroupId -> entityGroupId == null || entityGroupId.isNullUid());
            assertThat(newGroupPermission.getRoleId()).isEqualTo(newImportedRole.getId());
        });
    }

    private <E extends ExportableEntity<?> & HasTenantId> void checkImportedEntity(TenantId tenantId1, E initialEntity, TenantId tenantId2, E importedEntity) {
        assertThat(initialEntity.getTenantId()).isEqualTo(tenantId1);
        assertThat(importedEntity.getTenantId()).isEqualTo(tenantId2);
        assertThat(importedEntity.getExternalId()).isEqualTo(initialEntity.getId());
        boolean sameTenant = tenantId1.equals(tenantId2);
        if (sameTenant) {
            assertThat(importedEntity.getId()).isEqualTo(initialEntity.getId());
        } else {
            assertThat(importedEntity.getId()).isNotEqualTo(initialEntity.getId());
        }
    }

    protected <E extends ExportableEntity<?> & HasOwnerId> void checkImportedEntity(TenantId tenantId1, EntityId ownerId1, E initialEntity,
                                                                                    TenantId tenantId2, EntityId ownerId2, E importedEntity) {
        if (initialEntity instanceof HasTenantId) {
            assertThat(((HasTenantId) initialEntity).getTenantId()).isEqualTo(tenantId1);
            assertThat(((HasTenantId) importedEntity).getTenantId()).isEqualTo(tenantId2);
        }
        assertThat(initialEntity.getOwnerId()).isEqualTo(ownerId1);
        assertThat(importedEntity.getOwnerId()).isEqualTo(ownerId2);

        assertThat(importedEntity.getExternalId()).isEqualTo(initialEntity.getId());

        boolean sameTenant = tenantId1.equals(tenantId2);
        if (!sameTenant) {
            assertThat(importedEntity.getId()).isNotEqualTo(initialEntity.getId());
            assertThat(importedEntity.getOwnerId()).isNotEqualTo(initialEntity.getOwnerId());
        } else {
            assertThat(importedEntity.getId()).isEqualTo(initialEntity.getId());
            assertThat(importedEntity.getOwnerId()).isEqualTo(initialEntity.getOwnerId());
        }
    }

    protected void checkImportedAssetData(Asset initialAsset, Asset importedAsset) {
        assertThat(importedAsset.getName()).isEqualTo(initialAsset.getName());
        assertThat(importedAsset.getType()).isEqualTo(initialAsset.getType());
        assertThat(importedAsset.getLabel()).isEqualTo(initialAsset.getLabel());
        assertThat(importedAsset.getAdditionalInfo()).isEqualTo(initialAsset.getAdditionalInfo());
    }

    protected void checkImportedAssetProfileData(AssetProfile initialProfile, AssetProfile importedProfile) {
        assertThat(initialProfile.getName()).isEqualTo(importedProfile.getName());
        assertThat(initialProfile.getDescription()).isEqualTo(importedProfile.getDescription());
    }

    protected void checkImportedDeviceData(Device initialDevice, Device importedDevice) {
        assertThat(importedDevice.getName()).isEqualTo(initialDevice.getName());
        assertThat(importedDevice.getType()).isEqualTo(initialDevice.getType());
        assertThat(importedDevice.getDeviceData()).isEqualTo(initialDevice.getDeviceData());
        assertThat(importedDevice.getLabel()).isEqualTo(initialDevice.getLabel());
    }

    protected void checkImportedDeviceProfileData(DeviceProfile initialProfile, DeviceProfile importedProfile) {
        assertThat(initialProfile.getName()).isEqualTo(importedProfile.getName());
        assertThat(initialProfile.getType()).isEqualTo(importedProfile.getType());
        assertThat(initialProfile.getTransportType()).isEqualTo(importedProfile.getTransportType());
        assertThat(initialProfile.getProfileData()).isEqualTo(importedProfile.getProfileData());
        assertThat(initialProfile.getDescription()).isEqualTo(importedProfile.getDescription());
    }

    protected void checkImportedCustomerData(Customer initialCustomer, Customer importedCustomer) {
        assertThat(importedCustomer.getTitle()).isEqualTo(initialCustomer.getTitle());
        assertThat(importedCustomer.getCountry()).isEqualTo(initialCustomer.getCountry());
        assertThat(importedCustomer.getAddress()).isEqualTo(initialCustomer.getAddress());
        assertThat(importedCustomer.getEmail()).isEqualTo(initialCustomer.getEmail());
    }

    protected void checkImportedDashboardData(Dashboard initialDashboard, Dashboard importedDashboard) {
        assertThat(importedDashboard.getTitle()).isEqualTo(initialDashboard.getTitle());
        assertThat(importedDashboard.getConfiguration()).isEqualTo(initialDashboard.getConfiguration());
        assertThat(importedDashboard.getImage()).isEqualTo(initialDashboard.getImage());
        assertThat(importedDashboard.isMobileHide()).isEqualTo(initialDashboard.isMobileHide());
        if (initialDashboard.getAssignedCustomers() != null) {
            assertThat(importedDashboard.getAssignedCustomers()).containsAll(initialDashboard.getAssignedCustomers());
        }
    }

    protected void checkImportedEntityGroupData(EntityGroup initialEntityGroup, EntityGroup importedEntityGroup) {
        assertThat(importedEntityGroup.getType()).isEqualTo(initialEntityGroup.getType());
        assertThat(importedEntityGroup.getName()).isEqualTo(initialEntityGroup.getName());
        assertThat(importedEntityGroup.getConfiguration()).isEqualTo(initialEntityGroup.getConfiguration());
        assertThat(importedEntityGroup.getAdditionalInfo()).isEqualTo(initialEntityGroup.getAdditionalInfo());
    }

    protected void checkImportedConverterData(Converter initialConverter, Converter importedConverter) {
        assertThat(importedConverter.getType()).isEqualTo(initialConverter.getType());
        assertThat(importedConverter.getName()).isEqualTo(initialConverter.getName());
        assertThat(importedConverter.getConfiguration()).isEqualTo(initialConverter.getConfiguration());
        assertThat(importedConverter.getAdditionalInfo()).isEqualTo(initialConverter.getAdditionalInfo());
        assertThat(importedConverter.getDebugSettings()).isEqualTo(initialConverter.getDebugSettings());
    }

    protected void checkImportedIntegrationData(Integration initialIntegration, Integration importedIntegration) {
        assertThat(importedIntegration.getName()).isEqualTo(initialIntegration.getName());
        assertThat(importedIntegration.getType()).isEqualTo(initialIntegration.getType());
        assertThat(importedIntegration.getConfiguration()).isEqualTo(initialIntegration.getConfiguration());
        assertThat(importedIntegration.getAdditionalInfo()).isEqualTo(initialIntegration.getAdditionalInfo());
        assertThat(importedIntegration.getSecret()).isEqualTo(initialIntegration.getSecret());
    }

    private String createVersion(String name, EntityType... entityTypes) throws Exception {
        ComplexVersionCreateRequest request = new ComplexVersionCreateRequest();
        request.setVersionName(name);
        request.setBranch(branch);
        request.setSyncStrategy(SyncStrategy.MERGE);
        request.setEntityTypes(Arrays.stream(entityTypes).collect(Collectors.toMap(t -> t, entityType -> {
            EntityTypeVersionCreateConfig config = new EntityTypeVersionCreateConfig();
            config.setAllEntities(true);
            config.setSaveGroupEntities(true);
            config.setSaveRelations(true);
            config.setSaveAttributes(true);
            config.setSaveCredentials(true);
            config.setSavePermissions(true);
            config.setSaveGroupEntities(true);
            return config;
        })));

        UUID requestId = doPostAsync("/api/entities/vc/version", request, UUID.class, status().isOk());
        VersionCreationResult result = await().atMost(30, TimeUnit.SECONDS)
                .until(() -> doGet("/api/entities/vc/version/" + requestId + "/status", VersionCreationResult.class), r -> {
                    if (r.getError() != null) {
                        throw new RuntimeException("Failed to create version '" + name + "': " + r.getError());
                    }
                    return r.isDone();
                });
        assertThat(result.getVersion()).isNotNull();
        return result.getVersion().getId();
    }

    private String createVersion(String name, EntityId... entities) throws Exception {
        ComplexVersionCreateRequest request = new ComplexVersionCreateRequest();
        request.setVersionName(name);
        request.setBranch(branch);
        request.setSyncStrategy(SyncStrategy.MERGE);
        request.setEntityTypes(new HashMap<>());
        Map<EntityType, List<EntityId>> entitiesByType = Arrays.stream(entities)
                .collect(Collectors.groupingBy(EntityId::getEntityType));
        entitiesByType.forEach((entityType, ids) -> {
            EntityTypeVersionCreateConfig config = new EntityTypeVersionCreateConfig();
            config.setAllEntities(false);
            config.setEntityIds(ids.stream().map(EntityId::getId).toList());

            config.setSaveRelations(true);
            config.setSaveAttributes(true);
            config.setSaveCredentials(true);
            config.setSavePermissions(true);
            config.setSaveGroupEntities(true);
            request.getEntityTypes().put(entityType, config);
        });

        return createVersion(request);
    }

    private String createVersion(VersionCreateRequest request) throws Exception {
        UUID requestId = doPostAsync("/api/entities/vc/version", request, UUID.class, status().isOk());
        VersionCreationResult result = await().atMost(60, TimeUnit.SECONDS)
                .until(() -> doGet("/api/entities/vc/version/" + requestId + "/status", VersionCreationResult.class), r -> {
                    if (r.getError() != null) {
                        throw new RuntimeException("Failed to create version '" + request.getVersionName() + "': " + r.getError());
                    }
                    return r.isDone();
                });
        assertThat(result.getVersion()).isNotNull();
        return result.getVersion().getId();
    }

    private Map<EntityType, EntityTypeLoadResult> loadVersion(String versionId, EntityType... entityTypes) throws Exception {
        return loadVersion(versionId, config -> {}, entityTypes);
    }

    private Map<EntityType, EntityTypeLoadResult> loadVersion(String versionId, Consumer<EntityTypeVersionLoadConfig> configModifier, EntityType... entityTypes) throws Exception {
        assertThat(listVersions()).extracting(EntityVersion::getId).contains(versionId);

        EntityTypeVersionLoadRequest request = new EntityTypeVersionLoadRequest();
        request.setVersionId(versionId);
        request.setRollbackOnError(true);
        request.setEntityTypes(Arrays.stream(entityTypes).collect(Collectors.toMap(t -> t, entityType -> {
            EntityTypeVersionLoadConfig config = new EntityTypeVersionLoadConfig();
            config.setLoadAttributes(true);
            config.setLoadRelations(true);
            config.setLoadCredentials(true);
            config.setLoadPermissions(true);
            config.setLoadGroupEntities(true);
            config.setRemoveOtherEntities(false);
            config.setFindExistingEntityByName(true);
            configModifier.accept(config);
            return config;
        })));

        UUID requestId = doPost("/api/entities/vc/entity", request, UUID.class);
        VersionLoadResult result = await().atMost(60, TimeUnit.SECONDS)
                .until(() -> doGet("/api/entities/vc/entity/" + requestId + "/status", VersionLoadResult.class), VersionLoadResult::isDone);
        if (result.getError() != null) {
            throw new RuntimeException("Failed to load version: " + result);
        }
        return result.getResult().stream().collect(Collectors.toMap(EntityTypeLoadResult::getEntityType, r -> r));
    }

    private List<EntityVersion> listVersions() throws Exception {
        PageData<EntityVersion> versions = doGetAsyncTyped("/api/entities/vc/version?branch=" + branch + "&pageSize=100&page=0&sortProperty=timestamp&sortOrder=DESC", new TypeReference<PageData<EntityVersion>>() {});
        return versions.getData();
    }

    private void configureRepository(TenantId tenantId) throws Exception {
        RepositorySettings repositorySettings = new RepositorySettings();
        repositorySettings.setLocalOnly(true);
        repositorySettings.setDefaultBranch(branch);
        repositorySettings.setAuthMethod(RepositoryAuthMethod.USERNAME_PASSWORD);
        repositorySettings.setRepositoryUri(repoKey);
        versionControlService.saveVersionControlSettings(tenantId, repositorySettings).get();
    }

    private void loginTenant1() throws Exception {
        login(tenantAdmin1.getEmail(), tenantAdmin1.getEmail());
    }

    private void loginTenant2() throws Exception {
        login(tenantAdmin2.getEmail(), tenantAdmin2.getEmail());
    }

    private Device createDevice(CustomerId customerId, DeviceProfileId deviceProfileId, String name, String accessToken, Consumer<Device>... modifiers) {
        Device device = new Device();
        device.setCustomerId(customerId);
        device.setName(name);
        device.setLabel("lbl");
        device.setDeviceProfileId(deviceProfileId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        device.setDeviceData(deviceData);
        for (Consumer<Device> modifier : modifiers) {
            modifier.accept(device);
        }
        return doPost("/api/device?accessToken=" + accessToken, device, Device.class);
    }

    private DeviceProfile createDeviceProfile(RuleChainId defaultRuleChainId, DashboardId defaultDashboardId, String name) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(name);
        deviceProfile.setDescription("dscrptn");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setDefaultRuleChainId(defaultRuleChainId);
        deviceProfile.setDefaultDashboardId(defaultDashboardId);
        DeviceProfileData profileData = new DeviceProfileData();
        profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        profileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(profileData);
        return doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
    }

    protected EntityView createEntityView(CustomerId customerId, EntityId entityId, String name) {
        EntityView entityView = new EntityView();
        entityView.setTenantId(tenantId);
        entityView.setEntityId(entityId);
        entityView.setCustomerId(customerId);
        entityView.setName(name);
        entityView.setType("A");
        return doPost("/api/entityView", entityView, EntityView.class);
    }

    private AssetProfile createAssetProfile(RuleChainId defaultRuleChainId, DashboardId defaultDashboardId, String name) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(name);
        assetProfile.setDescription("dscrptn");
        assetProfile.setDefaultRuleChainId(defaultRuleChainId);
        assetProfile.setDefaultDashboardId(defaultDashboardId);
        return saveAssetProfile(assetProfile);
    }

    private AssetProfile saveAssetProfile(AssetProfile assetProfile) {
        return doPost("/api/assetProfile", assetProfile, AssetProfile.class);
    }

    private Asset createAsset(CustomerId customerId, AssetProfileId assetProfileId, String name) {
        Asset asset = new Asset();
        asset.setCustomerId(customerId);
        asset.setAssetProfileId(assetProfileId);
        asset.setName(name);
        asset.setLabel("lbl");
        asset.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return doPost("/api/asset", asset, Asset.class);
    }

    protected Customer createCustomer(String name) {
        Customer customer = new Customer();
        customer.setTitle(name);
        customer.setCountry("ua");
        customer.setAddress("abb");
        customer.setEmail("ccc@aa.org");
        customer.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return doPost("/api/customer", customer, Customer.class);
    }

    protected OtaPackage createOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType type) {
        OtaPackage otaPackage = new OtaPackage();
        otaPackage.setTenantId(tenantId);
        otaPackage.setDeviceProfileId(deviceProfileId);
        otaPackage.setType(type);
        otaPackage.setTitle("My " + type);
        otaPackage.setVersion("v1.0");
        otaPackage.setFileName("filename.txt");
        otaPackage.setContentType("text/plain");
        otaPackage.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        otaPackage.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        otaPackage.setDataSize(1L);
        otaPackage.setData(ByteBuffer.wrap(new byte[]{(int) 1}));
        return otaPackageService.saveOtaPackage(otaPackage);
    }

    protected Dashboard createDashboard(CustomerId customerId, String name) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(name);
        dashboard.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        dashboard.setImage("abvregewrg");
        dashboard.setMobileHide(true);
        dashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        if (customerId != null) {
            return assignDashboardToCustomer(dashboard.getId(), customerId);
        }
        return dashboard;
    }

    protected Dashboard createDashboard(CustomerId customerId, String name, AssetId assetForEntityAlias) {
        Dashboard dashboard = createDashboard(customerId, name);
        String entityAliases = "{\n" +
                "\t\"23c4185d-1497-9457-30b2-6d91e69a5b2c\": {\n" +
                "\t\t\"alias\": \"assets\",\n" +
                "\t\t\"filter\": {\n" +
                "\t\t\t\"entityList\": [\n" +
                "\t\t\t\t\"" + assetForEntityAlias.getId().toString() + "\"\n" +
                "\t\t\t],\n" +
                "\t\t\t\"entityType\": \"ASSET\",\n" +
                "\t\t\t\"resolveMultiple\": true,\n" +
                "\t\t\t\"type\": \"entityList\"\n" +
                "\t\t},\n" +
                "\t\t\"id\": \"23c4185d-1497-9457-30b2-6d91e69a5b2c\"\n" +
                "\t}\n" +
                "}";
        ObjectNode dashboardConfiguration = JacksonUtil.newObjectNode();
        dashboardConfiguration.set("entityAliases", JacksonUtil.toJsonNode(entityAliases));
        dashboardConfiguration.set("description", new TextNode("hallo"));
        dashboard.setConfiguration(dashboardConfiguration);
        return doPost("/api/dashboard", dashboard, Dashboard.class);
    }

    protected RuleChain createRuleChain(String name, EntityId originatorId) throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Generator 1");
        ruleNode1.setType(TbMsgGeneratorNode.class.getName());
        ruleNode1.setDebugSettings(DebugSettings.all());
        TbMsgGeneratorNodeConfiguration configuration1 = new TbMsgGeneratorNodeConfiguration();
        configuration1.setOriginatorType(originatorId.getEntityType());
        configuration1.setOriginatorId(originatorId.getId().toString());
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode2.setDebugSettings(DebugSettings.all());
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        doPost("/api/ruleChain/metadata", metaData, RuleChainMetaData.class);

        return doGet("/api/ruleChain/" + ruleChain.getUuidId(), RuleChain.class);
    }

    protected RuleChain createRuleChain(String name) throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Simple Rule Node 1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode1.setDebugSettings(DebugSettings.all());
        TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode2.setDebugSettings(DebugSettings.all());
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        doPost("/api/ruleChain/metadata", metaData, RuleChainMetaData.class);

        return doGet("/api/ruleChain/" + ruleChain.getUuidId(), RuleChain.class);
    }

    protected EntityRelation createRelation(EntityId from, EntityId to) throws Exception {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.MANAGES_TYPE);
        relation.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        return doPost("/api/v2/relation", relation, EntityRelation.class);
    }

    protected EntityGroup createEntityGroup(EntityId ownerId, EntityType groupType, String name) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setOwnerId(ownerId);
        entityGroup.setType(groupType);
        entityGroup.setName(name);
        entityGroup.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, ownerId, entityGroup);
    }

    protected Converter createConverter(ConverterType type, String name) {
        Converter converter = new Converter();
        converter.setType(type);
        converter.setName(name);
        converter.setConfiguration(JacksonUtil.newObjectNode()
                .<ObjectNode>set("encoder", new TextNode("b"))
                .set("decoder", new TextNode("c")));
        converter.setDebugSettings(DebugSettings.all());
        converter.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return doPost("/api/converter", converter, Converter.class);
    }

    protected Integration createIntegration(ConverterId converterId, IntegrationType type, String name) {
        Integration integration = new Integration();
        integration.setType(type);
        integration.setName(name);
        integration.setDefaultConverterId(converterId);
        integration.setRoutingKey("abc");
        integration.setSecret("scrt");
        integration.setEnabled(false);
        integration.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        integration.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return doPost("/api/integration", integration, Integration.class);
    }

    protected Role createGenericRole(CustomerId customerId, String name, Map<Resource, List<Operation>> permissions) {
        return createRole(customerId, name, RoleType.GENERIC, permissions);
    }

    protected Role createGroupRole(CustomerId customerId, String name, List<Operation> permissions) {
        return createRole(customerId, name, RoleType.GROUP, permissions);
    }

    private Role createRole(CustomerId customerId, String name, RoleType roleType, Object permissions) {
        Role role = new Role();
        role.setCustomerId(customerId);
        role.setName(name);
        role.setType(roleType);
        role.setPermissions(JacksonUtil.valueToTree(permissions));
        return doPost("/api/role", role, Role.class);
    }

    protected GroupPermission createGroupPermission(EntityGroupId userGroupId, RoleId genericRoleId) {
        return createGroupPermission(userGroupId, genericRoleId, null, null);
    }

    protected GroupPermission createGroupPermission(EntityGroupId userGroupId, RoleId roleId, EntityGroupId entityGroupId, EntityType entityGroupType) {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setUserGroupId(userGroupId);
        groupPermission.setRoleId(roleId);
        groupPermission.setEntityGroupId(entityGroupId);
        groupPermission.setEntityGroupType(entityGroupType);
        return doPost("/api/groupPermission", groupPermission, GroupPermission.class);
    }

    protected void checkImportedRuleChainData(RuleChain initialRuleChain, RuleChainMetaData initialMetaData, RuleChain importedRuleChain, RuleChainMetaData importedMetaData) {
        assertThat(importedRuleChain.getType()).isEqualTo(initialRuleChain.getType());
        assertThat(importedRuleChain.getName()).isEqualTo(initialRuleChain.getName());
        assertThat(importedRuleChain.isDebugMode()).isEqualTo(initialRuleChain.isDebugMode());
        assertThat(importedRuleChain.getConfiguration()).isEqualTo(initialRuleChain.getConfiguration());

        assertThat(importedMetaData.getConnections()).isEqualTo(initialMetaData.getConnections());
        assertThat(importedMetaData.getFirstNodeIndex()).isEqualTo(initialMetaData.getFirstNodeIndex());
        for (int i = 0; i < initialMetaData.getNodes().size(); i++) {
            RuleNode initialNode = initialMetaData.getNodes().get(i);
            RuleNode importedNode = importedMetaData.getNodes().get(i);
            assertThat(importedNode.getRuleChainId()).isEqualTo(importedRuleChain.getId());
            assertThat(importedNode.getName()).isEqualTo(initialNode.getName());
            assertThat(importedNode.getType()).isEqualTo(initialNode.getType());
            assertThat(importedNode.getConfiguration()).isEqualTo(initialNode.getConfiguration());
            assertThat(importedNode.getAdditionalInfo()).isEqualTo(initialNode.getAdditionalInfo());
        }
    }

    private Dashboard assignDashboardToCustomer(DashboardId dashboardId, CustomerId customerId) {
        return doPost("/api/customer/" + customerId + "/dashboard/" + dashboardId, Dashboard.class);
    }

    private Asset findAsset(String name) throws Exception {
        return doGetTypedWithPageLink("/api/tenant/assets?", new TypeReference<PageData<Asset>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private AssetProfile findAssetProfile(String name) throws Exception {
        return doGetTypedWithPageLink("/api/assetProfiles?", new TypeReference<PageData<AssetProfile>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private DeviceProfile findDeviceProfile(String name) throws Exception {
        return doGetTypedWithPageLink("/api/deviceProfiles?", new TypeReference<PageData<DeviceProfile>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private Device findDevice(String name) throws Exception {
        return doGetTypedWithPageLink("/api/tenant/devices?", new TypeReference<PageData<Device>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private DeviceCredentials findDeviceCredentials(DeviceId deviceId) throws Exception {
        return doGet("/api/device/" + deviceId + "/credentials", DeviceCredentials.class);
    }

    private Customer findCustomer(String name) throws Exception {
        return doGetTypedWithPageLink("/api/customers?", new TypeReference<PageData<Customer>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private Dashboard findDashboard(String name) throws Exception {
        DashboardInfo dashboardInfo = doGetTypedWithPageLink("/api/tenant/dashboards?", new TypeReference<PageData<DashboardInfo>>() {}, new PageLink(100, 0, name)).getData().get(0);
        return doGet("/api/dashboard/" + dashboardInfo.getUuidId(), Dashboard.class);
    }

    private RuleChain findRuleChain(String name) throws Exception {
        return doGetTypedWithPageLink("/api/ruleChains?", new TypeReference<PageData<RuleChain>>() {}, new PageLink(100, 0, name)).getData().get(0);

    }

    private RuleChainMetaData findRuleChainMetaData(RuleChainId ruleChainId) throws Exception {
        return doGet("/api/ruleChain/" + ruleChainId + "/metadata", RuleChainMetaData.class);
    }

    private Integration findIntegration(String name) throws Exception {
        return doGetTypedWithPageLink("/api/integrations?", new TypeReference<PageData<Integration>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private Converter findConverter(String name) throws Exception {
        return doGetTypedWithPageLink("/api/converters?", new TypeReference<PageData<Converter>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private EntityGroup findEntityGroup(String name, EntityType groupType) throws Exception {
        return doGetTypedWithPageLink("/api/entityGroups/" + groupType + "?", new TypeReference<PageData<EntityGroup>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private Role findRole(String name) throws Exception {
        return doGetTypedWithPageLink("/api/roles?", new TypeReference<PageData<Role>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private List<GroupPermissionInfo> findGroupPermissions(EntityGroupId userGroupId) throws Exception {
        return doGetTyped("/api/userGroup/" + userGroupId + "/groupPermissions?", new TypeReference<>() {});
    }

}
