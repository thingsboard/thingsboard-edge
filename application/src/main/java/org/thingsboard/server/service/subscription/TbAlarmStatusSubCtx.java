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
package org.thingsboard.server.service.subscription;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.query.OriginatorAlarmFilter;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmStatusCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmStatusUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.AlarmSubscriptionUpdate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@ToString(callSuper = true)
public class TbAlarmStatusSubCtx extends TbAbstractSubCtx {

    private final AlarmService alarmService;
    private final int alarmsPerAlarmStatusSubscriptionCacheSize;

    private volatile TbAlarmStatusSubscription subscription;

    public TbAlarmStatusSubCtx(String serviceId, WebSocketService wsService,
                               TbLocalSubscriptionService localSubscriptionService,
                               SubscriptionServiceStatistics stats, AlarmService alarmService,
                               int alarmsPerAlarmStatusSubscriptionCacheSize,
                               WebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, localSubscriptionService, stats, sessionRef, cmdId);
        this.alarmService = alarmService;
        this.alarmsPerAlarmStatusSubscriptionCacheSize = alarmsPerAlarmStatusSubscriptionCacheSize;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public void stop() {
        super.stop();
        localSubscriptionService.cancelSubscription(getTenantId(), sessionRef.getSessionId(), subscription.getSubscriptionId());
    }

    public void createSubscription(AlarmStatusCmd cmd) {
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        subscription = TbAlarmStatusSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(sessionRef.getSessionSubIdSeq().incrementAndGet())
                .tenantId(securityCtx.getTenantId())
                .entityId(cmd.getOriginatorId())
                .typeList(cmd.getTypeList())
                .severityList(cmd.getSeverityList())
                .updateProcessor(this::handleAlarmStatusSubscriptionUpdate)
                .build();
        localSubscriptionService.addSubscription(subscription, sessionRef);
    }

    public void sendUpdate() {
        sendWsMsg(AlarmStatusUpdate.builder()
                .cmdId(cmdId)
                .active(subscription.hasAlarms())
                .build());
    }

    public void fetchActiveAlarms() {
        log.trace("[{}, subId: {}] Fetching active alarms from DB", subscription.getSessionId(), subscription.getSubscriptionId());
        OriginatorAlarmFilter originatorAlarmFilter = new OriginatorAlarmFilter(subscription.getEntityId(), subscription.getTypeList(), subscription.getSeverityList());
        List<UUID> alarmIds = alarmService.findActiveOriginatorAlarms(subscription.getTenantId(), originatorAlarmFilter, alarmsPerAlarmStatusSubscriptionCacheSize);

        subscription.getAlarmIds().addAll(alarmIds);
        subscription.setHasMoreAlarmsInDB(alarmIds.size() == alarmsPerAlarmStatusSubscriptionCacheSize);
    }

    private void handleAlarmStatusSubscriptionUpdate(TbSubscription<AlarmSubscriptionUpdate> sub, AlarmSubscriptionUpdate subscriptionUpdate) {
        try {
            AlarmInfo alarm = subscriptionUpdate.getAlarm();
            Set<UUID> alarmsIds = subscription.getAlarmIds();
            if (alarmsIds.contains(alarm.getId().getId())) {
                if (!subscription.matches(alarm) || subscriptionUpdate.isAlarmDeleted()) {
                    alarmsIds.remove(alarm.getId().getId());
                    if (alarmsIds.isEmpty()) {
                        if (subscription.isHasMoreAlarmsInDB()) {
                            fetchActiveAlarms();
                            if (alarmsIds.isEmpty()) {
                                sendUpdate();
                            }
                        } else {
                            sendUpdate();
                        }
                    }
                }
            } else if (subscription.matches(alarm)) {
                if (alarmsIds.size() < alarmsPerAlarmStatusSubscriptionCacheSize) {
                    alarmsIds.add(alarm.getId().getId());
                    if (alarmsIds.size() == 1) {
                        sendUpdate();
                    }
                } else {
                    subscription.setHasMoreAlarmsInDB(true);
                }
            }
        } catch (Exception e) {
            log.error("[{}, subId: {}] Failed to handle update for alarm status subscription: {}", subscription.getSessionId(), subscription.getSubscriptionId(), subscriptionUpdate, e);
        }
    }
}
