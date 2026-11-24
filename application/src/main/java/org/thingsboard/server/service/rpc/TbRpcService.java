// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.HashPartitioner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.RpcPersistResult;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@TbCoreComponent
@Service
@Slf4j
public class TbRpcService {
    private final RpcService rpcService;
    private final TbClusterService tbClusterService;

    // Striped by rpcId so one command's lifecycle events keep their order (RPC_QUEUED before RPC_DELIVERED).
    private final ExecutorService[] ruleEngineCallbackExecutors;
    // Separate pool, so a stalled rule engine publish never holds up a device send.
    private final ExecutorService deviceActorCallbackExecutor;

    @Autowired
    private ApplicationEventPublisher eventPublisher; // Edge only

    public TbRpcService(RpcService rpcService, TbClusterService tbClusterService,
                        @Value("${sql.rpc.rule_engine_callback_threads:3}") int ruleEngineCallbackThreads,
                        @Value("${sql.rpc.device_actor_callback_threads:3}") int deviceActorCallbackThreads) {
        if (ruleEngineCallbackThreads < 1) {
            throw new IllegalArgumentException("sql.rpc.rule_engine_callback_threads must be >= 1, but was " + ruleEngineCallbackThreads);
        }
        if (deviceActorCallbackThreads < 1) {
            throw new IllegalArgumentException("sql.rpc.device_actor_callback_threads must be >= 1, but was " + deviceActorCallbackThreads);
        }
        this.rpcService = rpcService;
        this.tbClusterService = tbClusterService;
        this.ruleEngineCallbackExecutors = new ExecutorService[ruleEngineCallbackThreads];
        for (int i = 0; i < ruleEngineCallbackThreads; i++) {
            ruleEngineCallbackExecutors[i] = Executors.newSingleThreadExecutor(
                    ThingsBoardThreadFactory.forName("rpc-rule-engine-callback-" + i));
        }
        this.deviceActorCallbackExecutor = Executors.newFixedThreadPool(deviceActorCallbackThreads,
                ThingsBoardThreadFactory.forName("rpc-device-actor-callback"));
    }

    @PreDestroy
    private void destroy() {
        for (ExecutorService executor : ruleEngineCallbackExecutors) {
            executor.shutdownNow();
        }
        deviceActorCallbackExecutor.shutdownNow();
    }

    /**
     * Enqueues the create onto the batched write queue and resumes the caller once the row's fate is known.
     * The result is reported exactly once, on the actor-resume pool.
     */
    public void createIfAbsent(TenantId tenantId, Rpc rpc, Consumer<RpcPersistResult> onPersistResult) {
        ListenableFuture<Boolean> future = rpcService.createIfAbsentAsync(rpc);
        // Attached before the resume, so RPC_QUEUED reaches the stripe ahead of any status event the resumed
        // actor can cause.
        notifyRuleEngineWhenPersisted(tenantId, rpc, true, future, "a row for this RPC already existed");
        DonAsynchron.withCallback(future,
                inserted -> onPersistResult.accept(Boolean.TRUE.equals(inserted)
                        ? RpcPersistResult.INSERTED : RpcPersistResult.DUPLICATE),
                t -> onPersistResult.accept(RpcPersistResult.FAILED), // logged by the callback above, same future
                deviceActorCallbackExecutor);
    }

    public void update(TenantId tenantId, Rpc rpc) {
        notifyRuleEngineWhenPersisted(tenantId, rpc, false, rpcService.updateAsync(rpc),
                "RPC row is not updatable (already terminal or removed)");
    }

    /** {@code skipReason} explains the case where the write completed without changing a row. */
    private void notifyRuleEngineWhenPersisted(TenantId tenantId, Rpc rpc, boolean created, ListenableFuture<Boolean> future, String skipReason) {
        DonAsynchron.withCallback(future,
                persisted -> {
                    if (Boolean.TRUE.equals(persisted)) {
                        // Edge only: sync only writes that changed a row, so a rejected stale status never reaches the cloud
                        publishSaveEvent(tenantId, rpc, created);
                        pushRpcMsgToRuleEngine(tenantId, rpc);
                    } else {
                        log.debug("[{}][{}][{}] Skipping rule engine notification for status [{}] - {}",
                                tenantId, rpc.getDeviceId(), rpc.getId(), rpc.getStatus(), skipReason);
                    }
                },
                t -> log.error("[{}][{}][{}] Failed to persist RPC with status [{}]",
                        tenantId, rpc.getDeviceId(), rpc.getId(), rpc.getStatus(), t),
                ruleEngineCallbackExecutorFor(rpc.getUuidId()));
    }

    private void publishSaveEvent(TenantId tenantId, Rpc rpc, boolean created) {
        try {
            eventPublisher.publishEvent(SaveEntityEvent.<Rpc>builder()
                    .tenantId(tenantId)
                    .entityId(rpc.getId())
                    .entity(rpc)
                    .created(created)
                    .build());
        } catch (Throwable t) {
            log.error("[{}][{}][{}] Failed to publish RPC with status [{}] for cloud sync",
                    tenantId, rpc.getDeviceId(), rpc.getId(), rpc.getStatus(), t);
        }
    }

    private Executor ruleEngineCallbackExecutorFor(UUID rpcId) {
        return ruleEngineCallbackExecutors[HashPartitioner.resolvePartition(rpcId.hashCode(), ruleEngineCallbackExecutors.length)];
    }

    private void pushRpcMsgToRuleEngine(TenantId tenantId, Rpc rpc) {
        try {
            TbMsg msg = TbMsg.newMsg()
                    .type(TbMsgType.valueOf("RPC_" + rpc.getStatus().name()))
                    .originator(rpc.getDeviceId())
                    .copyMetaData(TbMsgMetaData.EMPTY)
                    .data(JacksonUtil.toString(rpc))
                    .build();
            tbClusterService.pushMsgToRuleEngine(tenantId, rpc.getDeviceId(), msg, null);
        } catch (Throwable t) {
            log.error("[{}][{}][{}] Failed to push RPC with status [{}] to rule engine",
                    tenantId, rpc.getDeviceId(), rpc.getId(), rpc.getStatus(), t);
        }
    }

    public PageData<Rpc> findInFlightForReload(TenantId tenantId, DeviceId deviceId, PageLink pageLink) {
        return rpcService.findInFlightForReload(tenantId, deviceId, pageLink);
    }

    public Rpc findRpcById(TenantId tenantId, RpcId rpcId) {
        return rpcService.findById(tenantId, rpcId);
    }

    /**
     * The only cloud-aware RPC delete: propagates the delete to the cloud (Edge -> Cloud) via DeleteEntityEvent.
     * Other delete paths (RpcService.deleteRpc, TTL clean-up) intentionally do not sync, so each side expires its own copies.
     */
    public void deleteRpc(TenantId tenantId, Rpc rpc) {
        rpcService.deleteRpc(tenantId, rpc.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.<Rpc>builder() // Edge only
                .tenantId(tenantId)
                .entityId(rpc.getId())
                .entity(rpc)
                .build());
    }

}
