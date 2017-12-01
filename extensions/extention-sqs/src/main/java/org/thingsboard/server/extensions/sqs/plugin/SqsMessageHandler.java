/**
 * The Thingsboard Authors ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 The Thingsboard Authors. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of The Thingsboard Authors and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to The Thingsboard Authors
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
package org.thingsboard.server.extensions.sqs.plugin;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.core.BasicStatusCodeResponse;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.AbstractRuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.sqs.action.fifo.SqsFifoQueueActionMsg;
import org.thingsboard.server.extensions.sqs.action.fifo.SqsFifoQueueActionPayload;
import org.thingsboard.server.extensions.sqs.action.standard.SqsStandardQueueActionMsg;
import org.thingsboard.server.extensions.sqs.action.standard.SqsStandardQueueActionPayload;

/**
 * Created by Valerii Sosliuk on 11/15/2017.
 */
@RequiredArgsConstructor
@Slf4j
public class SqsMessageHandler implements RuleMsgHandler {

    private final AmazonSQS sqs;

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (msg instanceof SqsStandardQueueActionMsg) {
            sendMessageToStandardQueue(ctx, tenantId, ruleId, msg);
            return;
        }
        if (msg instanceof SqsFifoQueueActionMsg) {
            sendMessageToFifoQueue(ctx, tenantId, ruleId, msg);
            return;
        }
        throw new RuleException("Unsupported message type " + msg.getClass().getName() + "!");
    }

    private void sendMessageToStandardQueue(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) {
        SqsStandardQueueActionPayload payload = ((SqsStandardQueueActionMsg) msg).getPayload();
        SendMessageRequest sendMsgRequest = new SendMessageRequest()
                .withDelaySeconds(payload.getDelaySeconds())
                .withQueueUrl(payload.getQueue())
                .withMessageBody(payload.getMsgBody());
        sqs.sendMessage(sendMsgRequest);
        if (payload.isSync()) {
            ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId,
                    BasicStatusCodeResponse.onSuccess(payload.getMsgType(), payload.getRequestId())));
        }
    }

    private void sendMessageToFifoQueue(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) {
        SqsFifoQueueActionPayload payload = ((SqsFifoQueueActionMsg) msg).getPayload();
        SendMessageRequest sendMsgRequest =  new SendMessageRequest()
                .withQueueUrl(payload.getQueue())
                .withMessageBody(payload.getMsgBody())
                .withMessageGroupId(payload.getDeviceId());
        sqs.sendMessage(sendMsgRequest);
        if (payload.isSync()) {
            ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId,
                    BasicStatusCodeResponse.onSuccess(payload.getMsgType(), payload.getRequestId())));
        }
    }
}
