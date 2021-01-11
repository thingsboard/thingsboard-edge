/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.rule.engine.analytics.latest;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Collections;
import java.util.List;

@Data
public class ParentEntitiesSingleEntity implements ParentEntitiesQuery {

    private EntityId entityId;
    private RelationsQuery childRelationsQuery;

    @Override
    public ListenableFuture<List<EntityId>> getParentEntitiesAsync(TbContext ctx) {
        if (ctx.getPeContext().isLocalEntity(entityId)) {
            return Futures.immediateFuture(Collections.singletonList(entityId));
        } else {
            return Futures.immediateFuture(Collections.emptyList());
        }
    }

    @Override
    public ListenableFuture<List<EntityId>> getChildEntitiesAsync(TbContext ctx, EntityId parentEntityId) {
        return EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctx, parentEntityId, childRelationsQuery);
    }

    @Override
    public boolean useParentEntitiesOnlyForSimpleAggregation() {
        return true;
    }
}
