/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.queue.azure.servicebus;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.microsoft.azure.servicebus.TransactionContext;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.CoreMessageReceiver;
import com.microsoft.azure.servicebus.primitives.MessageWithDeliveryTag;
import com.microsoft.azure.servicebus.primitives.MessagingEntityType;
import com.microsoft.azure.servicebus.primitives.MessagingFactory;
import com.microsoft.azure.servicebus.primitives.SettleModePair;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TbServiceBusConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<MessageWithDeliveryTag, T> {
    private final TbQueueAdmin admin;
    private final TbQueueMsgDecoder<T> decoder;
    private final TbServiceBusSettings serviceBusSettings;

    private final Gson gson = new Gson();

    private Set<CoreMessageReceiver> receivers;
    private final Map<CoreMessageReceiver, Collection<MessageWithDeliveryTag>> pendingMessages = new ConcurrentHashMap<>();
    private volatile int messagesPerQueue;

    public TbServiceBusConsumerTemplate(TbQueueAdmin admin, TbServiceBusSettings serviceBusSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        super(topic);
        this.admin = admin;
        this.decoder = decoder;
        this.serviceBusSettings = serviceBusSettings;
    }

    @Override
    protected List<MessageWithDeliveryTag> doPoll(long durationInMillis) {
        List<CompletableFuture<Collection<MessageWithDeliveryTag>>> messageFutures =
                receivers.stream()
                        .map(receiver -> receiver
                                .receiveAsync(messagesPerQueue, Duration.ofMillis(durationInMillis))
                                .whenComplete((messages, err) -> {
                                    if (!CollectionUtils.isEmpty(messages)) {
                                        pendingMessages.put(receiver, messages);
                                    } else if (err != null) {
                                        log.error("Failed to receive messages.", err);
                                    }
                                }))
                        .collect(Collectors.toList());
        try {
            return fromList(messageFutures)
                    .get()
                    .stream()
                    .flatMap(messages -> CollectionUtils.isEmpty(messages) ? Stream.empty() : messages.stream())
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            if (stopped) {
                log.info("[{}] Service Bus consumer is stopped.", getTopic());
            } else {
                log.error("Failed to receive messages", e);
            }
            return Collections.emptyList();
        }
    }

    @Override
    protected void doSubscribe(List<String> topicNames) {
        createReceivers();
        messagesPerQueue = serviceBusSettings.getMaxMessages() / Math.max(partitions.size(), 1);
    }

    @Override
    protected void doCommit() {
        pendingMessages.forEach((receiver, msgs) ->
                msgs.forEach(msg -> receiver.completeMessageAsync(msg.getDeliveryTag(), TransactionContext.NULL_TXN)));
        pendingMessages.clear();
    }

    @Override
    protected void doUnsubscribe() {
        receivers.forEach(CoreMessageReceiver::closeAsync);
    }

    private void createReceivers() {
        List<CompletableFuture<CoreMessageReceiver>> receiverFutures = partitions.stream()
                .map(TopicPartitionInfo::getFullTopicName)
                .map(queue -> {
                    MessagingFactory factory;
                    try {
                        factory = MessagingFactory.createFromConnectionStringBuilder(createConnection(queue));
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Failed to create factory for the queue [{}]", queue);
                        throw new RuntimeException("Failed to create the factory", e);
                    }

                    return CoreMessageReceiver.create(factory, queue, queue, 0,
                            new SettleModePair(SenderSettleMode.UNSETTLED, ReceiverSettleMode.SECOND),
                            MessagingEntityType.QUEUE);
                }).collect(Collectors.toList());

        try {
            receivers = new HashSet<>(fromList(receiverFutures).get());
        } catch (InterruptedException | ExecutionException e) {
            if (stopped) {
                log.info("[{}] Service Bus consumer is stopped.", getTopic());
            } else {
                log.error("Failed to create receivers", e);
            }
        }
    }

    private ConnectionStringBuilder createConnection(String queue) {
        admin.createTopicIfNotExists(queue);
        return new ConnectionStringBuilder(
                serviceBusSettings.getNamespaceName(),
                queue,
                serviceBusSettings.getSasKeyName(),
                serviceBusSettings.getSasKey());
    }

    private <V> CompletableFuture<List<V>> fromList(List<CompletableFuture<V>> futures) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Collection<V>>[] arrayFuture = new CompletableFuture[futures.size()];
        futures.toArray(arrayFuture);

        return CompletableFuture
                .allOf(arrayFuture)
                .thenApply(v -> futures
                        .stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    @Override
    protected T decode(MessageWithDeliveryTag data) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(new String(((Data) data.getMessage().getBody()).getValue().getArray()), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

}
