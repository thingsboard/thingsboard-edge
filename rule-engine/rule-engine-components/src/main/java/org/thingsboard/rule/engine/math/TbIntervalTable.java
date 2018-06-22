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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.math.state.TbAvgIntervalState;
import org.thingsboard.rule.engine.math.state.TbCountIntervalState;
import org.thingsboard.rule.engine.math.state.TbCountUniqueIntervalState;
import org.thingsboard.rule.engine.math.state.TbIntervalState;
import org.thingsboard.rule.engine.math.state.TbMaxIntervalState;
import org.thingsboard.rule.engine.math.state.TbMinIntervalState;
import org.thingsboard.rule.engine.math.state.TbSumIntervalState;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 07.06.18.
 */
class TbIntervalTable {

    private final TbContext ctx;
    private final JsonParser gsonParser;
    private final Gson gson = new Gson();
    private final long intervalDuration;
    private final long intervalTtl;
    private final MathFunction function;
    private Map<EntityId, Map<Long, TbIntervalState>> states = new HashMap<>();

    TbIntervalTable(TbContext ctx, TbSimpleAggMsgNodeConfiguration config, JsonParser gson) {
        this.ctx = ctx;
        this.gsonParser = gson;
        this.intervalDuration = TimeUnit.valueOf(config.getAggIntervalTimeUnit()).toMillis(config.getAggIntervalValue());
        this.intervalTtl = TimeUnit.valueOf(config.getIntervalTtlTimeUnit()).toMillis(config.getIntervalTtlValue());
        this.function = MathFunction.valueOf(config.getMathFunction());
    }

    //TODO: make this async
    TbIntervalState getByEntityIdAndTs(EntityId entityId, long ts) throws ExecutionException, InterruptedException {
        long intervalStartTs = calculateIntervalStart(ts);

        Map<Long, TbIntervalState> tsStates = states.computeIfAbsent(entityId, k -> new HashMap<>());
        TbIntervalState state = tsStates.get(intervalStartTs);
        if (state == null) {
            state = fetchIntervalState(entityId, intervalStartTs).get();
            tsStates.put(intervalStartTs, state);
        }
        return state;
    }

    ListenableFuture<List<Void>> saveIntervalState(EntityId entityId, long intervalStartTs, TbIntervalState state) {
        KvEntry kvEntry = new StringDataEntry("RuleNodeState_" + ctx.getSelfId(), state.toStateJson(gson));
        TsKvEntry tsKvEntry = new BasicTsKvEntry(intervalStartTs, kvEntry);
        return ctx.getTimeseriesService().save(entityId, tsKvEntry);
    }

    Map<EntityId, Map<Long, TbIntervalState>> getUpdatedStates() {
        Map<EntityId, Map<Long, TbIntervalState>> updatedStates = new HashMap<>();

        states.forEach((entityId, intervals) -> {
            Map<Long, TbIntervalState> updatedIntervals = intervals.entrySet().stream().filter(e -> e.getValue().hasChanges()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (!updatedIntervals.isEmpty()) {
                updatedIntervals.values().forEach(TbIntervalState::clearChanges);
                updatedStates.put(entityId, updatedIntervals);
            }
        });

        return updatedStates;
    }

    void cleanupStatesUsingTTL() {
        long expTime = System.currentTimeMillis() - intervalTtl;
        states.forEach((entityId, intervals) -> {
            List<Long> keysToRemove = intervals.keySet().stream().filter(ts -> ts < expTime).collect(Collectors.toList());
            keysToRemove.forEach(intervals::remove);
        });
    }

    private ListenableFuture<TbIntervalState> fetchIntervalState(EntityId entityId, long intervalStartTs) {
        ListenableFuture<TsKvEntry> f = ctx.getTimeseriesService().findOne(entityId, intervalStartTs, "RuleNodeState_" + ctx.getSelfId());
        return Futures.transform(f, input -> {
            String value = input.getStrValue().orElse(null);
            if (StringUtils.isEmpty(value)) {
                switch (function) {
                    case MIN:
                        return new TbMinIntervalState();
                    case MAX:
                        return new TbMaxIntervalState();
                    case SUM:
                        return new TbSumIntervalState();
                    case AVG:
                        return new TbAvgIntervalState();
                    case COUNT:
                        return new TbCountIntervalState();
                    case COUNT_UNIQUE:
                        return new TbCountUniqueIntervalState();
                    default:
                        throw new IllegalArgumentException("Unsupported math function: " + function.name() + "!");
                }
            } else {
                JsonElement stateJson = gsonParser.parse(value);
                switch (function) {
                    case MIN:
                        return new TbMinIntervalState(stateJson);
                    case MAX:
                        return new TbMaxIntervalState(stateJson);
                    case SUM:
                        return new TbSumIntervalState(stateJson);
                    case AVG:
                        return new TbAvgIntervalState(stateJson);
                    case COUNT:
                        return new TbCountIntervalState(stateJson);
                    case COUNT_UNIQUE:
                        return new TbCountUniqueIntervalState(stateJson);
                    default:
                        throw new IllegalArgumentException("Unsupported math function: " + function.name() + "!");
                }
            }
        });
    }

    private long calculateIntervalStart(long ts) {
        return (ts / intervalDuration) * intervalDuration;
    }
}
