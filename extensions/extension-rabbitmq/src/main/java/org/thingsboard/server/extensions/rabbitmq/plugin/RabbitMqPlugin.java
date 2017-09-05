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
package org.thingsboard.server.extensions.rabbitmq.plugin;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.rabbitmq.action.RabbitMqPluginAction;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Plugin(name = "RabbitMQ Plugin", actions = {RabbitMqPluginAction.class},
descriptor = "RabbitMqPluginDescriptor.json", configuration = RabbitMqPluginConfiguration.class)
@Slf4j
public class RabbitMqPlugin extends AbstractPlugin<RabbitMqPluginConfiguration> {

    private ConnectionFactory factory;
    private Connection connection;
    private RabbitMqMsgHandler handler;

    @Override
    public void init(RabbitMqPluginConfiguration configuration) {
        factory = new ConnectionFactory();
        factory.setHost(configuration.getHost());
        factory.setPort(configuration.getPort());
        set(configuration.getVirtualHost(), factory::setVirtualHost);
        set(configuration.getUserName(), factory::setUsername);
        set(configuration.getPassword(), factory::setPassword);
        set(configuration.getAutomaticRecoveryEnabled(), factory::setAutomaticRecoveryEnabled);
        set(configuration.getConnectionTimeout(), factory::setConnectionTimeout);
        set(configuration.getHandshakeTimeout(), factory::setHandshakeTimeout);
        set(configuration.getClientProperties(), props -> {
            factory.setClientProperties(props.stream().collect(Collectors.toMap(
                    RabbitMqPluginConfiguration.RabbitMqPluginProperties::getKey,
                    RabbitMqPluginConfiguration.RabbitMqPluginProperties::getValue)));
        });

        init();
    }

    private <T> void set(T source, Consumer<T> setter) {
        if (source != null && !StringUtils.isEmpty(source.toString())) {
            setter.accept(source);
        }
    }

    private void init() {
        try {
            this.connection = factory.newConnection();
            this.handler = new RabbitMqMsgHandler(connection.createChannel());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private void destroy() {
        try {
            this.handler = null;
            this.connection.close();
        } catch (Exception e) {
            log.info("Failed to close connection during destroy()", e);
        }
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return handler;
    }

    @Override
    public void resume(PluginContext ctx) {
        init();
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
