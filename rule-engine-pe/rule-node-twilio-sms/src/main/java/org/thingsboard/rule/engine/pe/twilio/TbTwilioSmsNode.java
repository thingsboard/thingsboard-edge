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
package org.thingsboard.rule.engine.pe.twilio;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.rule.engine.api.util.DonAsynchron.withCallback;

/**
 * Created by igor on 5/25/18.
 */
@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send email",
        configClazz = TbTwilioSmsNodeConfiguration.class,
        nodeDescription = "Sends SMS message via Twilio.",
        nodeDetails = "Will send message payload as SMS message via Twilio.",
        uiResources = {"static/rulenode/twilio-sms-config.js"},
        configDirective = "tbActionNodeTwilioSmsConfig",
        icon = "send"
)
public class TbTwilioSmsNode implements TbNode {

    private TbTwilioSmsNodeConfiguration config;
    private TwilioRestClient twilioRestClient;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbTwilioSmsNodeConfiguration.class);
        this.twilioRestClient = new TwilioRestClient.Builder(this.config.getAccountSid(), this.config.getAccountToken()).build();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        withCallback(ctx.getExternalCallExecutor().executeAsync(() -> {
                    sendSms(ctx, msg);
                    return null;
                }),
                ok -> ctx.tellNext(msg, SUCCESS),
                fail -> ctx.tellFailure(msg, fail));
    }

    private void sendSms(TbContext ctx, TbMsg msg) throws Exception {
        String numberForm = TbNodeUtils.processPattern(this.config.getNumberFrom(), msg.getMetaData());
        String numbersTo = TbNodeUtils.processPattern(this.config.getNumbersTo(), msg.getMetaData());
        String[] numbersToList = numbersTo.split(",");
        if (numbersToList.length == 0) {
            throw new IllegalArgumentException("To numbers list is empty!");
        }
        for (String numberTo : numbersToList) {
            Message.creator(
                    new PhoneNumber(numberTo),
                    new PhoneNumber(numberForm),
                    msg.getData()
            ).create(this.twilioRestClient);
        }
    }

    @Override
    public void destroy() {
    }

}
