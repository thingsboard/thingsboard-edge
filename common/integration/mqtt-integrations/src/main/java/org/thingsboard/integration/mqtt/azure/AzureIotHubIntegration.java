/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.integration.mqtt.azure;

import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.mqtt.MqttClientConfiguration;
import org.thingsboard.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.integration.mqtt.credentials.CertPemClientCredentials;
import org.thingsboard.integration.mqtt.credentials.MqttClientCredentials;
import org.thingsboard.mqtt.MqttClientConfig;

import java.util.Optional;

@Slf4j
public class AzureIotHubIntegration extends BasicMqttIntegration {

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
    }

    @Override
    protected void setupConfiguration(MqttClientConfiguration mqttClientConfiguration) {
        mqttClientConfiguration.setPort(8883);
        mqttClientConfiguration.setCleanSession(true);
        MqttClientCredentials credentials = mqttClientConfiguration.getCredentials();
        mqttClientConfiguration.setCredentials(new MqttClientCredentials() {
            @Override
            public Optional<SslContext> initSslContext() {
                if (credentials instanceof AzureIotHubSasCredentials) {
                    AzureIotHubSasCredentials sasCredentials = (AzureIotHubSasCredentials) credentials;
                    if (sasCredentials.getCaCert() == null || sasCredentials.getCaCert().isEmpty()) {
                        sasCredentials.setCaCert(AzureIotHubUtil.getDefaultCaCert());
                    }
                } else if (credentials instanceof CertPemClientCredentials) {
                    CertPemClientCredentials pemCredentials = (CertPemClientCredentials) credentials;
                    if (pemCredentials.getCaCert() == null || pemCredentials.getCaCert().isEmpty()) {
                        pemCredentials.setCaCert(AzureIotHubUtil.getDefaultCaCert());
                    }
                }
                return credentials.initSslContext();
            }

            @Override
            public void configure(MqttClientConfig config) {
                config.setProtocolVersion(MqttVersion.MQTT_3_1_1);
                config.setUsername(AzureIotHubUtil.buildUsername(mqttClientConfiguration.getHost(), config.getClientId()));
                if (credentials instanceof AzureIotHubSasCredentials) {
                    AzureIotHubSasCredentials sasCredentials = (AzureIotHubSasCredentials) credentials;
                    config.setPassword(AzureIotHubUtil.buildSasToken(mqttClientConfiguration.getHost(), sasCredentials.getSasKey()));
                }
            }
        });
    }
}
