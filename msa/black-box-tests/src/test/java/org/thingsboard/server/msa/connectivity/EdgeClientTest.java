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

//        testTimeseries();
//
//        testAttributes();

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
        verifyAdminSettings();
        verifyRuleChains();
        verifyEntityGroups(EntityType.DEVICE, 1);
        verifyEntityGroups(EntityType.ASSET, 1);
        verifyEntityGroups(EntityType.ENTITY_VIEW, 1);
        verifyEntityGroups(EntityType.DASHBOARD, 1);
        verifyEntityGroups(EntityType.USER, 3);
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
        Optional<WhiteLabelingParams> edgeWhiteLabelParams = edgeRestClient.getCurrentWhiteLabelParams();
        Assert.assertTrue("White Labeling is not available on edge", edgeWhiteLabelParams.isPresent());
        Optional<WhiteLabelingParams> cloudWhiteLabelParams = restClient.getCurrentWhiteLabelParams();
        Assert.assertTrue("White Labeling is not available on cloud", cloudWhiteLabelParams.isPresent());
        Assert.assertEquals("White Labeling on cloud and edge are different", edgeWhiteLabelParams.get(), cloudWhiteLabelParams.get());

        Optional<LoginWhiteLabelingParams> edgeLoginWhiteLabelParams = edgeRestClient.getCurrentLoginWhiteLabelParams();
        Assert.assertTrue("Login White Labeling is not available on edge", edgeLoginWhiteLabelParams.isPresent());
        Optional<LoginWhiteLabelingParams> cloudLoginWhiteLabelParams = restClient.getCurrentLoginWhiteLabelParams();
        Assert.assertTrue("Login White Labeling is not available on cloud", cloudLoginWhiteLabelParams.isPresent());
        Assert.assertEquals("Login White Labeling on cloud and edge are different", edgeLoginWhiteLabelParams.get(), cloudLoginWhiteLabelParams.get());

        Optional<CustomTranslation> edgeCustomTranslation = edgeRestClient.getCustomTranslation();
        Assert.assertTrue("Custom Translation is not available on edge", edgeCustomTranslation.isPresent());
        Optional<CustomTranslation> cloudCustomTranslation = restClient.getCustomTranslation();
        Assert.assertTrue("Custom Translation is not available on cloud", cloudCustomTranslation.isPresent());
        Assert.assertEquals("Custom Translation on cloud and edge are different", edgeCustomTranslation.get(), cloudCustomTranslation.get());
    }

    private void verifyAdminSettings() {
        verifyAdminSettingsByKey("general");
        verifyAdminSettingsByKey("mail");
        verifyAdminSettingsByKey("mailTemplates");
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

            Optional<RuleChainMetaData> edgeRuleChainMetaData = edgeRestClient.getRuleChainMetaData(ruleChainId);
            Optional<RuleChainMetaData> cloudRuleChainMetaData = restClient.getRuleChainMetaData(ruleChainId);
            Assert.assertTrue(edgeRuleChainMetaData.isPresent());
            Assert.assertTrue(cloudRuleChainMetaData.isPresent());
            RuleChainMetaData expectedMetadata = edgeRuleChainMetaData.get();
            RuleChainMetaData actualMetadata = cloudRuleChainMetaData.get();
            // TODO: voba - add check
            // Assert.assertEquals("Rule chains metadata on cloud and edge are different (except type)", expectedMetadata, actualMetadata);
        }
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

        restClient.assignEntityGroupToEdge(edge.getId(), savedDeviceEntityGroup.getId(), EntityType.DEVICE);

        verifyEntityGroups(EntityType.DEVICE, 2);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDeviceById(globalTestDevice.getId()).isPresent());

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
//
//    private void testDeviceEntityGroupRequestMsg(long msbId, long lsbId, DeviceId expectedDeviceId) throws Exception {
//        EntityGroupRequestMsg.Builder deviceEntitiesGroupRequestMsgBuilder = EntityGroupRequestMsg.newBuilder()
//                .setEntityGroupIdMSB(msbId)
//                .setEntityGroupIdLSB(lsbId)
//                .setType(EntityType.DEVICE.name());
//        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
//                .addEntityGroupEntitiesRequestMsg(deviceEntitiesGroupRequestMsgBuilder.build());
//
//        edgeImitator.expectResponsesAmount(1);
//        edgeImitator.expectMessageAmount(1);
//        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
//        Assert.assertTrue(edgeImitator.waitForResponses());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
//        DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
//        DeviceId receivedDeviceId =
//                new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
//        Assert.assertEquals(expectedDeviceId, receivedDeviceId);
//
//
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
//
//    private void testAssetEntityGroupRequestMsg(long msbId, long lsbId, AssetId expectedAssetId) throws Exception {
//        EntityGroupRequestMsg.Builder entitiesGroupRequestMsgBuilder = EntityGroupRequestMsg.newBuilder()
//                .setEntityGroupIdMSB(msbId)
//                .setEntityGroupIdLSB(lsbId)
//                .setType(EntityType.ASSET.name());
//
//        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
//                .addEntityGroupEntitiesRequestMsg(entitiesGroupRequestMsgBuilder.build());
//
//        edgeImitator.expectResponsesAmount(1);
//        edgeImitator.expectMessageAmount(1);
//        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
//        Assert.assertTrue(edgeImitator.waitForResponses());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
//        AssetUpdateMsg assetUpdateMsg = (AssetUpdateMsg) latestMessage;
//        AssetId receivedAssetId =
//                new AssetId(new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB()));
//        Assert.assertEquals(expectedAssetId, receivedAssetId);
//
//    }
//
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
//
//    private void testDashboardEntityGroupRequestMsg(long msbId, long lsbId, DashboardId expectedDashboardId) throws Exception {
//        EntityGroupRequestMsg.Builder entitiesGroupRequestMsgBuilder = EntityGroupRequestMsg.newBuilder()
//                .setEntityGroupIdMSB(msbId)
//                .setEntityGroupIdLSB(lsbId)
//                .setType(EntityType.DASHBOARD.name());
//
//        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
//                .addEntityGroupEntitiesRequestMsg(entitiesGroupRequestMsgBuilder.build());
//
//        edgeImitator.expectResponsesAmount(1);
//        edgeImitator.expectMessageAmount(1);
//        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
//        Assert.assertTrue(edgeImitator.waitForResponses());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
//        DashboardUpdateMsg dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
//        DashboardId receivedDashboardId =
//                new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
//        Assert.assertEquals(expectedDashboardId, receivedDashboardId);
//
//    }

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

//    private void testTimeseries() throws Exception {
//        log.info("Testing timeseries");
//
//        String timeseriesData = "{\"data\":{\"temperature\":25},\"ts\":" + System.currentTimeMillis() + "}";
//        JsonNode timeseriesEntityData = mapper.readTree(timeseriesData);
//
//
//        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED, globalTestDevice.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
//        edgeEventService.saveAsync(edgeEvent);
//        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof EntityDataProto);
//        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
//        Assert.assertEquals(latestEntityDataMsg.getEntityIdMSB(), globalTestDevice.getUuidId().getMostSignificantBits());
//        Assert.assertEquals(latestEntityDataMsg.getEntityIdLSB(), globalTestDevice.getUuidId().getLeastSignificantBits());
//        Assert.assertEquals(latestEntityDataMsg.getEntityType(), globalTestDevice.getId().getEntityType().name());
//        Assert.assertTrue(latestEntityDataMsg.hasPostTelemetryMsg());
//
//        TransportProtos.PostTelemetryMsg postTelemetryMsg = latestEntityDataMsg.getPostTelemetryMsg();
//        Assert.assertEquals(1, postTelemetryMsg.getTsKvListCount());
//        TransportProtos.TsKvListProto tsKvListProto = postTelemetryMsg.getTsKvList(0);
//        Assert.assertEquals(timeseriesEntityData.get("ts").asLong(), tsKvListProto.getTs());
//        Assert.assertEquals(1, tsKvListProto.getKvCount());
//        TransportProtos.KeyValueProto keyValueProto = tsKvListProto.getKv(0);
//        Assert.assertEquals("temperature", keyValueProto.getKey());
//        Assert.assertEquals(25, keyValueProto.getLongV());
//        log.info("Timeseries tested successfully");
//    }
//
//    private void testAttributes() throws Exception {
//        log.info("Testing attributes");
//
//        testAttributesUpdatedMsg(globalTestDevice);
//        testPostAttributesMsg(globalTestDevice);
//        testAttributesDeleteMsg(globalTestDevice);
//
//        log.info("Attributes tested successfully");
//    }
//
//    private void testAttributesUpdatedMsg(Device device) throws JsonProcessingException, InterruptedException {
//        String attributesData = "{\"scope\":\"SERVER_SCOPE\",\"kv\":{\"key1\":\"value1\"}}";
//        JsonNode attributesEntityData = mapper.readTree(attributesData);
//        EdgeEvent edgeEvent1 = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.ATTRIBUTES_UPDATED, device.getId().getId(), EdgeEventType.DEVICE, attributesEntityData);
//        edgeImitator.expectMessageAmount(1);
//        edgeEventService.saveAsync(edgeEvent1);
//        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof EntityDataProto);
//        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
//        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
//        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
//        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
//        Assert.assertEquals("SERVER_SCOPE", latestEntityDataMsg.getPostAttributeScope());
//        Assert.assertTrue(latestEntityDataMsg.hasAttributesUpdatedMsg());
//
//        TransportProtos.PostAttributeMsg attributesUpdatedMsg = latestEntityDataMsg.getAttributesUpdatedMsg();
//        Assert.assertEquals(1, attributesUpdatedMsg.getKvCount());
//        TransportProtos.KeyValueProto keyValueProto = attributesUpdatedMsg.getKv(0);
//        Assert.assertEquals("key1", keyValueProto.getKey());
//        Assert.assertEquals("value1", keyValueProto.getStringV());
//    }
//
//    private void testPostAttributesMsg(Device device) throws JsonProcessingException, InterruptedException {
//        String postAttributesData = "{\"scope\":\"SERVER_SCOPE\",\"kv\":{\"key2\":\"value2\"}}";
//        JsonNode postAttributesEntityData = mapper.readTree(postAttributesData);
//        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.POST_ATTRIBUTES, device.getId().getId(), EdgeEventType.DEVICE, postAttributesEntityData);
//        edgeImitator.expectMessageAmount(1);
//        edgeEventService.saveAsync(edgeEvent);
//        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof EntityDataProto);
//        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
//        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
//        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
//        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
//        Assert.assertEquals("SERVER_SCOPE", latestEntityDataMsg.getPostAttributeScope());
//        Assert.assertTrue(latestEntityDataMsg.hasPostAttributesMsg());
//
//        TransportProtos.PostAttributeMsg postAttributesMsg = latestEntityDataMsg.getPostAttributesMsg();
//        Assert.assertEquals(1, postAttributesMsg.getKvCount());
//        TransportProtos.KeyValueProto keyValueProto = postAttributesMsg.getKv(0);
//        Assert.assertEquals("key2", keyValueProto.getKey());
//        Assert.assertEquals("value2", keyValueProto.getStringV());
//    }
//
//    private void testAttributesDeleteMsg(Device device) throws JsonProcessingException, InterruptedException {
//        String deleteAttributesData = "{\"scope\":\"SERVER_SCOPE\",\"keys\":[\"key1\",\"key2\"]}";
//        JsonNode deleteAttributesEntityData = mapper.readTree(deleteAttributesData);
//        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.ATTRIBUTES_DELETED, device.getId().getId(), EdgeEventType.DEVICE, deleteAttributesEntityData);
//        edgeImitator.expectMessageAmount(1);
//        edgeEventService.saveAsync(edgeEvent);
//        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof EntityDataProto);
//        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
//        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
//        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
//        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
//
//        Assert.assertTrue(latestEntityDataMsg.hasAttributeDeleteMsg());
//
//        AttributeDeleteMsg attributeDeleteMsg = latestEntityDataMsg.getAttributeDeleteMsg();
//        Assert.assertEquals(attributeDeleteMsg.getScope(), deleteAttributesEntityData.get("scope").asText());
//
//        Assert.assertEquals(2, attributeDeleteMsg.getAttributeNamesCount());
//        Assert.assertEquals("key1", attributeDeleteMsg.getAttributeNames(0));
//        Assert.assertEquals("key2", attributeDeleteMsg.getAttributeNames(1));
//    }
//
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
        sendTelemetryToCloud();
//        sendRuleChainMetadataRequest();
//        // TODO: implement
//        // sendUserCredentialsRequest();
//        sendDeviceRpcResponse();
//        sendDeviceCredentialsUpdate();
//        sendAttributesRequest();
//        log.info("Messages were sent successfully");
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

    private void sendTelemetryToCloud() throws Exception {
        DeviceCredentials deviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(globalTestDevice.getId()).get();
        String accessToken = deviceCredentials.getCredentialsId();

        ResponseEntity deviceTelemetryResponse = edgeRestClient.getRestTemplate()
                .postForEntity(edgeUrl + "/api/v1/{credentialsId}/telemetry",
                        mapper.readTree(createPayload().toString()),
                        ResponseEntity.class,
                        accessToken);
        Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());

        ResponseEntity deviceClientsAttributes = edgeRestClient.getRestTemplate()
                .postForEntity(edgeUrl + "/api/v1/" + accessToken + "/attributes/", mapper.readTree(createPayload().toString()),
                        ResponseEntity.class,
                        accessToken);
        Assert.assertTrue(deviceClientsAttributes.getStatusCode().is2xxSuccessful());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<TsKvEntry> attributeKvEntries =
                            restClient.getLatestTimeseries(globalTestDevice.getId(), Arrays.asList("stringKey", "booleanKey", "doubleKey", "longKey"));
                    return attributeKvEntries.size() == 4;
                });

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<AttributeKvEntry> attributeKvEntries =
                            restClient.getAttributeKvEntries(globalTestDevice.getId(), Arrays.asList("stringKey", "booleanKey", "doubleKey", "longKey"));
                    return attributeKvEntries.size() == 4;
                });
    }

//    private void sendRuleChainMetadataRequest() throws Exception {
//        RuleChainId edgeRootRuleChainId = edge.getRootRuleChainId();
//
//        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
//        RuleChainMetadataRequestMsg.Builder ruleChainMetadataRequestMsgBuilder = RuleChainMetadataRequestMsg.newBuilder();
//        ruleChainMetadataRequestMsgBuilder.setRuleChainIdMSB(edgeRootRuleChainId.getId().getMostSignificantBits());
//        ruleChainMetadataRequestMsgBuilder.setRuleChainIdLSB(edgeRootRuleChainId.getId().getLeastSignificantBits());
//        uplinkMsgBuilder.addRuleChainMetadataRequestMsg(ruleChainMetadataRequestMsgBuilder.build());
//
//
//        edgeImitator.expectResponsesAmount(1);
//        edgeImitator.expectMessageAmount(1);
//        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
//        Assert.assertTrue(edgeImitator.waitForResponses());
//        Assert.assertTrue(edgeImitator.waitForMessages());;
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof RuleChainMetadataUpdateMsg);
//        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = (RuleChainMetadataUpdateMsg) latestMessage;
//        Assert.assertEquals(ruleChainMetadataUpdateMsg.getRuleChainIdMSB(), edgeRootRuleChainId.getId().getMostSignificantBits());
//        Assert.assertEquals(ruleChainMetadataUpdateMsg.getRuleChainIdLSB(), edgeRootRuleChainId.getId().getLeastSignificantBits());
//
//    }
//
//    private void sendUserCredentialsRequest() throws Exception {
//        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
//        UserCredentialsRequestMsg.Builder userCredentialsRequestMsgBuilder = UserCredentialsRequestMsg.newBuilder();
//        userCredentialsRequestMsgBuilder.setUserIdMSB(tenantAdmin.getId().getId().getMostSignificantBits());
//        userCredentialsRequestMsgBuilder.setUserIdLSB(tenantAdmin.getId().getId().getLeastSignificantBits());
//        uplinkMsgBuilder.addUserCredentialsRequestMsg(userCredentialsRequestMsgBuilder.build());
//
//
//        edgeImitator.expectResponsesAmount(1);
//        edgeImitator.expectMessageAmount(1);
//        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
//        Assert.assertTrue(edgeImitator.waitForResponses());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
//        UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
//        Assert.assertEquals(userCredentialsUpdateMsg.getUserIdMSB(), tenantAdmin.getId().getId().getMostSignificantBits());
//        Assert.assertEquals(userCredentialsUpdateMsg.getUserIdLSB(), tenantAdmin.getId().getId().getLeastSignificantBits());
//
//    }
//
//    private void sendDeviceCredentialsRequest() throws Exception {
//        DeviceCredentials deviceCredentials = doGet("/api/device/" + globalTestDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
//
//        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
//        DeviceCredentialsRequestMsg.Builder deviceCredentialsRequestMsgBuilder = DeviceCredentialsRequestMsg.newBuilder();
//        deviceCredentialsRequestMsgBuilder.setDeviceIdMSB(globalTestDevice.getUuidId().getMostSignificantBits());
//        deviceCredentialsRequestMsgBuilder.setDeviceIdLSB(globalTestDevice.getUuidId().getLeastSignificantBits());
//        uplinkMsgBuilder.addDeviceCredentialsRequestMsg(deviceCredentialsRequestMsgBuilder.build());
//
//
//        edgeImitator.expectResponsesAmount(1);
//        edgeImitator.expectMessageAmount(1);
//        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
//        Assert.assertTrue(edgeImitator.waitForResponses());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof DeviceCredentialsUpdateMsg);
//        DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = (DeviceCredentialsUpdateMsg) latestMessage;
//        Assert.assertEquals(deviceCredentialsUpdateMsg.getDeviceIdMSB(), globalTestDevice.getUuidId().getMostSignificantBits());
//        Assert.assertEquals(deviceCredentialsUpdateMsg.getDeviceIdLSB(), globalTestDevice.getUuidId().getLeastSignificantBits());
//        Assert.assertEquals(deviceCredentialsUpdateMsg.getCredentialsType(), deviceCredentials.getCredentialsType().name());
//        Assert.assertEquals(deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentials.getCredentialsId());
//    }
//
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
//
//    private void sendDeviceCredentialsUpdate() throws Exception {
//        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
//        DeviceCredentialsUpdateMsg.Builder deviceCredentialsUpdateMsgBuilder = DeviceCredentialsUpdateMsg.newBuilder();
//        deviceCredentialsUpdateMsgBuilder.setDeviceIdMSB(globalTestDevice.getUuidId().getMostSignificantBits());
//        deviceCredentialsUpdateMsgBuilder.setDeviceIdLSB(globalTestDevice.getUuidId().getLeastSignificantBits());
//        deviceCredentialsUpdateMsgBuilder.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN.name());
//        deviceCredentialsUpdateMsgBuilder.setCredentialsId("NEW_TOKEN");
//        uplinkMsgBuilder.addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsgBuilder.build());
//
//
//        edgeImitator.expectResponsesAmount(1);
//        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
//        Assert.assertTrue(edgeImitator.waitForResponses());
//    }
//
//    private void sendAttributesRequest() throws Exception {
//        sendAttributesRequest(globalTestDevice, DataConstants.SERVER_SCOPE, "{\"key1\":\"value1\"}", "key1", "value1");
//        sendAttributesRequest(globalTestDevice, DataConstants.SHARED_SCOPE, "{\"key2\":\"value2\"}", "key2", "value2");
//    }
//
//    private void sendAttributesRequest(Device device, String scope, String attributesDataStr, String expectedKey, String expectedValue) throws Exception {
//        JsonNode attributesData = mapper.readTree(attributesDataStr);
//
//        doPost("/api/plugins/telemetry/DEVICE/" + device.getId().getId().toString() + "/attributes/" + scope,
//                attributesData);
//
//        // Wait before device attributes saved to database before requesting them from edge
//        // queue used to save attributes to database
//        Thread.sleep(500);
//
//        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
//        AttributesRequestMsg.Builder attributesRequestMsgBuilder = AttributesRequestMsg.newBuilder();
//        attributesRequestMsgBuilder.setEntityIdMSB(device.getUuidId().getMostSignificantBits());
//        attributesRequestMsgBuilder.setEntityIdLSB(device.getUuidId().getLeastSignificantBits());
//        attributesRequestMsgBuilder.setEntityType(EntityType.DEVICE.name());
//        attributesRequestMsgBuilder.setScope(scope);
//        uplinkMsgBuilder.addAttributesRequestMsg(attributesRequestMsgBuilder.build());
//
//        edgeImitator.expectResponsesAmount(1);
//        edgeImitator.expectMessageAmount(1);
//        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
//        Assert.assertTrue(edgeImitator.waitForResponses());
//        Assert.assertTrue(edgeImitator.waitForMessages());
//
//        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
//        Assert.assertTrue(latestMessage instanceof EntityDataProto);
//        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
//        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
//        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
//        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
//        Assert.assertEquals(scope, latestEntityDataMsg.getPostAttributeScope());
//        Assert.assertTrue(latestEntityDataMsg.hasAttributesUpdatedMsg());
//
//        TransportProtos.PostAttributeMsg attributesUpdatedMsg = latestEntityDataMsg.getAttributesUpdatedMsg();
//        Assert.assertEquals(1, attributesUpdatedMsg.getKvCount());
//        TransportProtos.KeyValueProto keyValueProto = attributesUpdatedMsg.getKv(0);
//        Assert.assertEquals(expectedKey, keyValueProto.getKey());
//        Assert.assertEquals(expectedValue, keyValueProto.getStringV());
//    }
//
//    // Utility methods
//
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
//
//    private EdgeEvent constructEdgeEvent(TenantId tenantId, EdgeId edgeId, EdgeEventActionType edgeEventAction, UUID entityId, EdgeEventType edgeEventType, JsonNode entityBody) {
//        EdgeEvent edgeEvent = new EdgeEvent();
//        edgeEvent.setEdgeId(edgeId);
//        edgeEvent.setTenantId(tenantId);
//        edgeEvent.setAction(edgeEventAction);
//        edgeEvent.setEntityId(entityId);
//        edgeEvent.setType(edgeEventType);
//        edgeEvent.setBody(entityBody);
//        return edgeEvent;
//    }

}

