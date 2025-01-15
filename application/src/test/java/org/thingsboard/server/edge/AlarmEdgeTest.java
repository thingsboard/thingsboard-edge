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
package org.thingsboard.server.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AlarmEdgeTest extends AbstractEdgeTest {

    @Test
    @Ignore
    public void testSendAlarmToCloud() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        Alarm edgeAlarm = buildAlarmForUplinkMsg(device.getId());

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AlarmUpdateMsg.Builder alarmUpdateMgBuilder = AlarmUpdateMsg.newBuilder();
        alarmUpdateMgBuilder.setIdMSB(edgeAlarm.getUuidId().getMostSignificantBits());
        alarmUpdateMgBuilder.setIdLSB(edgeAlarm.getUuidId().getLeastSignificantBits());
        alarmUpdateMgBuilder.setEntity(JacksonUtil.toString(edgeAlarm));
        testAutoGeneratedCodeByProtobuf(alarmUpdateMgBuilder);
        uplinkMsgBuilder.addAlarmUpdateMsg(alarmUpdateMgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        List<AlarmInfo> alarms = doGetTypedWithPageLink("/api/alarm/{entityType}/{entityId}?",
                new TypeReference<PageData<AlarmInfo>>() {},
                new PageLink(100), device.getId().getEntityType().name(), device.getUuidId())
                .getData();
        Optional<AlarmInfo> foundAlarm = alarms.stream().filter(alarm -> alarm.getType().equals("alarm from edge")).findAny();
        Assert.assertTrue(foundAlarm.isPresent());
        AlarmInfo alarmInfo = foundAlarm.get();
        Assert.assertEquals(edgeAlarm.getId(), alarmInfo.getId());
        Assert.assertEquals(device.getId(), alarmInfo.getOriginator());
        Assert.assertEquals(AlarmStatus.ACTIVE_UNACK, alarmInfo.getStatus());
        Assert.assertEquals(AlarmSeverity.CRITICAL, alarmInfo.getSeverity());
    }

    @Test
    @Ignore
    public void testAlarms() throws Exception {
        // create alarm
        Device device = findDeviceByName("Edge Device 1");
        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setType("alarm");
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        Alarm savedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        edgeImitator.ignoreType(AlarmCommentUpdateMsg.class);

        // ack alarm - send only by using push to edge node
        edgeImitator.expectMessageAmount(1);
        doPost("/api/alarm/" + savedAlarm.getUuidId() + "/ack");
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        // clear alarm - send only by using push to edge node
        edgeImitator.expectMessageAmount(1);
        doPost("/api/alarm/" + savedAlarm.getUuidId() + "/clear");
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        // delete alarm
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/alarm/" + savedAlarm.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AlarmUpdateMsg);
        AlarmUpdateMsg alarmUpdateMsg = (AlarmUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, alarmUpdateMsg.getMsgType());
        Alarm alarmMsg = JacksonUtil.fromString(alarmUpdateMsg.getEntity(), Alarm.class, true);
        Assert.assertNotNull(alarmMsg);
        Assert.assertEquals(savedAlarm.getType(), alarmMsg.getType());
        Assert.assertEquals(savedAlarm.getName(), alarmMsg.getName());
        Assert.assertEquals(AlarmStatus.CLEARED_ACK, alarmMsg.getStatus());
        edgeImitator.allowIgnoredTypes();
    }

    @Test
    @Ignore
    public void testSendAlarmCommentToCloud() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        Alarm alarm = buildAlarmForUplinkMsg(device.getId());

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AlarmUpdateMsg.Builder alarmUpdateMgBuilder = AlarmUpdateMsg.newBuilder();
        alarmUpdateMgBuilder.setIdMSB(alarm.getUuidId().getMostSignificantBits());
        alarmUpdateMgBuilder.setIdLSB(alarm.getUuidId().getLeastSignificantBits());
        alarmUpdateMgBuilder.setEntity(JacksonUtil.toString(alarm));
        testAutoGeneratedCodeByProtobuf(alarmUpdateMgBuilder);
        uplinkMsgBuilder.addAlarmUpdateMsg(alarmUpdateMgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        AlarmComment alarmComment = buildAlarmCommentForUplinkMsg(alarm.getId());

        uplinkMsgBuilder = UplinkMsg.newBuilder();
        AlarmCommentUpdateMsg.Builder alarmCommentUpdateMgBuilder = AlarmCommentUpdateMsg.newBuilder();
        alarmCommentUpdateMgBuilder.setEntity(JacksonUtil.toString(alarmComment));
        alarmCommentUpdateMgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(alarmCommentUpdateMgBuilder);
        uplinkMsgBuilder.addAlarmCommentUpdateMsg(alarmCommentUpdateMgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        PageData<AlarmCommentInfo> pageData = doGetTyped("/api/alarm/" + alarmComment.getAlarmId().getId() + "/comment" + "?page=0&pageSize=1", new TypeReference<>() {});
        Assert.assertNotNull("Found pageData is null", pageData);
        Assert.assertNotEquals("Expected alarms are not found!", 0, pageData.getTotalElements());

        Assert.assertNotNull(pageData.getData().get(0));
        AlarmCommentInfo alarmInfo = pageData.getData().get(0);
        Assert.assertEquals(alarm.getId(), alarmInfo.getAlarmId());
        Assert.assertEquals(alarmComment.getAlarmId(), alarmInfo.getAlarmId());
    }

    @Test
    @Ignore
    public void testAlarmComments() throws Exception {
        Device device = findDeviceByName("Edge Device 1");
        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setType("alarm");
        alarm.setSeverity(AlarmSeverity.MINOR);
        Alarm savedAlarm = doPost("/api/alarm", alarm, Alarm.class);

        // create alarm comment
        edgeImitator.expectMessageAmount(1);
        AlarmComment alarmComment = new AlarmComment();
        alarmComment.setComment(new TextNode("Test"));
        alarmComment.setAlarmId(savedAlarm.getId());
        alarmComment = doPost("/api/alarm/" + savedAlarm.getUuidId() + "/comment", alarmComment, AlarmComment.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        AlarmCommentUpdateMsg alarmCommentUpdateMsg = (AlarmCommentUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, alarmCommentUpdateMsg.getMsgType());
        AlarmComment alarmCommentMsg = JacksonUtil.fromString(alarmCommentUpdateMsg.getEntity(), AlarmComment.class, true);
        Assert.assertNotNull(alarmCommentMsg);
        Assert.assertEquals(alarmComment, alarmCommentMsg);

        // update alarm comment
        edgeImitator.expectMessageAmount(1);
        alarmComment.setComment(JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment")));
        alarmComment = doPost("/api/alarm/" + savedAlarm.getUuidId() + "/comment", alarmComment, AlarmComment.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        alarmCommentUpdateMsg = (AlarmCommentUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, alarmCommentUpdateMsg.getMsgType());
        alarmCommentMsg = JacksonUtil.fromString(alarmCommentUpdateMsg.getEntity(), AlarmComment.class, true);
        Assert.assertNotNull(alarmCommentMsg);
        Assert.assertEquals(alarmComment, alarmCommentMsg);

        // delete alarm
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/alarm/" + savedAlarm.getUuidId() + "/comment/" + alarmComment.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        alarmCommentUpdateMsg = (AlarmCommentUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, alarmCommentUpdateMsg.getMsgType());
        alarmCommentMsg = JacksonUtil.fromString(alarmCommentUpdateMsg.getEntity(), AlarmComment.class, true);
        Assert.assertNotNull(alarmCommentMsg);
    }

    private Alarm buildAlarmForUplinkMsg(DeviceId deviceId) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(UUID.randomUUID()));
        alarm.setTenantId(tenantId);
        alarm.setType("alarm from edge");
        alarm.setOriginator(deviceId);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        return alarm;
    }

    private AlarmComment buildAlarmCommentForUplinkMsg(AlarmId alarmId) {
        UUID uuid = Uuids.timeBased();
        AlarmComment alarmComment = new AlarmComment();
        alarmComment.setAlarmId(alarmId);
        alarmComment.setType(AlarmCommentType.OTHER);
        alarmComment.setUserId(tenantAdminUserId);
        alarmComment.setId(new AlarmCommentId(uuid));
        alarmComment.setComment(new TextNode("AlarmComment"));
        alarmComment.setCreatedTime(Uuids.unixTimestamp(uuid));
        return alarmComment;
    }

}
