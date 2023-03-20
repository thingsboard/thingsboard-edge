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
package org.thingsboard.server.dao.sql.alarm;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.dao.alarm.AlarmDao;

import java.util.UUID;
import static org.junit.Assert.assertEquals;

@Slf4j
public class JpaAlarmCommentDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private AlarmCommentDao alarmCommentDao;
    @Autowired
    private AlarmDao alarmDao;


    @Test
    public void testFindAlarmCommentsByAlarmId() {
        log.info("Current system time in millis = {}", System.currentTimeMillis());
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID alarmId1 = UUID.randomUUID();
        UUID alarmId2 = UUID.randomUUID();
        UUID commentId1 = UUID.randomUUID();
        UUID commentId2 = UUID.randomUUID();
        UUID commentId3 = UUID.randomUUID();
        saveAlarm(alarmId1, UUID.randomUUID(), UUID.randomUUID(), "TEST_ALARM");
        saveAlarm(alarmId2, UUID.randomUUID(), UUID.randomUUID(), "TEST_ALARM");

        saveAlarmComment(commentId1, alarmId1, userId, AlarmCommentType.OTHER);
        saveAlarmComment(commentId2, alarmId1, userId, AlarmCommentType.OTHER);
        saveAlarmComment(commentId3, alarmId2, userId, AlarmCommentType.OTHER);

        int count = alarmCommentDao.findAlarmComments(TenantId.fromUUID(tenantId), new AlarmId(alarmId1), new PageLink(10, 0)).getData().size();
        assertEquals(2, count);
    }

    private void saveAlarm(UUID id, UUID tenantId, UUID deviceId, String type) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(id));
        alarm.setTenantId(TenantId.fromUUID(tenantId));
        alarm.setOriginator(new DeviceId(deviceId));
        alarm.setType(type);
        alarm.setPropagate(true);
        alarm.setStartTs(System.currentTimeMillis());
        alarm.setEndTs(System.currentTimeMillis());
        alarmDao.save(TenantId.fromUUID(tenantId), alarm);
    }
    private void saveAlarmComment(UUID id, UUID alarmId, UUID userId, AlarmCommentType type) {
        AlarmComment alarmComment = new AlarmComment();
        alarmComment.setId(new AlarmCommentId(id));
        alarmComment.setAlarmId(TenantId.fromUUID(alarmId));
        alarmComment.setUserId(new UserId(userId));
        alarmComment.setType(type);
        alarmComment.setComment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)));
        alarmCommentDao.createAlarmComment(TenantId.fromUUID(UUID.randomUUID()), alarmComment);
    }
}
