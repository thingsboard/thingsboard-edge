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
package org.thingsboard.monitoring.transport.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.TransportType;
import org.thingsboard.monitoring.config.service.MqttTransportMonitoringConfig;
import org.thingsboard.monitoring.transport.TransportHealthChecker;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class MqttTransportHealthChecker extends TransportHealthChecker<MqttTransportMonitoringConfig> {

    private MqttClient mqttClient;

    private static final String DEVICE_TELEMETRY_TOPIC = "v1/devices/me/telemetry";

    protected MqttTransportHealthChecker(MqttTransportMonitoringConfig config, MonitoringTargetConfig target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (mqttClient == null || !mqttClient.isConnected()) {
            String clientId = MqttAsyncClient.generateClientId();
            String accessToken = target.getDevice().getCredentials().getCredentialsId();
            mqttClient = new MqttClient(target.getBaseUrl(), clientId, new MemoryPersistence());
            mqttClient.setTimeToWait(config.getRequestTimeoutMs());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(accessToken);
            options.setConnectionTimeout(config.getRequestTimeoutMs() / 1000);
            IMqttToken result = mqttClient.connectWithResult(options);
            if (result.getException() != null) {
                throw result.getException();
            }
            log.debug("Initialized MQTT client for URI {}", mqttClient.getServerURI());
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload.getBytes());
        message.setQos(config.getQos());
        mqttClient.publish(DEVICE_TELEMETRY_TOPIC, message);
    }

    @Override
    protected void destroyClient() throws Exception {
        if (mqttClient != null) {
            mqttClient.disconnect();
            mqttClient = null;
            log.info("Disconnected MQTT client");
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.MQTT;
    }

}
