/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TelemetryClientTest extends AbstractContainerTest {

    @Test
    public void testSendPostTelemetryRequestToCloud_performanceTest() throws Exception {
        Device device = saveAndAssignDeviceToEdge();

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

        DeviceCredentials deviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        final String accessToken = deviceCredentials.getCredentialsId();
        final String telemetryKey = "index";
        final long numberOfTimeseriesToSend = 1000L;
        for (int idx = 1; idx <= numberOfTimeseriesToSend; idx++) {
            JsonObject timeseriesPayload = new JsonObject();
            timeseriesPayload.addProperty(telemetryKey, idx);
            ResponseEntity deviceTelemetryResponse = edgeRestClient.getRestTemplate()
                    .postForEntity(edgeUrl + "/api/v1/{credentialsId}/telemetry",
                            JacksonUtil.OBJECT_MAPPER.readTree(timeseriesPayload.toString()),
                            ResponseEntity.class,
                            accessToken);
            Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());
        }

        verifyDeviceIsActive(cloudRestClient, device.getId());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<TsKvEntry> latestTimeseries;
                    try {
                        latestTimeseries = cloudRestClient.getLatestTimeseries(device.getId(), List.of(telemetryKey));
                    } catch (Exception e) {
                        return false;
                    }
                    return latestTimeseries.size() == 1
                            && latestTimeseries.get(0).getLongValue().isPresent()
                            && latestTimeseries.get(0).getLongValue().get() == numberOfTimeseriesToSend;
                });

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
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

        List<TsKvEntry> kvEntries = sendPostTelemetryRequest(cloudRestClient, tbUrl, edgeRestClient, timeseriesPayload, keys);

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
        Device device = saveAndAssignDeviceToEdge();

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
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
                .pollInterval(500, TimeUnit.MILLISECONDS)
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

        List<TsKvEntry> latestTimeseries = targetRestClient.getLatestTimeseries(device.getId(), keys);

        // cleanup
        cloudRestClient.deleteDevice(device.getId());

        return latestTimeseries;
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

        List<AttributeKvEntry> kvEntries = testSendPostAttributesRequest(cloudRestClient, tbUrl, edgeRestClient, attrPayload, keys);

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

        Device device = saveAndAssignDeviceToEdge();

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> sourceRestClient.getDeviceCredentialsByDeviceId(device.getId()).isPresent());
        DeviceCredentials deviceCredentials = sourceRestClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        String accessToken = deviceCredentials.getCredentialsId();

        ResponseEntity deviceClientsAttributes = sourceRestClient.getRestTemplate()
                .postForEntity(sourceUrl + "/api/v1/" + accessToken + "/attributes/", JacksonUtil.OBJECT_MAPPER.readTree(attributesPayload.toString()),
                        ResponseEntity.class,
                        accessToken);
        Assert.assertTrue(deviceClientsAttributes.getStatusCode().is2xxSuccessful());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> targetRestClient.getAttributesByScope(device.getId(), DataConstants.CLIENT_SCOPE, keys).size() == keys.size());

        List<AttributeKvEntry> attributeKvEntries = targetRestClient.getAttributesByScope(device.getId(), DataConstants.CLIENT_SCOPE, keys);

        sourceRestClient.deleteEntityAttributes(device.getId(), DataConstants.CLIENT_SCOPE, keys);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> targetRestClient.getAttributesByScope(device.getId(), DataConstants.CLIENT_SCOPE, keys).size() == 0);

        verifyDeviceIsActive(targetRestClient, device.getId());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());

        return attributeKvEntries;
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
                Assert.assertEquals(42.0, attributeKvEntry.getDoubleValue().get(), 0.0);
            }
            if (attributeKvEntry.getKey().equals("longAttrToCloud")) {
                Assert.assertEquals(72L, attributeKvEntry.getLongValue().get().longValue());
            }
        }
    }

}
