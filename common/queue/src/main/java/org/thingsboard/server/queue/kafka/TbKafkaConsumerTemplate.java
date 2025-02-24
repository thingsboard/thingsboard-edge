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
package org.thingsboard.server.queue.kafka;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.util.StopWatch;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TbKafkaConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<ConsumerRecord<String, byte[]>, T> {

    private final TbQueueAdmin admin;
    private final KafkaConsumer<String, byte[]> consumer;
    private final TbKafkaDecoder<T> decoder;

    private final TbKafkaConsumerStatsService statsService;
    private final String groupId;

    private final boolean readFromBeginning; // reset offset to beginning
    private final boolean stopWhenRead; // stop consuming when reached end offset remembered on start
    private int readCount;
    private Map<Integer, Long> endOffsets; // needed if stopWhenRead is true

    private boolean partitionsAssigned = false;

    @Builder
    private TbKafkaConsumerTemplate(TbKafkaSettings settings, TbKafkaDecoder<T> decoder,
                                    String clientId, String groupId, String topic,
                                    TbQueueAdmin admin, TbKafkaConsumerStatsService statsService,
                                    boolean readFromBeginning, boolean stopWhenRead) {
        super(topic);
        Properties props = settings.toConsumerProps(topic);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        if (groupId != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }

        this.statsService = statsService;
        this.groupId = groupId;

        if (statsService != null) {
            statsService.registerClientGroup(groupId);
        }

        this.admin = admin;
        this.consumer = new KafkaConsumer<>(props);
        this.decoder = decoder;
        this.readFromBeginning = readFromBeginning;
        this.stopWhenRead = stopWhenRead;
    }

    @Override
    protected void doSubscribe(Set<TopicPartitionInfo> partitions) {
        Map<String, List<Integer>> topics;
        if (partitions == null) {
            topics = Collections.emptyMap();
        } else {
            topics = new HashMap<>();
            partitions.forEach(tpi -> {
                if (tpi.isUseInternalPartition()) {
                    topics.computeIfAbsent(tpi.getFullTopicName(), t -> new ArrayList<>()).add(tpi.getPartition().get());
                } else {
                    topics.put(tpi.getFullTopicName(), null);
                }
            });
        }
        if (!topics.isEmpty()) {
            topics.keySet().forEach(admin::createTopicIfNotExists);
            List<String> toSubscribe = new ArrayList<>();
            topics.forEach((topic, kafkaPartitions) -> {
                if (kafkaPartitions == null) {
                    toSubscribe.add(topic);
                } else {
                    consumer.assign(kafkaPartitions.stream()
                            .map(partition -> new TopicPartition(topic, partition))
                            .toList());
                    partitionsAssigned = true;
                    onPartitionsAssigned();
                }
            });
            if (!toSubscribe.isEmpty()) {
                consumer.subscribe(toSubscribe);
            }
            if (readFromBeginning) {
                consumer.seekToBeginning(Collections.emptySet()); // for all assigned partitions
            }
        } else {
            log.info("unsubscribe due to empty topic list");
            consumer.unsubscribe();
        }
    }

    @Override
    protected List<ConsumerRecord<String, byte[]>> doPoll(long durationInMillis) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        log.trace("poll topic {} maxDuration {}", getTopic(), durationInMillis);

        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(durationInMillis));
        if (!partitionsAssigned) {
            if (readFromBeginning) {
                consumer.seekToBeginning(Collections.emptySet());
            }
            partitionsAssigned = true;
            onPartitionsAssigned();
        }

        stopWatch.stop();
        log.trace("poll topic {} took {}ms", getTopic(), stopWatch.getTotalTimeMillis());

        List<ConsumerRecord<String, byte[]>> recordList;
        if (records.isEmpty()) {
            recordList = Collections.emptyList();
        } else {
            recordList = new ArrayList<>(256);
            records.forEach(record -> {
                recordList.add(record);
                if (stopWhenRead) {
                    readCount++;
                    int partition = record.partition();
                    Long endOffset = endOffsets.get(partition);
                    if (endOffset == null) {
                        log.warn("End offset not found for {} [{}]", record.topic(), partition);
                        return;
                    }
                    log.trace("[{}-{}] Got record offset {}, expected end offset: {}", record.topic(), partition, record.offset(), endOffset - 1);
                    if (record.offset() >= endOffset - 1) {
                        endOffsets.remove(partition);
                    }
                }
            });
        }
        if (stopWhenRead && endOffsets.isEmpty()) {
            log.info("Finished reading {}, processed {} messages", partitions, readCount);
            stop();
        }
        return recordList;
    }

    private void onPartitionsAssigned() {
        if (stopWhenRead) {
            endOffsets = consumer.endOffsets(consumer.assignment()).entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .collect(Collectors.toMap(entry -> entry.getKey().partition(), Map.Entry::getValue));
        }
    }

    @Override
    public T decode(ConsumerRecord<String, byte[]> record) throws IOException {
        return decoder.decode(new KafkaTbQueueMsg(record));
    }

    @Override
    protected void doCommit() {
        consumer.commitSync();
    }

    @Override
    protected void doUnsubscribe() {
        if (consumer != null) {
            consumer.unsubscribe();
            consumer.close();
        }
        if (statsService != null) {
            statsService.unregisterClientGroup(groupId);
        }
    }

    @Override
    public boolean isLongPollingSupported() {
        return true;
    }

}
