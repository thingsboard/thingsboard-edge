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
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    void processMsgPackWithoutUplinkExecutorReportsPackAsInterrupted() throws Exception {
        SettableFuture<Boolean> result = SettableFuture.create();
        ReflectionTestUtils.setField(service, "sendUplinkFutureResult", result);
        // destroy() clears the uplink executor, and establishRpcConnection replaces it on every reconnect.
        ReflectionTestUtils.setField(service, "uplinkExecutor", null);

        ReflectionTestUtils.invokeMethod(service, "processMsgPack", List.of(uplinkMsg()), true);

        // sendCloudEvents returns this future and the caller blocks on get(), so leaving it unset would
        // wedge uplink processing permanently. true means "interrupted", i.e. retry, do not commit.
        assertThat(result.isDone()).as("caller's future must never be left uncompleted").isTrue();
        assertThat(result.get()).isTrue();
    }

    @Test
    void processMsgPackOnShutDownUplinkExecutorReportsPackAsInterrupted() throws Exception {
        ScheduledExecutorService shutDownExecutor = Executors.newSingleThreadScheduledExecutor();
        shutDownExecutor.shutdownNow();
        SettableFuture<Boolean> result = SettableFuture.create();
        ReflectionTestUtils.setField(service, "sendUplinkFutureResult", result);
        ReflectionTestUtils.setField(service, "uplinkExecutor", shutDownExecutor);

        ReflectionTestUtils.invokeMethod(service, "processMsgPack", List.of(uplinkMsg()), true);

        assertThat(result.isDone()).as("rejected submission must complete the caller's future").isTrue();
        assertThat(result.get()).isTrue();
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
