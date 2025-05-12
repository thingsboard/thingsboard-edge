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
package org.thingsboard.server.edqs.state;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.edqs.processor.EdqsProcessor;
import org.thingsboard.server.edqs.processor.EdqsProducer;
import org.thingsboard.server.edqs.util.VersionsStore;
import org.thingsboard.server.gen.transport.TransportProtos.EdqsEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.common.state.KafkaQueueStateService;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.edqs.EdqsExecutors;
import org.thingsboard.server.queue.edqs.KafkaEdqsComponent;
import org.thingsboard.server.queue.edqs.KafkaEdqsQueueFactory;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@KafkaEdqsComponent
@Slf4j
public class KafkaEdqsStateService implements EdqsStateService {

    private final EdqsConfig config;
    private final EdqsPartitionService partitionService;
    private final KafkaEdqsQueueFactory queueFactory;
    private final DiscoveryService discoveryService;
    private final EdqsExecutors edqsExecutors;
    @Autowired
    @Lazy
    private EdqsProcessor edqsProcessor;

    private PartitionedQueueConsumerManager<TbProtoQueueMsg<ToEdqsMsg>> stateConsumer;
    private KafkaQueueStateService<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<ToEdqsMsg>> queueStateService;
    private QueueConsumerManager<TbProtoQueueMsg<ToEdqsMsg>> eventsToBackupConsumer;
    private EdqsProducer stateProducer;

    private final VersionsStore versionsStore = new VersionsStore();
    private final AtomicInteger stateReadCount = new AtomicInteger();
    private final AtomicInteger eventsReadCount = new AtomicInteger();

    private boolean ready = false;

    @Override
    public void init(PartitionedQueueConsumerManager<TbProtoQueueMsg<ToEdqsMsg>> eventConsumer, List<PartitionedQueueConsumerManager<?>> otherConsumers) {
        TbKafkaAdmin queueAdmin = queueFactory.getEdqsQueueAdmin();
        stateConsumer = PartitionedQueueConsumerManager.<TbProtoQueueMsg<ToEdqsMsg>>create()
                .queueKey(new QueueKey(ServiceType.EDQS, config.getStateTopic()))
                .topic(config.getStateTopic())
                .pollInterval(config.getPollInterval())
                .msgPackProcessor((msgs, consumer, config) -> {
                    for (TbProtoQueueMsg<ToEdqsMsg> queueMsg : msgs) {
                        try {
                            ToEdqsMsg msg = queueMsg.getValue();
                            edqsProcessor.process(msg, false);
                            if (stateReadCount.incrementAndGet() % 100000 == 0) {
                                log.info("[state] Processed {} msgs", stateReadCount.get());
                            }
                        } catch (Exception e) {
                            log.error("Failed to process message: {}", queueMsg, e);
                        }
                    }
                    consumer.commit();
                })
                .consumerCreator((config, tpi) -> queueFactory.createEdqsStateConsumer())
                .queueAdmin(queueAdmin)
                .consumerExecutor(edqsExecutors.getConsumersExecutor())
                .taskExecutor(edqsExecutors.getConsumerTaskExecutor())
                .scheduler(edqsExecutors.getScheduler())
                .uncaughtErrorHandler(edqsProcessor.getErrorHandler())
                .build();

        TbKafkaConsumerTemplate<TbProtoQueueMsg<ToEdqsMsg>> eventsToBackupKafkaConsumer = queueFactory.createEdqsEventsToBackupConsumer();
        eventsToBackupConsumer = QueueConsumerManager.<TbProtoQueueMsg<ToEdqsMsg>>builder()
                .name("edqs-events-to-backup-consumer")
                .pollInterval(config.getPollInterval())
                .msgPackProcessor((msgs, consumer) -> {
                    for (TbProtoQueueMsg<ToEdqsMsg> queueMsg : msgs) {
                        if (consumer.isStopped()) {
                            return;
                        }
                        try {
                            ToEdqsMsg msg = queueMsg.getValue();
                            log.trace("Processing message: {}", msg);

                            if (msg.hasEventMsg()) {
                                EdqsEventMsg eventMsg = msg.getEventMsg();
                                String key = eventMsg.getKey();
                                int count = eventsReadCount.incrementAndGet();
                                if (count % 100000 == 0) {
                                    log.info("[events-to-backup] Processed {} msgs", count);
                                }
                                if (eventMsg.hasVersion()) {
                                    if (!versionsStore.isNew(key, eventMsg.getVersion())) {
                                        continue;
                                    }
                                }

                                TenantId tenantId = getTenantId(msg);
                                ObjectType objectType = ObjectType.valueOf(eventMsg.getObjectType());
                                EdqsEventType eventType = EdqsEventType.valueOf(eventMsg.getEventType());
                                log.trace("[{}] Saving to backup [{}] [{}] [{}]", tenantId, objectType, eventType, key);
                                stateProducer.send(tenantId, objectType, key, msg);
                            }
                        } catch (Throwable t) {
                            log.error("Failed to process message: {}", queueMsg, t);
                        }
                    }
                    consumer.commit();
                })
                .consumerCreator(() -> eventsToBackupKafkaConsumer)
                .consumerExecutor(edqsExecutors.getConsumersExecutor())
                .threadPrefix("edqs-events-to-backup")
                .build();

        stateProducer = EdqsProducer.builder()
                .producer(queueFactory.createEdqsStateProducer())
                .partitionService(partitionService)
                .build();

        queueStateService = KafkaQueueStateService.<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<ToEdqsMsg>>builder()
                .eventConsumer(eventConsumer)
                .stateConsumer(stateConsumer)
                .otherConsumers(otherConsumers)
                .eventsStartOffsetsProvider(() -> {
                    // taking start offsets for events topics from the events-to-backup consumer group,
                    // since eventConsumer doesn't use consumer group management and thus offset tracking
                    // (because we need to be able to consume the same topic-partition by multiple instances)
                    Map<String, Long> offsets = new HashMap<>();
                    try {
                        queueAdmin.getConsumerGroupOffsets(eventsToBackupKafkaConsumer.getGroupId())
                                .forEach((topicPartition, offsetAndMetadata) -> {
                                    offsets.put(topicPartition.topic(), offsetAndMetadata.offset());
                                });
                    } catch (Exception e) {
                        log.error("Failed to get consumer group offsets for {}", eventsToBackupKafkaConsumer.getGroupId(), e);
                    }
                    return offsets;
                })
                .build();
    }

    @Override
    public void process(Set<TopicPartitionInfo> partitions) {
        if (queueStateService.getPartitions().isEmpty()) {
            Set<TopicPartitionInfo> allPartitions = IntStream.range(0, config.getPartitions())
                    .mapToObj(partition -> TopicPartitionInfo.builder()
                            .topic(config.getEventsTopic())
                            .partition(partition)
                            .build())
                    .collect(Collectors.toSet());
            eventsToBackupConsumer.subscribe(allPartitions);
            eventsToBackupConsumer.launch();
        }
        queueStateService.update(new QueueKey(ServiceType.EDQS), partitions, () -> {
            ready = true;
            discoveryService.setReady(true);
        });
    }

    @Override
    public void save(TenantId tenantId, ObjectType type, String key, EdqsEventType eventType, ToEdqsMsg msg) {
        // do nothing here, backup is done by events consumer
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    private TenantId getTenantId(ToEdqsMsg edqsMsg) {
        return TenantId.fromUUID(new UUID(edqsMsg.getTenantIdMSB(), edqsMsg.getTenantIdLSB()));
    }

    @Override
    public void stop() {
        stateConsumer.stop();
        stateConsumer.awaitStop();
        eventsToBackupConsumer.stop();
        stateProducer.stop();
    }

}
