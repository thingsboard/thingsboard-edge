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
package org.thingsboard.server.transport.mqtt.claim;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestClient;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_CLAIM_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_CLAIM_TOPIC;

@Slf4j
@DaoSqlTest
public class MqttClaimDeviceTest extends AbstractMqttIntegrationTest {

    protected static final String CUSTOMER_USER_PASSWORD = "customerUser123!";

    protected User customerAdmin;
    protected Customer savedCustomer;

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Claim device")
                .gatewayName("Test Claim gateway")
                .build();
        processBeforeTest(configProperties);
        createCustomerAndUser();
    }

    protected void createCustomerAndUser() throws Exception {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Test Claiming Customer");
        savedCustomer = doPost("/api/customer", customer, Customer.class);
        assertNotNull(savedCustomer);
        assertEquals(tenantId, savedCustomer.getTenantId());

        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(tenantId);
        user.setCustomerId(savedCustomer.getId());
        user.setEmail("customer@thingsboard.org");

        EntityGroupInfo customerAdmins = doGet("/api/entityGroup/"+ EntityType.CUSTOMER +"/"+savedCustomer.getId().toString()+"/"+EntityType.USER+"/"+EntityGroup.GROUP_CUSTOMER_ADMINS_NAME,
                EntityGroupInfo.class);

        customerAdmin = createUser(user, CUSTOMER_USER_PASSWORD, customerAdmins.getId());

        assertNotNull(customerAdmin);
        assertEquals(customerAdmin.getCustomerId(), savedCustomer.getId());
    }

    @Test
    public void testClaimingDevice() throws Exception {
        processTestClaimingDevice(false);
    }

    @Test
    public void testClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestClaimingDevice(true);
    }

    @Test
    public void testGatewayClaimingDevice() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device", false);
    }

    @Test
    public void testGatewayClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device empty payload", true);
    }


    protected void processTestClaimingDevice(boolean emptyPayload) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        byte[] payloadBytes;
        byte[] failurePayloadBytes;
        if (emptyPayload) {
            payloadBytes = "{}".getBytes();
            failurePayloadBytes = "{\"durationMs\":1}".getBytes();
        } else {
            payloadBytes = "{\"secretKey\":\"value\", \"durationMs\":60000}".getBytes();
            failurePayloadBytes = "{\"secretKey\":\"value\", \"durationMs\":1}".getBytes();
        }
        validateClaimResponse(emptyPayload, client, payloadBytes, failurePayloadBytes);
    }

    protected void validateClaimResponse(boolean emptyPayload, MqttTestClient client, byte[] payloadBytes, byte[] failurePayloadBytes) throws Exception {
        client.publishAndWait(DEVICE_CLAIM_TOPIC, failurePayloadBytes);

        loginUser(customerAdmin.getName(), CUSTOMER_USER_PASSWORD);
        ClaimRequest claimRequest;
        if (!emptyPayload) {
            claimRequest = new ClaimRequest("value");
        } else {
            claimRequest = new ClaimRequest(null);
        }

        ClaimResponse claimResponse = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest()),
                20,
                100
        );

        assertEquals(claimResponse, ClaimResponse.FAILURE);

        client.publishAndWait(DEVICE_CLAIM_TOPIC, payloadBytes);
        client.disconnect();

        ClaimResult claimResult = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResult.class, status().isOk()),
                20,
                100
        );
        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        claimResponse = doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    protected void validateGatewayClaimResponse(String deviceName, boolean emptyPayload, MqttTestClient client, byte[] failurePayloadBytes, byte[] payloadBytes) throws Exception {
        client.publishAndWait(GATEWAY_CLAIM_TOPIC, failurePayloadBytes);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
                20,
                100
        );

        assertNotNull(savedDevice);

        loginUser(customerAdmin.getName(), CUSTOMER_USER_PASSWORD);
        ClaimRequest claimRequest;
        if (!emptyPayload) {
            claimRequest = new ClaimRequest("value");
        } else {
            claimRequest = new ClaimRequest(null);
        }

        ClaimResponse claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.FAILURE);

        client.publishAndWait(GATEWAY_CLAIM_TOPIC, payloadBytes);
        client.disconnect();

        ClaimResult claimResult = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResult.class, status().isOk()),
                20,
                100
        );

        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    protected void processTestGatewayClaimingDevice(String deviceName, boolean emptyPayload) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        byte[] failurePayloadBytes;
        byte[] payloadBytes;
        String failurePayload;
        String payload;
        if (emptyPayload) {
            failurePayload = "{\"" + deviceName + "\": " + "{\"durationMs\":1}" + "}";
            payload = "{\"" + deviceName + "\": " + "{}" + "}";
        } else {
            failurePayload = "{\"" + deviceName + "\": " + "{\"secretKey\":\"value\", \"durationMs\":1}" + "}";
            payload = "{\"" + deviceName + "\": " + "{\"secretKey\":\"value\", \"durationMs\":60000}" + "}";
        }
        payloadBytes = payload.getBytes();
        failurePayloadBytes = failurePayload.getBytes();
        validateGatewayClaimResponse(deviceName, emptyPayload, client, failurePayloadBytes, payloadBytes);
    }

    protected void processProtoTestGatewayClaimDevice(String deviceName, boolean emptyPayload) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        byte[] failurePayloadBytes;
        byte[] payloadBytes;
        if (emptyPayload) {
            payloadBytes = getGatewayClaimMsg(deviceName, 0, emptyPayload).toByteArray();
        } else {
            payloadBytes = getGatewayClaimMsg(deviceName, 60000, emptyPayload).toByteArray();
        }
        failurePayloadBytes = getGatewayClaimMsg(deviceName, 1, emptyPayload).toByteArray();

        validateGatewayClaimResponse(deviceName, emptyPayload, client, failurePayloadBytes, payloadBytes);
    }

    private TransportApiProtos.GatewayClaimMsg getGatewayClaimMsg(String deviceName, long duration, boolean emptyPayload) {
        TransportApiProtos.GatewayClaimMsg.Builder gatewayClaimMsgBuilder = TransportApiProtos.GatewayClaimMsg.newBuilder();
        TransportApiProtos.ClaimDeviceMsg.Builder claimDeviceMsgBuilder = TransportApiProtos.ClaimDeviceMsg.newBuilder();
        TransportApiProtos.ClaimDevice.Builder claimDeviceBuilder = TransportApiProtos.ClaimDevice.newBuilder();
        if (!emptyPayload) {
            claimDeviceBuilder.setSecretKey("value");
        }
        if (duration > 0) {
            claimDeviceBuilder.setDurationMs(duration);
        }
        TransportApiProtos.ClaimDevice claimDevice = claimDeviceBuilder.build();
        claimDeviceMsgBuilder.setClaimRequest(claimDevice);
        claimDeviceMsgBuilder.setDeviceName(deviceName);
        TransportApiProtos.ClaimDeviceMsg claimDeviceMsg = claimDeviceMsgBuilder.build();
        gatewayClaimMsgBuilder.addMsg(claimDeviceMsg);
        return gatewayClaimMsgBuilder.build();
    }

}
