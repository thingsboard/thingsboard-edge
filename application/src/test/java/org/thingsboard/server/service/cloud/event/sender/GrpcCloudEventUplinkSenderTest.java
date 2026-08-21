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
package org.thingsboard.server.service.cloud.event.sender;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.edge.stats.CloudStatsCounterService;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.cloud.event.UplinkMsgMapper;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;
import org.thingsboard.server.service.cloud.info.PendingUplinkMsgPackHolder;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.cloud.rpc.GrpcClientManager;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GrpcCloudEventUplinkSenderTest {

    private static final long TIMEOUT_MS = 5000;

    @Mock
    private EdgeInfoHolder edgeInfo;
    @Mock
    private PendingUplinkMsgPackHolder pendingMsgs;
    @Mock
    private UplinkMsgMapper uplinkMsgMapper;
    @Mock
    private CloudEventStorageSettings cloudEventStorageSettings;
    @Mock
    private CloudStatsCounterService statsCounterService;
    @Mock
    private GrpcClientManager grpcClientManager;

    private GrpcCloudEventUplinkSender sender;

    @BeforeEach
    void setUp() {
        sender = new GrpcCloudEventUplinkSender(edgeInfo, pendingMsgs, uplinkMsgMapper,
                cloudEventStorageSettings, statsCounterService, grpcClientManager);
    }

    @AfterEach
    void tearDown() {
        sender.shutdown();
    }

    @Test
    void reInitReplacesPreviousUplinkExecutor() {
        sender.init();
        ExecutorService first = (ExecutorService) ReflectionTestUtils.getField(sender, "uplinkExecutor");
        assertThat(first).isNotNull();

        // init() is re-entered on every GrpcConnectionEstablishedEvent, i.e. after every reconnect.
        sender.init();

        assertThat(first.isShutdown()).as("previous uplink executor must not be orphaned").isTrue();
        assertThat(ReflectionTestUtils.getField(sender, "uplinkExecutor")).isNotNull().isNotSameAs(first);
    }

    @Test
    void shutdownClearsUplinkExecutor() {
        sender.init();
        ExecutorService executor = (ExecutorService) ReflectionTestUtils.getField(sender, "uplinkExecutor");

        sender.shutdown();

        assertThat(executor.isShutdown()).isTrue();
        assertThat(ReflectionTestUtils.getField(sender, "uplinkExecutor")).isNull();

        // @PreDestroy can follow a StopCloudEventProcessingEvent, so a second shutdown must be harmless.
        sender.shutdown();
    }

    @Test
    void processMsgPackWithoutUplinkExecutorFailsFast() {
        // shutdown() clears the executor, and init() replaces it on every GrpcConnectionEstablishedEvent,
        // which the uplink runner is not stopped for - so it can observe the field mid-replacement.
        assertPackRejected(null);
    }

    @Test
    void processMsgPackOnShutDownUplinkExecutorFailsFast() {
        ExecutorService shutDownExecutor = Executors.newSingleThreadExecutor();
        shutDownExecutor.shutdownNow();

        assertPackRejected(shutDownExecutor);
    }

    @Test
    void emptyPackDoesNotStrandTheUplinkFuture() {
        // Already completed, so interruptPreviousSendUplinkMsgsTask returns without waiting out its timeout.
        SettableFuture<Boolean> previous = SettableFuture.create();
        previous.set(false);
        ReflectionTestUtils.setField(sender, "sendUplinkFutureResult", previous);
        when(uplinkMsgMapper.convertCloudEventsToUplink(anyList())).thenReturn(List.of());

        assertThat(sender.sendCloudEvents(List.of(), true)).isDone();

        // A fresh future created before the empty-pack early return would never be completed, and the next
        // interruptPreviousSendUplinkMsgsTask would block for its full 10s timeout on it.
        assertThat(ReflectionTestUtils.getField(sender, "sendUplinkFutureResult")).isSameAs(previous);
    }

    @Test
    void disconnectedTimeseriesPackStopsRetryingInsteadOfLoopingForever() throws Exception {
        SettableFuture<Boolean> result = SettableFuture.create();
        ReflectionTestUtils.setField(sender, "sendUplinkFutureResult", result);
        ExecutorService uplinkExecutor = Executors.newSingleThreadExecutor();
        ReflectionTestUtils.setField(sender, "uplinkExecutor", uplinkExecutor);
        when(grpcClientManager.isConnected()).thenReturn(false);

        try {
            ReflectionTestUtils.invokeMethod(sender, "processMsgPack", List.of(uplinkMsg()), false);

            // Timeseries packs have no attempt cap, so before the connectivity gate this loop retried a dead
            // stream forever - parking the uplink thread and freezing the queue offset until a restart.
            assertThat(result.get(TIMEOUT_MS, TimeUnit.MILLISECONDS))
                    .as("pack must complete as interrupted so the offset is not advanced").isTrue();
            verify(grpcClientManager, never()).sendUplinkMsg(any());
        } finally {
            uplinkExecutor.shutdownNow();
        }
    }

    // The pack must surface as a failed future rather than an escaping unchecked exception: callers already
    // handle ExecutionException, whereas RejectedExecutionException crossing sendCloudEvents misses the Kafka
    // runner's catch and the batch is dropped without being committed. Completing as interrupted would
    // instead tell the Postgres runner to re-query the same page forever.
    private void assertPackRejected(ExecutorService executor) {
        SettableFuture<Boolean> result = SettableFuture.create();
        ReflectionTestUtils.setField(sender, "sendUplinkFutureResult", result);
        ReflectionTestUtils.setField(sender, "uplinkExecutor", executor);

        ReflectionTestUtils.invokeMethod(sender, "processMsgPack", List.of(uplinkMsg()), true);

        assertThat(result.isDone()).as("pack must be reported as failed, not left pending").isTrue();
        assertThatThrownBy(result::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RejectedExecutionException.class);
    }

    private static UplinkMsg uplinkMsg() {
        return UplinkMsg.newBuilder().setUplinkMsgId(1).build();
    }

}
