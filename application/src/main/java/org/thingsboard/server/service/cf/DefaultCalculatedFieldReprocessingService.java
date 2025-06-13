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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest.Strategy;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
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
import org.thingsboard.server.common.data.util.TbPair;
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
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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

    @Value("${queue.calculated_fields.telemetry_fetch_pack_size:2000}")
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
        CalculatedField calculatedField = task.getCalculatedField();
        if (OutputType.ATTRIBUTES.equals(calculatedField.getConfiguration().getOutput().getType())) {
            throw new IllegalArgumentException("'ATTRIBUTES' output type is not supported for reprocessing");
        }

        long startTs = task.getStartTs();
        long endTs = task.getEndTs();

        CalculatedFieldCtx cfCtx = new CalculatedFieldCtx(calculatedField, tbelInvokeService, apiLimitService);
        CalculatedFieldState state = initState(tenantId, entityId, cfCtx, startTs);
        cfCtx.init();
        CfReprocessingCtx ctx = CfReprocessingCtx.builder()
                .tenantId(tenantId)
                .entityId(entityId)
                .cfCtx(cfCtx)
                .state(state)
                .build();

        try (ctx) {
            ctx.checkStateSize();
            processStateIfReady(ctx, startTs).get();

            for (Entry<String, Argument> e : ctx.getCfCtx().getArguments().entrySet()) {
                String argName = e.getKey();
                Argument arg = e.getValue();
                if (ArgumentType.ATTRIBUTE.equals(arg.getRefEntityKey().getType())) {
                    continue;
                }
                LinkedList<TsKvEntry> batch = new LinkedList<>(fetchTelemetryBatch(tenantId, entityId, arg, startTs, endTs, telemetryFetchPackSize));
                if (!batch.isEmpty()) {
                    ctx.getTelemetryBuffers().put(argName, batch);
                    ctx.getCursors().put(argName, batch.getLast().getTs());
                }
            }

            while (true) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                OptionalLong minTs = ctx.getTelemetryBuffers().values().stream()
                        .filter(buffer -> !buffer.isEmpty())
                        .mapToLong(buffer -> buffer.get(0).getTs())
                        .min();
                if (minTs.isEmpty()) {
                    var latestResult = ctx.getLatestResult();
                    if (latestResult != null) {
                        saveResult(ctx, latestResult.getSecond(), latestResult.getFirst(), Strategy.LATEST_AND_WS);
                    }
                    break;
                }

                Map<String, ArgumentEntry> updatedArgs = getUpdatedArgs(ctx, minTs.getAsLong(), startTs, endTs);
                Future<Void> result = processArgumentValuesUpdate(ctx, updatedArgs, minTs.getAsLong());
                ctx.addResult(result, telemetryFetchPackSize);
            }
            ctx.awaitResults();
        }
    }

    private Map<String, ArgumentEntry> getUpdatedArgs(CfReprocessingCtx ctx, long minTs, long startTs, long endTs) throws InterruptedException {
        Map<String, ArgumentEntry> updatedArgs = new HashMap<>();
        for (Entry<String, LinkedList<TsKvEntry>> entry : ctx.getTelemetryBuffers().entrySet()) {
            String argName = entry.getKey();
            LinkedList<TsKvEntry> buffer = entry.getValue();

            if (!buffer.isEmpty() && buffer.getFirst().getTs() == minTs) {
                TsKvEntry tsEntry = buffer.removeFirst();
                updatedArgs.put(argName, ArgumentEntry.createSingleValueArgument(tsEntry));

                if (buffer.isEmpty()) {
                    Argument arg = ctx.getCfCtx().getArguments().get(argName);
                    Long cursorTs = ctx.getCursors().getOrDefault(argName, startTs);
                    LinkedList<TsKvEntry> nextBatch = fetchTelemetryBatch(ctx.getTenantId(), ctx.getEntityId(), arg, cursorTs, endTs, telemetryFetchPackSize).stream()
                            .filter(tsKvEntry -> tsKvEntry.getTs() > cursorTs)
                            .collect(Collectors.toCollection(LinkedList::new));
                    if (!nextBatch.isEmpty()) {
                        ctx.getTelemetryBuffers().put(argName, nextBatch);
                        ctx.getCursors().put(argName, nextBatch.getLast().getTs());
                    }
                }
            }
        }
        return updatedArgs;
    }

    private Future<Void> processStateIfReady(CfReprocessingCtx ctx, long ts) throws Exception {
        CalculatedFieldState state = ctx.getState();
        if (ctx.getCfCtx().isInitialized() && state.isReady()) {
            log.trace("[{}][{}] Performing calculation for CF {}", ctx.getTenantId(), ctx.getEntityId(), ctx.getCfId());
            CalculatedFieldResult calculationResult = state.performCalculation(ctx.getCfCtx()).get(cfCalculationResultTimeout, TimeUnit.SECONDS);
            ctx.checkStateSize();
            if (!calculationResult.isEmpty()) {
                ctx.setLatestResult(new TbPair<>(ts, calculationResult));
                return saveResult(ctx, calculationResult, ts, Strategy.TIME_SERIES_ONLY);
            }
        } else {
            ctx.checkStateSize();
        }
        return Futures.immediateVoidFuture();
    }

    private Future<Void> processArgumentValuesUpdate(CfReprocessingCtx ctx, Map<String, ArgumentEntry> newArgValues, long ts) throws Exception {
        if (newArgValues.isEmpty()) {
            log.info("[{}] No argument values to process for CF.", ctx.getCfId());
        }
        if (ctx.getState().updateState(ctx.getCfCtx(), newArgValues)) {
            return processStateIfReady(ctx, ts);
        } else {
            return Futures.immediateVoidFuture();
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
                            Entry::getKey,
                            entry -> {
                                try {
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
            throw new RuntimeException("Failed to fetch telemetry for " + sourceEntityId + " for key " + argument.getRefEntityKey().getKey() + ": " + e.getCause().getMessage(), e.getCause());
        }
        log.debug("[{}][{}] Fetched {} timeseries for query {}", tenantId, entityId, result.size(), query);
        return result;
    }

    private Future<Void> saveResult(CfReprocessingCtx ctx, CalculatedFieldResult calculatedFieldResult, long ts, Strategy strategy) throws InterruptedException {
        OutputType type = calculatedFieldResult.getType();
        JsonElement result = JsonParser.parseString(Objects.requireNonNull(JacksonUtil.toString(calculatedFieldResult.getResult())));
        log.trace("[{}][{}] Saving CF result: {}", ctx.getTenantId(), ctx.getEntityId(), result);
        SettableFuture<Void> future = SettableFuture.create();
        if (OutputType.TIME_SERIES.equals(type)) {
            Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToTelemetry(result, ts);
            List<TsKvEntry> tsKvEntryList = new ArrayList<>();
            for (Entry<Long, List<KvEntry>> tsKvEntry : tsKvMap.entrySet()) {
                for (KvEntry kvEntry : tsKvEntry.getValue()) {
                    tsKvEntryList.add(new BasicTsKvEntry(tsKvEntry.getKey(), kvEntry));
                }
            }
            telemetrySubscriptionService.saveTimeseriesInternal(TimeseriesSaveRequest.builder()
                    .tenantId(ctx.getTenantId())
                    .entityId(ctx.getEntityId())
                    .entries(tsKvEntryList)
                    .strategy(strategy)
                    .future(future)
                    .build()
            );
        } else {
            throw new IllegalArgumentException("Unsupported output type: " + type);
        }
        if (log.isTraceEnabled()) {
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(Void v) {
                    log.debug("[{}][{}] Saved CF result: {}", ctx.getTenantId(), ctx.getEntityId(), result);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to save CF result {}", ctx.getTenantId(), ctx.getEntityId(), result, t);
                }
            }, MoreExecutors.directExecutor());
        }
        return future;
    }

    @Getter
    public static class CfReprocessingCtx implements AutoCloseable {

        private final TenantId tenantId;
        private final EntityId entityId;
        private final CalculatedFieldCtx cfCtx;
        private final CalculatedFieldState state;
        private final CalculatedFieldId cfId;
        private final CalculatedFieldEntityCtxId ctxId;

        @Setter
        private TbPair<Long, CalculatedFieldResult> latestResult;

        private final Map<String, LinkedList<TsKvEntry>> telemetryBuffers = new HashMap<>();
        private final Map<String, Long> cursors = new HashMap<>();
        private final List<Future<Void>> resultFutures = new ArrayList<>();


        @Builder
        public CfReprocessingCtx(TenantId tenantId, EntityId entityId, CalculatedFieldCtx cfCtx, CalculatedFieldState state) {
            this.tenantId = tenantId;
            this.entityId = entityId;
            this.cfCtx = cfCtx;
            this.state = state;
            this.cfId = cfCtx.getCfId();
            this.ctxId = new CalculatedFieldEntityCtxId(tenantId, cfId, entityId);
        }

        public void checkStateSize() {
            state.checkStateSize(ctxId, cfCtx.getMaxStateSize());
            if (!state.isSizeOk()) {
                throw new RuntimeException(cfCtx.getSizeExceedsLimitMessage());
            }
        }

        public void addResult(Future<Void> resultFuture, int awaitPeriod) throws InterruptedException {
            resultFutures.add(resultFuture);
            if (resultFutures.size() % awaitPeriod == 0) {
                awaitResults();
            }
        }

        private void awaitResults() throws InterruptedException {
            for (Future<Void> resultFuture : resultFutures) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                try {
                    resultFuture.get();
                } catch (ExecutionException e) { // in case of single failure - cancelling everything
                    throw new RuntimeException("Failed to save calculated field results: " + e.getCause().getMessage(), e.getCause());
                }
            }
            log.debug("[{}][{}] Saved {} CF results", tenantId, entityId, resultFutures.size());
            resultFutures.clear();
        }

        @Override
        public void close() {
            log.debug("[{}][{}] Closing CF reprocessing context", tenantId, entityId);
            telemetryBuffers.clear();
            resultFutures.forEach(future -> future.cancel(true));
            cfCtx.stop();
        }

    }

}
