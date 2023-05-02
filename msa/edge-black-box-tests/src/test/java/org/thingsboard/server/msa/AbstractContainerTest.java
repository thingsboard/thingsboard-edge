/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.msa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractContainerTest {

    private static final String CUSTOM_DEVICE_PROFILE_NAME = "Custom Device Profile";

    protected static RestClient cloudRestClient = null;

    protected static RestClient edgeRestClient;

    protected static Edge edge;
    protected static String tbUrl;
    protected static String edgeUrl;

    @BeforeClass
    public static void before() throws Exception {
        if (cloudRestClient == null) {
            String tbHost = ContainerTestSuite.testContainer.getServiceHost("tb-monolith", 8080);
            Integer tbPort = ContainerTestSuite.testContainer.getServicePort("tb-monolith", 8080);
            tbUrl = "http://" + tbHost + ":" + tbPort;
            cloudRestClient = new RestClient(tbUrl);
            cloudRestClient.login("tenant@thingsboard.org", "tenant");

            String edgeHost = ContainerTestSuite.testContainer.getServiceHost("tb-edge", 8082);
            Integer edgePort = ContainerTestSuite.testContainer.getServicePort("tb-edge", 8082);
            edgeUrl = "http://" + edgeHost + ":" + edgePort;
            edgeRestClient = new RestClient(edgeUrl);

            updateRootRuleChain();
            updateEdgeRootRuleChain();

            edge = createEdge("test", "280629c7-f853-ee3d-01c0-fffbb6f2ef38", "g9ta4soeylw6smqkky8g");

            loginIntoEdgeWithRetries("tenant@thingsboard.org", "tenant");

            Optional<Tenant> tenant = edgeRestClient.getTenantById(edge.getTenantId());
            Assert.assertTrue(tenant.isPresent());
            Assert.assertEquals(edge.getTenantId(), tenant.get().getId());

            createCustomDeviceProfile(CUSTOM_DEVICE_PROFILE_NAME);

            // This is a starting point to start other tests
            verifyWidgetBundles();
        }
    }

    protected static void loginIntoEdgeWithRetries(String userName, String password) {
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(90, TimeUnit.SECONDS)
                .until(() -> {
                    boolean loginSuccessful = false;
                    try {
                        edgeRestClient.login(userName, password);
                        loginSuccessful = true;
                    } catch (Exception ignored) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ignored2) {
                        }
                    }
                    return loginSuccessful;
                });
    }

    private static void verifyWidgetBundles() {
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    try {
                        return edgeRestClient.getWidgetsBundles(new PageLink(100)).getTotalElements() == 15;
                    } catch (Throwable e) {
                        return false;
                    }
                });

        PageData<WidgetsBundle> pageData = edgeRestClient.getWidgetsBundles(new PageLink(100));

        for (String widgetsBundlesAlias : pageData.getData().stream().map(WidgetsBundle::getAlias).collect(Collectors.toList())) {
            Awaitility.await()
                    .pollInterval(1000, TimeUnit.MILLISECONDS)
                    .atMost(60, TimeUnit.SECONDS).
                    until(() -> {
                        try {
                            List<WidgetType> edgeBundleWidgetTypes = edgeRestClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
                            List<WidgetType> cloudBundleWidgetTypes = cloudRestClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
                            return cloudBundleWidgetTypes != null && edgeBundleWidgetTypes != null
                                    && edgeBundleWidgetTypes.size() == cloudBundleWidgetTypes.size();
                        } catch (Throwable e) {
                            return false;
                        }
                    });
            List<WidgetType> edgeBundleWidgetTypes = edgeRestClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
            List<WidgetType> cloudBundleWidgetTypes = cloudRestClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
            Assert.assertNotNull("edgeBundleWidgetTypes can't be null", edgeBundleWidgetTypes);
            Assert.assertNotNull("cloudBundleWidgetTypes can't be null", cloudBundleWidgetTypes);
        }
    }

    private static void updateRootRuleChain() throws IOException {
        // Modifications:
        // - add rule node 'script' to create RPC reply message
        // - add rule node 'rpc call reply' to send RPC reply
        // - add connection - from 'RPC from Device' to 'script'
        // - add connection - from 'script' to 'rpc call reply'
        updateRootRuleChain(RuleChainType.CORE, "Updated_RootRuleChainMetadata.json");
    }

    private static void updateEdgeRootRuleChain() throws IOException {
        // Modifications:
        // - add connection - from 'RPC from Device' to 'Push to cloud'
        updateRootRuleChain(RuleChainType.EDGE, "Updated_EdgeRootRuleChainMetadata.json");
    }

    private static void updateRootRuleChain(RuleChainType ruleChainType, String updatedRootRuleChainFileName) throws IOException {
        PageData<RuleChain> ruleChains = cloudRestClient.getRuleChains(ruleChainType, new PageLink(100));
        RuleChainId rootRuleChainId = null;
        for (RuleChain datum : ruleChains.getData()) {
            if (datum.isRoot()) {
                rootRuleChainId = datum.getId();
                break;
            }
        }
        Assert.assertNotNull(rootRuleChainId);
        JsonNode configuration = JacksonUtil.OBJECT_MAPPER.readTree(AbstractContainerTest.class.getClassLoader().getResourceAsStream(updatedRootRuleChainFileName));
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(rootRuleChainId);
        ruleChainMetaData.setFirstNodeIndex(configuration.get("firstNodeIndex").asInt());
        ruleChainMetaData.setNodes(Arrays.asList(JacksonUtil.OBJECT_MAPPER.treeToValue(configuration.get("nodes"), RuleNode[].class)));
        ruleChainMetaData.setConnections(Arrays.asList(JacksonUtil.OBJECT_MAPPER.treeToValue(configuration.get("connections"), NodeConnectionInfo[].class)));
        cloudRestClient.saveRuleChainMetaData(ruleChainMetaData);
    }

    protected static DeviceProfile createCustomDeviceProfile(String deviceProfileName,
                                                             DeviceProfileTransportConfiguration deviceProfileTransportConfiguration) {
        return createDeviceProfile(deviceProfileName, deviceProfileTransportConfiguration);
    }

    protected static DeviceProfile createCustomDeviceProfile(String deviceProfileName) {
        return createCustomDeviceProfile(deviceProfileName, null);
    }

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            log.info("=================================================");
            log.info("STARTING TEST: {}", description.getMethodName());
            log.info("=================================================");
        }

        /**
         * Invoked when a test succeeds
         */
        protected void succeeded(Description description) {
            log.info("=================================================");
            log.info("SUCCEEDED TEST: {}", description.getMethodName());
            log.info("=================================================");
        }

        /**
         * Invoked when a test fails
         */
        protected void failed(Throwable e, Description description) {
            log.info("=================================================");
            log.info("FAILED TEST: {}", description.getMethodName(), e);
            log.info("=================================================");
        }
    };

    protected static DeviceProfile createDeviceProfile(String name, DeviceProfileTransportConfiguration deviceProfileTransportConfiguration) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(name);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setImage("iVBORw0KGgoAAAANSUhEUgAAAQAAAAEABA");
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setDescription(null);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        deviceProfileData.setConfiguration(configuration);
        if (deviceProfileTransportConfiguration != null) {
            deviceProfileData.setTransportConfiguration(deviceProfileTransportConfiguration);
        } else {
            deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        }
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(null);
        deviceProfile.setDefaultQueueName("Main");
        extendDeviceProfileData(deviceProfile);
        return cloudRestClient.saveDeviceProfile(deviceProfile);
    }

    protected static void extendDeviceProfileData(DeviceProfile deviceProfile) {
        DeviceProfileData profileData = deviceProfile.getProfileData();
        List<DeviceProfileAlarm> alarms = new ArrayList<>();
        DeviceProfileAlarm deviceProfileAlarm = new DeviceProfileAlarm();
        deviceProfileAlarm.setAlarmType("High Temperature");
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setAlarmDetails("Alarm Details");
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(new SimpleAlarmConditionSpec());
        List<AlarmConditionFilter> condition = new ArrayList<>();
        AlarmConditionFilter alarmConditionFilter = new AlarmConditionFilter();
        alarmConditionFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        predicate.setValue(new FilterPredicateValue<>(55.0));
        alarmConditionFilter.setPredicate(predicate);
        alarmConditionFilter.setValueType(EntityKeyValueType.NUMERIC);
        condition.add(alarmConditionFilter);
        alarmCondition.setCondition(condition);
        alarmRule.setCondition(alarmCondition);
        deviceProfileAlarm.setClearRule(alarmRule);
        TreeMap<AlarmSeverity, AlarmRule> createRules = new TreeMap<>();
        createRules.put(AlarmSeverity.CRITICAL, alarmRule);
        deviceProfileAlarm.setCreateRules(createRules);
        alarms.add(deviceProfileAlarm);
        profileData.setAlarms(alarms);
        profileData.setProvisionConfiguration(new AllowCreateNewDevicesDeviceProfileProvisionConfiguration("123"));
    }

    private static HttpComponentsClientHttpRequestFactory getRequestFactoryForSelfSignedCert() throws Exception {
        SSLContextBuilder builder = SSLContexts.custom();
        builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
        SSLContext sslContext = builder.build();
        SSLConnectionSocketFactory sslSelfSigned = new SSLConnectionSocketFactory(sslContext, (s, sslSession) -> true);

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory>create()
                .register("https", sslSelfSigned)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    protected static Edge createEdge(String name, String routingKey, String secret) {
        Edge edge = new Edge();
        edge.setName(name + StringUtils.randomAlphanumeric(7));
        edge.setType("DEFAULT");
        edge.setRoutingKey(routingKey);
        edge.setSecret(secret);
        return cloudRestClient.saveEdge(edge);
    }

    protected Device saveDeviceOnEdge(String deviceName, String type) {
        return saveDevice(deviceName, type, edgeRestClient);
    }

    protected Device saveDeviceOnCloud(String deviceName, String type) {
        return saveDevice(deviceName, type, cloudRestClient);
    }

    private Device saveDevice(String deviceName, String type, RestClient restClient) {
        Device device = new Device();
        device.setName(deviceName);
        device.setType(type);
        return restClient.saveDevice(device);
    }

    protected Asset saveAndAssignAssetToEdge() {
        return saveAndAssignAssetToEdge("default");
    }

    protected Asset saveAndAssignAssetToEdge(String assetType) {
        Asset asset = saveAssetOnCloud(StringUtils.randomAlphanumeric(15), assetType);
        cloudRestClient.assignAssetToEdge(edge.getId(), asset.getId());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getAssetById(asset.getId()).isPresent());

        return asset;
    }

    private Asset saveAssetOnCloud(String assetName, String type) {
        Asset asset = new Asset();
        asset.setName(assetName);
        asset.setType(type);
        return cloudRestClient.saveAsset(asset);
    }

    protected Dashboard saveDashboardOnCloud(String dashboardTitle) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(dashboardTitle);
        return cloudRestClient.saveDashboard(dashboard);
    }

    protected void assertEntitiesByIdsAndType(List<EntityId> entityIds, EntityType entityType) {
        switch (entityType) {
            case DEVICE_PROFILE:
                assertDeviceProfiles(entityIds);
                break;
            case ASSET_PROFILE:
                assertAssetProfiles(entityIds);
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
            case OTA_PACKAGE:
                assertOtaPackages(entityIds);
                break;
            case QUEUE:
                assertQueues(entityIds);
                break;
        }
    }

    private void assertDeviceProfiles(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            DeviceProfileId deviceProfileId = new DeviceProfileId(entityId.getId());
            Optional<DeviceProfile> edgeDeviceProfile = edgeRestClient.getDeviceProfileById(deviceProfileId);
            Optional<DeviceProfile> cloudDeviceProfile = cloudRestClient.getDeviceProfileById(deviceProfileId);
            DeviceProfile expected = edgeDeviceProfile.get();
            DeviceProfile actual = cloudDeviceProfile.get();
            Assert.assertEquals(expected.getDefaultRuleChainId(), actual.getDefaultEdgeRuleChainId());
            expected.setDefaultRuleChainId(null);
            actual.setDefaultEdgeRuleChainId(null);
            actual.setDefaultRuleChainId(null);
            Assert.assertEquals("Device profiles on cloud and edge are different", expected, actual);
        }
    }

    private void assertAssetProfiles(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            AssetProfileId assetProfileId = new AssetProfileId(entityId.getId());
            Optional<AssetProfile> edgeAssetProfile = edgeRestClient.getAssetProfileById(assetProfileId);
            Optional<AssetProfile> cloudAssetProfile = cloudRestClient.getAssetProfileById(assetProfileId);
            AssetProfile expected = edgeAssetProfile.get();
            AssetProfile actual = cloudAssetProfile.get();
            Assert.assertEquals(expected.getDefaultRuleChainId(), actual.getDefaultEdgeRuleChainId());
            expected.setDefaultRuleChainId(null);
            actual.setDefaultEdgeRuleChainId(null);
            Assert.assertEquals("Asset profiles on cloud and edge are different", expected, actual);
        }
    }

    private void assertOtaPackages(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            OtaPackageId otaPackageId = new OtaPackageId(entityId.getId());
            OtaPackage edgeOtaPackage = edgeRestClient.getOtaPackageById(otaPackageId);
            OtaPackage cloudOtaPackage = cloudRestClient.getOtaPackageById(otaPackageId);
            Assert.assertEquals("Ota packages on cloud and edge are different", edgeOtaPackage, cloudOtaPackage);
        }
    }

    private void assertQueues(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            QueueId queueId = new QueueId(entityId.getId());
            Queue edgeQueue = edgeRestClient.getQueueById(queueId);
            Queue cloudQueue = cloudRestClient.getQueueById(queueId);
            Assert.assertEquals("Queues on cloud and edge are different", edgeQueue, cloudQueue);
        }
    }

    private void assertRuleChains(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            RuleChainId ruleChainId = new RuleChainId(entityId.getId());
            Optional<RuleChain> edgeRuleChain = edgeRestClient.getRuleChainById(ruleChainId);
            Optional<RuleChain> cloudRuleChain = cloudRestClient.getRuleChainById(ruleChainId);
            RuleChain expected = edgeRuleChain.get();
            RuleChain actual = cloudRuleChain.get();
            Assert.assertEquals("Edge rule chain type is incorrect", RuleChainType.CORE, expected.getType());
            Assert.assertEquals("Cloud rule chain type is incorrect", RuleChainType.EDGE, actual.getType());
            expected.setType(null);
            actual.setType(null);
            Assert.assertEquals("Rule chains on cloud and edge are different (except type)", expected, actual);

            Awaitility.await()
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .atMost(30, TimeUnit.SECONDS).
                    until(() -> {
                        Optional<RuleChainMetaData> edgeRuleChainMetaData = edgeRestClient.getRuleChainMetaData(ruleChainId);
                        Optional<RuleChainMetaData> cloudRuleChainMetaData = cloudRestClient.getRuleChainMetaData(ruleChainId);
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
            if (!expectedNode.equals(actualNode)) {
                return false;
            }
        }
        return true;
    }

    private void assertWidgetsBundles(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            WidgetsBundleId widgetsBundleId = new WidgetsBundleId(entityId.getId());
            Optional<WidgetsBundle> edgeWidgetsBundle = edgeRestClient.getWidgetsBundleById(widgetsBundleId);
            Optional<WidgetsBundle> cloudWidgetsBundle = cloudRestClient.getWidgetsBundleById(widgetsBundleId);
            WidgetsBundle expected = edgeWidgetsBundle.get();
            WidgetsBundle actual = cloudWidgetsBundle.get();
            Assert.assertEquals("Widgets bundles on cloud and edge are different", expected, actual);
        }
    }

    private void assertWidgetTypes(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            WidgetTypeId widgetTypeId = new WidgetTypeId(entityId.getId());
            Optional<WidgetTypeDetails> edgeWidgetsBundle = edgeRestClient.getWidgetTypeById(widgetTypeId);
            Optional<WidgetTypeDetails> cloudWidgetsBundle = cloudRestClient.getWidgetTypeById(widgetTypeId);
            WidgetTypeDetails expected = edgeWidgetsBundle.get();
            WidgetTypeDetails actual = cloudWidgetsBundle.get();
            Assert.assertEquals("Widget types on cloud and edge are different", expected, actual);
        }
    }

    private void assertDevices(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            DeviceId deviceId = new DeviceId(entityId.getId());
            Optional<Device> edgeDevice = edgeRestClient.getDeviceById(deviceId);
            Optional<Device> cloudDevice = cloudRestClient.getDeviceById(deviceId);
            Device expected = edgeDevice.get();
            Device actual = cloudDevice.get();
            Assert.assertEquals("Devices on cloud and edge are different", expected, actual);
        }
    }

    private void assertAssets(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            AssetId assetId = new AssetId(entityId.getId());
            Optional<Asset> edgeAsset = edgeRestClient.getAssetById(assetId);
            Optional<Asset> cloudAsset = cloudRestClient.getAssetById(assetId);
            Asset expected = edgeAsset.get();
            Asset actual = cloudAsset.get();
            Assert.assertEquals("Assets on cloud and edge are different", expected, actual);
        }
    }

    private void assertEntityViews(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            EntityViewId entityViewId = new EntityViewId(entityId.getId());
            Optional<EntityView> edgeEntityView = edgeRestClient.getEntityViewById(entityViewId);
            Optional<EntityView> cloudEntityView = cloudRestClient.getEntityViewById(entityViewId);
            EntityView expected = edgeEntityView.get();
            EntityView actual = cloudEntityView.get();
            Assert.assertEquals("Entity Views on cloud and edge are different", expected, actual);
        }
    }

    private void assertDashboards(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            DashboardId dashboardId = new DashboardId(entityId.getId());
            Optional<Dashboard> edgeDashboard = edgeRestClient.getDashboardById(dashboardId);
            Optional<Dashboard> cloudDashboard = cloudRestClient.getDashboardById(dashboardId);
            Dashboard expected = edgeDashboard.get();
            Dashboard actual = cloudDashboard.get();
            Assert.assertEquals("Dashboards on cloud and edge are different", expected, actual);
        }
    }

    private void assertUsers(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            UserId userId = new UserId(entityId.getId());
            Optional<User> edgeUser = edgeRestClient.getUserById(userId);
            Optional<User> cloudUser = cloudRestClient.getUserById(userId);
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

    protected Device saveAndAssignDeviceToEdge() {
        return saveAndAssignDeviceToEdge("default");
    }

    protected Device saveAndAssignDeviceToEdge(String deviceType) {
        Device device = saveDeviceOnCloud(StringUtils.randomAlphanumeric(15), deviceType);
        cloudRestClient.assignDeviceToEdge(edge.getId(), device.getId());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDeviceById(device.getId()).isPresent());

        return device;
    }

    protected List<AttributeKvEntry> sendAttributesUpdated(RestClient sourceRestClient, RestClient targetRestClient,
                                                           JsonObject attributesPayload, List<String> keys, String scope) throws Exception {

        Device device = saveAndAssignDeviceToEdge();

        sourceRestClient.saveDeviceAttributes(device.getId(), scope, JacksonUtil.OBJECT_MAPPER.readTree(attributesPayload.toString()));

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> targetRestClient.getAttributesByScope(device.getId(), scope, keys).size() == keys.size());

        List<AttributeKvEntry> attributeKvEntries =
                targetRestClient.getAttributesByScope(device.getId(), scope, keys);

        sourceRestClient.deleteEntityAttributes(device.getId(), scope, keys);

        verifyDeviceIsActive(targetRestClient, device.getId());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());

        return attributeKvEntries;
    }

    protected void verifyDeviceIsActive(RestClient restClient, DeviceId deviceId) {
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<AttributeKvEntry> attributeKvEntries =
                            restClient.getAttributesByScope(deviceId, DataConstants.SERVER_SCOPE, Collections.singletonList("active"));
                    if (attributeKvEntries.size() != 1) {
                        return false;
                    }
                    AttributeKvEntry activeAttributeKv = attributeKvEntries.get(0);
                    return activeAttributeKv.getValueAsString().equals("true");
                });
    }

    protected boolean verifyAttributeOnEdge(EntityId entityId, String scope, String key, String expectedValue) {
        return verifyAttribute(entityId, scope, key, expectedValue, edgeRestClient);
    }

    protected boolean verifyAttributeOnCloud(EntityId entityId, String scope, String key, String expectedValue) {
        return verifyAttribute(entityId, scope, key, expectedValue, cloudRestClient);
    }

    private boolean verifyAttribute(EntityId entityId, String scope, String key, String expectedValue, RestClient restClient) {
        List<AttributeKvEntry> attributesByScope = restClient.getAttributesByScope(entityId, scope, Arrays.asList(key));
        if (attributesByScope.isEmpty()) {
            return false;
        }
        AttributeKvEntry attributeKvEntry = attributesByScope.get(0);
        return attributeKvEntry.getValueAsString().equals(expectedValue);
    }

    protected void assignEdgeToCustomerAndValidateAssignmentOnCloud(Customer savedCustomer) {
        cloudRestClient.assignEdgeToCustomer(savedCustomer.getId(), edge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> savedCustomer.getId().equals(edgeRestClient.getEdgeById(edge.getId()).get().getCustomerId()));
    }

    protected RuleChainId createRuleChainAndAssignToEdge(String ruleChainName) throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(ruleChainName);
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = cloudRestClient.saveRuleChain(ruleChain);
        createRuleChainMetadata(savedRuleChain);

        cloudRestClient.assignRuleChainToEdge(edge.getId(), savedRuleChain.getId());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRuleChainById(savedRuleChain.getId()).isPresent());

        return savedRuleChain.getId();
    }

    private void createRuleChainMetadata(RuleChain ruleChain) throws Exception {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType("type1");
        ruleNode1.setConfiguration(JacksonUtil.OBJECT_MAPPER.readTree("\"key1\": \"val1\""));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType("type2");
        ruleNode2.setConfiguration(JacksonUtil.OBJECT_MAPPER.readTree("\"key2\": \"val2\""));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType("type3");
        ruleNode3.setConfiguration(JacksonUtil.OBJECT_MAPPER.readTree("\"key3\": \"val3\""));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0, 1, "success");
        ruleChainMetaData.addConnectionInfo(0, 2, "fail");
        ruleChainMetaData.addConnectionInfo(1, 2, "success");

        // ruleChainMetaData.addRuleChainConnectionInfo(2, edge.getRootRuleChainId(), "success", JacksonUtil.OBJECT_MAPPER.createObjectNode());

        cloudRestClient.saveRuleChainMetaData(ruleChainMetaData);
    }

    protected void unAssignFromEdgeAndDeleteRuleChain(RuleChainId ruleChainId) {
        // unassign rule chain from edge
        cloudRestClient.unassignRuleChainFromEdge(edge.getId(), ruleChainId);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRuleChainById(ruleChainId).isEmpty());

        // delete rule chain
        cloudRestClient.deleteRuleChain(ruleChainId);
    }

    protected DashboardId createDashboardAndAssignToEdge(String dashboardName) {
        Dashboard dashboard = saveDashboardOnCloud(dashboardName);
        cloudRestClient.assignDashboardToEdge(edge.getId(), dashboard.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(dashboard.getId()).isPresent());
        return dashboard.getId();
    }

    protected void unAssignFromEdgeAndDeleteDashboard(DashboardId dashboardId) {
        cloudRestClient.unassignDashboardFromEdge(edge.getId(), dashboardId);
        cloudRestClient.deleteDashboard(dashboardId);
    }

    protected OtaPackageId createOtaPackageInfo(DeviceProfileId deviceProfileId, OtaPackageType otaPackageType) throws Exception {
        OtaPackageInfo otaPackageInfo = new OtaPackageInfo();
        otaPackageInfo.setDeviceProfileId(deviceProfileId);
        otaPackageInfo.setType(otaPackageType);
        otaPackageInfo.setTitle("My " + otaPackageType + " #2");
        otaPackageInfo.setVersion("v2.0");
        otaPackageInfo.setTag("My " + otaPackageType + " #2 v2.0");
        otaPackageInfo.setHasData(false);
        OtaPackageInfo savedOtaPackageInfo = cloudRestClient.saveOtaPackageInfo(otaPackageInfo, false);

        cloudRestClient.saveOtaPackageData(savedOtaPackageInfo.getId(),
                null, ChecksumAlgorithm.SHA256, "firmware.bin", new byte[]{1, 3, 5});

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<OtaPackageInfo> otaPackages = edgeRestClient.getOtaPackages(new PageLink(100));
                    if (otaPackages.getData().isEmpty()) {
                        return false;
                    }
                    return otaPackages.getData().stream().map(OtaPackageInfo::getId).anyMatch(savedOtaPackageInfo.getId()::equals);
                });

        return savedOtaPackageInfo.getId();
    }

    protected Customer findPublicCustomer() {
        PageData<Customer> customers = cloudRestClient.getCustomers(new PageLink(100));
        return customers.getData().stream().filter(Customer::isPublic).findFirst().get();
    }
}
