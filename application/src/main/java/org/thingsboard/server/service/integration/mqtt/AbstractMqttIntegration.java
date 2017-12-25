/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.service.integration.mqtt;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import nl.jk5.mqtt.MqttClient;
import nl.jk5.mqtt.MqttClientConfig;
import nl.jk5.mqtt.MqttConnectResult;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.service.converter.ThingsboardDataConverter;
import org.thingsboard.server.service.integration.AbstractIntegration;

import javax.net.ssl.SSLException;
import java.util.Optional;

/**
 * Created by ashvayka on 25.12.17.
 */
@Slf4j
public abstract class AbstractMqttIntegration<T> extends AbstractIntegration<T> {

    private MqttClient mqttClient;

    @Override
    public void init(Integration dto, ThingsboardDataConverter converter) throws Exception {
        super.init(dto, converter);
        MqttClientConfiguration mqttClientConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("clientConfiguration")),
                MqttClientConfiguration.class);
        mqttClient = initClient(mqttClientConfiguration);
    }

    private MqttClient initClient(MqttClientConfiguration configuration) throws Exception {
        Optional<SslContext> sslContextOpt = initSslContext(configuration);

        MqttClientConfig config = sslContextOpt.isPresent() ? new MqttClientConfig(sslContextOpt.get()) : new MqttClientConfig();
        if (!StringUtils.isEmpty(configuration.getClientId())) {
            config.setClientId(configuration.getClientId());
        }

        configuration.getCredentials().configure(config);

        MqttClient client = MqttClient.create();
        MqttConnectResult result = client.connect(configuration.getHost(), configuration.getPort()).get();
        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to connect to MQTT broker. Result code is: " + result.getReturnCode());
        }
        return client;
    }

    private Optional<SslContext> initSslContext(MqttClientConfiguration configuration) throws SSLException {
        Optional<SslContext> result = configuration.getCredentials().initSslContext();
        if (configuration.isSsl() && !result.isPresent()) {
            result = Optional.of(SslContextBuilder.forClient().build());
        }
        return result;
    }
}
