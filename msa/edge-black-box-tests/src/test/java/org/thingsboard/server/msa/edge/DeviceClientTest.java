/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.MqttDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.transport.snmp.AuthenticationProtocol;
import org.thingsboard.server.common.data.transport.snmp.PrivacyProtocol;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;

@Slf4j
public class DeviceClientTest extends AbstractContainerTest {

    @Test
    public void testDevices() {
        performTestOnEachEdge(this::_testDevices);
    }

    private void _testDevices() {
        // create device #1, add to group #1 and assign group #1 to edge
        EntityGroup savedDeviceEntityGroup1 = createEntityGroup(EntityType.DEVICE);
        Device savedDevice1 = saveDeviceAndAssignEntityGroupToEdge("Remote Controller", savedDeviceEntityGroup1);
        DeviceId savedDevice1Id = savedDevice1.getId();

        // update device #1 credentials on cloud
        Optional<DeviceCredentials> deviceCredentialsByDeviceId =
                cloudRestClient.getDeviceCredentialsByDeviceId(savedDevice1Id);
        Assert.assertTrue(deviceCredentialsByDeviceId.isPresent());
        DeviceCredentials deviceCredentials = deviceCredentialsByDeviceId.get();
        deviceCredentials.setCredentialsId("UpdatedTokenCloudDevice");
        cloudRestClient.saveDeviceCredentials(deviceCredentials);
        verifyDeviceCredentialsOnCloudAndEdge(savedDevice1);

        // validate transport configurations
        savedDevice1 = validateDeviceTransportConfiguration(savedDevice1, cloudRestClient, edgeRestClient);

        // update device #1
        OtaPackageId firmwarePackageId = createOtaPackageInfo(savedDevice1.getDeviceProfileId(), FIRMWARE);
        savedDevice1.setFirmwareId(firmwarePackageId);

        OtaPackageId softwarePackageId = createOtaPackageInfo(savedDevice1.getDeviceProfileId(), SOFTWARE);
        savedDevice1.setSoftwareId(softwarePackageId);

        String updatedDeviceName = savedDevice1.getName() + "Updated";
        savedDevice1.setName(updatedDeviceName);
        cloudRestClient.saveDevice(savedDevice1);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> updatedDeviceName.equals(edgeRestClient.getDeviceById(savedDevice1Id).get().getName()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> firmwarePackageId.equals(edgeRestClient.getDeviceById(savedDevice1Id).get().getFirmwareId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> softwarePackageId.equals(edgeRestClient.getDeviceById(savedDevice1Id).get().getSoftwareId()));

        // save device #1 attribute
        cloudRestClient.saveDeviceAttributes(savedDevice1Id, DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"key1\":\"value1\"}"));
        cloudRestClient.saveDeviceAttributes(savedDevice1Id, DataConstants.SHARED_SCOPE, JacksonUtil.toJsonNode("{\"key2\":\"value2\"}"));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnEdge(savedDevice1Id, DataConstants.SERVER_SCOPE, "key1", "value1"));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnEdge(savedDevice1Id, DataConstants.SHARED_SCOPE, "key2", "value2"));

        // create device #2 inside group #1
        Device savedDevice2 = saveDeviceOnCloud(StringUtils.randomAlphanumeric(15), "Remote Controller", savedDeviceEntityGroup1.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceById(savedDevice2.getId()).isPresent());

        // add group #2 and assign to edge
        EntityGroup savedDeviceEntityGroup2 = createEntityGroup(EntityType.DEVICE);
        assignEntityGroupToEdge(savedDeviceEntityGroup2);

        // add device #2 to group #2
        cloudRestClient.addEntitiesToEntityGroup(savedDeviceEntityGroup2.getId(), Collections.singletonList(savedDevice2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> device2Groups = edgeRestClient.getEntityGroupsForEntity(savedDevice2.getId());
                    return device2Groups.contains(savedDeviceEntityGroup2.getId());
                });

        // add device #2 to group #1
        cloudRestClient.addEntitiesToEntityGroup(savedDeviceEntityGroup1.getId(), Collections.singletonList(savedDevice2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> device2Groups = edgeRestClient.getEntityGroupsForEntity(savedDevice2.getId());
                    return device2Groups.contains(savedDeviceEntityGroup1.getId());
                });

        // remove device #2 from group #1
        cloudRestClient.removeEntitiesFromEntityGroup(savedDeviceEntityGroup1.getId(), Collections.singletonList(savedDevice2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> device2Groups = edgeRestClient.getEntityGroupsForEntity(savedDevice2.getId());
                    return !device2Groups.contains(savedDeviceEntityGroup1.getId()) && device2Groups.contains(savedDeviceEntityGroup2.getId());
                });

        // remove device #2 from group #2
        cloudRestClient.removeEntitiesFromEntityGroup(savedDeviceEntityGroup2.getId(), Collections.singletonList(savedDevice2.getId()));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceById(savedDevice2.getId()).isEmpty());

        // delete device #2
        cloudRestClient.deleteDevice(savedDevice2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceById(savedDevice2.getId()).isEmpty());

        // unassign group #1 from edge
        cloudRestClient.unassignEntityGroupFromEdge(edge.getId(), savedDeviceEntityGroup1.getId(), EntityType.DEVICE);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedDeviceEntityGroup1.getId()).isEmpty());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceById(savedDevice1Id).isEmpty());

        // clean up
        cloudRestClient.deleteDevice(savedDevice1Id);
        cloudRestClient.deleteEntityGroup(savedDeviceEntityGroup1.getId());
        cloudRestClient.deleteEntityGroup(savedDeviceEntityGroup2.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedDeviceEntityGroup2.getId()).isEmpty());

        // delete "Remote Controller" device profile
        cloudRestClient.deleteDeviceProfile(savedDevice1.getDeviceProfileId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceProfileById(savedDevice2.getDeviceProfileId()).isEmpty());

    }

    @Test
    public void sendDeviceToCloud() {
        performTestOnEachEdge(this::_sendDeviceToCloud);
    }

    private void _sendDeviceToCloud() {
        // create device on edge
        Device savedDeviceOnEdge = saveDeviceOnEdge("Edge Device 2", "default");
        DeviceId savedDeviceOnEdgeId = savedDeviceOnEdge.getId();
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getDeviceById(savedDeviceOnEdgeId).isPresent());

        verifyDeviceCredentialsOnCloudAndEdge(savedDeviceOnEdge);

        // update device attributes
        edgeRestClient.saveDeviceAttributes(savedDeviceOnEdgeId, DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"key1\":\"value1\"}"));
        edgeRestClient.saveDeviceAttributes(savedDeviceOnEdgeId, DataConstants.SHARED_SCOPE, JacksonUtil.toJsonNode("{\"key2\":\"value2\"}"));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnCloud(savedDeviceOnEdgeId, DataConstants.SERVER_SCOPE, "key1", "value1"));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnCloud(savedDeviceOnEdgeId, DataConstants.SHARED_SCOPE, "key2", "value2"));

        // update device credentials on edge
        Optional<DeviceCredentials> deviceCredentialsByDeviceId =
                edgeRestClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdgeId);
        Assert.assertTrue(deviceCredentialsByDeviceId.isPresent());
        DeviceCredentials deviceCredentials = deviceCredentialsByDeviceId.get();
        deviceCredentials.setCredentialsId("UpdatedToken");
        edgeRestClient.saveDeviceCredentials(deviceCredentials);

        verifyDeviceCredentialsOnCloudAndEdge(savedDeviceOnEdge);

        // update device
        savedDeviceOnEdge.setName("Edge Device 2 Updated");
        edgeRestClient.saveDevice(savedDeviceOnEdge);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Edge Device 2 Updated".equals(cloudRestClient.getDeviceById(savedDeviceOnEdgeId).get().getName()));

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getEntityGroupsForEntity(savedDeviceOnEdgeId).size() == 2);

        validateDeviceTransportConfiguration(savedDeviceOnEdge, edgeRestClient, cloudRestClient);

        edgeRestClient.deleteDevice(savedDeviceOnEdgeId);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getEntityGroupsForEntity(savedDeviceOnEdgeId).size() == 1);

        cloudRestClient.deleteDevice(savedDeviceOnEdgeId);
    }

    private Device validateDeviceTransportConfiguration(Device device, RestClient sourceRestClient, RestClient targetRestClient) {
        device = validateDefaultDeviceTransportConfiguration(device, sourceRestClient, targetRestClient);
        device = validateMqttDeviceTransportConfiguration(device, sourceRestClient, targetRestClient);
        device = validateCoapDeviceTransportConfiguration(device, sourceRestClient, targetRestClient);
        device = validateLwm2mDeviceTransportConfiguration(device, sourceRestClient, targetRestClient);
        return validateSnmpDeviceTransportConfiguration(device, sourceRestClient, targetRestClient);
    }

    private Device validateDefaultDeviceTransportConfiguration(Device device, RestClient sourceRestClient, RestClient targetRestClient) {
        return setAndValidateDeviceTransportConfiguration(device,
                new DefaultDeviceTransportConfiguration(),
                sourceRestClient,
                targetRestClient);
    }

    private Device validateMqttDeviceTransportConfiguration(Device device, RestClient sourceRestClient, RestClient targetRestClient) {
        MqttDeviceTransportConfiguration transportConfiguration = new MqttDeviceTransportConfiguration();
        transportConfiguration.getProperties().put("topic", "tb_rule_engine.thermostat");
        return setAndValidateDeviceTransportConfiguration(device,
                transportConfiguration,
                sourceRestClient,
                targetRestClient);
    }

    private Device validateCoapDeviceTransportConfiguration(Device device, RestClient sourceRestClient, RestClient targetRestClient) {
        CoapDeviceTransportConfiguration transportConfiguration = new CoapDeviceTransportConfiguration();
        transportConfiguration.setEdrxCycle(1L);
        transportConfiguration.setPagingTransmissionWindow(2L);
        transportConfiguration.setPsmActivityTimer(3L);
        transportConfiguration.setPowerMode(PowerMode.DRX);
        return setAndValidateDeviceTransportConfiguration(device,
                transportConfiguration,
                sourceRestClient,
                targetRestClient);
    }

    private Device validateLwm2mDeviceTransportConfiguration(Device device, RestClient sourceRestClient, RestClient targetRestClient) {
        Lwm2mDeviceTransportConfiguration transportConfiguration = new Lwm2mDeviceTransportConfiguration();
        transportConfiguration.setEdrxCycle(1L);
        transportConfiguration.setPagingTransmissionWindow(2L);
        transportConfiguration.setPsmActivityTimer(3L);
        transportConfiguration.setPowerMode(PowerMode.PSM);
        return setAndValidateDeviceTransportConfiguration(device,
                transportConfiguration,
                sourceRestClient,
                targetRestClient);
    }

    private Device validateSnmpDeviceTransportConfiguration(Device device,
                                                            RestClient sourceRestClient,
                                                            RestClient targetRestClient) {
        SnmpDeviceTransportConfiguration transportConfiguration = new SnmpDeviceTransportConfiguration();
        transportConfiguration.setAuthenticationProtocol(AuthenticationProtocol.SHA_256);
        transportConfiguration.setPrivacyProtocol(PrivacyProtocol.AES_256);
        return setAndValidateDeviceTransportConfiguration(device,
                transportConfiguration,
                sourceRestClient,
                targetRestClient);
    }

    private Device setAndValidateDeviceTransportConfiguration(Device device,
                                                              DeviceTransportConfiguration transportConfiguration,
                                                              RestClient sourceRestClient,
                                                              RestClient targetRestClient) {
        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        deviceData.setTransportConfiguration(transportConfiguration);
        device.setDeviceData(deviceData);
        Device result = sourceRestClient.saveDevice(device);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<Device> targetDevice = targetRestClient.getDeviceById(device.getId());
                    Optional<Device> sourceDevice = sourceRestClient.getDeviceById(device.getId());
                    Device expected = targetDevice.get();
                    Device actual = sourceDevice.get();
                    cleanUpVersion(expected, actual);
                    return expected.equals(actual);
                });
        return result;
    }

    private void verifyDeviceCredentialsOnCloudAndEdge(Device savedDevice) {
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId()).isPresent());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId()).isPresent());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    DeviceCredentials deviceCredentialsOnEdge =
                            edgeRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId()).get();
                    DeviceCredentials deviceCredentialsOnCloud =
                            cloudRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId()).get();
                    // TODO: Edge-only:  potential fix for future releases
                    deviceCredentialsOnCloud.setId(null);
                    deviceCredentialsOnEdge.setId(null);
                    deviceCredentialsOnCloud.setCreatedTime(0);
                    deviceCredentialsOnEdge.setCreatedTime(0);
                    cleanUpVersion(deviceCredentialsOnCloud, deviceCredentialsOnCloud);
                    return deviceCredentialsOnCloud.equals(deviceCredentialsOnEdge);
                });
    }

    @Test
    public void testProvisionDevice() {
        performTestOnEachEdge(this::_testProvisionDevice);
    }

    private void _testProvisionDevice() {
        final String DEVICE_PROFILE_NAME = "Provision Device Profile";
        final String DEVICE_NAME = "Provisioned Device";

        DeviceProfile provisionDeviceProfile = new DeviceProfile();
        provisionDeviceProfile.setName(DEVICE_PROFILE_NAME);
        provisionDeviceProfile.setType(DeviceProfileType.DEFAULT);
        provisionDeviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        provisionDeviceProfile.setProvisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);
        provisionDeviceProfile.setProvisionDeviceKey("testProvisionKey");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        AllowCreateNewDevicesDeviceProfileProvisionConfiguration provisionConfiguration
                = new AllowCreateNewDevicesDeviceProfileProvisionConfiguration("testProvisionSecret");
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        provisionDeviceProfile.setProfileData(deviceProfileData);
        provisionDeviceProfile = cloudRestClient.saveDeviceProfile(provisionDeviceProfile);

        DeviceProfileId provisionDeviceProfileId = provisionDeviceProfile.getId();
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceProfileById(provisionDeviceProfileId).isPresent());

        Map<String, String> provisionRequest = new HashMap<>();
        provisionRequest.put("deviceName", DEVICE_NAME);
        provisionRequest.put("provisionDeviceKey", "testProvisionKey");
        provisionRequest.put("provisionDeviceSecret", "testProvisionSecret");
        ResponseEntity<JsonNode> provisionResponse = edgeRestClient.getRestTemplate()
                .postForEntity(edgeUrl + "/api/v1/provision",
                        provisionRequest,
                        JsonNode.class);

        Assert.assertNotNull(provisionResponse.getBody());
        Assert.assertNotNull(provisionResponse.getBody().get("status"));
        Assert.assertEquals("SUCCESS", provisionResponse.getBody().get("status").asText());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<Device> provisionDevices = cloudRestClient.getTenantDevices(DEVICE_PROFILE_NAME, new PageLink(100));
                    if (provisionDevices.getData().isEmpty()) {
                        return false;
                    }
                    List<Device> provisionDevicesData = provisionDevices.getData();
                    return provisionDevicesData.size() == 1 && provisionDevicesData.get(0).getName().equals(DEVICE_NAME);
                });

        DeviceId provisionedDeviceId = getDeviceByNameAndType(DEVICE_NAME, DEVICE_PROFILE_NAME, cloudRestClient).getId();
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<DeviceCredentials> edgeDeviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(provisionedDeviceId);
                    Optional<DeviceCredentials> cloudDeviceCredentials = cloudRestClient.getDeviceCredentialsByDeviceId(provisionedDeviceId);
                    return edgeDeviceCredentials.isPresent() &&
                            cloudDeviceCredentials.isPresent() &&
                            edgeDeviceCredentials.get().getCredentialsId().equals(cloudDeviceCredentials.get().getCredentialsId());
                });

        cloudRestClient.deleteDevice(provisionedDeviceId);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceById(provisionedDeviceId).isEmpty());

        cloudRestClient.deleteDeviceProfile(provisionDeviceProfile.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceProfileById(provisionDeviceProfileId).isEmpty());
    }

    private Device getDeviceByNameAndType(String deviceName, String type, RestClient restClient) {
        Device result = null;
        PageData<Device> provisionDevices = restClient.getTenantDevices(type, new PageLink(1000));
        for (Device device : provisionDevices.getData()) {
            if (device.getName().equals(deviceName)) {
                result = device;
                break;
            }
        }
        return result;
    }

    @Test
    public void testOneWayRpcCall() {
        performTestOnEachEdge(this::_testOneWayRpcCall);
    }

    private void _testOneWayRpcCall() {
        // create device on cloud and assign to edge
        Device device = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
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
        ObjectNode initialRequestBody = JacksonUtil.newObjectNode();
        initialRequestBody.put("method", "setGpio");
        initialRequestBody.put("params", "{\"pin\":\"23\", \"value\": 1}");
        initialRequestBody.put("timeout", "20000");
        cloudRestClient.handleOneWayDeviceRPCRequest(device.getId(), initialRequestBody);

        // verify that rpc request was received
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
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

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
    }

    @Test
    public void testTwoWayRpcCall() {
        performTestOnEachEdge(this::_testTwoWayRpcCall);
    }

    private void _testTwoWayRpcCall() {
        // create device on cloud and assign to edge
        Device device = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
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
        ObjectNode initialRequestBody = JacksonUtil.newObjectNode();
        initialRequestBody.put("method", "setGpio");
        initialRequestBody.put("params", "{\"pin\":\"23\", \"value\": 1}");

        final JsonNode[] rpcTwoWayRequest = new JsonNode[]{null};
        new Thread(() -> {
            rpcTwoWayRequest[0] = cloudRestClient.handleTwoWayDeviceRPCRequest(device.getId(), initialRequestBody);
        }).start();

        // verify that rpc request was received
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
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
        ObjectNode replyBody = JacksonUtil.newObjectNode();
        replyBody.put("result", "ok");

        String rpcReply = edgeUrl + "/api/v1/" + deviceCredentials.get().getCredentialsId() + "/rpc/" + rpcSubscriptionRequest[0].getBody().get("id");
        edgeRestClient.getRestTemplate().postForEntity(rpcReply, replyBody, Void.class);

        // verify on the cloud that rpc response was received
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    if (rpcTwoWayRequest[0] == null) {
                        return false;
                    }
                    JsonNode responseBody = rpcTwoWayRequest[0];
                    return "ok".equals(responseBody.get("result").textValue());
                });

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
    }

    @Test
    public void testClientRpcCallToCloud() {
        performTestOnEachEdge(this::_testClientRpcCallToCloud);
    }

    private void _testClientRpcCallToCloud() {
        // create device on cloud and assign to edge
        Device device = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<DeviceCredentials> edgeDeviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(device.getId());
                    Optional<DeviceCredentials> cloudDeviceCredentials = cloudRestClient.getDeviceCredentialsByDeviceId(device.getId());
                    return edgeDeviceCredentials.isPresent() &&
                            cloudDeviceCredentials.isPresent() &&
                            edgeDeviceCredentials.get().getCredentialsId().equals(cloudDeviceCredentials.get().getCredentialsId());
                });

        Optional<DeviceCredentials> deviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(device.getId());

        Assert.assertTrue(deviceCredentials.isPresent());

        // send request from device to cloud over edge
        ObjectNode requestBody = JacksonUtil.newObjectNode();
        requestBody.put("method", "getCurrentTime");
        requestBody.put("params", "{}");

        String rpcRequest = edgeUrl + "/api/v1/" + deviceCredentials.get().getCredentialsId() + "/rpc";
        ResponseEntity<String> responseEntity = edgeRestClient.getRestTemplate().postForEntity(rpcRequest, requestBody, String.class);

        Assert.assertNotNull(responseEntity.getBody());
        Assert.assertTrue(responseEntity.getBody().contains("currentTime"));

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
    }

    @Test
    public void sendDeviceWithNameThatAlreadyExistsOnCloud() {
        performTestOnEachEdge(this::_sendDeviceWithNameThatAlreadyExistsOnCloud);
    }

    private void _sendDeviceWithNameThatAlreadyExistsOnCloud() {
        String deviceName = StringUtils.randomAlphanumeric(15);
        Device savedDeviceOnCloud = saveDeviceOnCloud(deviceName, "default");
        Device savedDeviceOnEdge = saveDeviceOnEdge(deviceName, "default");

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS).
                until(() -> cloudRestClient.getDeviceById(savedDeviceOnEdge.getId()).isPresent());

        // device on edge must be renamed
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS).
                until(() -> !edgeRestClient.getDeviceById(savedDeviceOnEdge.getId()).get().getName().equals(deviceName));

        edgeRestClient.deleteDevice(savedDeviceOnEdge.getId());
        cloudRestClient.deleteDevice(savedDeviceOnEdge.getId());
        cloudRestClient.deleteDevice(savedDeviceOnCloud.getId());
    }

    @Test
    public void testClaimDevice() {
        performTestOnEachEdge(this::_testClaimDevice);
    }

    private void _testClaimDevice() {
        // create customer, user and device
        Customer customer = new Customer();
        customer.setTitle("Claim Test Customer" + edge.getName());
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);

        // add claim role and assign to customer admins group
        Role role = new Role();
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode("{\"DEVICE\":[\"CLAIM_DEVICES\"]}"));
        role.setName("Claiming Role " + edge.getName());
        Role savedRole = cloudRestClient.saveRole(role);
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(savedRole.getId());
        groupPermission.setUserGroupId(findCustomerAdminsGroup(savedCustomer).get().getId());
        cloudRestClient.saveGroupPermission(groupPermission);

        // change owner to customer
        cloudRestClient.changeOwnerToCustomer(savedCustomer.getId(), edge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isPresent());

        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(edge.getTenantId());
        user.setCustomerId(savedCustomer.getId());
        String email = "claimUser" + edge.getName() + "@thingsboard.org";
        user.setEmail(email);
        user.setFirstName("Claim");
        user.setLastName("User");
        User savedUser = cloudRestClient.saveUser(user, false, findCustomerAdminsGroup(savedCustomer).get().getId());
        cloudRestClient.activateUser(savedUser.getId(), "customer", false);

        Device savedDevice = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));
        Optional<DeviceCredentials> deviceCredentialsByDeviceId =
                cloudRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId());
        verifyDeviceCredentialsOnCloudAndEdge(savedDevice);

        // send claim device request
        JsonObject claimRequest = new JsonObject();
        claimRequest.addProperty("secretKey", "testKey");
        claimRequest.addProperty("duration", 10000);
        ResponseEntity claimDeviceRequest = edgeRestClient.getRestTemplate()
                .postForEntity(edgeUrl + "/api/v1/{credentialsId}/claim",
                        JacksonUtil.toJsonNode(claimRequest.toString()),
                        ResponseEntity.class,
                        deviceCredentialsByDeviceId.get().getCredentialsId());
        Assert.assertTrue(claimDeviceRequest.getStatusCode().is2xxSuccessful());

        // login as customer user and claim device
        loginIntoEdgeWithRetries(email, "customer");
        edgeRestClient.claimDevice(savedDevice.getName(), new ClaimRequest("testKey"));
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() ->
                        savedCustomer.getId().equals(edgeRestClient.getDeviceById(savedDevice.getId()).get().getCustomerId())
                                && savedCustomer.getId().equals(cloudRestClient.getDeviceById(savedDevice.getId()).get().getCustomerId()));

        // change owner to tenant
        loginIntoEdgeWithRetries("tenant@thingsboard.org", "tenant");
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(savedDevice.getId());
        cloudRestClient.deleteUser(savedUser.getId());
        cloudRestClient.deleteCustomer(savedCustomer.getId());
        cloudRestClient.deleteRole(savedRole.getId());
    }

    @Test
    public void testSharedAttributeUpdates() {
        performTestOnEachEdge(this::_testSharedAttributeUpdates);
    }

    private void _testSharedAttributeUpdates() {
        // create device on cloud and assign to edge
        Device savedDevice = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<DeviceCredentials> edgeDeviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId());
                    Optional<DeviceCredentials> cloudDeviceCredentials = cloudRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId());
                    return edgeDeviceCredentials.isPresent() &&
                            cloudDeviceCredentials.isPresent() &&
                            edgeDeviceCredentials.get().getCredentialsId().equals(cloudDeviceCredentials.get().getCredentialsId());
                });

        Optional<DeviceCredentials> deviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId());

        Assert.assertTrue(deviceCredentials.isPresent());

        // subscribe to shared attribute updates
        final ResponseEntity<JsonNode>[] sharedAttributeUpdateRequest = new ResponseEntity[]{null};

        new Thread(() -> {
            String subscribeToSharedAttributeUpdateUrl = edgeUrl + "/api/v1/" + deviceCredentials.get().getCredentialsId() + "/attributes/updates?timeout=20000";
            sharedAttributeUpdateRequest[0] = edgeRestClient.getRestTemplate().getForEntity(subscribeToSharedAttributeUpdateUrl, JsonNode.class);
        }).start();

        JsonNode deviceAttributes = JacksonUtil.toJsonNode("{\"sharedAttrKey\":\"sharedAttrValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedDevice.getId(), DataConstants.SHARED_SCOPE, deviceAttributes);

        // verify that shared attribute update was received
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    if (sharedAttributeUpdateRequest[0] == null || sharedAttributeUpdateRequest[0].getBody() == null) {
                        return false;
                    }
                    JsonNode requestBody = sharedAttributeUpdateRequest[0].getBody();
                    return "sharedAttrValue".equals(requestBody.get("sharedAttrKey").asText());
                });

        // cleanup
        cloudRestClient.deleteDevice(savedDevice.getId());
    }

}
