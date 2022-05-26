/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbAlarmService extends AbstractTbEntityService implements TbAlarmService {

    @Override
    public Alarm save(Alarm alarm, SecurityUser user) throws ThingsboardException {
        ActionType actionType = alarm.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = alarm.getTenantId();
        try {
            Alarm savedAlarm = checkNotNull(alarmService.createOrUpdateAlarm(alarm).getAlarm());
            notificationEntityService.notifyCreateOrUpdateAlarm(savedAlarm, actionType, user);
            return savedAlarm;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.ALARM), alarm, null, actionType, user, e);
            throw handleException(e);
        }
    }

    @Override
    public void ack(Alarm alarm, SecurityUser user) throws ThingsboardException {
        try {
            long ackTs = System.currentTimeMillis();
            alarmService.ackAlarm(user.getTenantId(), alarm.getId(), ackTs).get();
            alarm.setAckTs(ackTs);
            alarm.setStatus(alarm.getStatus().isCleared() ? AlarmStatus.CLEARED_ACK : AlarmStatus.ACTIVE_ACK);
            notificationEntityService.notifyCreateOrUpdateAlarm(alarm, ActionType.ALARM_ACK, user);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public void clear(Alarm alarm, SecurityUser user) throws ThingsboardException {
        try {
            long clearTs = System.currentTimeMillis();
            alarmService.clearAlarm(user.getTenantId(), alarm.getId(), null, clearTs).get();
            alarm.setClearTs(clearTs);
            alarm.setStatus(alarm.getStatus().isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK);
            notificationEntityService.notifyCreateOrUpdateAlarm(alarm, ActionType.ALARM_CLEAR, user);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public Boolean delete(Alarm alarm, SecurityUser user) throws ThingsboardException {
        try {
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(user.getTenantId(), alarm.getOriginator());
            notificationEntityService.notifyDeleteAlarm(user.getTenantId(), alarm, alarm.getOriginator(), user.getCustomerId(),
                    ActionType.DELETED, relatedEdgeIds, user, JacksonUtil.OBJECT_MAPPER.writeValueAsString(alarm));
            return alarmService.deleteAlarm(user.getTenantId(), alarm.getId()).isSuccessful();
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}