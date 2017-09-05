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
package org.thingsboard.server.extensions.kafka.plugin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.core.BasicStatusCodeResponse;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.kafka.action.KafkaActionMsg;
import org.thingsboard.server.extensions.kafka.action.KafkaActionPayload;

@RequiredArgsConstructor
@Slf4j
public class KafkaMsgHandler implements RuleMsgHandler {

    private final Producer<?, String> producer;

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (!(msg instanceof KafkaActionMsg)) {
            throw new RuleException("Unsupported message type " + msg.getClass().getName() + "!");
        }
        KafkaActionPayload payload = ((KafkaActionMsg) msg).getPayload();
        log.debug("Processing kafka payload: {}", payload);
        try {
            producer.send(new ProducerRecord<>(payload.getTopic(), payload.getMsgBody()),
                    (metadata, e) -> {
                        if (payload.isSync()) {
                            if (metadata != null) {
                                ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId,
                                        BasicStatusCodeResponse.onSuccess(payload.getMsgType(), payload.getRequestId())));
                            } else {
                                ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId,
                                        BasicStatusCodeResponse.onError(payload.getMsgType(), payload.getRequestId(), e)));
                            }
                        }
                    });
        } catch (Exception e) {
            throw new RuleException(e.getMessage(), e);
        }
    }
}
