/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.pe.twilio;

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

import java.util.concurrent.ExecutionException;

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
        uiResources = {"static/rulenode/twilio-sms-config.js"},
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
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        withCallback(ctx.getExternalCallExecutor().executeAsync(() -> {
                    sendSms(ctx, msg);
                    return null;
                }),
                ok -> ctx.tellNext(msg, SUCCESS),
                fail -> ctx.tellFailure(msg, fail));
    }

    private void sendSms(TbContext ctx, TbMsg msg) throws Exception {
        String numberFrom = TbNodeUtils.processPattern(this.config.getNumberFrom(), msg.getMetaData());
        String numbersTo = TbNodeUtils.processPattern(this.config.getNumbersTo(), msg.getMetaData());
        String[] numbersToList = numbersTo.split(",");
        if (numbersToList.length == 0) {
            throw new IllegalArgumentException("To numbers list is empty!");
        }
        for (String numberTo : numbersToList) {
            Message.creator(
                    new PhoneNumber(numberTo.trim()),
                    new PhoneNumber(numberFrom.trim()),
                    msg.getData().replaceAll("^\"|\"$", "").replaceAll("\\\\n", "\n")
            ).create(this.twilioRestClient);
        }
    }

    @Override
    public void destroy() {
    }

}
