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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationSeverity;
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
@Table(name = ModelConstants.NOTIFICATION_REQUEST_TABLE_NAME)
public class NotificationRequestEntity extends BaseSqlEntity<NotificationRequest> {

    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_TARGET_ID_PROPERTY, nullable = false)
    private UUID targetId;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_NOTIFICATION_REASON_PROPERTY, nullable = false)
    private String notificationReason;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_TEXT_TEMPLATE_PROPERTY, nullable = false)
    private String textTemplate;

    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_NOTIFICATION_INFO_PROPERTY)
    private JsonNode notificationInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_NOTIFICATION_SEVERITY_PROPERTY)
    private NotificationSeverity notificationSeverity;

    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ADDITIONAL_CONFIG_PROPERTY)
    private JsonNode additionalConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_STATUS_PROPERTY)
    private NotificationRequestStatus status;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_RULE_ID_PROPERTY)
    private UUID ruleId;

    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ALARM_ID_PROPERTY)
    private UUID alarmId;

    public NotificationRequestEntity() {}

    public NotificationRequestEntity(NotificationRequest notificationRequest) {
        setId(notificationRequest.getUuidId());
        setCreatedTime(notificationRequest.getCreatedTime());
        setTenantId(getUuid(notificationRequest.getTenantId()));
        setTargetId(getUuid(notificationRequest.getTargetId()));
        setNotificationReason(notificationRequest.getNotificationReason());
        setTextTemplate(notificationRequest.getTextTemplate());
        setNotificationInfo(toJson(notificationRequest.getNotificationInfo()));
        setNotificationSeverity(notificationRequest.getNotificationSeverity());
        setAdditionalConfig(toJson(notificationRequest.getAdditionalConfig()));
        setStatus(notificationRequest.getStatus());
        setRuleId(getUuid(notificationRequest.getRuleId()));
        setAlarmId(getUuid(notificationRequest.getAlarmId()));
    }

    @Override
    public NotificationRequest toData() {
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setId(new NotificationRequestId(id));
        notificationRequest.setCreatedTime(createdTime);
        notificationRequest.setTenantId(createId(tenantId, TenantId::new));
        notificationRequest.setTargetId(createId(targetId, NotificationTargetId::new));
        notificationRequest.setNotificationReason(notificationReason);
        notificationRequest.setTextTemplate(textTemplate);
        notificationRequest.setNotificationInfo(fromJson(notificationInfo));
        notificationRequest.setNotificationSeverity(notificationSeverity);
        notificationRequest.setAdditionalConfig(fromJson(additionalConfig));
        notificationRequest.setStatus(status);
        notificationRequest.setRuleId(createId(ruleId, NotificationRuleId::new));
        notificationRequest.setAlarmId(createId(alarmId, AlarmId::new));
        return notificationRequest;
    }

}
