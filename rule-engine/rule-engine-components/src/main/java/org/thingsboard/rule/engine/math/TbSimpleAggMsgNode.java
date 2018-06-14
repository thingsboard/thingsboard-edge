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

    private static final String TB_AGG_TICK_MSG = "TbAggTickMsg";

    private final JsonParser gsonParser = new JsonParser();
    private final Gson gson = new Gson();

    private StatePersistPolicy statePersistPolicy;
    private TbSimpleAggMsgNodeConfiguration config;
    private TbIntervalTable intervals;
    private UUID nextTickId;
    private long intervalCheckPeriod;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSimpleAggMsgNodeConfiguration.class);
        this.statePersistPolicy = StatePersistPolicy.valueOf(config.getStatePersistencePolicy());
        this.intervals = new TbIntervalTable(ctx, config, gsonParser);
        this.intervalCheckPeriod = TimeUnit.valueOf(config.getIntervalCheckTimeUnit()).toMillis(config.getIntervalCheckValue());
        scheduleTickMsg(ctx);
        //TODO: fetch all states that were not persisted before restart?
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (msg.getType().equals(TB_AGG_TICK_MSG)) {
            onTickMsg(ctx, msg);
        } else {
            onDataMsg(ctx, msg);
        }
    }

    private void onTickMsg(TbContext ctx, TbMsg msg) {
        if (!msg.getId().equals(nextTickId)) {
            return;
        }

        scheduleTickMsg(ctx);
        intervals.getUpdatedStates().forEach((entityId, entityStates) -> entityStates.forEach((ts, interval) -> {
            JsonObject json = new JsonObject();
            json.addProperty(config.getOutputValueKey(), interval.getValue());
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("ts", Long.toString(ts));
            ctx.tellNext(new TbMsg(UUIDs.timeBased(), SessionMsgType.POST_TELEMETRY_REQUEST.name(), entityId, metaData, TbMsgDataType.JSON, gson.toJson(json),
                    null, null, 0L), TbRelationTypes.SUCCESS);
        }));
    }

    private void onDataMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        EntityId entityId = msg.getOriginator();
        long ts = extractTs(msg);
        double value = extractValue(msg);
        //TODO: forward message to different server if needed.

        TbIntervalState state = intervals.getByEntityIdAndTs(entityId, ts);
        state.update(value);

        if (state.hasChanges() && statePersistPolicy == StatePersistPolicy.ON_EACH_CHANGE) {
            DonAsynchron.withCallback(intervals.saveIntervalState(entityId, ts, state),
                    v -> ctx.getPeContext().ack(msg),
                    t -> ctx.tellFailure(msg, t),
                    ctx.getDbCallbackExecutor());
        } else {
            ctx.getPeContext().ack(msg);
        }
    }

    private void scheduleTickMsg(TbContext ctx) {
        TbMsg tickMsg = ctx.newMsg(TB_AGG_TICK_MSG, ctx.getSelfId(), new TbMsgMetaData(), "");
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, intervalCheckPeriod);
    }


    private long extractTs(TbMsg msg) {
        String ts = msg.getMetaData().getValue("ts");
        if (!StringUtils.isEmpty(ts)) {
            return Long.parseLong(ts);
        } else {
            return System.currentTimeMillis();
        }
    }

    private double extractValue(TbMsg msg) {
        JsonElement jsonElement = gsonParser.parse(msg.getData());
        if (!jsonElement.isJsonObject()) {
            throw new IllegalArgumentException("Incoming message is not a json object!");
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!jsonObject.has(config.getInputValueKey())) {
            throw new IllegalArgumentException("Incoming message does not contain " + config.getInputValueKey() + "!");
        }
        return jsonObject.get(config.getInputValueKey()).getAsDouble();
    }

    @Override
    public void destroy() {

    }
}
