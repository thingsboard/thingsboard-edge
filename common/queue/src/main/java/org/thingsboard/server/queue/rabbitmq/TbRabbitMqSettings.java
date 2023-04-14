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
package org.thingsboard.server.queue.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
@Component
@Data
public class TbRabbitMqSettings {
    @Value("${queue.rabbitmq.exchange_name:}")
    private String exchangeName;
    @Value("${queue.rabbitmq.host:}")
    private String host;
    @Value("${queue.rabbitmq.port:}")
    private int port;
    @Value("${queue.rabbitmq.virtual_host:}")
    private String virtualHost;
    @Value("${queue.rabbitmq.username:}")
    private String username;
    @Value("${queue.rabbitmq.password:}")
    private String password;
    @Value("${queue.rabbitmq.automatic_recovery_enabled:}")
    private boolean automaticRecoveryEnabled;
    @Value("${queue.rabbitmq.connection_timeout:}")
    private int connectionTimeout;
    @Value("${queue.rabbitmq.handshake_timeout:}")
    private int handshakeTimeout;

    private ConnectionFactory connectionFactory;

    @PostConstruct
    private void init() {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setAutomaticRecoveryEnabled(automaticRecoveryEnabled);
        connectionFactory.setConnectionTimeout(connectionTimeout);
        connectionFactory.setHandshakeTimeout(handshakeTimeout);
    }
}
