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
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CREATED_TIME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ALARM_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@Entity
@Table(name = ENTITY_ALARM_COLUMN_FAMILY_NAME)
@IdClass(EntityAlarmCompositeKey.class)
public final class EntityAlarmEntity implements ToData<EntityAlarm> {

    @Column(name = TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = ENTITY_TYPE_COLUMN)
    private String entityType;

    @Id
    @Column(name = ENTITY_ID_COLUMN, columnDefinition = "uuid")
    private UUID entityId;

    @Id
    @Column(name = "alarm_id", columnDefinition = "uuid")
    private UUID alarmId;

    @Column(name = CREATED_TIME_PROPERTY)
    private long createdTime;

    @Column(name = "alarm_type")
    private String alarmType;

    @Column(name = CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    public EntityAlarmEntity() {
        super();
    }

    public EntityAlarmEntity(EntityAlarm entityAlarm) {
        tenantId = entityAlarm.getTenantId().getId();
        entityId = entityAlarm.getEntityId().getId();
        entityType = entityAlarm.getEntityId().getEntityType().name();
        alarmId = entityAlarm.getAlarmId().getId();
        alarmType = entityAlarm.getAlarmType();
        createdTime = entityAlarm.getCreatedTime();
        if (entityAlarm.getCustomerId() != null) {
            customerId = entityAlarm.getCustomerId().getId();
        }
    }

    @Override
    public EntityAlarm toData() {
        EntityAlarm result = new EntityAlarm();
        result.setTenantId(TenantId.fromUUID(tenantId));
        result.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        result.setAlarmId(new AlarmId(alarmId));
        result.setAlarmType(alarmType);
        result.setCreatedTime(createdTime);
        if (customerId != null) {
            result.setCustomerId(new CustomerId(customerId));
        }
        return result;
    }

}