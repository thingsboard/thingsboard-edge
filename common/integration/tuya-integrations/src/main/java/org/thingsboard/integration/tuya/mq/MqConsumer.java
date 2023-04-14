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

    public void start() throws Exception {
        stopped = false;
        while (!stopped) {
            try {
                if (!connected) {
                    connect(true);
                }
                processMessages();
            } catch (Exception ignored) {
                Thread.sleep(5 * 1000);
            }
        }
    }

    public void connect(boolean sleep) throws Exception {
        if (stopped) return;
        try {
            if (client == null || client.isClosed()) {
                client = PulsarClient.builder()
                        .serviceUrl(serviceUrl)
                        .allowTlsInsecureConnection(true)
                        .authentication(new MqAuthentication(accessId, accessKey))
                        .build();
                consumer = client.newConsumer()
                        .topic(String.format("%s/out/%s", accessId, (env != null ? env : MqEnv.PROD).getValue()))
                        .subscriptionName(String.format("%s-sub", accessId))
                        .subscriptionType(SubscriptionType.Failover)
//                        .subscriptionType(SubscriptionType.Exclusive)
                        .subscriptionTopicsMode(RegexSubscriptionMode.AllTopics)
                        .autoUpdatePartitions(Boolean.FALSE)
                        .subscribe();
                if (!checkConnection()) {
                    throw new RuntimeException("Cannot connect to message producer.");
                }
            }
            connected = true;
            resultHandler.onResult("CONNECT", "", null);
        } catch (Exception e) {
            connected = false;
            client.shutdown();
            resultHandler.onResult("CONNECT", "", e);
            if(sleep) {
                Thread.sleep(60 * 1000);
            }
        }
    }

    private void processMessages() throws Exception {
        try {
            Message<?> message = consumer.receive();
            messageListener.onMessageArrived(message);
            consumer.acknowledge(message);
        } catch (Exception e) {
            connected = false;
            if (!client.isClosed()) {
                client.close();
            }
            resultHandler.onResult("CONNECT", "", e);
        }
    }

    public interface IMessageListener {
        void onMessageArrived(Message message);
    }

    public interface ResultHandler {
        void onResult(String type, String msg, Exception exception);
    }

    public void stop() throws Exception {
        stopped = true;
        if (consumer != null) {
            consumer.unsubscribe();
        }
        if (!client.isClosed()) {
            client.close();
        }
    }

    private boolean checkConnection() {
        long connectionTimeoutTs = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < connectionTimeoutTs) {
            if (consumer.isConnected()) {
                return true;
            }
        }
        return false;
    }
}
