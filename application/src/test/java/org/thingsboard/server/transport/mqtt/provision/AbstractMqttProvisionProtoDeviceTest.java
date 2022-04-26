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
package org.thingsboard.server.transport.mqtt.provision;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.gen.transport.TransportProtos.CredentialsDataProto;
import org.thingsboard.server.gen.transport.TransportProtos.CredentialsType;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceCredentialsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateBasicMqttCredRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractMqttProvisionProtoDeviceTest extends AbstractMqttIntegrationTest {

    @Autowired
    DeviceCredentialsService deviceCredentialsService;

    @Autowired
    DeviceService deviceService;

    @After
    public void afterTest() throws Exception {
        super.processAfterTest();
    }

    @Test
    public void testProvisioningDisabledDevice() throws Exception {
        processTestProvisioningDisabledDevice();
    }

    @Test
    public void testProvisioningCheckPreProvisionedDevice() throws Exception {
        processTestProvisioningCheckPreProvisionedDevice();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        processTestProvisioningCreateNewDeviceWithoutCredentials();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        processTestProvisioningCreateNewDeviceWithAccessToken();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithCert() throws Exception {
        processTestProvisioningCreateNewDeviceWithCert();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithMqttBasic() throws Exception {
        processTestProvisioningCreateNewDeviceWithMqttBasic();
    }

    @Test
    public void testProvisioningWithBadKeyDevice() throws Exception {
        processTestProvisioningWithBadKeyDevice();
    }


    protected void processTestProvisioningDisabledDevice() throws Exception {
        super.processBeforeTest("Test Provision device", "Test Provision gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, null, null, null, DeviceProfileProvisionType.DISABLED, false, false, false);
        ProvisionDeviceResponseMsg result = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish().getPayloadBytes());
        Assert.assertNotNull(result);
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), result.getStatus().toString());
    }

    protected void processTestProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        super.processBeforeTest("Test Provision device3", "Test Provision gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, null, "testProvisionKey", "testProvisionSecret", DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES, false, false, false);
        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish().getPayloadBytes());

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(savedTenant.getTenantId(), "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    protected void processTestProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        super.processBeforeTest("Test Provision device3", "Test Provision gateway", TransportPayloadType.PROTOBUF, null, null,null, null, null, null, "testProvisionKey", "testProvisionSecret", DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES, false, false, false);
        CredentialsDataProto requestCredentials = CredentialsDataProto.newBuilder().setValidateDeviceTokenRequestMsg(ValidateDeviceTokenRequestMsg.newBuilder().setToken("test_token").build()).build();

        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish(createTestsProvisionMessage(CredentialsType.ACCESS_TOKEN, requestCredentials)).getPayloadBytes());

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(savedTenant.getTenantId(), "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(deviceCredentials.getCredentialsType(), DeviceCredentialsType.ACCESS_TOKEN);
        Assert.assertEquals(deviceCredentials.getCredentialsId(), "test_token");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    protected void processTestProvisioningCreateNewDeviceWithCert() throws Exception {
        super.processBeforeTest("Test Provision device3", "Test Provision gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, null, "testProvisionKey", "testProvisionSecret", DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES, false, false, false);
        CredentialsDataProto requestCredentials = CredentialsDataProto.newBuilder().setValidateDeviceX509CertRequestMsg(ValidateDeviceX509CertRequestMsg.newBuilder().setHash("testHash").build()).build();

        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish(createTestsProvisionMessage(CredentialsType.X509_CERTIFICATE, requestCredentials)).getPayloadBytes());

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(savedTenant.getTenantId(), "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(deviceCredentials.getCredentialsType(), DeviceCredentialsType.X509_CERTIFICATE);

        String cert = EncryptionUtil.certTrimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);

        Assert.assertEquals(deviceCredentials.getCredentialsId(), sha3Hash);

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), "testHash");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    protected void processTestProvisioningCreateNewDeviceWithMqttBasic() throws Exception {
        super.processBeforeTest("Test Provision device3", "Test Provision gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, null, "testProvisionKey", "testProvisionSecret", DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES, false, false, false);
        CredentialsDataProto requestCredentials = CredentialsDataProto.newBuilder().setValidateBasicMqttCredRequestMsg(
                ValidateBasicMqttCredRequestMsg.newBuilder()
                        .setClientId("test_clientId")
                        .setUserName("test_username")
                        .setPassword("test_password")
                    .build()
        ).build();

        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish(createTestsProvisionMessage(CredentialsType.MQTT_BASIC, requestCredentials)).getPayloadBytes());

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(savedTenant.getTenantId(), "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(deviceCredentials.getCredentialsType(), DeviceCredentialsType.MQTT_BASIC);
        Assert.assertEquals(deviceCredentials.getCredentialsId(), EncryptionUtil.getSha3Hash("|", "test_clientId", "test_username"));

        BasicMqttCredentials mqttCredentials = new BasicMqttCredentials();
        mqttCredentials.setClientId("test_clientId");
        mqttCredentials.setUserName("test_username");
        mqttCredentials.setPassword("test_password");

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), JacksonUtil.toString(mqttCredentials));
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    protected void processTestProvisioningCheckPreProvisionedDevice() throws Exception {
        super.processBeforeTest("Test Provision device", "Test Provision gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, null, "testProvisionKey", "testProvisionSecret", DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES, false, false, false);
        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish().getPayloadBytes());

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), savedDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    protected void processTestProvisioningWithBadKeyDevice() throws Exception {
        super.processBeforeTest("Test Provision device", "Test Provision gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, null, "testProvisionKeyOrig", "testProvisionSecret", DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES, false, false, false);
        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish().getPayloadBytes());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.getStatus().toString());
    }

    protected TestMqttCallback createMqttClientAndPublish() throws Exception {
        byte[] provisionRequestMsg = createTestProvisionMessage();
        return createMqttClientAndPublish(provisionRequestMsg);
    }

    protected TestMqttCallback createMqttClientAndPublish(byte[] provisionRequestMsg) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient("provision");
        TestMqttCallback onProvisionCallback = getTestMqttCallback();
        client.setCallback(onProvisionCallback);
        client.subscribe(MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC, MqttQoS.AT_MOST_ONCE.value());
        Thread.sleep(2000);
        client.publish(MqttTopics.DEVICE_PROVISION_REQUEST_TOPIC, new MqttMessage(provisionRequestMsg));
        onProvisionCallback.getLatch().await(3, TimeUnit.SECONDS);
        return onProvisionCallback;
    }


    protected TestMqttCallback getTestMqttCallback() {
        CountDownLatch latch = new CountDownLatch(1);
        return new TestMqttCallback(latch);
    }


    protected static class TestMqttCallback implements MqttCallback {

        private final CountDownLatch latch;
        private Integer qoS;
        private byte[] payloadBytes;

        TestMqttCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        public int getQoS() {
            return qoS;
        }

        public byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }

    protected byte[] createTestsProvisionMessage(CredentialsType credentialsType, CredentialsDataProto credentialsData) throws Exception {
        return ProvisionDeviceRequestMsg.newBuilder()
                .setDeviceName("Test Provision device")
                .setCredentialsType(credentialsType != null ? credentialsType : CredentialsType.ACCESS_TOKEN)
                .setCredentialsDataProto(credentialsData != null ? credentialsData: CredentialsDataProto.newBuilder().build())
                .setProvisionDeviceCredentialsMsg(
                        ProvisionDeviceCredentialsMsg.newBuilder()
                                .setProvisionDeviceKey("testProvisionKey")
                                .setProvisionDeviceSecret("testProvisionSecret")
                ).build()
                .toByteArray();
    }


    protected byte[] createTestProvisionMessage() throws Exception {
        return createTestsProvisionMessage(null, null);
    }

}
