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
package org.thingsboard.integration.mqtt.ttn;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Created by igor on 3/2/18.
 */
@Slf4j
public class TtnIntegration extends BasicMqttIntegration {

    private static final String TTN_ENDPOINT = "thethings.network";
    private static final String TTI_ENDPOINT = "thethings.industries";

    @Override
    protected String getDownlinkTopicPattern() {
        return this.configuration.getConfiguration().get("downlinkTopicPattern").asText();
    }

    @Override
    protected void setupConfiguration(MqttClientConfiguration mqttClientConfiguration) {
        String integrationType = this.configuration.getType().name();
        mqttClientConfiguration.setCleanSession(true);
        MqttClientCredentials credentials = mqttClientConfiguration.getCredentials();
        if (credentials == null || !(credentials instanceof BasicCredentials)) {
            throw new RuntimeException("Can't setup TheThingsNetwork integration without Application Credentials!");
        }
        BasicCredentials basicCredentials = (BasicCredentials) credentials;
        if (StringUtils.isEmpty(basicCredentials.getUsername()) ||
                StringUtils.isEmpty(basicCredentials.getPassword())) {
            throw new RuntimeException("Can't setup TheThingsNetwork integration. Required TheThingsNetwork Application Credentials values are missing!");
        }

        if (!mqttClientConfiguration.isCustomHost()) {
            String region = mqttClientConfiguration.getHost();
            if (integrationType.equals("TTN") && !region.endsWith(TTN_ENDPOINT)){
                mqttClientConfiguration.setHost(region + "." + TTN_ENDPOINT);
            } else if (integrationType.equals("TTI") && !region.endsWith(TTI_ENDPOINT)) {
                mqttClientConfiguration.setHost(region + "." + TTI_ENDPOINT);
            }
        }
//        mqttClientConfiguration.setPort(8883);
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
