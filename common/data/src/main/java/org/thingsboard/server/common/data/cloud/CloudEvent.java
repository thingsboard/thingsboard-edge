/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    public CloudEvent() {
        super();
    }

    public CloudEvent(CloudEventId id) {
        super(id);
    }

    public CloudEvent(TenantId tenantId, EdgeEventActionType action, UUID entityId, CloudEventType type, JsonNode entityBody) {
        this.tenantId = tenantId;
        this.action = action;
        this.entityId = entityId;
        this.type = type;
        this.entityBody = entityBody;
    }
}
