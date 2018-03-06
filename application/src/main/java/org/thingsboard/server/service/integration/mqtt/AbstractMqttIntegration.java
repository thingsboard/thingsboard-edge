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
package org.thingsboard.server.service.integration.mqtt;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import nl.jk5.mqtt.MqttClient;
import nl.jk5.mqtt.MqttClientConfig;
import nl.jk5.mqtt.MqttConnectResult;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.service.converter.TBDataConverter;
import org.thingsboard.server.service.integration.AbstractIntegration;
import org.thingsboard.server.service.integration.DefaultPlatformIntegrationService;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;
import org.thingsboard.server.service.integration.downlink.DownLinkMsg;
import org.thingsboard.server.service.integration.msg.RPCCallIntegrationMsg;
import org.thingsboard.server.service.integration.msg.SharedAttributesUpdateIntegrationMsg;

import javax.net.ssl.SSLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashvayka on 25.12.17.
 */
@Slf4j
public abstract class AbstractMqttIntegration<T extends MqttIntegrationMsg> extends AbstractIntegration<T> {

    protected MqttClient mqttClient;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        MqttClientConfiguration mqttClientConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("clientConfiguration")),
                MqttClientConfiguration.class);
        setupConfiguration(mqttClientConfiguration);
        mqttClient = initClient(mqttClientConfiguration);
    }

    protected void setupConfiguration(MqttClientConfiguration mqttClientConfiguration) {
    }

    @Override
    public void update(TbIntegrationInitParams params) throws Exception {
        if (mqttClient != null) {
            mqttClient.disconnect();
        }
        init(params);
    }

    @Override
    public void destroy() {
        if (mqttClient != null) {
            mqttClient.disconnect();
        }
    }

    @Override
    public void process(IntegrationContext context, T msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.warn("Failed to apply data converter function", e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, "Uplink", getUplinkContentType(), mapper.writeValueAsString(msg.toJson()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    @Override
    public void onSharedAttributeUpdate(IntegrationContext context, SharedAttributesUpdateIntegrationMsg msg) {
        logDownlink(context, "SharedAttributeUpdate", msg);
        if (downlinkConverter != null) {
            DownLinkMsg downLinkMsg = DownLinkMsg.from(msg);
            processDownLinkMsg(context, downLinkMsg);
        }
    }

    @Override
    public void onRPCCall(IntegrationContext context, RPCCallIntegrationMsg msg) {
        logDownlink(context, "RPCCall", msg);
        if (downlinkConverter != null) {
            DownLinkMsg downLinkMsg = DownLinkMsg.from(msg);
            processDownLinkMsg(context, downLinkMsg);
        }
    }

    protected void processDownLinkMsg(IntegrationContext context, DownLinkMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            if (doProcessDownLinkMsg(context, msg)) {
                integrationStatistics.incMessagesProcessed();
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            exception = e;
            status = "ERROR";
        }
        reportDownlinkError(context, msg, status, exception);
    }

    protected abstract boolean doProcessDownLinkMsg(IntegrationContext context, DownLinkMsg msg) throws Exception;

    protected abstract void doProcess(IntegrationContext context, T msg) throws Exception;

    private MqttClient initClient(MqttClientConfiguration configuration) throws Exception {
        Optional<SslContext> sslContextOpt = initSslContext(configuration);

        MqttClientConfig config = sslContextOpt.isPresent() ? new MqttClientConfig(sslContextOpt.get()) : new MqttClientConfig();
        if (!StringUtils.isEmpty(configuration.getClientId())) {
            config.setClientId(configuration.getClientId());
        }

        configuration.getCredentials().configure(config);

        MqttClient client = MqttClient.create(config);
        client.setEventLoop(DefaultPlatformIntegrationService.EVENT_LOOP_GROUP);
        Future<MqttConnectResult> connectFuture = client.connect(configuration.getHost(), configuration.getPort());
        MqttConnectResult result;
        try {
            result = connectFuture.get(configuration.getConnectTimeoutSec(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            connectFuture.cancel(true);
            client.disconnect();
            String hostPort = configuration.getHost() + ":" + configuration.getPort();
            throw new RuntimeException(String.format("Failed to connect to MQTT broker at %s.", hostPort));
        }
        if (!result.isSuccess()) {
            connectFuture.cancel(true);
            client.disconnect();
            String hostPort = configuration.getHost() + ":" + configuration.getPort();
            throw new RuntimeException(String.format("Failed to connect to MQTT broker at %s. Result code is: %s", hostPort, result.getReturnCode()));
        }
        return client;
    }

    protected Optional<SslContext> initSslContext(MqttClientConfiguration configuration) throws SSLException {
        Optional<SslContext> result = configuration.getCredentials().initSslContext();
        if (configuration.isSsl() && !result.isPresent()) {
            result = Optional.of(SslContextBuilder.forClient().build());
        }
        return result;
    }

    protected <T> void logDownlink(IntegrationContext context, String updateType, T msg) {
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, updateType, "JSON", mapper.writeValueAsString(msg), downlinkConverter != null ? "OK" : "FAILURE", null);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }


}
