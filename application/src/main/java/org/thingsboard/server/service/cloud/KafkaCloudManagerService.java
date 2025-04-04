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

import java.util.ArrayList;
import java.util.List;
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

    private void processUplinkMessages(List<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> consumer) {
        log.trace("[{}] starting processing cloud events", tenantId);
        if (initialized) {
            isGeneralProcessInProgress = true;
            processMessages(msgs, consumer, true);
            isGeneralProcessInProgress = false;
        } else {
            sleep();
        }
    }

    private void processTsUplinkMessages(List<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> consumer) {
        if (initialized && !isGeneralProcessInProgress) {
            processMessages(msgs, consumer, false);
        } else {
            sleep();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(cloudEventStorageSettings.getNoRecordsSleepInterval());
        } catch (InterruptedException interruptedException) {
            log.trace("Failed to wait until the server has capacity to handle new requests", interruptedException);
        }
    }

    private void processMessages(List<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCloudEventMsg>> consumer, boolean isGeneralMsg) {
        List<CloudEvent> cloudEvents = new ArrayList<>();
        for (TbProtoQueueMsg<TransportProtos.ToCloudEventMsg> msg : msgs) {
            CloudEvent cloudEvent = ProtoUtils.fromProto(msg.getValue().getCloudEventMsg());
            cloudEvents.add(cloudEvent);
        }
        try {
            boolean isInterrupted = processCloudEvents(cloudEvents, isGeneralMsg).get();
            if (isInterrupted) {
                log.debug("[{}] Send uplink messages task was interrupted", tenantId);
            } else {
                consumer.commit();
            }
        } catch (Exception e) {
            log.error("Failed to process all uplink messages", e);
        }
    }

}
