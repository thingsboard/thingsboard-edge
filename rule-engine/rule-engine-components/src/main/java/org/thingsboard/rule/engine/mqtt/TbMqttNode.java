/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttConnectResult;
import org.thingsboard.rule.engine.api.MqttClientSettings;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.BasicCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.net.ssl.SSLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "mqtt",
        configClazz = TbMqttNodeConfiguration.class,
        version = 1,
        clusteringMode = ComponentClusteringMode.USER_PREFERENCE,
        nodeDescription = "Publish messages to the MQTT broker",
        nodeDetails = "Will publish message payload to the MQTT broker with QoS <b>AT_LEAST_ONCE</b>.",
        configDirective = "tbExternalNodeMqttConfig",
        icon = "call_split"
)
public class TbMqttNode extends TbAbstractExternalNode {

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private static final String ERROR = "error";

    protected TbMqttNodeConfiguration mqttNodeConfiguration;

    protected MqttClient mqttClient;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.mqttNodeConfiguration = TbNodeUtils.convert(configuration, TbMqttNodeConfiguration.class);
        try {
            this.mqttClient = initClient(ctx);
        } catch (TbNodeException e) {
            throw e;
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String topic = TbNodeUtils.processPattern(this.mqttNodeConfiguration.getTopicPattern(), msg);
        var tbMsg = ackIfNeeded(ctx, msg);
        this.mqttClient.publish(topic, Unpooled.wrappedBuffer(getData(tbMsg, mqttNodeConfiguration.isParseToPlainText()).getBytes(UTF8)),
                        MqttQoS.AT_LEAST_ONCE, mqttNodeConfiguration.isRetainedMessage())
                .addListener(future -> {
                            if (future.isSuccess()) {
                                tellSuccess(ctx, tbMsg);
                            } else {
                                tellFailure(ctx, processException(tbMsg, future.cause()), future.cause());
                            }
                        }
                );
    }

    private TbMsg processException(TbMsg origMsg, Throwable e) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        return origMsg.transform()
                .metaData(metaData)
                .build();
    }

    @Override
    public void destroy() {
        if (this.mqttClient != null) {
            this.mqttClient.disconnect();
        }
    }

    String getOwnerId(TbContext ctx) {
        return "Tenant[" + ctx.getTenantId().getId() + "]RuleNode[" + ctx.getSelf().getId().getId() + "]";
    }

    protected MqttClient initClient(TbContext ctx) throws Exception {
        MqttClientConfig config = new MqttClientConfig(getSslContext());
        config.setOwnerId(getOwnerId(ctx));
        if (!StringUtils.isEmpty(this.mqttNodeConfiguration.getClientId())) {
            config.setClientId(getClientId(ctx));
        }
        config.setCleanSession(this.mqttNodeConfiguration.isCleanSession());
        config.setProtocolVersion(this.mqttNodeConfiguration.getProtocolVersion());

        MqttClientSettings mqttClientSettings = ctx.getMqttClientSettings();
        config.setRetransmissionConfig(new MqttClientConfig.RetransmissionConfig(
                mqttClientSettings.getRetransmissionMaxAttempts(),
                mqttClientSettings.getRetransmissionInitialDelayMillis(),
                mqttClientSettings.getRetransmissionJitterFactor()
        ));

        prepareMqttClientConfig(config);
        MqttClient client = getMqttClient(ctx, config);
        client.setEventLoop(ctx.getSharedEventLoop());
        Promise<MqttConnectResult> connectFuture = client.connect(this.mqttNodeConfiguration.getHost(), this.mqttNodeConfiguration.getPort());
        MqttConnectResult result;
        try {
            result = connectFuture.get(this.mqttNodeConfiguration.getConnectTimeoutSec(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            connectFuture.cancel(true);
            client.disconnect();
            String hostPort = this.mqttNodeConfiguration.getHost() + ":" + this.mqttNodeConfiguration.getPort();
            throw new RuntimeException(String.format("Failed to connect to MQTT broker at %s.", hostPort));
        }
        if (!result.isSuccess()) {
            connectFuture.cancel(true);
            client.disconnect();
            String hostPort = this.mqttNodeConfiguration.getHost() + ":" + this.mqttNodeConfiguration.getPort();
            throw new RuntimeException(String.format("Failed to connect to MQTT broker at %s. Result code is: %s", hostPort, result.getReturnCode()));
        }
        return client;
    }

    private String getClientId(TbContext ctx) throws TbNodeException {
        String clientId = this.mqttNodeConfiguration.isAppendClientIdSuffix() ?
                this.mqttNodeConfiguration.getClientId() + "_" + ctx.getServiceId() :
                this.mqttNodeConfiguration.getClientId();
        if (clientId.length() > 23) {
            throw new TbNodeException("Client ID is too long '" + clientId + "'. " +
                    "The length of Client ID cannot be longer than 23, but current length is " + clientId.length() + ".", true);
        }
        return clientId;
    }

    MqttClient getMqttClient(TbContext ctx, MqttClientConfig config) {
        return MqttClient.create(config, null, ctx.getExternalCallExecutor());
    }

    protected void prepareMqttClientConfig(MqttClientConfig config) {
        ClientCredentials credentials = this.mqttNodeConfiguration.getCredentials();
        if (credentials.getType() == CredentialsType.BASIC) {
            BasicCredentials basicCredentials = (BasicCredentials) credentials;
            config.setUsername(basicCredentials.getUsername());
            config.setPassword(basicCredentials.getPassword());
        }
    }

    private SslContext getSslContext() throws SSLException {
        return this.mqttNodeConfiguration.isSsl() ? this.mqttNodeConfiguration.getCredentials().initSslContext() : null;
    }

    private String getData(TbMsg tbMsg, boolean parseToPlainText) {
        if (parseToPlainText) {
            return JacksonUtil.toPlainText(tbMsg.getData());
        }
        return tbMsg.getData();
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                String parseToPlainText = "parseToPlainText";
                if (!oldConfiguration.has(parseToPlainText)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put(parseToPlainText, false);
                }
                break;
            case 1:
                String protocolVersion = "protocolVersion";
                if (!oldConfiguration.has(protocolVersion)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put(protocolVersion, MqttVersion.MQTT_3_1_1.name());
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
