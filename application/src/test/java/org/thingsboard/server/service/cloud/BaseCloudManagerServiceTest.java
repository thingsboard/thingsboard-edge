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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BaseCloudManagerServiceTest {

    private static final long INITIAL_TIMEOUT_MS = 1000L;
    private static final long MAX_TIMEOUT_MS = 8000L;

    @Mock
    private EdgeRpcClient edgeRpcClient;
    @Mock
    private ScheduledExecutorService reconnectExecutor;

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
