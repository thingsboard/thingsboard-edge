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
package org.thingsboard.integration.mqtt.ttn;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
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
