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

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaFuture;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashvayka on 24.09.18.
 */
public class TBKafkaAdmin {

    AdminClient client;

    public TBKafkaAdmin(TbKafkaSettings settings) {
        client = AdminClient.create(settings.toProps());
    }

    public void waitForTopic(String topic, long timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException {
        synchronized (this) {
            long timeoutExpiredMs = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);
            while (!topicExists(topic)) {
                long waitMs = timeoutExpiredMs - System.currentTimeMillis();
                if (waitMs <= 0) {
                    throw new TimeoutException("Timeout occurred while waiting for topic [" + topic + "] to be available!");
                } else {
                    wait(1000);
                }
            }
        }
    }

    public CreateTopicsResult createTopic(NewTopic topic){
        return client.createTopics(Collections.singletonList(topic));
    }

    private boolean topicExists(String topic) throws InterruptedException {
        KafkaFuture<TopicDescription> topicDescriptionFuture = client.describeTopics(Collections.singleton(topic)).values().get(topic);
        try {
            topicDescriptionFuture.get();
            return true;
        } catch (ExecutionException e) {
            return false;
        }
    }

}
