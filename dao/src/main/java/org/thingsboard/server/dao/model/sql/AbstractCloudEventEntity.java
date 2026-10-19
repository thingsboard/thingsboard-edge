// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CloudEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_ACTION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_ENTITY_BODY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_SEQUENTIAL_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EPOCH_DIFF;
import static org.thingsboard.server.dao.model.ModelConstants.TS_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public class AbstractCloudEventEntity extends BaseSqlEntity<CloudEvent> implements BaseEntity<CloudEvent> {

    @Column(name = CLOUD_EVENT_SEQUENTIAL_ID_PROPERTY)
    protected long seqId;

    @Column(name = CLOUD_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = CLOUD_EVENT_ENTITY_ID_PROPERTY)
    private UUID entityId;


    @Enumerated(EnumType.STRING)
    @Column(name = CLOUD_EVENT_TYPE_PROPERTY)
    private CloudEventType cloudEventType;

    @Enumerated(EnumType.STRING)
    @Column(name = CLOUD_EVENT_ACTION_PROPERTY)
    private EdgeEventActionType cloudEventAction;

    @Convert(converter = JsonConverter.class)
    @Column(name = CLOUD_EVENT_ENTITY_BODY_PROPERTY)
    private JsonNode entityBody;

    @Column(name = TS_COLUMN)
    private long ts;

    protected AbstractCloudEventEntity() {
        super();
    }

    protected AbstractCloudEventEntity(CloudEvent cloudEvent) {
        super();
        if (cloudEvent.getId() != null) {
            this.setUuid(cloudEvent.getId().getId());
            this.ts = getTs(cloudEvent.getId().getId());
        } else {
            this.ts = System.currentTimeMillis();
        }
        this.setCreatedTime(cloudEvent.getCreatedTime());
        if (cloudEvent.getTenantId() != null) {
            this.tenantId = cloudEvent.getTenantId().getId();
        }
        if (cloudEvent.getEntityId() != null) {
            this.entityId = cloudEvent.getEntityId();
        }
        this.cloudEventType = cloudEvent.getType();
        this.cloudEventAction = cloudEvent.getAction();
        this.entityBody = cloudEvent.getEntityBody();
    }

    @Override
    public CloudEvent toData() {
        CloudEvent cloudEvent = new CloudEvent(new CloudEventId(this.getUuid()));
        cloudEvent.setCreatedTime(createdTime);
        cloudEvent.setTenantId(TenantId.fromUUID(tenantId));
        if (entityId != null) {
            cloudEvent.setEntityId(entityId);
        }
        cloudEvent.setType(cloudEventType);
        cloudEvent.setAction(cloudEventAction);
        cloudEvent.setEntityBody(entityBody);
        cloudEvent.setSeqId(seqId);
        return cloudEvent;
    }

    private static long getTs(UUID uuid) {
        return (uuid.timestamp() - EPOCH_DIFF) / 10000;
    }

}
