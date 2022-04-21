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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.permission.GroupPermission;
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
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.sync.exporting.data.DeviceExportData;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.request.EntityExportSettings;
import org.thingsboard.server.service.sync.exporting.data.request.EntityListExportRequest;
import org.thingsboard.server.service.sync.exporting.data.request.ExportRequest;
import org.thingsboard.server.service.sync.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.importing.data.EntityImportSettings;
import org.thingsboard.server.service.sync.importing.data.request.ImportRequest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseEntitiesExportImportControllerTest extends AbstractControllerTest {

    @Autowired
    protected DeviceService deviceService;
    @Autowired
    protected OtaPackageService otaPackageService;
    @Autowired
    protected DeviceProfileService deviceProfileService;
    @Autowired
    protected AssetService assetService;
    @Autowired
    protected CustomerService customerService;
    @Autowired
    protected RuleChainService ruleChainService;
    @Autowired
    protected DashboardService dashboardService;
    @Autowired
    protected RelationService relationService;
    @Autowired
    protected TenantService tenantService;
    @Autowired
    protected EntityGroupService entityGroupService;
    @Autowired
    protected ConverterService converterService;
    @Autowired
    protected IntegrationService integrationService;
    @Autowired
    protected RoleService roleService;
    @Autowired
    protected GroupPermissionService groupPermissionService;

    protected TenantId tenantId1;
    protected User tenantAdmin1;

    protected TenantId tenantId2;
    protected User tenantAdmin2;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();
        Tenant tenant1 = new Tenant();
        tenant1.setTitle("Tenant 1");
        tenant1.setEmail("tenant1@thingsboard.org");
        this.tenantId1 = tenantService.saveTenant(tenant1).getId();
        User tenantAdmin1 = new User();
        tenantAdmin1.setTenantId(tenantId1);
        tenantAdmin1.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin1.setEmail("tenant1-admin@thingsboard.org");
        this.tenantAdmin1 = createUser(tenantAdmin1, "12345678");
        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Tenant 2");
        tenant2.setEmail("tenant2@thingsboard.org");
        this.tenantId2 = tenantService.saveTenant(tenant2).getId();
        User tenantAdmin2 = new User();
        tenantAdmin2.setTenantId(tenantId2);
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setEmail("tenant2-admin@thingsboard.org");
        this.tenantAdmin2 = createUser(tenantAdmin2, "12345678");
    }

    @After
    public void afterEach() {
        tenantService.deleteTenant(tenantId1);
        tenantService.deleteTenant(tenantId2);
    }

    protected Device createDevice(TenantId tenantId, CustomerId customerId, DeviceProfileId deviceProfileId, EntityGroupId entityGroupId, String name) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName(name);
        device.setLabel("lbl");
        device.setDeviceProfileId(deviceProfileId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        device.setDeviceData(deviceData);
        device = deviceService.saveDevice(device);
        if (entityGroupId != null) {
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, device.getId());
        }
        return device;
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

    protected void checkImportedDeviceData(Device initialDevice, Device importedDevice) {
        assertThat(importedDevice.getName()).isEqualTo(initialDevice.getName());
        assertThat(importedDevice.getType()).isEqualTo(initialDevice.getType());
        assertThat(importedDevice.getDeviceData()).isEqualTo(initialDevice.getDeviceData());
        assertThat(importedDevice.getLabel()).isEqualTo(initialDevice.getLabel());
    }

    protected DeviceProfile createDeviceProfile(TenantId tenantId, RuleChainId defaultRuleChainId, DashboardId defaultDashboardId, String name) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
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
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    protected void checkImportedDeviceProfileData(DeviceProfile initialProfile, DeviceProfile importedProfile) {
        assertThat(initialProfile.getName()).isEqualTo(importedProfile.getName());
        assertThat(initialProfile.getType()).isEqualTo(importedProfile.getType());
        assertThat(initialProfile.getTransportType()).isEqualTo(importedProfile.getTransportType());
        assertThat(initialProfile.getProfileData()).isEqualTo(importedProfile.getProfileData());
        assertThat(initialProfile.getDescription()).isEqualTo(importedProfile.getDescription());
    }

    protected Asset createAsset(TenantId tenantId, CustomerId customerId, EntityGroupId entityGroupId, String type, String name) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setCustomerId(customerId);
        asset.setType(type);
        asset.setName(name);
        asset.setLabel("lbl");
        asset.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        asset = assetService.saveAsset(asset);
        if (entityGroupId != null) {
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, asset.getId());
        }
        return asset;
    }

    protected void checkImportedAssetData(Asset initialAsset, Asset importedAsset) {
        assertThat(importedAsset.getName()).isEqualTo(initialAsset.getName());
        assertThat(importedAsset.getType()).isEqualTo(initialAsset.getType());
        assertThat(importedAsset.getLabel()).isEqualTo(initialAsset.getLabel());
        assertThat(importedAsset.getAdditionalInfo()).isEqualTo(initialAsset.getAdditionalInfo());
    }

    protected Customer createCustomer(TenantId tenantId, EntityGroupId entityGroupId, String name) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        if (entityGroupId != null) {
            EntityGroup customerGroup = entityGroupService.findEntityGroupById(tenantId, entityGroupId);
            if (customerGroup.getOwnerId() instanceof CustomerId) {
                customer.setParentCustomerId((CustomerId) customerGroup.getOwnerId());
            }
        }
        customer.setTitle(name);
        customer.setCountry("ua");
        customer.setAddress("abb");
        customer.setEmail("ccc@aa.org");
        customer.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        customer = customerService.saveCustomer(customer);
        if (entityGroupId != null) {
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, customer.getId());
        }
        return customer;
    }

    protected void checkImportedCustomerData(Customer initialCustomer, Customer importedCustomer) {
        assertThat(importedCustomer.getTitle()).isEqualTo(initialCustomer.getTitle());
        assertThat(importedCustomer.getCountry()).isEqualTo(initialCustomer.getCountry());
        assertThat(importedCustomer.getAddress()).isEqualTo(initialCustomer.getAddress());
        assertThat(importedCustomer.getEmail()).isEqualTo(initialCustomer.getEmail());
    }

    protected Dashboard createDashboard(TenantId tenantId, CustomerId customerId, EntityGroupId entityGroupId, String name) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setCustomerId(customerId);
        dashboard.setTitle(name);
        dashboard.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        dashboard.setImage("abvregewrg");
        dashboard.setMobileHide(true);
        dashboard = dashboardService.saveDashboard(dashboard);
        if (entityGroupId != null) {
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, dashboard.getId());
        }
        return dashboard;
    }

    protected void checkImportedDashboardData(Dashboard initialDashboard, Dashboard importedDashboard) {
        assertThat(importedDashboard.getTitle()).isEqualTo(initialDashboard.getTitle());
        assertThat(importedDashboard.getConfiguration()).isEqualTo(initialDashboard.getConfiguration());
        assertThat(importedDashboard.getImage()).isEqualTo(initialDashboard.getImage());
        assertThat(importedDashboard.isMobileHide()).isEqualTo(initialDashboard.isMobileHide());
    }

    protected RuleChain createRuleChain(TenantId tenantId, String name) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = ruleChainService.saveRuleChain(ruleChain);

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Simple Rule Node 1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(mapper.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(mapper.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, "Success");
        ruleChainService.saveRuleChainMetaData(tenantId, metaData);

        return ruleChainService.findRuleChainById(tenantId, ruleChain.getId());
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

    protected EntityRelation createRelation(EntityId from, EntityId to) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.MANAGES_TYPE);
        relation.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relationService.saveRelation(TenantId.SYS_TENANT_ID, relation);
        return relation;
    }

    protected EntityGroup createEntityGroup(EntityId ownerId, EntityType groupType, String name) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setOwnerId(ownerId);
        entityGroup.setType(groupType);
        entityGroup.setName(name);
        entityGroup.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, ownerId, entityGroup);
    }

    protected void checkImportedEntityGroupData(EntityGroup initialEntityGroup, EntityGroup importedEntityGroup) {
        assertThat(importedEntityGroup.getType()).isEqualTo(initialEntityGroup.getType());
        assertThat(importedEntityGroup.getName()).isEqualTo(initialEntityGroup.getName());
        assertThat(importedEntityGroup.getConfiguration()).isEqualTo(initialEntityGroup.getConfiguration());
        assertThat(importedEntityGroup.getAdditionalInfo()).isEqualTo(initialEntityGroup.getAdditionalInfo());
    }

    protected Converter createConverter(TenantId tenantId, ConverterType type, String name) {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setType(type);
        converter.setName(name);
        converter.setConfiguration(JacksonUtil.newObjectNode()
                .<ObjectNode>set("encoder", new TextNode("b"))
                .set("decoder", new TextNode("c")));
        converter.setDebugMode(true);
        converter.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return converterService.saveConverter(converter);
    }

    protected void checkImportedConverterData(Converter initialConverter, Converter importedConverter) {
        assertThat(importedConverter.getType()).isEqualTo(initialConverter.getType());
        assertThat(importedConverter.getName()).isEqualTo(initialConverter.getName());
        assertThat(importedConverter.getConfiguration()).isEqualTo(initialConverter.getConfiguration());
        assertThat(importedConverter.getAdditionalInfo()).isEqualTo(initialConverter.getAdditionalInfo());
        assertThat(importedConverter.isDebugMode()).isEqualTo(initialConverter.isDebugMode());
    }

    protected Integration createIntegration(TenantId tenantId, ConverterId converterId, IntegrationType type, String name) {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setType(type);
        integration.setName(name);
        integration.setDefaultConverterId(converterId);
        integration.setRoutingKey("abc");
        integration.setSecret("scrt");
        integration.setEnabled(false);
        integration.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        integration.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return integrationService.saveIntegration(integration);
    }

    protected void checkImportedIntegrationData(Integration initialIntegration, Integration importedIntegration) {
        assertThat(importedIntegration.getName()).isEqualTo(initialIntegration.getName());
        assertThat(importedIntegration.getType()).isEqualTo(initialIntegration.getType());
        assertThat(importedIntegration.getConfiguration()).isEqualTo(initialIntegration.getConfiguration());
        assertThat(importedIntegration.getAdditionalInfo()).isEqualTo(initialIntegration.getAdditionalInfo());
        assertThat(importedIntegration.getSecret()).isEqualTo(initialIntegration.getSecret());
    }

    protected Role createGenericRole(TenantId tenantId, CustomerId customerId, String name, Map<Resource, List<Operation>> permissions) {
        return createRole(tenantId, customerId, name, RoleType.GENERIC, permissions);
    }

    protected Role createGroupRole(TenantId tenantId, CustomerId customerId, String name, List<Operation> permissions) {
        return createRole(tenantId, customerId, name, RoleType.GROUP, permissions);
    }

    private Role createRole(TenantId tenantId, CustomerId customerId, String name, RoleType roleType, Object permissions) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCustomerId(customerId);
        role.setName(name);
        role.setType(roleType);
        role.setPermissions(JacksonUtil.valueToTree(permissions));
        return roleService.saveRole(tenantId, role);
    }

    protected GroupPermission createGroupPermission(TenantId tenantId, EntityGroupId userGroupId, RoleId genericRoleId) {
        return createGroupPermission(tenantId, userGroupId, genericRoleId, null, null);
    }

    protected GroupPermission createGroupPermission(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId, EntityGroupId entityGroupId, EntityType entityGroupType) {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setTenantId(tenantId);
        groupPermission.setUserGroupId(userGroupId);
        groupPermission.setRoleId(roleId);
        groupPermission.setEntityGroupId(entityGroupId);
        groupPermission.setEntityGroupType(entityGroupType);
        return groupPermissionService.saveGroupPermission(tenantId, groupPermission);
    }

    protected <E extends ExportableEntity<?> & HasTenantId> void checkImportedEntity(TenantId tenantId1, E initialEntity, TenantId tenantId2, E importedEntity) {
        assertThat(initialEntity.getTenantId()).isEqualTo(tenantId1);
        assertThat(importedEntity.getTenantId()).isEqualTo(tenantId2);

        assertThat(importedEntity.getExternalId()).isEqualTo(initialEntity.getId());

        boolean sameTenant = tenantId1.equals(tenantId2);
        if (!sameTenant) {
            assertThat(importedEntity.getId()).isNotEqualTo(initialEntity.getId());
        } else {
            assertThat(importedEntity.getId()).isEqualTo(initialEntity.getId());
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

    protected <D extends EntityExportData<?>> void updateExportData(List<EntityExportData<?>> exportDataList, EntityType entityType, Consumer<D> updater) {
        exportDataList.stream()
                .filter(exportData -> exportData.getEntityType() == entityType)
                .findFirst()
                .ifPresent(exportData -> updater.accept((D) exportData));
    }


    protected <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportSingleEntity(I entityId) throws Exception {
        return (EntityExportData<E>) exportEntities(entityId).get(0);
    }

    protected List<EntityExportData<?>> exportEntities(EntityId... entitiesIds) throws Exception {
        return exportEntities(Arrays.asList(entitiesIds), new EntityExportSettings());
    }

    protected List<EntityExportData<?>> exportEntities(Collection<EntityId> entitiesIds, EntityExportSettings exportSettings) throws Exception {
        EntityListExportRequest exportRequest = new EntityListExportRequest();
        exportRequest.setEntitiesIds(new ArrayList<>(entitiesIds));
        exportRequest.setExportSettings(exportSettings);
        return exportEntities(exportRequest);
    }

    protected List<EntityExportData<?>> exportEntities(ExportRequest exportRequest) throws Exception {
        return getResponse(doPost("/api/entities/export", exportRequest), new TypeReference<List<EntityExportData<?>>>() {});
    }

    protected List<EntityExportData<?>> exportEntities(List<ExportRequest> exportRequests) throws Exception {
        return getResponse(doPost("/api/entities/export?multiple", exportRequests), new TypeReference<List<EntityExportData<?>>>() {});
    }

    protected List<EntityImportResult<?>> exportAndImport(List<EntityId> entitiesIds, EntityExportSettings exportSettings,
                                                        EntityImportSettings importSettings) throws Exception {
        logInAsTenantAdmin1();
        List<EntityExportData<?>> exportDataList = exportEntities(entitiesIds,
                exportSettings == null ? new EntityExportSettings() : exportSettings);
        updateExportData(exportDataList, EntityType.DEVICE, this::resetUniqueProperties);
        updateExportData(exportDataList, EntityType.INTEGRATION, this::resetUniqueProperties);

        logInAsTenantAdmin2();
        ImportRequest importRequest = new ImportRequest();
        importRequest.setImportSettings(importSettings == null ? new EntityImportSettings() : importSettings);
        importRequest.setExportDataList(exportDataList);
        return importEntities(exportDataList);
    }

    protected void resetUniqueProperties(EntityExportData<?> exportData) {
        if (exportData.getEntityType() == EntityType.DEVICE) {
            ((DeviceExportData) exportData).setCredentials(null);
        } else if (exportData.getEntityType() == EntityType.INTEGRATION) {
            ((EntityExportData<Integration>) exportData).getEntity().setRoutingKey(RandomStringUtils.randomAlphanumeric(20));
        }
    }

    protected List<EntityImportResult<?>> exportAndImport(EntityId... entitiesIds) throws Exception {
        return exportAndImport(Arrays.asList(entitiesIds), new EntityExportSettings(), new EntityImportSettings());
    }


    protected <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(EntityExportData<E> exportData) throws Exception {
        return (EntityImportResult<E>) importEntities(List.of((EntityExportData<ExportableEntity<EntityId>>) exportData)).get(0);
    }

    protected List<EntityImportResult<?>> importEntities(List<EntityExportData<?>> exportDataList) throws Exception {
        return importEntities(exportDataList, new EntityImportSettings());
    }

    protected List<EntityImportResult<?>> importEntities(List<EntityExportData<?>> exportDataList, EntityImportSettings importSettings) throws Exception {
        ImportRequest importRequest = new ImportRequest();
        importRequest.setImportSettings(importSettings);
        importRequest.setExportDataList(exportDataList);
        return importEntities(importRequest);
    }

    protected List<EntityImportResult<?>> importEntities(ImportRequest importRequest) throws Exception {
        return getResponse(doPost("/api/entities/import", importRequest), new TypeReference<List<EntityImportResult<?>>>() {});
    }

    protected <T> T getResponse(ResultActions resultActions, TypeReference<T> typeReference) throws Exception {
        try {
            return readResponse(resultActions.andExpect(status().isOk()), typeReference);
        } catch (AssertionError e) {
            throw new AssertionError(readResponse(resultActions, String.class), e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <E extends ExportableEntity<?>> E findImportedEntity(List<EntityImportResult<?>> importResults, EntityType entityType, Predicate<E> entityPredicate) {
        return (E) importResults.stream().filter(importResult -> {
            return importResult.getEntityType() == entityType && entityPredicate.test((E) importResult.getSavedEntity());
        }).findFirst().map(EntityImportResult::getSavedEntity).orElse(null);
    }

    protected <E extends ExportableEntity<?>> E findImportedEntity(List<EntityImportResult<?>> importResults, EntityType entityType) {
        return findImportedEntity(importResults, entityType, e -> true);
    }

    protected void logInAsTenantAdmin1() throws Exception {
        login(tenantAdmin1.getEmail(), "12345678");
    }

    protected void logInAsTenantAdmin2() throws Exception {
        login(tenantAdmin2.getEmail(), "12345678");
    }

}
