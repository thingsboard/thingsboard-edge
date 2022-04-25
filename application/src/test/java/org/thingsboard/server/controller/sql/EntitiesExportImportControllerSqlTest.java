/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.controller.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import org.assertj.core.data.Index;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.query.EntityGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.BaseEntitiesExportImportControllerTest;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.sync.exporting.data.DeviceExportData;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.EntityGroupExportData;
import org.thingsboard.server.service.sync.exporting.data.RuleChainExportData;
import org.thingsboard.server.service.sync.exporting.data.request.CustomEntityFilterExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.data.request.EntityListExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.EntityTypeExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.ExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.SingleEntityExportRequest;
import org.thingsboard.server.service.sync.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.importing.data.EntityImportSettings;
import org.thingsboard.server.service.sync.importing.data.request.ImportRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class EntitiesExportImportControllerSqlTest extends BaseEntitiesExportImportControllerTest {

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;
    @SpyBean
    private EntityActionService entityActionService;
    @SpyBean
    private TbClusterService clusterService;
    @SpyBean
    private OtaPackageStateService otaPackageStateService;
    @Autowired
    private OwnersCacheService ownersCacheService;
    @SpyBean
    private UserPermissionsService userPermissionsService;
    @SpyBean
    private AccessControlService accessControlService;

    @Test
    public void testExportImportAsset_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Asset asset = createAsset(tenantId1, null, null, "AB", "Asset of tenant 1");
        EntityExportData<Asset> exportData = exportSingleEntity(asset.getId());
        assertThat(exportData.getEntity()).isEqualTo(asset);

        logInAsTenantAdmin2();
        EntityImportResult<Asset> importResult = importEntity(exportData);

        checkImportedEntity(tenantId1, asset, tenantId2, importResult.getSavedEntity());
        checkImportedAssetData(asset, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportAsset_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Asset asset = createAsset(tenantId1, null, null, "AB", "Asset v1.0");
        EntityExportData<Asset> exportData = exportSingleEntity(asset.getId());

        EntityImportResult<Asset> importResult = importEntity(exportData);

        checkImportedEntity(tenantId1, asset, tenantId1, importResult.getSavedEntity());
        checkImportedAssetData(asset, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportAsset_sameTenant_withCustomer() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantId1, null, "My customer");
        Asset asset = createAsset(tenantId1, customer.getId(), null, "AB", "My asset");

        Asset importedAsset = importEntity(this.<Asset, AssetId>exportSingleEntity(asset.getId())).getSavedEntity();

        assertThat(importedAsset.getCustomerId()).isEqualTo(asset.getCustomerId());
    }


    @Test
    public void testExportImportCustomer_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantAdmin1.getTenantId(), null, "Customer of tenant 1");
        EntityExportData<Customer> exportData = exportSingleEntity(customer.getId());
        assertThat(exportData.getEntity()).isEqualTo(customer);

        logInAsTenantAdmin2();
        EntityImportResult<Customer> importResult = importEntity(exportData);

        checkImportedEntity(tenantId1, customer, tenantId2, importResult.getSavedEntity());
        checkImportedCustomerData(customer, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportCustomer_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantAdmin1.getTenantId(), null, "Customer v1.0");
        EntityExportData<Customer> exportData = exportSingleEntity(customer.getId());

        EntityImportResult<Customer> importResult = importEntity(exportData);

        checkImportedEntity(tenantId1, customer, tenantId1, importResult.getSavedEntity());
        checkImportedCustomerData(customer, importResult.getSavedEntity());
    }


    @Test
    public void testExportImportDeviceWithProfile_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile of tenant 1");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), null, "Device of tenant 1");
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());

        EntityExportData<DeviceProfile> profileExportData = exportSingleEntity(deviceProfile.getId());
        assertThat(profileExportData.getEntity()).isEqualTo(deviceProfile);

        EntityExportData<Device> deviceExportData = exportSingleEntity(device.getId());
        assertThat(deviceExportData.getEntity()).isEqualTo(device);
        assertThat(((DeviceExportData) deviceExportData).getCredentials()).isEqualTo(credentials);
        DeviceCredentials exportedCredentials = ((DeviceExportData) deviceExportData).getCredentials();
        exportedCredentials.setCredentialsId(credentials.getCredentialsId() + "a");

        logInAsTenantAdmin2();
        EntityImportResult<DeviceProfile> profileImportResult = importEntity(profileExportData);
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, profileImportResult.getSavedEntity());
        checkImportedDeviceProfileData(deviceProfile, profileImportResult.getSavedEntity());


        EntityImportResult<Device> deviceImportResult = importEntity(deviceExportData);
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
        logInAsTenantAdmin1();
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), null, "Device v1.0");
        device.setFirmwareId(firmware.getId());
        device.setSoftwareId(software.getId());
        device = deviceService.saveDevice(device);

        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());

        EntityExportData<Device> deviceExportData = exportSingleEntity(device.getId());

        EntityImportResult<Device> importResult = importEntity(deviceExportData);
        Device importedDevice = importResult.getSavedEntity();

        checkImportedEntity(tenantId1, device, tenantId1, importResult.getSavedEntity());
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(device.getDeviceProfileId());
        assertThat(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId())).isEqualTo(credentials);
        assertThat(importedDevice.getFirmwareId()).isEqualTo(firmware.getId());
        assertThat(importedDevice.getSoftwareId()).isEqualTo(software.getId());
    }


    @Test
    public void testExportImportDashboard_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, null, "Dashboard of tenant 1");

        EntityExportData<Dashboard> exportData = exportSingleEntity(dashboard.getId());
        assertThat(exportData.getEntity()).isEqualTo(dashboard);

        logInAsTenantAdmin2();
        EntityImportResult<Dashboard> importResult = importEntity(exportData);
        checkImportedEntity(tenantId1, dashboard, tenantId2, importResult.getSavedEntity());
        checkImportedDashboardData(dashboard, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportDashboard_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, null, "Dashboard v1.0");

        EntityExportData<Dashboard> exportData = exportSingleEntity(dashboard.getId());

        EntityImportResult<Dashboard> importResult = importEntity(exportData);
        checkImportedEntity(tenantId1, dashboard, tenantId1, importResult.getSavedEntity());
        checkImportedDashboardData(dashboard, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportDashboard_betweenTenants_withCustomer_updated() throws Exception {
        logInAsTenantAdmin1();
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, null, "Dashboard of tenant 1");

        EntityExportData<Dashboard> exportData = exportSingleEntity(dashboard.getId());

        logInAsTenantAdmin2();
        Dashboard importedDashboard = (Dashboard) importEntities(List.of(exportData)).get(0).getSavedEntity();
        checkImportedEntity(tenantId1, dashboard, tenantId2, importedDashboard);

        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantId1, null, "Customer 1");
        EntityExportData<Customer> customerExportData = exportSingleEntity(customer.getId());
        dashboard.setCustomerId(customer.getId());
        dashboardService.saveDashboard(dashboard);
        exportData = exportSingleEntity(dashboard.getId());

        logInAsTenantAdmin2();
        Customer importedCustomer = (Customer) importEntities(List.of(customerExportData)).get(0).getSavedEntity();
        importedDashboard = (Dashboard) importEntities(List.of(exportData)).get(0).getSavedEntity();
        assertThat(importedDashboard.getCustomerId()).isEqualTo(importedCustomer.getId());
    }

    @Test
    public void testExportImportDashboard_betweenTenants_withEntityAliases() throws Exception {
        logInAsTenantAdmin1();
        Asset asset1 = createAsset(tenantId1, null, null, "A", "Asset 1");
        Asset asset2 = createAsset(tenantId1, null, null, "A", "Asset 2");
        Dashboard dashboard = createDashboard(tenantId1, null, null, "Dashboard 1");

        String entityAliases = "{\n" +
                "\t\"23c4185d-1497-9457-30b2-6d91e69a5b2c\": {\n" +
                "\t\t\"alias\": \"assets\",\n" +
                "\t\t\"filter\": {\n" +
                "\t\t\t\"entityList\": [\n" +
                "\t\t\t\t\"" + asset1.getId().toString() + "\",\n" +
                "\t\t\t\t\"" + asset2.getId().toString() + "\"\n" +
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
        dashboard = dashboardService.saveDashboard(dashboard);

        EntityTypeExportRequest assetsExportRequest = new EntityTypeExportRequest();
        assetsExportRequest.setEntityType(EntityType.ASSET);
        assetsExportRequest.setPageSize(10);
        assetsExportRequest.setExportSettings(new EntityExportSettings());
        EntityTypeExportRequest dashboardsExportRequest = new EntityTypeExportRequest();
        dashboardsExportRequest.setEntityType(EntityType.DASHBOARD);
        dashboardsExportRequest.setPageSize(10);
        dashboardsExportRequest.setExportSettings(new EntityExportSettings());
        List<EntityExportData<?>> exportDataList = exportEntities(List.of(assetsExportRequest, dashboardsExportRequest));

        logInAsTenantAdmin2();
        Map<EntityType, List<EntityImportResult<?>>> importResults = importEntities(exportDataList).stream().collect(Collectors.groupingBy(EntityImportResult::getEntityType));
        Asset importedAsset1 = (Asset) importResults.get(EntityType.ASSET).get(0).getSavedEntity();
        Asset importedAsset2 = (Asset) importResults.get(EntityType.ASSET).get(1).getSavedEntity();
        Dashboard importedDashboard = (Dashboard) importResults.get(EntityType.DASHBOARD).get(0).getSavedEntity();

        Set<String> entityAliasEntitiesIds = Streams.stream(importedDashboard.getConfiguration()
                        .get("entityAliases").elements().next().get("filter").get("entityList").elements())
                .map(JsonNode::asText).collect(Collectors.toSet());
        assertThat(entityAliasEntitiesIds).doesNotContain(asset1.getId().toString(), asset2.getId().toString());
        assertThat(entityAliasEntitiesIds).contains(importedAsset1.getId().toString(), importedAsset2.getId().toString());
    }


    @Test
    public void testExportImportRuleChain_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain of tenant 1");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());

        EntityExportData<RuleChain> exportData = exportSingleEntity(ruleChain.getId());
        assertThat(exportData.getEntity()).isEqualTo(ruleChain);
        assertThat(((RuleChainExportData) exportData).getMetaData()).isEqualTo(metaData);

        logInAsTenantAdmin2();
        EntityImportResult<RuleChain> importResult = importEntity(exportData);
        RuleChain importedRuleChain = importResult.getSavedEntity();
        RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId2, importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId2, importResult.getSavedEntity());
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    @Test
    public void testExportImportRuleChain_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain v1.0");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());

        EntityExportData<RuleChain> exportData = exportSingleEntity(ruleChain.getId());

        EntityImportResult<RuleChain> importResult = importEntity(exportData);
        RuleChain importedRuleChain = importResult.getSavedEntity();
        RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId1, importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId1, importResult.getSavedEntity());
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    @Test
    public void testExportImportConverterWithIntegration_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Converter converter = createConverter(tenantId1, ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");

        EntityListExportRequest exportRequest = new EntityListExportRequest();
        exportRequest.setExportSettings(new EntityExportSettings());
        exportRequest.setEntitiesIds(List.of(integration.getId(), converter.getId()));
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);
        this.<EntityExportData<Integration>>updateExportData(exportDataList, EntityType.INTEGRATION, integrationExportData -> {
            integrationExportData.getEntity().setRoutingKey(RandomStringUtils.randomAlphanumeric(10));
        });

        logInAsTenantAdmin2();
        Map<EntityType, EntityImportResult<?>> importResults = importEntities(exportDataList).stream().collect(Collectors.toMap(EntityImportResult::getEntityType, r -> r));
        Converter importedConverter = (Converter) importResults.get(EntityType.CONVERTER).getSavedEntity();
        checkImportedEntity(tenantId1, converter, tenantId2, importedConverter);
        checkImportedConverterData(converter, importedConverter);

        Integration importedIntegration = (Integration) importResults.get(EntityType.INTEGRATION).getSavedEntity();
        checkImportedEntity(tenantId1, integration, tenantId2, importedIntegration);
        checkImportedIntegrationData(integration, importedIntegration);
    }

    @Test
    public void testExportImportConverterWithIntegration_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Converter converter = createConverter(tenantId1, ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");

        EntityListExportRequest exportRequest = new EntityListExportRequest();
        exportRequest.setExportSettings(new EntityExportSettings());
        exportRequest.setEntitiesIds(List.of(integration.getId(), converter.getId()));
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        Map<EntityType, EntityImportResult<?>> importResults = importEntities(exportDataList).stream().collect(Collectors.toMap(EntityImportResult::getEntityType, r -> r));
        Converter importedConverter = (Converter) importResults.get(EntityType.CONVERTER).getSavedEntity();
        checkImportedEntity(tenantId1, converter, tenantId1, importedConverter);
        checkImportedConverterData(converter, importedConverter);

        Integration importedIntegration = (Integration) importResults.get(EntityType.INTEGRATION).getSavedEntity();
        checkImportedEntity(tenantId1, integration, tenantId1, importedIntegration);
        checkImportedIntegrationData(integration, importedIntegration);
    }


    @Test
    public void testExportImportEntityGroup_betweenTenants() throws Exception {
        for (EntityType groupType : EntityGroup.groupTypes) {
            logInAsTenantAdmin1();
            EntityGroup entityGroup = createEntityGroup(tenantId1, groupType, groupType + " group");
            EntityExportData<EntityGroup> exportData = exportSingleEntity(entityGroup.getId());

            logInAsTenantAdmin2();
            EntityGroup importedEntityGroup = (EntityGroup) importEntities(List.of(exportData)).get(0).getSavedEntity();
            checkImportedEntity(tenantId1, tenantId1, entityGroup, tenantId2, tenantId2, importedEntityGroup);
            checkImportedEntityGroupData(entityGroup, importedEntityGroup);
        }
    }

    @Test
    public void testExportImportEntityGroup_sameTenant() throws Exception {
        for (EntityType groupType : EntityGroup.groupTypes) {
            logInAsTenantAdmin1();
            EntityGroup entityGroup = createEntityGroup(tenantId1, groupType, groupType + " group");
            EntityExportData<EntityGroup> exportData = exportSingleEntity(entityGroup.getId());

            EntityGroup importedEntityGroup = (EntityGroup) importEntities(List.of(exportData)).get(0).getSavedEntity();
            checkImportedEntity(tenantId1, tenantId1, entityGroup, tenantId1, tenantId1, importedEntityGroup);
            checkImportedEntityGroupData(entityGroup, importedEntityGroup);
        }
    }

    @Test
    public void textExportImportAssetWithGroup_betweenTenants_groupsUpdated() throws Exception {
        logInAsTenantAdmin1();

        EntityGroup assetGroup1 = createEntityGroup(tenantId1, EntityType.ASSET, "Asset group 1");
        Asset asset1 = createAsset(tenantId1, null, assetGroup1.getId(), "A", "Asset 1");
        Asset asset2 = createAsset(tenantId1, null, assetGroup1.getId(), "A", "Asset 2");

        EntityListExportRequest exportRequest = new EntityListExportRequest();
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportEntityGroupsInfo(true)
                .build());
        exportRequest.setEntitiesIds(List.of(assetGroup1.getId(), asset1.getId(), asset2.getId()));
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        logInAsTenantAdmin2();

        ImportRequest importRequest = new ImportRequest();
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateEntityGroups(true)
                .build());
        importRequest.setExportDataList(exportDataList);

        Map<EntityType, List<ExportableEntity<?>>> importResults = importEntities(importRequest).stream()
                .map(EntityImportResult::getSavedEntity)
                .sorted(Comparator.comparing(HasName::getName))
                .collect(Collectors.groupingBy(e -> e.getId().getEntityType()));

        EntityGroup importedAssetGroup1 = (EntityGroup) importResults.get(EntityType.ENTITY_GROUP).get(0);
        Asset importedAsset1 = (Asset) importResults.get(EntityType.ASSET).get(0);
        Asset importedAsset2 = (Asset) importResults.get(EntityType.ASSET).get(1);

        List<EntityGroup> asset1EntityGroups = entityGroupService.findEntityGroupsForEntity(tenantId2, importedAsset1.getId()).get().stream()
                .map(entityGroupId -> entityGroupService.findEntityGroupById(tenantId2, entityGroupId))
                .collect(Collectors.toList());

        assertThat(asset1EntityGroups).size().isEqualTo(2);
        assertThat(asset1EntityGroups).anyMatch(EntityGroup::isGroupAll);
        assertThat(asset1EntityGroups).anySatisfy(entityGroup -> {
            assertThat(entityGroup.getId()).isEqualTo(importedAssetGroup1.getId());
        });

        List<EntityGroup> asset2EntityGroups = entityGroupService.findEntityGroupsForEntity(tenantId2, importedAsset2.getId()).get().stream()
                .map(entityGroupId -> entityGroupService.findEntityGroupById(tenantId2, entityGroupId))
                .collect(Collectors.toList());

        assertThat(asset2EntityGroups).size().isEqualTo(2);
        assertThat(asset2EntityGroups).anyMatch(EntityGroup::isGroupAll);
        assertThat(asset2EntityGroups).anySatisfy(entityGroup -> {
            assertThat(entityGroup.getId()).isEqualTo(importedAssetGroup1.getId());
        });

        logInAsTenantAdmin1();

        EntityGroup assetGroup2 = createEntityGroup(tenantId1, EntityType.ASSET, "Asset group 2");
        EntityGroup assetGroup3 = createEntityGroup(tenantId1, EntityType.ASSET, "Asset group 3");

        entityGroupService.removeEntityFromEntityGroup(tenantId1, assetGroup1.getId(), asset2.getId());
        entityGroupService.addEntityToEntityGroup(tenantId1, assetGroup2.getId(), asset2.getId());
        entityGroupService.addEntityToEntityGroup(tenantId1, assetGroup3.getId(), asset2.getId());

        exportRequest.setEntitiesIds(List.of(assetGroup2.getId(), assetGroup3.getId(), asset1.getId(), asset2.getId()));
        exportDataList = exportEntities(exportRequest);

        logInAsTenantAdmin2();

        importRequest.setExportDataList(exportDataList);
        List<ExportableEntity<?>> importedEntities = importEntities(importRequest).stream()
                .map(EntityImportResult::getSavedEntity)
                .collect(Collectors.toList());

        EntityGroup importedAssetGroup2 = (EntityGroup) importedEntities.stream().filter(entity -> entity instanceof EntityGroup &&
                entity.getName().equalsIgnoreCase("asset group 2")).findFirst().get();
        EntityGroup importedAssetGroup3 = (EntityGroup) importedEntities.stream().filter(entity -> entity instanceof EntityGroup &&
                entity.getName().equalsIgnoreCase("asset group 3")).findFirst().get();

        asset1EntityGroups = entityGroupService.findEntityGroupsForEntity(tenantId2, importedAsset1.getId()).get().stream()
                .map(entityGroupId -> entityGroupService.findEntityGroupById(tenantId2, entityGroupId))
                .collect(Collectors.toList());

        assertThat(asset1EntityGroups).size().isEqualTo(2);
        assertThat(asset1EntityGroups).anyMatch(EntityGroup::isGroupAll);
        assertThat(asset1EntityGroups).anySatisfy(entityGroup -> {
            assertThat(entityGroup.getId()).isEqualTo(importedAssetGroup1.getId());
        });

        asset2EntityGroups = entityGroupService.findEntityGroupsForEntity(tenantId2, importedAsset2.getId()).get().stream()
                .map(entityGroupId -> entityGroupService.findEntityGroupById(tenantId2, entityGroupId))
                .collect(Collectors.toList());
        assertThat(asset2EntityGroups).size().isEqualTo(3);
        assertThat(asset2EntityGroups).anyMatch(EntityGroup::isGroupAll);
        assertThat(asset2EntityGroups).noneMatch(entityGroup -> entityGroup.getId().equals(importedAssetGroup1.getId()));
        assertThat(asset2EntityGroups).anyMatch(entityGroup -> entityGroup.getId().equals(importedAssetGroup2.getId()));
        assertThat(asset2EntityGroups).anyMatch(entityGroup -> entityGroup.getId().equals(importedAssetGroup3.getId()));
    }


    @Test
    public void testExportImportBatch_betweenTenants() throws Exception {
        logInAsTenantAdmin1();

        Customer customer = createCustomer(tenantId1, null, "Customer 1");
        Asset asset = createAsset(tenantId1, customer.getId(), null, "A", "Customer 1 - Asset 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Dashboard dashboard = createDashboard(tenantId1, null, null, "Customer 1 - Dashboard 1");
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Device device = createDevice(tenantId1, customer.getId(), deviceProfile.getId(), deviceGroup.getId(), "Customer 1 - Device 1");
        Converter converter = createConverter(tenantId1, ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");

        EntityListExportRequest exportRequest = new EntityListExportRequest();
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportEntityGroupsInfo(true)
                .build());
        exportRequest.setEntitiesIds(List.of(customer.getId(), asset.getId(), device.getId(), ruleChain.getId(), deviceProfile.getId(),
                dashboard.getId(), deviceGroup.getId(), converter.getId(), integration.getId()));
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        this.<DeviceExportData>updateExportData(exportDataList, EntityType.DEVICE, deviceExportData -> {
            deviceExportData.getCredentials().setCredentialsId(RandomStringUtils.randomAlphanumeric(10));
        });
        this.<EntityExportData<Integration>>updateExportData(exportDataList, EntityType.INTEGRATION, integrationExportData -> {
            integrationExportData.getEntity().setRoutingKey(RandomStringUtils.randomAlphanumeric(10));
        });

        logInAsTenantAdmin2();
        ImportRequest importRequest = new ImportRequest();
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateEntityGroups(true)
                .build());
        importRequest.setExportDataList(exportDataList);
        Map<EntityType, EntityImportResult<?>> importResults = importEntities(importRequest).stream()
                .collect(Collectors.toMap(EntityImportResult::getEntityType, r -> r));

        Customer importedCustomer = (Customer) importResults.get(EntityType.CUSTOMER).getSavedEntity();
        checkImportedEntity(tenantId1, customer, tenantId2, importedCustomer);

        Asset importedAsset = (Asset) importResults.get(EntityType.ASSET).getSavedEntity();
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);
        assertThat(importedAsset.getCustomerId()).isEqualTo(importedCustomer.getId());

        RuleChain importedRuleChain = (RuleChain) importResults.get(EntityType.RULE_CHAIN).getSavedEntity();
        checkImportedEntity(tenantId1, ruleChain, tenantId2, importedRuleChain);

        Dashboard importedDashboard = (Dashboard) importResults.get(EntityType.DASHBOARD).getSavedEntity();
        checkImportedEntity(tenantId1, dashboard, tenantId2, importedDashboard);

        DeviceProfile importedDeviceProfile = (DeviceProfile) importResults.get(EntityType.DEVICE_PROFILE).getSavedEntity();
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, importedDeviceProfile);
        assertThat(importedDeviceProfile.getDefaultRuleChainId()).isEqualTo(importedRuleChain.getId());
        assertThat(importedDeviceProfile.getDefaultDashboardId()).isEqualTo(importedDashboard.getId());

        EntityGroup importedDeviceGroup = (EntityGroup) importResults.get(EntityType.ENTITY_GROUP).getSavedEntity();
        checkImportedEntity(tenantId1, tenantId1, deviceGroup, tenantId2, tenantId2, importedDeviceGroup);

        Device importedDevice = (Device) importResults.get(EntityType.DEVICE).getSavedEntity();
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        assertThat(importedDevice.getCustomerId()).isEqualTo(importedCustomer.getId());
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(importedDeviceProfile.getId());
        assertThat(entityGroupService.isEntityInGroup(importedDevice.getId(), importedDeviceGroup.getId())).isTrue();

        Converter importedConverter = (Converter) importResults.get(EntityType.CONVERTER).getSavedEntity();
        checkImportedEntity(tenantId1, converter, tenantId2, importedConverter);

        Integration importedIntegration = (Integration) importResults.get(EntityType.INTEGRATION).getSavedEntity();
        checkImportedEntity(tenantId1, integration, tenantId2, importedIntegration);
        assertThat(importedIntegration.getDefaultConverterId()).isEqualTo(importedConverter.getId());
    }


    @Test
    public void testExportImportWithInboundRelations_betweenTenants() throws Exception {
        logInAsTenantAdmin1();

        Asset asset = createAsset(tenantId1, null, null, "A", "Asset 1");
        Device device = createDevice(tenantId1, null, null, null, "Device 1");
        EntityRelation relation = createRelation(asset.getId(), device.getId());

        EntityListExportRequest exportRequest = new EntityListExportRequest();
        exportRequest.setEntitiesIds(List.of(asset.getId(), device.getId()));
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        EntityExportData<?> deviceExportData = exportDataList.stream().filter(exportData -> exportData.getEntityType() == EntityType.DEVICE).findFirst().orElse(null);
        assertThat(deviceExportData.getRelations()).size().isOne();
        assertThat(deviceExportData.getRelations().get(0)).matches(entityRelation -> {
            return entityRelation.getFrom().equals(asset.getId()) && entityRelation.getTo().equals(device.getId());
        });
        ((DeviceExportData) deviceExportData).getCredentials().setCredentialsId("ab");
        ((Device) deviceExportData.getEntity()).setDeviceProfileId(null);

        logInAsTenantAdmin2();

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(exportDataList);
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateRelations(true)
                .build());
        Map<EntityType, EntityImportResult<?>> importResults = importEntities(importRequest).stream().collect(Collectors.toMap(EntityImportResult::getEntityType, r -> r));

        Device importedDevice = (Device) importResults.get(EntityType.DEVICE).getSavedEntity();
        Asset importedAsset = (Asset) importResults.get(EntityType.ASSET).getSavedEntity();
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
        logInAsTenantAdmin1();

        Asset asset = createAsset(tenantId1, null, null, "A", "Asset 1");
        Device device = createDevice(tenantId1, null, null, null, "Device 1");
        EntityRelation relation = createRelation(asset.getId(), device.getId());

        EntityListExportRequest exportRequest = new EntityListExportRequest();
        exportRequest.setEntitiesIds(List.of(asset.getId(), device.getId()));
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        assertThat(exportDataList).allMatch(exportData -> exportData.getRelations().size() == 1);

        EntityExportData<?> deviceExportData = exportDataList.stream().filter(exportData -> exportData.getEntityType() == EntityType.DEVICE).findFirst().orElse(null);
        ((DeviceExportData) deviceExportData).getCredentials().setCredentialsId("ab");
        ((Device) deviceExportData.getEntity()).setDeviceProfileId(null);

        logInAsTenantAdmin2();

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(exportDataList);
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateRelations(true)
                .build());
        Map<EntityType, EntityImportResult<?>> importResults = importEntities(importRequest).stream().collect(Collectors.toMap(EntityImportResult::getEntityType, r -> r));

        Device importedDevice = (Device) importResults.get(EntityType.DEVICE).getSavedEntity();
        Asset importedAsset = (Asset) importResults.get(EntityType.ASSET).getSavedEntity();

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
        logInAsTenantAdmin1();

        Asset asset = createAsset(tenantId1, null, null, "A", "Asset 1");
        Device device1 = createDevice(tenantId1, null, null, null, "Device 1");
        EntityRelation relation1 = createRelation(asset.getId(), device1.getId());

        SingleEntityExportRequest exportRequest = new SingleEntityExportRequest();
        exportRequest.setEntityId(asset.getId());
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        EntityExportData<Asset> assetExportData = (EntityExportData<Asset>) exportEntities(exportRequest).get(0);
        assertThat(assetExportData.getRelations()).size().isOne();

        Device device2 = createDevice(tenantId1, null, null, null, "Device 2");
        EntityRelation relation2 = createRelation(asset.getId(), device2.getId());

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(List.of(assetExportData));
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateRelations(true)
                .build());

        importEntities(importRequest);

        List<EntityRelation> relations = relationService.findByFrom(TenantId.SYS_TENANT_ID, asset.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).contains(relation1);
        assertThat(relations).doesNotContain(relation2);
    }

    @Test
    public void textExportImportWithRelations_sameTenant_removeExisting() throws Exception {
        logInAsTenantAdmin1();

        Asset asset1 = createAsset(tenantId1, null, null, "A", "Asset 1");
        Device device = createDevice(tenantId1, null, null, null, "Device 1");
        EntityRelation relation1 = createRelation(asset1.getId(), device.getId());

        SingleEntityExportRequest exportRequest = new SingleEntityExportRequest();
        exportRequest.setEntityId(device.getId());
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        EntityExportData<?> deviceExportData = exportEntities(exportRequest).get(0);
        assertThat(deviceExportData.getRelations()).size().isOne();

        Asset asset2 = createAsset(tenantId1, null, null, "A", "Asset 2");
        EntityRelation relation2 = createRelation(asset2.getId(), device.getId());

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(List.of(deviceExportData));
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateRelations(true)
                .build());

        importEntities(importRequest);

        List<EntityRelation> relations = relationService.findByTo(TenantId.SYS_TENANT_ID, device.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).contains(relation1);
        assertThat(relations).doesNotContain(relation2);
    }


    @Test
    public void testExportImportDeviceProfile_betweenTenants_findExistingByName() throws Exception {
        logInAsTenantAdmin1();
        DeviceProfile defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId1);

        EntityListExportRequest exportRequest = new EntityListExportRequest();
        exportRequest.setEntitiesIds(List.of(defaultDeviceProfile.getId()));
        exportRequest.setExportSettings(new EntityExportSettings());
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        logInAsTenantAdmin2();
        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(exportDataList);
        importRequest.setImportSettings(EntityImportSettings.builder()
                .findExistingByName(false)
                .build());
        assertThatThrownBy(() -> {
            importEntities(importRequest);
        }).hasMessageContaining("default device profile is present");

        importRequest.getImportSettings().setFindExistingByName(true);
        importEntities(importRequest);
        checkImportedEntity(tenantId1, defaultDeviceProfile, tenantId2, deviceProfileService.findDefaultDeviceProfile(tenantId2));
    }


    @Test
    public void testExportRequests() throws Exception {
        logInAsTenantAdmin1();

        Device device = createDevice(tenantId1, null, null, null, "Device 1");
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Asset asset = createAsset(tenantId1, null, null, "A", "Asset 1");
        Dashboard dashboard = createDashboard(tenantId1, null, null, "Dashboard 1");
        Customer customer = createCustomer(tenantId1, null, "Customer 1");
        Converter converter = createConverter(tenantId1, ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");

        Map<EntityType, ExportableEntity<?>> entities = Map.of(
                EntityType.DEVICE, device, EntityType.DEVICE_PROFILE, deviceProfile,
                EntityType.RULE_CHAIN, ruleChain, EntityType.ASSET, asset,
                EntityType.DASHBOARD, dashboard, EntityType.CUSTOMER, customer,
                EntityType.CONVERTER, converter, EntityType.INTEGRATION, integration
        );

        for (ExportableEntity<?> entity : entities.values()) {
            testEntityTypeExportRequest(entity);
            testCustomEntityFilterExportRequest(entity);
        }
    }

    private void testEntityTypeExportRequest(ExportableEntity<?> entity) throws Exception {
        EntityTypeExportRequest exportRequest = new EntityTypeExportRequest();
        exportRequest.setExportSettings(new EntityExportSettings());
        exportRequest.setPageSize(10);
        exportRequest.setEntityType(entity.getId().getEntityType());

        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);
        assertThat(exportDataList).size().isNotZero();
        assertThat(exportDataList).anySatisfy(exportData -> {
            assertThat(exportData.getEntity()).isEqualTo(entity);
        });
    }

    private void testCustomEntityFilterExportRequest(ExportableEntity<?> entity) throws Exception {
        CustomEntityFilterExportRequest exportRequest = new CustomEntityFilterExportRequest();
        exportRequest.setExportSettings(new EntityExportSettings());
        exportRequest.setPageSize(10);

        EntityListFilter filter = new EntityListFilter();
        filter.setEntityType(entity.getId().getEntityType());
        filter.setEntityList(List.of(entity.getId().toString()));
        exportRequest.setFilter(filter);

        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);
        assertThat(exportDataList).hasOnlyOneElementSatisfying(exportData -> {
            assertThat(exportData.getEntity()).isEqualTo(entity);
        });
    }


    @Test
    public void testExportImportCustomerEntities_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantId1, null, "Customer 1");

        Device tenantDevice = createDevice(tenantId1, null, null, null, "Tenant device 1");
        Device customerDevice = createDevice(tenantId1, customer.getId(), null, null, "Customer device 1");
        Asset tenantAsset = createAsset(tenantId1, null, null, "A", "Tenant asset 1");
        Asset customerAsset = createAsset(tenantId1, customer.getId(), null, "A", "Customer asset 1");

        List<ExportRequest> exportRequests = new ArrayList<>();

        for (EntityType entityType : Set.of(EntityType.DEVICE, EntityType.ASSET)) {
            EntityTypeExportRequest exportRequest = new EntityTypeExportRequest();
            exportRequest.setExportSettings(new EntityExportSettings());
            exportRequest.setPageSize(10);
            exportRequest.setEntityType(entityType);
            exportRequest.setCustomerId(customer.getUuidId());
            exportRequests.add(exportRequest);
        }

        List<EntityExportData<?>> exportDataList = exportEntities(exportRequests);
        assertThat(exportDataList).size().isEqualTo(2);
        assertThat(exportDataList).anySatisfy(exportData -> {
            assertThat(exportData.getEntity()).isEqualTo(customerDevice);
        });
        assertThat(exportDataList).anySatisfy(exportData -> {
            assertThat(exportData.getEntity()).isEqualTo(customerAsset);
        });
    }


    @Test
    public void testEntityEventsOnImport() throws Exception {
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        RuleChain importedRuleChain = (RuleChain) exportAndImport(ruleChain.getId()).get(0).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedRuleChain.getId()), eq(importedRuleChain),
                any(), eq(ActionType.ADDED), isNull());
        verify(clusterService).broadcastEntityStateChangeEvent(any(), eq(importedRuleChain.getId()), eq(ComponentLifecycleEvent.CREATED));

        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), null, "Device profile 1");
        DeviceProfile importedDeviceProfile = (DeviceProfile) exportAndImport(deviceProfile.getId()).get(0).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDeviceProfile.getId()), eq(importedDeviceProfile),
                any(), eq(ActionType.ADDED), isNull());
        verify(clusterService).onDeviceProfileChange(eq(importedDeviceProfile), any());
        verify(clusterService).broadcastEntityStateChangeEvent(any(), eq(importedDeviceProfile.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(clusterService).sendNotificationMsgToEdgeService(any(), any(), eq(importedDeviceProfile.getId()), any(), any(), eq(EdgeEventActionType.ADDED), any(), any());
        verify(otaPackageStateService).update(eq(importedDeviceProfile), eq(false), eq(false));

        Converter converter = createConverter(tenantId1, ConverterType.DOWNLINK, "Converter 1");
        Converter importedConverter = (Converter) exportAndImport(converter.getId()).get(0).getSavedEntity();
        verify(clusterService).broadcastEntityStateChangeEvent(any(), eq(importedConverter.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(entityActionService).logEntityAction(any(), eq(importedConverter.getId()), eq(importedConverter), any(), eq(ActionType.ADDED), isNull());

        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");
        Integration importedIntegration = (Integration) exportAndImport(integration.getId()).get(0).getSavedEntity();
        verify(clusterService).broadcastEntityStateChangeEvent(any(), eq(importedIntegration.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(entityActionService).logEntityAction(any(), eq(importedIntegration.getId()), eq(importedIntegration), any(), eq(ActionType.ADDED), isNull());

        Role role = createGenericRole(tenantId1, null, "Role 1", Map.of(Resource.DEVICE, List.of(Operation.READ)));
        Role importedRole = (Role) exportAndImport(role.getId()).get(0).getSavedEntity();
        verify(userPermissionsService).onRoleUpdated(argThat(r -> r.getId().equals(importedRole.getId())));
        verify(entityActionService).logEntityAction(any(), eq(importedRole.getId()), notNull(), any(), eq(ActionType.ADDED), isNull());
        verify(clusterService).sendNotificationMsgToEdgeService(any(), any(), eq(importedRole.getId()),
                any(), any(), eq(EdgeEventActionType.ADDED), any(), any());

        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        createGroupPermission(tenantId1, userGroup.getId(), role.getId());
        EntityGroup importedUserGroup = (EntityGroup) exportAndImport(List.of(userGroup.getId()),
                EntityExportSettings.builder().exportUserGroupPermissions(true).build(),
                EntityImportSettings.builder().updateUserGroupPermissions(true).build()).get(0).getSavedEntity();
        GroupPermission importedGroupPermission = groupPermissionService.findGroupPermissionByTenantIdAndUserGroupIdAndRoleId(tenantId2,
                importedUserGroup.getId(), importedRole.getId(), new PageLink(200)).getData().get(0);
        verify(userPermissionsService).onGroupPermissionUpdated(eq(importedGroupPermission));
        verify(entityActionService).logEntityAction(any(), eq(importedGroupPermission.getId()), notNull(), any(), eq(ActionType.ADDED), isNull());
        verify(clusterService).sendNotificationMsgToEdgeService(any(), any(), eq(importedGroupPermission.getId()),
                any(), any(), eq(EdgeEventActionType.ADDED), any(), any());

        EntityGroup customerGroup = createEntityGroup(tenantId1, EntityType.CUSTOMER, "Customer group");
        Customer customer = createCustomer(tenantId1, customerGroup.getId(), "Customer 1");

        EntityGroup assetGroup = createEntityGroup(tenantId1, EntityType.ASSET, "Asset group 1");
        Asset asset = createAsset(tenantId1, null, assetGroup.getId(), "A", "Asset 1");

        EntityGroup dashboardGroup = createEntityGroup(tenantId1, EntityType.DASHBOARD, "Dashboard group 1");
        Dashboard dashboard = createDashboard(tenantId1, null, dashboardGroup.getId(), "Dashboard 1");

        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "Device group 1");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), deviceGroup.getId(), "Device 1");

        Map<ExportableEntity<?>, EntityGroup> groupEntities = new IdentityHashMap<>();
        groupEntities.put(customer, customerGroup);
        groupEntities.put(asset, assetGroup);
        groupEntities.put(dashboard, dashboardGroup);
        groupEntities.put(device, deviceGroup);

        for (Map.Entry<ExportableEntity<?>, EntityGroup> entry : groupEntities.entrySet()) {
            ExportableEntity<?> groupEntity = entry.getKey();
            EntityGroup entityGroup = entry.getValue();
            EntityType entityType = groupEntity.getId().getEntityType();

            List<EntityImportResult<?>> importResults = exportAndImport(List.of(entityGroup.getId(), groupEntity.getId()),
                    EntityExportSettings.builder().exportEntityGroupsInfo(true).build(),
                    EntityImportSettings.builder().updateEntityGroups(true).build());

            ExportableEntity<?> importedGroupEntity = findImportedEntity(importResults, entityType);
            EntityGroup importedEntityGroup = findImportedEntity(importResults, EntityType.ENTITY_GROUP);

            testEntityEvents(importedGroupEntity, true);
            testEntityEvents(importedEntityGroup, true);
            verify(entityActionService).logEntityAction(any(), eq(importedGroupEntity.getId()), notNull(),
                    any(), eq(ActionType.ADDED_TO_ENTITY_GROUP), isNull(),
                    eq(importedGroupEntity.getId().toString()), eq(importedEntityGroup.getId().toString()), eq(importedEntityGroup.getName()));
            verify(clusterService).sendNotificationMsgToEdgeService(any(), any(), eq(importedGroupEntity.getId()),
                    any(), any(), eq(EdgeEventActionType.ADDED_TO_ENTITY_GROUP), eq(entityType), eq(importedEntityGroup.getId()));
            if (entityType == EntityType.DEVICE) {
                verify(clusterService).onDeviceUpdated(eq((Device) importedGroupEntity), isNull());
            }

            exportAndImport(groupEntity.getId());
            testEntityEvents(importedGroupEntity, false);
        }

    }

    private void testEntityEvents(ExportableEntity<?> entity, boolean newEntity) {
        if (newEntity) {
            verify(entityActionService).logEntityAction(any(), eq(entity.getId()), eq(entity),
                    any(), eq(ActionType.ADDED), isNull());
            verify(clusterService, never()).sendNotificationMsgToEdgeService(any(), any(), eq(entity.getId()),
                    any(), any(), eq(EdgeEventActionType.ADDED), any(), any());
        } else {
            verify(entityActionService).logEntityAction(any(), eq(entity.getId()), eq(entity),
                    any(), eq(ActionType.UPDATED), isNull());
            verify(clusterService, atLeastOnce()).sendNotificationMsgToEdgeService(any(), any(), eq(entity.getId()),
                    any(), any(), eq(EdgeEventActionType.UPDATED), any(), any());
        }
    }

    @Test
    public void testExportImportGroupWithPermissions_betweenTenants() throws Exception {
        logInAsTenantAdmin1();

        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(tenantId1, null, "Role for User group 1", List.of(Operation.READ));
        createGroupPermission(tenantId1, userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);

        EntityExportData<Role> roleExportData = exportSingleEntity(role.getId());

        SingleEntityExportRequest groupExportRequest = new SingleEntityExportRequest();
        groupExportRequest.setExportSettings(EntityExportSettings.builder()
                .exportUserGroupPermissions(true)
                .build());
        groupExportRequest.setEntityId(deviceGroup.getId());
        EntityGroupExportData deviceGroupExportData = (EntityGroupExportData) exportEntities(groupExportRequest).get(0);

        groupExportRequest.setEntityId(userGroup.getId());
        EntityGroupExportData userGroupExportData = (EntityGroupExportData) exportEntities(groupExportRequest).get(0);

        logInAsTenantAdmin2();

        Role importedRole = (Role) importEntities(List.of(roleExportData)).get(0).getSavedEntity();
        checkImportedEntity(tenantId1, role, tenantId2, importedRole);
        assertThat(importedRole.getName()).isEqualTo(role.getName());
        assertThat(importedRole.getPermissions()).isEqualTo(role.getPermissions());

        ImportRequest groupImportRequest = new ImportRequest();
        groupImportRequest.setImportSettings(EntityImportSettings.builder()
                .updateUserGroupPermissions(true)
                .build());
        groupImportRequest.setExportDataList(List.of(deviceGroupExportData));
        EntityGroup importedDeviceGroup = (EntityGroup) importEntities(groupImportRequest).get(0).getSavedEntity();
        checkImportedEntity(tenantId1, tenantId1, deviceGroup, tenantId2, tenantId2, importedDeviceGroup);

        groupImportRequest.setExportDataList(List.of(userGroupExportData));
        EntityGroup importedUserGroup = (EntityGroup) importEntities(groupImportRequest).get(0).getSavedEntity();
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
        logInAsTenantAdmin1();

        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(tenantId1, null, "Role for User group 1", List.of(Operation.READ));
        GroupPermission groupPermission = createGroupPermission(tenantId1, userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);

        SingleEntityExportRequest groupExportRequest = new SingleEntityExportRequest();
        groupExportRequest.setExportSettings(EntityExportSettings.builder()
                .exportUserGroupPermissions(true)
                .build());
        groupExportRequest.setEntityId(userGroup.getId());
        EntityGroupExportData userGroupExportData = (EntityGroupExportData) exportEntities(groupExportRequest).get(0);

        ImportRequest groupImportRequest = new ImportRequest();
        groupImportRequest.setImportSettings(EntityImportSettings.builder()
                .updateUserGroupPermissions(true)
                .build());
        groupImportRequest.setExportDataList(List.of(userGroupExportData));
        EntityGroup importedUserGroup = (EntityGroup) importEntities(groupImportRequest).get(0).getSavedEntity();
        checkImportedEntity(tenantId1, tenantId1, userGroup, tenantId1, tenantId1, importedUserGroup);

        List<GroupPermission> groupPermissions = groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(tenantId1, userGroup.getId());
        assertThat(groupPermissions).hasOnlyOneElementSatisfying(permission -> {
            assertThat(permission).isEqualTo(groupPermission);
        });
    }

    @Test
    public void testExportImportGroupWithPermissions_betweenTenants_permissionsUpdated() throws Exception {
        logInAsTenantAdmin1();

        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(tenantId1, null, "Role for User group 1", List.of(Operation.READ));

        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        GroupPermission groupPermission = createGroupPermission(tenantId1, userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);

        EntityListExportRequest exportRequest = new EntityListExportRequest();
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportUserGroupPermissions(true)
                .build());
        exportRequest.setEntitiesIds(List.of(deviceGroup.getId(), role.getId(), userGroup.getId()));
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        logInAsTenantAdmin2();

        ImportRequest importRequest = new ImportRequest();
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateUserGroupPermissions(true)
                .build());
        importRequest.setExportDataList(exportDataList);

        Map<EntityType, ExportableEntity<?>> importResults = importEntities(importRequest).stream()
                .map(EntityImportResult::getSavedEntity)
                .collect(Collectors.toMap(entity -> entity instanceof EntityGroup ?
                        ((EntityGroup) entity).getType() : entity.getId().getEntityType(), r -> r));

        Role importedRole = (Role) importResults.get(EntityType.ROLE);
        EntityGroup importedDeviceGroup = (EntityGroup) importResults.get(EntityType.DEVICE);
        EntityGroup importedUserGroup = (EntityGroup) importResults.get(EntityType.USER);

        List<GroupPermission> importedGroupPermissions = groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(tenantId2, importedUserGroup.getId());
        assertThat(importedGroupPermissions).size().isOne();
        assertThat(importedGroupPermissions).satisfies(importedGroupPermission -> {
            assertThat(importedGroupPermission.getRoleId()).isEqualTo(importedRole.getId());
            assertThat(importedGroupPermission.getEntityGroupId()).isEqualTo(importedDeviceGroup.getId());
            assertThat(importedGroupPermission.getEntityGroupType()).isEqualTo(EntityType.DEVICE);
        }, Index.atIndex(0));

        logInAsTenantAdmin1();

        groupPermissionService.deleteGroupPermissionsByTenantIdAndUserGroupId(tenantId1, userGroup.getId());
        role = createGenericRole(tenantId1, null, "Read devices", Map.of(
                Resource.DEVICE, List.of(Operation.READ),
                Resource.DEVICE_GROUP, List.of(Operation.READ)
        ));
        groupPermission = createGroupPermission(tenantId1, userGroup.getId(), role.getId());

        exportRequest.setEntitiesIds(List.of(role.getId(), userGroup.getId()));
        exportDataList = exportEntities(exportRequest);

        logInAsTenantAdmin2();

        importRequest.setExportDataList(exportDataList);
        importResults = importEntities(importRequest).stream()
                .collect(Collectors.toMap(EntityImportResult::getEntityType, EntityImportResult::getSavedEntity));

        Role newImportedRole = (Role) importResults.get(EntityType.ROLE);
        List<GroupPermission> updatedGroupPermissions = groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(tenantId2, importedUserGroup.getId());

        assertThat(updatedGroupPermissions).size().isOne();
        assertThat(updatedGroupPermissions).hasOnlyOneElementSatisfying(newGroupPermission -> {
            assertThat(newGroupPermission.getEntityGroupId()).matches(entityGroupId -> entityGroupId == null || entityGroupId.isNullUid());
            assertThat(newGroupPermission.getRoleId()).isEqualTo(newImportedRole.getId());
        });
    }

    @Test
    public void testExportImportCustomerHierarchy_betweenTenants() throws Exception {
        logInAsTenantAdmin1();

        EntityGroup customerGroup = createEntityGroup(tenantId1, EntityType.CUSTOMER, "[Tenant] My customers");

        Customer customer = createCustomer(tenantId1, customerGroup.getId(), "Customer");
        EntityGroup customerAssetGroup = createEntityGroup(customer.getId(), EntityType.ASSET, "[Customer] My assets");
        Asset customerAsset = createAsset(tenantId1, customer.getId(), customerAssetGroup.getId(), "A", "[Customer] Asset");
        EntityGroup customerCustomerGroup = createEntityGroup(customer.getId(), EntityType.CUSTOMER, "[Customer] My customers");

        Customer subCustomer = createCustomer(tenantId1, customerCustomerGroup.getId(), "Sub Customer");
        EntityGroup subCustomerDashboardGroup = createEntityGroup(subCustomer.getId(), EntityType.DASHBOARD, "[Sub Customer] My dashboards");
        Dashboard subCustomerDashboard = createDashboard(tenantId1, subCustomer.getId(), subCustomerDashboardGroup.getId(), "[Sub Customer] Dashboard");
        EntityGroup subCustomerCustomerGroup = createEntityGroup(subCustomer.getId(), EntityType.CUSTOMER, "[Sub Customer] My customers");

        Customer subSubCustomer = createCustomer(tenantId1, subCustomerCustomerGroup.getId(), "Sub Sub Customer");
        EntityGroup subSubCustomerUserGroup = createEntityGroup(subSubCustomer.getId(), EntityType.USER, "[Sub Sub Customer] My users");

        Role subCustomerReadGroupRole = createGroupRole(tenantId1, subCustomer.getId(), "Read group", List.of(Operation.READ));
        createGroupPermission(tenantId1, subSubCustomerUserGroup.getId(),
                subCustomerReadGroupRole.getId(), subCustomerDashboardGroup.getId(), EntityType.DASHBOARD);

        List<ExportRequest> exportRequests = new ArrayList<>();
        for (EntityType entityType : Set.of(EntityType.CUSTOMER, EntityType.ASSET, EntityType.DASHBOARD, EntityType.ROLE)) {
            EntityTypeExportRequest entityTypeExportRequest = new EntityTypeExportRequest();
            entityTypeExportRequest.setEntityType(entityType);
            entityTypeExportRequest.setPageSize(100);
            entityTypeExportRequest.setExportSettings(EntityExportSettings.builder()
                    .exportEntityGroupsInfo(true)
                    .build());
            exportRequests.add(entityTypeExportRequest);
        }
        for (EntityType groupType : Set.of(EntityType.CUSTOMER, EntityType.ASSET, EntityType.DASHBOARD, EntityType.USER)) {
            CustomEntityFilterExportRequest groupsExportRequest = new CustomEntityFilterExportRequest();
            groupsExportRequest.setExportSettings(EntityExportSettings.builder()
                    .exportUserGroupPermissions(true)
                    .build());
            groupsExportRequest.setPageSize(100);
            EntityGroupNameFilter entityFilter = new EntityGroupNameFilter();
            entityFilter.setGroupType(groupType);
            entityFilter.setEntityGroupNameFilter("");
            groupsExportRequest.setFilter(entityFilter);
            exportRequests.add(groupsExportRequest);
        }

        List<EntityExportData<?>> exportDataList = exportEntities(exportRequests);

        logInAsTenantAdmin2();

        ImportRequest importRequest = new ImportRequest();
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateEntityGroups(true)
                .updateUserGroupPermissions(true)
                .findExistingByName(true)
                .build());
        importRequest.setExportDataList(exportDataList);

        List<EntityImportResult<?>> importResults = importEntities(importRequest);

        EntityGroup importedCustomerGroup = findImportedEntity(importResults, EntityType.ENTITY_GROUP, entity -> {
            return entity.getType() == EntityType.CUSTOMER && entity.getName().equals(customerGroup.getName());
        });
        checkImportedEntity(tenantId1, tenantId1, customerGroup,
                tenantId2, tenantId2, importedCustomerGroup);
        Customer importedCustomer = findImportedEntity(importResults, EntityType.CUSTOMER, entity -> {
            return entity.getTitle().equals(customer.getTitle());
        });
        checkImportedEntity(tenantId1, tenantId1, customer,
                tenantId2, tenantId2, importedCustomer);
        assertThat(entityGroupService.isEntityInGroup(importedCustomer.getId(), importedCustomerGroup.getId())).isTrue();

        EntityGroup importedCustomerAssetGroup = findImportedEntity(importResults, EntityType.ENTITY_GROUP, entity -> {
            return entity.getType() == EntityType.ASSET && entity.getName().equals(customerAssetGroup.getName());
        });
        checkImportedEntity(tenantId1, customer.getId(), customerAssetGroup,
                tenantId2, importedCustomer.getId(), importedCustomerAssetGroup);
        Asset importedCustomerAsset = findImportedEntity(importResults, EntityType.ASSET, entity -> {
            return entity.getName().equals(customerAsset.getName());
        });
        checkImportedEntity(tenantId1, customer.getId(), customerAsset,
                tenantId2, importedCustomer.getId(), importedCustomerAsset);
        assertThat(entityGroupService.isEntityInGroup(importedCustomerAsset.getId(), importedCustomerAssetGroup.getId())).isTrue();

        EntityGroup importedCustomerCustomerGroup = findImportedEntity(importResults, EntityType.ENTITY_GROUP, entity -> {
            return entity.getType() == EntityType.CUSTOMER && entity.getName().equals(customerCustomerGroup.getName());
        });
        checkImportedEntity(tenantId1, customer.getId(), customerCustomerGroup,
                tenantId2, importedCustomer.getId(), importedCustomerCustomerGroup);

        Customer importedSubCustomer = findImportedEntity(importResults, EntityType.CUSTOMER, entity -> {
            return entity.getName().equals(subCustomer.getName());
        });
        checkImportedEntity(tenantId1, customer.getId(), subCustomer,
                tenantId2, importedCustomer.getId(), importedSubCustomer);
        assertThat(entityGroupService.isEntityInGroup(importedSubCustomer.getId(), importedCustomerCustomerGroup.getId())).isTrue();

        EntityGroup importedSubCustomerDashboardGroup = findImportedEntity(importResults, EntityType.ENTITY_GROUP, entity -> {
            return entity.getType() == EntityType.DASHBOARD && entity.getName().equals(subCustomerDashboardGroup.getName());
        });
        checkImportedEntity(tenantId1, subCustomer.getId(), subCustomerDashboardGroup,
                tenantId2, importedSubCustomer.getId(), importedSubCustomerDashboardGroup);
        Dashboard importedSubCustomerDashboard = findImportedEntity(importResults, EntityType.DASHBOARD, entity -> {
            return entity.getName().equals(subCustomerDashboard.getName());
        });
        checkImportedEntity(tenantId1, subCustomer.getId(), subCustomerDashboard,
                tenantId2, importedSubCustomer.getId(), importedSubCustomerDashboard);
        assertThat(entityGroupService.isEntityInGroup(importedSubCustomerDashboard.getId(), importedSubCustomerDashboardGroup.getId())).isTrue();

        EntityGroup importedSubCustomerCustomerGroup = findImportedEntity(importResults, EntityType.ENTITY_GROUP, entity -> {
            return entity.getType() == EntityType.CUSTOMER && entity.getName().equals(subCustomerCustomerGroup.getName());
        });
        checkImportedEntity(tenantId1, subCustomer.getId(), subCustomerCustomerGroup,
                tenantId2, importedSubCustomer.getId(), importedSubCustomerCustomerGroup);

        Customer importedSubSubCustomer = findImportedEntity(importResults, EntityType.CUSTOMER, entity -> {
            return entity.getName().equals(subSubCustomer.getName());
        });
        checkImportedEntity(tenantId1, subCustomer.getId(), subSubCustomer,
                tenantId2, importedSubCustomer.getId(), importedSubSubCustomer);
        assertThat(entityGroupService.isEntityInGroup(importedSubSubCustomer.getId(), importedSubCustomerCustomerGroup.getId())).isTrue();

        EntityGroup importedSubSubCustomerUserGroup = findImportedEntity(importResults, EntityType.ENTITY_GROUP, entity -> {
            return entity.getType() == EntityType.USER && entity.getName().equals(subSubCustomerUserGroup.getName());
        });
        checkImportedEntity(tenantId1, subSubCustomer.getId(), subSubCustomerUserGroup,
                tenantId2, importedSubSubCustomer.getId(), importedSubSubCustomerUserGroup);

        Role importedSubCustomerReadGroupRole = findImportedEntity(importResults, EntityType.ROLE, entity -> {
            return entity.getName().equals(subCustomerReadGroupRole.getName());
        });
        checkImportedEntity(tenantId1, subCustomer.getId(), subCustomerReadGroupRole,
                tenantId2, importedSubCustomer.getId(), importedSubCustomerReadGroupRole);

        List<GroupPermission> importedSubSubCustomerUserGroupPermissions =
                groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(tenantId2, importedSubSubCustomerUserGroup.getId());
        assertThat(importedSubSubCustomerUserGroupPermissions).hasOnlyOneElementSatisfying(importedGroupPermission -> {
            assertThat(importedGroupPermission.getRoleId()).isEqualTo(importedSubCustomerReadGroupRole.getId());
            assertThat(importedGroupPermission.getEntityGroupId()).isEqualTo(importedSubCustomerDashboardGroup.getId());
        });
    }

    @Test
    public void testExportByCustomerId() throws Exception {
        logInAsTenantAdmin1();

        EntityGroup customerGroup = createEntityGroup(tenantId1, EntityType.CUSTOMER, "[Tenant] My customers");
        Customer customer = createCustomer(tenantId1, customerGroup.getId(), "Customer");
        EntityGroup customerCustomerGroup = createEntityGroup(customer.getId(), EntityType.CUSTOMER, "[Customer] My customers");

        Customer subCustomer = createCustomer(tenantId1, customerCustomerGroup.getId(), "Sub Customer");
        EntityGroup subCustomerDashboardGroup = createEntityGroup(subCustomer.getId(), EntityType.DASHBOARD, "[Sub Customer] My dashboards");
        Dashboard subCustomerDashboard = createDashboard(tenantId1, subCustomer.getId(), subCustomerDashboardGroup.getId(), "[Sub Customer] Dashboard");
        EntityGroup subCustomerCustomerGroup = createEntityGroup(subCustomer.getId(), EntityType.CUSTOMER, "[Sub Customer] My customers");

        Customer subSubCustomer = createCustomer(tenantId1, subCustomerCustomerGroup.getId(), "Sub Sub Customer");
        EntityGroup subSubCustomerUserGroup = createEntityGroup(subSubCustomer.getId(), EntityType.USER, "[Sub Sub Customer] My users");

        Role subCustomerReadGroupRole = createGroupRole(tenantId1, subCustomer.getId(), "Read group", List.of(Operation.READ));
        createGroupPermission(tenantId1, subSubCustomerUserGroup.getId(),
                subCustomerReadGroupRole.getId(), subCustomerDashboardGroup.getId(), EntityType.DASHBOARD);

        CustomerId customerId = subCustomer.getId();

        List<ExportRequest> exportRequests = new ArrayList<>();
        for (EntityType entityType : Set.of(EntityType.CUSTOMER, EntityType.DASHBOARD, EntityType.ROLE)) {
            EntityTypeExportRequest exportRequest = new EntityTypeExportRequest();
            exportRequest.setExportSettings(new EntityExportSettings());
            exportRequest.setCustomerId(customerId.getId());
            exportRequest.setEntityType(entityType);
            exportRequest.setPageSize(200);
            exportRequests.add(exportRequest);
        }
        for (EntityType groupType : Set.of(EntityType.CUSTOMER, EntityType.DASHBOARD, EntityType.USER)) {
            CustomEntityFilterExportRequest exportRequest = new CustomEntityFilterExportRequest();
            exportRequest.setExportSettings(new EntityExportSettings());
            exportRequest.setCustomerId(customerId.getId());
            exportRequest.setPageSize(200);

            EntityGroupNameFilter filter = new EntityGroupNameFilter();
            filter.setGroupType(groupType);
            filter.setEntityGroupNameFilter("");
            exportRequest.setFilter(filter);

            exportRequests.add(exportRequest);
        }

        List<EntityExportData<?>> exportDataList = exportEntities(exportRequests);

        assertThat(exportDataList).allSatisfy(exportData -> {
            Set<EntityId> owners = ownersCacheService.getOwners(tenantId1, exportData.getEntity().getId(), (HasOwnerId) exportData.getEntity());
            if (!exportData.getEntity().getId().equals(customerId)) {
                assertThat(customerId).isIn(owners);
            }
        });

        Set<EntityId> exportedEntities = exportDataList.stream()
                .map(exportData -> exportData.getEntity().getId())
                .collect(Collectors.toSet());

        assertThat(exportedEntities).doesNotContain(customer.getId(), customerCustomerGroup.getId(), customerGroup.getId());
        assertThat(exportedEntities).contains(subCustomer.getId(), subCustomerDashboardGroup.getId(),
                subCustomerDashboard.getId(), subCustomerCustomerGroup.getId(), subSubCustomer.getId(),
                subSubCustomerUserGroup.getId(), subCustomerReadGroupRole.getId());
    }

    @Test
    public void testPermissionChecksOnExportImport() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile 1");
        exportImportAndAssertPermissionChecks(deviceProfile);

        Device device = createDevice(tenantId1, null, deviceProfile.getId(), null, "Device 1");
        Device importedDevice = exportImportAndAssertPermissionChecks(device);

        EntityGroup assetGroup = createEntityGroup(tenantId1, EntityType.ASSET, "Asset group 1");
        logInAsTenantAdmin1();
        EntityExportData exportData = exportSingleEntity(assetGroup.getId());
        verify(accessControlService).checkEntityGroupPermission(getIdMatcher(tenantAdmin1),
                eq(Operation.READ), getEntityMatcher(assetGroup, false));

        logInAsTenantAdmin2();
        EntityGroup importedAssetGroup = (EntityGroup) importEntity(exportData).getSavedEntity();
        verify(accessControlService).checkEntityGroupPermission(getIdMatcher(tenantAdmin2),
                eq(Operation.CREATE), getEntityMatcher(importedAssetGroup, true));
        importEntity(exportData);
        verify(accessControlService).checkEntityGroupPermission(getIdMatcher(tenantAdmin2),
                eq(Operation.WRITE), getEntityMatcher(importedAssetGroup, false));

        Asset asset = createAsset(tenantId1, null, assetGroup.getId(), "A", "Asset 1");
        logInAsTenantAdmin1();
        exportData = exportEntities(List.of(asset.getId()), EntityExportSettings.builder()
                .exportEntityGroupsInfo(true)
                .build()).get(0);
        assertPermissionCheck(tenantAdmin1, Operation.READ, asset, false);

        logInAsTenantAdmin2();
        Asset importedAsset = (Asset) ((List<EntityImportResult<?>>) importEntities(List.of(exportData), EntityImportSettings.builder()
                .updateEntityGroups(true)
                .build())).get(0).getSavedEntity();
        assertPermissionCheck(tenantAdmin2, Operation.CREATE, importedAsset, true);
        verify(accessControlService).checkEntityGroupPermission(getIdMatcher(tenantAdmin2),
                eq(Operation.ADD_TO_GROUP), getEntityMatcher(importedAssetGroup, false));

        logInAsTenantAdmin1();
        entityGroupService.removeEntityFromEntityGroup(tenantId1, assetGroup.getId(), asset.getId());
        exportData = exportEntities(List.of(asset.getId()), EntityExportSettings.builder()
                .exportEntityGroupsInfo(true)
                .build()).get(0);
        logInAsTenantAdmin2();
        importedAsset = (Asset) ((List<EntityImportResult<?>>) importEntities(List.of(exportData), EntityImportSettings.builder()
                .updateEntityGroups(true)
                .build())).get(0).getSavedEntity();
        assertPermissionCheck(tenantAdmin2, Operation.WRITE, importedAsset, false);
        verify(accessControlService).checkEntityGroupPermission(getIdMatcher(tenantAdmin2),
                eq(Operation.REMOVE_FROM_GROUP), getEntityMatcher(importedAssetGroup, false));

        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        exportImportAndAssertPermissionChecks(ruleChain);

        Customer customer = createCustomer(tenantId1, null, "Customer 1");
        exportImportAndAssertPermissionChecks(customer);

        Dashboard dashboard = createDashboard(tenantId1, null, null, "Dashboard 1");
        exportImportAndAssertPermissionChecks(dashboard);

        Converter converter = createConverter(tenantId1, ConverterType.DOWNLINK, "Converter 1");
        exportImportAndAssertPermissionChecks(converter);

        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");
        exportImportAndAssertPermissionChecks(integration);

        Role role = createGroupRole(tenantId1, null, "Role 1", List.of(Operation.READ));
        Role importedRole = exportImportAndAssertPermissionChecks(role);

        EntityGroup userGroup = createEntityGroup(customer.getId(), EntityType.USER, "Customer user group 1");
        GroupPermission groupPermission = createGroupPermission(tenantId1, userGroup.getId(), role.getId(), assetGroup.getId(), EntityType.ASSET);
        logInAsTenantAdmin1();
        exportData = exportEntities(List.of(userGroup.getId()), EntityExportSettings.builder()
                .exportUserGroupPermissions(true)
                .build()).get(0);
        verify(accessControlService).checkPermission(getIdMatcher(tenantAdmin1), eq(Resource.GROUP_PERMISSION), eq(Operation.READ));

        logInAsTenantAdmin2();
        EntityGroup importedUserGroup = (EntityGroup) ((EntityImportResult<?>) importEntities(List.of(exportData), EntityImportSettings.builder()
                .updateUserGroupPermissions(true)
                .build()).get(0)).getSavedEntity();
        verify(accessControlService).checkPermission(getIdMatcher(tenantAdmin2), eq(Resource.GROUP_PERMISSION), eq(Operation.CREATE),
                isNull(), argThat(entity -> ((GroupPermission) entity).getUserGroupId().equals(importedUserGroup.getId())));
        assertPermissionCheck(tenantAdmin2, Operation.READ, importedRole, false);
        verify(accessControlService).checkEntityGroupPermission(getIdMatcher(tenantAdmin2), eq(Operation.WRITE),
                getEntityMatcher(importedUserGroup, false));
        verify(accessControlService, atLeastOnce()).checkEntityGroupPermission(getIdMatcher(tenantAdmin2), eq(Operation.WRITE),
                getEntityMatcher(importedAssetGroup, false));

        logInAsTenantAdmin1();
        groupPermissionService.deleteGroupPermission(tenantId1, groupPermission.getId());
        exportData = exportEntities(List.of(userGroup.getId()), EntityExportSettings.builder()
                .exportUserGroupPermissions(true)
                .build()).get(0);
        logInAsTenantAdmin2();
        importEntities(List.of(exportData), EntityImportSettings.builder()
                .updateUserGroupPermissions(true)
                .build());
        assertPermissionCheck(tenantAdmin2, Operation.READ, importedRole, false);
        verify(accessControlService, atLeastOnce()).checkEntityGroupPermission(getIdMatcher(tenantAdmin2), eq(Operation.WRITE),
                getEntityMatcher(importedUserGroup, false));
        verify(accessControlService, atLeastOnce()).checkEntityGroupPermission(getIdMatcher(tenantAdmin2), eq(Operation.WRITE),
                getEntityMatcher(importedAssetGroup, false));
        verify(accessControlService).checkPermission(getIdMatcher(tenantAdmin2), eq(Resource.GROUP_PERMISSION),
                eq(Operation.DELETE), notNull(), argThat(entity -> ((GroupPermission) entity).getUserGroupId().equals(importedUserGroup.getId())));

        createRelation(asset.getId(), device.getId());
        logInAsTenantAdmin1();
        exportData = exportEntities(List.of(asset.getId()), EntityExportSettings.builder()
                .exportRelations(true)
                .build()).get(0);
        assertPermissionCheck(tenantAdmin1, Operation.READ, device, false);
        logInAsTenantAdmin2();
        importEntities(List.of(exportData), EntityImportSettings.builder()
                .updateRelations(true)
                .build());
        assertPermissionCheck(tenantAdmin2, Operation.WRITE, importedAsset, false);
        assertPermissionCheck(tenantAdmin2, Operation.WRITE, importedDevice, false);

        logInAsTenantAdmin1();
        relationService.deleteEntityRelations(tenantId1, asset.getId());
        exportData = exportEntities(List.of(asset.getId()), EntityExportSettings.builder()
                .exportRelations(true)
                .build()).get(0);
        logInAsTenantAdmin2();
        importEntities(List.of(exportData), EntityImportSettings.builder()
                .updateRelations(true)
                .build());
        assertPermissionCheck(tenantAdmin2, Operation.WRITE, importedAsset, false);
        assertPermissionCheck(tenantAdmin2, Operation.WRITE, importedDevice, false);
    }

    private <E extends ExportableEntity<?>> E exportImportAndAssertPermissionChecks(E entity) throws Exception {
        logInAsTenantAdmin1();
        EntityExportData exportData = exportSingleEntity(entity.getId());
        resetUniqueProperties(exportData);
        assertPermissionCheck(tenantAdmin1, Operation.READ, entity, false);

        logInAsTenantAdmin2();
        ExportableEntity<?> importedEntity = importEntity(exportData).getSavedEntity();
        assertPermissionCheck(tenantAdmin2, Operation.CREATE, importedEntity, true);
        importEntity(exportData);
        assertPermissionCheck(tenantAdmin2, Operation.WRITE, importedEntity, false);

        return (E) importedEntity;
    }


    private void assertPermissionCheck(User user, Operation operation, ExportableEntity<?> entity, boolean newEntity) throws ThingsboardException {
        verify(accessControlService, atLeastOnce()).checkPermission(getIdMatcher(user),
                eq(Resource.resourceFromEntityType(entity.getId().getEntityType())), eq(operation),
                newEntity ? isNull() : eq(entity.getId()), getEntityMatcher(entity, newEntity));
    }

    private <E extends HasId> E getIdMatcher(HasId expected) {
        return argThat(actual -> actual.getId().equals(expected.getId()));
    }

    private <E extends HasId> E getEntityMatcher(HasId expected, boolean newEntity) {
        return argThat(actual -> (newEntity ? actual.getId() == null : actual.getId().equals(expected.getId()))
                && (expected instanceof HasOwnerId ? ((HasOwnerId) actual).getOwnerId().equals(((HasOwnerId) expected).getOwnerId())
                : ((HasTenantId) actual).getTenantId().equals(((HasTenantId) expected).getTenantId())));
    }

}
