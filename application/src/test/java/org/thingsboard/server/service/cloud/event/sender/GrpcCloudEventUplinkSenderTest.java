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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class GrpcCloudEventUplinkSenderTest {

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
    void processMsgPackWithoutUplinkExecutorReportsPackAsInterrupted() throws Exception {
        SettableFuture<Boolean> result = SettableFuture.create();
        ReflectionTestUtils.setField(sender, "sendUplinkFutureResult", result);
        // shutdown() clears the executor, and init() replaces it on every GrpcConnectionEstablishedEvent,
        // which the uplink runner is not stopped for - so it can observe the field mid-replacement.
        ReflectionTestUtils.setField(sender, "uplinkExecutor", null);

        ReflectionTestUtils.invokeMethod(sender, "processMsgPack", List.of(uplinkMsg()), true);

        // sendCloudEvents returns this future and its caller blocks on get(), so leaving it unset would
        // wedge uplink processing permanently. true means "interrupted", i.e. retry, do not commit.
        assertThat(result.isDone()).as("caller's future must never be left uncompleted").isTrue();
        assertThat(result.get()).isTrue();
    }

    @Test
    void processMsgPackOnShutDownUplinkExecutorReportsPackAsInterrupted() throws Exception {
        ExecutorService shutDownExecutor = Executors.newSingleThreadExecutor();
        shutDownExecutor.shutdownNow();
        SettableFuture<Boolean> result = SettableFuture.create();
        ReflectionTestUtils.setField(sender, "sendUplinkFutureResult", result);
        ReflectionTestUtils.setField(sender, "uplinkExecutor", shutDownExecutor);

        ReflectionTestUtils.invokeMethod(sender, "processMsgPack", List.of(uplinkMsg()), true);

        assertThat(result.isDone()).as("rejected submission must complete the caller's future").isTrue();
        assertThat(result.get()).isTrue();
    }

    private static UplinkMsg uplinkMsg() {
        return UplinkMsg.newBuilder().setUplinkMsgId(1).build();
    }

}
