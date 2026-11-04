// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CloudEventId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CloudEvent extends BaseData<CloudEventId> {

    private long seqId;
    private TenantId tenantId;
    private EdgeEventActionType action;
    private UUID entityId;
    private CloudEventType type;
    private transient JsonNode entityBody;
    private UUID entityGroupId;

    public CloudEvent() {
        super();
    }

    public CloudEvent(CloudEventId id) {
        super(id);
    }

    public CloudEvent(TenantId tenantId, EdgeEventActionType action, UUID entityId, CloudEventType type, JsonNode entityBody, UUID entityGroupId) {
        this.tenantId = tenantId;
        this.action = action;
        this.entityId = entityId;
        this.type = type;
        this.entityBody = entityBody;
        this.entityGroupId = entityGroupId;
    }

}
