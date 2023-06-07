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
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlarmAssignmentNotificationInfo implements RuleOriginatedNotificationInfo {

    private String action;

    private String assigneeEmail;
    private String assigneeFirstName;
    private String assigneeLastName;
    private UserId assigneeId;

    private String userEmail;
    private String userFirstName;
    private String userLastName;

    private String alarmType;
    private UUID alarmId;
    private EntityId alarmOriginator;
    private String alarmOriginatorName;
    private AlarmSeverity alarmSeverity;
    private AlarmStatus alarmStatus;
    private CustomerId alarmCustomerId;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "action", action,
                "assigneeTitle", User.getTitle(assigneeEmail, assigneeFirstName, assigneeLastName),
                "assigneeFirstName", assigneeFirstName,
                "assigneeLastName", assigneeLastName,
                "assigneeEmail", assigneeEmail,
                "assigneeId", assigneeId != null ? assigneeId.toString() : null,
                "userTitle", User.getTitle(userEmail, userFirstName, userLastName),
                "userEmail", userEmail,
                "userFirstName", userFirstName,
                "userLastName", userLastName,
                "alarmType", alarmType,
                "alarmId", alarmId.toString(),
                "alarmSeverity", alarmSeverity.name().toLowerCase(),
                "alarmStatus", alarmStatus.toString(),
                "alarmOriginatorEntityType", alarmOriginator.getEntityType().getNormalName(),
                "alarmOriginatorId", alarmOriginator.getId().toString(),
                "alarmOriginatorName", alarmOriginatorName
        );
    }

    @Override
    public CustomerId getAffectedCustomerId() {
        return alarmCustomerId;
    }

    @Override
    public UserId getAffectedUserId() {
        return assigneeId;
    }

    @Override
    public EntityId getStateEntityId() {
        return alarmOriginator;
    }

}
