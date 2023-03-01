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
package org.thingsboard.server.transport.mqtt.mqttv5.claim;

import lombok.extern.slf4j.Slf4j;
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
import org.thingsboard.server.transport.mqtt.mqttv5.AbstractMqttV5Test;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_CLAIM_TOPIC;

@Slf4j
public abstract class AbstractMqttV5ClaimTest extends AbstractMqttV5Test {
    protected static final String CUSTOMER_USER_PASSWORD = "customerUser123!";

    protected User customerAdmin;
    protected Customer savedCustomer;

    protected void processTestClaimingDevice() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);
        byte[] payloadBytes;
        byte[] failurePayloadBytes;
        payloadBytes = "{\"secretKey\":\"value\", \"durationMs\":60000}".getBytes();
        failurePayloadBytes = "{\"secretKey\":\"value\", \"durationMs\":1}".getBytes();
        validateClaimResponse(client, payloadBytes, failurePayloadBytes);
    }

    protected void validateClaimResponse(MqttV5TestClient client, byte[] payloadBytes, byte[] failurePayloadBytes) throws Exception {
        client.publishAndWait(DEVICE_CLAIM_TOPIC, failurePayloadBytes);

        loginUser(customerAdmin.getName(), CUSTOMER_USER_PASSWORD);
        ClaimRequest claimRequest = new ClaimRequest("value");

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

        EntityGroupInfo customerAdmins = doGet("/api/entityGroup/" + EntityType.CUSTOMER + "/" + savedCustomer.getId().toString() + "/" + EntityType.USER + "/" + EntityGroup.GROUP_CUSTOMER_ADMINS_NAME,
                EntityGroupInfo.class);

        customerAdmin = createUser(user, CUSTOMER_USER_PASSWORD, customerAdmins.getId());

        assertNotNull(customerAdmin);
        assertEquals(customerAdmin.getCustomerId(), savedCustomer.getId());
    }
}
