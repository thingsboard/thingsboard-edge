/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
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
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractContainerTest {

    private static final String CUSTOM_DEVICE_PROFILE_NAME = "Custom Device Profile";

    protected static final String CLOUD_HTTPS_URL = "https://localhost";
    protected static final String WSS_URL = "wss://localhost";
    protected static RestClient restClient;
    protected static ObjectMapper mapper = new ObjectMapper();

    protected static RestClient edgeRestClient;

    protected static Edge edge;
    protected static String edgeUrl;

    @BeforeClass
    public static void before() throws Exception {
        restClient = new RestClient(CLOUD_HTTPS_URL);
        restClient.getRestTemplate().setRequestFactory(getRequestFactoryForSelfSignedCert());
        restClient.login("tenant@thingsboard.org", "tenant");

        String edgeHost = ContainerTestSuite.testContainer.getServiceHost("tb-edge", 8082);
        Integer edgePort = ContainerTestSuite.testContainer.getServicePort("tb-edge", 8082);
        edgeUrl = "http://" + edgeHost + ":" + edgePort;
        edgeRestClient = new RestClient(edgeUrl);

        edge = createEdge("test", "280629c7-f853-ee3d-01c0-fffbb6f2ef38", "g9ta4soeylw6smqkky8g");

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(90, TimeUnit.SECONDS).
                until(() -> {
                    boolean loginSuccessful = false;
                    try {
                        edgeRestClient.login("tenant@thingsboard.org", "tenant");
                        loginSuccessful = true;
                    } catch (Throwable ignored1) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ignored2) {}
                    }
                    return loginSuccessful;
                });

        Optional<Tenant> tenant = edgeRestClient.getTenantById(edge.getTenantId());
        Assert.assertTrue(tenant.isPresent());
        Assert.assertEquals(edge.getTenantId(), tenant.get().getId());

        updateRootRuleChain();

        createCustomDeviceProfile(CUSTOM_DEVICE_PROFILE_NAME);

        // This is a starting point to start other tests
        verifyWidgetBundles();
    }

    private static void verifyWidgetBundles() {
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS).
                until(() ->  {
                    try {
                        return edgeRestClient.getWidgetsBundles(new PageLink(100)).getTotalElements() == 14;
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
                            List<WidgetType> cloudBundleWidgetTypes = restClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
                            return cloudBundleWidgetTypes != null && edgeBundleWidgetTypes != null
                                    && edgeBundleWidgetTypes.size() == cloudBundleWidgetTypes.size();
                        } catch (Throwable e) {
                            return false;
                        }
                    });
            List<WidgetType> edgeBundleWidgetTypes = edgeRestClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
            List<WidgetType> cloudBundleWidgetTypes = restClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
            Assert.assertNotNull("edgeBundleWidgetTypes can't be null", edgeBundleWidgetTypes);
            Assert.assertNotNull("cloudBundleWidgetTypes can't be null", cloudBundleWidgetTypes);
        }
    }

    protected static void updateRootRuleChain() throws IOException {
        PageData<RuleChain> ruleChains = restClient.getRuleChains(new PageLink(100));
        RuleChainId rootRuleChainId = null;
        for (RuleChain datum : ruleChains.getData()) {
            if (datum.isRoot()) {
                rootRuleChainId = datum.getId();
                break;
            }
        }
        Assert.assertNotNull(rootRuleChainId);
        JsonNode configuration = mapper.readTree(AbstractContainerTest.class.getClassLoader().getResourceAsStream("PushToEdgeRootRuleChainMetadata.json"));
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(rootRuleChainId);
        ruleChainMetaData.setFirstNodeIndex(configuration.get("firstNodeIndex").asInt());
        ruleChainMetaData.setNodes(Arrays.asList(mapper.treeToValue(configuration.get("nodes"), RuleNode[].class)));
        ruleChainMetaData.setConnections(Arrays.asList(mapper.treeToValue(configuration.get("connections"), NodeConnectionInfo[].class)));
        restClient.saveRuleChainMetaData(ruleChainMetaData);
    }

    protected static DeviceProfile createCustomDeviceProfile(String deviceProfileName) {
        DeviceProfile deviceProfile = createDeviceProfile(deviceProfileName, null);
        extendDeviceProfileData(deviceProfile);
        return restClient.saveDeviceProfile(deviceProfile);
    }

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            log.info("=================================================");
            log.info("STARTING TEST: {}" , description.getMethodName());
            log.info("=================================================");
        }

        /**
         * Invoked when a test succeeds
         */
        protected void succeeded(Description description) {
            log.info("=================================================");
            log.info("SUCCEEDED TEST: {}" , description.getMethodName());
            log.info("=================================================");
        }

        /**
         * Invoked when a test fails
         */
        protected void failed(Throwable e, Description description) {
            log.info("=================================================");
            log.info("FAILED TEST: {}" , description.getMethodName(), e);
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
        extendDeviceProfileData(deviceProfile);
        return restClient.saveDeviceProfile(deviceProfile);
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
        edge.setName(name + RandomStringUtils.randomAlphanumeric(7));
        edge.setType("DEFAULT");
        edge.setRoutingKey(routingKey);
        edge.setSecret(secret);
        return restClient.saveEdge(edge);
    }

}
