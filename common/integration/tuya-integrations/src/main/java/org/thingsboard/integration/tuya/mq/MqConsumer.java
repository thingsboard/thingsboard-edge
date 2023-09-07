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
package org.thingsboard.integration.tuya.mq;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;

@Builder
@Slf4j
public class MqConsumer {

    private final String serviceUrl;
    private final String accessId;
    private final String accessKey;
    private final MqEnv env;

    private final IMessageListener messageListener;
    private final ResultHandler resultHandler;

    private Consumer consumer;
    private PulsarClient client;

    private volatile boolean stopped;
    private volatile boolean connected;

    public void initClient() throws PulsarClientException {
        stopClient();
        client = PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .allowTlsInsecureConnection(true)
                .authentication(new MqAuthentication(accessId, accessKey))
                .build();
    }

    public void connect() throws PulsarClientException {
        String topic = String.format("%s/out/%s", accessId, (env != null ? env : MqEnv.PROD).getValue());
        if (client == null) {
            initClient();
        }
        if (consumer == null) {
            try {
                consumer = client.newConsumer()
                        .topic(topic)
                        .subscriptionName(String.format("%s-sub", accessId))
                        .subscriptionType(SubscriptionType.Failover)
                        .subscriptionTopicsMode(RegexSubscriptionMode.AllTopics)
                        .autoUpdatePartitions(Boolean.FALSE)
                        .messageListener(((consumer1, msg) -> {
                            try {
                                messageListener.onMessageArrived(msg);
                                consumer1.acknowledge(msg);
                            } catch (Exception e) {
                                resultHandler.onResult("Uplink", "", e);
                            }
                        }))
                        .subscribe();
            } catch (PulsarClientException wrappedException) {
                if (wrappedException.getCause() == null || wrappedException.getCause().getCause() == null) {
                    throw wrappedException;
                }
                throw (PulsarClientException) wrappedException.getCause().getCause();
            }
        }
        if (!checkConnection()) {
            throw new RuntimeException("Cannot connect to message producer.");
        }
        connected = true;
    }

    public interface IMessageListener {
        void onMessageArrived(Message message);
    }

    public interface ResultHandler {
        void onResult(String type, String msg, Exception exception);
    }

    public void stopClient() throws PulsarClientException {
        stopped = true;
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }
        if (client != null) {
            client.close();
            client = null;
        }
    }

    public boolean checkConnection() {
        if (client == null || client.isClosed() || consumer == null) {
            return false;
        }
        long connectionTimeoutTs = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < connectionTimeoutTs) {
            if (consumer.isConnected()) {
                return true;
            }
        }
        return false;
    }
}
