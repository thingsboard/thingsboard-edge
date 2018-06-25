/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.rule.engine.math;

import com.datastax.driver.core.utils.UUIDs;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.util.DonAsynchron;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.math.state.StatePersistPolicy;
import org.thingsboard.rule.engine.math.state.TbIntervalState;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "aggregation",
        configClazz = TbSimpleAggMsgNodeConfiguration.class,
        nodeDescription = "Calculates MIN/MAX/AVG/SUM based on the incoming data",
        nodeDetails = "Calculates MIN/MAX/AVG/SUM based on the incoming data"
)
public class TbSimpleAggMsgNode implements TbNode {

    private static final String TB_INTERVAL_TICK_MSG = "TbIntervalTickMsg";
    private static final String TB_PERSIST_TICK_MSG = "TbPersistTickMsg";
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
    private long intervalReportCheckPeriod;
    private long statePersistCheckPeriod;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSimpleAggMsgNodeConfiguration.class);
        this.statePersistPolicy = StatePersistPolicy.valueOf(config.getStatePersistencePolicy());
        this.intervalPersistPolicy = IntervalPersistPolicy.valueOf(config.getIntervalPersistencePolicy());
        this.intervals = new TbIntervalTable(ctx, config, gsonParser);
        this.intervalReportCheckPeriod = TimeUnit.valueOf(config.getIntervalCheckTimeUnit()).toMillis(config.getIntervalCheckValue());
        this.statePersistCheckPeriod = TimeUnit.valueOf(config.getStatePersistenceTimeUnit()).toMillis(config.getStatePersistenceValue());
        scheduleReportTickMsg(ctx);
        if (StatePersistPolicy.PERIODICALLY.name().equalsIgnoreCase(config.getStatePersistencePolicy())) {
            scheduleStatePersistTickMsg(ctx);
        }
        //TODO: fetch all states that were not persisted before restart?
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        switch (msg.getType()) {
            case TB_INTERVAL_TICK_MSG:
                onIntervalTickMsg(ctx, msg);
                break;
            case TB_PERSIST_TICK_MSG:
                onPersistTickMsg(ctx, msg);
                break;
            default:
                onDataMsg(ctx, msg);
                break;
        }
    }

    private void onDataMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        EntityId entityId = msg.getOriginator();
        long ts = extractTs(msg);
        JsonElement value = extractValue(msg);
        //TODO: forward message to different server if needed.

        TbIntervalState state = intervals.getByEntityIdAndTs(entityId, ts);
        state.update(value);

        if (state.hasChangesToPersist() && statePersistPolicy == StatePersistPolicy.ON_EACH_CHANGE) {
            DonAsynchron.withCallback(intervals.saveIntervalState(entityId, ts, state),
                    v -> {
                        ctx.getPeContext().ack(msg);
                        state.clearChangesToPersist();
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
        intervals.getStatesToReport(intervalPersistPolicy).forEach((entityId, entityStates) -> entityStates.forEach((ts, interval) -> {
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("ts", Long.toString(ts));
            ctx.tellNext(new TbMsg(UUIDs.timeBased(), SessionMsgType.POST_TELEMETRY_REQUEST.name(), entityId, metaData, TbMsgDataType.JSON,
                    interval.toValueJson(gson, config.getOutputValueKey()),
                    null, null, 0L), TbRelationTypes.SUCCESS);
        }));

        intervals.cleanupStatesUsingTTL();
    }

    private void onPersistTickMsg(TbContext ctx, TbMsg msg) {
        if (!msg.getId().equals(nextPersistTickId)) {
            return;
        }
        scheduleStatePersistTickMsg(ctx);
        intervals.getStatesToPersist().forEach((entityId, entityStates) -> entityStates.forEach((ts, state) -> {
            intervals.saveIntervalState(entityId, ts, state);
        }));

        intervals.cleanupStatesUsingTTL();
    }

    private void scheduleReportTickMsg(TbContext ctx) {
        TbMsg tickMsg = ctx.newMsg(TB_INTERVAL_TICK_MSG, ctx.getSelfId(), new TbMsgMetaData(), "");
        nextReportTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, intervalReportCheckPeriod);
    }

    private void scheduleStatePersistTickMsg(TbContext ctx) {
        TbMsg tickMsg = ctx.newMsg(TB_PERSIST_TICK_MSG, ctx.getSelfId(), new TbMsgMetaData(), "");
        nextPersistTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, statePersistCheckPeriod);
    }

    private long extractTs(TbMsg msg) {
        String ts = msg.getMetaData().getValue("ts");
        if (!StringUtils.isEmpty(ts)) {
            return Long.parseLong(ts);
        } else {
            return (msg.getId().timestamp() / 10000) + START_EPOCH;
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
