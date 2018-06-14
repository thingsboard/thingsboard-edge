package org.thingsboard.rule.engine.math;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.math.state.TbAvgIntervalState;
import org.thingsboard.rule.engine.math.state.TbIntervalState;
import org.thingsboard.rule.engine.math.state.TbMaxIntervalState;
import org.thingsboard.rule.engine.math.state.TbMinIntervalState;
import org.thingsboard.rule.engine.math.state.TbSumIntervalState;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.math.BigDecimal;
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
    private final MathFunction function;
    private Map<EntityId, Map<Long, TbIntervalState>> states = new HashMap<>();

    TbIntervalTable(TbContext ctx, TbSimpleAggMsgNodeConfiguration config, JsonParser gson) {
        this.ctx = ctx;
        this.gsonParser = gson;
        this.intervalDuration = TimeUnit.valueOf(config.getAggIntervalTimeUnit()).toMillis(config.getAggIntervalValue());
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
        KvEntry kvEntry = new StringDataEntry("RuleNodeState_" + ctx.getSelfId(), state.toJson(gson));
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
                    default:
                        throw new IllegalArgumentException("Unsupported math function: " + function.name() + "!");
                }
            } else {
                JsonObject stateJson = gsonParser.parse(value).getAsJsonObject();
                switch (function) {
                    case MIN:
                        return new TbMinIntervalState(stateJson.get("min").getAsDouble());
                    case MAX:
                        return new TbMaxIntervalState(stateJson.get("max").getAsDouble());
                    case SUM:
                        return new TbSumIntervalState(stateJson.get("sum").getAsString());
                    case AVG:
                        return new TbAvgIntervalState(stateJson.get("sum").getAsString(), stateJson.get("count").getAsLong());
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
