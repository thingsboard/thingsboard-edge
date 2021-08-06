/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.PageDataFetcherWithAttempts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class EdgeClientTest extends AbstractContainerTest {

    private static final String CUSTOM_DEVICE_PROFILE_NAME = "Custom Device Profile";

    private Device globalTestDevice;
    private Asset globalTestAsset;

    @Test
    public void test() throws Exception {
        updateRootRuleChain();

        createCustomDeviceProfile();

        testReceivedInitialData();

        testDevices();

        testAssets();

        testRuleChains();

        testDashboards();

        testRelations();

        testAlarms();

        testEntityViews();

        // TODO
//        changeOwnerToCustomer();

        testWidgetsBundleAndWidgetType();

        testSendPostTelemetryRequestToEdge();

        testSendPostAttributesRequestToEdge();

        testSendAttributesUpdatedToEdge();

//        testRpcCall();

//        testTimeseriesWithFailures();

        testSendMessagesToCloud();
    }

    private void createCustomDeviceProfile() {
        DeviceProfile deviceProfile = createDeviceProfile(CUSTOM_DEVICE_PROFILE_NAME, null);
        extendDeviceProfileData(deviceProfile);
        restClient.saveDeviceProfile(deviceProfile);
    }

    private void testReceivedInitialData() {
        log.info("Checking received initial data");

        verifyRoles();
        verifyWidgetsBundles();
        verifyDeviceProfiles();
        verifyRuleChains();
        verifyEntityGroups(EntityType.DEVICE, 1);
        verifyEntityGroups(EntityType.ASSET, 1);
        verifyEntityGroups(EntityType.ENTITY_VIEW, 1);
        verifyEntityGroups(EntityType.DASHBOARD, 1);
        verifyEntityGroups(EntityType.USER, 3);

        verifyAdminSettings();
        verifyWhiteLabeling();

        log.info("Received initial data checked");
    }

    private void verifyDeviceProfiles() {
        PageData<DeviceProfile> pageData = new PageDataFetcherWithAttempts<>(
                link -> edgeRestClient.getDeviceProfiles(new PageLink(100)),
                50,
                3).fetchData();
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.DEVICE_PROFILE);
    }

    private void verifyEntityGroups(EntityType entityType, int expectedGroupsCount) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupInfo> entityGroupsByType = edgeRestClient.getEntityGroupsByType(entityType);
                    return entityGroupsByType.size() == expectedGroupsCount;
                });
        List<EntityGroupInfo> entityGroupsByType = edgeRestClient.getEntityGroupsByType(entityType);
        for (EntityGroupInfo entityGroupInfo : entityGroupsByType) {
            List<EntityId> entityIds;
            switch (entityType) {
                case DEVICE:
                    PageData<Device> devicesByEntityGroupId = edgeRestClient.getDevicesByEntityGroupId(entityGroupInfo.getId(), new PageLink(1000));
                    entityIds = devicesByEntityGroupId.getData().stream().map(IdBased::getId).collect(Collectors.toList());
                    break;
                case ASSET:
                    PageData<Asset> assetsByEntityGroupId = edgeRestClient.getAssetsByEntityGroupId(entityGroupInfo.getId(), new PageLink(1000));
                    entityIds = assetsByEntityGroupId.getData().stream().map(IdBased::getId).collect(Collectors.toList());
                    break;
                case ENTITY_VIEW:
                    PageData<EntityView> entityViewsByEntityGroupId = edgeRestClient.getEntityViewsByEntityGroupId(entityGroupInfo.getId(), new PageLink(1000));
                    entityIds = entityViewsByEntityGroupId.getData().stream().map(IdBased::getId).collect(Collectors.toList());
                    break;
                case DASHBOARD:
                    PageData<DashboardInfo> dashboardsByEntityGroupId = edgeRestClient.getGroupDashboards(entityGroupInfo.getId(), new PageLink(1000));
                    entityIds = dashboardsByEntityGroupId.getData().stream().map(IdBased::getId).collect(Collectors.toList());
                    break;
                case USER:
                    PageData<User> usersByEntityGroupId = edgeRestClient.getUsersByEntityGroupId(entityGroupInfo.getId(), new PageLink(1000));
                    entityIds = usersByEntityGroupId.getData().stream().map(IdBased::getId).collect(Collectors.toList());
                    break;
                default:
                    throw new IllegalArgumentException("Incorrect entity type provided " + entityType);
            }
            assertEntitiesByIdsAndType(entityIds, entityType);
        }
    }

    private void verifyWhiteLabeling() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    Optional<WhiteLabelingParams> edgeWhiteLabelParams = edgeRestClient.getCurrentWhiteLabelParams();
                    Optional<WhiteLabelingParams> cloudWhiteLabelParams = restClient.getCurrentWhiteLabelParams();
                    return edgeWhiteLabelParams.isPresent() &&
                            cloudWhiteLabelParams.isPresent() &&
                            edgeWhiteLabelParams.get().equals(cloudWhiteLabelParams.get());
                });

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    Optional<LoginWhiteLabelingParams> edgeLoginWhiteLabelParams = edgeRestClient.getCurrentLoginWhiteLabelParams();
                    Optional<LoginWhiteLabelingParams> cloudLoginWhiteLabelParams = restClient.getCurrentLoginWhiteLabelParams();
                    return edgeLoginWhiteLabelParams.isPresent() &&
                            cloudLoginWhiteLabelParams.isPresent() &&
                            edgeLoginWhiteLabelParams.get().equals(cloudLoginWhiteLabelParams.get());
                });

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    Optional<CustomTranslation> edgeCustomTranslation = edgeRestClient.getCustomTranslation();
                    Optional<CustomTranslation> cloudCustomTranslation = restClient.getCustomTranslation();
                    return edgeCustomTranslation.isPresent() &&
                            cloudCustomTranslation.isPresent() &&
                            edgeCustomTranslation.get().equals(cloudCustomTranslation.get());
                });
    }

    private void verifyAdminSettings() {
        verifyAdminSettingsByKey("general");
        verifyAdminSettingsByKey("mailTemplates");
        // TODO: @voba - uncomment this after latest merge with 3.3.0
        // verifyAdminSettingsByKey("mail");

        // TODO: @voba - verify admin setting in next release. In the current there is no sysadmin on edge to fetch it
        // login as sysadmin on edge
        // login as sysadmin on cloud
        // verifyAdminSettingsByKey("general");
        // verifyAdminSettingsByKey("mailTemplates");
        // verifyAdminSettingsByKey("mail");
    }

    private void verifyAdminSettingsByKey(String key) {
        Optional<AdminSettings> edgeAdminSettings = edgeRestClient.getAdminSettings(key);
        Assert.assertTrue("Admin settings is not available on edge, key = " + key, edgeAdminSettings.isPresent());
        Optional<AdminSettings> cloudAdminSettings = restClient.getAdminSettings(key);
        Assert.assertTrue("Admin settings is not available on cloud, key = " + key, cloudAdminSettings.isPresent());
        Assert.assertEquals("Admin settings on cloud and edge are different", edgeAdminSettings.get(), cloudAdminSettings.get());
    }

    private void verifyRoles() {
        PageData<Role> genericPageData = new PageDataFetcherWithAttempts<>(
                link -> edgeRestClient.getRoles(RoleType.GENERIC, new PageLink(100)),
                50,
                2).fetchData();
        List<EntityId> genericIds = genericPageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        PageData<Role> groupPageData = new PageDataFetcherWithAttempts<>(
                link -> edgeRestClient.getRoles(RoleType.GROUP, new PageLink(100)),
                50,
                1).fetchData();
        List<EntityId> groupIds = groupPageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        genericIds.addAll(groupIds);
        assertEntitiesByIdsAndType(genericIds, EntityType.ROLE);
    }

    private void verifyWidgetsBundles() {
        PageData<WidgetsBundle> pageData = new PageDataFetcherWithAttempts<>(
                link -> edgeRestClient.getWidgetsBundles(new PageLink(100)),
                50,
                16).fetchData();
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.WIDGETS_BUNDLE);

        for (String widgetsBundlesAlias : pageData.getData().stream().map(WidgetsBundle::getAlias).collect(Collectors.toList())) {
            boolean found = false;
            int attempt = 0;
            List<WidgetType> edgeBundleWidgetTypes = null;
            List<WidgetType> cloudBundleWidgetTypes = null;
            do {
                try {
                    edgeBundleWidgetTypes = edgeRestClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
                    cloudBundleWidgetTypes = restClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
                    if (cloudBundleWidgetTypes != null && edgeBundleWidgetTypes != null
                            && edgeBundleWidgetTypes.size() == cloudBundleWidgetTypes.size()) {
                        found = true;
                    }
                } catch (Exception ignored1) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored2) {}
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored2) {}
                attempt++;
                if (attempt > 50) {
                    break;
                }
            } while (!found);
            Assert.assertNotNull("edgeBundleWidgetTypes can't be null", edgeBundleWidgetTypes);
            Assert.assertNotNull("cloudBundleWidgetTypes can't be null", cloudBundleWidgetTypes);
            Assert.assertTrue("Number of fetched widget types for cloud and edge is different. " +
                    "Alias " + widgetsBundlesAlias + ", Cloud " + cloudBundleWidgetTypes.size() + ", Edge " + edgeBundleWidgetTypes.size(), found);
            assertEntitiesByIdsAndType(edgeBundleWidgetTypes.stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.WIDGET_TYPE);
        }
    }

    private void verifyRuleChains() {
        PageData<RuleChain> pageData = new PageDataFetcherWithAttempts<>(
                link -> edgeRestClient.getRuleChains(new PageLink(100)),
                50,
                1).fetchData();
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.RULE_CHAIN);
    }

    private void assertEntitiesByIdsAndType(List<EntityId> entityIds, EntityType entityType) {
        switch (entityType) {
            case DEVICE_PROFILE:
                assertDeviceProfiles(entityIds);
                break;
            case RULE_CHAIN:
                assertRuleChains(entityIds);
                break;
            case WIDGETS_BUNDLE:
                assertWidgetsBundles(entityIds);
                break;
            case WIDGET_TYPE:
                assertWidgetTypes(entityIds);
                break;
            case ROLE:
                assertRoles(entityIds);
                break;
            case DEVICE:
                assertDevices(entityIds);
                break;
            case ASSET:
                assertAssets(entityIds);
                break;
            case ENTITY_VIEW:
                assertEntityViews(entityIds);
                break;
            case DASHBOARD:
                assertDashboards(entityIds);
                break;
            case USER:
                assertUsers(entityIds);
                break;
        }
    }

    private void assertDeviceProfiles(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            DeviceProfileId deviceProfileId = new DeviceProfileId(entityId.getId());
            Optional<DeviceProfile> edgeDeviceProfile = edgeRestClient.getDeviceProfileById(deviceProfileId);
            Optional<DeviceProfile> cloudDeviceProfile = restClient.getDeviceProfileById(deviceProfileId);
            DeviceProfile expected = edgeDeviceProfile.get();
            DeviceProfile actual = cloudDeviceProfile.get();
            actual.setDefaultRuleChainId(null);
            Assert.assertEquals("Device profiles on cloud and edge are different (except defaultRuleChainId)", expected, actual);
        }
    }

    private void assertRuleChains(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            RuleChainId ruleChainId = new RuleChainId(entityId.getId());
            Optional<RuleChain> edgeRuleChain = edgeRestClient.getRuleChainById(ruleChainId);
            Optional<RuleChain> cloudRuleChain = restClient.getRuleChainById(ruleChainId);
            RuleChain expected = edgeRuleChain.get();
            RuleChain actual = cloudRuleChain.get();
            Assert.assertEquals("Edge rule chain type is incorrect", RuleChainType.CORE, expected.getType());
            Assert.assertEquals("Cloud rule chain type is incorrect", RuleChainType.EDGE, actual.getType());
            expected.setType(null);
            actual.setType(null);
            Assert.assertEquals("Rule chains on cloud and edge are different (except type)", expected, actual);

            Awaitility.await()
                    .atMost(30, TimeUnit.SECONDS).
                    until(() -> {
                        Optional<RuleChainMetaData> edgeRuleChainMetaData = edgeRestClient.getRuleChainMetaData(ruleChainId);
                        Optional<RuleChainMetaData> cloudRuleChainMetaData = restClient.getRuleChainMetaData(ruleChainId);
                        if (edgeRuleChainMetaData.isEmpty()) {
                            return false;
                        }
                        if (cloudRuleChainMetaData.isEmpty()) {
                            return false;
                        }
                        return validateRuleChainMetadata(edgeRuleChainMetaData.get(), cloudRuleChainMetaData.get());
                    });
        }
    }

    private boolean validateRuleChainMetadata(RuleChainMetaData expectedMetadata, RuleChainMetaData actualMetadata) {
        if (!expectedMetadata.getRuleChainId().equals(actualMetadata.getRuleChainId())) {
            return false;
        }
        if (expectedMetadata.getNodes().size() != actualMetadata.getNodes().size()) {
            return false;
        }
        if (expectedMetadata.getConnections().size() != actualMetadata.getConnections().size()) {
            return false;
        }
        for (RuleNode expectedNode : expectedMetadata.getNodes()) {
            Optional<RuleNode> actualNodeOpt =
                    actualMetadata.getNodes().stream().filter(n -> n.getId().equals(expectedNode.getId())).findFirst();
            if (actualNodeOpt.isEmpty()) {
                return false;
            }
            RuleNode actualNode = actualNodeOpt.get();
            // TODO: @voba - fix send of created time from cloud to edge
            actualNode.setCreatedTime(0);
            if (!expectedNode.equals(actualNode)) {
                return false;
            }
        }
        return true;
    }

    private void assertRoles(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            RoleId roleId = new RoleId(entityId.getId());
            Optional<Role> edgeRole = edgeRestClient.getRoleById(roleId);
            Optional<Role> cloudRole = restClient.getRoleById(roleId);
            Role expected = edgeRole.get();
            Role actual = cloudRole.get();
            // permissions field is transient and not used in comparison
            Assert.assertEquals("Roles on cloud and edge are different", expected, actual);
        }
    }

    private void assertWidgetsBundles(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            WidgetsBundleId widgetsBundleId = new WidgetsBundleId(entityId.getId());
            Optional<WidgetsBundle> edgeWidgetsBundle = edgeRestClient.getWidgetsBundleById(widgetsBundleId);
            Optional<WidgetsBundle> cloudWidgetsBundle = restClient.getWidgetsBundleById(widgetsBundleId);
            WidgetsBundle expected = edgeWidgetsBundle.get();
            WidgetsBundle actual = cloudWidgetsBundle.get();
            Assert.assertEquals("Widgets bundles on cloud and edge are different", expected, actual);
        }
    }

    private void assertWidgetTypes(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            WidgetTypeId widgetTypeId = new WidgetTypeId(entityId.getId());
            Optional<WidgetType> edgeWidgetsBundle = edgeRestClient.getWidgetTypeById(widgetTypeId);
            Optional<WidgetType> cloudWidgetsBundle = restClient.getWidgetTypeById(widgetTypeId);
            WidgetType expected = edgeWidgetsBundle.get();
            WidgetType actual = cloudWidgetsBundle.get();
            Assert.assertEquals("Widget types on cloud and edge are different", expected, actual);
        }
    }

    private void assertDevices(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            DeviceId deviceId = new DeviceId(entityId.getId());
            Optional<Device> edgeDevice = edgeRestClient.getDeviceById(deviceId);
            Optional<Device> cloudDevice = restClient.getDeviceById(deviceId);
            Device expected = edgeDevice.get();
            Device actual = cloudDevice.get();
            Assert.assertEquals("Devices on cloud and edge are different", expected, actual);
        }
    }

    private void assertAssets(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            AssetId assetId = new AssetId(entityId.getId());
            Optional<Asset> edgeAsset = edgeRestClient.getAssetById(assetId);
            Optional<Asset> cloudAsset = restClient.getAssetById(assetId);
            Asset expected = edgeAsset.get();
            Asset actual = cloudAsset.get();
            Assert.assertEquals("Assets on cloud and edge are different", expected, actual);
        }
    }

    private void assertEntityViews(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            EntityViewId entityViewId = new EntityViewId(entityId.getId());
            Optional<EntityView> edgeEntityView = edgeRestClient.getEntityViewById(entityViewId);
            Optional<EntityView> cloudEntityView = restClient.getEntityViewById(entityViewId);
            EntityView expected = edgeEntityView.get();
            EntityView actual = cloudEntityView.get();
            Assert.assertEquals("Entity Views on cloud and edge are different", expected, actual);
        }
    }

    private void assertDashboards(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            DashboardId dashboardId = new DashboardId(entityId.getId());
            Optional<Dashboard> edgeDashboard = edgeRestClient.getDashboardById(dashboardId);
            Optional<Dashboard> cloudDashboard = restClient.getDashboardById(dashboardId);
            Dashboard expected = edgeDashboard.get();
            Dashboard actual = cloudDashboard.get();
            Assert.assertEquals("Dashboards on cloud and edge are different", expected, actual);
        }
    }

    private void assertUsers(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            UserId userId = new UserId(entityId.getId());
            Optional<User> edgeUser = edgeRestClient.getUserById(userId);
            Optional<User> cloudUser = restClient.getUserById(userId);
            User expected = edgeUser.get();
            User actual = cloudUser.get();
            expected.setAdditionalInfo(cleanLastLoginTsFromAdditionalInfo(expected.getAdditionalInfo()));
            actual.setAdditionalInfo(cleanLastLoginTsFromAdditionalInfo(actual.getAdditionalInfo()));
            Assert.assertEquals("Users on cloud and edge are different (except lastLoginTs)", expected, actual);
        }
    }

    private JsonNode cleanLastLoginTsFromAdditionalInfo(JsonNode additionalInfo) {
        if (additionalInfo != null && additionalInfo.has("lastLoginTs")) {
            ((ObjectNode) additionalInfo).remove("lastLoginTs");
        }
        return additionalInfo;
    }

    private void testDevices() throws Exception {
        log.info("Testing devices");

        EntityGroup deviceEntityGroup = new EntityGroup();
        deviceEntityGroup.setType(EntityType.DEVICE);
        deviceEntityGroup.setName("DeviceGroup");
        EntityGroupInfo savedDeviceEntityGroup = restClient.saveEntityGroup(deviceEntityGroup);
        globalTestDevice = saveDeviceOnCloud("Edge Device 1", "default", savedDeviceEntityGroup.getId());

        restClient.saveDeviceAttributes(globalTestDevice.getId(), DataConstants.SERVER_SCOPE, mapper.readTree("{\"key1\":\"value1\"}"));
        restClient.saveDeviceAttributes(globalTestDevice.getId(), DataConstants.SHARED_SCOPE, mapper.readTree("{\"key2\":\"value2\"}"));

        restClient.assignEntityGroupToEdge(edge.getId(), savedDeviceEntityGroup.getId(), EntityType.DEVICE);

        verifyEntityGroups(EntityType.DEVICE, 2);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDeviceById(globalTestDevice.getId()).isPresent());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> verifyAttributeOnEdge(globalTestDevice.getId(), DataConstants.SERVER_SCOPE, "key1", "value1"));

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> verifyAttributeOnEdge(globalTestDevice.getId(), DataConstants.SHARED_SCOPE, "key2", "value2"));

        // wait to fully process all edge entities for group requests
        Thread.sleep(1000);

        restClient.unassignEntityGroupFromEdge(edge.getId(), savedDeviceEntityGroup.getId(), EntityType.DEVICE);

        verifyEntityGroups(EntityType.DEVICE, 1);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDeviceById(globalTestDevice.getId()).isEmpty());

        restClient.assignEntityGroupToEdge(edge.getId(), savedDeviceEntityGroup.getId(), EntityType.DEVICE);
        verifyEntityGroups(EntityType.DEVICE, 2);

        log.info("Devices tested successfully");
    }

    private boolean verifyAttributeOnEdge(EntityId entityId, String scope, String key, String expectedValue) {
        List<AttributeKvEntry> attributesByScope = edgeRestClient.getAttributesByScope(entityId, scope, Arrays.asList(key));
        if (attributesByScope.isEmpty()) {
            return false;
        }
        AttributeKvEntry attributeKvEntry = attributesByScope.get(0);
        return attributeKvEntry.getValueAsString().equals(expectedValue);
    }

    private void testAssets() throws Exception {
        log.info("Testing assets");

        EntityGroup assetEntityGroup = new EntityGroup();
        assetEntityGroup.setType(EntityType.ASSET);
        assetEntityGroup.setName("AssetGroup");
        EntityGroupInfo savedAssetEntityGroup = restClient.saveEntityGroup(assetEntityGroup);
        globalTestAsset = saveAssetOnCloud("Edge Asset 1", "Building", savedAssetEntityGroup.getId());

        restClient.assignEntityGroupToEdge(edge.getId(), savedAssetEntityGroup.getId(), EntityType.ASSET);

        verifyEntityGroups(EntityType.ASSET, 2);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getAssetById(globalTestAsset.getId()).isPresent());

        // wait to fully process all edge entities for group requests
        Thread.sleep(1000);

        restClient.unassignEntityGroupFromEdge(edge.getId(), savedAssetEntityGroup.getId(), EntityType.ASSET);

        verifyEntityGroups(EntityType.ASSET, 1);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getAssetById(globalTestAsset.getId()).isEmpty());

        restClient.assignEntityGroupToEdge(edge.getId(), savedAssetEntityGroup.getId(), EntityType.ASSET);

        verifyEntityGroups(EntityType.ASSET, 2);

        log.info("Assets tested successfully");
    }

    private void testRuleChains() throws Exception {
        log.info("Testing RuleChains");

        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge Test Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = restClient.saveRuleChain(ruleChain);
        restClient.assignRuleChainToEdge(edge.getId(), savedRuleChain.getId());
        createRuleChainMetadata(savedRuleChain);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getRuleChainById(savedRuleChain.getId()).isPresent());

        assertEntitiesByIdsAndType(Collections.singletonList(savedRuleChain.getId()), EntityType.RULE_CHAIN);

        restClient.unassignRuleChainFromEdge(edge.getId(), savedRuleChain.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getRuleChainById(savedRuleChain.getId()).isEmpty());

        restClient.deleteRuleChain(savedRuleChain.getId());

        log.info("RuleChains tested successfully");
    }

    private void createRuleChainMetadata(RuleChain ruleChain) throws Exception {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());

        ObjectMapper mapper = new ObjectMapper();

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType("type1");
        ruleNode1.setConfiguration(mapper.readTree("\"key1\": \"val1\""));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType("type2");
        ruleNode2.setConfiguration(mapper.readTree("\"key2\": \"val2\""));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType("type3");
        ruleNode3.setConfiguration(mapper.readTree("\"key3\": \"val3\""));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0, 1, "success");
        ruleChainMetaData.addConnectionInfo(0, 2, "fail");
        ruleChainMetaData.addConnectionInfo(1, 2, "success");

        ruleChainMetaData.addRuleChainConnectionInfo(2, edge.getRootRuleChainId(), "success", mapper.createObjectNode());

        restClient.saveRuleChainMetaData(ruleChainMetaData);
    }

    private void testDashboards() throws Exception {
        log.info("Testing Dashboards");

        EntityGroup dashboardEntityGroup = new EntityGroup();
        dashboardEntityGroup.setType(EntityType.DASHBOARD);
        dashboardEntityGroup.setName("DashboardGroup");
        EntityGroupInfo savedDashboardEntityGroup = restClient.saveEntityGroup(dashboardEntityGroup);
        Dashboard savedDashboardOnCloud = saveDashboardOnCloud("Edge Dashboard 1", savedDashboardEntityGroup.getId());

        restClient.assignEntityGroupToEdge(edge.getId(), savedDashboardEntityGroup.getId(), EntityType.DASHBOARD);

        verifyEntityGroups(EntityType.DASHBOARD, 2);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).isPresent());

        // wait to fully process all edge entities for group requests
        Thread.sleep(1000);

        restClient.unassignEntityGroupFromEdge(edge.getId(), savedDashboardEntityGroup.getId(), EntityType.DASHBOARD);

        verifyEntityGroups(EntityType.DASHBOARD, 1);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).isEmpty());

        restClient.deleteDashboard(savedDashboardOnCloud.getId());

        log.info("Dashboards tested successfully");
    }

    private void testRelations() throws Exception {
        log.info("Testing Relations");

        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(globalTestDevice.getId());
        relation.setTo(globalTestAsset.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        restClient.saveRelation(relation);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isPresent());

        restClient.deleteRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isEmpty());

        log.info("Relations tested successfully");
    }

    private void testAlarms() throws Exception {
        log.info("Testing Alarms");

        Alarm alarm = new Alarm();
        alarm.setOriginator(globalTestDevice.getId());
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarm.setType("alarm");
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        Alarm savedAlarm = restClient.saveAlarm(alarm);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> getAlarmForEntityFromEdge(globalTestDevice.getId()).isPresent());

        restClient.ackAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    AlarmInfo alarmData = getAlarmForEntityFromEdge(globalTestDevice.getId()).get();
                    return alarmData.getAckTs() > 0;
                });

        restClient.clearAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    AlarmInfo alarmData = getAlarmForEntityFromEdge(globalTestDevice.getId()).get();
                    return alarmData.getClearTs() > 0;
                });

        restClient.deleteAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> getAlarmForEntityFromEdge(globalTestDevice.getId()).isEmpty());

        log.info("Alarms tested successfully");
    }


    private Optional<AlarmInfo> getAlarmForEntityFromCloud(EntityId entityId) {
        return getAlarmForEntity(entityId, restClient);
    }

    private Optional<AlarmInfo> getAlarmForEntityFromEdge(EntityId entityId) {
        return getAlarmForEntity(entityId, edgeRestClient);
    }

    private Optional<AlarmInfo> getAlarmForEntity(EntityId entityId, RestClient restClient) {
        PageData<AlarmInfo> alarmDataByQuery =
                restClient.getAlarms(entityId, AlarmSearchStatus.ANY, null, new TimePageLink(1), false);
        if (alarmDataByQuery.getData().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(alarmDataByQuery.getData().get(0));
        }
    }

    private void testEntityViews() throws Exception {
        log.info("Testing EntityView");

        EntityGroup entityViewEntityGroup = new EntityGroup();
        entityViewEntityGroup.setType(EntityType.ENTITY_VIEW);
        entityViewEntityGroup.setName("EntityViewGroup");
        EntityGroupInfo savedEntityViewEntityGroup = restClient.saveEntityGroup(entityViewEntityGroup);
        EntityView savedEntityViewOnCloud = saveEntityViewOnCloud("Edge Entity View 1", "Default", globalTestDevice.getId(), savedEntityViewEntityGroup.getId());

        restClient.assignEntityGroupToEdge(edge.getId(), savedEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);

        verifyEntityGroups(EntityType.ENTITY_VIEW, 2);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isPresent());

        // wait to fully process all edge entities for group requests
        Thread.sleep(1000);

        restClient.unassignEntityGroupFromEdge(edge.getId(), savedEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);

        verifyEntityGroups(EntityType.ENTITY_VIEW, 1);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isEmpty());

        restClient.deleteEntityView(savedEntityViewOnCloud.getId());

        log.info("EntityView tested successfully");
    }

    private void testWidgetsBundleAndWidgetType() throws Exception {
        log.info("Testing WidgetsBundle and WidgetType");

        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("Test Widget Bundle");
        WidgetsBundle savedWidgetsBundle = restClient.saveWidgetsBundle(widgetsBundle);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getWidgetsBundleById(savedWidgetsBundle.getId()).isPresent());

        WidgetType widgetType = new WidgetType();
        widgetType.setName("Test Widget Type");
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        ObjectNode descriptor = mapper.createObjectNode();
        descriptor.put("key", "value");
        widgetType.setDescriptor(descriptor);
        WidgetType savedWidgetType = restClient.saveWidgetType(widgetType);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getWidgetTypeById(savedWidgetType.getId()).isPresent());

        restClient.deleteWidgetType(savedWidgetType.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getWidgetTypeById(savedWidgetType.getId()).isEmpty());

        restClient.deleteWidgetsBundle(savedWidgetsBundle.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getWidgetsBundleById(savedWidgetsBundle.getId()).isEmpty());

        log.info("WidgetsBundle and WidgetType tested successfully");
    }

    private void testSendPostTelemetryRequestToCloud() throws Exception {
        List<String> keys = Arrays.asList("strTelemetryToCloud", "boolTelemetryToCloud", "doubleTelemetryToCloud", "longTelemetryToCloud");

        JsonObject timeseriesPayload = new JsonObject();
        timeseriesPayload.addProperty("strTelemetryToCloud", "value1");
        timeseriesPayload.addProperty("boolTelemetryToCloud", true);
        timeseriesPayload.addProperty("doubleTelemetryToCloud", 42.0);
        timeseriesPayload.addProperty("longTelemetryToCloud", 72L);

        List<TsKvEntry> kvEntries = sendPostTelemetryRequest(edgeRestClient, edgeUrl, restClient, timeseriesPayload, keys);

        for (TsKvEntry kvEntry : kvEntries) {
            if (kvEntry.getKey().equals("strTelemetryToCloud")) {
                Assert.assertEquals("value1", kvEntry.getStrValue().get());
            }
            if (kvEntry.getKey().equals("boolTelemetryToCloud")) {
                Assert.assertEquals(true, kvEntry.getBooleanValue().get());
            }
            if (kvEntry.getKey().equals("doubleTelemetryToCloud")) {
                Assert.assertEquals(42.0, (double) kvEntry.getDoubleValue().get(), 0.0);
            }
            if (kvEntry.getKey().equals("longTelemetryToCloud")) {
                Assert.assertEquals(72L, kvEntry.getLongValue().get().longValue());
            }
        }
    }

    private void testSendPostTelemetryRequestToEdge() throws Exception {
        List<String> keys = Arrays.asList("strTelemetryToEdge", "boolTelemetryToEdge", "doubleTelemetryToEdge", "longTelemetryToEdge");

        JsonObject timeseriesPayload = new JsonObject();
        timeseriesPayload.addProperty("strTelemetryToEdge", "value1");
        timeseriesPayload.addProperty("boolTelemetryToEdge", true);
        timeseriesPayload.addProperty("doubleTelemetryToEdge", 42.0);
        timeseriesPayload.addProperty("longTelemetryToEdge", 72L);

        List<TsKvEntry> kvEntries = sendPostTelemetryRequest(restClient, CLOUD_HTTPS_URL, edgeRestClient, timeseriesPayload, keys);

        for (TsKvEntry kvEntry : kvEntries) {
            if (kvEntry.getKey().equals("strTelemetryToEdge")) {
                Assert.assertEquals("value1", kvEntry.getStrValue().get());
            }
            if (kvEntry.getKey().equals("boolTelemetryToEdge")) {
                Assert.assertEquals(true, kvEntry.getBooleanValue().get());
            }
            if (kvEntry.getKey().equals("doubleTelemetryToEdge")) {
                Assert.assertEquals(42.0, (double) kvEntry.getDoubleValue().get(), 0.0);
            }
            if (kvEntry.getKey().equals("longTelemetryToEdge")) {
                Assert.assertEquals(72L, kvEntry.getLongValue().get().longValue());
            }
        }
    }

    private List<TsKvEntry> sendPostTelemetryRequest(RestClient sourceRestClient, String sourceUrl, RestClient targetRestClient,
                                          JsonObject timeseriesPayload, List<String> keys) throws Exception {
        DeviceCredentials deviceCredentials = sourceRestClient.getDeviceCredentialsByDeviceId(globalTestDevice.getId()).get();
        String accessToken = deviceCredentials.getCredentialsId();

        ResponseEntity deviceTelemetryResponse = sourceRestClient.getRestTemplate()
                .postForEntity(sourceUrl + "/api/v1/{credentialsId}/telemetry",
                        mapper.readTree(timeseriesPayload.toString()),
                        ResponseEntity.class,
                        accessToken);
        Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> targetRestClient.getLatestTimeseries(globalTestDevice.getId(), keys).size() == keys.size());

        return targetRestClient.getLatestTimeseries(globalTestDevice.getId(), keys);
    }


    private void testSendPostAttributesRequestToCloud() throws Exception {
        List<String> keys = Arrays.asList("strAttrToCloud", "boolAttrToCloud", "doubleAttrToCloud", "longAttrToCloud");

        JsonObject attrPayload = new JsonObject();
        attrPayload.addProperty("strAttrToCloud", "value1");
        attrPayload.addProperty("boolAttrToCloud", true);
        attrPayload.addProperty("doubleAttrToCloud", 42.0);
        attrPayload.addProperty("longAttrToCloud", 72L);

        List<AttributeKvEntry> kvEntries = testSendPostAttributesRequest(edgeRestClient, edgeUrl, restClient, attrPayload, keys);

        for (AttributeKvEntry attributeKvEntry : kvEntries) {
            if (attributeKvEntry.getKey().equals("strAttrToCloud")) {
                Assert.assertEquals("value1", attributeKvEntry.getStrValue().get());
            }
            if (attributeKvEntry.getKey().equals("boolAttrToCloud")) {
                Assert.assertEquals(true, attributeKvEntry.getBooleanValue().get());
            }
            if (attributeKvEntry.getKey().equals("doubleAttrToCloud")) {
                Assert.assertEquals(42.0, (double) attributeKvEntry.getDoubleValue().get(), 0.0);
            }
            if (attributeKvEntry.getKey().equals("longAttrToCloud")) {
                Assert.assertEquals(72L, attributeKvEntry.getLongValue().get().longValue());
            }
        }

    }

    private void testSendPostAttributesRequestToEdge() throws Exception {
        List<String> keys = Arrays.asList("strAttrToEdge", "boolAttrToEdge", "doubleAttrToEdge", "longAttrToEdge");

        JsonObject attrPayload = new JsonObject();
        attrPayload.addProperty("strAttrToEdge", "value1");
        attrPayload.addProperty("boolAttrToEdge", true);
        attrPayload.addProperty("doubleAttrToEdge", 42.0);
        attrPayload.addProperty("longAttrToEdge", 72L);

        List<AttributeKvEntry> kvEntries = testSendPostAttributesRequest(restClient, CLOUD_HTTPS_URL, edgeRestClient, attrPayload, keys);

        for (AttributeKvEntry attributeKvEntry : kvEntries) {
            if (attributeKvEntry.getKey().equals("strAttrToEdge")) {
                Assert.assertEquals("value1", attributeKvEntry.getStrValue().get());
            }
            if (attributeKvEntry.getKey().equals("boolAttrToEdge")) {
                Assert.assertEquals(true, attributeKvEntry.getBooleanValue().get());
            }
            if (attributeKvEntry.getKey().equals("doubleAttrToEdge")) {
                Assert.assertEquals(42.0, (double) attributeKvEntry.getDoubleValue().get(), 0.0);
            }
            if (attributeKvEntry.getKey().equals("longAttrToEdge")) {
                Assert.assertEquals(72L, attributeKvEntry.getLongValue().get().longValue());
            }
        }
    }

    private List<AttributeKvEntry> testSendPostAttributesRequest(RestClient sourceRestClient, String sourceUrl, RestClient targetRestClient,
                                               JsonObject attributesPayload, List<String> keys) throws Exception {
        DeviceCredentials deviceCredentials = sourceRestClient.getDeviceCredentialsByDeviceId(globalTestDevice.getId()).get();
        String accessToken = deviceCredentials.getCredentialsId();

        ResponseEntity deviceClientsAttributes = sourceRestClient.getRestTemplate()
                .postForEntity(sourceUrl + "/api/v1/" + accessToken + "/attributes/", mapper.readTree(attributesPayload.toString()),
                        ResponseEntity.class,
                        accessToken);
        Assert.assertTrue(deviceClientsAttributes.getStatusCode().is2xxSuccessful());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> targetRestClient.getAttributeKvEntries(globalTestDevice.getId(), keys).size() == keys.size());

        List<AttributeKvEntry> attributeKvEntries = targetRestClient.getAttributeKvEntries(globalTestDevice.getId(), keys);

        sourceRestClient.deleteEntityAttributes(globalTestDevice.getId(), DataConstants.CLIENT_SCOPE, keys);

        // TODO: @voba - verify remove of attributes from cloud
//        Awaitility.await()
//                .atMost(30, TimeUnit.SECONDS)
//                .until(() -> targetRestClient.getAttributeKvEntries(globalTestDevice.getId(), keys).isEmpty());

        return attributeKvEntries;
    }

    private void testSendAttributesUpdatedToEdge() throws Exception {
        List<String> keys = Arrays.asList("strAttrToEdge", "boolAttrToEdge", "doubleAttrToEdge", "longAttrToEdge");

        JsonObject attrPayload = new JsonObject();
        attrPayload.addProperty("strAttrToEdge", "value1");
        attrPayload.addProperty("boolAttrToEdge", true);
        attrPayload.addProperty("doubleAttrToEdge", 42.0);
        attrPayload.addProperty("longAttrToEdge", 72L);

        List<AttributeKvEntry> kvEntries = sendAttributesUpdated(restClient, edgeRestClient, attrPayload, keys, DataConstants.SERVER_SCOPE);
        verifyAttributesUpdatedToEdge(kvEntries);

        kvEntries = sendAttributesUpdated(restClient, edgeRestClient, attrPayload, keys, DataConstants.SHARED_SCOPE);
        verifyAttributesUpdatedToEdge(kvEntries);
    }

    private void verifyAttributesUpdatedToEdge(List<AttributeKvEntry> kvEntries) {
        for (AttributeKvEntry attributeKvEntry : kvEntries) {
            if (attributeKvEntry.getKey().equals("strAttrToEdge")) {
                Assert.assertEquals("value1", attributeKvEntry.getStrValue().get());
            }
            if (attributeKvEntry.getKey().equals("boolAttrToEdge")) {
                Assert.assertEquals(true, attributeKvEntry.getBooleanValue().get());
            }
            if (attributeKvEntry.getKey().equals("doubleAttrToEdge")) {
                Assert.assertEquals(42.0, (double) attributeKvEntry.getDoubleValue().get(), 0.0);
            }
            if (attributeKvEntry.getKey().equals("longAttrToEdge")) {
                Assert.assertEquals(72L, attributeKvEntry.getLongValue().get().longValue());
            }
        }
    }

    private void testSendAttributesUpdatedToCloud() throws Exception {
        List<String> keys = Arrays.asList("strAttrToCloud", "boolAttrToCloud", "doubleAttrToCloud", "longAttrToCloud");

        JsonObject attrPayload = new JsonObject();
        attrPayload.addProperty("strAttrToCloud", "value1");
        attrPayload.addProperty("boolAttrToCloud", true);
        attrPayload.addProperty("doubleAttrToCloud", 42.0);
        attrPayload.addProperty("longAttrToCloud", 72L);

        List<AttributeKvEntry> kvEntries = sendAttributesUpdated(edgeRestClient, restClient, attrPayload, keys, DataConstants.SERVER_SCOPE);
        verifyAttributesUpdatedToCloud(kvEntries);

        kvEntries = sendAttributesUpdated(edgeRestClient, restClient, attrPayload, keys, DataConstants.SHARED_SCOPE);
        verifyAttributesUpdatedToCloud(kvEntries);
    }

    private void verifyAttributesUpdatedToCloud(List<AttributeKvEntry> kvEntries) {
        for (AttributeKvEntry attributeKvEntry : kvEntries) {
            if (attributeKvEntry.getKey().equals("strAttrToCloud")) {
                Assert.assertEquals("value1", attributeKvEntry.getStrValue().get());
            }
            if (attributeKvEntry.getKey().equals("boolAttrToCloud")) {
                Assert.assertEquals(true, attributeKvEntry.getBooleanValue().get());
            }
            if (attributeKvEntry.getKey().equals("doubleAttrToCloud")) {
                Assert.assertEquals(42.0, (double) attributeKvEntry.getDoubleValue().get(), 0.0);
            }
            if (attributeKvEntry.getKey().equals("longAttrToCloud")) {
                Assert.assertEquals(72L, attributeKvEntry.getLongValue().get().longValue());
            }
        }
    }

    private List<AttributeKvEntry> sendAttributesUpdated(RestClient sourceRestClient, RestClient targetRestClient,
                                                         JsonObject attributesPayload, List<String> keys, String scope) throws Exception {
        sourceRestClient.saveDeviceAttributes(globalTestDevice.getId(), scope, mapper.readTree(attributesPayload.toString()));

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> targetRestClient.getAttributeKvEntries(globalTestDevice.getId(), keys).size() == keys.size());

        List<AttributeKvEntry> attributeKvEntries =
                targetRestClient.getAttributeKvEntries(globalTestDevice.getId(), keys);

        sourceRestClient.deleteEntityAttributes(globalTestDevice.getId(), scope, keys);

        // TODO: @voba - verify remove of attributes from edge
//        Awaitility.await()
//                .atMost(30, TimeUnit.SECONDS)
//                .until(() -> targetRestClient.getAttributeKvEntries(globalTestDevice.getId(), keys).isEmpty());

        return attributeKvEntries;
    }

//    private void testRpcCall() throws Exception {
//        ObjectNode body = mapper.createObjectNode();
//        body.put("requestId", new Random().nextInt());
//        body.put("requestUUID", Uuids.timeBased().toString());
//        body.put("oneway", false);
//        body.put("expirationTime", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
//        body.put("method", "test_method");
//        body.put("params", "{\"param1\":\"value1\"}");
//
//        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.RPC_CALL, globalTestDevice.getId().getId(), EdgeEventType.DEVICE, body);
//        edgeImitator.expectMessageAmount(1);
//        edgeEventService.saveAsync(edgeEvent);
//        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof DeviceRpcCallMsg);
//        DeviceRpcCallMsg latestDeviceRpcCallMsg = (DeviceRpcCallMsg) latestMessage;
//        Assert.assertEquals("test_method", latestDeviceRpcCallMsg.getRequestMsg().getMethod());
//    }
//
//    private void testTimeseriesWithFailures() throws Exception {
//        log.info("Testing timeseries with failures");
//
//        int numberOfTimeseriesToSend = 1000;
//
//        edgeImitator.setRandomFailuresOnTimeseriesDownlink(true);
//        // imitator will generate failure in 5% of cases
//        edgeImitator.setFailureProbability(5.0);
//
//        edgeImitator.expectMessageAmount(numberOfTimeseriesToSend);
//        Device device = saveDevice(RandomStringUtils.randomAlphanumeric(15), CUSTOM_DEVICE_PROFILE_NAME);
//        for (int idx = 1; idx <= numberOfTimeseriesToSend; idx++) {
//            String timeseriesData = "{\"data\":{\"idx\":" + idx + "},\"ts\":" + System.currentTimeMillis() + "}";
//            JsonNode timeseriesEntityData = mapper.readTree(timeseriesData);
//            EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED,
//                    device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
//            edgeEventService.saveAsync(edgeEvent);
//            clusterService.onEdgeEventUpdate(tenantId, edge.getId());
//        }
//
//        Assert.assertTrue(edgeImitator.waitForMessages(60));
//
//        List<EntityDataProto> allTelemetryMsgs = edgeImitator.findAllMessagesByType(EntityDataProto.class);
//        Assert.assertEquals(numberOfTimeseriesToSend, allTelemetryMsgs.size());
//
//        for (int idx = 1; idx <= numberOfTimeseriesToSend; idx++) {
//            Assert.assertTrue(isIdxExistsInTheDownlinkList(idx, allTelemetryMsgs));
//        }
//
//        edgeImitator.setRandomFailuresOnTimeseriesDownlink(false);
//        log.info("Timeseries with failures tested successfully");
//    }
//
//    private boolean isIdxExistsInTheDownlinkList(int idx, List<EntityDataProto> allTelemetryMsgs) {
//        for (EntityDataProto proto : allTelemetryMsgs) {
//            TransportProtos.PostTelemetryMsg postTelemetryMsg = proto.getPostTelemetryMsg();
//            Assert.assertEquals(1, postTelemetryMsg.getTsKvListCount());
//            TransportProtos.TsKvListProto tsKvListProto = postTelemetryMsg.getTsKvList(0);
//            Assert.assertEquals(1, tsKvListProto.getKvCount());
//            TransportProtos.KeyValueProto keyValueProto = tsKvListProto.getKv(0);
//            Assert.assertEquals("idx", keyValueProto.getKey());
//            if (keyValueProto.getLongV() == idx) {
//                return true;
//            }
//        }
//        return false;
//    }
//
    private void testSendMessagesToCloud() throws Exception {
        log.info("Sending messages to cloud");
        sendDeviceToCloud();
        sendDeviceWithNameThatAlreadyExistsOnCloud();
        sendRelationToCloud();
        sendAlarmToCloud();
        testSendPostTelemetryRequestToCloud();
        testSendPostAttributesRequestToCloud();
        testSendAttributesUpdatedToCloud();
//        sendDeviceRpcResponse();
        log.info("Messages were sent successfully");
    }

    private void sendDeviceToCloud() throws Exception {
        log.info("Testing send device to cloud");

        Device savedDeviceOnEdge = saveDeviceOnEdge("Edge Device 2", "default");

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> restClient.getDeviceById(savedDeviceOnEdge.getId()).isPresent());

        verifyDeviceCredentialsOnCloudAndEdge(savedDeviceOnEdge);

        Optional<DeviceCredentials> deviceCredentialsByDeviceId =
                edgeRestClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdge.getId());
        Assert.assertTrue(deviceCredentialsByDeviceId.isPresent());
        DeviceCredentials deviceCredentials = deviceCredentialsByDeviceId.get();
        deviceCredentials.setCredentialsId("UpdatedToken");
        edgeRestClient.saveDeviceCredentials(deviceCredentials);

        verifyDeviceCredentialsOnCloudAndEdge(savedDeviceOnEdge);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> restClient.getEntityGroupsForEntity(savedDeviceOnEdge.getId()).size() == 2);

        edgeRestClient.deleteDevice(savedDeviceOnEdge.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> restClient.getEntityGroupsForEntity(savedDeviceOnEdge.getId()).size() == 1);

        log.info("Send device to cloud tested successfully");
    }

    private void verifyDeviceCredentialsOnCloudAndEdge(Device savedDeviceOnEdge) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> restClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdge.getId()).isPresent());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdge.getId()).isPresent());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    DeviceCredentials deviceCredentialsOnEdge =
                            edgeRestClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdge.getId()).get();
                    DeviceCredentials deviceCredentialsOnCloud =
                            restClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdge.getId()).get();
                    // TODO: @voba - potential fix for future releases
                    deviceCredentialsOnCloud.setId(null);
                    deviceCredentialsOnEdge.setId(null);
                    deviceCredentialsOnCloud.setCreatedTime(0);
                    deviceCredentialsOnEdge.setCreatedTime(0);
                    return deviceCredentialsOnCloud.equals(deviceCredentialsOnEdge);
                });
    }

    private void sendDeviceWithNameThatAlreadyExistsOnCloud() throws Exception {
        log.info("Testing send device to cloud with name already exists on cloud");

        String deviceName = RandomStringUtils.randomAlphanumeric(15);
        Device savedDeviceOnCloud = saveDeviceOnCloud(deviceName, "default");
        Device savedDeviceOnEdge = saveDeviceOnEdge(deviceName, "default");

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> restClient.getDeviceById(savedDeviceOnEdge.getId()).isPresent());

        // device on edge must be renamed
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> !edgeRestClient.getDeviceById(savedDeviceOnEdge.getId()).get().getName().equals(deviceName));

        edgeRestClient.deleteDevice(savedDeviceOnEdge.getId());
        restClient.deleteDevice(savedDeviceOnEdge.getId());
        restClient.deleteDevice(savedDeviceOnCloud.getId());

        log.info("Send device to cloud with name already exists on cloud tested successfully");
    }

    private void sendRelationToCloud() throws Exception {
        log.info("Testing relations from edge");

        Device savedDeviceOnEdge = saveDeviceOnEdge("Test Device 3", "default");
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> restClient.getDeviceById(savedDeviceOnEdge.getId()).isPresent());

        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(globalTestDevice.getId());
        relation.setTo(savedDeviceOnEdge.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        edgeRestClient.saveRelation(relation);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> restClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isPresent());

        edgeRestClient.deleteRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> restClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isEmpty());

        edgeRestClient.deleteDevice(savedDeviceOnEdge.getId());
        restClient.deleteDevice(savedDeviceOnEdge.getId());

        log.info("Relations from edge tested successfully");
    }

    private void sendAlarmToCloud() throws Exception {
        log.info("Testing alarms from edge");

        Alarm alarm = new Alarm();
        alarm.setOriginator(globalTestDevice.getId());
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarm.setType("alarm from edge");
        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm savedAlarm = edgeRestClient.saveAlarm(alarm);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> getAlarmForEntityFromCloud(globalTestDevice.getId()).isPresent());

        Assert.assertEquals("Alarm on edge and cloud have different types",
                "alarm from edge", getAlarmForEntityFromCloud(globalTestDevice.getId()).get().getType());

        edgeRestClient.ackAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    AlarmInfo alarmData = getAlarmForEntityFromCloud(globalTestDevice.getId()).get();
                    return alarmData.getAckTs() > 0;
                });

        edgeRestClient.clearAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    AlarmInfo alarmData = getAlarmForEntityFromCloud(globalTestDevice.getId()).get();
                    return alarmData.getClearTs() > 0;
                });

        edgeRestClient.deleteAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> getAlarmForEntityFromCloud(globalTestDevice.getId()).isEmpty());

        log.info("Alarms from edge tested successfully");
    }


//    private void sendDeviceRpcResponse() throws Exception {
//        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
//        DeviceRpcCallMsg.Builder deviceRpcCallResponseBuilder = DeviceRpcCallMsg.newBuilder();
//        deviceRpcCallResponseBuilder.setDeviceIdMSB(globalTestDevice.getUuidId().getMostSignificantBits());
//        deviceRpcCallResponseBuilder.setDeviceIdLSB(globalTestDevice.getUuidId().getLeastSignificantBits());
//        deviceRpcCallResponseBuilder.setOneway(true);
//        deviceRpcCallResponseBuilder.setRequestId(0);
//        deviceRpcCallResponseBuilder.setExpirationTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
//        RpcResponseMsg.Builder responseBuilder =
//                RpcResponseMsg.newBuilder().setResponse("{}");
//
//        deviceRpcCallResponseBuilder.setResponseMsg(responseBuilder.build());
//
//        uplinkMsgBuilder.addDeviceRpcCallMsg(deviceRpcCallResponseBuilder.build());
//
//        edgeImitator.expectResponsesAmount(1);
//        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
//        Assert.assertTrue(edgeImitator.waitForResponses());
//    }


    // Utility methods

    private Device saveDeviceOnEdge(String deviceName, String type) throws Exception {
        return saveDevice(deviceName, type, null, edgeRestClient);
    }

    private Device saveDeviceOnCloud(String deviceName, String type) throws Exception {
        return saveDevice(deviceName, type, null, restClient);
    }

    private Device saveDeviceOnCloud(String deviceName, String type, EntityGroupId entityGroupId) throws Exception {
        return saveDevice(deviceName, type, entityGroupId, restClient);
    }

    private Device saveDevice(String deviceName, String type, EntityGroupId entityGroupId, RestClient restClient) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType(type);
        Device savedDevice = restClient.saveDevice(device);
        if (entityGroupId != null) {
            restClient.addEntitiesToEntityGroup(entityGroupId, Arrays.asList(savedDevice.getId()));
        }
        return savedDevice;
    }

    private Asset saveAssetOnCloud(String assetName, String type, EntityGroupId entityGroupId) throws Exception {
        Asset asset = new Asset();
        asset.setName(assetName);
        asset.setType(type);
        Asset savedAsset = restClient.saveAsset(asset);
        if (entityGroupId != null) {
            restClient.addEntitiesToEntityGroup(entityGroupId, Arrays.asList(savedAsset.getId()));
        }
        return savedAsset;
    }

    private Dashboard saveDashboardOnCloud(String dashboardTitle, EntityGroupId entityGroupId) throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(dashboardTitle);
        Dashboard savedDashboard = restClient.saveDashboard(dashboard);
        if (entityGroupId != null) {
            restClient.addEntitiesToEntityGroup(entityGroupId, Arrays.asList(savedDashboard.getId()));
        }
        return savedDashboard;
    }

    private EntityView saveEntityViewOnCloud(String entityViewName, String type, DeviceId deviceId, EntityGroupId entityGroupId) throws Exception {
        EntityView entityView = new EntityView();
        entityView.setName("Edge EntityView 1");
        entityView.setType("test");
        entityView.setEntityId(deviceId);
        EntityView savedEntityView = restClient.saveEntityView(entityView);
        if (entityGroupId != null) {
            restClient.addEntitiesToEntityGroup(entityGroupId, Arrays.asList(savedEntityView.getId()));
        }
        return savedEntityView;
    }

}

