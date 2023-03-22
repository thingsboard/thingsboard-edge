/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AlarmClientTest extends AbstractContainerTest {

    @Test
    public void testAlarms() {
        // create alarm
        Device device = saveAndAssignDeviceToEdge();

        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        // TODO @voba alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarm.setType("alarm");
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        Alarm savedAlarm = cloudRestClient.saveAlarm(alarm);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromEdge(device.getId()).isPresent());

        // ack alarm
        cloudRestClient.ackAlarm(savedAlarm.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    AlarmInfo alarmData = getLatestAlarmByEntityIdFromEdge(device.getId()).get();
                    return alarmData.getAckTs() > 0;
                });

        // clear alarm
        cloudRestClient.clearAlarm(savedAlarm.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    AlarmInfo alarmData = getLatestAlarmByEntityIdFromEdge(device.getId()).get();
                    return alarmData.getClearTs() > 0;
                });

        // delete alarm
        cloudRestClient.deleteAlarm(savedAlarm.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromEdge(device.getId()).isEmpty());
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
        // create alarm
        Device device = saveAndAssignDeviceToEdge();
        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        // TODO @voba alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarm.setType("alarm from edge");
        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm savedAlarm = edgeRestClient.saveAlarm(alarm);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromCloud(device.getId()).isPresent());

        Assert.assertEquals("Alarm on edge and cloud have different types",
                "alarm from edge", getLatestAlarmByEntityIdFromCloud(device.getId()).get().getType());

        // ack alarm
        edgeRestClient.ackAlarm(savedAlarm.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    AlarmInfo alarmData = getLatestAlarmByEntityIdFromCloud(device.getId()).get();
                    return alarmData.getAckTs() > 0;
                });

        // clear alarm
        edgeRestClient.clearAlarm(savedAlarm.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    AlarmInfo alarmData = getLatestAlarmByEntityIdFromCloud(device.getId()).get();
                    return alarmData.getClearTs() > 0;
                });

        // delete alarm
        edgeRestClient.deleteAlarm(savedAlarm.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromCloud(device.getId()).isEmpty());
    }

    private Optional<AlarmInfo> getLatestAlarmByEntityIdFromCloud(EntityId entityId) {
        return getLatestAnyAlarmByEntityId(entityId, cloudRestClient);
    }

}

