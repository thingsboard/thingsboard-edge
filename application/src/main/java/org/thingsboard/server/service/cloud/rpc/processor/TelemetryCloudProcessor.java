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
package org.thingsboard.server.service.cloud.rpc.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.telemetry.BaseTelemetryProcessor;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelemetryCloudProcessor extends BaseTelemetryProcessor {

    @Override
    protected String getMsgSourceKey() {
        return DataConstants.CLOUD_MSG_SOURCE;
    }

    public UplinkMsg convertTelemetryEventToUplink(CloudEvent cloudEvent) throws Exception {
        EntityType entityType = EntityType.valueOf(cloudEvent.getType().name());
        EntityDataProto entityDataProto = convertTelemetryEventToEntityDataProto(entityType, cloudEvent.getEntityId(),
                cloudEvent.getAction(), cloudEvent.getEntityBody());
        return UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addEntityData(entityDataProto)
                .build();
    }

    public UplinkMsg convertAttributesRequestEventToUplink(CloudEvent cloudEvent) {
        log.trace("Executing convertAttributesRequestEventToUplink, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getType(), cloudEvent.getEntityId());
        try {
            List<AttributesRequestMsg> allAttributesRequestMsg = new ArrayList<>();
            AttributesRequestMsg serverAttributesRequestMsg = AttributesRequestMsg.newBuilder()
                    .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                    .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                    .setEntityType(entityId.getEntityType().name())
                    .setScope(DataConstants.SERVER_SCOPE)
                    .build();
            allAttributesRequestMsg.add(serverAttributesRequestMsg);
            if (EntityType.DEVICE.equals(entityId.getEntityType())) {
                AttributesRequestMsg sharedAttributesRequestMsg = AttributesRequestMsg.newBuilder()
                        .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                        .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                        .setEntityType(entityId.getEntityType().name())
                        .setScope(DataConstants.SHARED_SCOPE)
                        .build();
                allAttributesRequestMsg.add(sharedAttributesRequestMsg);
            }
            UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                    .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                    .addAllAttributesRequestMsg(allAttributesRequestMsg);
            return builder.build();
        } catch (Exception e) {
            log.warn("Can't send attribute request msg, entityId [{}], body [{}]", cloudEvent.getEntityId(), cloudEvent.getEntityBody(), e);
            return null;
        }
    }

}
