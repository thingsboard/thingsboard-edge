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
package org.thingsboard.server.service.sync.ie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import org.assertj.core.data.Index;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNode;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNodeConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.sync.ie.DeviceExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.ie.RuleChainExportData;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class ExportImportServiceSqlTest extends BaseExportImportServiceTest {

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;
    @SpyBean
    private EntityActionService entityActionService;
    @SpyBean
    private OtaPackageStateService otaPackageStateService;
    @SpyBean
    private UserPermissionsService userPermissionsService;
    @SpyBean
    private AccessControlService accessControlService;

    @Test
    public void testExportImportAssetWithProfile_betweenTenants() throws Exception {
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "Asset profile of tenant 1");
        Asset asset = createAsset(tenantId1, null, assetProfile.getId(), null, "Asset of tenant 1");
        EntityExportData<Asset> exportData = exportEntity(tenantAdmin1, asset.getId());

        EntityExportData<AssetProfile> profileExportData = exportEntity(tenantAdmin1, assetProfile.getId());

        EntityExportData<Asset> assetExportData = exportEntity(tenantAdmin1, asset.getId());

        EntityImportResult<AssetProfile> profileImportResult = importEntity(tenantAdmin2, profileExportData);
        checkImportedEntity(tenantId1, assetProfile, tenantId2, profileImportResult.getSavedEntity());
        checkImportedAssetProfileData(assetProfile, profileImportResult.getSavedEntity());

        EntityImportResult<Asset> assetImportResult = importEntity(tenantAdmin2, assetExportData);
        Asset importedAsset = assetImportResult.getSavedEntity();
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);
        checkImportedAssetData(asset, importedAsset);

        assertThat(importedAsset.getAssetProfileId()).isEqualTo(profileImportResult.getSavedEntity().getId());
    }

    @Test
    public void testExportImportAsset_sameTenant() throws Exception {
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "Asset profile v1.0");
        Asset asset = createAsset(tenantId1, null, assetProfile.getId(), null, "Asset v1.0");
        EntityExportData<Asset> exportData = exportEntity(tenantAdmin1, asset.getId());

        EntityImportResult<Asset> importResult = importEntity(tenantAdmin1, exportData);
        checkImportedEntity(tenantId1, asset, tenantId1, importResult.getSavedEntity());
        checkImportedAssetData(asset, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportAsset_sameTenant_withCustomer() throws Exception {
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "Asset profile v1.0");
        Customer customer = createCustomer(tenantId1, null, "My customer");
        Asset asset = createAsset(tenantId1, customer.getId(), assetProfile.getId(), null,"My asset");

        Asset importedAsset = importEntity(tenantAdmin1, this.<Asset, AssetId>exportEntity(tenantAdmin1, asset.getId())).getSavedEntity();
        assertThat(importedAsset.getCustomerId()).isEqualTo(asset.getCustomerId());
    }


    @Test
    public void testExportImportCustomer_betweenTenants() throws Exception {
        Customer customer = createCustomer(tenantAdmin1.getTenantId(), null, "Customer of tenant 1");
        EntityExportData<Customer> exportData = exportEntity(tenantAdmin1, customer.getId());

        EntityImportResult<Customer> importResult = importEntity(tenantAdmin2, exportData);
        checkImportedEntity(tenantId1, customer, tenantId2, importResult.getSavedEntity());
        checkImportedCustomerData(customer, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportCustomer_sameTenant() throws Exception {
        Customer customer = createCustomer(tenantAdmin1.getTenantId(), null, "Customer v1.0");
        EntityExportData<Customer> exportData = exportEntity(tenantAdmin1, customer.getId());

        EntityImportResult<Customer> importResult = importEntity(tenantAdmin1, exportData);
        checkImportedEntity(tenantId1, customer, tenantId1, importResult.getSavedEntity());
        checkImportedCustomerData(customer, importResult.getSavedEntity());
    }


    @Test
    public void testExportImportDeviceWithProfile_betweenTenants() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile of tenant 1");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), null, "Device of tenant 1");
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());

        EntityExportData<DeviceProfile> profileExportData = exportEntity(tenantAdmin1, deviceProfile.getId());

        EntityExportData<Device> deviceExportData = exportEntity(tenantAdmin1, device.getId());
        DeviceCredentials exportedCredentials = ((DeviceExportData) deviceExportData).getCredentials();
        exportedCredentials.setCredentialsId(credentials.getCredentialsId() + "a");

        EntityImportResult<DeviceProfile> profileImportResult = importEntity(tenantAdmin2, profileExportData);
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, profileImportResult.getSavedEntity());
        checkImportedDeviceProfileData(deviceProfile, profileImportResult.getSavedEntity());

        EntityImportResult<Device> deviceImportResult = importEntity(tenantAdmin2, deviceExportData);
        Device importedDevice = deviceImportResult.getSavedEntity();
        checkImportedEntity(tenantId1, device, tenantId2, deviceImportResult.getSavedEntity());
        checkImportedDeviceData(device, importedDevice);

        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(profileImportResult.getSavedEntity().getId());

        DeviceCredentials importedCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId2, importedDevice.getId());
        assertThat(importedCredentials.getId()).isNotEqualTo(credentials.getId());
        assertThat(importedCredentials.getCredentialsId()).isEqualTo(exportedCredentials.getCredentialsId());
        assertThat(importedCredentials.getCredentialsValue()).isEqualTo(credentials.getCredentialsValue());
        assertThat(importedCredentials.getCredentialsType()).isEqualTo(credentials.getCredentialsType());
    }

    @Test
    public void testExportImportDevice_sameTenant() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), null, "Device v1.0");
        device.setFirmwareId(firmware.getId());
        device.setSoftwareId(software.getId());
        device = deviceService.saveDevice(device);

        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());

        EntityExportData<Device> deviceExportData = exportEntity(tenantAdmin1, device.getId());

        EntityImportResult<Device> importResult = importEntity(tenantAdmin1, deviceExportData);
        Device importedDevice = importResult.getSavedEntity();

        checkImportedEntity(tenantId1, device, tenantId1, importResult.getSavedEntity());
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(device.getDeviceProfileId());
        assertThat(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId())).isEqualTo(credentials);
        assertThat(importedDevice.getFirmwareId()).isEqualTo(firmware.getId());
        assertThat(importedDevice.getSoftwareId()).isEqualTo(software.getId());
    }


    @Test
    public void testExportImportDashboard_betweenTenants() throws Exception {
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, null, "Dashboard of tenant 1");
        EntityExportData<Dashboard> exportData = exportEntity(tenantAdmin1, dashboard.getId());

        EntityImportResult<Dashboard> importResult = importEntity(tenantAdmin2, exportData);
        checkImportedEntity(tenantId1, dashboard, tenantId2, importResult.getSavedEntity());
        checkImportedDashboardData(dashboard, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportDashboard_sameTenant() throws Exception {
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, null, "Dashboard v1.0");
        EntityExportData<Dashboard> exportData = exportEntity(tenantAdmin1, dashboard.getId());

        EntityImportResult<Dashboard> importResult = importEntity(tenantAdmin1, exportData);
        checkImportedEntity(tenantId1, dashboard, tenantId1, importResult.getSavedEntity());
        checkImportedDashboardData(dashboard, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportDashboard_betweenTenants_withEntityAliases() throws Exception {
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "A");
        Asset asset1 = createAsset(tenantId1, null, assetProfile.getId(), null, "Asset 1");
        Asset asset2 = createAsset(tenantId1, null, assetProfile.getId(), null, "Asset 2");
        Dashboard dashboard = createDashboard(tenantId1, null, null, "Dashboard 1");
        DeviceProfile existingDeviceProfile = createDeviceProfile(tenantId2, null, null, "Existing");

        String aliasId = "23c4185d-1497-9457-30b2-6d91e69a5b2c";
        String unknownUuid = "ea0dc8b0-3d85-11ed-9200-77fc04fa14fa";
        String entityAliases = "{\n" +
                "\"" + aliasId + "\": {\n" +
                "\"alias\": \"assets\",\n" +
                "\"filter\": {\n" +
                "\"entityList\": [\n" +
                "\"" + asset1.getId().toString() + "\",\n" +
                "\"" + asset2.getId().toString() + "\",\n" +
                "\"" + tenantId1.getId().toString() + "\",\n" +
                "\"" + existingDeviceProfile.getId().toString() + "\",\n" +
                "\"" + unknownUuid + "\"\n" +
                "],\n" +
                "\"resolveMultiple\": true\n" +
                "},\n" +
                "\"id\": \"" + aliasId + "\"\n" +
                "}\n" +
                "}";
        ObjectNode dashboardConfiguration = JacksonUtil.newObjectNode();
        dashboardConfiguration.set("entityAliases", JacksonUtil.toJsonNode(entityAliases));
        dashboardConfiguration.set("description", new TextNode("hallo"));
        dashboard.setConfiguration(dashboardConfiguration);
        dashboard = dashboardService.saveDashboard(dashboard);

        EntityExportData<AssetProfile> profileExportData = exportEntity(tenantAdmin1, assetProfile.getId());

        EntityExportData<Asset> asset1ExportData = exportEntity(tenantAdmin1, asset1.getId());
        EntityExportData<Asset> asset2ExportData = exportEntity(tenantAdmin1, asset2.getId());
        EntityExportData<Dashboard> dashboardExportData = exportEntity(tenantAdmin1, dashboard.getId());

        AssetProfile importedProfile = importEntity(tenantAdmin2, profileExportData).getSavedEntity();
        Asset importedAsset1 = importEntity(tenantAdmin2, asset1ExportData).getSavedEntity();
        Asset importedAsset2 = importEntity(tenantAdmin2, asset2ExportData).getSavedEntity();
        Dashboard importedDashboard = importEntity(tenantAdmin2, dashboardExportData).getSavedEntity();

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
    }


    @Test
    public void testExportImportRuleChain_betweenTenants() throws Exception {
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain of tenant 1");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());
        EntityExportData<RuleChain> exportData = exportEntity(tenantAdmin1, ruleChain.getId());

        EntityImportResult<RuleChain> importResult = importEntity(tenantAdmin2, exportData);
        RuleChain importedRuleChain = importResult.getSavedEntity();
        RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId2, importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId2, importResult.getSavedEntity());
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    @Test
    public void testExportImportRuleChain_sameTenant() throws Exception {
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain v1.0");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());
        EntityExportData<RuleChain> exportData = exportEntity(tenantAdmin1, ruleChain.getId());

        EntityImportResult<RuleChain> importResult = importEntity(tenantAdmin1, exportData);
        RuleChain importedRuleChain = importResult.getSavedEntity();
        RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId1, importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId1, importResult.getSavedEntity());
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }


    @Test
    public void testExportImportWithInboundRelations_betweenTenants() throws Exception {
        Asset asset = createAsset(tenantId1, null, null, null, "Asset 1");
        Device device = createDevice(tenantId1, null, null, null, "Device 1");
        EntityRelation relation = createRelation(asset.getId(), device.getId());

        EntityExportData<Asset> assetExportData = exportEntity(tenantAdmin1, asset.getId());
        EntityExportData<Device> deviceExportData = exportEntity(tenantAdmin1, device.getId(), EntityExportSettings.builder()
                .exportRelations(true)
                .exportCredentials(false)
                .build());

        assertThat(deviceExportData.getRelations()).size().isOne();
        assertThat(deviceExportData.getRelations().get(0)).matches(entityRelation -> {
            return entityRelation.getFrom().equals(asset.getId()) && entityRelation.getTo().equals(device.getId());
        });
        ((Asset) assetExportData.getEntity()).setAssetProfileId(null);
        ((Device) deviceExportData.getEntity()).setDeviceProfileId(null);

        Asset importedAsset = importEntity(tenantAdmin2, assetExportData).getSavedEntity();
        Device importedDevice = importEntity(tenantAdmin2, deviceExportData, EntityImportSettings.builder()
                .updateRelations(true)
                .build()).getSavedEntity();
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);

        List<EntityRelation> importedRelations = relationService.findByTo(TenantId.SYS_TENANT_ID, importedDevice.getId(), RelationTypeGroup.COMMON);
        assertThat(importedRelations).size().isOne();
        assertThat(importedRelations.get(0)).satisfies(importedRelation -> {
            assertThat(importedRelation.getFrom()).isEqualTo(importedAsset.getId());
            assertThat(importedRelation.getType()).isEqualTo(relation.getType());
            assertThat(importedRelation.getAdditionalInfo()).isEqualTo(relation.getAdditionalInfo());
        });
    }

    @Test
    public void testExportImportWithRelations_betweenTenants() throws Exception {
        Asset asset = createAsset(tenantId1, null, null, null, "Asset 1");
        Device device = createDevice(tenantId1, null, null, null, "Device 1");
        EntityRelation relation = createRelation(asset.getId(), device.getId());

        EntityExportData<Asset> assetExportData = exportEntity(tenantAdmin1, asset.getId());
        EntityExportData<Device> deviceExportData = exportEntity(tenantAdmin1, device.getId(), EntityExportSettings.builder()
                .exportRelations(true)
                .exportCredentials(false)
                .build());
        assetExportData.getEntity().setAssetProfileId(null);
        deviceExportData.getEntity().setDeviceProfileId(null);

        Asset importedAsset = importEntity(tenantAdmin2, assetExportData).getSavedEntity();
        Device importedDevice = importEntity(tenantAdmin2, deviceExportData, EntityImportSettings.builder()
                .updateRelations(true)
                .build()).getSavedEntity();

        List<EntityRelation> importedRelations = relationService.findByTo(TenantId.SYS_TENANT_ID, importedDevice.getId(), RelationTypeGroup.COMMON);
        assertThat(importedRelations).size().isOne();
        assertThat(importedRelations.get(0)).satisfies(importedRelation -> {
            assertThat(importedRelation.getFrom()).isEqualTo(importedAsset.getId());
            assertThat(importedRelation.getType()).isEqualTo(relation.getType());
            assertThat(importedRelation.getAdditionalInfo()).isEqualTo(relation.getAdditionalInfo());
        });
    }

    @Test
    public void testExportImportWithRelations_sameTenant() throws Exception {
        Asset asset = createAsset(tenantId1, null, null, null, "Asset 1");
        Device device1 = createDevice(tenantId1, null, null, null, "Device 1");
        EntityRelation relation1 = createRelation(asset.getId(), device1.getId());

        EntityExportData<Asset> assetExportData = exportEntity(tenantAdmin1, asset.getId(), EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        assertThat(assetExportData.getRelations()).size().isOne();

        Device device2 = createDevice(tenantId1, null, null, null, "Device 2");
        EntityRelation relation2 = createRelation(asset.getId(), device2.getId());

        importEntity(tenantAdmin1, assetExportData, EntityImportSettings.builder()
                .updateRelations(true)
                .build());

        List<EntityRelation> relations = relationService.findByFrom(TenantId.SYS_TENANT_ID, asset.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).contains(relation1);
        assertThat(relations).doesNotContain(relation2);
    }

    @Test
    public void textExportImportWithRelations_sameTenant_removeExisting() throws Exception {
        Asset asset1 = createAsset(tenantId1, null, null, null, "Asset 1");
        Device device = createDevice(tenantId1, null, null, null, "Device 1");
        EntityRelation relation1 = createRelation(asset1.getId(), device.getId());

        EntityExportData<Device> deviceExportData = exportEntity(tenantAdmin1, device.getId(), EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        assertThat(deviceExportData.getRelations()).size().isOne();

        Asset asset2 = createAsset(tenantId1, null, null, null, "Asset 2");
        EntityRelation relation2 = createRelation(asset2.getId(), device.getId());

        importEntity(tenantAdmin1, deviceExportData, EntityImportSettings.builder()
                .updateRelations(true)
                .build());

        List<EntityRelation> relations = relationService.findByTo(TenantId.SYS_TENANT_ID, device.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).contains(relation1);
        assertThat(relations).doesNotContain(relation2);
    }


    @Test
    public void testExportImportDeviceProfile_betweenTenants_findExistingByName() throws Exception {
        DeviceProfile defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId1);
        EntityExportData<DeviceProfile> deviceProfileExportData = exportEntity(tenantAdmin1, defaultDeviceProfile.getId());

        assertThatThrownBy(() -> {
            importEntity(tenantAdmin2, deviceProfileExportData, EntityImportSettings.builder()
                    .findExistingByName(false)
                    .build());
        }).hasMessageContaining("default device profile is present");

        importEntity(tenantAdmin2, deviceProfileExportData, EntityImportSettings.builder()
                .findExistingByName(true)
                .build());
        checkImportedEntity(tenantId1, defaultDeviceProfile, tenantId2, deviceProfileService.findDefaultDeviceProfile(tenantId2));
    }


    @SuppressWarnings("rawTypes")
    private static EntityExportData getAndClone(Map<EntityType, EntityExportData> map, EntityType entityType) {
        return JacksonUtil.clone(map.get(entityType));
    }

    @SuppressWarnings({"rawTypes", "unchecked"})
    @Test
    public void testEntityEventsOnImport() throws Exception {
        Customer customer = createCustomer(tenantId1, null, "Customer 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Dashboard dashboard = createDashboard(tenantId1, null, null, "Dashboard 1");
        AssetProfile assetProfile = createAssetProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Asset profile 1");
        Asset asset = createAsset(tenantId1, null, assetProfile.getId(), null, "Asset 1");
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), null, "Device 1");
        Converter converter = createConverter(tenantId1, ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");
        Role role = createGenericRole(tenantId1, null, "Role 1", Map.of(Resource.DEVICE, List.of(Operation.READ)));
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        createGroupPermission(tenantId1, userGroup.getId(), role.getId());

        Map<EntityType, EntityExportData> entitiesExportData = Stream.of(customer.getId(), asset.getId(), device.getId(),
                        ruleChain.getId(), dashboard.getId(), assetProfile.getId(), deviceProfile.getId(), converter.getId(),
                        integration.getId(), role.getId(), userGroup.getId())
                .map(entityId -> {
                    try {
                        return exportEntity(tenantAdmin1, entityId, EntityExportSettings.builder()
                                .exportCredentials(false)
                                .exportPermissions(true)
                                .build());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(EntityExportData::getEntityType, d -> d));

        Mockito.reset(entityActionService, tbClusterService);
        Customer importedCustomer = (Customer) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.CUSTOMER)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedCustomer.getId()), eq(importedCustomer),
                any(), eq(ActionType.ADDED), isNull());
        Mockito.reset(entityActionService);
        importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.CUSTOMER));
        verify(entityActionService, Mockito.never()).logEntityAction(any(), eq(importedCustomer.getId()), eq(importedCustomer),
                any(), eq(ActionType.UPDATED), isNull());

        EntityExportData<Customer> updatedCustomerEntity = getAndClone(entitiesExportData, EntityType.CUSTOMER);
        updatedCustomerEntity.getEntity().setEmail("t" + updatedCustomerEntity.getEntity().getEmail());
        Customer updatedCustomer = importEntity(tenantAdmin2, updatedCustomerEntity).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedCustomer.getId()), eq(updatedCustomer),
                any(), eq(ActionType.UPDATED), isNull());
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedCustomer.getId()), any(), any(), eq(EdgeEventActionType.UPDATED), any(), any());

        Mockito.reset(entityActionService);

        RuleChain importedRuleChain = (RuleChain) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.RULE_CHAIN)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedRuleChain.getId()), eq(importedRuleChain),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).broadcastEntityStateChangeEvent(any(), eq(importedRuleChain.getId()), eq(ComponentLifecycleEvent.CREATED));

        Dashboard importedDashboard = (Dashboard) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DASHBOARD)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDashboard.getId()), eq(importedDashboard),
                any(), eq(ActionType.ADDED), isNull());

        AssetProfile importedAssetProfile = (AssetProfile) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.ASSET_PROFILE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedAssetProfile.getId()), eq(importedAssetProfile),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).broadcastEntityStateChangeEvent(any(), eq(importedAssetProfile.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedAssetProfile.getId()), any(), any(), eq(EdgeEventActionType.ADDED));

        Asset importedAsset = (Asset) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.ASSET)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedAsset.getId()), eq(importedAsset),
                any(), eq(ActionType.ADDED), isNull());
        importEntity(tenantAdmin2, entitiesExportData.get(EntityType.ASSET));
        verify(entityActionService, Mockito.never()).logEntityAction(any(), eq(importedAsset.getId()), eq(importedAsset),
                any(), eq(ActionType.UPDATED), isNull());


        EntityExportData<Asset> updatedAssetEntity = getAndClone(entitiesExportData, EntityType.ASSET);
        updatedAssetEntity.getEntity().setLabel("t" + updatedAssetEntity.getEntity().getLabel());
        Asset updatedAsset = importEntity(tenantAdmin2, updatedAssetEntity).getSavedEntity();

        verify(entityActionService).logEntityAction(any(), eq(importedAsset.getId()), eq(updatedAsset),
                any(), eq(ActionType.UPDATED), isNull());
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedAsset.getId()), any(), any(), eq(EdgeEventActionType.UPDATED), any(), any());

        DeviceProfile importedDeviceProfile = (DeviceProfile) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DEVICE_PROFILE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDeviceProfile.getId()), eq(importedDeviceProfile),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).onDeviceProfileChange(eq(importedDeviceProfile), any());
        verify(tbClusterService).broadcastEntityStateChangeEvent(any(), eq(importedDeviceProfile.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedDeviceProfile.getId()), any(), any(), eq(EdgeEventActionType.ADDED), any(), any());
        verify(otaPackageStateService).update(eq(importedDeviceProfile), eq(false), eq(false));

        Device importedDevice = (Device) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DEVICE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDevice.getId()), eq(importedDevice),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).onDeviceUpdated(eq(importedDevice), isNull());
        importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DEVICE));
        verify(tbClusterService, Mockito.never()).onDeviceUpdated(eq(importedDevice), eq(importedDevice));

        EntityExportData<Device> updatedDeviceEntity = getAndClone(entitiesExportData, EntityType.DEVICE);
        updatedDeviceEntity.getEntity().setLabel("t" + updatedDeviceEntity.getEntity().getLabel());
        Device updatedDevice = importEntity(tenantAdmin2, updatedDeviceEntity).getSavedEntity();
        verify(tbClusterService).onDeviceUpdated(eq(updatedDevice), eq(importedDevice));

        Converter importedConverter = (Converter) importEntity(tenantAdmin2, entitiesExportData.get(EntityType.CONVERTER)).getSavedEntity();
        verify(tbClusterService).broadcastEntityStateChangeEvent(any(), eq(importedConverter.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(entityActionService).logEntityAction(any(), eq(importedConverter.getId()), eq(importedConverter), any(), eq(ActionType.ADDED), isNull());

        Integration importedIntegration = (Integration) importEntity(tenantAdmin2, entitiesExportData.get(EntityType.INTEGRATION), EntityImportSettings.builder()
                .autoGenerateIntegrationKey(true)
                .build()).getSavedEntity();
        verify(tbClusterService).broadcastEntityStateChangeEvent(any(), eq(importedIntegration.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(entityActionService).logEntityAction(any(), eq(importedIntegration.getId()), eq(importedIntegration), any(), eq(ActionType.ADDED), isNull());

        Role importedRole = (Role) importEntity(tenantAdmin2, entitiesExportData.get(EntityType.ROLE)).getSavedEntity();
        verify(userPermissionsService).onRoleUpdated(argThat(r -> r.getId().equals(importedRole.getId())));
        verify(entityActionService).logEntityAction(any(), eq(importedRole.getId()), notNull(), any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedRole.getId()),
                any(), any(), eq(EdgeEventActionType.ADDED), any(), any());
    }

    @Test
    public void testExportImportConverterWithIntegration_betweenTenants() throws Exception {
        Converter converter = createConverter(tenantId1, ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");
        EntityExportData<Converter> converterExportData = exportEntity(tenantAdmin1, converter.getId());
        EntityExportData<Integration> integrationExportData = exportEntity(tenantAdmin1, integration.getId());

        Converter importedConverter = importEntity(tenantAdmin2, converterExportData).getSavedEntity();
        checkImportedEntity(tenantId1, converter, tenantId2, importedConverter);
        checkImportedConverterData(converter, importedConverter);

        Integration importedIntegration = importEntity(tenantAdmin2, integrationExportData).getSavedEntity();
        checkImportedEntity(tenantId1, integration, tenantId2, importedIntegration);
        checkImportedIntegrationData(integration, importedIntegration);
    }

    @Test
    public void testExportImportConverterWithIntegration_sameTenant() throws Exception {
        Converter converter = createConverter(tenantId1, ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");
        EntityExportData<Converter> converterExportData = exportEntity(tenantAdmin1, converter.getId());
        EntityExportData<Integration> integrationExportData = exportEntity(tenantAdmin1, integration.getId());

        Converter importedConverter = importEntity(tenantAdmin1, converterExportData).getSavedEntity();
        checkImportedEntity(tenantId1, converter, tenantId1, importedConverter);
        checkImportedConverterData(converter, importedConverter);

        Integration importedIntegration = importEntity(tenantAdmin1, integrationExportData).getSavedEntity();
        checkImportedEntity(tenantId1, integration, tenantId1, importedIntegration);
        checkImportedIntegrationData(integration, importedIntegration);
    }

    @Test
    public void testExportImportEntityGroup_betweenTenants() throws Exception {
        for (EntityType groupType : EntityGroup.groupTypes) {
            EntityGroup entityGroup = createEntityGroup(tenantId1, groupType, groupType + " group");
            EntityExportData<EntityGroup> exportData = exportEntity(tenantAdmin1, entityGroup.getId());

            EntityGroup importedEntityGroup = importEntity(tenantAdmin2, exportData).getSavedEntity();
            checkImportedEntity(tenantId1, tenantId1, entityGroup, tenantId2, tenantId2, importedEntityGroup);
            checkImportedEntityGroupData(entityGroup, importedEntityGroup);
        }
    }

    @Test
    public void testExportImportEntityGroup_sameTenant() throws Exception {
        for (EntityType groupType : EntityGroup.groupTypes) {
            EntityGroup entityGroup = createEntityGroup(tenantId1, groupType, groupType + " group");
            EntityExportData<EntityGroup> exportData = exportEntity(tenantAdmin1, entityGroup.getId());

            EntityGroup importedEntityGroup = importEntity(tenantAdmin1, exportData).getSavedEntity();
            checkImportedEntity(tenantId1, tenantId1, entityGroup, tenantId1, tenantId1, importedEntityGroup);
            checkImportedEntityGroupData(entityGroup, importedEntityGroup);
        }
    }

    @Test
    public void testExportImportGroupWithPermissions_betweenTenants() throws Exception {
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(tenantId1, null, "Role for User group 1", List.of(Operation.READ));
        createGroupPermission(tenantId1, userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);

        EntityExportData<Role> roleExportData = exportEntity(tenantAdmin1, role.getId());
        EntityExportData<EntityGroup> deviceGroupExportData = exportEntity(tenantAdmin1, deviceGroup.getId());
        EntityExportData<EntityGroup> userGroupExportData = exportEntity(tenantAdmin1, userGroup.getId(), EntityExportSettings.builder()
                .exportPermissions(true)
                .build());

        Role importedRole = importEntity(tenantAdmin2, roleExportData).getSavedEntity();
        checkImportedEntity(tenantId1, role, tenantId2, importedRole);
        assertThat(importedRole.getName()).isEqualTo(role.getName());
        assertThat(importedRole.getPermissions()).isEqualTo(role.getPermissions());

        EntityGroup importedDeviceGroup = importEntity(tenantAdmin2, deviceGroupExportData).getSavedEntity();
        checkImportedEntity(tenantId1, tenantId1, deviceGroup, tenantId2, tenantId2, importedDeviceGroup);

        EntityGroup importedUserGroup = importEntity(tenantAdmin2, userGroupExportData, EntityImportSettings.builder()
                .saveUserGroupPermissions(true)
                .build()).getSavedEntity();
        checkImportedEntity(tenantId1, tenantId1, userGroup, tenantId2, tenantId2, importedUserGroup);

        List<GroupPermission> importedGroupPermissions = groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(tenantId2, importedUserGroup.getId());
        assertThat(importedGroupPermissions).size().isOne();
        assertThat(importedGroupPermissions).satisfies(importedGroupPermission -> {
            assertThat(importedGroupPermission.getRoleId()).isEqualTo(importedRole.getId());
            assertThat(importedGroupPermission.getEntityGroupId()).isEqualTo(importedDeviceGroup.getId());
            assertThat(importedGroupPermission.getEntityGroupType()).isEqualTo(EntityType.DEVICE);
        }, Index.atIndex(0));
    }

    @Test
    public void testExportImportGroupWithPermissions_sameTenant() throws Exception {
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(tenantId1, null, "Role for User group 1", List.of(Operation.READ));
        GroupPermission groupPermission = createGroupPermission(tenantId1, userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);

        EntityExportData<EntityGroup> userGroupExportData = exportEntity(tenantAdmin1, userGroup.getId(), EntityExportSettings.builder()
                .exportPermissions(true)
                .build());

        EntityGroup importedUserGroup = importEntity(tenantAdmin1, userGroupExportData, EntityImportSettings.builder()
                .saveUserGroupPermissions(true)
                .build()).getSavedEntity();
        checkImportedEntity(tenantId1, tenantId1, userGroup, tenantId1, tenantId1, importedUserGroup);

        List<GroupPermission> groupPermissions = groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(tenantId1, userGroup.getId());
        assertThat(groupPermissions).hasOnlyOneElementSatisfying(permission -> {
            assertThat(permission).isEqualTo(groupPermission);
        });
    }

    @Test
    public void testExportImportGroupWithPermissions_betweenTenants_permissionsUpdated() throws Exception {
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(tenantId1, null, "Role for User group 1", List.of(Operation.READ));
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        GroupPermission groupPermission = createGroupPermission(tenantId1, userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);

        EntityExportData<Role> roleExportData = exportEntity(tenantAdmin1, role.getId());
        EntityExportData<EntityGroup> deviceGroupExportData = exportEntity(tenantAdmin1, deviceGroup.getId());
        EntityExportData<EntityGroup> userGroupExportData = exportEntity(tenantAdmin1, userGroup.getId(), EntityExportSettings.builder()
                .exportPermissions(true)
                .build());

        Role importedRole = importEntity(tenantAdmin2, roleExportData).getSavedEntity();
        EntityGroup importedDeviceGroup = importEntity(tenantAdmin2, deviceGroupExportData).getSavedEntity();
        EntityGroup importedUserGroup = importEntity(tenantAdmin2, userGroupExportData, EntityImportSettings.builder()
                .saveUserGroupPermissions(true)
                .build()).getSavedEntity();

        List<GroupPermission> importedGroupPermissions = groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(tenantId2, importedUserGroup.getId());
        assertThat(importedGroupPermissions).size().isOne();
        assertThat(importedGroupPermissions).satisfies(importedGroupPermission -> {
            assertThat(importedGroupPermission.getRoleId()).isEqualTo(importedRole.getId());
            assertThat(importedGroupPermission.getEntityGroupId()).isEqualTo(importedDeviceGroup.getId());
            assertThat(importedGroupPermission.getEntityGroupType()).isEqualTo(EntityType.DEVICE);
        }, Index.atIndex(0));

        groupPermissionService.deleteGroupPermissionsByTenantIdAndUserGroupId(tenantId1, userGroup.getId());
        role = createGenericRole(tenantId1, null, "Read devices", Map.of(
                Resource.DEVICE, List.of(Operation.READ),
                Resource.DEVICE_GROUP, List.of(Operation.READ)
        ));
        groupPermission = createGroupPermission(tenantId1, userGroup.getId(), role.getId());

        roleExportData = exportEntity(tenantAdmin1, role.getId());
        userGroupExportData = exportEntity(tenantAdmin1, userGroup.getId(), EntityExportSettings.builder()
                .exportPermissions(true)
                .build());

        Role newImportedRole = importEntity(tenantAdmin2, roleExportData).getSavedEntity();
        importEntity(tenantAdmin2, userGroupExportData, EntityImportSettings.builder()
                .saveUserGroupPermissions(true).build());
        List<GroupPermission> updatedGroupPermissions = groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(tenantId2, importedUserGroup.getId());

        assertThat(updatedGroupPermissions).size().isOne();
        assertThat(updatedGroupPermissions).hasOnlyOneElementSatisfying(newGroupPermission -> {
            assertThat(newGroupPermission.getEntityGroupId()).matches(entityGroupId -> entityGroupId == null || entityGroupId.isNullUid());
            assertThat(newGroupPermission.getRoleId()).isEqualTo(newImportedRole.getId());
        });
    }

    @Test
    public void testExternalIdsInExportData() throws Exception {
        Customer customer = createCustomer(tenantId1, null, "Customer 1");
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "Asset profile 1");
        Asset asset = createAsset(tenantId1, customer.getId(), assetProfile.getId(), null, "Asset 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1", asset.getId());
        Dashboard dashboard = createDashboard(tenantId1, customer.getId(), "Dashboard 1", asset.getId());

        assetProfile.setDefaultRuleChainId(ruleChain.getId());
        assetProfile.setDefaultDashboardId(dashboard.getId());
        assetProfile = assetProfileService.saveAssetProfile(assetProfile);

        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        Device device = createDevice(tenantId1, customer.getId(), deviceProfile.getId(), null, "Device 1");
        EntityView entityView = createEntityView(tenantId1, customer.getId(), device.getId(), "Entity view 1");
        Converter converter = createConverter(tenantId1, ConverterType.UPLINK, "Converter 1");
        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");

        Map<EntityId, EntityId> ids = new HashMap<>();
        for (EntityId entityId : List.of(customer.getId(), ruleChain.getId(), dashboard.getId(), assetProfile.getId(), asset.getId(),
                deviceProfile.getId(), device.getId(), entityView.getId(), converter.getId(), integration.getId(),
                ruleChain.getId(), dashboard.getId())) {
            EntityExportData exportData = exportEntity(getSecurityUser(tenantAdmin1), entityId);
            EntityImportResult importResult = importEntity(getSecurityUser(tenantAdmin2), exportData, EntityImportSettings.builder()
                    .saveCredentials(false)
                    .autoGenerateIntegrationKey(true)
                    .build());
            ids.put(entityId, (EntityId) importResult.getSavedEntity().getId());
        }

        AssetProfile exportedAssetProfile = (AssetProfile) exportEntity(tenantAdmin2, (AssetProfileId) ids.get(assetProfile.getId())).getEntity();
        assertThat(exportedAssetProfile.getDefaultRuleChainId()).isEqualTo(ruleChain.getId());
        assertThat(exportedAssetProfile.getDefaultDashboardId()).isEqualTo(dashboard.getId());

        Asset exportedAsset = (Asset) exportEntity(tenantAdmin2, (AssetId) ids.get(asset.getId())).getEntity();
        assertThat(exportedAsset.getCustomerId()).isEqualTo(customer.getId());

        EntityExportData<RuleChain> ruleChainExportData = exportEntity(tenantAdmin2, (RuleChainId) ids.get(ruleChain.getId()));
        TbMsgGeneratorNodeConfiguration exportedRuleNodeConfig = ((RuleChainExportData) ruleChainExportData).getMetaData().getNodes().stream()
                .filter(node -> node.getType().equals(TbMsgGeneratorNode.class.getName())).findFirst()
                .map(RuleNode::getConfiguration).map(config -> JacksonUtil.treeToValue(config, TbMsgGeneratorNodeConfiguration.class)).orElse(null);
        assertThat(exportedRuleNodeConfig.getOriginatorId()).isEqualTo(asset.getId().toString());

        Dashboard exportedDashboard = (Dashboard) exportEntity(tenantAdmin2, (DashboardId) ids.get(dashboard.getId())).getEntity();
        assertThat(exportedDashboard.getCustomerId()).isEqualTo(customer.getId());
        String exportedEntityAliasAssetId = exportedDashboard.getConfiguration().get("entityAliases").elements().next()
                .get("filter").get("entityList").elements().next().asText();
        assertThat(exportedEntityAliasAssetId).isEqualTo(asset.getId().toString());

        DeviceProfile exportedDeviceProfile = (DeviceProfile) exportEntity(tenantAdmin2, (DeviceProfileId) ids.get(deviceProfile.getId())).getEntity();
        assertThat(exportedDeviceProfile.getDefaultRuleChainId()).isEqualTo(ruleChain.getId());
        assertThat(exportedDeviceProfile.getDefaultDashboardId()).isEqualTo(dashboard.getId());

        Device exportedDevice = (Device) exportEntity(tenantAdmin2, (DeviceId) ids.get(device.getId())).getEntity();
        assertThat(exportedDevice.getCustomerId()).isEqualTo(customer.getId());
        assertThat(exportedDevice.getDeviceProfileId()).isEqualTo(deviceProfile.getId());

        EntityView exportedEntityView = (EntityView) exportEntity(tenantAdmin2, (EntityViewId) ids.get(entityView.getId())).getEntity();
        assertThat(exportedEntityView.getCustomerId()).isEqualTo(customer.getId());
        assertThat(exportedEntityView.getEntityId()).isEqualTo(device.getId());

        Integration exportedIntegration = (Integration) exportEntity(tenantAdmin2, (IntegrationId) ids.get(integration.getId())).getEntity();
        assertThat(exportedIntegration.getDefaultConverterId()).isEqualTo(converter.getId());

        deviceProfile.setDefaultDashboardId(null);
        deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfile importedDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId2, (DeviceProfileId) ids.get(deviceProfile.getId()));
        importedDeviceProfile.setDefaultDashboardId(null);
        deviceProfileService.saveDeviceProfile(importedDeviceProfile);
    }

}
