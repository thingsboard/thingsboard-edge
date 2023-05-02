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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.UserId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGNEE_EMAIL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGNEE_FIRST_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGNEE_LAST_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_LABEL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_STATUS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_VIEW_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ALARM_VIEW_NAME)
public class AlarmInfoEntity extends AbstractAlarmEntity<AlarmInfo> {

    @Column(name = ALARM_ORIGINATOR_NAME_PROPERTY)
    private String originatorName;
    @Column(name = ALARM_ORIGINATOR_LABEL_PROPERTY)
    private String originatorLabel;
    @Column(name = ALARM_ASSIGNEE_FIRST_NAME_PROPERTY)
    private String assigneeFirstName;
    @Column(name = ALARM_ASSIGNEE_LAST_NAME_PROPERTY)
    private String assigneeLastName;
    @Column(name = ALARM_ASSIGNEE_EMAIL_PROPERTY)
    private String assigneeEmail;
    @Column(name = ALARM_STATUS_PROPERTY)
    private String status;

    public AlarmInfoEntity() {
        super();
    }

    @Override
    public AlarmInfo toData() {
        AlarmInfo alarmInfo = new AlarmInfo(super.toAlarm());
        alarmInfo.setOriginatorName(originatorName);
        alarmInfo.setOriginatorLabel(originatorLabel);
        if (getAssigneeId() != null) {
            alarmInfo.setAssignee(new AlarmAssignee(new UserId(getAssigneeId()), assigneeFirstName, assigneeLastName, assigneeEmail));
        }
        return alarmInfo;
    }
}
