/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud.rpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.edge.stats.CloudStatsCounterService;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.cloud.DownlinkMessageService;
import org.thingsboard.server.service.cloud.config.EdgeConfigurationHandler;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;
import org.thingsboard.server.service.cloud.info.PendingUplinkMsgPackHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BaseGrpcClientManagerTest {

    private static final long INITIAL_TIMEOUT_MS = 1000L;
    private static final long MAX_TIMEOUT_MS = 8000L;
    private static final long TIMEOUT_MS = 10000L;

    @Mock
    private EdgeRpcClient edgeRpcClient;
    @Mock
    private EdgeInfoHolder edgeInfo;
    @Mock
    private PendingUplinkMsgPackHolder pendingMsgs;
    @Mock
    private CloudStatsCounterService statsCounterService;
    @Mock
    private PartitionService partitionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ConnectionStatusManager connectionStatusManager;
    @Mock
    private ConfigurableApplicationContext context;
    @Mock
    private EdgeConfigurationHandler edgeConfigurationHandler;
    @Mock
    private DownlinkMessageService downlinkMessageService;
    @Mock
    private ScheduledExecutorService reconnectExecutor;

    private BaseGrpcClientManager manager;

    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final List<Long> scheduledDelays = new ArrayList<>();
    private final List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();

    @BeforeEach
    void setUp() {
        manager = new BaseGrpcClientManager(edgeRpcClient, edgeInfo, pendingMsgs, statsCounterService,
                partitionService, eventPublisher, connectionStatusManager, context, edgeConfigurationHandler,
                downlinkMessageService);
        ReflectionTestUtils.setField(manager, "reconnectExecutor", reconnectExecutor);

        lenient().when(edgeInfo.getReconnectTimeoutMs()).thenReturn(INITIAL_TIMEOUT_MS);
        lenient().when(edgeInfo.getReconnectMaxTimeoutMs()).thenReturn(MAX_TIMEOUT_MS);

        // Capture every task the reconnect loop schedules so the test can drive it deterministically.
        lenient().when(reconnectExecutor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .thenAnswer(invocation -> {
                    scheduledTasks.add(invocation.getArgument(0));
                    scheduledDelays.add(invocation.getArgument(1));
                    ScheduledFuture<?> future = mock(ScheduledFuture.class);
                    scheduledFutures.add(future);
                    return future;
                });
    }

    @Test
    void reconnectDelayDoublesUpToMaxTimeout() throws InterruptedException {
        triggerReconnect();
        assertThat(scheduledDelays).containsExactly(1000L);

        // Each fired attempt reschedules the next one with a doubled delay, capped at the max.
        runScheduledTask(0);
        runScheduledTask(1);
        runScheduledTask(2);
        runScheduledTask(3);

        assertThat(scheduledDelays).containsExactly(1000L, 2000L, 4000L, 8000L, 8000L);
        verify(edgeRpcClient, times(4)).disconnect(true);
        verify(edgeRpcClient, times(4)).connect(any(), any(), any(), any(), any(), any());
    }

    @Test
    void reconnectDelayIsJitteredAroundTheDoublingBase() {
        lenient().when(edgeInfo.getReconnectJitterFactor()).thenReturn(0.5);

        triggerReconnect();
        for (int i = 0; i < 20; i++) {
            runScheduledTask(i);
        }

        // Each delay sits within ±50% of its base (1000/2000/4000). The bases are unaffected by jitter, so
        // the ranges stay centred on the doubling sequence - if jitter compounded, the third delay could
        // reach 1500*2*1.5*2*1.5 and escape its range.
        assertThat(scheduledDelays.get(0)).isBetween(500L, 1500L);
        assertThat(scheduledDelays.get(1)).isBetween(1000L, 3000L);
        assertThat(scheduledDelays.get(2)).isBetween(2000L, 6000L);

        // From the 4th attempt on every base is the same capped 8000ms, so these delays must still differ
        // from one another - that is the whole point of jitter, and a fixed delay would fail here.
        List<Long> cappedDelays = scheduledDelays.subList(3, scheduledDelays.size());
        assertThat(cappedDelays).allSatisfy(delay -> assertThat(delay).isBetween(4000L, 12000L));
        assertThat(new HashSet<>(cappedDelays)).as("jitter must actually vary the delay").hasSizeGreaterThan(1);
    }

    @Test
    void secondScheduleReconnectWhileLoopRunningIsNoOp() {
        triggerReconnect();
        triggerReconnect();

        assertThat(scheduledDelays).containsExactly(1000L);
        verify(reconnectExecutor, times(1)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    void reconnectTimeoutResetsAfterCancel() {
        triggerReconnect();
        runScheduledTask(0);
        assertThat(scheduledDelays).containsExactly(1000L, 2000L);

        // cancelReconnect() is the teardown path used by onEdgeUpdate (successful connect) and destroy.
        ReflectionTestUtils.invokeMethod(manager, "cancelReconnect");
        assertReconnectStopped();

        // A fresh failure must start the backoff over from the initial timeout.
        triggerReconnect();
        assertThat(scheduledDelays).containsExactly(1000L, 2000L, 1000L);
    }

    @Test
    void destroyCancelsReconnectLoop() throws InterruptedException {
        triggerReconnect();
        ScheduledFuture<?> pendingAttempt = scheduledFutures.get(0);

        ReflectionTestUtils.invokeMethod(manager, "destroy");

        verify(pendingAttempt).cancel(true);
        assertReconnectStopped();
        verify(edgeRpcClient).disconnect(false);
    }

    @Test
    void destroyShutsDownReconnectExecutorBeforeDisconnecting() throws InterruptedException {
        triggerReconnect();

        ReflectionTestUtils.invokeMethod(manager, "destroy");

        // The channel shutdown inside disconnect(false) fires the gRPC onError callback, which calls
        // scheduleReconnect - so the executor must already be gone by then.
        InOrder inOrder = inOrder(reconnectExecutor, edgeRpcClient);
        inOrder.verify(reconnectExecutor).shutdownNow();
        inOrder.verify(edgeRpcClient).disconnect(false);
        assertThat(ReflectionTestUtils.getField(manager, "reconnectExecutor")).isNull();
    }

    @Test
    void reEstablishingConnectionReplacesPreviousReconnectExecutor() {
        lenient().when(partitionService.resolve(any(), any(TenantId.class), any(TenantId.class)))
                .thenReturn(new TopicPartitionInfo("tb_core", TenantId.SYS_TENANT_ID, 0, true));
        lenient().when(edgeInfo.getRoutingKey()).thenReturn("key");
        lenient().when(edgeInfo.getRoutingSecret()).thenReturn("secret");
        // Run the scheduled connect task immediately instead of after the initial reconnect timeout.
        lenient().when(edgeInfo.getReconnectTimeoutMs()).thenReturn(0L);

        try {
            manager.establishRpcConnection();
            verify(edgeRpcClient, timeout(TIMEOUT_MS)).connect(any(), any(), any(), any(), any(), any());
            ExecutorService firstReconnect = (ExecutorService) ReflectionTestUtils.getField(manager, "reconnectExecutor");
            assertThat(firstReconnect).isNotNull().isNotSameAs(reconnectExecutor);

            // A successful connect followed by a dropped connection leaves initialized/initInProgress cleared
            // (onEdgeUpdate and scheduleReconnect both reset them), so a PartitionChangeEvent re-enters init.
            // The edgeInfo mock already reports both as false, which is exactly that state.
            manager.establishRpcConnection();
            verify(edgeRpcClient, timeout(TIMEOUT_MS).times(2)).connect(any(), any(), any(), any(), any(), any());

            assertThat(firstReconnect.isShutdown()).as("previous reconnect executor must not be orphaned").isTrue();
            assertThat(ReflectionTestUtils.getField(manager, "reconnectExecutor")).isNotSameAs(firstReconnect);
        } finally {
            ReflectionTestUtils.invokeMethod(manager, "destroy");
            shutdownConnectExecutor();
        }
    }

    @Test
    void blankRoutingKeyCreatesSingleShutdownExecutor() {
        lenient().when(edgeInfo.getRoutingKey()).thenReturn("");
        lenient().when(edgeInfo.getRoutingSecret()).thenReturn("");

        assertThat((Boolean) ReflectionTestUtils.invokeMethod(manager, "validateRoutingKeyAndSecret")).isFalse();
        ExecutorService first = (ExecutorService) ReflectionTestUtils.getField(manager, "shutdownExecutor");
        assertThat(first).isNotNull();

        // Every PartitionChangeEvent re-enters this path; a second executor would only duplicate the message.
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(manager, "validateRoutingKeyAndSecret")).isFalse();

        assertThat(ReflectionTestUtils.getField(manager, "shutdownExecutor")).isSameAs(first);
        first.shutdownNow();
    }

    @Test
    void scheduleReconnectAfterDestroyIsNoOp() {
        ReflectionTestUtils.invokeMethod(manager, "destroy");

        // Mimics the gRPC onError callback arriving after the executor was torn down.
        triggerReconnect();

        assertThat(scheduledDelays).isEmpty();
        assertThat((Boolean) ReflectionTestUtils.getField(manager, "reconnecting")).isFalse();
        verify(reconnectExecutor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    void destroyShutsDownConnectExecutorAndDropsPendingRetry() throws Exception {
        // A long delay keeps the scheduled connect task pending, so destroy() has something to discard.
        lenient().when(edgeInfo.getReconnectTimeoutMs()).thenReturn(TIMEOUT_MS);
        manager.establishRpcConnection();
        ExecutorService connectExecutor = (ExecutorService) ReflectionTestUtils.getField(manager, "connectExecutor");
        assertThat(connectExecutor).isNotNull();

        ReflectionTestUtils.invokeMethod(manager, "destroy");

        // cloud-manager-connect threads are non-daemon, so one left running holds up JVM shutdown.
        assertThat(ReflectionTestUtils.getField(manager, "connectExecutor")).isNull();
        assertThat(ReflectionTestUtils.getField(manager, "connectFuture")).isNull();
        assertThat(connectExecutor.isShutdown()).as("connect executor must not be orphaned").isTrue();

        // Terminating promptly proves the queued retry was discarded rather than left to fire later and
        // resurrect the manager after destroy. Waiting the full delay would mean it was still pending.
        assertThat(connectExecutor.awaitTermination(TIMEOUT_MS / 2, TimeUnit.MILLISECONDS))
                .as("pending connect retry must be dropped on shutdown").isTrue();
        verify(edgeRpcClient, never()).connect(any(), any(), any(), any(), any(), any());
    }

    @Test
    void destroyIsSafeWhenRunningOnTheConnectExecutorThread() throws Exception {
        lenient().when(edgeInfo.getReconnectTimeoutMs()).thenReturn(TIMEOUT_MS);
        manager.establishRpcConnection();
        ScheduledExecutorService connectExecutor =
                (ScheduledExecutorService) ReflectionTestUtils.getField(manager, "connectExecutor");

        // onDestroy calls destroy() from a task running on connectExecutor itself when the system tenant
        // partition moves away, so the teardown must not interrupt the very thread that is executing it.
        Boolean interrupted = connectExecutor.submit(() -> {
            ReflectionTestUtils.invokeMethod(manager, "destroy");
            return Thread.currentThread().isInterrupted();
        }).get(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertThat(interrupted).as("destroy() must not interrupt the thread it runs on").isFalse();
        verify(edgeRpcClient).disconnect(false);
    }

    @Test
    void failedEdgeUpdateReArmsReconnect() throws Exception {
        when(edgeConfigurationHandler.initAndUpdateEdgeSettings(any())).thenThrow(new RuntimeException("DB is down"));

        // The channel stays up after a failed init, so nothing else would ever retry the handshake.
        ReflectionTestUtils.invokeMethod(manager, "onEdgeUpdate", ceEdgeConfiguration());

        assertThat(scheduledDelays).as("failed init must re-arm the reconnect loop").containsExactly(INITIAL_TIMEOUT_MS);
        assertThat((Boolean) ReflectionTestUtils.getField(manager, "reconnecting")).isTrue();
        assertThat(ReflectionTestUtils.getField(manager, "reconnectFuture")).isNotNull();
        verify(edgeInfo).setInitInProgress(false);
    }

    @Test
    void failedEdgeUpdateDoesNotResetTheBackoff() throws Exception {
        triggerReconnect();
        runScheduledTask(0);
        assertThat(scheduledDelays).containsExactly(INITIAL_TIMEOUT_MS, 2 * INITIAL_TIMEOUT_MS);
        when(edgeConfigurationHandler.initAndUpdateEdgeSettings(any())).thenThrow(new RuntimeException("DB is down"));

        ReflectionTestUtils.invokeMethod(manager, "onEdgeUpdate", ceEdgeConfiguration());

        assertThat(scheduledDelays).as("a failed init must not restart the backoff from the initial timeout")
                .containsExactly(INITIAL_TIMEOUT_MS, 2 * INITIAL_TIMEOUT_MS, 2 * INITIAL_TIMEOUT_MS);
        assertThat((Boolean) ReflectionTestUtils.getField(manager, "reconnecting")).isTrue();
    }

    private static EdgeConfiguration ceEdgeConfiguration() {
        return EdgeConfiguration.newBuilder().setCloudType("CE").build();
    }

    private void shutdownConnectExecutor() {
        ExecutorService connectExecutor = (ExecutorService) ReflectionTestUtils.getField(manager, "connectExecutor");
        if (connectExecutor != null) {
            connectExecutor.shutdownNow();
        }
    }

    private void triggerReconnect() {
        ReflectionTestUtils.invokeMethod(manager, "scheduleReconnect", new Exception("connection lost"));
    }

    private void runScheduledTask(int index) {
        scheduledTasks.get(index).run();
    }

    private void assertReconnectStopped() {
        assertThat(ReflectionTestUtils.getField(manager, "reconnectFuture")).isNull();
        assertThat((Boolean) ReflectionTestUtils.getField(manager, "reconnecting")).isFalse();
    }

}
