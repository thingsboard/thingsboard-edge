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
package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.TestCoapClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
public class CoapClientTest  extends AbstractContainerTest {
    private TestCoapClient client;

    private Device device;
    @BeforeMethod
    public void setUp() throws Exception {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("http_"));
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
    }

    @Test
    public void provisionRequestForDeviceWithPreProvisionedStrategy() throws Exception {

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());
        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        JsonNode provisionResponse = JacksonUtil.fromBytes(createCoapClientAndPublish(device.getName()));

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    @Test
    public void provisionRequestForDeviceWithAllowToCreateNewDevicesStrategy() throws Exception {

        String testDeviceName = "test_provision_device";

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());

        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);

        JsonNode provisionResponse = JacksonUtil.fromBytes(createCoapClientAndPublish(testDeviceName));

        testRestClient.deleteDeviceIfExists(device.getId());
        device = testRestClient.getDeviceByName(testDeviceName);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    @Test
    public void provisionRequestForDeviceWithDisabledProvisioningStrategy() throws Exception {

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);

        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish(null));

        assertThat(response.get("status").asText()).isEqualTo("NOT_FOUND");
    }

    private byte[] createCoapClientAndPublish(String deviceName) throws Exception {
        String provisionRequestMsg = createTestProvisionMessage(deviceName);
        client = new TestCoapClient(TestCoapClient.getFeatureTokenUrl(FeatureType.PROVISION));
        return client.postMethod(provisionRequestMsg.getBytes()).getPayload();
    }

    private String createTestProvisionMessage(String deviceName) {
        ObjectNode provisionRequest = JacksonUtil.newObjectNode();
        provisionRequest.put("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.put("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        if (deviceName != null) {
            provisionRequest.put("deviceName", deviceName);
        }
        return provisionRequest.toString();
    }

}
