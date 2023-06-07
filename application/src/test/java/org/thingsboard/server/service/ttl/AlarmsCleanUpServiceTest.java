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
package org.thingsboard.server.service.ttl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DaoSqlTest
@TestPropertySource(properties = {
        "sql.ttl.alarms.removal_batch_size=5"
})
public class AlarmsCleanUpServiceTest extends AbstractControllerTest {

    @Autowired
    private AlarmsCleanUpService alarmsCleanUpService;
    @SpyBean
    private AlarmService alarmService;
    @Autowired
    private AlarmDao alarmDao;

    private static Logger cleanUpServiceLogger;

    @BeforeClass
    public static void before() throws Exception {
        cleanUpServiceLogger = Mockito.spy(LoggerFactory.getLogger(AlarmsCleanUpService.class));
        setStaticFinalFieldValue(AlarmsCleanUpService.class, "log", cleanUpServiceLogger);
    }

    @Test
    public void testAlarmsCleanUp() throws Exception {
        int ttlDays = 1;
        updateDefaultTenantProfile(profileConfiguration -> {
            profileConfiguration.setAlarmsTtlDays(ttlDays);
        });

        loginTenantAdmin();
        Device device = createDevice("device_0", "device_0");
        int count = 100;
        long ts = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(ttlDays) - TimeUnit.MINUTES.toMillis(10);
        List<AlarmId> outdatedAlarms = new ArrayList<>();
        List<AlarmId> freshAlarms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Alarm alarm = Alarm.builder()
                    .tenantId(tenantId)
                    .originator(device.getId())
                    .cleared(false)
                    .acknowledged(false)
                    .severity(AlarmSeverity.CRITICAL)
                    .type("outdated_alarm_" + i)
                    .startTs(ts)
                    .endTs(ts)
                    .build();
            alarm.setId(new AlarmId(UUID.randomUUID()));
            alarm.setCreatedTime(ts);

            outdatedAlarms.add(alarmDao.save(tenantId, alarm).getId());

            alarm.setType("fresh_alarm_" + i);
            alarm.setStartTs(System.currentTimeMillis());
            alarm.setEndTs(alarm.getStartTs());
            alarm.setId(new AlarmId(UUID.randomUUID()));
            alarm.setCreatedTime(alarm.getStartTs());
            freshAlarms.add(alarmDao.save(tenantId, alarm).getId());
        }

        alarmsCleanUpService.cleanUp();

        for (AlarmId outdatedAlarm : outdatedAlarms) {
            verify(alarmService).delAlarm(eq(tenantId), eq(outdatedAlarm));
        }
        for (AlarmId freshAlarm : freshAlarms) {
            verify(alarmService, never()).delAlarm(eq(tenantId), eq(freshAlarm));
        }

        verify(cleanUpServiceLogger).info(startsWith("Removed {} outdated alarm"), eq((long) count), eq(tenantId), any());
    }

}
