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

import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.notification.info.AlarmNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmNotificationRuleTriggerConfig.ClearRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.service.notification.rule.trigger.AlarmTriggerProcessor.AlarmTriggerObject;

@Service
public class AlarmTriggerProcessor implements NotificationRuleTriggerProcessor<AlarmTriggerObject, AlarmNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(AlarmTriggerObject triggerObject, AlarmNotificationRuleTriggerConfig triggerConfig) {
        Alarm alarm = triggerObject.getAlarm();
        return (CollectionUtils.isEmpty(triggerConfig.getAlarmTypes()) || triggerConfig.getAlarmTypes().contains(alarm.getType())) &&
                (CollectionUtils.isEmpty(triggerConfig.getAlarmSeverities()) || triggerConfig.getAlarmSeverities().contains(alarm.getSeverity()));
    }

    @Override
    public boolean matchesClearRule(AlarmTriggerObject triggerObject, AlarmNotificationRuleTriggerConfig triggerConfig) {
        if (triggerObject.isDeleted()) {
            return true;
        }
        Alarm alarm = triggerObject.getAlarm();
        ClearRule clearRule = triggerConfig.getClearRule();
        if (clearRule != null) {
            if (clearRule.getAlarmStatus() != null) {
                return clearRule.getAlarmStatus().equals(alarm.getStatus());
            }
        }
        return false;
    }

    @Override
    public NotificationInfo constructNotificationInfo(AlarmTriggerObject triggerObject, AlarmNotificationRuleTriggerConfig triggerConfig) {
        Alarm alarm = triggerObject.getAlarm();
        return AlarmNotificationInfo.builder()
                .alarmId(alarm.getUuidId())
                .alarmType(alarm.getType())
                .alarmOriginator(alarm.getOriginator())
                .alarmSeverity(alarm.getSeverity())
                .alarmStatus(alarm.getStatus())
                .alarmCustomerId(alarm.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM;
    }

    @Data
    @Builder
    public static class AlarmTriggerObject {
        private final Alarm alarm;
        private final boolean deleted;
    }

}
