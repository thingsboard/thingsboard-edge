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
package org.thingsboard.server.msa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.MqttDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.PowerSavingConfiguration;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.NoSecLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.transport.snmp.AuthenticationProtocol;
import org.thingsboard.server.common.data.transport.snmp.PrivacyProtocol;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.impl.TelemetryQueryingSnmpCommunicationConfig;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

@Slf4j
public class EdgeClientTest extends AbstractContainerTest {

    private static final String DEVICE_TELEMETRY_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostTelemetry {\n" +
            "  optional string key1 = 1;\n" +
            "  optional bool key2 = 2;\n" +
            "  optional double key3 = 3;\n" +
            "  optional int32 key4 = 4;\n" +
            "  JsonObject key5 = 5;\n" +
            "\n" +
            "  message JsonObject {\n" +
            "    optional int32 someNumber = 6;\n" +
            "    repeated int32 someArray = 7;\n" +
            "    optional NestedJsonObject someNestedObject = 8;\n" +
            "    message NestedJsonObject {\n" +
            "       optional string key = 9;\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String DEVICE_ATTRIBUTES_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostAttributes {\n" +
            "  optional string key1 = 1;\n" +
            "  optional bool key2 = 2;\n" +
            "  optional double key3 = 3;\n" +
            "  optional int32 key4 = 4;\n" +
            "  JsonObject key5 = 5;\n" +
            "\n" +
            "  message JsonObject {\n" +
            "    optional int32 someNumber = 6;\n" +
            "    repeated int32 someArray = 7;\n" +
            "    NestedJsonObject someNestedObject = 8;\n" +
            "    message NestedJsonObject {\n" +
            "       optional string key = 9;\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String DEVICE_RPC_RESPONSE_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "package rpc;\n" +
            "\n" +
            "message RpcResponseMsg {\n" +
            "  optional string payload = 1;\n" +
            "}";

    private static final String DEVICE_RPC_REQUEST_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "package rpc;\n" +
            "\n" +
            "message RpcRequestMsg {\n" +
            "  optional string method = 1;\n" +
            "  optional int32 requestId = 2;\n" +
            "  optional string params = 3;\n" +
            "}";

    private static final String OBSERVE_ATTRIBUTES_WITH_PARAMS =

            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/3_1.0/0/9\": \"batteryLevel\"\n" +
                    "    },\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [\n" +
                    "    ],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/3_1.0/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    private static final String CLIENT_LWM2M_SETTINGS =
            "     {\n" +
                    "    \"edrxCycle\": null,\n" +
                    "    \"powerMode\": \"DRX\",\n" +
                    "    \"fwUpdateResource\": null,\n" +
                    "    \"fwUpdateStrategy\": 1,\n" +
                    "    \"psmActivityTimer\": null,\n" +
                    "    \"swUpdateResource\": null,\n" +
                    "    \"swUpdateStrategy\": 1,\n" +
                    "    \"pagingTransmissionWindow\": null,\n" +
                    "    \"clientOnlyObserveAfterConnect\": 1\n" +
                    "  }";

    @Test
    public void testUsers() {
        verifyEntityGroups(EntityType.USER, 3);
    }

    @Test
    public void testRoles() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() ->  edgeRestClient.getRoles(RoleType.GENERIC, new PageLink(100)).getTotalElements() == 2);

        PageData<Role> genericPageData = edgeRestClient.getRoles(RoleType.GENERIC, new PageLink(100));

        List<EntityId> genericIds = genericPageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() ->  edgeRestClient.getRoles(RoleType.GROUP, new PageLink(100)).getTotalElements() == 1);
        PageData<Role> groupPageData = edgeRestClient.getRoles(RoleType.GROUP, new PageLink(100));
        List<EntityId> groupIds = groupPageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        genericIds.addAll(groupIds);
        assertEntitiesByIdsAndType(genericIds, EntityType.ROLE);
    }

    @Test
    public void testDeviceProfiles() {
        verifyDeviceProfilesOnEdge(3);

        DeviceProfile oneMoreDeviceProfile = createCustomDeviceProfile("ONE_MORE_DEVICE_PROFILE");

        verifyDeviceProfilesOnEdge(4);

        DeviceProfile snmpDeviceProfile = createCustomDeviceProfile("SNMP", createSnmpDeviceProfileTransportConfiguration());

        verifyDeviceProfilesOnEdge(5);

        DeviceProfile lwm2mDeviceProfile = createCustomDeviceProfile("LWM2M", createLwm2mDeviceProfileTransportConfiguration());

        verifyDeviceProfilesOnEdge(6);

        DeviceProfile coapDeviceProfile = createCustomDeviceProfile("COAP", createCoapDeviceProfileTransportConfiguration());

        verifyDeviceProfilesOnEdge(7);

        cloudRestClient.deleteDeviceProfile(oneMoreDeviceProfile.getId());
        cloudRestClient.deleteDeviceProfile(snmpDeviceProfile.getId());
        cloudRestClient.deleteDeviceProfile(coapDeviceProfile.getId());
        cloudRestClient.deleteDeviceProfile(lwm2mDeviceProfile.getId());

        verifyDeviceProfilesOnEdge(3);
    }

    private SnmpDeviceProfileTransportConfiguration createSnmpDeviceProfileTransportConfiguration() {
        SnmpDeviceProfileTransportConfiguration transportConfiguration = new SnmpDeviceProfileTransportConfiguration();
        List<SnmpCommunicationConfig> communicationConfigs = new ArrayList<>();
        TelemetryQueryingSnmpCommunicationConfig communicationConfig = new TelemetryQueryingSnmpCommunicationConfig();
        communicationConfig.setQueryingFrequencyMs(500L);
        List<SnmpMapping> mappings = new ArrayList<>();
        mappings.add(new SnmpMapping("1.3.3.5.6.7.8.9.1", "temperature", DataType.DOUBLE));
        communicationConfig.setMappings(mappings);
        communicationConfigs.add(communicationConfig);
        transportConfiguration.setCommunicationConfigs(communicationConfigs);
        transportConfiguration.setTimeoutMs(1000);
        transportConfiguration.setRetries(3);
        return transportConfiguration;
    }

    private Lwm2mDeviceProfileTransportConfiguration createLwm2mDeviceProfileTransportConfiguration() {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();

        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);

        transportConfiguration.setBootstrapServerUpdateEnable(true);

        TelemetryMappingConfiguration observeAttrConfiguration =
                JacksonUtil.fromString(OBSERVE_ATTRIBUTES_WITH_PARAMS, TelemetryMappingConfiguration.class);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);

        List<LwM2MBootstrapServerCredential> bootstrap = new ArrayList<>();
        AbstractLwM2MBootstrapServerCredential bootstrapServerCredential = new NoSecLwM2MBootstrapServerCredential();
        bootstrapServerCredential.setServerPublicKey("PUBLIC_KEY");
        bootstrapServerCredential.setShortServerId(123);
        bootstrapServerCredential.setBootstrapServerIs(true);
        bootstrapServerCredential.setHost("localhost");
        bootstrapServerCredential.setPort(5687);
        bootstrap.add(bootstrapServerCredential);
        transportConfiguration.setBootstrap(bootstrap);

        return transportConfiguration;
    }

    private CoapDeviceProfileTransportConfiguration createCoapDeviceProfileTransportConfiguration() {
        CoapDeviceProfileTransportConfiguration transportConfiguration = new CoapDeviceProfileTransportConfiguration();
        PowerSavingConfiguration clientSettings = new PowerSavingConfiguration();
        clientSettings.setPowerMode(PowerMode.DRX);
        clientSettings.setEdrxCycle(1L);
        clientSettings.setPsmActivityTimer(1L);
        clientSettings.setPagingTransmissionWindow(1L);
        transportConfiguration.setClientSettings(clientSettings);
        DefaultCoapDeviceTypeConfiguration coapDeviceTypeConfiguration = new DefaultCoapDeviceTypeConfiguration();
        ProtoTransportPayloadConfiguration transportPayloadTypeConfiguration = new ProtoTransportPayloadConfiguration();
        transportPayloadTypeConfiguration.setDeviceTelemetryProtoSchema(DEVICE_TELEMETRY_PROTO_SCHEMA);
        transportPayloadTypeConfiguration.setDeviceAttributesProtoSchema(DEVICE_ATTRIBUTES_PROTO_SCHEMA);
        transportPayloadTypeConfiguration.setDeviceRpcResponseProtoSchema(DEVICE_RPC_RESPONSE_PROTO_SCHEMA);
        transportPayloadTypeConfiguration.setDeviceRpcRequestProtoSchema(DEVICE_RPC_REQUEST_PROTO_SCHEMA);
        coapDeviceTypeConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
        transportConfiguration.setCoapDeviceTypeConfiguration(coapDeviceTypeConfiguration);
        return transportConfiguration;
    }

    private void verifyDeviceProfilesOnEdge(int expectedDeviceProfilesCnt) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() ->  edgeRestClient.getDeviceProfiles(new PageLink(100)).getTotalElements() == expectedDeviceProfilesCnt);

        PageData<DeviceProfile> pageData = edgeRestClient.getDeviceProfiles(new PageLink(100));
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

    @Test
    public void testWhiteLabeling() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    Optional<WhiteLabelingParams> edgeWhiteLabelParams = edgeRestClient.getCurrentWhiteLabelParams();
                    Optional<WhiteLabelingParams> cloudWhiteLabelParams = cloudRestClient.getCurrentWhiteLabelParams();
                    return edgeWhiteLabelParams.isPresent() &&
                            cloudWhiteLabelParams.isPresent() &&
                            edgeWhiteLabelParams.get().equals(cloudWhiteLabelParams.get());
                });

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    Optional<LoginWhiteLabelingParams> edgeLoginWhiteLabelParams = edgeRestClient.getCurrentLoginWhiteLabelParams();
                    Optional<LoginWhiteLabelingParams> cloudLoginWhiteLabelParams = cloudRestClient.getCurrentLoginWhiteLabelParams();
                    return edgeLoginWhiteLabelParams.isPresent() &&
                            cloudLoginWhiteLabelParams.isPresent() &&
                            edgeLoginWhiteLabelParams.get().equals(cloudLoginWhiteLabelParams.get());
                });

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    Optional<CustomTranslation> edgeCustomTranslationOpt = edgeRestClient.getCustomTranslation();
                    Optional<CustomTranslation> cloudCustomTranslationOpt = cloudRestClient.getCustomTranslation();
                    if (edgeCustomTranslationOpt.isEmpty() || cloudCustomTranslationOpt.isEmpty()) {
                        return false;
                    }
                    CustomTranslation edgeCustomTranslation = edgeCustomTranslationOpt.get();
                    if (edgeCustomTranslation.getTranslationMap().get("en_us") == null) {
                        return false;
                    }
                    JsonNode enUsNode = JacksonUtil.OBJECT_MAPPER.readTree(edgeCustomTranslation.getTranslationMap().get("en_us"));
                    if (!"TENANT_HOME".equals(enUsNode.get("home.home").asText())) {
                        return false;
                    }
                    CustomTranslation cloudCustomTranslation = cloudCustomTranslationOpt.get();
                    if (cloudCustomTranslation.getTranslationMap().get("en_us") == null) {
                        return false;
                    }
                    enUsNode = JacksonUtil.OBJECT_MAPPER.readTree(cloudCustomTranslation.getTranslationMap().get("en_us"));
                    if (!"TENANT_HOME".equals(enUsNode.get("home.home").asText())) {
                        return false;
                    }
                    return edgeCustomTranslation.equals(cloudCustomTranslation);
                });
    }

    @Test
    public void testTenantAdminSettings() {
        verifyTenantAdminSettingsByKey("general");
        verifyTenantAdminSettingsByKey("mailTemplates");
        verifyTenantAdminSettingsByKey("mail");

        // TODO: @voba - verify admin setting in next release. In the current there is no sysadmin on edge to fetch it
        // login as sysadmin on edge
        // login as sysadmin on cloud
        // verifyAdminSettingsByKey("general");
        // verifyAdminSettingsByKey("mailTemplates");
        // verifyAdminSettingsByKey("mail");
    }

    private void verifyTenantAdminSettingsByKey(String key) {
        Optional<AdminSettings> edgeAdminSettings = edgeRestClient.getAdminSettings(key);
        Assert.assertTrue("Admin settings is not available on edge, key = " + key, edgeAdminSettings.isPresent());
        Optional<AdminSettings> cloudAdminSettings = cloudRestClient.getAdminSettings(key);
        Assert.assertTrue("Admin settings is not available on cloud, key = " + key, cloudAdminSettings.isPresent());
        Assert.assertEquals("Admin settings on cloud and edge are different", edgeAdminSettings.get(), cloudAdminSettings.get());
    }

    @Test
    public void testWidgetsBundles() {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() ->  edgeRestClient.getWidgetsBundles(new PageLink(100)).getTotalElements() == 16);
        PageData<WidgetsBundle> pageData = edgeRestClient.getWidgetsBundles(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.WIDGETS_BUNDLE);

        for (String widgetsBundlesAlias : pageData.getData().stream().map(WidgetsBundle::getAlias).collect(Collectors.toList())) {
            Awaitility.await()
                    .atMost(30, TimeUnit.SECONDS).
                    until(() -> {
                        List<WidgetType> edgeBundleWidgetTypes = edgeRestClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
                        List<WidgetType> cloudBundleWidgetTypes = cloudRestClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
                        return cloudBundleWidgetTypes != null && edgeBundleWidgetTypes != null
                                && edgeBundleWidgetTypes.size() == cloudBundleWidgetTypes.size();
                    });
            List<WidgetType> edgeBundleWidgetTypes = edgeRestClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
            List<WidgetType> cloudBundleWidgetTypes = cloudRestClient.getBundleWidgetTypes(true, widgetsBundlesAlias);
            Assert.assertNotNull("edgeBundleWidgetTypes can't be null", edgeBundleWidgetTypes);
            Assert.assertNotNull("cloudBundleWidgetTypes can't be null", cloudBundleWidgetTypes);
            assertEntitiesByIdsAndType(edgeBundleWidgetTypes.stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.WIDGET_TYPE);
        }
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
            case OTA_PACKAGE:
                assertOtaPackages(entityIds);
                break;
            case QUEUE:
                assertQueues(entityIds);
                break;
            case CONVERTER:
                assertConverters(entityIds);
                break;
            case INTEGRATION:
                assertIntegrations(entityIds);
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
            actual.setDefaultRuleChainId(null);
            Assert.assertEquals("Device profiles on cloud and edge are different (except defaultRuleChainId)", expected, actual);
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

    private void assertRoles(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            RoleId roleId = new RoleId(entityId.getId());
            Optional<Role> edgeRole = edgeRestClient.getRoleById(roleId);
            Optional<Role> cloudRole = cloudRestClient.getRoleById(roleId);
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

    private void assertConverters(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            ConverterId converterId = new ConverterId(entityId.getId());
            Optional<Converter> edgeConverter = edgeRestClient.getConverterById(converterId);
            Optional<Converter> cloudConverter = cloudRestClient.getConverterById(converterId);
            Converter expected = edgeConverter.get();
            Converter actual = cloudConverter.get();
            Assert.assertEquals("Edge converter isEdgeTemplate incorrect", false, expected.isEdgeTemplate());
            Assert.assertEquals("Cloud converter isEdgeTemplate incorrect", true, actual.isEdgeTemplate());
            actual.setEdgeTemplate(false);
            Assert.assertEquals("Converters on cloud and edge are different", expected, actual);
        }
    }

    private void assertIntegrations(List<EntityId> entityIds) {
        for (EntityId entityId : entityIds) {
            IntegrationId integrationId = new IntegrationId(entityId.getId());
            Optional<Integration> edgeIntegration = edgeRestClient.getIntegrationById(integrationId);
            Optional<Integration> cloudIntegration = cloudRestClient.getIntegrationById(integrationId);
            Integration expected = edgeIntegration.get();
            Integration actual = cloudIntegration.get();
            Assert.assertEquals("Edge integration isEdgeTemplate incorrect", false, expected.isEdgeTemplate());
            Assert.assertEquals("Cloud integration isEdgeTemplate incorrect", true, actual.isEdgeTemplate());
            actual.setEdgeTemplate(false);

            // configuration must be reset, because configuration on cloud contains placeholders
            actual.setConfiguration(null);
            expected.setConfiguration(null);

            Assert.assertEquals("Integrations on cloud and edge are different", expected, actual);
        }
    }


    private JsonNode cleanLastLoginTsFromAdditionalInfo(JsonNode additionalInfo) {
        if (additionalInfo != null && additionalInfo.has("lastLoginTs")) {
            ((ObjectNode) additionalInfo).remove("lastLoginTs");
        }
        return additionalInfo;
    }

    @Test
    public void testDevices() throws Exception {
        EntityGroup savedDeviceEntityGroup = createEntityGroup(EntityType.DEVICE);
        Device edgeDevice1 = saveAndAssignDeviceToEdge(savedDeviceEntityGroup);

        cloudRestClient.saveDeviceAttributes(edgeDevice1.getId(), DataConstants.SERVER_SCOPE, JacksonUtil.OBJECT_MAPPER.readTree("{\"key1\":\"value1\"}"));
        cloudRestClient.saveDeviceAttributes(edgeDevice1.getId(), DataConstants.SHARED_SCOPE, JacksonUtil.OBJECT_MAPPER.readTree("{\"key2\":\"value2\"}"));

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> verifyAttributeOnEdge(edgeDevice1.getId(), DataConstants.SERVER_SCOPE, "key1", "value1"));

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> verifyAttributeOnEdge(edgeDevice1.getId(), DataConstants.SHARED_SCOPE, "key2", "value2"));

        validateDeviceTransportConfiguration(edgeDevice1, cloudRestClient, edgeRestClient);

        cloudRestClient.unassignEntityGroupFromEdge(edge.getId(), savedDeviceEntityGroup.getId(), EntityType.DEVICE);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDeviceById(edgeDevice1.getId()).isEmpty());

        cloudRestClient.assignEntityGroupToEdge(edge.getId(), savedDeviceEntityGroup.getId(), EntityType.DEVICE);
    }

    private void validateDeviceTransportConfiguration(Device device,
                                                      RestClient sourceRestClient,
                                                      RestClient targetRestClient) {
        validateDefaultDeviceTransportConfiguration(device, sourceRestClient, targetRestClient);
        validateMqttDeviceTransportConfiguration(device, sourceRestClient, targetRestClient);
        validateCoapDeviceTransportConfiguration(device, sourceRestClient, targetRestClient);
        validateLwm2mDeviceTransportConfiguration(device, sourceRestClient, targetRestClient);
        validateSnmpDeviceTransportConfiguration(device, sourceRestClient, targetRestClient);
    }

    private void validateDefaultDeviceTransportConfiguration(Device device,
                                                             RestClient sourceRestClient,
                                                             RestClient targetRestClient) {
        setAndValidateDeviceTransportConfiguration(device,
                new DefaultDeviceTransportConfiguration(),
                sourceRestClient,
                targetRestClient);
    }

    private void validateMqttDeviceTransportConfiguration(Device device,
                                                          RestClient sourceRestClient,
                                                          RestClient targetRestClient) {
        MqttDeviceTransportConfiguration transportConfiguration = new MqttDeviceTransportConfiguration();
        transportConfiguration.getProperties().put("topic", "tb_rule_engine.thermostat");
        setAndValidateDeviceTransportConfiguration(device,
                transportConfiguration,
                sourceRestClient,
                targetRestClient);
    }
    private void validateCoapDeviceTransportConfiguration(Device device,
                                                          RestClient sourceRestClient,
                                                          RestClient targetRestClient) {
        CoapDeviceTransportConfiguration transportConfiguration = new CoapDeviceTransportConfiguration();
        transportConfiguration.setEdrxCycle(1L);
        transportConfiguration.setPagingTransmissionWindow(2L);
        transportConfiguration.setPsmActivityTimer(3L);
        transportConfiguration.setPowerMode(PowerMode.DRX);
        setAndValidateDeviceTransportConfiguration(device,
                transportConfiguration,
                sourceRestClient,
                targetRestClient);
    }

    private void validateLwm2mDeviceTransportConfiguration(Device device,
                                                           RestClient sourceRestClient,
                                                           RestClient targetRestClient) {
        Lwm2mDeviceTransportConfiguration transportConfiguration = new Lwm2mDeviceTransportConfiguration();
        transportConfiguration.setEdrxCycle(1L);
        transportConfiguration.setPagingTransmissionWindow(2L);
        transportConfiguration.setPsmActivityTimer(3L);
        transportConfiguration.setPowerMode(PowerMode.PSM);
        setAndValidateDeviceTransportConfiguration(device,
                transportConfiguration,
                sourceRestClient,
                targetRestClient);
    }

    private void validateSnmpDeviceTransportConfiguration(Device device,
                                                          RestClient sourceRestClient,
                                                          RestClient targetRestClient) {
        SnmpDeviceTransportConfiguration transportConfiguration = new SnmpDeviceTransportConfiguration();
        transportConfiguration.setAuthenticationProtocol(AuthenticationProtocol.SHA_256);
        transportConfiguration.setPrivacyProtocol(PrivacyProtocol.AES_256);
        setAndValidateDeviceTransportConfiguration(device,
                transportConfiguration,
                sourceRestClient,
                targetRestClient);
    }

    private void setAndValidateDeviceTransportConfiguration(Device device,
                                                            DeviceTransportConfiguration transportConfiguration,
                                                            RestClient sourceRestClient,
                                                            RestClient targetRestClient) {
        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        deviceData.setTransportConfiguration(transportConfiguration);
        device.setDeviceData(deviceData);
        sourceRestClient.saveDevice(device);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<Device> targetDevice = targetRestClient.getDeviceById(device.getId());
                    Optional<Device> sourceDevice = sourceRestClient.getDeviceById(device.getId());
                    Device expected = targetDevice.get();
                    Device actual = sourceDevice.get();
                    return expected.equals(actual);
                });
    }

    private boolean verifyAttributeOnEdge(EntityId entityId, String scope, String key, String expectedValue) {
        List<AttributeKvEntry> attributesByScope = edgeRestClient.getAttributesByScope(entityId, scope, Arrays.asList(key));
        if (attributesByScope.isEmpty()) {
            return false;
        }
        AttributeKvEntry attributeKvEntry = attributesByScope.get(0);
        return attributeKvEntry.getValueAsString().equals(expectedValue);
    }

    @Test
    public void testAssets() throws Exception {
        EntityGroup savedAssetEntityGroup = createEntityGroup(EntityType.ASSET);
        Asset savedAsset = saveAndAssignAssetToEdge(savedAssetEntityGroup);

        JsonNode assetAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"assetKey\":\"assetValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedAsset.getId(), DataConstants.SERVER_SCOPE, assetAttributes);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> verifyAttributeOnEdge(savedAsset.getId(),
                        DataConstants.SERVER_SCOPE, "assetKey", "assetValue"));

        cloudRestClient.unassignEntityGroupFromEdge(edge.getId(), savedAssetEntityGroup.getId(), EntityType.ASSET);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getAssetById(savedAsset.getId()).isEmpty());

        cloudRestClient.assignEntityGroupToEdge(edge.getId(), savedAssetEntityGroup.getId(), EntityType.ASSET);
    }

    @Test
    public void testRuleChains() throws Exception {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getRuleChains(new PageLink(100)).getTotalElements() == 1);

        PageData<RuleChain> pageData = edgeRestClient.getRuleChains(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.RULE_CHAIN);

        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge Test Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = cloudRestClient.saveRuleChain(ruleChain);
        createRuleChainMetadata(savedRuleChain);

        cloudRestClient.assignRuleChainToEdge(edge.getId(), savedRuleChain.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getRuleChainById(savedRuleChain.getId()).isPresent());

        assertEntitiesByIdsAndType(Collections.singletonList(savedRuleChain.getId()), EntityType.RULE_CHAIN);

        cloudRestClient.unassignRuleChainFromEdge(edge.getId(), savedRuleChain.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getRuleChainById(savedRuleChain.getId()).isEmpty());

        cloudRestClient.deleteRuleChain(savedRuleChain.getId());
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

    @Test
    public void testDashboards() throws Exception {
        verifyEntityGroups(EntityType.DASHBOARD, 1);

        EntityGroup dashboardEntityGroup = new EntityGroup();
        dashboardEntityGroup.setType(EntityType.DASHBOARD);
        dashboardEntityGroup.setName("DashboardGroup");
        EntityGroupInfo savedDashboardEntityGroup = cloudRestClient.saveEntityGroup(dashboardEntityGroup);
        Dashboard savedDashboardOnCloud = saveDashboardOnCloud("Edge Dashboard 1", savedDashboardEntityGroup.getId());

        cloudRestClient.assignEntityGroupToEdge(edge.getId(), savedDashboardEntityGroup.getId(), EntityType.DASHBOARD);

        verifyEntityGroups(EntityType.DASHBOARD, 2);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).isPresent());

        JsonNode dashboardAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"dashboardKey\":\"dashboardValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedDashboardOnCloud.getId(), DataConstants.SERVER_SCOPE, dashboardAttributes);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> verifyAttributeOnEdge(savedDashboardOnCloud.getId(),
                        DataConstants.SERVER_SCOPE, "dashboardKey", "dashboardValue"));

        cloudRestClient.unassignEntityGroupFromEdge(edge.getId(), savedDashboardEntityGroup.getId(), EntityType.DASHBOARD);

        verifyEntityGroups(EntityType.DASHBOARD, 1);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).isEmpty());

        cloudRestClient.deleteDashboard(savedDashboardOnCloud.getId());
    }

    @Test
    public void testRelations() throws Exception {
        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));
        Asset asset = saveAndAssignAssetToEdge(createEntityGroup(EntityType.ASSET));

        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(asset.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        cloudRestClient.saveRelation(relation);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isPresent());

        cloudRestClient.deleteRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isEmpty());
    }

    @Test
    public void testAlarms() throws Exception {
        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));

        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarm.setType("alarm");
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        Alarm savedAlarm = cloudRestClient.saveAlarm(alarm);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> getAlarmForEntityFromEdge(device.getId()).isPresent());

        cloudRestClient.ackAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    AlarmInfo alarmData = getAlarmForEntityFromEdge(device.getId()).get();
                    return alarmData.getAckTs() > 0;
                });

        cloudRestClient.clearAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    AlarmInfo alarmData = getAlarmForEntityFromEdge(device.getId()).get();
                    return alarmData.getClearTs() > 0;
                });

        cloudRestClient.deleteAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> getAlarmForEntityFromEdge(device.getId()).isEmpty());
    }


    private Optional<AlarmInfo> getAlarmForEntityFromCloud(EntityId entityId) {
        return getAlarmForEntity(entityId, cloudRestClient);
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

    @Test
    public void testEntityViews() throws Exception {
        verifyEntityGroups(EntityType.ENTITY_VIEW, 1);

        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));

        EntityGroup entityViewEntityGroup = new EntityGroup();
        entityViewEntityGroup.setType(EntityType.ENTITY_VIEW);
        entityViewEntityGroup.setName("EntityViewGroup");
        EntityGroupInfo savedEntityViewEntityGroup = cloudRestClient.saveEntityGroup(entityViewEntityGroup);
        EntityView savedEntityViewOnCloud = saveEntityViewOnCloud("Edge Entity View 1", "Default", device.getId(), savedEntityViewEntityGroup.getId());

        cloudRestClient.assignEntityGroupToEdge(edge.getId(), savedEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);

        verifyEntityGroups(EntityType.ENTITY_VIEW, 2);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isPresent());

        JsonNode entityViewAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"entityViewKey\":\"entityViewValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedEntityViewOnCloud.getId(), DataConstants.SERVER_SCOPE, entityViewAttributes);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> verifyAttributeOnEdge(savedEntityViewOnCloud.getId(),
                        DataConstants.SERVER_SCOPE, "entityViewKey", "entityViewValue"));

        cloudRestClient.unassignEntityGroupFromEdge(edge.getId(), savedEntityViewEntityGroup.getId(), EntityType.ENTITY_VIEW);

        verifyEntityGroups(EntityType.ENTITY_VIEW, 1);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getEntityViewById(savedEntityViewOnCloud.getId()).isEmpty());

        cloudRestClient.deleteEntityView(savedEntityViewOnCloud.getId());
    }

    @Test
    public void testWidgetsBundleAndWidgetType() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("Test Widget Bundle");
        WidgetsBundle savedWidgetsBundle = cloudRestClient.saveWidgetsBundle(widgetsBundle);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getWidgetsBundleById(savedWidgetsBundle.getId()).isPresent());

        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Test Widget Type");
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        ObjectNode descriptor = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        descriptor.put("key", "value");
        widgetType.setDescriptor(descriptor);
        WidgetType savedWidgetType = cloudRestClient.saveWidgetType(widgetType);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getWidgetTypeById(savedWidgetType.getId()).isPresent());

        cloudRestClient.deleteWidgetType(savedWidgetType.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getWidgetTypeById(savedWidgetType.getId()).isEmpty());

        cloudRestClient.deleteWidgetsBundle(savedWidgetsBundle.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getWidgetsBundleById(savedWidgetsBundle.getId()).isEmpty());
    }

    @Test
    public void testSendPostTelemetryRequestToCloud() throws Exception {
        List<String> keys = Arrays.asList("strTelemetryToCloud", "boolTelemetryToCloud", "doubleTelemetryToCloud", "longTelemetryToCloud");

        JsonObject timeseriesPayload = new JsonObject();
        timeseriesPayload.addProperty("strTelemetryToCloud", "value1");
        timeseriesPayload.addProperty("boolTelemetryToCloud", true);
        timeseriesPayload.addProperty("doubleTelemetryToCloud", 42.0);
        timeseriesPayload.addProperty("longTelemetryToCloud", 72L);

        List<TsKvEntry> kvEntries = sendPostTelemetryRequest(edgeRestClient, edgeUrl, cloudRestClient, timeseriesPayload, keys);

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

    @Test
    public void testSendPostTelemetryRequestToEdge() throws Exception {
        List<String> keys = Arrays.asList("strTelemetryToEdge", "boolTelemetryToEdge", "doubleTelemetryToEdge", "longTelemetryToEdge");

        JsonObject timeseriesPayload = new JsonObject();
        timeseriesPayload.addProperty("strTelemetryToEdge", "value1");
        timeseriesPayload.addProperty("boolTelemetryToEdge", true);
        timeseriesPayload.addProperty("doubleTelemetryToEdge", 42.0);
        timeseriesPayload.addProperty("longTelemetryToEdge", 72L);

        List<TsKvEntry> kvEntries = sendPostTelemetryRequest(cloudRestClient, CLOUD_HTTPS_URL, edgeRestClient, timeseriesPayload, keys);

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
        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> sourceRestClient.getDeviceCredentialsByDeviceId(device.getId()).isPresent());

        DeviceCredentials deviceCredentials = sourceRestClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        String accessToken = deviceCredentials.getCredentialsId();

        ResponseEntity deviceTelemetryResponse = sourceRestClient.getRestTemplate()
                .postForEntity(sourceUrl + "/api/v1/{credentialsId}/telemetry",
                        JacksonUtil.OBJECT_MAPPER.readTree(timeseriesPayload.toString()),
                        ResponseEntity.class,
                        accessToken);
        Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<TsKvEntry> latestTimeseries;
                    try {
                        latestTimeseries = targetRestClient.getLatestTimeseries(device.getId(), keys);
                    } catch (Exception e) {
                        return false;
                    }
                    return latestTimeseries.size() == keys.size();
                });

        verifyDeviceIsActive(targetRestClient, device.getId());

        return targetRestClient.getLatestTimeseries(device.getId(), keys);
    }

    @Test
    public void testSendPostAttributesRequestToCloud() throws Exception {
        List<String> keys = Arrays.asList("strAttrToCloud", "boolAttrToCloud", "doubleAttrToCloud", "longAttrToCloud");

        JsonObject attrPayload = new JsonObject();
        attrPayload.addProperty("strAttrToCloud", "value1");
        attrPayload.addProperty("boolAttrToCloud", true);
        attrPayload.addProperty("doubleAttrToCloud", 42.0);
        attrPayload.addProperty("longAttrToCloud", 72L);

        List<AttributeKvEntry> kvEntries = testSendPostAttributesRequest(edgeRestClient, edgeUrl, cloudRestClient, attrPayload, keys);

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

    @Test
    public void testSendPostAttributesRequestToEdge() throws Exception {
        List<String> keys = Arrays.asList("strAttrToEdge", "boolAttrToEdge", "doubleAttrToEdge", "longAttrToEdge");

        JsonObject attrPayload = new JsonObject();
        attrPayload.addProperty("strAttrToEdge", "value1");
        attrPayload.addProperty("boolAttrToEdge", true);
        attrPayload.addProperty("doubleAttrToEdge", 42.0);
        attrPayload.addProperty("longAttrToEdge", 72L);

        List<AttributeKvEntry> kvEntries = testSendPostAttributesRequest(cloudRestClient, CLOUD_HTTPS_URL, edgeRestClient, attrPayload, keys);

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

        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> sourceRestClient.getDeviceCredentialsByDeviceId(device.getId()).isPresent());
        DeviceCredentials deviceCredentials = sourceRestClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        String accessToken = deviceCredentials.getCredentialsId();

        ResponseEntity deviceClientsAttributes = sourceRestClient.getRestTemplate()
                .postForEntity(sourceUrl + "/api/v1/" + accessToken + "/attributes/", JacksonUtil.OBJECT_MAPPER.readTree(attributesPayload.toString()),
                        ResponseEntity.class,
                        accessToken);
        Assert.assertTrue(deviceClientsAttributes.getStatusCode().is2xxSuccessful());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> targetRestClient.getAttributesByScope(device.getId(), DataConstants.CLIENT_SCOPE, keys).size() == keys.size());

        List<AttributeKvEntry> attributeKvEntries = targetRestClient.getAttributesByScope(device.getId(), DataConstants.CLIENT_SCOPE, keys);

        sourceRestClient.deleteEntityAttributes(device.getId(), DataConstants.CLIENT_SCOPE, keys);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> targetRestClient.getAttributesByScope(device.getId(), DataConstants.CLIENT_SCOPE, keys).size() == 0);

        verifyDeviceIsActive(targetRestClient, device.getId());

        return attributeKvEntries;
    }

    private void verifyDeviceIsActive(RestClient restClient, DeviceId deviceId) {
        Awaitility.await()
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

    @Test
    public void testSendAttributesUpdatedToEdge() throws Exception {
        List<String> keys = Arrays.asList("strAttrToEdge", "boolAttrToEdge", "doubleAttrToEdge", "longAttrToEdge");

        JsonObject attrPayload = new JsonObject();
        attrPayload.addProperty("strAttrToEdge", "value1");
        attrPayload.addProperty("boolAttrToEdge", true);
        attrPayload.addProperty("doubleAttrToEdge", 42.0);
        attrPayload.addProperty("longAttrToEdge", 72L);

        List<AttributeKvEntry> kvEntries = sendAttributesUpdated(cloudRestClient, edgeRestClient, attrPayload, keys, DataConstants.SERVER_SCOPE);
        verifyAttributesUpdatedToEdge(kvEntries);

        kvEntries = sendAttributesUpdated(cloudRestClient, edgeRestClient, attrPayload, keys, DataConstants.SHARED_SCOPE);
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

    @Test
    public void testSendAttributesUpdatedToCloud() throws Exception {
        List<String> keys = Arrays.asList("strAttrToCloud", "boolAttrToCloud", "doubleAttrToCloud", "longAttrToCloud");

        JsonObject attrPayload = new JsonObject();
        attrPayload.addProperty("strAttrToCloud", "value1");
        attrPayload.addProperty("boolAttrToCloud", true);
        attrPayload.addProperty("doubleAttrToCloud", 42.0);
        attrPayload.addProperty("longAttrToCloud", 72L);

        List<AttributeKvEntry> kvEntries = sendAttributesUpdated(edgeRestClient, cloudRestClient, attrPayload, keys, DataConstants.SERVER_SCOPE);
        verifyAttributesUpdatedToCloud(kvEntries);

        kvEntries = sendAttributesUpdated(edgeRestClient, cloudRestClient, attrPayload, keys, DataConstants.SHARED_SCOPE);
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

        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));

        sourceRestClient.saveDeviceAttributes(device.getId(), scope, JacksonUtil.OBJECT_MAPPER.readTree(attributesPayload.toString()));

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> targetRestClient.getAttributesByScope(device.getId(), scope, keys).size() == keys.size());

        List<AttributeKvEntry> attributeKvEntries =
                targetRestClient.getAttributesByScope(device.getId(), scope, keys);

        sourceRestClient.deleteEntityAttributes(device.getId(), scope, keys);

        verifyDeviceIsActive(targetRestClient, device.getId());

        return attributeKvEntries;
    }

    @Test
    public void sendDeviceToCloud() throws Exception {
        Device savedDeviceOnEdge = saveDeviceOnEdge("Edge Device 2", "default");

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> cloudRestClient.getDeviceById(savedDeviceOnEdge.getId()).isPresent());

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
                until(() -> cloudRestClient.getEntityGroupsForEntity(savedDeviceOnEdge.getId()).size() == 2);

        validateDeviceTransportConfiguration(savedDeviceOnEdge, edgeRestClient, cloudRestClient);

        edgeRestClient.deleteDevice(savedDeviceOnEdge.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> cloudRestClient.getEntityGroupsForEntity(savedDeviceOnEdge.getId()).size() == 1);
    }

    private void verifyDeviceCredentialsOnCloudAndEdge(Device savedDeviceOnEdge) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> cloudRestClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdge.getId()).isPresent());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdge.getId()).isPresent());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    DeviceCredentials deviceCredentialsOnEdge =
                            edgeRestClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdge.getId()).get();
                    DeviceCredentials deviceCredentialsOnCloud =
                            cloudRestClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdge.getId()).get();
                    // TODO: @voba - potential fix for future releases
                    deviceCredentialsOnCloud.setId(null);
                    deviceCredentialsOnEdge.setId(null);
                    deviceCredentialsOnCloud.setCreatedTime(0);
                    deviceCredentialsOnEdge.setCreatedTime(0);
                    return deviceCredentialsOnCloud.equals(deviceCredentialsOnEdge);
                });
    }

    @Test
    public void sendDeviceWithNameThatAlreadyExistsOnCloud() throws Exception {
        String deviceName = RandomStringUtils.randomAlphanumeric(15);
        Device savedDeviceOnCloud = saveDeviceOnCloud(deviceName, "default");
        Device savedDeviceOnEdge = saveDeviceOnEdge(deviceName, "default");

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> cloudRestClient.getDeviceById(savedDeviceOnEdge.getId()).isPresent());

        // device on edge must be renamed
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> !edgeRestClient.getDeviceById(savedDeviceOnEdge.getId()).get().getName().equals(deviceName));

        edgeRestClient.deleteDevice(savedDeviceOnEdge.getId());
        cloudRestClient.deleteDevice(savedDeviceOnEdge.getId());
        cloudRestClient.deleteDevice(savedDeviceOnCloud.getId());
    }

    @Test
    public void sendRelationToCloud() throws Exception {
        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));

        Device savedDeviceOnEdge = saveDeviceOnEdge("Test Device 3", "default");
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> cloudRestClient.getDeviceById(savedDeviceOnEdge.getId()).isPresent());

        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(savedDeviceOnEdge.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        edgeRestClient.saveRelation(relation);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> cloudRestClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isPresent());

        edgeRestClient.deleteRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> cloudRestClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isEmpty());

        edgeRestClient.deleteDevice(savedDeviceOnEdge.getId());
        cloudRestClient.deleteDevice(savedDeviceOnEdge.getId());
    }

    @Test
    public void sendAlarmToCloud() throws Exception {
        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));

        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarm.setType("alarm from edge");
        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm savedAlarm = edgeRestClient.saveAlarm(alarm);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> getAlarmForEntityFromCloud(device.getId()).isPresent());

        Assert.assertEquals("Alarm on edge and cloud have different types",
                "alarm from edge", getAlarmForEntityFromCloud(device.getId()).get().getType());

        edgeRestClient.ackAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    AlarmInfo alarmData = getAlarmForEntityFromCloud(device.getId()).get();
                    return alarmData.getAckTs() > 0;
                });

        edgeRestClient.clearAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    AlarmInfo alarmData = getAlarmForEntityFromCloud(device.getId()).get();
                    return alarmData.getClearTs() > 0;
                });

        edgeRestClient.deleteAlarm(savedAlarm.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> getAlarmForEntityFromCloud(device.getId()).isEmpty());
    }

    @Test
    public void changeOwnerToCustomer() {
        // create device and assign it to edge
        // create customer A on cloud
        // add admin users to customer A
        // change edge owner from tenant to customer A
        // login to edge with customer A admin user
        // make sure that device assigned to edge from tenant is not available on edge anymore
        // change edge owner from customer A to tenant
        // make sure that login edge with customer A admin user doesn't work
    }

    @Test
    public void testOneWayRpcCall() throws Exception {
        // create device on cloud and assign to edge
        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    Optional<DeviceCredentials> edgeDeviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(device.getId());
                    Optional<DeviceCredentials> cloudDeviceCredentials = cloudRestClient.getDeviceCredentialsByDeviceId(device.getId());
                    return edgeDeviceCredentials.isPresent() &&
                            cloudDeviceCredentials.isPresent() &&
                            edgeDeviceCredentials.get().getCredentialsId().equals(cloudDeviceCredentials.get().getCredentialsId());
                });

        Optional<DeviceCredentials> deviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(device.getId());

        Assert.assertTrue(deviceCredentials.isPresent());

        // subscribe to rpc requests to edge
        final ResponseEntity<JsonNode>[] rpcSubscriptionRequest = new ResponseEntity[]{null};

        new Thread(() -> {
            String subscribeToRpcRequestUrl = edgeUrl + "/api/v1/" + deviceCredentials.get().getCredentialsId() + "/rpc?timeout=20000";
            rpcSubscriptionRequest[0] = edgeRestClient.getRestTemplate().getForEntity(subscribeToRpcRequestUrl, JsonNode.class);
        }).start();

        // send rpc request to device over cloud
        ObjectNode initialRequestBody = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        initialRequestBody.put("method", "setGpio");
        initialRequestBody.put("params", "{\"pin\":\"23\", \"value\": 1}");
        cloudRestClient.handleOneWayDeviceRPCRequest(device.getId(), initialRequestBody);

        // verify that rpc request was received
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    if (rpcSubscriptionRequest[0] == null || rpcSubscriptionRequest[0].getBody() == null) {
                        return false;
                    }
                    JsonNode requestBody = rpcSubscriptionRequest[0].getBody();
                    if (requestBody.get("id") == null) {
                        return false;
                    }
                    return initialRequestBody.get("method").equals(requestBody.get("method"))
                            && initialRequestBody.get("params").equals(requestBody.get("params"));
                });
    }

    @Test
    public void testTwoWayRpcCall() throws Exception {
        // create device on cloud and assign to edge
        Device device = saveAndAssignDeviceToEdge(createEntityGroup(EntityType.DEVICE));

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    Optional<DeviceCredentials> edgeDeviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(device.getId());
                    Optional<DeviceCredentials> cloudDeviceCredentials = cloudRestClient.getDeviceCredentialsByDeviceId(device.getId());
                    return edgeDeviceCredentials.isPresent() &&
                            cloudDeviceCredentials.isPresent() &&
                            edgeDeviceCredentials.get().getCredentialsId().equals(cloudDeviceCredentials.get().getCredentialsId());
                });

        Optional<DeviceCredentials> deviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(device.getId());

        Assert.assertTrue(deviceCredentials.isPresent());

        // subscribe to rpc requests to edge
        final ResponseEntity<JsonNode>[] rpcSubscriptionRequest = new ResponseEntity[]{null};

        new Thread(() -> {
            String subscribeToRpcRequestUrl = edgeUrl + "/api/v1/" + deviceCredentials.get().getCredentialsId() + "/rpc?timeout=20000";
            rpcSubscriptionRequest[0] = edgeRestClient.getRestTemplate().getForEntity(subscribeToRpcRequestUrl, JsonNode.class);
        }).start();

        // send two-way rpc request to device over cloud
        ObjectNode initialRequestBody = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        initialRequestBody.put("method", "setGpio");
        initialRequestBody.put("params", "{\"pin\":\"23\", \"value\": 1}");

        final JsonNode[] rpcTwoWayRequest = new JsonNode[]{null};
        new Thread(() -> {
            rpcTwoWayRequest[0] = cloudRestClient.handleTwoWayDeviceRPCRequest(device.getId(), initialRequestBody);
        }).start();

        // verify that rpc request was received
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    if (rpcSubscriptionRequest[0] == null || rpcSubscriptionRequest[0].getBody() == null) {
                        return false;
                    }
                    JsonNode requestBody = rpcSubscriptionRequest[0].getBody();
                    if (requestBody.get("id") == null) {
                        return false;
                    }
                    return initialRequestBody.get("method").equals(requestBody.get("method"))
                            && initialRequestBody.get("params").equals(requestBody.get("params"));
                });

        // send response back to the rpc request
        ObjectNode replyBody = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        replyBody.put("result", "ok");

        String rpcReply = edgeUrl + "/api/v1/" + deviceCredentials.get().getCredentialsId() + "/rpc/" + rpcSubscriptionRequest[0].getBody().get("id");
        edgeRestClient.getRestTemplate().postForEntity(rpcReply, replyBody, Void.class);

        // verify on the cloud that rpc response was received
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    if (rpcTwoWayRequest[0] == null) {
                        return false;
                    }
                    JsonNode responseBody = rpcTwoWayRequest[0];
                    return "ok".equals(responseBody.get("result").textValue());
                });
    }

    @Test
    public void testOtaPackages() throws Exception {
        DeviceProfileInfo defaultDeviceProfileInfo = cloudRestClient.getDefaultDeviceProfileInfo();
        OtaPackageInfo firmware = new OtaPackageInfo();
        firmware.setDeviceProfileId(new DeviceProfileId(defaultDeviceProfileInfo.getId().getId()));
        firmware.setType(FIRMWARE);
        firmware.setTitle("My firmware #2");
        firmware.setVersion("v2.0");
        firmware.setTag("My firmware #2 v2.0");
        firmware.setHasData(false);
        OtaPackageInfo savedOtaPackageInfo = cloudRestClient.saveOtaPackageInfo(firmware, false);

        cloudRestClient.saveOtaPackageData(savedOtaPackageInfo.getId(), null, ChecksumAlgorithm.SHA256, "firmware.bin", new byte[]{1, 3, 5});

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() ->  {
                    PageData<OtaPackageInfo> otaPackages = edgeRestClient.getOtaPackages(new PageLink(100));
                    if (otaPackages.getData().size() < 1) {
                        return false;
                    }
                    OtaPackage otaPackageById = edgeRestClient.getOtaPackageById(otaPackages.getData().get(0).getId());
                    return otaPackageById.isHasData();
                });

        PageData<OtaPackageInfo> pageData = edgeRestClient.getOtaPackages(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.OTA_PACKAGE);

        cloudRestClient.deleteOtaPackage(savedOtaPackageInfo.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() ->  edgeRestClient.getOtaPackages(new PageLink(100)).getTotalElements() == 0);
    }

    @Test
    public void testIntegrations() throws Exception {
        JsonNode edgeAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"valAttr\":\"val3\", \"baseUrl\":\"" + edgeUrl + "\"}");
        cloudRestClient.saveEntityAttributesV1(edge.getId(), DataConstants.SERVER_SCOPE, edgeAttributes);

        ObjectNode converterConfiguration = JacksonUtil.OBJECT_MAPPER.createObjectNode()
                .put("decoder", "return {deviceName: 'Device Converter ' + metadata['key'], deviceType: 'thermostat'};");
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(converterConfiguration);
        converter.setEdgeTemplate(true);
        Converter savedConverter = cloudRestClient.saveConverter(converter);

        Integration integration = new Integration();
        integration.setName("Edge integration");
        integration.setAllowCreateDevicesOrAssets(true);
        integration.setRoutingKey(RandomStringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(savedConverter.getId());
        integration.setType(IntegrationType.HTTP);

        ObjectNode integrationConfiguration = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        integrationConfiguration.putObject("metadata").put("key", "val1");
        integrationConfiguration.put("baseUrl", "${{baseUrl}}");
        integration.setConfiguration(integrationConfiguration);
        integration.setEdgeTemplate(true);
        integration.setEnabled(true);
        Integration savedIntegration = cloudRestClient.saveIntegration(integration);

        validateIntegrationAssignToEdge(savedIntegration);

        verifyHttpIntegrationUpAndRunning(savedIntegration, "Device Converter val1");

        validateIntegrationConfigurationUpdate(savedIntegration);

        validateEdgeAttributesUpdate(savedIntegration);

        validateIntegrationDefaultConverterUpdate(savedIntegration);

        validateIntegrationDownlinkConverterUpdate(savedIntegration);

        validateIntegrationUnassignFromEdge(savedIntegration);

        validateRemoveOfIntegration(savedIntegration);
    }

    private void verifyHttpIntegrationUpAndRunning(Integration integration, String expectedDeviceName) throws JsonProcessingException {
        try {
            SECONDS.sleep(1); // wait for integration to be recompiled and restarted with updated config
        } catch (Throwable ignored) {}

        ObjectNode values = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        values.put("deviceName", expectedDeviceName);
        edgeRestClient.getRestTemplate().postForEntity(edgeUrl + "/api/v1/integrations/http/" + integration.getRoutingKey(),
                        values,
                        ResponseEntity.class);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    Optional<Device> tenantDevice = edgeRestClient.getTenantDevice(expectedDeviceName);
                    return tenantDevice.isPresent();
                });
    }

    private void validateIntegrationAssignToEdge(Integration savedIntegration) {
        cloudRestClient.assignIntegrationToEdge(edge.getId(), savedIntegration.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getIntegrations(new PageLink(100)).getTotalElements() == 1);

        PageData<Converter> converters = edgeRestClient.getConverters(new PageLink(100));
        assertEntitiesByIdsAndType(converters.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.CONVERTER);

        PageData<Integration> integrations = edgeRestClient.getIntegrations(new PageLink(100));
        assertEntitiesByIdsAndType(integrations.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.INTEGRATION);
    }

    private void validateIntegrationConfigurationUpdate(Integration savedIntegration) throws JsonProcessingException {
        ObjectNode updatedIntegrationConfig = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        updatedIntegrationConfig.putObject("metadata").put("key", "val2");
        savedIntegration.setConfiguration(updatedIntegrationConfig);
        cloudRestClient.saveIntegration(savedIntegration);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    PageData<Integration> integrations = edgeRestClient.getIntegrations(new PageLink(100));
                    Integration integration = integrations.getData().get(0);
                    return updatedIntegrationConfig.equals(integration.getConfiguration());
                });

        verifyHttpIntegrationUpAndRunning(savedIntegration, "Device Converter val2");
    }

    private void validateEdgeAttributesUpdate(Integration savedIntegration) throws JsonProcessingException {
        ObjectNode updatedIntegrationConfig = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        updatedIntegrationConfig.putObject("metadata").put("key", "${{valAttr}}");
        savedIntegration.setConfiguration(updatedIntegrationConfig);
        cloudRestClient.saveIntegration(savedIntegration);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    PageData<Integration> integrations = edgeRestClient.getIntegrations(new PageLink(100));
                    Integration integration = integrations.getData().get(0);
                    return integration.getConfiguration().toString().contains("val3");
                });

        verifyHttpIntegrationUpAndRunning(savedIntegration, "Device Converter val3");

        JsonNode edgeAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"valAttr\":\"val4\", \"baseUrl\":\"" + edgeUrl + "\"}");
        cloudRestClient.saveEntityAttributesV1(edge.getId(), DataConstants.SERVER_SCOPE, edgeAttributes);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    PageData<Integration> integrations = edgeRestClient.getIntegrations(new PageLink(100));
                    Integration integration = integrations.getData().get(0);
                    return integration.getConfiguration().toString().contains("val4");
                });

        verifyHttpIntegrationUpAndRunning(savedIntegration, "Device Converter val4");
    }

    private void validateIntegrationDefaultConverterUpdate(Integration savedIntegration) throws JsonProcessingException {
        ObjectNode newConverterConfiguration = JacksonUtil.OBJECT_MAPPER.createObjectNode()
                .put("decoder", "return {deviceName: 'Device Converter val5', deviceType: 'default'};");
        Converter converter = new Converter();
        converter.setName("My new converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(newConverterConfiguration);
        converter.setEdgeTemplate(true);
        Converter newSavedConverter = cloudRestClient.saveConverter(converter);

        savedIntegration.setDefaultConverterId(newSavedConverter.getId());
        cloudRestClient.saveIntegration(savedIntegration);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    PageData<Integration> integrations = edgeRestClient.getIntegrations(new PageLink(100));
                    Integration integration = integrations.getData().get(0);
                    return newSavedConverter.getId().equals(integration.getDefaultConverterId());
                });

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getConverters(new PageLink(100)).getTotalElements() == 1);

        verifyHttpIntegrationUpAndRunning(savedIntegration, "Device Converter val5");
    }

    private void validateIntegrationDownlinkConverterUpdate(Integration savedIntegration) {
        ObjectNode downlinkConverterConfiguration = JacksonUtil.OBJECT_MAPPER.createObjectNode()
                .put("encoder", "return {contentType: 'JSON', data: '{\"pin\": 3}'};");
        Converter converter = new Converter();
        converter.setName("My downlink converter");
        converter.setType(ConverterType.DOWNLINK);
        converter.setConfiguration(downlinkConverterConfiguration);
        converter.setEdgeTemplate(true);
        Converter savedDownlinkConverter = cloudRestClient.saveConverter(converter);

        savedIntegration.setDownlinkConverterId(savedDownlinkConverter.getId());
        cloudRestClient.saveIntegration(savedIntegration);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    PageData<Integration> integrations = edgeRestClient.getIntegrations(new PageLink(100));
                    Integration integration = integrations.getData().get(0);
                    return savedDownlinkConverter.getId().equals(integration.getDownlinkConverterId());
                });

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getConverters(new PageLink(100)).getTotalElements() == 2);

        addIntegrationDownlinkRuleNodeToEdgeRootRuleChain(savedIntegration);

        sendRpcRequestToDevice();

        sendHttpUplinkAndVerifyDownlink(savedIntegration);

        savedIntegration.setDownlinkConverterId(null);
        cloudRestClient.saveIntegration(savedIntegration);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getConverters(new PageLink(100)).getTotalElements() == 1);
    }

    private void sendHttpUplinkAndVerifyDownlink(Integration savedIntegration) {
        ObjectNode values = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        values.put("result", "ok");
        ResponseEntity<JsonNode> responseEntityResponseEntity =
                edgeRestClient.getRestTemplate().postForEntity(
                        edgeUrl + "/api/v1/integrations/http/" + savedIntegration.getRoutingKey(),
                        values,
                        JsonNode.class);
        Assert.assertNotNull(responseEntityResponseEntity.getBody());
        Assert.assertEquals(3, responseEntityResponseEntity.getBody().get("pin").asInt());
    }

    private void sendRpcRequestToDevice() {
        Optional<Device> tenantDeviceOpt = edgeRestClient.getTenantDevice("Device Converter val5");
        Assert.assertTrue(tenantDeviceOpt.isPresent());
        Device device = tenantDeviceOpt.get();

        ObjectNode rpcRequest = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        rpcRequest.put("method", "rpcCommand");
        rpcRequest.set("params", JacksonUtil.OBJECT_MAPPER.createObjectNode());
        rpcRequest.put("persistent", false);
        rpcRequest.put("timeout", 5000);
        edgeRestClient.handleOneWayDeviceRPCRequest(device.getId(), rpcRequest);

        try {
            SECONDS.sleep(1); // wait for rpc request to be stored in downlink cache
        } catch (Throwable ignored) {}
    }

    private void addIntegrationDownlinkRuleNodeToEdgeRootRuleChain(Integration savedIntegration) {
        RuleChain rootRuleChain = getEdgeRootRuleChain();
        Assert.assertNotNull(rootRuleChain);

        addIntegrationDownlinkRuleNode(rootRuleChain, savedIntegration);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> {
                    Optional<RuleChainMetaData> ruleChainMetaDataOpt = edgeRestClient.getRuleChainMetaData(rootRuleChain.getId());
                    if (ruleChainMetaDataOpt.isPresent()) {
                        RuleChainMetaData ruleChainMetaData = ruleChainMetaDataOpt.get();
                        for (RuleNode node : ruleChainMetaData.getNodes()) {
                            if (node.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode")) {
                                return true;
                            }
                        }
                    }
                    return false;
                });

        try {
            SECONDS.sleep(1); // wait for edge root rule chain to be restarted with updated config
        } catch (Throwable ignored) {}
    }

    private void addIntegrationDownlinkRuleNode(RuleChain rootRuleChain, Integration savedIntegration) {
        Optional<RuleChainMetaData> ruleChainMetaDataOpt = cloudRestClient.getRuleChainMetaData(rootRuleChain.getId());
        if (ruleChainMetaDataOpt.isPresent()) {
            RuleChainMetaData ruleChainMetaData = ruleChainMetaDataOpt.get();

            int msgTypeSwitchIdx = 0;
            for (RuleNode node : ruleChainMetaData.getNodes()) {
                if (node.getType().equals("org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode")) {
                    break;
                }
                msgTypeSwitchIdx = msgTypeSwitchIdx + 1;
            }

            RuleNode ruleNode = new RuleNode();
            ruleNode.setName("Integration Downlink");
            ruleNode.setType("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode");
            ruleNode.setDebugMode(true);
            ObjectNode configuration = JacksonUtil.OBJECT_MAPPER.createObjectNode();
            configuration.put("integrationId", savedIntegration.getId().getId().toString());
            ruleNode.setConfiguration(configuration);

            ObjectNode additionalInfo = JacksonUtil.OBJECT_MAPPER.createObjectNode();
            additionalInfo.put("layoutX", 514);
            additionalInfo.put("layoutY", 511);
            additionalInfo.put("additionalInfo", "");
            ruleNode.setAdditionalInfo(additionalInfo);

            ruleNode.setRuleChainId(rootRuleChain.getId());
            ruleChainMetaData.getNodes().add(ruleNode);

            NodeConnectionInfo e = new NodeConnectionInfo();
            e.setType("RPC Request to Device");
            e.setFromIndex(msgTypeSwitchIdx);
            e.setToIndex(ruleChainMetaData.getNodes().size() - 1);
            ruleChainMetaData.getConnections().add(e);

            cloudRestClient.saveRuleChainMetaData(ruleChainMetaData);
        }
    }

    private RuleChain getEdgeRootRuleChain() {
        PageData<RuleChain> edgeRuleChains = cloudRestClient.getEdgeRuleChains(edge.getId(), new PageLink(1024));
        for (RuleChain edgeRuleChain : edgeRuleChains.getData()) {
            if (edgeRuleChain.isRoot()) {
                return edgeRuleChain;
            }
        }
        return null;
    }

    private void validateIntegrationUnassignFromEdge(Integration savedIntegration) {
        cloudRestClient.unassignIntegrationFromEdge(edge.getId(), savedIntegration.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getIntegrations(new PageLink(100)).getTotalElements() == 0);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getConverters(new PageLink(100)).getTotalElements() == 0);
    }

    private void validateRemoveOfIntegration(Integration savedIntegration) {
        cloudRestClient.deleteIntegration(savedIntegration.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getIntegrations(new PageLink(100), true).getTotalElements() == 0);
    }

    @Test
    public void testQueues() throws Exception {
        cloudRestClient.login("sysadmin@thingsboard.org", "sysadmin");

        // 1 - validate create
        Queue queue = new Queue();
        queue.setName("EdgeMain");
        queue.setTopic("tb_rule_engine.EdgeMain");
        queue.setPollInterval(25);
        queue.setPartitions(10);
        queue.setConsumerPerPartition(false);
        queue.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.7);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue.setProcessingStrategy(processingStrategy);
        Queue savedQueue = cloudRestClient.saveQueue(queue, "TB_RULE_ENGINE");
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getQueuesByServiceType("TB_RULE_ENGINE", new PageLink(100)).getTotalElements() == 4);
        PageData<Queue> pageData = edgeRestClient.getQueuesByServiceType("TB_RULE_ENGINE", new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.QUEUE);

        // 2 - validate update
        savedQueue.setPollInterval(50);
        cloudRestClient.saveQueue(savedQueue, "TB_RULE_ENGINE");
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getQueueById(savedQueue.getId()).getPollInterval() == 50);

        // 3 - validate delete
        cloudRestClient.deleteQueue(savedQueue.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getQueuesByServiceType("TB_RULE_ENGINE", new PageLink(100)).getTotalElements() == 3);

        cloudRestClient.login("tenant@thingsboard.org", "tenant");
    }

    // Utility methods
    private Device saveDeviceOnEdge(String deviceName, String type) throws Exception {
        return saveDevice(deviceName, type, null, edgeRestClient);
    }

    private Device saveDeviceOnCloud(String deviceName, String type) throws Exception {
        return saveDevice(deviceName, type, null, cloudRestClient);
    }

    private Device saveDeviceOnCloud(String deviceName, String type, EntityGroupId entityGroupId) throws Exception {
        return saveDevice(deviceName, type, entityGroupId, cloudRestClient);
    }

    private Device saveAndAssignDeviceToEdge(EntityGroup savedDeviceEntityGroup) throws Exception {
        Device device = saveDeviceOnCloud(RandomStringUtils.randomAlphanumeric(15), "default", savedDeviceEntityGroup.getId());
        cloudRestClient.assignEntityGroupToEdge(edge.getId(), savedDeviceEntityGroup.getId(), EntityType.DEVICE);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getDeviceById(device.getId()).isPresent());

        return device;
    }

    private EntityGroup createEntityGroup(EntityType entityType) {
        EntityGroup assetEntityGroup = new EntityGroup();
        assetEntityGroup.setType(entityType);
        assetEntityGroup.setName(RandomStringUtils.randomAlphanumeric(15));
        return cloudRestClient.saveEntityGroup(assetEntityGroup);
    }

    private Asset saveAndAssignAssetToEdge(EntityGroup savedAssetEntityGroup) throws Exception {
        Asset asset = saveAssetOnCloud(RandomStringUtils.randomAlphanumeric(15), "Building", savedAssetEntityGroup.getId());
        cloudRestClient.assignEntityGroupToEdge(edge.getId(), savedAssetEntityGroup.getId(), EntityType.ASSET);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS).
                until(() -> edgeRestClient.getAssetById(asset.getId()).isPresent());

        return asset;
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
        Asset savedAsset = cloudRestClient.saveAsset(asset);
        if (entityGroupId != null) {
            cloudRestClient.addEntitiesToEntityGroup(entityGroupId, Arrays.asList(savedAsset.getId()));
        }
        return savedAsset;
    }

    private Dashboard saveDashboardOnCloud(String dashboardTitle, EntityGroupId entityGroupId) throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(dashboardTitle);
        Dashboard savedDashboard = cloudRestClient.saveDashboard(dashboard);
        if (entityGroupId != null) {
            cloudRestClient.addEntitiesToEntityGroup(entityGroupId, Arrays.asList(savedDashboard.getId()));
        }
        return savedDashboard;
    }

    private EntityView saveEntityViewOnCloud(String entityViewName, String type, DeviceId deviceId, EntityGroupId entityGroupId) throws Exception {
        EntityView entityView = new EntityView();
        entityView.setName(entityViewName);
        entityView.setType(type);
        entityView.setEntityId(deviceId);
        EntityView savedEntityView = cloudRestClient.saveEntityView(entityView);
        if (entityGroupId != null) {
            cloudRestClient.addEntitiesToEntityGroup(entityGroupId, Arrays.asList(savedEntityView.getId()));
        }
        return savedEntityView;
    }



}

