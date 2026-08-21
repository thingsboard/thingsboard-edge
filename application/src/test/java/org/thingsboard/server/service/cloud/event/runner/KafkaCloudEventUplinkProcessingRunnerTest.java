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
package org.thingsboard.server.service.cloud.event.runner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.gen.transport.TransportProtos.ToCloudEventMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCloudEventQueueFactory;
import org.thingsboard.server.queue.settings.TbQueueCloudEventSettings;
import org.thingsboard.server.queue.settings.TbQueueCloudEventTSSettings;
import org.thingsboard.server.service.cloud.event.sender.GrpcCloudEventUplinkSender;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KafkaCloudEventUplinkProcessingRunnerTest {

    private static final Duration LOOP_EXIT_TIMEOUT = Duration.ofSeconds(5);

    @Mock
    private TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> consumer;
    @Mock
    private TbQueueCloudEventTSSettings tbQueueCloudEventTSSettings;
    @Mock
    private TbQueueCloudEventSettings tbQueueCloudEventSettings;
    @Mock
    private CloudEventStorageSettings cloudEventStorageSettings;
    @Mock
    private TbCloudEventQueueFactory tbCloudEventQueueProvider;
    @Mock
    private GrpcCloudEventUplinkSender cloudEventUplinkSender;
    @Mock
    private EdgeInfoHolder edgeInfo;

    private KafkaCloudEventUplinkProcessingRunner runner;

    @BeforeEach
    void setUp() {
        runner = new KafkaCloudEventUplinkProcessingRunner(tbQueueCloudEventTSSettings, tbQueueCloudEventSettings,
                cloudEventStorageSettings, tbCloudEventQueueProvider, cloudEventUplinkSender, edgeInfo);
        lenient().when(cloudEventStorageSettings.getNoRecordsSleepInterval()).thenReturn(1L);
    }

    // edgeInfo.isInitialized() stays false - the state left behind once the uplink executor is gone for good.
    // The batch can therefore never be processed, so the stop check is the only thing that can end the loop.
    // Without it the consumer thread spins until QueueConsumerManager.stop() gives up after 10 seconds.

    @Test
    void generalUplinkLoopExitsWhenConsumerIsStopped() {
        when(consumer.isStopped()).thenReturn(true);

        assertTimeoutPreemptively(LOOP_EXIT_TIMEOUT, () ->
                ReflectionTestUtils.invokeMethod(runner, "processUplinkMessages", List.of(), consumer));

        verify(consumer, never()).commit();
    }

    @Test
    void tsUplinkLoopExitsWhenConsumerIsStopped() {
        when(consumer.isStopped()).thenReturn(true);

        assertTimeoutPreemptively(LOOP_EXIT_TIMEOUT, () ->
                ReflectionTestUtils.invokeMethod(runner, "processTsUplinkMessages", List.of(), consumer));

        verify(consumer, never()).commit();
    }

}
