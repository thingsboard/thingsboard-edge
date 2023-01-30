/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.pe.twilio.voice;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Pause;
import com.twilio.twiml.voice.Say;
import com.twilio.twiml.voice.SsmlProsody;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "twilio voice",
        configClazz = TbTwilioVoiceNodeConfiguration.class,
        nodeDescription = "Sends voice message via Twilio.",
        nodeDetails = "Will send message payload as voice message via Twilio, using Twilio text to speech service.",
        uiResources = {"static/rulenode/twilio-config.js"},
        configDirective = "tbActionNodeTwilioVoiceConfig",
        icon = "phone_in_talk",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/external-nodes/#twilio-voice-node"
)
public class TbTwilioVoiceNode implements TbNode {

    private TbTwilioVoiceNodeConfiguration config;
    private TwilioRestClient twilioRestClient;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbTwilioVoiceNodeConfiguration.class);
        this.twilioRestClient = new TwilioRestClient.Builder(this.config.getAccountSid(), this.config.getAccountToken()).build();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        withCallback(ctx.getExternalCallExecutor().executeAsync(() -> {
                    sendVoiceMessage(ctx, msg);
                    return null;
                }),
                ok -> ctx.tellNext(msg, SUCCESS),
                fail -> ctx.tellFailure(msg, fail));
    }

    private void sendVoiceMessage(TbContext ctx, TbMsg msg) throws Exception {
        String numberFrom = TbNodeUtils.processPattern(this.config.getNumberFrom(), msg);
        String numbersTo = TbNodeUtils.processPattern(this.config.getNumbersTo(), msg);

        String[] numbersToList = numbersTo.split(",");
        if (StringUtils.isBlank(numbersToList[0])) {
            throw new IllegalArgumentException("To numbers list is empty!");
        }

        String payload = msg.getData();
        payload = payload.substring(1, payload.length()-1);
        Say.Language language = Say.Language.EN_US;
        for (Say.Language lang : Say.Language.values()) {
            if (lang.toString().equals(config.getLanguage())){
                language = lang;
                break;
            }
        }

        Say.Voice voice = Say.Voice.MAN;
        for (Say.Voice voiceIter : Say.Voice.values()) {
            if (voiceIter.toString().equals(config.getVoice())){
                voice = voiceIter;
                break;
            }
        }

        SsmlProsody prosody = new SsmlProsody.Builder(payload)
                .pitch(config.getPitch().toString() + "%")
                .rate(config.getRate().toString() + "%")
                .volume(config.getVolume().toString() + "dB")
                .build();

        Pause startPause = new Pause.Builder().length(config.getStartPause()).build();
        Say say = new Say.Builder().language(language).voice(voice).prosody(prosody).build();
        VoiceResponse response = new VoiceResponse.Builder().pause(startPause).say(say).build();

        for (String numberTo : numbersToList) {
            Call.creator(
                    new PhoneNumber(numberTo.trim()),
                    new PhoneNumber(numberFrom.trim()),
                    new Twiml(response.toXml())
            ).create(this.twilioRestClient);
        }
    }

}
