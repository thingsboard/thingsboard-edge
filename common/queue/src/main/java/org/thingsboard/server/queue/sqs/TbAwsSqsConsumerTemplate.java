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
package org.thingsboard.server.queue.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TbAwsSqsConsumerTemplate<T extends TbQueueMsg> implements TbQueueConsumer<T> {

    private static final int MAX_NUM_MSGS = 10;

    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final AmazonSQS sqsClient;
    private final String topic;
    private final TbQueueMsgDecoder<T> decoder;
    private final TbAwsSqsSettings sqsSettings;

    private final List<AwsSqsMsgWrapper> pendingMessages = new CopyOnWriteArrayList<>();
    private volatile Set<String> queueUrls;
    private volatile Set<TopicPartitionInfo> partitions;
    private ListeningExecutorService consumerExecutor;
    private volatile boolean subscribed;
    private volatile boolean stopped = false;

    public TbAwsSqsConsumerTemplate(TbQueueAdmin admin, TbAwsSqsSettings sqsSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        this.admin = admin;
        this.decoder = decoder;
        this.topic = topic;
        this.sqsSettings = sqsSettings;

        AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);

        this.sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credProvider)
                .withRegion(sqsSettings.getRegion())
                .build();
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

        if (sqsClient != null) {
            sqsClient.shutdown();
        }
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
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
                List<String> topicNames = partitions.stream().map(TopicPartitionInfo::getFullTopicName).collect(Collectors.toList());
                queueUrls = topicNames.stream().map(this::getQueueUrl).collect(Collectors.toSet());
                consumerExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(queueUrls.size() * sqsSettings.getThreadsPerTopic() + 1));
                subscribed = true;
            }

            if (!pendingMessages.isEmpty()) {
                log.warn("Present {} non committed messages.", pendingMessages.size());
                return Collections.emptyList();
            }

            List<ListenableFuture<List<Message>>> futureList = queueUrls
                    .stream()
                    .map(url -> poll(url, (int) TimeUnit.MILLISECONDS.toSeconds(durationInMillis)))
                    .collect(Collectors.toList());
            ListenableFuture<List<List<Message>>> futureResult = Futures.allAsList(futureList);
            try {
                return futureResult.get().stream()
                        .flatMap(List::stream)
                        .map(msg -> {
                            try {
                                return decode(msg);
                            } catch (IOException e) {
                                log.error("Failed to decode message: [{}]", msg);
                                return null;
                            }
                        }).filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } catch (InterruptedException | ExecutionException e) {
                if (stopped) {
                    log.info("[{}] Aws SQS consumer is stopped.", topic);
                } else {
                    log.error("Failed to pool messages.", e);
                }
            }
        }
        return Collections.emptyList();
    }

    private ListenableFuture<List<Message>> poll(String url, int waitTimeSeconds) {
        List<ListenableFuture<List<Message>>> result = new ArrayList<>();

        for (int i = 0; i < sqsSettings.getThreadsPerTopic(); i++) {
            result.add(consumerExecutor.submit(() -> {
                ReceiveMessageRequest request = new ReceiveMessageRequest();
                request
                        .withWaitTimeSeconds(waitTimeSeconds)
                        .withMessageAttributeNames("headers")
                        .withQueueUrl(url)
                        .withMaxNumberOfMessages(MAX_NUM_MSGS);
                return sqsClient.receiveMessage(request).getMessages();
            }));
        }
        return Futures.transform(Futures.allAsList(result), list -> {
            if (!CollectionUtils.isEmpty(list)) {
                return list.stream()
                        .flatMap(messageList -> {
                            if (!messageList.isEmpty()) {
                                this.pendingMessages.add(new AwsSqsMsgWrapper(url, messageList));
                                return messageList.stream();
                            }
                            return Stream.empty();
                        })
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }, consumerExecutor);
    }

    @Override
    public void commit() {
        pendingMessages.forEach(msg ->
                consumerExecutor.submit(() -> {
                    List<DeleteMessageBatchRequestEntry> entries = msg.getMessages()
                            .stream()
                            .map(message -> new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()))
                            .collect(Collectors.toList());
                    sqsClient.deleteMessageBatch(msg.getUrl(), entries);
                }));

        pendingMessages.clear();
    }

    public T decode(Message message) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(message.getBody(), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

    @Data
    private static class AwsSqsMsgWrapper {
        private final String url;
        private final List<Message> messages;

        public AwsSqsMsgWrapper(String url, List<Message> messages) {
            this.url = url;
            this.messages = messages;
        }
    }

    private String getQueueUrl(String topic) {
        admin.createTopicIfNotExists(topic);
        return sqsClient.getQueueUrl(topic.replaceAll("\\.", "_") + ".fifo").getQueueUrl();
    }
}
