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
package org.thingsboard.server.msa.edge;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.PowerSavingConfiguration;
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
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.impl.TelemetryQueryingSnmpCommunicationConfig;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;

@Slf4j
public class DeviceProfileClientTest extends AbstractContainerTest {

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
    public void testDeviceProfiles() throws Exception {
        verifyDeviceProfilesOnEdge(3);

        // create device profile
        DeviceProfile oneMoreDeviceProfile = createCustomDeviceProfile("ONE_MORE_DEVICE_PROFILE");

        verifyDeviceProfilesOnEdge(4);

        DeviceProfile snmpDeviceProfile = createCustomDeviceProfile("SNMP", createSnmpDeviceProfileTransportConfiguration());

        verifyDeviceProfilesOnEdge(5);

        DeviceProfile lwm2mDeviceProfile = createCustomDeviceProfile("LWM2M", createLwm2mDeviceProfileTransportConfiguration());

        verifyDeviceProfilesOnEdge(6);

        DeviceProfile coapDeviceProfile = createCustomDeviceProfile("COAP", createCoapDeviceProfileTransportConfiguration());

        verifyDeviceProfilesOnEdge(7);

        // update device profile
        OtaPackageId firmwarePackageId = createOtaPackageInfo(oneMoreDeviceProfile.getId(), FIRMWARE);
        oneMoreDeviceProfile.setFirmwareId(firmwarePackageId);

        OtaPackageId softwarePackageId = createOtaPackageInfo(oneMoreDeviceProfile.getId(), SOFTWARE);
        oneMoreDeviceProfile.setSoftwareId(softwarePackageId);

        EntityGroup dashboardGroup = createEntityGroup(EntityType.DASHBOARD);
        DashboardId dashboardId = createDashboardAndAssignToEdge("Device Profile Test Dashboard", dashboardGroup);
        RuleChainId savedRuleChainId = createRuleChainAndAssignToEdge("Device Profile Test RuleChain");
        oneMoreDeviceProfile.setDefaultDashboardId(dashboardId);
        oneMoreDeviceProfile.setName("ONE_MORE_DEVICE_PROFILE_UPDATED");
        oneMoreDeviceProfile.setDefaultEdgeRuleChainId(savedRuleChainId);
        cloudRestClient.saveDeviceProfile(oneMoreDeviceProfile);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "ONE_MORE_DEVICE_PROFILE_UPDATED".equals(edgeRestClient.getDeviceProfileById(oneMoreDeviceProfile.getId()).get().getName()));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> dashboardId.equals(edgeRestClient.getDeviceProfileById(oneMoreDeviceProfile.getId()).get().getDefaultDashboardId()));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> firmwarePackageId.equals(edgeRestClient.getDeviceProfileById(oneMoreDeviceProfile.getId()).get().getFirmwareId()));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> softwarePackageId.equals(edgeRestClient.getDeviceProfileById(oneMoreDeviceProfile.getId()).get().getSoftwareId()));

        // delete device profile
        cloudRestClient.deleteDeviceProfile(oneMoreDeviceProfile.getId());
        cloudRestClient.deleteDeviceProfile(snmpDeviceProfile.getId());
        cloudRestClient.deleteDeviceProfile(coapDeviceProfile.getId());
        cloudRestClient.deleteDeviceProfile(lwm2mDeviceProfile.getId());

        verifyDeviceProfilesOnEdge(3);

        unAssignFromEdgeAndDeleteDashboard(dashboardId, dashboardGroup.getId());
        unAssignFromEdgeAndDeleteRuleChain(savedRuleChainId);
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
                .atMost(30, TimeUnit.SECONDS)
                .until(() ->  edgeRestClient.getDeviceProfiles(new PageLink(100)).getTotalElements() == expectedDeviceProfilesCnt);

        PageData<DeviceProfile> pageData = edgeRestClient.getDeviceProfiles(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.DEVICE_PROFILE);
    }

}

