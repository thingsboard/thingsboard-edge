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
package org.thingsboard.server.service.notification;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationSeverity;
import org.thingsboard.server.common.data.notification.rule.NonConfirmedNotificationEscalation;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.List;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultNotificationRuleProcessingService implements NotificationRuleProcessingService {

    private final NotificationRuleService notificationRuleService;
    private final NotificationService notificationService;
    private final NotificationSubscriptionService notificationSubscriptionService;
    private final DbCallbackExecutorService dbCallbackExecutorService;

    @Override
    public ListenableFuture<?> onAlarmCreatedOrUpdated(TenantId tenantId, Alarm alarm) {
        return processAlarmUpdate(tenantId, alarm);
    }

    @Override
    public ListenableFuture<?> onAlarmAcknowledged(TenantId tenantId, Alarm alarm) {
        return processAlarmUpdate(tenantId, alarm);
    }

    private ListenableFuture<?> processAlarmUpdate(TenantId tenantId, Alarm alarm) {
        if (alarm.getNotificationRuleId() == null) return Futures.immediateFuture(null);
        return dbCallbackExecutorService.submit(() -> {
            onAlarmUpdate(tenantId, alarm.getNotificationRuleId(), alarm);
        });
    }

    private void onAlarmUpdate(TenantId tenantId, NotificationRuleId notificationRuleId, Alarm alarm) {
        List<NotificationRequest> notificationRequests = notificationService.findNotificationRequestsByRuleIdAndAlarmId(tenantId, notificationRuleId, alarm.getId());
        NotificationRule notificationRule = notificationRuleService.findNotificationRuleById(tenantId, notificationRuleId);

        if (notificationRequests.isEmpty()) { // in case it is first notification for alarm, or it was previously acked and now we need to send notifications again
            NotificationTargetId initialNotificationTargetId = notificationRule.getInitialNotificationTargetId();
            submitNotificationRequest(tenantId, initialNotificationTargetId, notificationRule, alarm, 0);

            for (NonConfirmedNotificationEscalation escalation : notificationRule.getEscalations()) {
                submitNotificationRequest(tenantId, escalation.getNotificationTargetId(), notificationRule, alarm, escalation.getDelayInMinutes());
            }
        } else {
            if (alarmAcknowledged(alarm)) {
                for (NotificationRequest notificationRequest : notificationRequests) {
                    if (notificationRequest.getStatus() == NotificationRequestStatus.SCHEDULED) {
                        // using regular service due to no need to send an update to subscription manager
                        notificationService.deleteNotificationRequestById(tenantId, notificationRequest.getId());
                    } else {
                        notificationSubscriptionService.deleteNotificationRequest(tenantId, notificationRequest.getId());
                        // todo: or should we mark already sent notifications as read?
                    }
                }
            } else {
                NotificationInfo newNotificationInfo = constructNotificationInfo(alarm, notificationRule);
                for (NotificationRequest notificationRequest : notificationRequests) {
                    NotificationInfo previousNotificationInfo = notificationRequest.getNotificationInfo();
                    if (!previousNotificationInfo.equals(newNotificationInfo)) {
                        notificationRequest.setNotificationInfo(newNotificationInfo);
                        notificationSubscriptionService.updateNotificationRequest(tenantId, notificationRequest);
                    }
                    // fixme: no need to send an update event for scheduled requests, only for sent
                }
            }
        }
    }

    private boolean alarmAcknowledged(Alarm alarm) { // todo: decide when to consider the alarm processed by notification target (not to escalate then)
        return alarm.getStatus().isAck() && alarm.getStatus().isCleared();
    }

    private void submitNotificationRequest(TenantId tenantId, NotificationTargetId targetId, NotificationRule notificationRule, Alarm alarm, int delayInMinutes) {
        NotificationRequestConfig config = new NotificationRequestConfig();
        if (delayInMinutes > 0) {
            config.setSendingDelayInMinutes(delayInMinutes);
        }
        NotificationInfo notificationInfo = constructNotificationInfo(alarm, notificationRule);

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(tenantId)
                .targetId(targetId)
                .notificationReason("Alarm")
                .textTemplate(notificationRule.getNotificationTextTemplate()) // todo: format with alarm vars
                .notificationInfo(notificationInfo)
                .notificationSeverity(NotificationSeverity.NORMAL) // todo: from alarm severity
                .additionalConfig(config)
                .ruleId(notificationRule.getId())
                .alarmId(alarm.getId())
                .build();
        notificationSubscriptionService.processNotificationRequest(tenantId, notificationRequest);
    }

    private NotificationInfo constructNotificationInfo(Alarm alarm, NotificationRule notificationRule) {
        return NotificationInfo.builder()
                .alarmId(alarm.getId())
                .alarmType(alarm.getType())
                .alarmOriginator(alarm.getOriginator())
                .alarmSeverity(alarm.getSeverity())
                .alarmStatus(alarm.getStatus())
                .build();
    }

}
