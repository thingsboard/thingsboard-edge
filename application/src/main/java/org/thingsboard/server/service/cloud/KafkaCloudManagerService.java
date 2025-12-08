/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCloudEventQueueFactory;
import org.thingsboard.server.queue.settings.TbQueueCloudEventSettings;
import org.thingsboard.server.queue.settings.TbQueueCloudEventTSSettings;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@Primary
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaCloudManagerService extends BaseCloudManagerService {

    private QueueConsumerManager<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> consumer;
    private QueueConsumerManager<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> tsConsumer;

    private ExecutorService consumerExecutor;
    private ExecutorService tsConsumerExecutor;

    @Autowired
    private TbCloudEventQueueFactory tbCloudEventQueueProvider;

    @Autowired
    private TbQueueCloudEventTSSettings tbQueueCloudEventTSSettings;

    @Autowired
    private TbQueueCloudEventSettings tbQueueCloudEventSettings;

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (ServiceType.TB_CORE.equals(event.getServiceType())) {
            establishRpcConnection();
        }
    }

    @Override
    protected void launchUplinkProcessing() {
        if (consumer == null) {
            this.consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("cloud-event-consumer"));
            this.consumer = QueueConsumerManager.<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>>builder()
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
    protected void resetQueueOffset() {
    }

    @PreDestroy
    protected void onDestroy() throws InterruptedException {
        super.destroy();

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

    private void processUplinkMessages(List<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> msgs,
                                       TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> consumer) {
        boolean isProcessed = false;
        do {
            log.trace("[{}] Trying to process general uplink messages", tenantId);

            if (initialized && !syncInProgress) {
                isGeneralProcessInProgress = true;
                isProcessed = processMessages(msgs, consumer, true);
                isGeneralProcessInProgress = false;
            } else {
                log.debug("[{}] Waiting: initialized={}, syncInProgress={}", tenantId, initialized, syncInProgress);
            }

            if (!isProcessed) {
                sleep();
            }
        } while (!isProcessed);
    }

    private void processTsUplinkMessages(List<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> msgs,
                                         TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> consumer) {
        boolean isProcessed = false;

        do {
            log.trace("[{}] Trying to process TS uplink messages", tenantId);

            if (initialized && !syncInProgress && !isGeneralProcessInProgress) {
                isProcessed = processMessages(msgs, consumer, false);
            } else {
                log.debug("[{}] Waiting: initialized={}, syncInProgress={}, generalInProgress={}",
                        tenantId, initialized, syncInProgress, isGeneralProcessInProgress);
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

    private boolean processMessages(List<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> msgs,
                                    TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> consumer,
                                    boolean isGeneralMsg) {
        List<CloudEvent> cloudEvents = msgs.stream()
                .map(msg -> ProtoUtils.fromProto(msg.getValue().getCloudEventMsg()))
                .toList();

        boolean isInterrupted;
        try {
            isInterrupted = processCloudEvents(cloudEvents, isGeneralMsg).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to process all uplink messages", e);
            return false;
        }
        if (isInterrupted) {
            log.warn("[{}] Send uplink messages task was interrupted", tenantId);
            return false;
        } else {
            consumer.commit();
            log.trace("[{}] Successfully processed {} uplink messages (type={})", tenantId, cloudEvents.size(), isGeneralMsg ? "GENERAL" : "TS");
            return true;
        }
    }

}
