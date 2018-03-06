/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.extensions.mqtt.plugin;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.mqtt.action.MqttPluginAction;

import java.util.UUID;

@Plugin(name = "Mqtt Plugin", actions = {MqttPluginAction.class},
        descriptor = "MqttPluginDescriptor.json", configuration = MqttPluginConfiguration.class)
@Slf4j
public class MqttPlugin extends AbstractPlugin<MqttPluginConfiguration> {

    private MqttMsgHandler handler;

    private MqttAsyncClient mqttClient;
    private MqttConnectOptions mqttClientOptions;

    private int retryInterval;

    private final Object connectLock = new Object();

    @Override
    public void init(MqttPluginConfiguration configuration) {
        retryInterval = configuration.getRetryInterval();

        mqttClientOptions = new MqttConnectOptions();
        mqttClientOptions.setCleanSession(false);
        mqttClientOptions.setMaxInflight(configuration.getMaxInFlight());
        mqttClientOptions.setAutomaticReconnect(true);
        String clientId = configuration.getClientId();
        if (StringUtils.isEmpty(clientId)) {
            clientId = UUID.randomUUID().toString();
        }
        if (!StringUtils.isEmpty(configuration.getAccessToken())) {
            mqttClientOptions.setUserName(configuration.getAccessToken());
        }
        try {
            mqttClient = new MqttAsyncClient("tcp://" + configuration.getHost() + ":" + configuration.getPort(), clientId);
        } catch (Exception e) {
            log.error("Failed to create mqtt client", e);
            throw new RuntimeException(e);
        }
        connect();
    }

    private void connect() {
        if (!mqttClient.isConnected()) {
            synchronized (connectLock) {
                while (!mqttClient.isConnected()) {
                    log.debug("Attempt to connect to requested mqtt host [{}]!", mqttClient.getServerURI());
                    try {
                        mqttClient.connect(mqttClientOptions, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken iMqttToken) {
                                log.info("Connected to requested mqtt host [{}]!", mqttClient.getServerURI());
                            }

                            @Override
                            public void onFailure(IMqttToken iMqttToken, Throwable e) {
                                //Do nothing
                            }
                        }).waitForCompletion();
                    } catch (MqttException e) {
                        log.warn("Failed to connect to requested mqtt host  [{}]!", mqttClient.getServerURI(), e);
                        if (!mqttClient.isConnected()) {
                            try {
                                connectLock.wait(retryInterval);
                            } catch (InterruptedException e1) {
                                log.trace("Failed to wait for retry interval!", e);
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
            }
        }
        this.handler = new MqttMsgHandler(mqttClient);
    }

    private void destroy() {
        try {
            this.handler = null;
            this.mqttClient.disconnect();
        } catch (MqttException e) {
            log.error("Failed to close mqtt client connection during destroy()", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return handler;
    }

    @Override
    public void resume(PluginContext ctx) {
        connect();
    }

    @Override
    public void suspend(PluginContext ctx) {
        destroy();
    }

    @Override
    public void stop(PluginContext ctx) {
        destroy();
    }
}
