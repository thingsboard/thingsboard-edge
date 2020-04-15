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
package org.thingsboard.server.queue.rabbitmq;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class TbRabbitMqConsumerTemplate<T extends TbQueueMsg> implements TbQueueConsumer<T> {

    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final String topic;
    private final TbQueueMsgDecoder<T> decoder;
    private final TbRabbitMqSettings rabbitMqSettings;
    private final Channel channel;
    private final Connection connection;

    private volatile Set<TopicPartitionInfo> partitions;
    private volatile boolean subscribed;
    private volatile Set<String> queues;
    private volatile boolean stopped;

    public TbRabbitMqConsumerTemplate(TbQueueAdmin admin, TbRabbitMqSettings rabbitMqSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        this.admin = admin;
        this.decoder = decoder;
        this.topic = topic;
        this.rabbitMqSettings = rabbitMqSettings;
        try {
            connection = rabbitMqSettings.getConnectionFactory().newConnection();
        } catch (IOException | TimeoutException e) {
            log.error("Failed to create connection.", e);
            throw new RuntimeException("Failed to create connection.", e);
        }

        try {
            channel = connection.createChannel();
        } catch (IOException e) {
            log.error("Failed to create chanel.", e);
            throw new RuntimeException("Failed to create chanel.", e);
        }
        stopped = false;
    }

    @Override
    public String getTopic() {
        return topic;
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
    public void unsubscribe() {
        stopped = true;
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.error("Failed to close the channel.");
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Failed to close the connection.");
            }
        }
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
            if (!subscribed) {
                queues = partitions.stream()
                        .map(TopicPartitionInfo::getFullTopicName)
                        .collect(Collectors.toSet());

                queues.forEach(admin::createTopicIfNotExists);
                subscribed = true;
            }

            List<T> result = queues.stream()
                    .map(queue -> {
                        try {
                            return channel.basicGet(queue, false);
                        } catch (IOException e) {
                            log.error("Failed to get messages from queue: [{}]", queue);
                            throw new RuntimeException("Failed to get messages from queue.", e);
                        }
                    }).filter(Objects::nonNull).map(message -> {
                        try {
                            return decode(message);
                        } catch (InvalidProtocolBufferException e) {
                            log.error("Failed to decode message: [{}].", message);
                            throw new RuntimeException("Failed to decode message.", e);
                        }
                    }).collect(Collectors.toList());
            if (result.size() > 0) {
                return result;
            }
        }
        try {
            Thread.sleep(durationInMillis);
        } catch (InterruptedException e) {
            if (!stopped) {
                log.error("Failed to wait.", e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void commit() {
        try {
            channel.basicAck(0, true);
        } catch (IOException e) {
            log.error("Failed to ack messages.", e);
        }
    }

    public T decode(GetResponse message) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(new String(message.getBody()), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }
}
