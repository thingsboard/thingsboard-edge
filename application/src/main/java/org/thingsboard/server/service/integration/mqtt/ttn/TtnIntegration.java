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
package org.thingsboard.server.service.integration.mqtt.ttn;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.thingsboard.server.service.integration.mqtt.MqttClientConfiguration;
import org.thingsboard.server.service.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.server.service.integration.mqtt.credentials.BasicCredentials;
import org.thingsboard.server.service.integration.mqtt.credentials.MqttClientCredentials;

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
    private static final String TTN_DOWNLINK_TOPIC = "/devices/${devId}/down";

    private String appId;

    @Override
    protected String getDownlinkTopicPattern() {
        return this.appId + TTN_DOWNLINK_TOPIC;
    }

    @Override
    protected void setupConfiguration(MqttClientConfiguration mqttClientConfiguration) {
        MqttClientCredentials credentials = mqttClientConfiguration.getCredentials();
        if (credentials == null || !(credentials instanceof BasicCredentials)) {
            throw new RuntimeException("Can't setup TheThingsNetwork integration without Application Credentials!");
        }
        BasicCredentials basicCredentials = (BasicCredentials) credentials;
        if (StringUtils.isEmpty(basicCredentials.getUsername()) ||
                StringUtils.isEmpty(basicCredentials.getPassword())) {
            throw new RuntimeException("Can't setup TheThingsNetwork integration. Required TheThingsNetwork Application Credentials values are missing!");
        }

        this.appId = basicCredentials.getUsername();

        String region = mqttClientConfiguration.getHost();
        mqttClientConfiguration.setHost(region + "." + TTN_ENDPOINT);
        mqttClientConfiguration.setPort(8883);
    }

    @Override
    protected Optional<SslContext> initSslContext(MqttClientConfiguration configuration) throws SSLException {
        try {
            Security.addProvider(new BouncyCastleProvider());
            return Optional.of(SslContextBuilder.forClient()
                    .keyManager(null)
                    .trustManager((File) null)
                    .clientAuth(ClientAuth.NONE)
                    .build());
        } catch (Exception e) {
            log.error("Creating TLS factory failed!", e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

}
