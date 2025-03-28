/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
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
            consumerExecutor.shutdown();
            consumer = null;
        }

        if (tsConsumer != null) {
            tsConsumer.stop();
            tsConsumerExecutor.shutdown();
            tsConsumer = null;
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
