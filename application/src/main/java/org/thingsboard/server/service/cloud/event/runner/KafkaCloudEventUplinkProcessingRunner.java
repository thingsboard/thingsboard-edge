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

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCloudEventMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.provider.TbCloudEventQueueFactory;
import org.thingsboard.server.queue.settings.TbQueueCloudEventSettings;
import org.thingsboard.server.queue.settings.TbQueueCloudEventTSSettings;
import org.thingsboard.server.service.cloud.event.sender.GrpcCloudEventUplinkSender;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaCloudEventUplinkProcessingRunner implements CloudEventUplinkProcessingRunner {

    private final TbQueueCloudEventTSSettings tbQueueCloudEventTSSettings;
    private final TbQueueCloudEventSettings tbQueueCloudEventSettings;
    private final CloudEventStorageSettings cloudEventStorageSettings;
    private final TbCloudEventQueueFactory tbCloudEventQueueProvider;
    private final GrpcCloudEventUplinkSender cloudEventUplinkSender;
    private final EdgeInfoHolder edgeInfo;

    private QueueConsumerManager<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> consumer;
    private QueueConsumerManager<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> tsConsumer;

    private ExecutorService consumerExecutor;
    private ExecutorService tsConsumerExecutor;

    @Override
    public void init() {
        if (consumer == null) {
            this.consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("cloud-event-consumer"));
            this.consumer = QueueConsumerManager.<TbProtoQueueMsg<ToCloudEventMsg>>builder()
                    .name("TB Cloud Events")
                    .msgPackProcessor(this::processUplinkMessages)
                    .pollInterval(tbQueueCloudEventSettings.getPollInterval())
                    .consumerCreator(tbCloudEventQueueProvider::createCloudEventMsgConsumer)
                    .consumerExecutor(consumerExecutor)
                    .threadPrefix("cloud-events")
                    .build();
            consumer.subscribe();
            consumer.launch();
        }
        if (tsConsumer == null) {
            this.tsConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ts-cloud-event-consumer"));
            this.tsConsumer = QueueConsumerManager.<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>>builder()
                    .name("TB TS Cloud Events")
                    .msgPackProcessor(this::processTsUplinkMessages)
                    .pollInterval(tbQueueCloudEventTSSettings.getPollInterval())
                    .consumerCreator(tbCloudEventQueueProvider::createCloudEventTSMsgConsumer)
                    .consumerExecutor(tsConsumerExecutor)
                    .threadPrefix("ts-cloud-events")
                    .build();
            tsConsumer.subscribe();
            tsConsumer.launch();
        }
    }

    @Override
    @PreDestroy
    public void shutdown() {
        if (consumer != null) {
            consumer.stop();
            consumer = null;
            consumerExecutor.shutdown();
        }

        if (tsConsumer != null) {
            tsConsumer.stop();
            tsConsumer = null;
            tsConsumerExecutor.shutdown();
        }
    }

    private void processUplinkMessages(List<TbProtoQueueMsg<ToCloudEventMsg>> msgs,
                                       TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> consumer) {
        boolean isProcessed = false;
        int attempt = 1;
        do {
            log.trace("[{}] Trying to process general uplink messages, attempt: {}", edgeInfo.getTenantId(), attempt++);

            if (canProcessGeneralMessages()) {
                edgeInfo.setGeneralProcessInProgress(true);
                isProcessed = doProcessMessages(msgs, consumer, true);
                edgeInfo.setGeneralProcessInProgress(false);
            } else {
                log.debug("[{}] Waiting: initialized={}, syncInProgress={}", edgeInfo.getTenantId(), edgeInfo.isInitialized(), edgeInfo.isSyncInProgress());
            }

            if (!isProcessed) {
                sleep();
            }
        } while (!isProcessed);
    }

    private void processTsUplinkMessages(List<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> msgs,
                                         TbQueueConsumer<TbProtoQueueMsg<ToCloudEventMsg>> consumer) {
        boolean isProcessed = false;

        do {
            log.trace("[{}] Trying to process TS uplink messages", edgeInfo.getTenantId());

            if (canProcessTsMessages()) {
                isProcessed = doProcessMessages(msgs, consumer, false);
            } else {
                log.debug("[{}] Waiting: initialized={}, syncInProgress={}, generalInProgress={}",
                        edgeInfo.getTenantId(), edgeInfo.isInitialized(), edgeInfo.isSyncInProgress(), edgeInfo.isGeneralProcessInProgress());
            }

            if (!isProcessed) {
                sleep();
            }
        } while (!isProcessed);
    }

    private void sleep() {
        try {
            Thread.sleep(cloudEventStorageSettings.getNoRecordsSleepInterval());
        } catch (InterruptedException interruptedException) {
            log.trace("Interrupted while waiting to retry uplink processing", interruptedException);
        }
    }

    private boolean doProcessMessages(List<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> msgs,
                                      TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> consumer,
                                      boolean isGeneralMsg) {
        List<CloudEvent> cloudEvents = convertQueueMsgsToCloudEvents(msgs);
        boolean isInterrupted;
        try {
            isInterrupted = cloudEventUplinkSender.sendCloudEvents(cloudEvents, isGeneralMsg).get();
        } catch (Exception e) {
            log.error("Failed to process all uplink messages", e);
            return false;
        }
        if (isInterrupted) {
            log.warn("[{}] Send uplink messages task was interrupted", edgeInfo.getTenantId());
            return false;
        } else {
            consumer.commit();
            log.trace("[{}] Successfully processed {} uplink messages (type={})", edgeInfo.getTenantId(), cloudEvents.size(), isGeneralMsg ? "GENERAL" : "TS");
            return true;
        }
    }

    private List<CloudEvent> convertQueueMsgsToCloudEvents(List<TbProtoQueueMsg<ToCloudEventMsg>> msgs) {
        return msgs.stream()
                .map(msg -> ProtoUtils.fromProto(msg.getValue().getCloudEventMsg()))
                .toList();
    }

    private boolean canProcessTsMessages() {
        return edgeInfo.isInitialized() && !edgeInfo.isSyncInProgress() && !edgeInfo.isGeneralProcessInProgress();
    }

    private boolean canProcessGeneralMessages() {
        return edgeInfo.isInitialized() && !edgeInfo.isSyncInProgress();
    }

}
