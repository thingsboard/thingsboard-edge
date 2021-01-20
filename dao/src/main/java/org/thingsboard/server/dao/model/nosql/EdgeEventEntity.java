/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.type.EdgeEventActionTypeCodec;
import org.thingsboard.server.dao.model.type.EdgeEventTypeCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

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
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;

@Data
@NoArgsConstructor
@Table(name = EDGE_EVENT_COLUMN_FAMILY_NAME)
public class EdgeEventEntity implements BaseEntity<EdgeEvent> {

    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey()
    @Column(name = EDGE_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 1)
    @Column(name = EDGE_EVENT_EDGE_ID_PROPERTY)
    private UUID edgeId;

    @ClusteringColumn()
    @Column(name = EDGE_EVENT_TYPE_PROPERTY, codec = EdgeEventTypeCodec.class)
    private EdgeEventType edgeEventType;

    @ClusteringColumn(value = 1)
    @Column(name = EDGE_EVENT_ACTION_PROPERTY, codec = EdgeEventActionTypeCodec.class)
    private EdgeEventActionType edgeEventAction;

    @ClusteringColumn(value = 2)
    @Column(name = EDGE_EVENT_UID_PROPERTY)
    private String edgeEventUid;

    @Column(name = EDGE_EVENT_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Column(name = EDGE_EVENT_ENTITY_GROUP_ID_PROPERTY)
    private UUID entityGroupId;

    @Column(name = EDGE_EVENT_BODY_PROPERTY, codec = JsonCodec.class)
    private JsonNode body;

    public EdgeEventEntity(EdgeEvent edgeEvent) {
        if (edgeEvent.getId() != null) {
            this.id = edgeEvent.getId().getId();
        }
        if (edgeEvent.getTenantId() != null) {
            this.tenantId = edgeEvent.getTenantId().getId();
        }
        if (edgeEvent.getEdgeId() != null) {
            this.edgeId = edgeEvent.getEdgeId().getId();
        }
        this.entityId = edgeEvent.getEntityId();
        this.edgeEventType = edgeEvent.getType();
        this.edgeEventAction = edgeEvent.getAction();
        this.edgeEventUid = edgeEvent.getUid();
        this.body = edgeEvent.getBody();
        this.entityGroupId = edgeEvent.getEntityGroupId();
    }

    @Override
    public UUID getUuid() {
        return id;
    }

    @Override
    public void setUuid(UUID id) {
        this.id = id;
    }

    @Override
    public EdgeEvent toData() {
        EdgeEvent edgeEvent = new EdgeEvent(new EdgeEventId(id));
        edgeEvent.setCreatedTime(UUIDs.unixTimestamp(id));
        edgeEvent.setTenantId(new TenantId(tenantId));
        edgeEvent.setEdgeId(new EdgeId(edgeId));
        edgeEvent.setEntityId(entityId);
        edgeEvent.setType(edgeEventType);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setBody(body);
        edgeEvent.setUid(edgeEventUid);
        edgeEvent.setEntityGroupId(entityGroupId);
        return edgeEvent;
    }
}
