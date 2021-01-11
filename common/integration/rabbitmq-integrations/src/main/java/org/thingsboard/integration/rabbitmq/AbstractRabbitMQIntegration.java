/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.rabbitmq;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractRabbitMQIntegration<T extends RabbitMQIntegrationMsg> extends AbstractIntegration<T> {

    private final Gson gson = new Gson();
    protected RabbitMQConsumerConfiguration rabbitMQConsumerConfiguration;
    protected ConnectionFactory connectionFactory;
    protected Connection connection;
    protected Consumer rabbitMQConsumer;
    protected Channel channel;
    protected IntegrationContext ctx;
    protected ExecutorService loopExecutor;
    protected ListeningExecutorService producerExecutor;
    protected List<String> queues;
    protected List<String> routingKeys;
    protected volatile boolean stopped = false;
    protected Lock rabbitMQLock = new ReentrantLock();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        loopExecutor = Executors.newSingleThreadExecutor();
        producerExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        this.ctx = params.getContext();
        rabbitMQConsumerConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("clientConfiguration")),
                RabbitMQConsumerConfiguration.class);
        routingKeys = new ArrayList<>(Arrays.asList(rabbitMQConsumerConfiguration.getRoutingKeys().trim().split(",")));
        queues = new ArrayList<>(Arrays.asList(rabbitMQConsumerConfiguration.getQueues().trim().split(",")));
        createConnection();
    }

    @Override
    public void process(T msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, "Uplink", getUplinkContentType(), mapper.writeValueAsString(msg.getMsg()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    @Override
    public void update(TbIntegrationInitParams params) throws Exception {
        destroy();
        init(params);
    }

    @Override
    public void destroy() {
        stopped = true;
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.error("Failed to close Chanel.", e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Failed to close Connection.", e);
            }
        }
    }

    protected List<GetResponse> doPoll() {
        List<GetResponse> result = queues.stream()
                .map(queue -> {
                    try {
                        return channel.basicGet(queue, false);
                    } catch (IOException e) {
                        log.error("Failed to get messages from queue: [{}]", queue);
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
        if (result.size() > 0) {
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    protected void doCommit() {
        try {
            channel.basicAck(0, true);
        } catch (IOException e) {
            log.error("Failed to ack messages.", e);
        }
    }

    public boolean send(IntegrationContext ctx, TbMsg msg) {
        AMQP.BasicProperties properties = new AMQP.BasicProperties();
        try {
            channel.basicPublish(rabbitMQConsumerConfiguration.getExchangeName(), rabbitMQConsumerConfiguration.getDownlinkTopic(), properties, gson.toJson(msg.getData()).getBytes());
            return true;
        } catch (IOException e) {
            log.error("Failed publish message: [{}].", msg, e);
        }
        return false;
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            processDownLinkMsg(context, msg);
        }
    }

    protected void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            if (doProcessDownLinkMsg(context, msg)) {
                integrationStatistics.incMessagesProcessed();
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            exception = e;
            status = "ERROR";
        }
        reportDownlinkError(context, msg, status, exception);
    }

    protected boolean doProcessDownLinkMsg(IntegrationContext context, TbMsg msg) {
        return send(context, msg);
    }

    protected void initConsumer(RabbitMQConsumerConfiguration configuration) {
        rabbitMQConsumer = new DefaultConsumer(channel);
        rabbitMQLock.lock();
        try {
            routingKeys.forEach( (topic) -> {
                createTopicIfNotExists(topic, null);
            });
        } finally {
            rabbitMQLock.unlock();
        }
        stopped = false;
    }

    protected void createConnection() {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMQConsumerConfiguration.getHost());
        connectionFactory.setPort(rabbitMQConsumerConfiguration.getPort());
        if (!StringUtils.isEmpty(rabbitMQConsumerConfiguration.getVirtualHost())){
            connectionFactory.setVirtualHost(rabbitMQConsumerConfiguration.getVirtualHost());
        }
        if (!StringUtils.isEmpty(rabbitMQConsumerConfiguration.getUsername())) {
            connectionFactory.setUsername(rabbitMQConsumerConfiguration.getUsername());
        }
        if (!StringUtils.isEmpty(rabbitMQConsumerConfiguration.getPassword())) {
            connectionFactory.setPassword(rabbitMQConsumerConfiguration.getPassword());
        }
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setConnectionTimeout(rabbitMQConsumerConfiguration.getConnectionTimeout());
        connectionFactory.setHandshakeTimeout(rabbitMQConsumerConfiguration.getHandshakeTimeout());


        try {
            connection = connectionFactory.newConnection();
        } catch (IOException | TimeoutException e) {
            log.error("Failed to create connection.", e);
            throw new RuntimeException("Failed to create connection.", e);
        }

        try {
            channel = connection.createChannel();
        } catch (IOException e) {
            log.error("Failed to create channel.", e);
            throw new RuntimeException("Failed to create channel.", e);
        }
        stopped = false;

    }

    protected abstract void doProcess(IntegrationContext context, T msg) throws Exception;

    public void createTopicIfNotExists(String topic, Map<String, Object> arguments) {
        try {
            channel.queueDeclare(topic, false, false, false, arguments);
        } catch (IOException e) {
            log.error("Failed to bind queue: [{}]", topic, e);
        }
    }
}
