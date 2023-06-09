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
package org.thingsboard.rule.engine.pe.twilio;

import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

/**
 * Created by igor on 5/25/18.
 */
@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "twilio sms",
        configClazz = TbTwilioSmsNodeConfiguration.class,
        nodeDescription = "Sends SMS message via Twilio.",
        nodeDetails = "Will send message payload as SMS message via Twilio.",
        uiResources = {"static/rulenode/twilio-config.js"},
        configDirective = "tbActionNodeTwilioSmsConfig",
        icon = "sms",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/external-nodes/#twilio-sms-node"
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
    public void onMsg(TbContext ctx, TbMsg msg) {
        log.trace("[{}][{}] Msg received: {}", ctx.getTenantId().getId(), ctx.getSelfId().getId(), msg);
        try {
            withCallback(ctx.getExternalCallExecutor().executeAsync(() -> {
                        sendSms(ctx, msg);
                        return null;
                    }),
                    ok -> {
                        log.trace("[{}][{}] Successfully processed msg: {}", ctx.getTenantId().getId(), ctx.getSelfId().getId(), msg);
                        ctx.tellNext(msg, SUCCESS);
                    },
                    fail -> {
                        logFailure(ctx, msg, fail);
                        ctx.tellFailure(msg, fail);
                    });
        } catch (Exception ex) {
            logFailure(ctx, msg, ex);
            ctx.tellFailure(msg, ex);
        }
    }

    private void logFailure(TbContext ctx, TbMsg msg, Throwable fail) {
        String errorMsg = String.format("[%s][%s] Failed to process msg: %s", ctx.getTenantId().getId(), ctx.getSelfId().getId(), msg);
        log.error(errorMsg, fail);
    }

    private void sendSms(TbContext ctx, TbMsg msg) {
        String numberFrom = TbNodeUtils.processPattern(this.config.getNumberFrom(), msg);
        String numbersTo = TbNodeUtils.processPattern(this.config.getNumbersTo(), msg);
        String[] numbersToList = numbersTo.split(",");
        if (numbersToList.length == 0) {
            throw new IllegalArgumentException("To numbers list is empty!");
        }
        for (String numberTo : numbersToList) {
            log.trace("[{}][{}][{}] Sending sms for number: {} ...", ctx.getTenantId().getId(), ctx.getSelfId().getId(), msg.getId(), numbersTo);
            try {
                Message.creator(
                        new PhoneNumber(numberTo.trim()),
                        new PhoneNumber(numberFrom.trim()),
                        msg.getData().replaceAll("^\"|\"$", "").replaceAll("\\\\n", "\n")
                ).create(this.twilioRestClient);
                log.trace("[{}][{}][{}] Sms for number: {} sent successfully!", ctx.getTenantId().getId(), ctx.getSelfId().getId(), msg.getId(), numbersTo);
            } catch (ApiException e) {
                String apiMsg = String.format("[%s][%s] Failed to send sms from number %s to number %s",
                        ctx.getTenantId().getId(), ctx.getSelfId().getId(), numberFrom, numberTo);
                log.debug(apiMsg, e);
                ctx.tellFailure(msg, new RuntimeException(apiMsg, e));
            }
        }
    }

}
