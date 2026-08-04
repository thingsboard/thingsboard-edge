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
package org.thingsboard.server.service.cloud;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.cloud.EdgeSettingsService;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
public class BaseCloudManagerServiceTest {

    private static final long INITIAL_TIMEOUT_MS = 1000L;
    private static final long MAX_TIMEOUT_MS = 8000L;
    private static final long TIMEOUT_MS = 10000L;

    @Mock
    private EdgeRpcClient edgeRpcClient;
    @Mock
    private ScheduledExecutorService reconnectExecutor;
    @Mock
    private PartitionService partitionService;
    @Mock
    private EdgeSettingsService edgeSettingsService;
    @Mock
    private TelemetrySubscriptionService tsSubService;

    private TestCloudManagerService service;

    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final List<Long> scheduledDelays = new ArrayList<>();
    private final List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new TestCloudManagerService();
        ReflectionTestUtils.setField(service, "edgeRpcClient", edgeRpcClient);
        ReflectionTestUtils.setField(service, "reconnectExecutor", reconnectExecutor);
        ReflectionTestUtils.setField(service, "reconnectTimeoutMs", INITIAL_TIMEOUT_MS);
        ReflectionTestUtils.setField(service, "reconnectMaxTimeoutMs", MAX_TIMEOUT_MS);

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
        ReflectionTestUtils.setField(service, "reconnectJitterFactor", 0.5);

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
    void reconnectTimeoutResetsAfterSuccessfulReconnect() {
        triggerReconnect();
        runScheduledTask(0);
        assertThat(scheduledDelays).containsExactly(1000L, 2000L);

        // Simulate a successful reconnect (the onEdgeUpdate / destroy path).
        ReflectionTestUtils.invokeMethod(service, "cancelReconnect");
        assertThat(ReflectionTestUtils.getField(service, "reconnectFuture")).isNull();
        assertThat((Boolean) ReflectionTestUtils.getField(service, "reconnecting")).isFalse();

        // A fresh failure must start the backoff over from the initial timeout.
        triggerReconnect();
        assertThat(scheduledDelays).containsExactly(1000L, 2000L, 1000L);
    }

    @Test
    void destroyCancelsReconnectLoop() throws InterruptedException {
        triggerReconnect();
        ScheduledFuture<?> pendingAttempt = scheduledFutures.get(0);

        service.destroy();

        verify(pendingAttempt).cancel(true);
        assertThat(ReflectionTestUtils.getField(service, "reconnectFuture")).isNull();
        assertThat((Boolean) ReflectionTestUtils.getField(service, "reconnecting")).isFalse();
        verify(edgeRpcClient).disconnect(false);
    }

    @Test
    void destroyShutsDownReconnectExecutorBeforeDisconnecting() throws InterruptedException {
        triggerReconnect();

        service.destroy();

        // The channel shutdown inside disconnect(false) fires the gRPC onError callback, which calls
        // scheduleReconnect - so the executor must already be gone by then.
        InOrder inOrder = inOrder(reconnectExecutor, edgeRpcClient);
        inOrder.verify(reconnectExecutor).shutdownNow();
        inOrder.verify(edgeRpcClient).disconnect(false);
        assertThat(ReflectionTestUtils.getField(service, "reconnectExecutor")).isNull();
    }

    @Test
    void reEstablishingConnectionReplacesPreviousExecutors() throws InterruptedException {
        ReflectionTestUtils.setField(service, "partitionService", partitionService);
        when(partitionService.resolve(any(), any(TenantId.class), any(TenantId.class)))
                .thenReturn(new TopicPartitionInfo("tb_core", TenantId.SYS_TENANT_ID, 0, true));
        ReflectionTestUtils.setField(service, "routingKey", "key");
        ReflectionTestUtils.setField(service, "routingSecret", "secret");
        ReflectionTestUtils.setField(service, "reconnectTimeoutMs", 0L);

        try {
            service.establishRpcConnection();
            verify(edgeRpcClient, timeout(TIMEOUT_MS)).connect(any(), any(), any(), any(), any(), any());
            ExecutorService firstUplink = (ExecutorService) ReflectionTestUtils.getField(service, "uplinkExecutor");
            ExecutorService firstReconnect = (ExecutorService) ReflectionTestUtils.getField(service, "reconnectExecutor");
            assertThat(firstUplink).isNotNull();
            assertThat(firstReconnect).isNotNull();

            // State left behind by a successful connect followed by a dropped connection: onEdgeUpdate cleared
            // initInProgress, scheduleReconnect cleared initialized. A PartitionChangeEvent now re-enters init.
            ReflectionTestUtils.setField(service, "initInProgress", false);
            ReflectionTestUtils.setField(service, "initialized", false);

            service.establishRpcConnection();
            verify(edgeRpcClient, timeout(TIMEOUT_MS).times(2)).connect(any(), any(), any(), any(), any(), any());

            assertThat(firstUplink.isShutdown()).as("previous uplink executor must not be orphaned").isTrue();
            assertThat(firstReconnect.isShutdown()).as("previous reconnect executor must not be orphaned").isTrue();
            assertThat(ReflectionTestUtils.getField(service, "uplinkExecutor")).isNotSameAs(firstUplink);
            assertThat(ReflectionTestUtils.getField(service, "reconnectExecutor")).isNotSameAs(firstReconnect);
        } finally {
            service.destroy();
            shutdownConnectExecutor();
        }
    }

    @Test
    void blankRoutingKeyCreatesSingleShutdownExecutor() {
        ReflectionTestUtils.setField(service, "routingKey", "");
        ReflectionTestUtils.setField(service, "routingSecret", "");

        assertThat((Boolean) ReflectionTestUtils.invokeMethod(service, "validateRoutingKeyAndSecret")).isFalse();
        ExecutorService first = (ExecutorService) ReflectionTestUtils.getField(service, "shutdownExecutor");
        assertThat(first).isNotNull();

        // Every PartitionChangeEvent re-enters this path; a second executor would only duplicate the message.
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(service, "validateRoutingKeyAndSecret")).isFalse();

        assertThat(ReflectionTestUtils.getField(service, "shutdownExecutor")).isSameAs(first);
        first.shutdownNow();
    }

    @Test
    void scheduleReconnectAfterDestroyIsNoOp() throws InterruptedException {
        service.destroy();

        // Mimics the gRPC onError callback arriving after the executor was torn down.
        triggerReconnect();

        assertThat(scheduledDelays).isEmpty();
        assertThat((Boolean) ReflectionTestUtils.getField(service, "reconnecting")).isFalse();
        verify(reconnectExecutor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    void destroyShutsDownConnectExecutorAndDropsPendingRetry() throws Exception {
        // A long delay keeps the scheduled connect task pending, so destroy() has something to discard.
        ReflectionTestUtils.setField(service, "reconnectTimeoutMs", TIMEOUT_MS);
        service.establishRpcConnection();
        ExecutorService connectExecutor = (ExecutorService) ReflectionTestUtils.getField(service, "connectExecutor");
        assertThat(connectExecutor).isNotNull();

        service.destroy();

        // cloud-manager-connect threads are non-daemon, so one left running holds up JVM shutdown.
        assertThat(ReflectionTestUtils.getField(service, "connectExecutor")).isNull();
        assertThat(ReflectionTestUtils.getField(service, "connectFuture")).isNull();
        assertThat(connectExecutor.isShutdown()).as("connect executor must not be orphaned").isTrue();

        // Terminating promptly proves the queued retry was discarded rather than left to fire later and
        // resurrect the manager after destroy. Waiting the full delay would mean it was still pending.
        assertThat(connectExecutor.awaitTermination(TIMEOUT_MS / 2, TimeUnit.MILLISECONDS))
                .as("pending connect retry must be dropped on shutdown").isTrue();
        verify(edgeRpcClient, never()).connect(any(), any(), any(), any(), any(), any());
    }

    @Test
    void destroyIsSafeWhenRunningOnTheConnectExecutorThread() throws Exception {
        ReflectionTestUtils.setField(service, "reconnectTimeoutMs", TIMEOUT_MS);
        service.establishRpcConnection();
        ScheduledExecutorService connectExecutor =
                (ScheduledExecutorService) ReflectionTestUtils.getField(service, "connectExecutor");

        // The partition-moved-away path calls destroy() from a task running on connectExecutor itself, so
        // the teardown must not interrupt the very thread that is executing it.
        Boolean interrupted = connectExecutor.submit(() -> {
            service.destroy();
            return Thread.currentThread().isInterrupted();
        }).get(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertThat(interrupted).as("destroy() must not interrupt the thread it runs on").isFalse();
        verify(edgeRpcClient).disconnect(false);
    }

    @Test
    void processMsgPackWithoutUplinkExecutorFailsFast() {
        // destroy() clears the uplink executor, and establishRpcConnection replaces it on every reconnect.
        assertPackRejected(null);
    }

    @Test
    void processMsgPackOnShutDownUplinkExecutorFailsFast() {
        ScheduledExecutorService shutDownExecutor = Executors.newSingleThreadScheduledExecutor();
        shutDownExecutor.shutdownNow();

        assertPackRejected(shutDownExecutor);
    }

    @Test
    void failedEdgeUpdateReArmsReconnect() {
        givenSystemTenantPartitionIsMine();
        ReflectionTestUtils.setField(service, "edgeSettingsService", edgeSettingsService);
        ReflectionTestUtils.setField(service, "tsSubService", tsSubService);
        when(edgeSettingsService.findEdgeSettings()).thenThrow(new RuntimeException("DB is down"));

        // The channel stays up after a failed init, so nothing else would ever retry the handshake.
        ReflectionTestUtils.invokeMethod(service, "onEdgeUpdate", ceEdgeConfiguration());

        assertThat(scheduledDelays).as("failed init must re-arm the reconnect loop").containsExactly(INITIAL_TIMEOUT_MS);
        assertThat((Boolean) ReflectionTestUtils.getField(service, "reconnecting")).isTrue();
        assertThat(ReflectionTestUtils.getField(service, "reconnectFuture")).isNotNull();
    }

    @Test
    void failedEdgeUpdateDoesNotResetTheBackoff() {
        triggerReconnect();
        runScheduledTask(0);
        assertThat(scheduledDelays).containsExactly(INITIAL_TIMEOUT_MS, 2 * INITIAL_TIMEOUT_MS);
        givenSystemTenantPartitionIsMine();
        ReflectionTestUtils.setField(service, "edgeSettingsService", edgeSettingsService);
        ReflectionTestUtils.setField(service, "tsSubService", tsSubService);
        when(edgeSettingsService.findEdgeSettings()).thenThrow(new RuntimeException("DB is down"));

        ReflectionTestUtils.invokeMethod(service, "onEdgeUpdate", ceEdgeConfiguration());

        assertThat(scheduledDelays).as("a failed init must not restart the backoff from the initial timeout")
                .containsExactly(INITIAL_TIMEOUT_MS, 2 * INITIAL_TIMEOUT_MS, 2 * INITIAL_TIMEOUT_MS);
        assertThat((Boolean) ReflectionTestUtils.getField(service, "reconnecting")).isTrue();
    }

    private void givenSystemTenantPartitionIsMine() {
        ReflectionTestUtils.setField(service, "partitionService", partitionService);
        when(partitionService.resolve(any(), any(TenantId.class), any(TenantId.class)))
                .thenReturn(new TopicPartitionInfo("tb_core", TenantId.SYS_TENANT_ID, 0, true));
    }

    private static EdgeConfiguration ceEdgeConfiguration() {
        return EdgeConfiguration.newBuilder().setCloudType("CE").build();
    }

    @Test
    void emptyPackDoesNotStrandTheUplinkFuture() {
        // Already completed, so interruptPreviousSendUplinkMsgsTask returns without waiting out its timeout.
        SettableFuture<Boolean> previous = SettableFuture.create();
        previous.set(false);
        ReflectionTestUtils.setField(service, "sendUplinkFutureResult", previous);

        // No events convert to an uplink msg, so processCloudEvents takes the empty-pack early return.
        assertThat(service.processCloudEvents(List.of(), true)).isDone();

        // A fresh future created before that return would never be completed, and the next
        // interruptPreviousSendUplinkMsgsTask would block for its full 10s timeout on it.
        assertThat(ReflectionTestUtils.getField(service, "sendUplinkFutureResult")).isSameAs(previous);
    }

    // The pack must surface as a failed future rather than an escaping unchecked exception: both callers
    // already handle ExecutionException, whereas RejectedExecutionException crossing processCloudEvents
    // misses the Kafka catch and the batch is dropped without being committed. Completing as interrupted
    // would instead tell processUplinkMessages to re-query the same page forever.
    private void assertPackRejected(ScheduledExecutorService executor) {
        SettableFuture<Boolean> result = SettableFuture.create();
        ReflectionTestUtils.setField(service, "sendUplinkFutureResult", result);
        ReflectionTestUtils.setField(service, "uplinkExecutor", executor);

        ReflectionTestUtils.invokeMethod(service, "processMsgPack", List.of(uplinkMsg()), true);

        assertThat(result.isDone()).as("pack must be reported as failed, not left pending").isTrue();
        assertThatThrownBy(result::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RejectedExecutionException.class);
    }

    private static UplinkMsg uplinkMsg() {
        return UplinkMsg.newBuilder().setUplinkMsgId(1).build();
    }

    private void shutdownConnectExecutor() {
        ExecutorService connectExecutor = (ExecutorService) ReflectionTestUtils.getField(service, "connectExecutor");
        if (connectExecutor != null) {
            connectExecutor.shutdownNow();
        }
    }

    private void triggerReconnect() {
        ReflectionTestUtils.invokeMethod(service, "scheduleReconnect", new Exception("connection lost"));
    }

    private void runScheduledTask(int index) {
        scheduledTasks.get(index).run();
    }

    private static class TestCloudManagerService extends BaseCloudManagerService {

        @Override
        protected void launchUplinkProcessing() {
        }

        @Override
        protected void onDestroy() {
        }

        @Override
        protected void onTbApplicationEvent(PartitionChangeEvent event) {
        }

    }

}
