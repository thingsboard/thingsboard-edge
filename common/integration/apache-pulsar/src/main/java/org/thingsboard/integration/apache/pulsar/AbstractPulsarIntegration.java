/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.apache.pulsar;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.BatchReceivePolicy;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkContentType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.integration.api.util.ConvertUtil.toDebugMessage;

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
        loopExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-loop"));
        this.ctx = params.getContext();
        pulsarConfiguration = getClientConfiguration(configuration, PulsarConfiguration.class);

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
                persistDebug(context, "Uplink", UplinkContentType.BINARY, toDebugMessage(UplinkContentType.BINARY, msg.getMsg()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
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
