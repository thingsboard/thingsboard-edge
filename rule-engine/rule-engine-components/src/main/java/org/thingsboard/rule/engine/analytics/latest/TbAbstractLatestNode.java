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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceQueue;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.*;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractLatestNode<C extends TbAbstractLatestNodeConfiguration> implements TbNode {

    private final Gson gson = new Gson();

    protected C config;
    private long delay;
    private long lastScheduledTs;
    private UUID nextTickId;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadMapperNodeConfig(configuration);
        this.delay = config.getPeriodTimeUnit().toMillis(config.getPeriodValue());
        scheduleTickMsg(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.getType().equals(tickMessageType()) && msg.getId().equals(nextTickId)) {
            withCallback(aggregate(ctx),
                    m -> scheduleTickMsg(ctx),
                    t -> {
                        ctx.tellFailure(msg, t);
                        scheduleTickMsg(ctx);
                    });
        }
    }

    private void scheduleTickMsg(TbContext ctx) {
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs = lastScheduledTs + delay;
        long curDelay = Math.max(0L, (lastScheduledTs - curTs));
        TbMsg tickMsg = ctx.newMsg(ServiceQueue.MAIN, tickMessageType(), ctx.getSelfId(), new TbMsgMetaData(), "");
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, curDelay);
    }

    private ListenableFuture<List<TbMsg>> aggregate(TbContext ctx) {
        ListenableFuture<List<EntityId>> parentEntityIdsFuture = this.config.getParentEntitiesQuery().getParentEntitiesAsync(ctx);
        return Futures.transformAsync(parentEntityIdsFuture, parentEntityIds -> {
            List<ListenableFuture<TbMsg>> msgFutures = new ArrayList<>();
            String dataTs = Long.toString(System.currentTimeMillis());
            parentEntityIds.forEach(parentEntityId -> {
                Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> aggregateFuturesMap = doParentAggregations(ctx, parentEntityId);
                aggregateFuturesMap.forEach((originatorId, aggregateFutures) -> aggregateFutures.forEach(aggregateFuture -> {
                    ListenableFuture<Optional<JsonObject>>
                            aggregateFutureWithFallback = Futures.catching(aggregateFuture, Throwable.class, e -> {
                        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                                originatorId, new TbMsgMetaData(), TbMsgDataType.JSON, "");
                        ctx.enqueueForTellFailure(msg, e.getMessage());
                        return Optional.empty();
                    }, MoreExecutors.directExecutor());
                    ListenableFuture<TbMsg> msgFuture = Futures.transform(aggregateFutureWithFallback, element -> {
                        if (element.isPresent()) {
                            TbMsgMetaData metaData = new TbMsgMetaData();
                            metaData.putValue("ts", dataTs);
                            JsonObject messageData = element.get();
                            TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                                    originatorId, metaData, gson.toJson(messageData));
                            ctx.enqueueForTellNext(msg, SUCCESS);
                            return msg;
                        } else {
                            return null;
                        }
                    }, MoreExecutors.directExecutor());
                    msgFutures.add(msgFuture);
                }));
            });
            return Futures.allAsList(msgFutures);
        }, ctx.getDbCallbackExecutor());
    }

    protected abstract C loadMapperNodeConfig(TbNodeConfiguration configuration) throws TbNodeException;

    protected abstract String tickMessageType();

    protected abstract Map<EntityId, List<ListenableFuture<Optional<JsonObject>>>> doParentAggregations(TbContext ctx, EntityId parentEntityId);

}
