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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.user.UserService;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.alarm.AlarmCommentType.OTHER;

public abstract class BaseAlarmCommentServiceTest extends AbstractServiceTest {

    @Autowired
    AlarmService alarmService;
    @Autowired
    AlarmCommentService alarmCommentService;
    @Autowired
    UserService userService;

    public static final String TEST_ALARM = "TEST_ALARM";
    private Alarm alarm;
    private User user;

    @Before
    public void before() {
        alarm = Alarm.builder().tenantId(tenantId).originator(new AssetId(Uuids.timeBased()))
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(System.currentTimeMillis()).build();
        alarm = alarmService.createOrUpdateAlarm(alarm).getAlarm();

        user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail("tenant@thingsboard.org");
        user.setFirstName("John");
        user.setLastName("Brown");
        user = userService.saveUser(user);
    }

    @After
    public void after() {
        alarmService.deleteAlarm(tenantId, alarm.getId());
    }


    @Test
    public void testCreateAndFetchAlarmComment() throws ExecutionException, InterruptedException {
        AlarmComment alarmComment = AlarmComment.builder().alarmId(alarm.getId())
                .userId(user.getId())
                .type(OTHER)
                .comment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)))
                .build();

        AlarmComment createdComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);

        Assert.assertNotNull(createdComment);
        Assert.assertNotNull(createdComment.getId());

        Assert.assertEquals(alarm.getId(), createdComment.getAlarmId());
        Assert.assertEquals(user.getId(), createdComment.getUserId());
        Assert.assertEquals(OTHER, createdComment.getType());
        Assert.assertTrue(createdComment.getCreatedTime() > 0);

        AlarmComment fetched = alarmCommentService.findAlarmCommentByIdAsync(tenantId, createdComment.getId()).get();
        Assert.assertEquals(createdComment, fetched);

        PageData<AlarmCommentInfo> alarmComments = alarmCommentService.findAlarmComments(tenantId, alarm.getId(), new PageLink(10, 0));
        Assert.assertNotNull(alarmComments.getData());
        Assert.assertEquals(1, alarmComments.getData().size());
        Assert.assertEquals(createdComment, new AlarmComment(alarmComments.getData().get(0)));
    }

    @Test
    public void testUpdateAlarmComment() throws ExecutionException, InterruptedException {
        UserId userId = new UserId(UUID.randomUUID());
        AlarmComment alarmComment = AlarmComment.builder().alarmId(alarm.getId())
                .userId(userId)
                .type(OTHER)
                .comment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)))
                .build();

        AlarmComment createdComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);

        Assert.assertNotNull(createdComment);
        Assert.assertNotNull(createdComment.getId());

        //update comment
        String newComment = "new comment";
        createdComment.setComment(JacksonUtil.newObjectNode().put("text", newComment));
        AlarmComment updatedComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, createdComment);

        Assert.assertEquals(alarm.getId(), updatedComment.getAlarmId());
        Assert.assertEquals(userId, updatedComment.getUserId());
        Assert.assertEquals(OTHER, updatedComment.getType());
        Assert.assertTrue(updatedComment.getCreatedTime() > 0);
        Assert.assertEquals(newComment, updatedComment.getComment().get("text").asText());
        Assert.assertEquals("true", updatedComment.getComment().get("edited").asText());
        Assert.assertNotNull(updatedComment.getComment().get("editedOn").asText());

        AlarmComment fetched = alarmCommentService.findAlarmCommentByIdAsync(tenantId, createdComment.getId()).get();
        Assert.assertEquals(updatedComment, fetched);

        PageData<AlarmCommentInfo> alarmComments = alarmCommentService.findAlarmComments(tenantId, alarm.getId(), new PageLink(10, 0));
        Assert.assertNotNull(alarmComments.getData());
        Assert.assertEquals(1, alarmComments.getData().size());
        Assert.assertEquals(updatedComment, new AlarmComment(alarmComments.getData().get(0)));
    }

    @Test
    public void testSaveAlarmComment() throws ExecutionException, InterruptedException {
        UserId userId = new UserId(UUID.randomUUID());
        AlarmComment alarmComment = AlarmComment.builder().alarmId(alarm.getId())
                .userId(userId)
                .type(OTHER)
                .comment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)))
                .build();

        AlarmComment createdComment = alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);

        createdComment.setType(AlarmCommentType.SYSTEM);
        createdComment.setUserId(null);
        alarmCommentService.saveAlarmComment(tenantId, createdComment);

        AlarmComment fetched = alarmCommentService.findAlarmCommentByIdAsync(tenantId, createdComment.getId()).get();
        Assert.assertNull(fetched.getUserId());
        Assert.assertEquals(AlarmCommentType.SYSTEM, fetched.getType());
    }
}
