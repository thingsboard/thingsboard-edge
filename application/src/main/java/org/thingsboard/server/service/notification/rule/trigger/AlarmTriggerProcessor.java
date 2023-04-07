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
package org.thingsboard.server.service.notification.rule.trigger;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.notification.info.AlarmNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig.AlarmAction;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig.ClearRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.msg.notification.trigger.AlarmTrigger;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Service
public class AlarmTriggerProcessor implements NotificationRuleTriggerProcessor<AlarmTrigger, AlarmNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(AlarmTrigger trigger, AlarmNotificationRuleTriggerConfig triggerConfig) {
        AlarmApiCallResult alarmUpdate = trigger.getAlarmUpdate();
        Alarm alarm = alarmUpdate.getAlarm();
        if (!typeMatches(alarm, triggerConfig)) {
            return false;
        }

        if (alarmUpdate.isCreated()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.CREATED)) {
                return severityMatches(alarm, triggerConfig);
            }
        }  else if (alarmUpdate.isSeverityChanged()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.SEVERITY_CHANGED)) {
                return severityMatches(alarmUpdate.getOld(), triggerConfig) || severityMatches(alarm, triggerConfig);
            }  else {
                // if we haven't yet sent notification about the alarm
                return !severityMatches(alarmUpdate.getOld(), triggerConfig) && severityMatches(alarm, triggerConfig);
            }
        } else if (alarmUpdate.isAcknowledged()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.ACKNOWLEDGED)) {
                return severityMatches(alarm, triggerConfig);
            }
        } else if (alarmUpdate.isCleared()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.CLEARED)) {
                return severityMatches(alarm, triggerConfig);
            }
        }
        return false;
    }

    @Override
    public boolean matchesClearRule(AlarmTrigger trigger, AlarmNotificationRuleTriggerConfig triggerConfig) {
        AlarmApiCallResult alarmUpdate = trigger.getAlarmUpdate();
        Alarm alarm = alarmUpdate.getAlarm();
        if (!typeMatches(alarm, triggerConfig)) {
            return false;
        }
        if (alarmUpdate.isDeleted()) {
            return true;
        }
        ClearRule clearRule = triggerConfig.getClearRule();
        if (clearRule != null) {
            if (isNotEmpty(clearRule.getAlarmStatuses())) {
                return AlarmStatusFilter.from(clearRule.getAlarmStatuses()).matches(alarm);
            }
        }
        return false;
    }

    private boolean severityMatches(Alarm alarm, AlarmNotificationRuleTriggerConfig triggerConfig) {
        return isEmpty(triggerConfig.getAlarmSeverities()) || triggerConfig.getAlarmSeverities().contains(alarm.getSeverity());
    }

    private boolean typeMatches(Alarm alarm, AlarmNotificationRuleTriggerConfig triggerConfig) {
        return isEmpty(triggerConfig.getAlarmTypes()) || triggerConfig.getAlarmTypes().contains(alarm.getType());
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(AlarmTrigger trigger) {
        AlarmApiCallResult alarmUpdate = trigger.getAlarmUpdate();
        AlarmInfo alarmInfo = alarmUpdate.getAlarm();
        return AlarmNotificationInfo.builder()
                .alarmId(alarmInfo.getUuidId())
                .alarmType(alarmInfo.getType())
                .action(alarmUpdate.isCreated() ? "created" :
                        alarmUpdate.isSeverityChanged() ? "severity changed" :
                        alarmUpdate.isAcknowledged() ? "acknowledged" :
                        alarmUpdate.isCleared() ? "cleared" :
                        alarmUpdate.isDeleted() ? "deleted" : null)
                .alarmOriginator(alarmInfo.getOriginator())
                .alarmOriginatorName(alarmInfo.getOriginatorName())
                .alarmSeverity(alarmInfo.getSeverity())
                .alarmStatus(alarmInfo.getStatus())
                .alarmCustomerId(alarmInfo.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM;
    }

}
