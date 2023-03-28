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
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CloudEventId;
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

import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_ACTION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_ENTITY_BODY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_ENTITY_GROUP_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CLOUD_EVENT_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EPOCH_DIFF;
import static org.thingsboard.server.dao.model.ModelConstants.TS_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = CLOUD_EVENT_COLUMN_FAMILY_NAME)
@NoArgsConstructor
public class CloudEventEntity extends BaseSqlEntity<CloudEvent> implements BaseEntity<CloudEvent> {

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

    @Type(type = "json")
    @Column(name = CLOUD_EVENT_ENTITY_BODY_PROPERTY)
    private JsonNode entityBody;

    @Column(name = CLOUD_EVENT_ENTITY_GROUP_ID_PROPERTY)
    private UUID entityGroupId;

    @Column(name = TS_COLUMN)
    private long ts;

    public CloudEventEntity(CloudEvent cloudEvent) {
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
        if (cloudEvent.getEntityGroupId() != null) {
            this.entityGroupId = cloudEvent.getEntityGroupId();
        }
        this.cloudEventType = cloudEvent.getType();
        this.cloudEventAction = cloudEvent.getAction();
        this.entityBody = cloudEvent.getEntityBody();
    }

    @Override
    public CloudEvent toData() {
        CloudEvent cloudEvent = new CloudEvent(new CloudEventId(this.getUuid()));
        cloudEvent.setCreatedTime(createdTime);
        cloudEvent.setTenantId(new TenantId(tenantId));
        if (entityId != null) {
            cloudEvent.setEntityId(entityId);
        }
        if (entityGroupId != null) {
            cloudEvent.setEntityGroupId(entityGroupId);
        }
        cloudEvent.setType(cloudEventType);
        cloudEvent.setAction(cloudEventAction);
        cloudEvent.setEntityBody(entityBody);
        return cloudEvent;
    }

    private static long getTs(UUID uuid) {
        return (uuid.timestamp() - EPOCH_DIFF) / 10000;
    }
}
