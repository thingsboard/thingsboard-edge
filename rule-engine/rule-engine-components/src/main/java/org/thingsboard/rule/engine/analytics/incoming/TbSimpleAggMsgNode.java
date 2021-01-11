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
package org.thingsboard.rule.engine.analytics.incoming;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesQuery;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.analytics.incoming.state.StatePersistPolicy;
import org.thingsboard.rule.engine.analytics.incoming.state.TbIntervalState;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceQueue;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "aggregate stream",
        configClazz = TbSimpleAggMsgNodeConfiguration.class,
        nodeDescription = "Aggregates incoming data stream grouped by originator Entity Id",
        nodeDetails = "Calculates MIN/MAX/SUM/AVG/COUNT/UNIQUE based on the incoming data stream. " +
                "Groups incoming data stream based on originator id of the message (i.e. particular device, asset, customer) and <b>\"aggregation interval value\"</b> into Intervals.<br/><br/>" +
                "Intervals are periodically persisted based on <b>\"interval persistence policy\"</b> and <b>\"interval check value\"</b>.<br/><br/>" +
                "Intervals are cached in memory based on <b>\"Interval TTL value\"</b>.<br/><br/>" +
                "State of the Intervals are persisted as timeseries entities based on <b>\"state persistence policy\"</b> and <b>\"state persistence value\"</b>.<br/><br/>" +
                "In case there is no data for certain entity, it might be useful to generate default values for those entities. " +
                "To lookup those entities one may select <b>\"Create intervals automatically\"</b> checkbox and configure <b>\"Interval entities\"</b>.<br/><br/>" +
                "Generates 'POST_TELEMETRY_REQUEST' messages with the results of the aggregation for particular interval.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbAnalyticsNodeAggregateIncomingConfig",
        icon = "functions"
)
public class TbSimpleAggMsgNode implements TbNode {

    private static final String TB_REPORT_TICK_MSG = "TbIntervalTickMsg";
    private static final String TB_PERSIST_TICK_MSG = "TbPersistTickMsg";
    private static final String TB_ENTITIES_TICK_MSG = "TbEntitiesTickMsg";
    // millis at 00:00:00.000 15 Oct 1582.
    private static final long START_EPOCH = -12219292800000L;

    private final JsonParser gsonParser = new JsonParser();
    private final Gson gson = new Gson();

    private StatePersistPolicy statePersistPolicy;
    private IntervalPersistPolicy intervalPersistPolicy;
    private TbSimpleAggMsgNodeConfiguration config;
    private TbIntervalTable intervals;
    private UUID nextReportTickId;
    private UUID nextPersistTickId;
    private UUID nextEntitiesTickId;
    private long intervalReportCheckPeriod;
    private long statePersistCheckPeriod;
    private long entitiesCheckPeriod;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSimpleAggMsgNodeConfiguration.class);
        this.statePersistPolicy = StatePersistPolicy.valueOf(config.getStatePersistencePolicy());
        this.intervalPersistPolicy = IntervalPersistPolicy.valueOf(config.getIntervalPersistencePolicy());
        this.intervals = new TbIntervalTable(ctx, config, gsonParser);
        this.intervalReportCheckPeriod = Math.max(TimeUnit.valueOf(config.getIntervalCheckTimeUnit()).toMillis(config.getIntervalCheckValue()), TimeUnit.MINUTES.toMillis(1));
        this.statePersistCheckPeriod = Math.max(TimeUnit.valueOf(config.getStatePersistenceTimeUnit()).toMillis(config.getStatePersistenceValue()), TimeUnit.MINUTES.toMillis(1));
        scheduleReportTickMsg(ctx);
        if (StatePersistPolicy.PERIODICALLY.name().equalsIgnoreCase(config.getStatePersistencePolicy())) {
            scheduleStatePersistTickMsg(ctx);
        }
        if (config.isAutoCreateIntervals()) {
            this.entitiesCheckPeriod = Math.max(config.getPeriodTimeUnit().toMillis(config.getPeriodValue()), TimeUnit.MINUTES.toMillis(1));
            try {
                initEntities(ctx, null);
            } catch (Exception e) {
                throw new TbNodeException(e);
            }
            scheduleEntitiesTickMsg(ctx);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        switch (msg.getType()) {
            case TB_REPORT_TICK_MSG:
                onIntervalTickMsg(ctx, msg);
                break;
            case TB_PERSIST_TICK_MSG:
                onPersistTickMsg(ctx, msg);
                break;
            case TB_ENTITIES_TICK_MSG:
                try {
                    onEntitiesTickMsg(ctx, msg);
                } catch (Exception e) {
                    throw new TbNodeException(e);
                }
                break;
            default:
                onDataMsg(ctx, msg);
                break;
        }
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        log.trace("Cluster change msg received: {}", msg);
        intervals.cleanupEntities(ctx);
    }

    private void onDataMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        EntityId entityId = msg.getOriginator();
        long ts = extractTs(msg);
        JsonElement value = extractValue(msg);
        TbIntervalState state = intervals.getByEntityIdAndTs(entityId, ts);
        state.update(value);

        log.trace("Data Msg received: {}", msg);
        if (state.hasChangesToPersist() && statePersistPolicy == StatePersistPolicy.ON_EACH_CHANGE) {
            log.trace("Persisting state: {}", state);
            DonAsynchron.withCallback(intervals.saveIntervalState(entityId, ts, state),
                    v -> {
                        ctx.getPeContext().ack(msg);
                        state.clearChangesToPersist();
                        log.trace("Cleared state after persising: {}", state);
                    },
                    t -> ctx.tellFailure(msg, t),
                    ctx.getDbCallbackExecutor());
        } else {
            ctx.getPeContext().ack(msg);
        }
    }

    private void onIntervalTickMsg(TbContext ctx, TbMsg msg) {
        if (!msg.getId().equals(nextReportTickId)) {
            return;
        }
        scheduleReportTickMsg(ctx);
        log.trace("Reporting intervals!");
        intervals.getStatesToReport(intervalPersistPolicy).forEach((entityId, entityStates) -> entityStates.forEach((ts, interval) -> {
            log.trace("Reporting interval: [{}][{}]", ts, interval);
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("ts", Long.toString(ts));
            ctx.enqueueForTellNext(TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), entityId, metaData,
                    interval.toValueJson(gson, config.getOutputValueKey())), TbRelationTypes.SUCCESS);
        }));

        intervals.cleanupStatesUsingTTL();
    }

    private void onPersistTickMsg(TbContext ctx, TbMsg msg) {
        if (!msg.getId().equals(nextPersistTickId)) {
            return;
        }
        scheduleStatePersistTickMsg(ctx);
        log.trace("[{}] Persisting states!", ctx.getSelfId());
        intervals.getStatesToPersist().forEach((entityId, entityStates) -> entityStates.forEach((ts, state) -> {
            log.trace("[{}] Persisting state: [{}][{}]", ctx.getSelfId(), ts, state);
            intervals.saveIntervalState(entityId, ts, state);
        }));

        intervals.cleanupStatesUsingTTL();
    }

    private void onEntitiesTickMsg(TbContext ctx, TbMsg msg) throws Exception {
        if (!msg.getId().equals(nextEntitiesTickId)) {
            return;
        }
        scheduleEntitiesTickMsg(ctx);
        initEntities(ctx, msg);
    }

    private void initEntities(TbContext ctx, TbMsg msg) throws Exception {
        log.trace("[{}] Lookup entities!", ctx.getSelfId());
        ParentEntitiesQuery query = config.getParentEntitiesQuery();
        if (query.useParentEntitiesOnlyForSimpleAggregation()) {
            addIntervals(ctx, msg, query.getParentEntitiesAsync(ctx));
        } else {
            DonAsynchron.withCallback(query.getParentEntitiesAsync(ctx), parents -> {
                for (EntityId parentId : parents) {
                    addIntervals(ctx, msg, query.getChildEntitiesAsync(ctx, parentId));
                }
            }, getErrorsConsumer(ctx, msg), ctx.getDbCallbackExecutor());
        }
    }

    private void addIntervals(TbContext ctx, TbMsg msg, ListenableFuture<List<EntityId>> entities) {
        DonAsynchron.withCallback(entities,
                tmp -> intervals.addEntities(ctx, msg, tmp), getErrorsConsumer(ctx, msg), ctx.getDbCallbackExecutor());
    }

    private Consumer<Throwable> getErrorsConsumer(TbContext ctx, TbMsg msg) {
        return t -> {
            if (msg != null) {
                ctx.tellFailure(msg, t);
            }
        };
    }

    private void scheduleReportTickMsg(TbContext ctx) {
        TbMsg tickMsg = ctx.newMsg(ServiceQueue.MAIN, TB_REPORT_TICK_MSG, ctx.getSelfId(), new TbMsgMetaData(), "");
        nextReportTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, intervalReportCheckPeriod);
    }

    private void scheduleStatePersistTickMsg(TbContext ctx) {
        TbMsg tickMsg = ctx.newMsg(ServiceQueue.MAIN, TB_PERSIST_TICK_MSG, ctx.getSelfId(), new TbMsgMetaData(), "");
        nextPersistTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, statePersistCheckPeriod);
    }

    private void scheduleEntitiesTickMsg(TbContext ctx) {
        TbMsg tickMsg = ctx.newMsg(ServiceQueue.MAIN, TB_ENTITIES_TICK_MSG, ctx.getSelfId(), new TbMsgMetaData(), "");
        nextEntitiesTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, entitiesCheckPeriod);
    }

    private long extractTs(TbMsg msg) {
        String ts = msg.getMetaData().getValue("ts");
        if (!StringUtils.isEmpty(ts)) {
            return Long.parseLong(ts);
        } else {
            return msg.getTs();
        }
    }

    private JsonElement extractValue(TbMsg msg) {
        JsonElement jsonElement = gsonParser.parse(msg.getData());
        if (!jsonElement.isJsonObject()) {
            throw new IllegalArgumentException("Incoming message is not a json object!");
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!jsonObject.has(config.getInputValueKey())) {
            throw new IllegalArgumentException("Incoming message does not contain " + config.getInputValueKey() + "!");
        }
        return jsonObject.get(config.getInputValueKey());
    }

    @Override
    public void destroy() {

    }
}
