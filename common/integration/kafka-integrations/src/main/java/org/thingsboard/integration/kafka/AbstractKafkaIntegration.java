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
package org.thingsboard.integration.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class AbstractKafkaIntegration<T extends KafkaIntegrationMsg> extends AbstractIntegration<T> {

    protected KafkaConsumerConfiguration kafkaConsumerConfiguration;
    protected Consumer<String, String> kafkaConsumer;
    protected IntegrationContext ctx;
    protected ExecutorService loopExecutor;
    protected volatile boolean stopped = false;
    protected long pollInterval;
    protected Lock kafkaLock = new ReentrantLock();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        loopExecutor = Executors.newSingleThreadExecutor();
        this.ctx = params.getContext();
        kafkaConsumerConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("clientConfiguration")),
                KafkaConsumerConfiguration.class);
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
        if (loopExecutor != null) {
            loopExecutor.shutdownNow();
        }
        if (kafkaConsumer != null) {
            kafkaLock.lock();
            try {
                kafkaConsumer.unsubscribe();
                kafkaConsumer.close();
            } finally {
                kafkaLock.unlock();
            }

        }
    }

    protected void initConsumer(KafkaConsumerConfiguration configuration) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, configuration.getClientId());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, configuration.getGroupId());
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, configuration.getAutoCreateTopics());
        if (configuration.getOtherProperties() != null) {
            configuration.getOtherProperties().forEach(properties::put);
        }
        kafkaConsumer = new KafkaConsumer<>(properties);

        kafkaLock.lock();
        try {
            kafkaConsumer.subscribe(Collections.singletonList(configuration.getTopics()));
        } finally {
            kafkaLock.unlock();
        }

        pollInterval = configuration.getPollInterval();
        stopped = false;
    }

    protected abstract void doProcess(IntegrationContext context, T msg) throws Exception;

}
