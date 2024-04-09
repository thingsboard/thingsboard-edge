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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AlarmClientTest extends AbstractContainerTest {

    @Test
    public void testAlarms() {
        cloudRemoveFromSaveTimeseriesToPushToNodeSuccessConnection();

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
                .postForEntity(tbUrl + "/api/v1/" + accessToken + "/telemetry",
                        JacksonUtil.toJsonNode(telemetry.toString()),
                        ResponseEntity.class);

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

        cloudRestoreFromSaveTimeseriesToPushToNodeSuccessConnection();
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
        edgeRemoveFromSaveTimeseriesToPushToNodeSuccessConnection();

        // create alarm on edge
        Device device = saveAndAssignDeviceToEdge(CUSTOM_DEVICE_PROFILE_NAME);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceCredentialsByDeviceId(device.getId()).isPresent());

        DeviceCredentials deviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        String accessToken = deviceCredentials.getCredentialsId();

        JsonObject telemetry = new JsonObject();
        telemetry.addProperty("temperature", 100);

        ResponseEntity deviceTelemetryResponse = edgeRestClient.getRestTemplate()
                .postForEntity(edgeUrl + "/api/v1/" + accessToken + "/telemetry",
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

        edgeRestoreFromSaveTimeseriesToPushToNodeSuccessConnection();
    }

    private Optional<AlarmInfo> getLatestAlarmByEntityIdFromCloud(EntityId entityId) {
        return getLatestAnyAlarmByEntityId(entityId, cloudRestClient);
    }

    @Test
    public void testAlarmComments() {
        cloudRemoveFromSaveTimeseriesToPushToNodeSuccessConnection();

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
                .postForEntity(tbUrl + "/api/v1/" + accessToken + "/telemetry",
                        JacksonUtil.toJsonNode(telemetry.toString()),
                        ResponseEntity.class);

        Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromCloud(device.getId()).isPresent() && getLatestAlarmByEntityIdFromEdge(device.getId()).isPresent());

        Alarm savedAlarm = getLatestAlarmByEntityIdFromCloud(device.getId()).get();

        ObjectNode comment = JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10));
        AlarmComment alarmComment = saveAlarmComment(savedAlarm.getId(), comment, cloudRestClient);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAlarmComments(savedAlarm.getId(), new PageLink(100)).getTotalElements() > 0 &&
                        edgeRestClient.getAlarmComments(savedAlarm.getId(), new PageLink(100)).getData().stream()
                                .anyMatch(ac -> ac.getComment().equals(alarmComment.getComment())
                                        && ac.getId().equals(alarmComment.getId())
                                        && ac.getAlarmId().equals(alarmComment.getAlarmId())));

        comment = JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10));
        alarmComment.setComment(comment);
        AlarmComment updated = cloudRestClient.saveAlarmComment(savedAlarm.getId(), alarmComment);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAlarmComments(savedAlarm.getId(), new PageLink(100)).getTotalElements() > 0 &&
                        edgeRestClient.getAlarmComments(savedAlarm.getId(), new PageLink(100)).getData().stream()
                                .anyMatch(ac -> ac.getComment().get("text").equals(updated.getComment().get("text"))
                                        && ac.getComment().get("edited").asBoolean()
                                        && updated.getComment().get("edited").asBoolean()
                                        && ac.getId().equals(updated.getId())
                                        && ac.getAlarmId().equals(updated.getAlarmId())));

        // delete alarm
        cloudRestClient.deleteAlarm(savedAlarm.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromEdge(device.getId()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());

        cloudRestoreFromSaveTimeseriesToPushToNodeSuccessConnection();
    }

    @Test
    public void sendAlarmCommentToCloud() {
        edgeRemoveFromSaveTimeseriesToPushToNodeSuccessConnection();

        // create alarm
        Device device = saveAndAssignDeviceToEdge(CUSTOM_DEVICE_PROFILE_NAME);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDeviceCredentialsByDeviceId(device.getId()).isPresent());

        DeviceCredentials deviceCredentials = edgeRestClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        String accessToken = deviceCredentials.getCredentialsId();

        JsonObject telemetry = new JsonObject();
        telemetry.addProperty("temperature", 100);

        ResponseEntity deviceTelemetryResponse = edgeRestClient.getRestTemplate()
                .postForEntity(edgeUrl + "/api/v1/" + accessToken + "/telemetry",
                        JacksonUtil.toJsonNode(telemetry.toString()),
                        ResponseEntity.class);

        Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromCloud(device.getId()).isPresent() && getLatestAlarmByEntityIdFromEdge(device.getId()).isPresent());

        Alarm savedAlarm = getLatestAlarmByEntityIdFromEdge(device.getId()).get();

        ObjectNode comment = JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10));
        AlarmComment alarmComment = saveAlarmComment(savedAlarm.getId(), comment, edgeRestClient);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getAlarmComments(savedAlarm.getId(), new PageLink(100)).getData().stream()
                        .anyMatch(ac -> ac.getComment().equals(alarmComment.getComment())
                                && ac.getId().equals(alarmComment.getId())
                                && ac.getAlarmId().equals(alarmComment.getAlarmId())));

        comment = JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10));
        alarmComment.setComment(comment);
        AlarmComment updated = edgeRestClient.saveAlarmComment(savedAlarm.getId(), alarmComment);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getAlarmComments(savedAlarm.getId(), new PageLink(100)).getTotalElements() > 0 &&
                    edgeRestClient.getAlarmComments(savedAlarm.getId(), new PageLink(100)).getData().stream()
                            .anyMatch(ac -> ac.getComment().get("text").equals(updated.getComment().get("text"))
                                    && ac.getComment().get("edited").asBoolean()
                                    && updated.getComment().get("edited").asBoolean()
                                    && ac.getId().equals(updated.getId())
                                    && ac.getAlarmId().equals(updated.getAlarmId())));

        // delete alarm
        cloudRestClient.deleteAlarm(savedAlarm.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmByEntityIdFromEdge(device.getId()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());

        edgeRestoreFromSaveTimeseriesToPushToNodeSuccessConnection();
    }

    private AlarmComment saveAlarmComment(AlarmId alarmId, JsonNode comment, RestClient restClient) {
        AlarmComment alarmComment = new AlarmComment();
        alarmComment.setAlarmId(alarmId);
        alarmComment.setType(AlarmCommentType.OTHER);
        alarmComment.setComment(comment);
        return restClient.saveAlarmComment(alarmId, alarmComment);
    }

    // required to avoid creating duplicate alarms on the cloud or edge
    // success save timeseries - push to edge/cloud conneciton is removed
    // alarm are not created by a local device profile rule node
    // restored connection back after test completed
    private void cloudRemoveFromSaveTimeseriesToPushToNodeSuccessConnection() {
        removeFromSaveTimeseriesToPushToNodeSuccessConnection(RuleChainType.CORE, "Push To Edge (Timeseries & Attributes)");
    }

    private void cloudRestoreFromSaveTimeseriesToPushToNodeSuccessConnection() {
        restoreFromSaveTimeseriesToPushToNodeSuccessConnection(RuleChainType.CORE, "Push To Edge (Timeseries & Attributes)");
    }

    private void edgeRemoveFromSaveTimeseriesToPushToNodeSuccessConnection() {
        removeFromSaveTimeseriesToPushToNodeSuccessConnection(RuleChainType.EDGE, "Push to Cloud (Timeseries & Attributes)");
    }

    private void edgeRestoreFromSaveTimeseriesToPushToNodeSuccessConnection() {
        restoreFromSaveTimeseriesToPushToNodeSuccessConnection(RuleChainType.EDGE, "Push to Cloud (Timeseries & Attributes)");
    }

    private void removeFromSaveTimeseriesToPushToNodeSuccessConnection(RuleChainType ruleChainType, String pushToNodeName) {
        RuleChainMetaData ruleChainMetaData = findRootRuleChainMetadata(ruleChainType);
        int saveTimeSeriesRuleNodeIdx = findRuleNodeIdxByName(ruleChainMetaData.getNodes(), "Save Timeseries");
        int pushToRuleNodeIdx = findRuleNodeIdxByName(ruleChainMetaData.getNodes(), pushToNodeName);
        ArrayList<NodeConnectionInfo> connections = new ArrayList<>();
        for (NodeConnectionInfo connection : ruleChainMetaData.getConnections()) {
            if (connection.getFromIndex() != saveTimeSeriesRuleNodeIdx || connection.getToIndex() != pushToRuleNodeIdx) {
                connections.add(connection);
            }
        }
        ruleChainMetaData.setConnections(connections);
        cloudRestClient.saveRuleChainMetaData(ruleChainMetaData);
    }

    private void restoreFromSaveTimeseriesToPushToNodeSuccessConnection(RuleChainType ruleChainType, String pushToRuleNodeName) {
        RuleChainMetaData ruleChainMetaData = findRootRuleChainMetadata(ruleChainType);
        int saveTimeSeriesRuleNodeIdx = findRuleNodeIdxByName(ruleChainMetaData.getNodes(), "Save Timeseries");
        int pushToRuleNodeIdx = findRuleNodeIdxByName(ruleChainMetaData.getNodes(), pushToRuleNodeName);
        var fromSaveTimeseriesToPushToNodeSuccessConnection = new NodeConnectionInfo();
        fromSaveTimeseriesToPushToNodeSuccessConnection.setType("Success");
        fromSaveTimeseriesToPushToNodeSuccessConnection.setFromIndex(saveTimeSeriesRuleNodeIdx);
        fromSaveTimeseriesToPushToNodeSuccessConnection.setToIndex(pushToRuleNodeIdx);
        ruleChainMetaData.getConnections().add(fromSaveTimeseriesToPushToNodeSuccessConnection);
        cloudRestClient.saveRuleChainMetaData(ruleChainMetaData);
    }

    private RuleChainMetaData findRootRuleChainMetadata(RuleChainType ruleChainType) {
        RuleChainId rootRuleChainId = findRootRuleChainId(ruleChainType);
        Optional<RuleChainMetaData> ruleChainMetaDataOpt = cloudRestClient.getRuleChainMetaData(rootRuleChainId);
        if (ruleChainMetaDataOpt.isEmpty()) {
            throw new RuntimeException("Root rule chain metadata was not found!");
        }
        return ruleChainMetaDataOpt.get();
    }

    private int findRuleNodeIdxByName(List<RuleNode> ruleNodes, String nodeName) {
        int idx = 0;
        for (RuleNode node : ruleNodes) {
            if (node.getName().equalsIgnoreCase(nodeName)) {
                return idx;
            }
            idx++;
        }
        throw new RuntimeException("Rule node idx was not found!");
    }

    private RuleChainId findRootRuleChainId(RuleChainType ruleChainType) {
        PageData<RuleChain> ruleChains = cloudRestClient.getRuleChains(ruleChainType, new PageLink(100));
        RuleChainId rootRuleChainId = null;
        for (RuleChain ruleChain : ruleChains.getData()) {
            if (ruleChain.isRoot()) {
                rootRuleChainId = ruleChain.getId();
                break;
            }
        }
        return rootRuleChainId;
    }

}
