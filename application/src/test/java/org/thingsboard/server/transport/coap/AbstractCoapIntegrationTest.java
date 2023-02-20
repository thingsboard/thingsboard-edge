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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.EfentoCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.transport.AbstractTransportIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestPropertySource(properties = {
        "transport.coap.enabled=true",
})
@Slf4j
public abstract class AbstractCoapIntegrationTest extends AbstractTransportIntegrationTest {

    protected final byte[] EMPTY_PAYLOAD = new byte[0];
    protected CoapTestClient client;

    protected void processAfterTest() throws Exception {
        if (client != null) {
            client.disconnect();
        }
    }

    protected void processBeforeTest(CoapTestConfigProperties config) throws Exception {
        loginTenantAdmin();
        deviceProfile = createCoapDeviceProfile(config);
        assertNotNull(deviceProfile);
        savedDevice = createDevice(config.getDeviceName(), deviceProfile.getName());
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        assertNotNull(deviceCredentials);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);
    }

    protected DeviceProfile createCoapDeviceProfile(CoapTestConfigProperties config) throws Exception {
        CoapDeviceType coapDeviceType = config.getCoapDeviceType();
        if (coapDeviceType == null) {
            DeviceProfileInfo defaultDeviceProfileInfo = doGet("/api/deviceProfileInfo/default", DeviceProfileInfo.class);
            return doGet("/api/deviceProfile/" + defaultDeviceProfileInfo.getId().getId(), DeviceProfile.class);
        } else {
            TransportPayloadType transportPayloadType = config.getTransportPayloadType();
            DeviceProfile deviceProfile = new DeviceProfile();
            deviceProfile.setName(transportPayloadType.name());
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            DeviceProfileProvisionType provisionType = config.getProvisionType() != null ?
                    config.getProvisionType() : DeviceProfileProvisionType.DISABLED;
            deviceProfile.setProvisionType(provisionType);
            deviceProfile.setProvisionDeviceKey(config.getProvisionKey());
            deviceProfile.setDescription(transportPayloadType.name() + " Test");
            DeviceProfileData deviceProfileData = new DeviceProfileData();
            DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
            deviceProfile.setTransportType(DeviceTransportType.COAP);
            CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration = new CoapDeviceProfileTransportConfiguration();
            CoapDeviceTypeConfiguration coapDeviceTypeConfiguration;
            if (CoapDeviceType.DEFAULT.equals(coapDeviceType)) {
                DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = new DefaultCoapDeviceTypeConfiguration();
                TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
                if (TransportPayloadType.PROTOBUF.equals(transportPayloadType)) {
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
                    transportPayloadTypeConfiguration = protoTransportPayloadConfiguration;
                } else {
                    transportPayloadTypeConfiguration = new JsonTransportPayloadConfiguration();
                }
                defaultCoapDeviceTypeConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
                coapDeviceTypeConfiguration = defaultCoapDeviceTypeConfiguration;
            } else {
                coapDeviceTypeConfiguration = new EfentoCoapDeviceTypeConfiguration();
            }
            coapDeviceProfileTransportConfiguration.setCoapDeviceTypeConfiguration(coapDeviceTypeConfiguration);
            deviceProfileData.setTransportConfiguration(coapDeviceProfileTransportConfiguration);
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

    protected Device createDevice(String name, String type) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return doPost("/api/device", device, Device.class);
    }
}
