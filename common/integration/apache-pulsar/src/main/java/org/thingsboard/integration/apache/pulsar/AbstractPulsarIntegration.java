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
package org.thingsboard.integration.apache.pulsar;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.BatchReceivePolicy;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractPulsarIntegration<T extends PulsarIntegrationMsg> extends AbstractIntegration<T> {

    protected PulsarConfiguration pulsarConfiguration;
    protected IntegrationContext ctx;
    protected ExecutorService loopExecutor;
    protected volatile boolean stopped = false;
    protected PulsarClient pulsarClient;
    protected Consumer<byte[]> pulsarConsumer;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        loopExecutor = Executors.newSingleThreadExecutor();
        this.ctx = params.getContext();
        pulsarConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("clientConfiguration")),
                PulsarConfiguration.class);

        pulsarClient = PulsarClient.builder()
                .authentication(pulsarConfiguration.getCredentials().getAuthentication())
                .serviceUrl(pulsarConfiguration.getServiceUrl())
                .allowTlsInsecureConnection(true)
                .build();

        BatchReceivePolicy batchReceivePolicy = BatchReceivePolicy.builder()
                .maxNumMessages(pulsarConfiguration.getMaxNumMessages())
                .maxNumBytes(pulsarConfiguration.getMaxNumBytes())
                .timeout(pulsarConfiguration.getTimeoutInMs(), TimeUnit.MILLISECONDS)
                .build();

        pulsarConsumer = pulsarClient
                .newConsumer()
                .subscriptionName(pulsarConfiguration.getSubscriptionName())
                .topic(pulsarConfiguration.getTopics().split(","))
                .batchReceivePolicy(batchReceivePolicy)
                .subscribe();
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

        if (pulsarConsumer != null) {
            try {
                pulsarConsumer.close();
            } catch (PulsarClientException e) {
                log.warn("Failed to stop Apache Pulsar Consumer!!!", e);
            }
        }

        if (pulsarClient != null) {
            try {
                pulsarClient.close();
            } catch (PulsarClientException e) {
                log.warn("Failed to stop Apache Pulsar Client!!!", e);
            }
        }
    }

    protected abstract void doProcess(IntegrationContext context, T msg) throws Exception;

}
