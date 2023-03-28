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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.notification.info.AlarmAssignmentNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmAssignmentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmAssignmentNotificationRuleTriggerConfig.Action;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.dao.notification.trigger.RuleEngineMsgTrigger;

import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Service
public class AlarmAssignmentTriggerProcessor implements RuleEngineMsgNotificationRuleTriggerProcessor<AlarmAssignmentNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(RuleEngineMsgTrigger trigger, AlarmAssignmentNotificationRuleTriggerConfig triggerConfig) {
        Action action = trigger.getMsg().getType().equals(DataConstants.ALARM_ASSIGN) ? Action.ASSIGNED : Action.UNASSIGNED;
        if (!triggerConfig.getNotifyOn().contains(action)) {
            return false;
        }
        Alarm alarm = JacksonUtil.fromString(trigger.getMsg().getData(), Alarm.class);
        return (isEmpty(triggerConfig.getAlarmTypes()) || triggerConfig.getAlarmTypes().contains(alarm.getType())) &&
                (isEmpty(triggerConfig.getAlarmSeverities()) || triggerConfig.getAlarmSeverities().contains(alarm.getSeverity())) &&
                (isEmpty(triggerConfig.getAlarmStatuses()) || AlarmStatusFilter.from(triggerConfig.getAlarmStatuses()).matches(alarm));
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(RuleEngineMsgTrigger trigger) {
        AlarmInfo alarmInfo = JacksonUtil.fromString(trigger.getMsg().getData(), AlarmInfo.class);
        AlarmAssignee assignee = alarmInfo.getAssignee();
        return AlarmAssignmentNotificationInfo.builder()
                .action(trigger.getMsg().getType().equals(DataConstants.ALARM_ASSIGN) ? "assigned" : "unassigned")
                .assigneeFirstName(assignee != null ? assignee.getFirstName() : null)
                .assigneeLastName(assignee != null ? assignee.getLastName() : null)
                .assigneeEmail(assignee != null ? assignee.getEmail() : null)
                .assigneeId(assignee != null ? assignee.getId() : null)
                .userEmail(trigger.getMsg().getMetaData().getValue("userEmail"))
                .userFirstName(trigger.getMsg().getMetaData().getValue("userFirstName"))
                .userLastName(trigger.getMsg().getMetaData().getValue("userLastName"))
                .alarmId(alarmInfo.getUuidId())
                .alarmType(alarmInfo.getType())
                .alarmOriginator(alarmInfo.getOriginator())
                .alarmOriginatorName(alarmInfo.getOriginatorName())
                .alarmSeverity(alarmInfo.getSeverity())
                .alarmStatus(alarmInfo.getStatus())
                .alarmCustomerId(alarmInfo.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM_ASSIGNMENT;
    }

    @Override
    public Set<String> getSupportedMsgTypes() {
        return Set.of(DataConstants.ALARM_ASSIGN, DataConstants.ALARM_UNASSIGN);
    }

}
