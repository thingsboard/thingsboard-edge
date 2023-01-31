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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.alarm.AlarmDao;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
@Slf4j
public class JpaAlarmDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private AlarmDao alarmDao;


    @Test
    public void testFindLatestByOriginatorAndType() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Current system time in millis = {}", System.currentTimeMillis());
        UUID tenantId = UUID.fromString("d4b68f40-3e96-11e7-a884-898080180d6b");
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID originator2Id = UUID.fromString("d4b68f42-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        UUID alarm2Id = UUID.fromString("d4b68f44-3e96-11e7-a884-898080180d6b");
        UUID alarm3Id = UUID.fromString("d4b68f45-3e96-11e7-a884-898080180d6b");
        int alarmCountBeforeSave = alarmDao.find(TenantId.fromUUID(tenantId)).size();
        saveAlarm(alarm1Id, tenantId, originator1Id, "TEST_ALARM");
        //The timestamp of the startTime should be different in order for test to always work
        Thread.sleep(1);
        saveAlarm(alarm2Id, tenantId, originator1Id, "TEST_ALARM");
        saveAlarm(alarm3Id, tenantId, originator2Id, "TEST_ALARM");
        int alarmCountAfterSave = alarmDao.find(TenantId.fromUUID(tenantId)).size();
        assertEquals(3, alarmCountAfterSave - alarmCountBeforeSave);
        ListenableFuture<Alarm> future = alarmDao
                .findLatestByOriginatorAndTypeAsync(TenantId.fromUUID(tenantId), new DeviceId(originator1Id), "TEST_ALARM");
        Alarm alarm = future.get(30, TimeUnit.SECONDS);
        assertNotNull(alarm);
        assertEquals(alarm2Id, alarm.getId().getId());
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
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarmDao.save(TenantId.fromUUID(tenantId), alarm);
    }
}
