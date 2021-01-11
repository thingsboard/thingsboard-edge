/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;

public abstract class TbAbstractDuplicateMsgToOriginatorsNode extends TbAbstractDuplicateMsgNode {

    @Override
    protected ListenableFuture<List<TbMsg>> duplicate(TbContext ctx, TbMsg msg) {
        ListenableFuture<List<EntityId>> newOriginators = getNewOriginators(ctx, msg.getOriginator());
        return Futures.transform(newOriginators, entityIds -> {
            if (entityIds == null || entityIds.isEmpty()) {
                return null;
            }
            List<TbMsg> messages = new ArrayList<>();
            if (entityIds.size() == 1) {
                messages.add(ctx.transformMsg(msg, msg.getType(), entityIds.get(0), msg.getMetaData(), msg.getData()));
            } else {
                for (EntityId entityId : entityIds) {
                    messages.add(ctx.newMsg(msg.getQueueName(), msg.getType(), entityId, msg.getMetaData(), msg.getData()));
                }
            }
            return messages;
        }, ctx.getDbCallbackExecutor());
    }

    protected abstract ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, EntityId original);

}
