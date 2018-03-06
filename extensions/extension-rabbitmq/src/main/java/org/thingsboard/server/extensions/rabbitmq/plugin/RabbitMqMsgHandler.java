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
package org.thingsboard.server.extensions.rabbitmq.plugin;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.core.BasicStatusCodeResponse;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.rabbitmq.action.RabbitMqActionMsg;
import org.thingsboard.server.extensions.rabbitmq.action.RabbitMqActionPayload;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Andrew Shvayka
 */
@RequiredArgsConstructor
public class RabbitMqMsgHandler implements RuleMsgHandler {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final Channel channel;

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (!(msg instanceof RabbitMqActionMsg)) {
            throw new RuleException("Unsupported message type " + msg.getClass().getName() + "!");
        }
        RabbitMqActionPayload payload = ((RabbitMqActionMsg) msg).getPayload();
        AMQP.BasicProperties properties = convert(payload.getMessageProperties());
        try {
            channel.basicPublish(
                    payload.getExchange() != null ? payload.getExchange() : "",
                    payload.getQueueName(),
                    properties,
                    payload.getPayload().getBytes(UTF8));
            if (payload.isSync()) {
                ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId,
                        BasicStatusCodeResponse.onSuccess(payload.getMsgType(), payload.getRequestId())));
            }
        } catch (IOException e) {
            throw new RuleException(e.getMessage(), e);
        }
    }

    private static AMQP.BasicProperties convert(String name) throws RuleException {
        switch (name) {
            case "BASIC":
                return MessageProperties.BASIC;
            case "TEXT_PLAIN":
                return MessageProperties.TEXT_PLAIN;
            case "MINIMAL_BASIC":
                return MessageProperties.MINIMAL_BASIC;
            case "MINIMAL_PERSISTENT_BASIC":
                return MessageProperties.MINIMAL_PERSISTENT_BASIC;
            case "PERSISTENT_BASIC":
                return MessageProperties.PERSISTENT_BASIC;
            case "PERSISTENT_TEXT_PLAIN":
                return MessageProperties.PERSISTENT_TEXT_PLAIN;
            default:
                throw new RuleException("Message Properties: '" + name + "' is undefined!");
        }
    }

}
