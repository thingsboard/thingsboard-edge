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
package org.thingsboard.server.queue.pubsub;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class TbPubSubConsumerTemplate<T extends TbQueueMsg> implements TbQueueConsumer<T> {

    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final String topic;
    private final TbQueueMsgDecoder<T> decoder;
    private final TbPubSubSettings pubSubSettings;

    private volatile boolean subscribed;
    private volatile Set<TopicPartitionInfo> partitions;
    private volatile Set<String> subscriptionNames;
    private final List<AcknowledgeRequest> acknowledgeRequests = new CopyOnWriteArrayList<>();

    private ExecutorService consumerExecutor;
    private final SubscriberStub subscriber;
    private volatile boolean stopped;

    private volatile int messagesPerTopic;

    public TbPubSubConsumerTemplate(TbQueueAdmin admin, TbPubSubSettings pubSubSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        this.admin = admin;
        this.pubSubSettings = pubSubSettings;
        this.topic = topic;
        this.decoder = decoder;

        try {
            SubscriberStubSettings subscriberStubSettings =
                    SubscriberStubSettings.newBuilder()
                            .setCredentialsProvider(pubSubSettings.getCredentialsProvider())
                            .setTransportChannelProvider(
                                    SubscriberStubSettings.defaultGrpcTransportProviderBuilder()
                                            .setMaxInboundMessageSize(pubSubSettings.getMaxMsgSize())
                                            .build())
                            .build();

            this.subscriber = GrpcSubscriberStub.create(subscriberStubSettings);
        } catch (IOException e) {
            log.error("Failed to create subscriber.", e);
            throw new RuntimeException("Failed to create subscriber.", e);
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
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }

        if (subscriber != null) {
            subscriber.close();
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
                subscriptionNames = partitions.stream().map(TopicPartitionInfo::getFullTopicName).collect(Collectors.toSet());
                subscriptionNames.forEach(admin::createTopicIfNotExists);
                consumerExecutor = Executors.newFixedThreadPool(subscriptionNames.size());
                messagesPerTopic = pubSubSettings.getMaxMessages() / subscriptionNames.size();
                subscribed = true;
            }
            List<ReceivedMessage> messages;
            try {
                messages = receiveMessages();
                if (!messages.isEmpty()) {
                    List<T> result = new ArrayList<>();
                    messages.forEach(msg -> {
                        try {
                            result.add(decode(msg.getMessage()));
                        } catch (InvalidProtocolBufferException e) {
                            log.error("Failed decode record: [{}]", msg);
                        }
                    });
                    return result;
                }
            } catch (ExecutionException | InterruptedException e) {
                if (stopped) {
                    log.info("[{}] Pub/Sub consumer is stopped.", topic);
                } else {
                    log.error("Failed to receive messages", e);
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void commit() {
        acknowledgeRequests.forEach(subscriber.acknowledgeCallable()::futureCall);
        acknowledgeRequests.clear();
    }

    private List<ReceivedMessage> receiveMessages() throws ExecutionException, InterruptedException {
        List<ApiFuture<List<ReceivedMessage>>> result = subscriptionNames.stream().map(subscriptionId -> {
            String subscriptionName = ProjectSubscriptionName.format(pubSubSettings.getProjectId(), subscriptionId);
            PullRequest pullRequest =
                    PullRequest.newBuilder()
                            .setMaxMessages(messagesPerTopic)
                            .setReturnImmediately(false) // return immediately if messages are not available
                            .setSubscription(subscriptionName)
                            .build();

            ApiFuture<PullResponse> pullResponseApiFuture = subscriber.pullCallable().futureCall(pullRequest);

            return ApiFutures.transform(pullResponseApiFuture, pullResponse -> {
                if (pullResponse != null && !pullResponse.getReceivedMessagesList().isEmpty()) {
                    List<String> ackIds = new ArrayList<>();
                    for (ReceivedMessage message : pullResponse.getReceivedMessagesList()) {
                        ackIds.add(message.getAckId());
                    }
                    AcknowledgeRequest acknowledgeRequest =
                            AcknowledgeRequest.newBuilder()
                                    .setSubscription(subscriptionName)
                                    .addAllAckIds(ackIds)
                                    .build();

                    acknowledgeRequests.add(acknowledgeRequest);
                    return pullResponse.getReceivedMessagesList();
                }
                return null;
            }, consumerExecutor);

        }).collect(Collectors.toList());

        ApiFuture<List<ReceivedMessage>> transform = ApiFutures.transform(ApiFutures.allAsList(result), listMessages -> {
            if (!CollectionUtils.isEmpty(listMessages)) {
                return listMessages.stream().filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }, consumerExecutor);

        return transform.get();
    }

    public T decode(PubsubMessage message) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(message.getData().toStringUtf8(), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

}
