/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.actors.rule;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.extensions.api.device.DeviceMetaData;
import org.thingsboard.server.extensions.api.rules.RuleContext;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class RuleProcessingContext implements RuleContext {

    private final TimeseriesService tsService;
    private final EventService eventService;
    private final AlarmService alarmService;
    private final RuleId ruleId;
    private TenantId tenantId;
    private CustomerId customerId;
    private DeviceId deviceId;
    private DeviceMetaData deviceMetaData;

    RuleProcessingContext(ActorSystemContext systemContext, RuleId ruleId) {
        this.tsService = systemContext.getTsService();
        this.eventService = systemContext.getEventService();
        this.alarmService = systemContext.getAlarmService();
        this.ruleId = ruleId;
    }

    void update(ToDeviceActorMsg toDeviceActorMsg, DeviceMetaData deviceMetaData) {
        this.tenantId = toDeviceActorMsg.getTenantId();
        this.customerId = toDeviceActorMsg.getCustomerId();
        this.deviceId = toDeviceActorMsg.getDeviceId();
        this.deviceMetaData = deviceMetaData;
    }

    @Override
    public RuleId getRuleId() {
        return ruleId;
    }

    @Override
    public DeviceMetaData getDeviceMetaData() {
        return deviceMetaData;
    }

    @Override
    public Event save(Event event) {
        checkEvent(event);
        return eventService.save(event);
    }

    @Override
    public Optional<Event> saveIfNotExists(Event event) {
        checkEvent(event);
        return eventService.saveIfNotExists(event);
    }

    @Override
    public Optional<Event> findEvent(String eventType, String eventUid) {
        return eventService.findEvent(tenantId, deviceId, eventType, eventUid);
    }

    @Override
    public Alarm createOrUpdateAlarm(Alarm alarm) {
        alarm.setTenantId(tenantId);
        return alarmService.createOrUpdateAlarm(alarm);
    }

    public Optional<Alarm> findLatestAlarm(EntityId originator, String alarmType) {
        try {
            return Optional.ofNullable(alarmService.findLatestByOriginatorAndType(tenantId, originator, alarmType).get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to lookup alarm!", e);
        }
    }

    @Override
    public ListenableFuture<Boolean> clearAlarm(AlarmId alarmId, long clearTs) {
        return alarmService.clearAlarm(alarmId, clearTs);
    }

    private void checkEvent(Event event) {
        if (event.getTenantId() == null) {
            event.setTenantId(tenantId);
        } else if (!tenantId.equals(event.getTenantId())) {
            throw new IllegalArgumentException("Invalid Tenant id!");
        }
        if (event.getEntityId() == null) {
            event.setEntityId(deviceId);
        }
    }
}
