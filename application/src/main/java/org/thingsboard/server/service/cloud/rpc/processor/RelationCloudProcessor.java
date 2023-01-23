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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.relation.BaseRelationProcessor;

@Component
@Slf4j
public class RelationCloudProcessor extends BaseRelationProcessor {

    public UplinkMsg convertRelationRequestEventToUplink(CloudEvent cloudEvent) {
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getType(), cloudEvent.getEntityId());
        RelationRequestMsg relationRequestMsg = RelationRequestMsg.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name())
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addRelationRequestMsg(relationRequestMsg);
        return builder.build();
    }

    public UplinkMsg convertRelationEventToUplink(CloudEvent cloudEvent) {
        UplinkMsg msg = null;
        UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
        EntityRelation entityRelation = JacksonUtil.OBJECT_MAPPER.convertValue(cloudEvent.getEntityBody(), EntityRelation.class);
        if (entityRelation != null) {
            RelationUpdateMsg relationUpdateMsg = relationMsgConstructor.constructRelationUpdatedMsg(msgType, entityRelation);
            msg = UplinkMsg.newBuilder()
                    .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                    .addRelationUpdateMsg(relationUpdateMsg).build();
        }
        return msg;
    }

}
