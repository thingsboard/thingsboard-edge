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
package org.thingsboard.server.transport.mqtt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.AbstractTransportIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestPropertySource(properties = {
        "service.integrations.supported=ALL",
        "transport.mqtt.enabled=true",
})
@Slf4j
public abstract class AbstractMqttIntegrationTest extends AbstractTransportIntegrationTest {

    protected Device savedGateway;
    protected String gatewayAccessToken;

    protected void processBeforeTest(MqttTestConfigProperties config) throws Exception {
        loginTenantAdmin();
        deviceProfile = createMqttDeviceProfile(config);
        assertNotNull(deviceProfile);
        if (config.getDeviceName() != null) {
            savedDevice = createDevice(config.getDeviceName(), deviceProfile.getName(), false);
            DeviceCredentials deviceCredentials =
                    doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
            assertNotNull(deviceCredentials);
            assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
            accessToken = deviceCredentials.getCredentialsId();
            assertNotNull(accessToken);
        }
        if (config.getGatewayName() != null) {
            savedGateway = createDevice(config.getGatewayName(), deviceProfile.getName(), true);
            DeviceCredentials gatewayCredentials =
                    doGet("/api/device/" + savedGateway.getId().getId().toString() + "/credentials", DeviceCredentials.class);
            assertNotNull(gatewayCredentials);
            assertEquals(savedGateway.getId(), gatewayCredentials.getDeviceId());
            gatewayAccessToken = gatewayCredentials.getCredentialsId();
            assertNotNull(gatewayAccessToken);
        }
    }

    protected DeviceProfile createMqttDeviceProfile(MqttTestConfigProperties config) throws Exception {
        TransportPayloadType transportPayloadType = config.getTransportPayloadType();
        if (transportPayloadType == null) {
            DeviceProfileInfo defaultDeviceProfileInfo = doGet("/api/deviceProfileInfo/default", DeviceProfileInfo.class);
            return doGet("/api/deviceProfile/" + defaultDeviceProfileInfo.getId().getId(), DeviceProfile.class);
        } else {
            DeviceProfile deviceProfile = new DeviceProfile();
            deviceProfile.setName(transportPayloadType.name());
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            deviceProfile.setTransportType(DeviceTransportType.MQTT);
            DeviceProfileProvisionType provisionType = config.getProvisionType() != null ?
                    config.getProvisionType() : DeviceProfileProvisionType.DISABLED;
            deviceProfile.setProvisionType(provisionType);
            deviceProfile.setProvisionDeviceKey(config.getProvisionKey());
            deviceProfile.setDescription(transportPayloadType.name() + " Test");
            DeviceProfileData deviceProfileData = new DeviceProfileData();
            DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
            MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = new MqttDeviceProfileTransportConfiguration();
            if (StringUtils.hasLength(config.getTelemetryTopicFilter())) {
                mqttDeviceProfileTransportConfiguration.setDeviceTelemetryTopic(config.getTelemetryTopicFilter());
            }
            if (StringUtils.hasLength(config.getAttributesTopicFilter())) {
                mqttDeviceProfileTransportConfiguration.setDeviceAttributesTopic(config.getAttributesTopicFilter());
            }
            mqttDeviceProfileTransportConfiguration.setSparkplug(config.isSparkplug());
            mqttDeviceProfileTransportConfiguration.setSparkplugAttributesMetricNames(config.sparkplugAttributesMetricNames);
            mqttDeviceProfileTransportConfiguration.setSendAckOnValidationException(config.isSendAckOnValidationException());
            TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
            if (TransportPayloadType.JSON.equals(transportPayloadType)) {
                transportPayloadTypeConfiguration = new JsonTransportPayloadConfiguration();
            } else {
                ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = new ProtoTransportPayloadConfiguration();
                String telemetryProtoSchema = config.getTelemetryProtoSchema();
                String attributesProtoSchema = config.getAttributesProtoSchema();
                String rpcResponseProtoSchema = config.getRpcResponseProtoSchema();
                String rpcRequestProtoSchema = config.getRpcRequestProtoSchema();
                protoTransportPayloadConfiguration.setDeviceTelemetryProtoSchema(
                        telemetryProtoSchema != null ? telemetryProtoSchema : DEVICE_TELEMETRY_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setDeviceAttributesProtoSchema(
                        attributesProtoSchema != null ? attributesProtoSchema : DEVICE_ATTRIBUTES_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setDeviceRpcResponseProtoSchema(
                        rpcResponseProtoSchema != null ? rpcResponseProtoSchema : DEVICE_RPC_RESPONSE_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setDeviceRpcRequestProtoSchema(
                        rpcRequestProtoSchema != null ? rpcRequestProtoSchema : DEVICE_RPC_REQUEST_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setEnableCompatibilityWithJsonPayloadFormat(
                        config.isEnableCompatibilityWithJsonPayloadFormat()
                );
                protoTransportPayloadConfiguration.setUseJsonPayloadFormatForDefaultDownlinkTopics(
                        config.isEnableCompatibilityWithJsonPayloadFormat() &&
                                config.isUseJsonPayloadFormatForDefaultDownlinkTopics()
                );
                transportPayloadTypeConfiguration = protoTransportPayloadConfiguration;
            }
            mqttDeviceProfileTransportConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
            deviceProfileData.setTransportConfiguration(mqttDeviceProfileTransportConfiguration);
            DeviceProfileProvisionConfiguration provisionConfiguration;
            switch (provisionType) {
                case ALLOW_CREATE_NEW_DEVICES:
                    provisionConfiguration = new AllowCreateNewDevicesDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
                case CHECK_PRE_PROVISIONED_DEVICES:
                    provisionConfiguration = new CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
                case DISABLED:
                default:
                    provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
            }
            deviceProfileData.setProvisionConfiguration(provisionConfiguration);
            deviceProfileData.setConfiguration(configuration);
            deviceProfile.setProfileData(deviceProfileData);
            deviceProfile.setDefault(false);
            deviceProfile.setDefaultRuleChainId(null);
            return doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        }
    }

    protected Device createDevice(String name, String type, boolean gateway) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        if (gateway) {
            ObjectNode additionalInfo = mapper.createObjectNode();
            additionalInfo.put("gateway", true);
            device.setAdditionalInfo(additionalInfo);
        }
        return doPost("/api/device", device, Device.class);
    }

    protected TransportProtos.PostAttributeMsg getPostAttributeMsg(List<String> expectedKeys) {
        List<TransportProtos.KeyValueProto> kvProtos = getKvProtos(expectedKeys);
        TransportProtos.PostAttributeMsg.Builder builder = TransportProtos.PostAttributeMsg.newBuilder();
        builder.addAllKv(kvProtos);
        return builder.build();
    }

    protected void subscribeAndWait(MqttTestClient client, String attrSubTopic, DeviceId deviceId, FeatureType featureType) throws MqttException {
        int subscriptionCount = getDeviceActorSubscriptionCount(deviceId, featureType);
        client.subscribeAndWait(attrSubTopic, MqttQoS.AT_MOST_ONCE);
        // TODO: This test awaits for the device actor to receive the subscription. Ideally it should not happen. See details below:
        // The transport layer acknowledge subscription request once the message about subscription is in the queue.
        // Test sends data immediately after acknowledgement.
        // But there is a time lag between push to the queue and read from the queue in the tb-core component.
        // Ideally, we should reply to device with SUBACK only when the device actor on the tb-core receives the message.
        awaitForDeviceActorToReceiveSubscription(deviceId, featureType, subscriptionCount + 1);
    }

    protected void subscribeAndCheckSubscription(MqttTestClient client, String attrSubTopic, DeviceId deviceId, FeatureType featureType) throws MqttException {
        client.subscribeAndWait(attrSubTopic, MqttQoS.AT_MOST_ONCE);
        // TODO: This test awaits for the device actor to receive the subscription. Ideally it should not happen. See details below:
        // The transport layer acknowledge subscription request once the message about subscription is in the queue.
        // Test sends data immediately after acknowledgement.
        // But there is a time lag between push to the queue and read from the queue in the tb-core component.
        // Ideally, we should reply to device with SUBACK only when the device actor on the tb-core receives the message.
        awaitForDeviceActorToReceiveSubscription(deviceId, featureType, 1);
    }
}
