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
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Component
@Slf4j
public class EntityCloudProcessor extends BaseEdgeProcessor {

    public UplinkMsg convertCredentialsRequestEventToUplink(CloudEvent cloudEvent) {
        log.trace("Executing convertCredentialsRequestEventToUplink, cloudEvent [{}]", cloudEvent);
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getType(), cloudEvent.getEntityId());
        UplinkMsg msg = null;
        switch (entityId.getEntityType()) {
            case USER:
                UserCredentialsRequestMsg userCredentialsRequestMsg = UserCredentialsRequestMsg.newBuilder()
                        .setUserIdMSB(entityId.getId().getMostSignificantBits())
                        .setUserIdLSB(entityId.getId().getLeastSignificantBits())
                        .build();
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addUserCredentialsRequestMsg(userCredentialsRequestMsg)
                        .build();
                break;
            case DEVICE:
                DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                        .setDeviceIdMSB(entityId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(entityId.getId().getLeastSignificantBits())
                        .build();
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceCredentialsRequestMsg(deviceCredentialsRequestMsg)
                        .build();
                break;
            default:
                log.info("Skipping event as entity type doesn't supported [{}]", cloudEvent);
        }
        return msg;
    }
}
