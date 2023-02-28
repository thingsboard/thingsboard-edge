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
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_DATA_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_DATA_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_METADATA_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MSG_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MSG_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_RELATION_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_NODE_DEBUG_EVENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RULE_NODE_DEBUG_EVENT_TABLE_NAME)
@NoArgsConstructor
public class RuleNodeDebugEventEntity extends EventEntity<RuleNodeDebugEvent> implements BaseEntity<RuleNodeDebugEvent> {

    @Column(name = EVENT_TYPE_COLUMN_NAME)
    private String eventType;
    @Column(name = EVENT_ENTITY_ID_COLUMN_NAME)
    private UUID eventEntityId;
    @Column(name = EVENT_ENTITY_TYPE_COLUMN_NAME)
    private String eventEntityType;
    @Column(name = EVENT_MSG_ID_COLUMN_NAME)
    private UUID msgId;
    @Column(name = EVENT_MSG_TYPE_COLUMN_NAME)
    private String msgType;
    @Column(name = EVENT_DATA_TYPE_COLUMN_NAME)
    private String dataType;
    @Column(name = EVENT_RELATION_TYPE_COLUMN_NAME)
    private String relationType;
    @Column(name = EVENT_DATA_COLUMN_NAME)
    private String data;
    @Column(name = EVENT_METADATA_COLUMN_NAME)
    private String metadata;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public RuleNodeDebugEventEntity(RuleNodeDebugEvent event) {
        super(event);
        this.eventType = event.getEventType();
        if (event.getEventEntity() != null) {
            this.eventEntityId = event.getEventEntity().getId();
            this.eventEntityType = event.getEventEntity().getEntityType().name();
        }
        this.msgId = event.getMsgId();
        this.msgType = event.getMsgType();
        this.dataType = event.getDataType();
        this.relationType = event.getRelationType();
        this.data = event.getData();
        this.metadata = event.getMetadata();
        this.error = event.getError();
    }

    @Override
    public RuleNodeDebugEvent toData() {
        var builder = RuleNodeDebugEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .eventType(eventType)
                .msgId(msgId)
                .msgType(msgType)
                .dataType(dataType)
                .relationType(relationType)
                .data(data)
                .metadata(metadata)
                .error(error);
        if (eventEntityId != null) {
            builder.eventEntity(EntityIdFactory.getByTypeAndUuid(eventEntityType, eventEntityId));
        }
        return builder.build();
    }

}
