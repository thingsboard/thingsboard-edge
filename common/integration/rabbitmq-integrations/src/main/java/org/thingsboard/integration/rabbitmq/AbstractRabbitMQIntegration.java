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
import com.rabbitmq.client.ShutdownSignalException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
        loopExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-loop"));
        producerExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        this.ctx = params.getContext();
        rabbitMQConsumerConfiguration = getClientConfiguration(configuration, RabbitMQConsumerConfiguration.class);
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
                persistDebug(context, "Uplink", getDefaultUplinkContentType(), JacksonUtil.toString(msg.getMsg()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    @Override
    public void destroy() {
        stopped = true;
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (Exception e) {
                log.error("Failed to close Channel.", e);
            }
        }
        if (connection != null && connection.isOpen()) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Failed to close Connection.", e);
            }
        }
        loopExecutor.shutdownNow();
    }

    @Override
    protected void doCheckConnection(Integration integration, IntegrationContext ctx) throws ThingsboardException {
        context = ctx;
        this.configuration = integration;
        try {
            rabbitMQConsumerConfiguration = getClientConfiguration(configuration, RabbitMQConsumerConfiguration.class);
            queues = new ArrayList<>(Arrays.asList(rabbitMQConsumerConfiguration.getQueues().trim().split(",")));
            createConnection();
        } catch (Exception e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    protected List<GetResponse> doPoll() {
        List<GetResponse> result = queues.stream()
                .map(queue -> {
                    try {
                        return channel.basicGet(queue, true);
                    } catch (IOException | ShutdownSignalException exception) {
                        log.error("Channel was closed with the error: {}", exception.getMessage());
                        if (configuration.isDebugMode()) {
                            try {
                                persistDebug(context, "Uplink", getDefaultUplinkContentType(), "", "ERROR", exception);
                            } catch (Exception e) {
                                log.warn("[{}] Failed to persist debug message", this.configuration.getName(), e);
                            }
                        }
                    }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toList());
        return result;
    }

    protected void doCommit() {
        try {
            if (channel.isOpen()) {
                channel.basicAck(0, true);
            }
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

    protected void initConsumer() {
        rabbitMQConsumer = new DefaultConsumer(channel);
        rabbitMQLock.lock();
        try {
            routingKeys.forEach((topic) -> {
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
        if (!StringUtils.isEmpty(rabbitMQConsumerConfiguration.getVirtualHost())) {
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
            channel.queueDeclare(topic,
                    rabbitMQConsumerConfiguration.getDurable(),
                    rabbitMQConsumerConfiguration.getExclusive(),
                    rabbitMQConsumerConfiguration.getAutoDelete(),
                    arguments);
        } catch (IOException | ShutdownSignalException e) {
            log.error("Failed to bind queue: [{}]", topic);
            throw new RuntimeException(String.format("Failed to bind queue: [%s]", topic), e);
        }
    }
}
