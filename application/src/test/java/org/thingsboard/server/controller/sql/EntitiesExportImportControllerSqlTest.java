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
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.BaseEntitiesExportImportControllerTest;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.sync.exporting.data.DeviceExportData;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

/*
 * - export/import roles
 * - entity group assignment
 * - entity group unassignment
 * - sub entity groups of customers
 * - complex customer hierarchy
 *
 * - test repetitive import for ce
 * */
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
        dashboardService.assignDashboardToCustomer(tenantId1, dashboard.getId(), customer.getId());
        exportData = exportSingleEntity(dashboard.getId());

        logInAsTenantAdmin2();
        Customer importedCustomer = (Customer) importEntities(List.of(customerExportData)).get(0).getSavedEntity();
        importedDashboard = (Dashboard) importEntities(List.of(exportData)).get(0).getSavedEntity();
        assertThat(importedDashboard.getAssignedCustomers()).hasOnlyOneElementSatisfying(customerInfo -> {
            assertThat(customerInfo.getCustomerId()).isEqualTo(importedCustomer.getId());
        });
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
    public void testExportImportBatch_betweenTenants() throws Exception {
        logInAsTenantAdmin1();

        Customer customer = createCustomer(tenantId1, null, "Customer 1");
        Asset asset = createAsset(tenantId1, customer.getId(), null, "A", "Customer 1 - Asset 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Dashboard dashboard = createDashboard(tenantId1, customer.getId(), null, "Customer 1 - Dashboard 1");
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
        assertThat(importedDashboard.getAssignedCustomers()).size().isOne();
        assertThat(importedDashboard.getAssignedCustomers()).hasOnlyOneElementSatisfying(customerInfo -> {
            assertThat(customerInfo.getCustomerId()).isEqualTo(importedCustomer.getId());
        });

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
                .exportInboundRelations(true)
                .exportOutboundRelations(false)
                .build());
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        EntityExportData<?> deviceExportData = exportDataList.stream().filter(exportData -> exportData.getEntityType() == EntityType.DEVICE).findFirst().orElse(null);
        assertThat(deviceExportData.getInboundRelations()).size().isOne();
        assertThat(deviceExportData.getInboundRelations().get(0)).matches(entityRelation -> {
            return entityRelation.getFrom().equals(asset.getId()) && entityRelation.getTo().equals(device.getId());
        });
        ((DeviceExportData) deviceExportData).getCredentials().setCredentialsId("ab");
        ((Device) deviceExportData.getEntity()).setDeviceProfileId(null);

        logInAsTenantAdmin2();

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(exportDataList);
        importRequest.setImportSettings(EntityImportSettings.builder()
                .importInboundRelations(true)
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
                .exportInboundRelations(true)
                .exportOutboundRelations(true)
                .build());
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        assertThat(exportDataList).allMatch(exportData -> exportData.getInboundRelations().size() + exportData.getOutboundRelations().size() == 1);

        EntityExportData<?> deviceExportData = exportDataList.stream().filter(exportData -> exportData.getEntityType() == EntityType.DEVICE).findFirst().orElse(null);
        ((DeviceExportData) deviceExportData).getCredentials().setCredentialsId("ab");
        ((Device) deviceExportData.getEntity()).setDeviceProfileId(null);

        logInAsTenantAdmin2();

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(exportDataList);
        importRequest.setImportSettings(EntityImportSettings.builder()
                .importInboundRelations(true)
                .importOutboundRelations(true)
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
                .exportOutboundRelations(true)
                .build());
        EntityExportData<Asset> assetExportData = (EntityExportData<Asset>) exportEntities(exportRequest).get(0);
        assertThat(assetExportData.getOutboundRelations()).size().isOne();

        Device device2 = createDevice(tenantId1, null, null, null, "Device 2");
        EntityRelation relation2 = createRelation(asset.getId(), device2.getId());

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(List.of(assetExportData));
        importRequest.setImportSettings(EntityImportSettings.builder()
                .importOutboundRelations(true)
                .build());

        importEntities(importRequest);

        List<EntityRelation> relations = relationService.findByFrom(TenantId.SYS_TENANT_ID, asset.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).contains(relation1, relation2);
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
                .exportInboundRelations(true)
                .build());
        EntityExportData<?> deviceExportData = exportEntities(exportRequest).get(0);
        assertThat(deviceExportData.getInboundRelations()).size().isOne();

        Asset asset2 = createAsset(tenantId1, null, null, "A", "Asset 2");
        EntityRelation relation2 = createRelation(asset2.getId(), device.getId());

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(List.of(deviceExportData));
        importRequest.setImportSettings(EntityImportSettings.builder()
                .importInboundRelations(true)
                .removeExistingRelations(true)
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

        Map<EntityType, ExportableEntity<?>> entities = Map.of(
                EntityType.DEVICE, device, EntityType.DEVICE_PROFILE, deviceProfile,
                EntityType.RULE_CHAIN, ruleChain, EntityType.ASSET, asset,
                EntityType.DASHBOARD, dashboard, EntityType.CUSTOMER, customer
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
            exportRequest.setCustomerId(customer.getId());
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
        logInAsTenantAdmin1();

        Customer customer = createCustomer(tenantId1, null, "Customer 1");
        Asset asset = createAsset(tenantId1, null, null, "A", "Asset 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Dashboard dashboard = createDashboard(tenantId1, null, null, "Dashboard 1");
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), null, "Device 1");

        EntityListExportRequest exportRequest = new EntityListExportRequest();
        exportRequest.setEntitiesIds(List.of(customer.getId(), asset.getId(), device.getId(), ruleChain.getId(), dashboard.getId(), deviceProfile.getId()));
        exportRequest.setExportSettings(new EntityExportSettings());

        Map<EntityType, EntityExportData> entitiesExportData = exportEntities(exportRequest).stream()
                .collect(Collectors.toMap(EntityExportData::getEntityType, r -> r));

        logInAsTenantAdmin2();

        Customer importedCustomer = (Customer) importEntity(entitiesExportData.get(EntityType.CUSTOMER)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedCustomer.getId()), eq(importedCustomer),
                any(), eq(ActionType.ADDED), isNull());
        importEntity(entitiesExportData.get(EntityType.CUSTOMER));
        verify(entityActionService).logEntityAction(any(), eq(importedCustomer.getId()), eq(importedCustomer),
                any(), eq(ActionType.UPDATED), isNull());
        verify(clusterService).sendNotificationMsgToEdgeService(any(), any(), eq(importedCustomer.getId()), any(), any(), eq(EdgeEventActionType.UPDATED), any(), any());

        Asset importedAsset = (Asset) importEntity(entitiesExportData.get(EntityType.ASSET)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedAsset.getId()), eq(importedAsset),
                any(), eq(ActionType.ADDED), isNull());
        importEntity(entitiesExportData.get(EntityType.ASSET));
        verify(entityActionService).logEntityAction(any(), eq(importedAsset.getId()), eq(importedAsset),
                any(), eq(ActionType.UPDATED), isNull());
        verify(clusterService).sendNotificationMsgToEdgeService(any(), any(), eq(importedAsset.getId()), any(), any(), eq(EdgeEventActionType.UPDATED), any(), any());

        RuleChain importedRuleChain = (RuleChain) importEntity(entitiesExportData.get(EntityType.RULE_CHAIN)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedRuleChain.getId()), eq(importedRuleChain),
                any(), eq(ActionType.ADDED), isNull());
        verify(clusterService).broadcastEntityStateChangeEvent(any(), eq(importedRuleChain.getId()), eq(ComponentLifecycleEvent.CREATED));

        Dashboard importedDashboard = (Dashboard) importEntity(entitiesExportData.get(EntityType.DASHBOARD)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDashboard.getId()), eq(importedDashboard),
                any(), eq(ActionType.ADDED), isNull());

        DeviceProfile importedDeviceProfile = (DeviceProfile) importEntity(entitiesExportData.get(EntityType.DEVICE_PROFILE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDeviceProfile.getId()), eq(importedDeviceProfile),
                any(), eq(ActionType.ADDED), isNull());
        verify(clusterService).onDeviceProfileChange(eq(importedDeviceProfile), any());
        verify(clusterService).broadcastEntityStateChangeEvent(any(), eq(importedDeviceProfile.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(clusterService).sendNotificationMsgToEdgeService(any(), any(), eq(importedDeviceProfile.getId()), any(), any(), eq(EdgeEventActionType.ADDED), any(), any());
        verify(otaPackageStateService).update(eq(importedDeviceProfile), eq(false), eq(false));

        ((DeviceExportData) entitiesExportData.get(EntityType.DEVICE)).getCredentials().setCredentialsId("abc");
        Device importedDevice = (Device) importEntity(entitiesExportData.get(EntityType.DEVICE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDevice.getId()), eq(importedDevice),
                any(), eq(ActionType.ADDED), isNull());
        verify(clusterService).onDeviceUpdated(eq(importedDevice), isNull());
        importEntity(entitiesExportData.get(EntityType.DEVICE));
        verify(clusterService).onDeviceUpdated(eq(importedDevice), eq(importedDevice));
    }

}
