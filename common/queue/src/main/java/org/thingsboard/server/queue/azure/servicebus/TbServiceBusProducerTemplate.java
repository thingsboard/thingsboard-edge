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
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class TbServiceBusProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    private final String defaultTopic;
    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final TbServiceBusSettings serviceBusSettings;
    private final Map<String, QueueClient> clients = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    public TbServiceBusProducerTemplate(TbQueueAdmin admin, TbServiceBusSettings serviceBusSettings, String defaultTopic) {
        this.admin = admin;
        this.defaultTopic = defaultTopic;
        this.serviceBusSettings = serviceBusSettings;
        executorService = Executors.newCachedThreadPool();
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
        IMessage message = new Message(gson.toJson(new DefaultTbQueueMsg(msg)));
        CompletableFuture<Void> future = getClient(tpi.getFullTopicName()).sendAsync(message);
        future.whenCompleteAsync((success, err) -> {
            if (err != null) {
                callback.onFailure(err);
            } else {
                callback.onSuccess(null);
            }
        }, executorService);
    }

    @Override
    public void stop() {
        clients.forEach((t, client) -> {
            try {
                client.close();
            } catch (ServiceBusException e) {
                log.error("Failed to close QueueClient.", e);
            }
        });

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private QueueClient getClient(String topic) {
        return clients.computeIfAbsent(topic, k -> {
            admin.createTopicIfNotExists(topic);
            ConnectionStringBuilder builder =
                    new ConnectionStringBuilder(
                            serviceBusSettings.getNamespaceName(),
                            topic,
                            serviceBusSettings.getSasKeyName(),
                            serviceBusSettings.getSasKey());
            try {
                return new QueueClient(builder, ReceiveMode.PEEKLOCK);
            } catch (InterruptedException | ServiceBusException e) {
                log.error("Failed to create new client for the Queue: [{}]", topic, e);
                throw new RuntimeException("Failed to create new client for the Queue", e);
            }
        });
    }
}
