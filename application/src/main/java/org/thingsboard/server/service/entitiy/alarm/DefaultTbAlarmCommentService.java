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
package org.thingsboard.server.service.entitiy.alarm;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbAlarmCommentService extends AbstractTbEntityService implements TbAlarmCommentService{

    @Autowired
    private AlarmCommentService alarmCommentService;

    @Override
    public AlarmComment saveAlarmComment(Alarm alarm, AlarmComment alarmComment, User user) throws ThingsboardException {
        ActionType actionType = alarmComment.getId() == null ? ActionType.ADDED_COMMENT : ActionType.UPDATED_COMMENT;
        UserId userId = user.getId();
        alarmComment.setUserId(userId);
        try {
            AlarmComment savedAlarmComment = checkNotNull(alarmCommentService.createOrUpdateAlarmComment(alarm.getTenantId(), alarmComment));
            notificationEntityService.notifyAlarmComment(alarm, savedAlarmComment, actionType, user);
            return savedAlarmComment;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(alarm.getTenantId(), emptyId(EntityType.ALARM), alarm, actionType, user, e, alarmComment);
            throw e;
        }
    }

    @Override
    public void deleteAlarmComment(Alarm alarm, AlarmComment alarmComment, User user) {
        alarmCommentService.deleteAlarmComment(alarm.getTenantId(), alarmComment.getId());
        notificationEntityService.notifyAlarmComment(alarm, alarmComment, ActionType.DELETED_COMMENT, user);
    }
}
