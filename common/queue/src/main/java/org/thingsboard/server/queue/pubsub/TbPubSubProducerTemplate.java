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
package org.thingsboard.server.queue.pubsub;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TbPubSubProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {

    private final Gson gson = new Gson();

    private final String defaultTopic;
    private final TbQueueAdmin admin;
    private final TbPubSubSettings pubSubSettings;

    private final Map<String, Publisher> publisherMap = new ConcurrentHashMap<>();

    private final ExecutorService pubExecutor = Executors.newCachedThreadPool();

    public TbPubSubProducerTemplate(TbQueueAdmin admin, TbPubSubSettings pubSubSettings, String defaultTopic) {
        this.defaultTopic = defaultTopic;
        this.admin = admin;
        this.pubSubSettings = pubSubSettings;
    }

    @Override
    public void init() {

    }

    @Override
    public String getDefaultTopic() {
        return defaultTopic;
    }

    @Override
    public void send(TopicPartitionInfo tpi, T msg, TbQueueCallback callback) {
        PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder();
        pubsubMessageBuilder.setData(getMsg(msg));

        Publisher publisher = getOrCreatePublisher(tpi.getFullTopicName());
        ApiFuture<String> future = publisher.publish(pubsubMessageBuilder.build());

        ApiFutures.addCallback(future, new ApiFutureCallback<String>() {
            public void onSuccess(String messageId) {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }

            public void onFailure(Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        }, pubExecutor);
    }

    @Override
    public void stop() {
        publisherMap.forEach((k, v) -> {
            if (v != null) {
                try {
                    v.shutdown();
                    v.awaitTermination(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Failed to shutdown PubSub client during destroy()", e);
                }
            }
        });

        if (pubExecutor != null) {
            pubExecutor.shutdownNow();
        }
    }

    private ByteString getMsg(T msg) {
        String json = gson.toJson(new DefaultTbQueueMsg(msg));
        return ByteString.copyFrom(json.getBytes());
    }

    private Publisher getOrCreatePublisher(String topic) {
        if (publisherMap.containsKey(topic)) {
            return publisherMap.get(topic);
        } else {
            try {
                admin.createTopicIfNotExists(topic);
                ProjectTopicName topicName = ProjectTopicName.of(pubSubSettings.getProjectId(), topic);
                Publisher publisher = Publisher.newBuilder(topicName).setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
                publisherMap.put(topic, publisher);
                return publisher;
            } catch (IOException e) {
                log.error("Failed to create Publisher for the topic [{}].", topic, e);
                throw new RuntimeException("Failed to create Publisher for the topic.", e);
            }
        }

    }

}
