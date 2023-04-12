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
package org.thingsboard.server.service.entitiy.alarm;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTbAlarmService.class)
@TestPropertySource(properties = {
        "server.log_controller_error_stack_trace=false"
})
public class DefaultTbAlarmServiceTest {

    @MockBean
    protected DbCallbackExecutorService dbExecutor;
    @MockBean
    protected TbNotificationEntityService notificationEntityService;
    @MockBean
    protected EdgeService edgeService;
    @MockBean
    protected AlarmService alarmService;
    @MockBean
    protected TbAlarmCommentService alarmCommentService;
    @MockBean
    protected AlarmSubscriptionService alarmSubscriptionService;
    @MockBean
    protected CustomerService customerService;
    @MockBean
    protected TbClusterService tbClusterService;
    @MockBean
    private EntitiesVersionControlService vcService;
    @MockBean
    protected EntityGroupService entityGroupService;

    @SpyBean
    DefaultTbAlarmService service;

    @Test
    public void testSave() throws ThingsboardException {
        var alarm = new AlarmInfo();
        when(alarmSubscriptionService.createAlarm(any())).thenReturn(AlarmApiCallResult.builder()
                .successful(true)
                .modified(true)
                .alarm(alarm)
                .build());
        service.save(alarm, new User());

        verify(notificationEntityService, times(1)).notifyCreateOrUpdateAlarm(any(), any(), any());
        verify(alarmSubscriptionService, times(1)).createAlarm(any());
    }

    @Test
    public void testAck() throws ThingsboardException {
        var alarm = new Alarm();
        when(alarmSubscriptionService.acknowledgeAlarm(any(), any(), anyLong()))
                .thenReturn(AlarmApiCallResult.builder().successful(true).modified(true).build());
        service.ack(alarm, new User(new UserId(UUID.randomUUID())));

        verify(alarmCommentService, times(1)).saveAlarmComment(any(), any(), any());
        verify(notificationEntityService, times(1)).notifyCreateOrUpdateAlarm(any(), any(), any());
        verify(alarmSubscriptionService, times(1)).acknowledgeAlarm(any(), any(), anyLong());
    }

    @Test
    public void testClear() throws ThingsboardException {
        var alarm = new Alarm();
        alarm.setAcknowledged(true);
        when(alarmSubscriptionService.clearAlarm(any(), any(), anyLong(), any()))
                .thenReturn(AlarmApiCallResult.builder().successful(true).cleared(true).build());
        service.clear(alarm, new User(new UserId(UUID.randomUUID())));

        verify(alarmCommentService, times(1)).saveAlarmComment(any(), any(), any());
        verify(notificationEntityService, times(1)).notifyCreateOrUpdateAlarm(any(), any(), any());
        verify(alarmSubscriptionService, times(1)).clearAlarm(any(), any(), anyLong(), any());
    }

    @Test
    public void testDelete() {
        service.delete(new Alarm(), new User());

        verify(notificationEntityService, times(1)).notifyDeleteAlarm(any(), any(), any(), any(), any(), any(), anyString());
        verify(alarmSubscriptionService, times(1)).deleteAlarm(any(), any());
    }
}