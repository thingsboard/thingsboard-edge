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
package org.thingsboard.server.service.edge.rpc.processor.relation;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

@Component
@TbCoreComponent
public class RelationEdgeProcessorV1 extends RelationEdgeProcessor {

    @Override
    protected EntityRelation constructEntityRelationFromUpdateMsg(RelationUpdateMsg relationUpdateMsg) {
        EntityRelation entityRelation = new EntityRelation();

        UUID fromUUID = new UUID(relationUpdateMsg.getFromIdMSB(), relationUpdateMsg.getFromIdLSB());
        EntityId fromId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getFromEntityType()), fromUUID);
        entityRelation.setFrom(fromId);

        UUID toUUID = new UUID(relationUpdateMsg.getToIdMSB(), relationUpdateMsg.getToIdLSB());
        EntityId toId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getToEntityType()), toUUID);
        entityRelation.setTo(toId);

        entityRelation.setType(relationUpdateMsg.getType());
        entityRelation.setTypeGroup(relationUpdateMsg.hasTypeGroup()
                ? RelationTypeGroup.valueOf(relationUpdateMsg.getTypeGroup()) : RelationTypeGroup.COMMON);
        entityRelation.setAdditionalInfo(JacksonUtil.toJsonNode(relationUpdateMsg.getAdditionalInfo()));
        return entityRelation;
    }

}
