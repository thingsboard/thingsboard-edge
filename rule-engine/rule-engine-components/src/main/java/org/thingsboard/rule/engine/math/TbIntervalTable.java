package org.thingsboard.rule.engine.math;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashvayka on 07.06.18.
 */
class TbIntervalTable {

    private final TbContext ctx;
    private final JsonParser gson;
    private final long intervalDuration;
    private Map<EntityId, Map<Long, TbIntervalState>> states = new HashMap<>();

    TbIntervalTable(TbContext ctx, TbSimpleAggMsgNodeConfiguration config, JsonParser gson) {
        this.ctx = ctx;
        this.gson = gson;
        this.intervalDuration = TimeUnit.valueOf(config.getIntervalTimeUnit()).toMillis(config.getIntervalValue());
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

    private ListenableFuture<TbIntervalState> fetchIntervalState(EntityId entityId, long intervalStartTs) {
        ListenableFuture<TsKvEntry> f = ctx.getTimeseriesService().findOne(entityId, intervalStartTs, "RuleNodeState_" + ctx.getSelfId());
        return Futures.transform(f, input -> {
            String value = input.getStrValue().orElse(null);
            if (StringUtils.isEmpty(value)) {
                return new TbIntervalState(0, 0, null, 0);
            } else {
                JsonObject stateJson = gson.parse(value).getAsJsonObject();
                TbIntervalState.TbIntervalStateBuilder builder = TbIntervalState.builder();
                if (stateJson.has("min")) {
                    builder.min(stateJson.get("min").getAsDouble());
                }
                if (stateJson.has("max")) {
                    builder.max(stateJson.get("max").getAsDouble());
                }
                if (stateJson.has("count")) {
                    builder.count(stateJson.get("count").getAsLong());
                }
                if (stateJson.has("sum")) {
                    builder.sum(stateJson.get("sum").getAsBigDecimal());
                }
                return builder.build();
            }
        });
    }

    private long calculateIntervalStart(long ts) {
        return (ts / intervalDuration) * intervalDuration;
    }
}
