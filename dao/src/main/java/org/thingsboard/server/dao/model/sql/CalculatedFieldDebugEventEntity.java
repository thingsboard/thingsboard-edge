/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.CalculatedFieldDebugEvent;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_DEBUG_EVENT_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_CALCULATED_FIELD_ARGUMENTS_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_CALCULATED_FIELD_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_CALCULATED_FIELD_RESULT_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MSG_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MSG_TYPE_COLUMN_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CALCULATED_FIELD_DEBUG_EVENT_TABLE_NAME)
@NoArgsConstructor
public class CalculatedFieldDebugEventEntity extends EventEntity<CalculatedFieldDebugEvent> implements BaseEntity<CalculatedFieldDebugEvent> {

    @Column(name = EVENT_CALCULATED_FIELD_ID_COLUMN_NAME)
    private UUID calculatedFieldId;
    @Column(name = EVENT_ENTITY_ID_COLUMN_NAME)
    private UUID eventEntityId;
    @Column(name = EVENT_ENTITY_TYPE_COLUMN_NAME)
    private String eventEntityType;
    @Column(name = EVENT_MSG_ID_COLUMN_NAME)
    private UUID msgId;
    @Column(name = EVENT_MSG_TYPE_COLUMN_NAME)
    private String msgType;
    @Column(name = EVENT_CALCULATED_FIELD_ARGUMENTS_COLUMN_NAME)
    private String arguments;
    @Column(name = EVENT_CALCULATED_FIELD_RESULT_COLUMN_NAME)
    private String result;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public CalculatedFieldDebugEventEntity(CalculatedFieldDebugEvent event) {
        super(event);
        if (event.getCalculatedFieldId() != null) {
            this.calculatedFieldId = event.getCalculatedFieldId().getId();
        }
        if (event.getEventEntity() != null) {
            this.eventEntityId = event.getEventEntity().getId();
            this.eventEntityType = event.getEventEntity().getEntityType().name();
        }
        this.msgId = event.getMsgId();
        this.msgType = event.getMsgType();
        this.arguments = event.getArguments();
        this.result = event.getResult();
        this.error = event.getError();
    }

    @Override
    public CalculatedFieldDebugEvent toData() {
        var builder = CalculatedFieldDebugEvent.builder()
                .id(id)
                .tenantId(TenantId.fromUUID(tenantId))
                .ts(ts)
                .serviceId(serviceId)
                .entityId(entityId)
                .msgId(msgId)
                .msgType(msgType)
                .arguments(arguments)
                .result(result)
                .error(error);
        if (calculatedFieldId != null) {
            builder.calculatedFieldId(new CalculatedFieldId(calculatedFieldId));
        }
        if (eventEntityId != null) {
            builder.eventEntity(EntityIdFactory.getByTypeAndUuid(eventEntityType, eventEntityId));
        }
        return builder.build();
    }

}
