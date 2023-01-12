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
package org.thingsboard.server.msa.edge;

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
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
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
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.transport.snmp.AuthenticationProtocol;
import org.thingsboard.server.common.data.transport.snmp.PrivacyProtocol;
import org.thingsboard.server.msa.AbstractContainerTest;

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
    public void testDevices() throws Exception {
        // create device #1 and assign to edge
        Device savedDevice1 = saveAndAssignDeviceToEdge("Remote Controller");

        // update device #1 attributes
        cloudRestClient.saveDeviceAttributes(savedDevice1.getId(), DataConstants.SERVER_SCOPE, JacksonUtil.OBJECT_MAPPER.readTree("{\"key1\":\"value1\"}"));
        cloudRestClient.saveDeviceAttributes(savedDevice1.getId(), DataConstants.SHARED_SCOPE, JacksonUtil.OBJECT_MAPPER.readTree("{\"key2\":\"value2\"}"));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnEdge(savedDevice1.getId(), DataConstants.SERVER_SCOPE, "key1", "value1"));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnEdge(savedDevice1.getId(), DataConstants.SHARED_SCOPE, "key2", "value2"));

        // update device #1 credentials on cloud
        Optional<DeviceCredentials> deviceCredentialsByDeviceId =
                cloudRestClient.getDeviceCredentialsByDeviceId(savedDevice1.getId());
        Assert.assertTrue(deviceCredentialsByDeviceId.isPresent());
        DeviceCredentials deviceCredentials = deviceCredentialsByDeviceId.get();
        deviceCredentials.setCredentialsId("UpdatedTokenCloudDevice");
        cloudRestClient.saveDeviceCredentials(deviceCredentials);
        verifyDeviceCredentialsOnCloudAndEdge(savedDevice1);

        // validate transport configurations
        validateDeviceTransportConfiguration(savedDevice1, cloudRestClient, edgeRestClient);

        // update device #1
        OtaPackageId firmwarePackageId = createOtaPackageInfo(savedDevice1.getDeviceProfileId(), FIRMWARE);
        savedDevice1.setFirmwareId(firmwarePackageId);

        OtaPackageId softwarePackageId = createOtaPackageInfo(savedDevice1.getDeviceProfileId(), SOFTWARE);
        savedDevice1.setSoftwareId(softwarePackageId);

        savedDevice1.setName("Updated device name");
        cloudRestClient.saveDevice(savedDevice1);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Updated device name".equals(edgeRestClient.getDeviceById(savedDevice1.getId()).get().getName()));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> firmwarePackageId.equals(edgeRestClient.getDeviceById(savedDevice1.getId()).get().getFirmwareId()));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> softwarePackageId.equals(edgeRestClient.getDeviceById(savedDevice1.getId()).get().getSoftwareId()));

        // unassign device #1 from edge
        cloudRestClient.unassignDeviceFromEdge(edge.getId(), savedDevice1.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceById(savedDevice1.getId()).isEmpty());
        cloudRestClient.deleteDevice(savedDevice1.getId());

        // create device #2 and assign to edge
        Device savedDevice2 = saveAndAssignDeviceToEdge("Remote Controller");

        // assign device #2 to customer
        Customer customer = new Customer();
        customer.setTitle("Device Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);
        cloudRestClient.assignDeviceToCustomer(savedCustomer.getId(), savedDevice2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> savedCustomer.getId().equals(edgeRestClient.getDeviceById(savedDevice2.getId()).get().getCustomerId()));

        // unassign device #2 from customer
        cloudRestClient.unassignDeviceFromCustomer(savedDevice2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getDeviceById(savedDevice2.getId()).get().getCustomerId().getId()));
        cloudRestClient.deleteCustomer(savedCustomer.getId());

        // assign device #2 to public customer
        Customer publicCustomer = findPublicCustomer();
        cloudRestClient.assignDeviceToCustomer(publicCustomer.getId(), savedDevice2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> publicCustomer.getId().equals(edgeRestClient.getDeviceById(savedDevice2.getId()).get().getCustomerId()));

        // unassign device #2 from public customer
        cloudRestClient.unassignDeviceFromCustomer(savedDevice2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getDeviceById(savedDevice2.getId()).get().getCustomerId().getId()));

        // delete device #2
        cloudRestClient.deleteDevice(savedDevice2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceById(savedDevice2.getId()).isEmpty());

        // delete "Remote Controller" device profile
        cloudRestClient.deleteDeviceProfile(savedDevice1.getDeviceProfileId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceProfileById(savedDevice1.getDeviceProfileId()).isEmpty());
    }

    @Test
    public void sendDeviceToCloud() throws Exception {
        // create device on edge
        Device savedDeviceOnEdge = saveDeviceOnEdge("Edge Device 2", "default");
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getDeviceById(savedDeviceOnEdge.getId()).isPresent());
        verifyDeviceCredentialsOnCloudAndEdge(savedDeviceOnEdge);

        // update device attributes
        edgeRestClient.saveDeviceAttributes(savedDeviceOnEdge.getId(), DataConstants.SERVER_SCOPE, JacksonUtil.OBJECT_MAPPER.readTree("{\"key1\":\"value1\"}"));
        edgeRestClient.saveDeviceAttributes(savedDeviceOnEdge.getId(), DataConstants.SHARED_SCOPE, JacksonUtil.OBJECT_MAPPER.readTree("{\"key2\":\"value2\"}"));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnCloud(savedDeviceOnEdge.getId(), DataConstants.SERVER_SCOPE, "key1", "value1"));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnCloud(savedDeviceOnEdge.getId(), DataConstants.SHARED_SCOPE, "key2", "value2"));

        // update device credentials on edge
        Optional<DeviceCredentials> deviceCredentialsByDeviceId =
                edgeRestClient.getDeviceCredentialsByDeviceId(savedDeviceOnEdge.getId());
        Assert.assertTrue(deviceCredentialsByDeviceId.isPresent());
        DeviceCredentials deviceCredentials = deviceCredentialsByDeviceId.get();
        deviceCredentials.setCredentialsId("UpdatedTokenEdgeDevice");
        edgeRestClient.saveDeviceCredentials(deviceCredentials);
        verifyDeviceCredentialsOnCloudAndEdge(savedDeviceOnEdge);

        // validate transport configurations
        validateDeviceTransportConfiguration(savedDeviceOnEdge, edgeRestClient, cloudRestClient);

        // update device
        savedDeviceOnEdge.setName("Edge Device 2 Updated");
        edgeRestClient.saveDevice(savedDeviceOnEdge);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Edge Device 2 Updated".equals(cloudRestClient.getDeviceById(savedDeviceOnEdge.getId()).get().getName()));

        // assign device to customer
        Customer customer = new Customer();
        customer.setTitle("Device On Edge Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isPresent());
        edgeRestClient.assignDeviceToCustomer(savedCustomer.getId(), savedDeviceOnEdge.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> savedCustomer.getId().equals(cloudRestClient.getDeviceById(savedDeviceOnEdge.getId()).get().getCustomerId()));

        // unassign device from customer
        edgeRestClient.unassignDeviceFromCustomer(savedDeviceOnEdge.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(cloudRestClient.getDeviceById(savedDeviceOnEdge.getId()).get().getCustomerId().getId()));
        cloudRestClient.deleteCustomer(savedCustomer.getId());

        // delete device
        edgeRestClient.deleteDevice(savedDeviceOnEdge.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<Device> edgeDevices = cloudRestClient.getEdgeDevices(edge.getId(), new PageLink(1000));
                    long count = edgeDevices.getData().stream().filter(d -> savedDeviceOnEdge.getId().equals(d.getId())).count();
                    return count == 0;
                });
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

    private void verifyDeviceCredentialsOnCloudAndEdge(Device savedDevice) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId()).isPresent());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId()).isPresent());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    DeviceCredentials deviceCredentialsOnEdge =
                            edgeRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId()).get();
                    DeviceCredentials deviceCredentialsOnCloud =
                            cloudRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId()).get();
                    // TODO: @voba - potential fix for future releases
                    deviceCredentialsOnCloud.setId(null);
                    deviceCredentialsOnEdge.setId(null);
                    deviceCredentialsOnCloud.setCreatedTime(0);
                    deviceCredentialsOnEdge.setCreatedTime(0);
                    return deviceCredentialsOnCloud.equals(deviceCredentialsOnEdge);
                });
    }

    @Test
    public void testProvisionDevice() {
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
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceById(provisionedDeviceId).isEmpty());

        cloudRestClient.deleteDeviceProfile(provisionDeviceProfile.getId());
        Awaitility.await()
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
        // create device on cloud and assign to edge
        Device device = saveAndAssignDeviceToEdge();

        Awaitility.await()
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
        ObjectNode initialRequestBody = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        initialRequestBody.put("method", "setGpio");
        initialRequestBody.put("params", "{\"pin\":\"23\", \"value\": 1}");
        cloudRestClient.handleOneWayDeviceRPCRequest(device.getId(), initialRequestBody);

        // verify that rpc request was received
        Awaitility.await()
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
    }

    @Test
    public void testTwoWayRpcCall() {
        // create device on cloud and assign to edge
        Device device = saveAndAssignDeviceToEdge();

        Awaitility.await()
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
        ObjectNode initialRequestBody = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        initialRequestBody.put("method", "setGpio");
        initialRequestBody.put("params", "{\"pin\":\"23\", \"value\": 1}");

        final JsonNode[] rpcTwoWayRequest = new JsonNode[]{null};
        new Thread(() -> {
            rpcTwoWayRequest[0] = cloudRestClient.handleTwoWayDeviceRPCRequest(device.getId(), initialRequestBody);
        }).start();

        // verify that rpc request was received
        Awaitility.await()
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
        ObjectNode replyBody = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        replyBody.put("result", "ok");

        String rpcReply = edgeUrl + "/api/v1/" + deviceCredentials.get().getCredentialsId() + "/rpc/" + rpcSubscriptionRequest[0].getBody().get("id");
        edgeRestClient.getRestTemplate().postForEntity(rpcReply, replyBody, Void.class);

        // verify on the cloud that rpc response was received
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    if (rpcTwoWayRequest[0] == null) {
                        return false;
                    }
                    JsonNode responseBody = rpcTwoWayRequest[0];
                    return "ok".equals(responseBody.get("result").textValue());
                });
    }

    @Test
    public void testClientRpcCallToCloud() {
        // create device on cloud and assign to edge
        Device device = saveAndAssignDeviceToEdge();

        Awaitility.await()
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
        ObjectNode requestBody = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        requestBody.put("method", "getCurrentTime");
        requestBody.put("params", "{}");

        String rpcRequest = edgeUrl + "/api/v1/" + deviceCredentials.get().getCredentialsId() + "/rpc";
        ResponseEntity<String> responseEntity = edgeRestClient.getRestTemplate().postForEntity(rpcRequest, requestBody, String.class);

        Assert.assertNotNull(responseEntity.getBody());
        Assert.assertTrue(responseEntity.getBody().contains("currentTime"));
    }

    @Test
    public void sendDeviceWithNameThatAlreadyExistsOnCloud() {
        String deviceName = RandomStringUtils.randomAlphanumeric(15);
        Device savedDeviceOnCloud = saveDeviceOnCloud(deviceName, "default");
        Device savedDeviceOnEdge = saveDeviceOnEdge(deviceName, "default");

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getDeviceById(savedDeviceOnEdge.getId()).isPresent());

        // device on edge must be renamed
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> !edgeRestClient.getDeviceById(savedDeviceOnEdge.getId()).get().getName().equals(deviceName));

        edgeRestClient.deleteDevice(savedDeviceOnEdge.getId());
        cloudRestClient.deleteDevice(savedDeviceOnEdge.getId());
        cloudRestClient.deleteDevice(savedDeviceOnCloud.getId());
    }

    @Test
    public void testClaimDevice() throws Exception {
        // create customer, user and device
        Customer customer = new Customer();
        customer.setTitle("Claim Test Customer");
        Customer savedCustomer = cloudRestClient.saveCustomer(customer);
        assignEdgeToCustomerAndValidateAssignmentOnCloud(savedCustomer);
        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(edge.getTenantId());
        user.setCustomerId(savedCustomer.getId());
        user.setEmail("claimUser@thingsboard.org");
        user.setFirstName("Claim");
        user.setLastName("User");
        User savedUser = cloudRestClient.saveUser(user, false);
        cloudRestClient.activateUser(savedUser.getId(), "customer", false);

        Device savedDevice = saveAndAssignDeviceToEdge();
        Optional<DeviceCredentials> deviceCredentialsByDeviceId =
                cloudRestClient.getDeviceCredentialsByDeviceId(savedDevice.getId());
        verifyDeviceCredentialsOnCloudAndEdge(savedDevice);

        // send claim device request
        JsonObject claimRequest = new JsonObject();
        claimRequest.addProperty("secretKey", "testKey");
        claimRequest.addProperty("duration", 10000);
        ResponseEntity claimDeviceRequest = edgeRestClient.getRestTemplate()
                .postForEntity(edgeUrl + "/api/v1/{credentialsId}/claim",
                        JacksonUtil.OBJECT_MAPPER.readTree(claimRequest.toString()),
                        ResponseEntity.class,
                        deviceCredentialsByDeviceId.get().getCredentialsId());
        Assert.assertTrue(claimDeviceRequest.getStatusCode().is2xxSuccessful());

        // login as customer user and claim device
        loginIntoEdgeWithRetries("claimUser@thingsboard.org", "customer");
        edgeRestClient.claimDevice(savedDevice.getName(), new ClaimRequest("testKey"));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() ->
                        savedCustomer.getId().equals(edgeRestClient.getDeviceById(savedDevice.getId()).get().getCustomerId())
                                && savedCustomer.getId().equals(cloudRestClient.getDeviceById(savedDevice.getId()).get().getCustomerId()));

        // cleanup
        cloudRestClient.deleteDevice(savedDevice.getId());
        cloudRestClient.deleteUser(savedUser.getId());
        cloudRestClient.deleteCustomer(savedCustomer.getId());
        loginIntoEdgeWithRetries("tenant@thingsboard.org", "tenant");
    }

    @Test
    public void testSharedAttributeUpdates() throws JsonProcessingException {
        // create device on cloud and assign to edge
        Device savedDevice = saveAndAssignDeviceToEdge();

        Awaitility.await()
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

        JsonNode deviceAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"sharedAttrKey\":\"sharedAttrValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedDevice.getId(), DataConstants.SHARED_SCOPE, deviceAttributes);

        // verify that shared attribute update was received
        Awaitility.await()
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


