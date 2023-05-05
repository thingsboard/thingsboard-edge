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
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.NOTIFICATION_TABLE_NAME)
public class NotificationEntity extends BaseSqlEntity<Notification> {

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ID_PROPERTY, nullable = false)
    private UUID requestId;

    @Column(name = ModelConstants.NOTIFICATION_RECIPIENT_ID_PROPERTY, nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_TYPE_PROPERTY, nullable = false)
    private NotificationType type;

    @Column(name = ModelConstants.NOTIFICATION_SUBJECT_PROPERTY)
    private String subject;

    @Column(name = ModelConstants.NOTIFICATION_TEXT_PROPERTY, nullable = false)
    private String text;

    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_ADDITIONAL_CONFIG_PROPERTY)
    private JsonNode additionalConfig;

    @Type(type = "json")
    @Formula("(SELECT r.info FROM notification_request r WHERE r.id = request_id)")
    private JsonNode info;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_STATUS_PROPERTY)
    private NotificationStatus status;

    public NotificationEntity() {}

    public NotificationEntity(Notification notification) {
        setId(notification.getUuidId());
        setCreatedTime(notification.getCreatedTime());
        setRequestId(getUuid(notification.getRequestId()));
        setRecipientId(getUuid(notification.getRecipientId()));
        setType(notification.getType());
        setSubject(notification.getSubject());
        setText(notification.getText());
        setAdditionalConfig(notification.getAdditionalConfig());
        setInfo(toJson(notification.getInfo()));
        setStatus(notification.getStatus());
    }

    @Override
    public Notification toData() {
        Notification notification = new Notification();
        notification.setId(new NotificationId(id));
        notification.setCreatedTime(createdTime);
        notification.setRequestId(getEntityId(requestId, NotificationRequestId::new));
        notification.setRecipientId(getEntityId(recipientId, UserId::new));
        notification.setType(type);
        notification.setSubject(subject);
        notification.setText(text);
        notification.setAdditionalConfig(additionalConfig);
        notification.setInfo(fromJson(info, NotificationInfo.class));
        notification.setStatus(status);
        return notification;
    }

}
