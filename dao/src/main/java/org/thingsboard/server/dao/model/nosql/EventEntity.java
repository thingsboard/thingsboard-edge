/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
import com.datastax.driver.mapping.annotations.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.type.EntityTypeCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

/**
 * @author Andrew Shvayka
 */
@Data
@NoArgsConstructor
@Table(name = EVENT_COLUMN_FAMILY_NAME)
public class EventEntity implements BaseEntity<Event> {

    @Transient
    private static final long serialVersionUID = -1265181166886910153L;

    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey()
    @Column(name = EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 1)
    @Column(name = EVENT_ENTITY_TYPE_PROPERTY, codec = EntityTypeCodec.class)
    private EntityType entityType;

    @PartitionKey(value = 2)
    @Column(name = EVENT_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @ClusteringColumn()
    @Column(name = EVENT_TYPE_PROPERTY)
    private String eventType;

    @ClusteringColumn(value = 1)
    @Column(name = EVENT_UID_PROPERTY)
    private String eventUid;

    @Column(name = EVENT_BODY_PROPERTY, codec = JsonCodec.class)
    private JsonNode body;

    public EventEntity(Event event) {
        if (event.getId() != null) {
            this.id = event.getId().getId();
        }
        if (event.getTenantId() != null) {
            this.tenantId = event.getTenantId().getId();
        }
        if (event.getEntityId() != null) {
            this.entityType = event.getEntityId().getEntityType();
            this.entityId = event.getEntityId().getId();
        }
        this.eventType = event.getType();
        this.eventUid = event.getUid();
        this.body = event.getBody();
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public Event toData() {
        Event event = new Event(new EventId(id));
        event.setCreatedTime(UUIDs.unixTimestamp(id));
        event.setTenantId(new TenantId(tenantId));
        event.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        event.setBody(body);
        event.setType(eventType);
        event.setUid(eventUid);
        return event;
    }
}
