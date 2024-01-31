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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AlarmClientTest extends AbstractContainerTest {

    @Test
    public void testAlarms() throws Exception {
        // create alarm
        Device device = saveAndAssignDeviceToEdge(CUSTOM_DEVICE_PROFILE_NAME);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getDeviceCredentialsByDeviceId(device.getId()).isPresent());

        DeviceCredentials deviceCredentials = cloudRestClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        String accessToken = deviceCredentials.getCredentialsId();

        JsonObject telemetry = new JsonObject();
        telemetry.addProperty("temperature", 100);

        ResponseEntity deviceTelemetryResponse = cloudRestClient.getRestTemplate()
                .postForEntity(tbUrl + "/api/v1/{credentialsId}/telemetry",
                        JacksonUtil.OBJECT_MAPPER.readTree(telemetry.toString()),
                        ResponseEntity.class,
                        accessToken);

        Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromEdge(device.getId()).isPresent());

        Alarm savedAlarm = getLatestAlarmByEntityIdFromEdge(device.getId()).get();

        // ack alarm
        cloudRestClient.ackAlarm(savedAlarm.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    AlarmInfo alarmData = getLatestAlarmByEntityIdFromEdge(device.getId()).get();
                    return alarmData.getAckTs() > 0;
                });

        // clear alarm
        cloudRestClient.clearAlarm(savedAlarm.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    AlarmInfo alarmData = getLatestAlarmByEntityIdFromEdge(device.getId()).get();
                    return alarmData.getClearTs() > 0;
                });

        // delete alarm
        cloudRestClient.deleteAlarm(savedAlarm.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromEdge(device.getId()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
    }

    private Optional<AlarmInfo> getLatestAlarmByEntityIdFromEdge(EntityId entityId) {
        return getLatestAnyAlarmByEntityId(entityId, edgeRestClient);
    }

    private Optional<AlarmInfo> getLatestAnyAlarmByEntityId(EntityId entityId, RestClient restClient) {
        PageData<AlarmInfo> alarmDataByQuery =
                restClient.getAlarms(entityId, AlarmSearchStatus.ANY, null, new TimePageLink(1), false);
        if (alarmDataByQuery.getData().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(alarmDataByQuery.getData().get(0));
        }
    }

    @Test
    public void sendAlarmToCloud() {
        // create alarm on edge
        Device device = saveAndAssignDeviceToEdge(CUSTOM_DEVICE_PROFILE_NAME);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getDeviceCredentialsByDeviceId(device.getId()).isPresent());

        DeviceCredentials deviceCredentials = cloudRestClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        String accessToken = deviceCredentials.getCredentialsId();

        JsonObject telemetry = new JsonObject();
        telemetry.addProperty("temperature", 100);

        ResponseEntity deviceTelemetryResponse = cloudRestClient.getRestTemplate()
                .postForEntity(tbUrl + "/api/v1/" + accessToken + "/telemetry",
                        JacksonUtil.toJsonNode(telemetry.toString()),
                        ResponseEntity.class);
        Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromCloud(device.getId()).isPresent());

        Alarm savedAlarm = getLatestAlarmByEntityIdFromEdge(device.getId()).get();

        // ack alarm
        edgeRestClient.ackAlarm(savedAlarm.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    AlarmInfo alarmData = getLatestAlarmByEntityIdFromCloud(device.getId()).get();
                    return alarmData.getAckTs() > 0;
                });

        // clear alarm
        edgeRestClient.clearAlarm(savedAlarm.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    AlarmInfo alarmData = getLatestAlarmByEntityIdFromCloud(device.getId()).get();
                    return alarmData.getClearTs() > 0;
                });

        // delete alarm
        edgeRestClient.deleteAlarm(savedAlarm.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromCloud(device.getId()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
    }

    private Optional<AlarmInfo> getLatestAlarmByEntityIdFromCloud(EntityId entityId) {
        return getLatestAnyAlarmByEntityId(entityId, cloudRestClient);
    }

}
