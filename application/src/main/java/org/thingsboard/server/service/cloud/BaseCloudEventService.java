/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cloud.CloudEventService;

@Slf4j
public abstract class BaseCloudEventService {

    @Autowired
    protected CloudEventService cloudEventService;

    protected ListenableFuture<Void> saveCloudEvent(TenantId tenantId,
                                                    CloudEventType cloudEventType,
                                                    EdgeEventActionType cloudEventAction,
                                                    EntityId entityId,
                                                    JsonNode entityBody) {
        log.debug("Pushing event to cloud queue. tenantId [{}], cloudEventType [{}], cloudEventAction[{}], entityId [{}], entityBody [{}]",
                tenantId, cloudEventType, cloudEventAction, entityId, entityBody);

        CloudEvent cloudEvent = new CloudEvent();
        cloudEvent.setTenantId(tenantId);
        cloudEvent.setType(cloudEventType);
        cloudEvent.setAction(cloudEventAction);
        if (entityId != null) {
            cloudEvent.setEntityId(entityId.getId());
        }
        cloudEvent.setEntityBody(entityBody);
        return cloudEventService.saveAsync(cloudEvent);
    }
}
