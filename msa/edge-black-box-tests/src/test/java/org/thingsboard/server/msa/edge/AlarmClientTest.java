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
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
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
        performTestOnEachEdge(this::_testAlarms);
    }

    private void _testAlarms() {
        cloudRemoveFromSaveTimeseriesToPushToNodeSuccessConnection();

        // create alarm
        Device device = saveDeviceAndAssignEntityGroupToEdge(CUSTOM_DEVICE_PROFILE_NAME, createEntityGroup(EntityType.DEVICE));

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
                .until(() -> getLatestAlarmByEntityIdFromEdge(device.getId()).isPresent() && getLatestAlarmByEntityIdFromCloud(device.getId()).isPresent() &&
                        getLatestAlarmByEntityIdFromEdge(device.getId()).get().getId().equals(getLatestAlarmByEntityIdFromCloud(device.getId()).get().getId()));

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
                .until(() -> getLatestAlarmByEntityIdFromCloud(device.getId()).isEmpty()
                        && getLatestAlarmByEntityIdFromEdge(device.getId()).isEmpty());

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
        performTestOnEachEdge(this::_sendAlarmToCloud);
    }

    private void _sendAlarmToCloud() {
        edgeRemoveFromSaveTimeseriesToPushToNodeSuccessConnection();

        // create alarm on edge
        Device device = saveDeviceAndAssignEntityGroupToEdge(CUSTOM_DEVICE_PROFILE_NAME, createEntityGroup(EntityType.DEVICE));

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
                .until(() -> getLatestAlarmByEntityIdFromEdge(device.getId()).isPresent() && getLatestAlarmByEntityIdFromCloud(device.getId()).isPresent() &&
                        getLatestAlarmByEntityIdFromEdge(device.getId()).get().getId().equals(getLatestAlarmByEntityIdFromCloud(device.getId()).get().getId()));

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
        performTestOnEachEdge(this::_testAlarmComments);
    }

    private void _testAlarmComments() {
        cloudRemoveFromSaveTimeseriesToPushToNodeSuccessConnection();

        // create alarm
        Device device = saveDeviceAndAssignEntityGroupToEdge(CUSTOM_DEVICE_PROFILE_NAME, createEntityGroup(EntityType.DEVICE));

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
                .until(() -> getLatestAlarmByEntityIdFromEdge(device.getId()).isPresent() && getLatestAlarmByEntityIdFromCloud(device.getId()).isPresent() &&
                        getLatestAlarmByEntityIdFromEdge(device.getId()).get().getId().equals(getLatestAlarmByEntityIdFromCloud(device.getId()).get().getId()));

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
        performTestOnEachEdge(this::_sendAlarmCommentToCloud);
    }

    private void _sendAlarmCommentToCloud() {
        edgeRemoveFromSaveTimeseriesToPushToNodeSuccessConnection();

        // create alarm
        Device device = saveDeviceAndAssignEntityGroupToEdge(CUSTOM_DEVICE_PROFILE_NAME, createEntityGroup(EntityType.DEVICE));

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
                .until(() -> getLatestAlarmByEntityIdFromEdge(device.getId()).isPresent() && getLatestAlarmByEntityIdFromCloud(device.getId()).isPresent() &&
                        getLatestAlarmByEntityIdFromEdge(device.getId()).get().getId().equals(getLatestAlarmByEntityIdFromCloud(device.getId()).get().getId()));

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
