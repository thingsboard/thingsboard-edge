/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.kafka;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.header.Header;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TBKafkaProducerTemplate<T> {

    private final KafkaProducer<String, byte[]> producer;
    private final TbKafkaEncoder<T> encoder;

    @Builder.Default
    private TbKafkaEnricher<T> enricher = ((value, responseTopic, requestId) -> value);

    private final TbKafkaPartitioner<T> partitioner;
    private ConcurrentMap<String, List<PartitionInfo>> partitionInfoMap;
    @Getter
    private final String defaultTopic;

    @Getter
    private final TbKafkaSettings settings;

    @Builder
    private TBKafkaProducerTemplate(TbKafkaSettings settings, TbKafkaEncoder<T> encoder, TbKafkaEnricher<T> enricher,
                                    TbKafkaPartitioner<T> partitioner, String defaultTopic, String clientId) {
        Properties props = settings.toProps();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        if (!StringUtils.isEmpty(clientId)) {
            props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        }
        this.settings = settings;
        this.producer = new KafkaProducer<>(props);
        this.encoder = encoder;
        this.enricher = enricher;
        this.partitioner = partitioner;
        this.defaultTopic = defaultTopic;
    }

    public void init() {
        this.partitionInfoMap = new ConcurrentHashMap<>();
        if (!StringUtils.isEmpty(defaultTopic)) {
            try {
                TBKafkaAdmin admin = new TBKafkaAdmin(this.settings);
                admin.waitForTopic(defaultTopic, 30, TimeUnit.SECONDS);
                log.info("[{}] Topic exists.", defaultTopic);
            } catch (Exception e) {
                log.info("[{}] Failed to wait for topic: {}", defaultTopic, e.getMessage(), e);
                throw new RuntimeException(e);
            }
            //Maybe this should not be cached, but we don't plan to change size of partitions
            this.partitionInfoMap.putIfAbsent(defaultTopic, producer.partitionsFor(defaultTopic));
        }
    }

    T enrich(T value, String responseTopic, UUID requestId) {
        if (enricher != null) {
            return enricher.enrich(value, responseTopic, requestId);
        } else {
            return value;
        }
    }

    public Future<RecordMetadata> send(String key, T value, Callback callback) {
        return send(key, value, null, callback);
    }

    public Future<RecordMetadata> send(String key, T value, Iterable<Header> headers, Callback callback) {
        return send(key, value, null, headers, callback);
    }

    public Future<RecordMetadata> send(String key, T value, Long timestamp, Iterable<Header> headers, Callback callback) {
        if (!StringUtils.isEmpty(this.defaultTopic)) {
            return send(this.defaultTopic, key, value, timestamp, headers, callback);
        } else {
            throw new RuntimeException("Failed to send message! Default topic is not specified!");
        }
    }

    public Future<RecordMetadata> send(String topic, String key, T value, Iterable<Header> headers, Callback callback) {
        return send(topic, key, value, null, headers, callback);
    }

    public Future<RecordMetadata> send(String topic, String key, T value, Callback callback) {
        return send(topic, key, value, null, null, callback);
    }

    public Future<RecordMetadata> send(String topic, String key, T value, Long timestamp, Iterable<Header> headers, Callback callback) {
        byte[] data = encoder.encode(value);
        ProducerRecord<String, byte[]> record;
        Integer partition = getPartition(topic, key, value, data);
        record = new ProducerRecord<>(topic, partition, timestamp, key, data, headers);
        return producer.send(record, callback);
    }

    private Integer getPartition(String topic, String key, T value, byte[] data) {
        if (partitioner == null) {
            return null;
        } else {
            return partitioner.partition(topic, key, value, data, partitionInfoMap.computeIfAbsent(topic, producer::partitionsFor));
        }
    }
}
