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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TelemetryClientTest extends AbstractContainerTest {

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
        Device device = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));

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

        Device device = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));

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
                Assert.assertEquals(42.0, (double) attributeKvEntry.getDoubleValue().get(), 0.0);
            }
            if (attributeKvEntry.getKey().equals("longAttrToCloud")) {
                Assert.assertEquals(72L, attributeKvEntry.getLongValue().get().longValue());
            }
        }
    }

}

