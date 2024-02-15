/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.housekeeper;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.housekeeper.HousekeeperService;
import org.thingsboard.server.dao.housekeeper.data.HousekeeperTask;
import org.thingsboard.server.dao.housekeeper.data.HousekeeperTaskType;
import org.thingsboard.server.gen.transport.TransportProtos.HousekeeperTaskProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.housekeeper.processor.HousekeeperTaskProcessor;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@Slf4j
public class DefaultHousekeeperService implements HousekeeperService {

    private final Map<HousekeeperTaskType, HousekeeperTaskProcessor<?>> taskProcessors;

    private final TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> consumer;
    private final TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> producer;
    private final HousekeeperReprocessingService reprocessingService;
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper-consumer"));

    @Value("${queue.core.housekeeper.poll-interval-ms:10000}")
    private int pollInterval;

    private boolean stopped;

    public DefaultHousekeeperService(HousekeeperReprocessingService reprocessingService,
                                     TbCoreQueueFactory queueFactory,
                                     TbQueueProducerProvider producerProvider,
                                     DataDecodingEncodingService dataDecodingEncodingService,
                                     @Lazy List<HousekeeperTaskProcessor<?>> taskProcessors) {
        this.consumer = queueFactory.createHousekeeperMsgConsumer();
        this.producer = producerProvider.getHousekeeperMsgProducer();
        this.reprocessingService = reprocessingService;
        this.taskProcessors = taskProcessors.stream().collect(Collectors.toMap(HousekeeperTaskProcessor::getTaskType, p -> p));
        this.dataDecodingEncodingService = dataDecodingEncodingService;
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        consumer.subscribe();
        consumerExecutor.submit(() -> {
            while (!stopped && !consumer.isStopped()) {
                try {
                    List<TbProtoQueueMsg<ToHousekeeperServiceMsg>> msgs = consumer.poll(pollInterval);
                    if (msgs.isEmpty()) {
                        continue;
                    }

                    for (TbProtoQueueMsg<ToHousekeeperServiceMsg> msg : msgs) {
                        try {
                            processTask(msg.getValue());
                        } catch (Exception e) {
                            log.error("Message processing failed", e);
                        }
                    }
                    consumer.commit();
                } catch (Throwable t) {
                    if (!consumer.isStopped()) {
                        log.warn("Failed to process messages from queue", t);
                        try {
                            Thread.sleep(pollInterval);
                        } catch (InterruptedException interruptedException) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", interruptedException);
                        }
                    }
                }
            }
        });
        log.info("Started Housekeeper service");
    }

    @SuppressWarnings("unchecked")
    protected <T extends HousekeeperTask> void processTask(ToHousekeeperServiceMsg msg) {
        HousekeeperTask task = dataDecodingEncodingService.<HousekeeperTask>decode(msg.getTask().getValue().toByteArray()).get();
        HousekeeperTaskProcessor<T> taskProcessor = getTaskProcessor(task.getTaskType());

        log.info("[{}][{}][{}] Processing task: {}", task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(), task.getTaskType());
        try {
            taskProcessor.process((T) task);
        } catch (Exception e) {
            log.error("[{}][{}][{}] {} task processing failed, submitting for reprocessing (attempt {}): {}",
                    task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(),
                    task.getTaskType(), msg.getTask().getAttempt(), task, e);
            reprocessingService.submitForReprocessing(msg);
        }
    }

    @Override
    public void submitTask(HousekeeperTask task) {
        submitTask(UUID.randomUUID(), task);
    }

    @Override
    public void submitTask(UUID key, HousekeeperTask task) {
        TopicPartitionInfo tpi = TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build();
        producer.send(tpi, new TbProtoQueueMsg<>(key, ToHousekeeperServiceMsg.newBuilder()
                .setTask(HousekeeperTaskProto.newBuilder()
                        .setValue(ByteString.copyFrom(dataDecodingEncodingService.encode(task)))
                        .setTs(task.getTs())
                        .setAttempt(0)
                        .build())
                .build()), null);
        log.trace("[{}][{}][{}] Submitted task: {}", task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(), task.getTaskType());
    }

    @SuppressWarnings("unchecked")
    private <T extends HousekeeperTask> HousekeeperTaskProcessor<T> getTaskProcessor(HousekeeperTaskType taskType) {
        return Optional.ofNullable((HousekeeperTaskProcessor<T>) taskProcessors.get(taskType))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported task type " + taskType));
    }

    @PreDestroy
    private void stop() {
        log.info("Stopped Housekeeper service");
        stopped = true;
        consumer.unsubscribe();
        consumerExecutor.shutdownNow();
    }

}
