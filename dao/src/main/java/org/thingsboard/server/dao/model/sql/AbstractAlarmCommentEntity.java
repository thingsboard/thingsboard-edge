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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_ALARM_ID;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_COMMENT;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_TYPE;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractAlarmCommentEntity<T extends AlarmComment> extends BaseSqlEntity<T> implements BaseEntity<T> {

    @Column(name = ALARM_COMMENT_ALARM_ID, columnDefinition = "uuid")
    private UUID alarmId;

    @Column(name = ModelConstants.ALARM_COMMENT_USER_ID)
    private UUID userId;

    @Column(name = ALARM_COMMENT_TYPE)
    private AlarmCommentType type;

    @Type(type = "json")
    @Column(name = ALARM_COMMENT_COMMENT)
    private JsonNode comment;

    public AbstractAlarmCommentEntity() {
        super();
    }

    public AbstractAlarmCommentEntity(AlarmComment alarmComment) {
        if (alarmComment.getId() != null) {
            this.setUuid(alarmComment.getUuidId());
        }
        this.setCreatedTime(alarmComment.getCreatedTime());
        this.alarmId = alarmComment.getAlarmId().getId();
        if (alarmComment.getUserId() != null) {
            this.userId = alarmComment.getUserId().getId();
        }
        if (alarmComment.getType() != null) {
            this.type = alarmComment.getType();
        }
        this.setComment(alarmComment.getComment());
    }

    public AbstractAlarmCommentEntity(AlarmCommentEntity alarmCommentEntity) {
        this.setId(alarmCommentEntity.getId());
        this.setCreatedTime(alarmCommentEntity.getCreatedTime());
        this.userId = alarmCommentEntity.getUserId();
        this.alarmId = alarmCommentEntity.getAlarmId();
        this.type = alarmCommentEntity.getType();
        this.comment = alarmCommentEntity.getComment();
    }
    protected AlarmComment toAlarmComment() {
        AlarmComment alarmComment = new AlarmComment(new AlarmCommentId(id));
        alarmComment.setCreatedTime(createdTime);
        alarmComment.setAlarmId(new AlarmId(alarmId));
        if (userId != null) {
            alarmComment.setUserId(new UserId(userId));
        }
        alarmComment.setType(type);
        alarmComment.setComment(comment);
        return alarmComment;
    }
}
