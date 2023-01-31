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
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_ACTION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_BODY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_EDGE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_ENTITY_GROUP_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_UID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EPOCH_DIFF;
import static org.thingsboard.server.dao.model.ModelConstants.TS_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = EDGE_EVENT_COLUMN_FAMILY_NAME)
@NoArgsConstructor
public class EdgeEventEntity extends BaseSqlEntity<EdgeEvent> implements BaseEntity<EdgeEvent> {

    @Column(name = EDGE_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = EDGE_EVENT_EDGE_ID_PROPERTY)
    private UUID edgeId;

    @Column(name = EDGE_EVENT_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = EDGE_EVENT_TYPE_PROPERTY)
    private EdgeEventType edgeEventType;

    @Enumerated(EnumType.STRING)
    @Column(name = EDGE_EVENT_ACTION_PROPERTY)
    private EdgeEventActionType edgeEventAction;

    @Type(type = "json")
    @Column(name = EDGE_EVENT_BODY_PROPERTY)
    private JsonNode entityBody;

    @Column(name = EDGE_EVENT_UID_PROPERTY)
    private String edgeEventUid;

    @Column(name = EDGE_EVENT_ENTITY_GROUP_ID_PROPERTY)
    private UUID entityGroupId;

    @Column(name = TS_COLUMN)
    private long ts;

    public EdgeEventEntity(EdgeEvent edgeEvent) {
        if (edgeEvent.getId() != null) {
            this.setUuid(edgeEvent.getId().getId());
            this.ts = getTs(edgeEvent.getId().getId());
        } else {
            this.ts = System.currentTimeMillis();
        }
        this.setCreatedTime(edgeEvent.getCreatedTime());
        if (edgeEvent.getTenantId() != null) {
            this.tenantId = edgeEvent.getTenantId().getId();
        }
        if (edgeEvent.getEdgeId() != null) {
            this.edgeId = edgeEvent.getEdgeId().getId();
        }
        if (edgeEvent.getEntityId() != null) {
            this.entityId = edgeEvent.getEntityId();
        }
        if (edgeEvent.getEntityGroupId() != null) {
            this.entityGroupId = edgeEvent.getEntityGroupId();
        }
        this.edgeEventType = edgeEvent.getType();
        this.edgeEventAction = edgeEvent.getAction();
        this.entityBody = edgeEvent.getBody();
        this.edgeEventUid = edgeEvent.getUid();
    }

    @Override
    public EdgeEvent toData() {
        EdgeEvent edgeEvent = new EdgeEvent(new EdgeEventId(this.getUuid()));
        edgeEvent.setCreatedTime(createdTime);
        edgeEvent.setTenantId(TenantId.fromUUID(tenantId));
        edgeEvent.setEdgeId(new EdgeId(edgeId));
        if (entityId != null) {
            edgeEvent.setEntityId(entityId);
        }
        if (entityGroupId != null) {
            edgeEvent.setEntityGroupId(entityGroupId);
        }
        edgeEvent.setType(edgeEventType);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setBody(entityBody);
        edgeEvent.setUid(edgeEventUid);
        return edgeEvent;
    }

    private static long getTs(UUID uuid) {
        return (uuid.timestamp() - EPOCH_DIFF) / 10000;
    }
}
