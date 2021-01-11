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
package org.thingsboard.integration.mqtt.ibm;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.thingsboard.integration.mqtt.MqttClientConfiguration;
import org.thingsboard.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.integration.mqtt.credentials.BasicCredentials;
import org.thingsboard.integration.mqtt.credentials.MqttClientCredentials;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import java.io.File;
import java.security.Security;
import java.util.Optional;

@Slf4j
public class IbmWatsonIotIntegration extends BasicMqttIntegration {

    private static final String IBM_WATSON_IOT_ENDPOINT = "messaging.internetofthings.ibmcloud.com";
    private static final String IBM_WATSON_IOT_COMMANDS_TOPIC = "iot-2/type/${device_type}/id/${device_id}/cmd/${command_id}/fmt/${format}";

    @Override
    protected String getDownlinkTopicPattern() {
        return IBM_WATSON_IOT_COMMANDS_TOPIC;
    }

    @Override
    protected void setupConfiguration(MqttClientConfiguration mqttClientConfiguration) {
        mqttClientConfiguration.setCleanSession(true);
        MqttClientCredentials credentials = mqttClientConfiguration.getCredentials();
        if (credentials == null || !(credentials instanceof BasicCredentials)) {
            throw new RuntimeException("Can't setup IBM Watson IoT integration without Application Credentials!");
        }
        BasicCredentials basicCredentials = (BasicCredentials) credentials;
        if (StringUtils.isEmpty(basicCredentials.getUsername()) ||
                StringUtils.isEmpty(basicCredentials.getPassword())) {
            throw new RuntimeException("Can't setup IBM Watson IoT integration. Required IBM Watson IoT Application Credentials values are missing!");
        }

        String apiKey = basicCredentials.getUsername();
        String[] parts = apiKey.split("-");
        if (parts.length != 3 || !parts[0].equals("a") || StringUtils.isEmpty(parts[1])) {
            throw new RuntimeException("Can't setup IBM Watson IoT integration. Invalid format of Application API Key!");
        }
        String organizationId = parts[1];

        mqttClientConfiguration.setHost(organizationId + "." + IBM_WATSON_IOT_ENDPOINT);
        mqttClientConfiguration.setPort(8883);
        mqttClientConfiguration.setClientId("a:" + organizationId + ":" + RandomStringUtils.randomAlphanumeric(10));
    }

    @Override
    protected Optional<SslContext> initSslContext(MqttClientConfiguration configuration) throws SSLException {
        try {
            Security.addProvider(new BouncyCastleProvider());
            return Optional.of(SslContextBuilder.forClient()
                    .keyManager((KeyManagerFactory) null)
                    .trustManager((File) null)
                    .clientAuth(ClientAuth.NONE)
                    .build());
        } catch (Exception e) {
            log.error("Creating TLS factory failed!", e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

}
