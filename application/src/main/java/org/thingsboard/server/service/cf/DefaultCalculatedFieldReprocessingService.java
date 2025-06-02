/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
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
package org.thingsboard.server.service.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest.Strategy;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.CfReprocessingTask;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createDefaultKvEntry;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createStateByType;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.transformSingleValueArgument;

@TbRuleEngineComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldReprocessingService implements CalculatedFieldReprocessingService {

    private static final Set<EntityType> supportedReprocessingEntities = EnumSet.of(
            EntityType.DEVICE, EntityType.ASSET
    );

    @Value("${actors.calculated_fields.calculation_timeout:5}")
    private long cfCalculationResultTimeout;

    @Value("${queue.calculated_fields.telemetry_fetch_pack_size:1000}")
    private int telemetryFetchPackSize;

    private final TimeseriesService timeseriesService;
    private final AttributesService attributesService;
    private final TbelInvokeService tbelInvokeService;
    private final ApiLimitService apiLimitService;
    private final TelemetrySubscriptionService telemetrySubscriptionService;

    private ListeningExecutorService calculatedFieldCallbackExecutor;

    @PostConstruct
    public void init() {
        calculatedFieldCallbackExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "calculated-field-reprocessing-callback"));
    }

    @PreDestroy
    public void stop() {
        if (calculatedFieldCallbackExecutor != null) {
            calculatedFieldCallbackExecutor.shutdownNow();
        }
    }

    @Override
    public void reprocess(CfReprocessingTask task) throws Exception {
        TenantId tenantId = task.getTenantId();
        EntityId entityId = task.getEntityId();
        log.debug("[{}] Received reprocessing request: {}", tenantId, task);

        if (!supportedReprocessingEntities.contains(entityId.getEntityType())) {
            throw new IllegalArgumentException("EntityType '" + entityId.getEntityType() + "' is not supported for reprocessing");
        }

        long startTs = task.getStartTs();
        long endTs = task.getEndTs();

        CalculatedFieldCtx ctx = getCFCtx(task.getCalculatedField());
        if (OutputType.ATTRIBUTES.equals(ctx.getOutput().getType())) {
            throw new IllegalArgumentException("'ATTRIBUTES' output type is not supported for reprocessing");
        }
        CalculatedFieldState state = initState(tenantId, entityId, ctx, startTs);

        performInitialProcessing(tenantId, entityId, state, ctx, startTs);

        Map<String, LinkedList<TsKvEntry>> telemetryBuffers = new HashMap<>();
        Map<String, Long> cursors = new HashMap<>();
        for (Entry<String, Argument> e : ctx.getArguments().entrySet()) {
            String argName = e.getKey();
            Argument arg = e.getValue();
            if (ArgumentType.ATTRIBUTE.equals(arg.getRefEntityKey().getType())) {
                continue;
            }
            LinkedList<TsKvEntry> batch = new LinkedList<>(fetchTelemetryBatch(tenantId, entityId, arg, startTs, endTs, telemetryFetchPackSize));
            if (!batch.isEmpty()) {
                telemetryBuffers.put(argName, batch);
                cursors.put(argName, batch.getLast().getTs());
            }
        }

        while (!Thread.interrupted()) {
            long minTs = telemetryBuffers.values().stream()
                    .filter(buffer -> !buffer.isEmpty())
                    .mapToLong(buffer -> buffer.get(0).getTs())
                    .min().orElse(Long.MAX_VALUE);

            if (minTs == Long.MAX_VALUE) {
                break;
            }

            Map<String, ArgumentEntry> updatedArgs = new HashMap<>();

            for (Map.Entry<String, LinkedList<TsKvEntry>> entry : telemetryBuffers.entrySet()) {
                String argName = entry.getKey();
                LinkedList<TsKvEntry> buffer = entry.getValue();

                if (!buffer.isEmpty() && buffer.getFirst().getTs() == minTs) {
                    TsKvEntry tsEntry = buffer.removeFirst();
                    updatedArgs.put(argName, ArgumentEntry.createSingleValueArgument(tsEntry));

                    if (buffer.isEmpty()) {
                        Argument arg = ctx.getArguments().get(argName);
                        Long cursorTs = cursors.getOrDefault(argName, startTs);
                        LinkedList<TsKvEntry> nextBatch = fetchTelemetryBatch(tenantId, entityId, arg, cursorTs, endTs, telemetryFetchPackSize).stream()
                                .filter(tsKvEntry -> tsKvEntry.getTs() > cursorTs)
                                .collect(Collectors.toCollection(LinkedList::new));
                        if (!nextBatch.isEmpty()) {
                            telemetryBuffers.put(argName, nextBatch);
                            cursors.put(argName, nextBatch.getLast().getTs());
                        }
                    }
                }
            }

            processArgumentValuesUpdate(tenantId, entityId, state, ctx, updatedArgs, minTs);
        }
    }

    private void performInitialProcessing(TenantId tenantId, EntityId entityId, CalculatedFieldState state, CalculatedFieldCtx ctx, long startTs) throws Exception {
        if (state.isSizeOk()) {
            processStateIfReady(tenantId, entityId, ctx, state, startTs);
        } else {
            throw new RuntimeException(ctx.getSizeExceedsLimitMessage());
        }
    }

    private void processStateIfReady(TenantId tenantId, EntityId entityId, CalculatedFieldCtx ctx, CalculatedFieldState state, long ts) throws Exception {
        CalculatedFieldEntityCtxId ctxId = new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId);
        boolean stateSizeChecked = false;
        if (ctx.isInitialized() && state.isReady()) {
            log.trace("[{}][{}] Performing calculation for CF {}", tenantId, entityId, ctx.getCfId());
            CalculatedFieldResult calculationResult = state.performCalculation(ctx).get(cfCalculationResultTimeout, TimeUnit.SECONDS);
            state.checkStateSize(ctxId, ctx.getMaxStateSize());
            stateSizeChecked = true;
            if (state.isSizeOk()) {
                if (!calculationResult.isEmpty()) {
                    saveResult(tenantId, entityId, checkAndSetTs(calculationResult, ts), ts);
                }
            }
        }
        if (!stateSizeChecked) {
            state.checkStateSize(ctxId, ctx.getMaxStateSize());
        }
        if (!state.isSizeOk()) {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build();
        }
    }

    private CalculatedFieldResult checkAndSetTs(CalculatedFieldResult result, long ts) {
        JsonNode resultJson = result.getResult();
        JsonNode newResultJson = resultJson.deepCopy();
        if (newResultJson.isObject()) {
            newResultJson = withTs(newResultJson, ts);
        }
        if (newResultJson.isArray()) {
            ArrayNode newArray = JacksonUtil.newArrayNode();
            for (JsonNode entry : newResultJson) {
                newArray.add(withTs(entry, ts));
            }
            newResultJson = newArray;
        }
        return new CalculatedFieldResult(result.getType(), result.getScope(), newResultJson);
    }

    private JsonNode withTs(JsonNode node, long ts) {
        if (node.isObject() && !node.has("ts")) {
            if (!node.has("values")) {
                ObjectNode wrapped = JacksonUtil.newObjectNode();
                wrapped.put("ts", ts);
                wrapped.set("values", node);
                return wrapped;
            } else {
                ((ObjectNode) node).put("ts", ts);
            }
        }
        return node;
    }

    private void processArgumentValuesUpdate(TenantId tenantId, EntityId entityId, CalculatedFieldState state, CalculatedFieldCtx ctx, Map<String, ArgumentEntry> newArgValues, long ts) throws Exception {
        if (newArgValues.isEmpty()) {
            log.info("[{}] No argument values to process for CF.", ctx.getCfId());
        }
        if (state == null) {
            state = createStateByType(ctx);
        }
        if (state.isSizeOk()) {
            if (state.updateState(ctx, newArgValues)) {
                processStateIfReady(tenantId, entityId, ctx, state, ts);
            }
        } else {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build();
        }
    }

    private CalculatedFieldState initState(TenantId tenantId, EntityId entityId, CalculatedFieldCtx ctx, long startTs) throws InterruptedException {
        ListenableFuture<CalculatedFieldState> stateFuture = fetchStateFromDb(ctx, entityId, startTs);
        CalculatedFieldState state;
        try {
            state = stateFuture.get(); // will be interrupted on task processing timeout
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException(cause.getMessage(), cause);
        }
        state.checkStateSize(new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId), ctx.getMaxStateSize());
        log.debug("[{}][{}] Initialized state for CF {}", tenantId, entityId, ctx.getCfId());
        return state;
    }

    private ListenableFuture<CalculatedFieldState> fetchStateFromDb(CalculatedFieldCtx ctx, EntityId entityId, long startTs) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = new HashMap<>();
        for (var entry : ctx.getArguments().entrySet()) {
            var argEntityId = entry.getValue().getRefEntityId() != null ? entry.getValue().getRefEntityId() : entityId;
            var argValueFuture = fetchArgumentValue(ctx.getTenantId(), argEntityId, entry.getValue(), startTs);
            argFutures.put(entry.getKey(), argValueFuture);
        }
        return Futures.whenAllComplete(argFutures.values()).call(() -> {
            var result = createStateByType(ctx);
            result.updateState(ctx, argFutures.entrySet().stream()
                    .collect(Collectors.toMap(
                            Entry::getKey, // Keep the key as is
                            entry -> {
                                try {
                                    // Resolve the future to get the value
                                    return entry.getValue().get();
                                } catch (ExecutionException e) {
                                    Throwable cause = e.getCause();
                                    throw new RuntimeException("Failed to fetch " + entry.getKey() + ": " + cause.getMessage(), cause);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException("Failed to fetch" + entry.getKey(), e);
                                }
                            }
                    )));
            return result;
        }, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> fetchArgumentValue(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        return switch (argument.getRefEntityKey().getType()) {
            case TS_ROLLING -> fetchTsRolling(tenantId, entityId, argument, startTs);
            case ATTRIBUTE -> fetchAttribute(tenantId, entityId, argument, startTs);
            case TS_LATEST -> fetchTsLatest(tenantId, entityId, argument, startTs);
        };
    }

    private ListenableFuture<ArgumentEntry> fetchAttribute(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        log.trace("[{}][{}] Fetching attribute for key {}", tenantId, entityId, argument.getRefEntityKey());
        var attributeOptFuture = attributesService.find(tenantId, entityId, argument.getRefEntityKey().getScope(), argument.getRefEntityKey().getKey());

        return Futures.transform(attributeOptFuture, attrOpt -> {
            log.debug("[{}][{}] Fetched attribute for key {}: {}", tenantId, entityId, argument.getRefEntityKey(), attrOpt);
            AttributeKvEntry attributeKvEntry = attrOpt.orElseGet(() -> new BaseAttributeKvEntry(createDefaultKvEntry(argument), startTs, 0L));
            return transformSingleValueArgument(Optional.of(attributeKvEntry));
        }, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> fetchTsLatest(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), 0, startTs, 0, 1, Aggregation.NONE);
        log.trace("[{}][{}] Fetching timeseries for latest for query {}", tenantId, entityId, query);
        ListenableFuture<List<TsKvEntry>> tsKvListFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        return Futures.transform(tsKvListFuture, tsKvList -> {
            log.debug("[{}][{}] Fetched timeseries for latest for query {}: {}", tenantId, entityId, query, tsKvList);
            TsKvEntry tsKvEntry;
            if (tsKvList.isEmpty() || tsKvList.get(0) == null || tsKvList.get(0).getValue() == null) {
                tsKvEntry = new BasicTsKvEntry(startTs, createDefaultKvEntry(argument), 0L);
            } else {
                tsKvEntry = tsKvList.get(0);
            }
            return transformSingleValueArgument(Optional.of(tsKvEntry));
        }, calculatedFieldCallbackExecutor);
    }

    private ListenableFuture<ArgumentEntry> fetchTsRolling(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        long argTimeWindow = argument.getTimeWindow() == 0 ? startTs : argument.getTimeWindow();
        long startInterval = startTs - argTimeWindow;
        long maxDataPoints = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxDataPointsPerRollingArg);
        int argumentLimit = argument.getLimit();
        int limit = argumentLimit == 0 || argumentLimit > maxDataPoints ? (int) maxDataPoints : argument.getLimit();

        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startInterval, startTs, 0, limit, Aggregation.NONE);
        log.trace("[{}][{}] Fetching timeseries for query {}", tenantId, entityId, query);
        ListenableFuture<List<TsKvEntry>> tsRollingFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        return Futures.transform(tsRollingFuture, tsRolling -> {
            log.debug("[{}][{}] Fetched {} timeseries for query {}", tenantId, entityId, tsRolling.size(), query);
            return ArgumentEntry.createTsRollingArgument(tsRolling, limit, argTimeWindow);
        }, calculatedFieldCallbackExecutor);
    }

    private List<TsKvEntry> fetchTelemetryBatch(TenantId tenantId, EntityId entityId, Argument argument, long startTs, long endTs, int limit) throws InterruptedException {
        EntityId sourceEntityId = argument.getRefEntityId() != null ? argument.getRefEntityId() : entityId;
        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startTs, endTs, 0, limit, Aggregation.NONE, "ASC");
        log.trace("[{}][{}] Fetching telemetry batch for query {}", tenantId, entityId, query);
        List<TsKvEntry> result;// will be interrupted on task processing timeout
        try {
            result = timeseriesService.findAll(tenantId, sourceEntityId, List.of(query)).get();
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch telemetry for " + sourceEntityId + " for key " + argument.getRefEntityKey().getKey(), e.getCause());
        }
        log.debug("[{}][{}] Fetched {} timeseries for query {}", tenantId, entityId, result.size(), query);
        return result;
    }

    private CalculatedFieldCtx getCFCtx(CalculatedField calculatedField) {
        CalculatedFieldCtx ctx = new CalculatedFieldCtx(calculatedField, tbelInvokeService, apiLimitService);
        ctx.init();
        return ctx;
    }

    private void saveResult(TenantId tenantId, EntityId entityId, CalculatedFieldResult calculatedFieldResult, long ts) throws InterruptedException {
        OutputType type = calculatedFieldResult.getType();
        JsonElement result = JsonParser.parseString(Objects.requireNonNull(JacksonUtil.toString(calculatedFieldResult.getResult())));
        log.trace("[{}][{}] Saving calculated field result: {}", tenantId, entityId, result);
        SettableFuture<Void> future = SettableFuture.create();
        switch (type) {
            case TIME_SERIES -> {
                Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToTelemetry(result, ts);
                List<TsKvEntry> tsKvEntryList = new ArrayList<>();
                for (Entry<Long, List<KvEntry>> tsKvEntry : tsKvMap.entrySet()) {
                    for (KvEntry kvEntry : tsKvEntry.getValue()) {
                        tsKvEntryList.add(new BasicTsKvEntry(tsKvEntry.getKey(), kvEntry));
                    }
                }
                telemetrySubscriptionService.saveTimeseriesInternal(TimeseriesSaveRequest.builder()
                        .tenantId(tenantId)
                        .entityId(entityId)
                        .entries(tsKvEntryList)
                        .strategy(new Strategy(true, false, false, false))
                        .future(future)
                        .build()
                );
            }
            case ATTRIBUTES -> {
                List<AttributeKvEntry> attributes = new ArrayList<>(JsonConverter.convertToAttributes(result, ts));
                telemetrySubscriptionService.saveAttributes(AttributesSaveRequest.builder()
                        .tenantId(tenantId)
                        .entityId(entityId)
                        .scope(calculatedFieldResult.getScope())
                        .entries(attributes)
                        .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                        .future(future)
                        .build()
                );
            }
            default -> {
                throw new IllegalArgumentException("Unsupported output type: " + type);
            }
        }
        try {
            future.get();
            log.debug("[{}][{}] Saved calculated field result: {}", tenantId, entityId, result);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to save calculated field result", e.getCause());
        }
    }

}
