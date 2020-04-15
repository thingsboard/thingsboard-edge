/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TBKafkaConsumerTemplate<T extends TbQueueMsg> implements TbQueueConsumer<T> {

    private final TBKafkaAdmin admin;
    private final KafkaConsumer<String, byte[]> consumer;
    private final TbKafkaDecoder<T> decoder;
    private volatile boolean subscribed;
    private volatile Set<TopicPartitionInfo> partitions;
    private final Lock consumerLock;

    @Getter
    private final String topic;

    @Builder
    private TBKafkaConsumerTemplate(TbKafkaSettings settings, TbKafkaDecoder<T> decoder,
                                    String clientId, String groupId, String topic,
                                    boolean autoCommit, int autoCommitIntervalMs,
                                    int maxPollRecords) {
        Properties props = settings.toProps();
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        if (groupId != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitIntervalMs);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        if (maxPollRecords > 0) {
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        }
        this.admin = new TBKafkaAdmin(settings);
        this.consumer = new KafkaConsumer<>(props);
        this.decoder = decoder;
        this.topic = topic;
        this.consumerLock = new ReentrantLock();
    }

    @Override
    public void subscribe() {
        partitions = Collections.singleton(new TopicPartitionInfo(topic, null, null, true));
        subscribed = false;
    }

    @Override
    public void subscribe(Set<TopicPartitionInfo> partitions) {
        this.partitions = partitions;
        subscribed = false;
    }

    @Override
    public List<T> poll(long durationInMillis) {
        if (!subscribed && partitions == null) {
            try {
                Thread.sleep(durationInMillis);
            } catch (InterruptedException e) {
                log.debug("Failed to await subscription", e);
            }
        } else {
            try {
                consumerLock.lock();

                if (!subscribed) {
                    List<String> topicNames = partitions.stream().map(TopicPartitionInfo::getFullTopicName).collect(Collectors.toList());
                    topicNames.forEach(admin::createTopicIfNotExists);
                    consumer.subscribe(topicNames);
                    subscribed = true;
                }

                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(durationInMillis));
                if (records.count() > 0) {
                    List<T> result = new ArrayList<>();
                    records.forEach(record -> {
                        try {
                            result.add(decode(record));
                        } catch (IOException e) {
                            log.error("Failed decode record: [{}]", record);
                        }
                    });
                    return result;
                }
            } finally {
                consumerLock.unlock();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void commit() {
        try {
            consumerLock.lock();
            consumer.commitAsync();
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public void unsubscribe() {
        try {
            consumerLock.lock();
            if (consumer != null) {
                consumer.unsubscribe();
                consumer.close();
            }
        } finally {
            consumerLock.unlock();
        }
    }

    public T decode(ConsumerRecord<String, byte[]> record) throws IOException {
        return decoder.decode(new KafkaTbQueueMsg(record));
    }

}
