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

import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ListSubscriptionsRequest;
import com.google.pubsub.v1.ListTopicsRequest;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TbPubSubAdmin implements TbQueueAdmin {

    private final TbPubSubSettings pubSubSettings;
    private final SubscriptionAdminSettings subscriptionAdminSettings;
    private final TopicAdminSettings topicAdminSettings;
    private final Set<String> topicSet = ConcurrentHashMap.newKeySet();
    private final Set<String> subscriptionSet = ConcurrentHashMap.newKeySet();

    public TbPubSubAdmin(TbPubSubSettings pubSubSettings) {
        this.pubSubSettings = pubSubSettings;

        try {
            topicAdminSettings = TopicAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        } catch (IOException e) {
            log.error("Failed to create TopicAdminSettings");
            throw new RuntimeException("Failed to create TopicAdminSettings.");
        }

        try {
            subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        } catch (IOException e) {
            log.error("Failed to create SubscriptionAdminSettings");
            throw new RuntimeException("Failed to create SubscriptionAdminSettings.");
        }

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            ListTopicsRequest listTopicsRequest =
                    ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
            TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
            for (Topic topic : response.iterateAll()) {
                topicSet.add(topic.getName());
            }
        } catch (IOException e) {
            log.error("Failed to get topics.", e);
            throw new RuntimeException("Failed to get topics.", e);
        }

        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {

            ListSubscriptionsRequest listSubscriptionsRequest =
                    ListSubscriptionsRequest.newBuilder()
                            .setProject(ProjectName.of(pubSubSettings.getProjectId()).toString())
                            .build();
            SubscriptionAdminClient.ListSubscriptionsPagedResponse response =
                    subscriptionAdminClient.listSubscriptions(listSubscriptionsRequest);

            for (Subscription subscription : response.iterateAll()) {
                subscriptionSet.add(subscription.getName());
            }
        } catch (IOException e) {
            log.error("Failed to get subscriptions.", e);
            throw new RuntimeException("Failed to get subscriptions.", e);
        }
    }

    @Override
    public void createTopicIfNotExists(String partition) {
        ProjectTopicName topicName = ProjectTopicName.of(pubSubSettings.getProjectId(), partition);

        if (topicSet.contains(topicName.toString())) {
            createSubscriptionIfNotExists(partition, topicName);
            return;
        }

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            ListTopicsRequest listTopicsRequest =
                    ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
            TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
            for (Topic topic : response.iterateAll()) {
                if (topic.getName().contains(topicName.toString())) {
                    topicSet.add(topic.getName());
                    createSubscriptionIfNotExists(partition, topicName);
                    return;
                }
            }

            topicAdminClient.createTopic(topicName);
            topicSet.add(topicName.toString());
            log.info("Created new topic: [{}]", topicName.toString());
            createSubscriptionIfNotExists(partition, topicName);
        } catch (IOException e) {
            log.error("Failed to create topic: [{}].", topicName.toString(), e);
            throw new RuntimeException("Failed to create topic.", e);
        }
    }

    private void createSubscriptionIfNotExists(String partition, ProjectTopicName topicName) {
        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(pubSubSettings.getProjectId(), partition);

        if (subscriptionSet.contains(subscriptionName.toString())) {
            return;
        }

        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
            ListSubscriptionsRequest listSubscriptionsRequest =
                    ListSubscriptionsRequest.newBuilder()
                            .setProject(ProjectName.of(pubSubSettings.getProjectId()).toString())
                            .build();
            SubscriptionAdminClient.ListSubscriptionsPagedResponse response =
                    subscriptionAdminClient.listSubscriptions(listSubscriptionsRequest);

            for (Subscription subscription : response.iterateAll()) {
                if (subscription.getName().equals(subscriptionName.toString())) {
                    subscriptionSet.add(subscription.getName());
                    return;
                }
            }

            subscriptionAdminClient.createSubscription(
                    subscriptionName, topicName, PushConfig.getDefaultInstance(), pubSubSettings.getAckDeadline()).getName();
            subscriptionSet.add(subscriptionName.toString());
            log.info("Created new subscription: [{}]", subscriptionName.toString());
        } catch (IOException e) {
            log.error("Failed to create subscription: [{}].", subscriptionName.toString(), e);
            throw new RuntimeException("Failed to create subscription.", e);
        }
    }

}
